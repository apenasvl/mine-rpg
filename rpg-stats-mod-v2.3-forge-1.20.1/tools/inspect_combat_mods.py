"""Inspect separately installed Forge binaries; never packages third-party jars."""
import argparse, hashlib, json, re, tomllib, zipfile
from pathlib import Path

class InspectionError(ValueError): pass

def includes_version(version, requirement):
    def parts(v):
        match=re.match(r'\d+(?:\.\d+)*',v.strip())
        if not match: raise InspectionError(f'Unsupported version: {v}')
        result=tuple(int(x) for x in match.group().split('.'))
        return result + (0,) * (5-len(result))
    if not requirement or requirement[0] not in '[(': return version == requirement
    body=requirement[1:-1]
    if ',' not in body: return version == body.strip()
    lower,upper=body.split(',',1);value=parts(version)
    return (not lower.strip() or value >= parts(lower) if requirement[0]=='[' else not lower.strip() or value > parts(lower)) and (not upper.strip() or value <= parts(upper) if requirement[-1]==']' else not upper.strip() or value < parts(upper))


def inspect_jar(path, expected, installed=None):
    digest=hashlib.sha256(Path(path).read_bytes()).hexdigest()
    if expected.get('sha256') and expected['sha256'] != digest: raise InspectionError('Checksum mismatch')
    with zipfile.ZipFile(path) as jar:
        if 'META-INF/mods.toml' not in jar.namelist(): raise InspectionError('Missing Forge metadata')
        meta=tomllib.loads(jar.read('META-INF/mods.toml').decode())
        if meta.get('modLoader')!='javafml': raise InspectionError('Unsupported loader')
        runtime_forge=installed.get('forge','47.4.0') if isinstance(installed,dict) else '47.4.0'
        if not includes_version(runtime_forge, meta.get('loaderVersion','')): raise InspectionError(f'Forge {runtime_forge} not supported')
        mods=meta.get('mods',[])
        mod=next((m for m in mods if m['modId']==expected.get('modId')),mods[0] if mods else None)
        if not mod: raise InspectionError('Missing mod id')
        if mod.get('version')=='${file.jarVersion}':
            manifest=jar.read('META-INF/MANIFEST.MF').decode()
            match=re.search(r'^Implementation-Version: (.+)$',manifest,re.M)
            if not match: raise InspectionError('Unresolved jar version')
            mod['version']=match.group(1).strip()
        for key in ('modId','version'):
            if expected.get(key) and mod[key]!=expected[key]: raise InspectionError(f'{key} mismatch: {mod[key]}')
        dependencies=meta.get('dependencies',{}).get(mod['modId'],[])
        minecraft=next((d for d in dependencies if d['modId']=='minecraft'),None)
        if minecraft is None and not expected.get('library'): raise InspectionError('Minecraft 1.20.1 requirement not verified')
        if minecraft is not None and not includes_version('1.20.1', minecraft['versionRange']): raise InspectionError('Minecraft 1.20.1 requirement not verified')
        if installed is not None:
            for dep in dependencies:
                if not dep.get('mandatory',False): continue
                if dep['modId'] not in installed: raise InspectionError(f"Missing dependency: {dep['modId']}")
                if isinstance(installed,dict) and not includes_version(installed[dep['modId']],dep['versionRange']):
                    raise InspectionError(f"Incompatible dependency: {dep['modId']}")
        return {'modId':mod['modId'],'version':mod['version'],'sha256':digest,'dependencies':dependencies,'status':'metadata-verified'}

def main():
    p=argparse.ArgumentParser();p.add_argument('--lock',required=True);p.add_argument('--jars',required=True);p.add_argument('--output',required=True)
    args=p.parse_args();lock=json.loads(Path(args.lock).read_text()); reports=[]
    installed={'forge':'47.4.0','minecraft':'1.20.1'}|{m['modId']:m['version'] for m in lock['mods'] if m.get('enabled')}
    for entry in lock['mods']:
        if not entry.get('enabled'): continue
        report=inspect_jar(Path(args.jars)/f"{entry['coordinate'].split(':')[0]}.jar",entry,installed)
        reports.append(entry|report)
    out=Path(args.output);out.mkdir(parents=True,exist_ok=True);(out/'manifest.json').write_text(json.dumps(reports,indent=2)+'\n')
    print(f'Verified {len(reports)} Forge binaries')
if __name__=='__main__': main()
