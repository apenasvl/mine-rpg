"""Static Forge/compat contracts. Explicitly NOT a runtime test."""
from pathlib import Path
import json
import re
import sys
import zipfile

root=Path(__file__).resolve().parents[1]
java=root/'src/main/java/com/rpgstats'
irons_compat=java/'compat'/'irons'
files=list(java.rglob('*.java'))
for p in files:
    source=p.read_text(encoding='utf-8')
    assert not re.search(r'^import (?:static )?net\.fabricmc\.',source,re.M), p
    # Direct Iron imports are restricted to the optional compat implementation package.
    if irons_compat in p.parents:
        continue
    assert not re.search(r'^import (?:static )?io\.redspace\.ironsspellbooks\.', source, re.M), \
        f'Iron import leaked into core: {p}'
assert not (root/'src/main/resources/fabric.mod.json').exists()

meta=(root/'src/main/resources/META-INF/mods.toml').read_text(encoding='utf-8')
assert 'modLoader="javafml"' in meta and 'modId="forge"' in meta
assert 'mandatory=true' in meta and 'fabricloader' not in meta
assert 'modId="irons_spellbooks"' in meta and 'mandatory=false' in meta
assert '[47.4.0,48)' in meta

config=(root/'gradle.properties').read_text(encoding='utf-8')
assert 'loom.platform=forge' in config and 'minecraft_version=1.20.1' in config
assert 'forge_version=47.4.0' in config
assert 'irons_spells_version=1.20.1-3.16.3' in config

build=(root/'build.gradle').read_text(encoding='utf-8')
assert 'net.minecraftforge:forge:' in build and 'fabric-api' not in build
assert 'https://cursemaven.com' in build
assert 'curse.maven:irons-spells-n-spellbooks-855414:8680180' in build
assert 'modCompileOnly' in build and 'transitive = false' in build
assert 'verifyForgeJar' in build and 'remapJar' in build

mixins=json.loads((root/'src/main/resources/rpgstats.mixins.json').read_text(encoding='utf-8'))
assert mixins['refmap']=='rpgstats.refmap.json'
for name in mixins['mixins']:
    assert (java/'mixin'/f'{name}.java').exists(), name
for p in (root/'src/main/resources').rglob('*.json'):
    json.loads(p.read_text(encoding='utf-8'))

for name in ['IEntityDataSaver.java','mixin/PlayerEntityMixin.java','stats/StatsManager.java']:
    text=(java/name).read_text(encoding='utf-8')
    assert 'rpgstats$getPersistentData()' in text, name
    assert not re.search(r'(?<!\$)getPersistentData\(\)',text), name

network=(java/'network/RpgNetwork.java').read_text(encoding='utf-8')
mod=(java/'RPGStatsMod.java').read_text(encoding='utf-8')
ids=re.findall(r'new Identifier\(MOD_ID, "([^"]+)"\)',mod)
for action in ids:
    if action not in ('sync_stats','open_screen'):
        assert f'case "{action}"' in network, action
assert 'Optional.of(NetworkDirection.PLAY_TO_SERVER)' in network
assert network.count('Optional.of(NetworkDirection.PLAY_TO_CLIENT)')==4
assert 'b.readByteArray(512)' in network and 'ctx.enqueueWork' in network

# Universal dodge is intentionally gone: no key, packet route or active backend implementation.
client=(java/'RPGStatsClient.java').read_text(encoding='utf-8')
souls=(java/'combat/SoulslikeCombat.java').read_text(encoding='utf-8')
combat_state=(java/'combat/CombatState.java').read_text(encoding='utf-8')
assert client.count('value=Dist.CLIENT')==2
assert 'key.rpgstats.dodge' not in client
assert 'case "dodge"' not in network
assert 'new Identifier(MOD_ID, "dodge")' not in mod
assert 'tryDodge(' not in souls and 'DODGE_' not in souls
assert 'dodgeTicks' not in combat_state and 'dodgeCooldown' not in combat_state
for lang in ['pt_br.json','en_us.json']:
    assert 'rpgstats.dodge' not in (root/'src/main/resources/assets/rpgstats/lang'/lang).read_text(encoding='utf-8')

# Iron's is linked only through an optional, reflectively loaded adapter.
compat=(java/'compat/CompatManager.java').read_text(encoding='utf-8')
adapter=java/'compat/irons/IronsSpellsCompat.java'
assert adapter.exists()
assert 'Class.forName' in compat and 'IronsSpellsCompat' in compat
adapter_text=adapter.read_text(encoding='utf-8')
assert 'stats.clazz != RPGClass.MAGO) event.setCanceled(true)' in adapter_text
assert 'sumPassive(stats.unlockedNodes, "cooldown_recovery")' in adapter_text
assert 'sumPassive(stats.unlockedNodes, "cooldown_recovery", false)' not in adapter_text
for contract in ['SpellDamageEvent','SpellOnCastEvent','SpellCooldownAddedEvent','SpellPreCastEvent',
                 'LivingDamageEvent','MageCombatHandler.modifyMagicDamage','HouseRules.surcharge']:
    assert contract in adapter_text, contract
mixin=(java/'mixin/LivingEntityDamageMixin.java').read_text(encoding='utf-8')
assert 'CompatManager.isIronsSpellDamage(source)' in mixin

# Mage arsenal restructuring: Iron's owns castable spells; RPG Stats keeps unique techniques.
audit=(root/'MAGE-IRONS-AUDIT.md').read_text(encoding='utf-8')
assert 'KEEP/ADAPT/REPLACE/REMOVE' not in audit  # table uses explicit decisions, not an unresolved placeholder
assert audit.count('| `mag_') == 71
mage_registry=(java/'ability/MageAbilityRegistry.java').read_text(encoding='utf-8')
active_ids=set(re.findall(r'put\("([^"]+)"[^\n]*SkillEffect\.active', mage_registry))
assert len(active_ids) == 29, active_ids
assert mage_registry.count('fallback("mag_') == 42
assert '!CompatManager.isActive("irons_spells")' in mage_registry
replaced={
    'mag_pyr_ember_lance','mag_cryo_ice_shard','mag_storm_arc_bolt','mag_arc_pulse',
    'mag_pyr_flame_wall','mag_tele_blink','mag_tele_singularity','mag_blood_lance',
    'mag_blood_transfusion','mag_acc_temporal_step','mag_stag_temporal_slow'
}
assert active_ids.isdisjoint(replaced), active_ids & replaced
assert 'MageManaBridge' in compat and 'io.redspace.ironsspellbooks.api' not in compat
for contract in ['MagicData.getPlayerMagicData','AttributeRegistry.MAX_MANA',
                 'registerMageManaBridge','MageCombatHandler.onExternalSpellHit',
                 'MageBalance.TEMPORARY_CDR_CAP']:
    assert contract in adapter_text, contract
combat=(java/'combat/CombatHandler.java').read_text(encoding='utf-8')
assert 'CompatManager.usesExternalMageMana()' in combat
assert 'never run a second RPG regen loop' in combat
mage_handler=(java/'combat/MageCombatHandler.java').read_text(encoding='utf-8')
mage_state=(java/'combat/MageState.java').read_text(encoding='utf-8')
assert 'CompatManager.canUseRpgTechnique' in combat
assert 'temporalMarkDimension' in mage_state and 'rewriteDimension' in mage_state
assert 'TeleportSpell.TeleportData' not in mage_handler
assert 'TargetAreaCastData' not in mage_handler
house=(java/'classes/HouseRules.java').read_text(encoding='utf-8')
assert 'case "cooldown_recovery" -> .60f;' in house
assert 'default -> .65f;' in house

events=(java/'forge/ForgeEvents.java').read_text(encoding='utf-8')
for event in ['Clone','PlayerRespawnEvent','PlayerLoggedInEvent','PlayerLoggedOutEvent',
              'PlayerChangedDimensionEvent','ServerTickEvent','EntityJoinLevelEvent',
              'EntityLeaveLevelEvent','ServerStoppedEvent','AddReloadListenerEvent']:
    assert event in events, event
assert 'LivingDeathEvent event' not in events
print(f'PASS: Forge + Iron\'s static contracts for {len(files)} Java files (NOT runtime).')

if len(sys.argv)>1:
    # Optional migration-only comparison. CombatState/SoulslikeCombat are excluded because v2.4
    # intentionally removes dodge; compat/build files are likewise allowed to change.
    prefixes=('classes/','ability/','tree/')
    exact={'stats/PlayerStats.java','stats/Stat.java','stats/SkillNodeHolder.java',
           'stats/StatsApplier.java','combat/CombatHandler.java','combat/MageCombatHandler.java',
           'combat/MageState.java','combat/MageBalance.java','balance/GlobalCaps.java'}
    checked=0
    with zipfile.ZipFile(sys.argv[1]) as base:
        for name in base.namelist():
            marker='/src/main/java/com/rpgstats/'
            if marker not in name: continue
            rel=name.split(marker,1)[1]
            if rel.startswith(prefixes) or rel in exact:
                assert (java/rel).read_bytes()==base.read(name), f'Game rules changed: {rel}'
                checked+=1
    print(f'PASS: {checked} migration-baseline sources byte-identical to the supplied ZIP.')

