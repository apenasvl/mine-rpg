"""Representative encounter plan from current profiles; never manufactures fight results."""
import json
from pathlib import Path
CLASSES=('GUERREIRO','MAGO','ARQUEIRO','ASSASSINO')
ANCHORS={'legendary_monsters:overgrown_colossus','soulsweapons:returning_knight','bosses_of_mass_destruction:obsidilith'}
def build_matrix(profiles,native):
    baseline={b['id']:b for b in native};rows=[];cases=[];seen=set()
    for p in profiles:
        identifier=p['id'];fixed=p['fixed'];level=fixed['reference_level']
        if identifier not in baseline:raise ValueError('missing native baseline: '+identifier)
        b=baseline[identifier]
        rows.append({'registry_id':identifier,'mod':identifier.split(':')[0],'recommended_level':level,
                     'native_hp':b['health'],'profile_hp_before_party':b['health']*fixed['health_factor'],
                     'native_armor':b['armor'],'native_attack_attribute':b['attack'],
                     'profile_damage_factor':fixed['damage_factor'],
                     'very_overlevel':'AVAILABLE' if level+15<=50 else 'UNAVAILABLE_LEVEL_CAP',
                     'phases':None,'special_attacks':None,'summons':None,'indirect_sources':None,
                     'encounter_status':'NOT_RUN'})
        trials=[(level,'balanced')]
        if identifier in ANCHORS:
            trials += [(n,build) for n in (level-5,level,level+5,level+15) if 1<=n<=50 for build in ('offensive','balanced','defensive')]
        for n,build in trials:
            for clazz in CLASSES:
                key=(identifier,clazz,n,build)
                if key in seen:continue
                seen.add(key)
                cases.append({'registry_id':identifier,'class':clazz,'level':n,'build':build,'status':'NOT_RUN',
                              'ttk_seconds':None,'player_dps':None,'damage_received':None,'sustain':None,
                              'resources':None,'cooldowns':None,'uptime':None,'outcome':None,'evidence':None})
    return {'schema':1,'coverage':'Representative plan; no full encounters measured',
            'references':{'boss_seconds':[90,240],'miniboss_seconds':[30,90]},'profiles':rows,'cases':cases}
if __name__=='__main__':
    root=Path(__file__).resolve().parents[1]
    profiles=json.loads((root/'src/main/resources/data/rpgstats/rpgstats/native_bosses.json').read_text())['entries']
    native=json.loads((root/'compat/boss-native-calibration-baseline.json').read_text())['bosses']
    result=build_matrix(profiles,native)
    (root/'compat/boss-arena-matrix.json').write_text(json.dumps(result,indent=2)+'\n')
    print(f"{len(result['profiles'])} current profiles; {len(result['cases'])} cases; ALL NOT_RUN")
