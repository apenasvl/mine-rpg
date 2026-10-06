"""CI harness for original native Forge jars; not a user mod installer."""
import argparse,json,hashlib,os,re,shutil,subprocess,urllib.request,zipfile,io,tomllib
from pathlib import Path
from inspect_combat_mods import inspect_jar
ROOT=Path(__file__).resolve().parents[1]
def download(url,target):
 target.parent.mkdir(parents=True,exist_ok=True)
 with urllib.request.urlopen(url,timeout=90) as response,target.open('wb') as output:shutil.copyfileobj(response,output)
 return target
def coordinate_jar(coordinate,target):
 name,file=coordinate.split(':');return download(f'https://cursemaven.com/curse/maven/{name}/{file}/{name}-{file}.jar',target)
def validated_runtime_forge(log,expected):
 evidence=re.search(r'Forge mod loading, version ([0-9.]+)',log)
 if evidence is None or evidence[1]!=expected:raise RuntimeError(f'runtime Forge mismatch: expected {expected}, observed {evidence[1] if evidence else "missing"}')
 return evidence[1]
def main():
 p=argparse.ArgumentParser();p.add_argument('--full',action='store_true');p.add_argument('--forge-version',choices=['47.4.0','47.4.10'],default='47.4.0');args=p.parse_args()
 server=ROOT/'build'/('production-boss-full' if args.full else 'production-boss');server.mkdir(parents=True,exist_ok=True)
 mods=server/'mods';mods.mkdir(exist_ok=True)
 # Main artifact must never contain the probe or native third-party classes.
 jars=[x for x in (ROOT/'build/libs').glob('*.jar') if not x.name.endswith('-sources.jar')]
 if len(jars)!=1:raise RuntimeError(f'Expected one remapped RPG artifact, found {jars}')
 with zipfile.ZipFile(jars[0]) as z:
  if any(x.startswith(('com/rpgstats/probe/','net/soulsweaponry/','net/miauczel/','com/cerbon/')) for x in z.namelist()):raise RuntimeError('Probe/native code in RPG artifact')
 shutil.copyfile(jars[0],mods/'rpgstats.jar')
 shutil.copyfile(ROOT/'build/production-probe/rpgstats-production-probe.jar',mods/'rpgstats-production-probe.jar')
 lock=json.loads((ROOT/'compat/boss-mods.lock.json').read_text());entries=[x for x in lock['mods'] if x.get('enabled')]
 if args.full:
  present={x['modId'] for x in entries}
  for x in json.loads((ROOT/'compat/combat-mods.lock.json').read_text())['mods']:
   if x.get('enabled') and x['modId'] not in present:entries.append(x);present.add(x['modId'])
 installed={'minecraft':'1.20.1','forge':args.forge_version}|{x['modId']:x['version'] for x in entries}
 reports=[]
 for x in entries:
  jar=coordinate_jar(x['coordinate'],mods/(x['modId']+'.jar'))
  reports.append(inspect_jar(jar,x,installed))
 if args.full:
  for coordinate in ['better-weaponry-better-combat-990523:6046452','simply-swords-659887:8746028','fzzy-config-1005914:6313552','kotlin-for-forge-351264:5402061','more-bows-and-arrows-888468:6843671','too-many-bows-1141533:8735189',
   'simply-bear-traps-1643250:8595248','monolib-968432:8543117','architectury-api-419699:5137938','curios-309927:6418456',
   'irons-spells-n-spellbooks-855414:8680180','playeranimator-658587:4587214',
   'irons-lib-1492763:9003169','caelus-308989:5281700',
   'mmmmmmmmmmmm-225738:7963208','selene-499980:7541536']:
   jar=coordinate_jar(coordinate,mods/(coordinate.split(':')[0]+'.jar'))
   if coordinate.startswith('kotlin-for-forge-'):
    # Original KFF is a Forge library container; its actual mod descriptor is nested.
    with zipfile.ZipFile(jar) as container:
     manifest=container.read('META-INF/MANIFEST.MF').decode()
     if 'FMLModType: LIBRARY' not in manifest:raise RuntimeError('KFF container is not a Forge library')
     embedded=json.loads(container.read('META-INF/jarjar/metadata.json'))['jars']
     descriptors=[]
     for entry in embedded:
      with zipfile.ZipFile(io.BytesIO(container.read(entry['path']))) as nested:
       if 'META-INF/mods.toml' in nested.namelist():descriptors.extend(tomllib.loads(nested.read('META-INF/mods.toml').decode()).get('mods',[]))
     if not any(x.get('modId')=='kotlinforforge' for x in descriptors):raise RuntimeError('Original KFF mod descriptor missing')
     reports.append({'modId':'kotlinforforge','sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'embeddedMods':descriptors,'status':'native-container-verified'})
   else:reports.append(inspect_jar(jar,{'library':True}))
 installer=server/'forge-installer.jar'
 if args.forge_version=='47.4.0':shutil.copyfile(ROOT/'build/production-probe/forge-installer.jar',installer)
 else:download(f'https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-{args.forge_version}/forge-1.20.1-{args.forge_version}-installer.jar',installer)
 subprocess.run(['java','-jar',str(installer),'--installServer'],cwd=server,check=True)
 (server/'eula.txt').write_text('eula=true\n')
 (server/'user_jvm_args.txt').write_text('-Xmx4G\n-Dforge.gameTestServer=true\n-Dforge.enableGameTest=true\n-Dforge.enabledGameTestNamespaces=rpgstats\n-Drpgstats.productionBossTests=true\n')
 (server/'server.properties').write_text('online-mode=false\ndifficulty=normal\nlevel-type=minecraft:flat\nmax-tick-time=120000\nview-distance=3\nsimulation-distance=3\n')
 (server/'native-manifest.json').write_text(json.dumps(reports,indent=2)+'\n')
 launcher=['cmd.exe','/d','/c','run.bat','nogui'] if os.name=='nt' else ['bash','run.sh','nogui']
 result=subprocess.run(launcher,cwd=server,timeout=900)
 log=(server/'logs/latest.log').read_text()
 runtime_forge=validated_runtime_forge(log,args.forge_version)
 if result.returncode!=0 or not re.search(r'All \d+ required tests passed',log):
  raise RuntimeError(f'Production tests not proven; exit {result.returncode}')
 (server/'result.json').write_text(json.dumps({'status':'PASS','runtimeForge':runtime_forge,'originalNativeJars':True,'mainJarSha256':hashlib.sha256(jars[0].read_bytes()).hexdigest(),'summary':re.search(r'All \d+ required tests passed',log)[0]},indent=2)+'\n')
if __name__=='__main__':main()
