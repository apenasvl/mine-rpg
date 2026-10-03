from inspect_combat_mods import inspect_jar as inspect_metadata, InspectionError
import argparse, json, zipfile
from pathlib import Path

def inspect_jar(path, expected=None, installed=None):
    report=inspect_metadata(path, expected or {}, installed)
    namespace=report['modId']
    entities={};items={};recipe_ids=set()
    with zipfile.ZipFile(path) as jar:
        for name in jar.namelist():
            if name.startswith('assets/') and name.endswith('/lang/en_us.json'):
                for key,label in json.loads(jar.read(name)).items():
                    for kind,target in (('entity',entities),('item',items)):
                        prefix=f'{kind}.{namespace}.'
                        if key.startswith(prefix) and '.' not in key[len(prefix):]:
                            ident=f'{namespace}:{key[len(prefix):]}'
                            target[ident]={'id':ident,'label':label,'source':'translation-key','verifiedRegistry':False}
            if name.startswith('data/') and '/recipes/' in name and name.endswith('.json'):
                result=json.loads(jar.read(name)).get('result',{})
                ident=result if isinstance(result,str) else result.get('item') if isinstance(result,dict) else None
                if ident: recipe_ids.add(ident)
        for ident,row in items.items(): row['recipeReferenced']=ident in recipe_ids
        report['entities']=[entities[x] for x in sorted(entities)]
        report['items']=[items[x] for x in sorted(items)]
        report['nativeClasses']=sorted(n[:-6].replace('/','.') for n in jar.namelist() if n.endswith('.class') and not n.startswith('META-INF/'))
    return report

def main():
    p=argparse.ArgumentParser();p.add_argument('--jars',required=True);p.add_argument('--output',required=True);p.add_argument('--lock')
    args=p.parse_args()
    expected=json.loads(Path(args.lock).read_text())['mods'] if args.lock else None
    installed=None if expected is None else {'minecraft':'1.20.1','forge':'47.4.0'}|{m['modId']:m['version'] for m in expected if m.get('enabled',True)}
    reports=[]
    for path in sorted(Path(args.jars).glob('*.jar')):
        entry=None if expected is None else next((m for m in expected if path.name==m['jarName']),None)
        if expected is not None and entry is None: raise InspectionError(f'Unexpected binary: {path.name}')
        reports.append(inspect_jar(path,entry,installed))
    if not reports: raise InspectionError('No native jars')
    if expected is not None and len(reports)!=sum(m.get('enabled',True) for m in expected): raise InspectionError('Missing selected binary')
    Path(args.output).write_text(json.dumps({'mods':reports,'warning':'Translation keys are candidates; registry IDs and effect hooks need bytecode/runtime confirmation.'},indent=2)+'\n')
    print(f'Inspected {len(reports)} Forge binaries')
if __name__=='__main__': main()
