"""Install only the selected external profile. RPG Stats jar is supplied separately."""
import argparse,json,os,tempfile,urllib.request,tomllib,zipfile
from pathlib import Path
from inspect_combat_mods import inspect_jar, InspectionError

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--destination',required=True,type=Path)
    parser.add_argument('--client',action='store_true',help='Include Forge footsteps on a client')
    args=parser.parse_args()
    lock=json.loads((Path(__file__).resolve().parents[1]/'compat/combat-mods.lock.json').read_text())
    entries=[m for m in lock['mods'] if m.get('enabled') or (args.client and m.get('status')=='client-only')]
    count=install_profile(args.destination, entries)
    print(f'Installed {count} verified external jars in {args.destination}. Install the matching RPG Stats jar separately.')

def install_profile(destination, entries):
    installed={'forge':'47.4.0','minecraft':'1.20.1'}|{m['modId']:m['version'] for m in entries}
    destination.mkdir(parents=True,exist_ok=True)
    for existing in destination.glob('*.jar'):
        try:
            with zipfile.ZipFile(existing) as jar:
                ids={m['modId'] for m in tomllib.loads(jar.read('META-INF/mods.toml').decode()).get('mods',[])}
        except (KeyError,ValueError,zipfile.BadZipFile): continue
        if ids & {'rogues','simplyswords'}:
            raise InspectionError(f'Excluded mod still installed: {existing.name}. Use a separate selected profile.')
    with tempfile.TemporaryDirectory(prefix='.rpg-combat-',dir=destination) as tmp:
        staged=[]
        for mod in entries:
            if not mod.get('sha256'): raise InspectionError(f"Unverified hash: {mod['modId']}")
            name,file=mod['coordinate'].split(':');filename=f'{name}-{file}.jar'
            url=f'https://cursemaven.com/curse/maven/{name}/{file}/{filename}'
            path=Path(tmp)/filename
            with urllib.request.urlopen(url,timeout=120) as response: path.write_bytes(response.read())
            inspect_jar(path,mod,installed)
            target=destination/filename
            if target.exists() and target.read_bytes()!=path.read_bytes(): raise InspectionError(f'Refusing to replace different file: {target}')
            # Avoid duplicate versions already present; do not delete user files.
            for existing in destination.glob('*.jar'):
                try:
                    with zipfile.ZipFile(existing) as jar:
                        ids={m['modId'] for m in tomllib.loads(jar.read('META-INF/mods.toml').decode()).get('mods',[])}
                except (KeyError,ValueError,zipfile.BadZipFile): continue
                if mod['modId'] in ids and existing.name!=filename:
                    raise InspectionError(f'Remove existing version first: {existing.name}')
            staged.append((path,target))
        for path,target in staged: os.replace(path,target)
    return len(entries)
if __name__=='__main__': main()
