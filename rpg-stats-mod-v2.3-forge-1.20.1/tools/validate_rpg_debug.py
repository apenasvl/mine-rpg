#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
src = ROOT / "src/main/java/com/rpgstats"
mod = (src / "RPGStatsMod.java").read_text(encoding="utf-8")
debug = (src / "debug/RpgDebug.java").read_text(encoding="utf-8")

for token in (
    'literal("debug")',
    'literal("on")',
    'literal("off")',
    'literal("state")',
    'literal("clear")',
    'RpgDebug.enable(player)',
    'RpgDebug.disable(player)',
    'RpgDebug.sendState(player)',
):
    assert token in mod, f"debug command missing: {token}"

for token in (
    '@Mod.EventBusSubscriber',
    'LivingHurtEvent',
    'LivingDeathEvent',
    'PlayerTickEvent',
    'ass_combo',
    'ass_echo',
    'internal_ass_nightblade_escape',
    'internal_ass_counterblade',
    'internal_ass_soulknife',
    'internal_ass_void_breach',
    'internal_ass_demo_detonation',
):
    assert token in debug, f"runtime debug observer missing: {token}"

# Debug must be opt-in and observational; it may read state, but it must not call damage/heal or alter stats.
assert 'ENABLED.contains' in debug
assert '.damage(' not in debug
assert '.heal(' not in debug
assert 'stats.resource =' not in debug
assert 'setGauge(' not in debug
assert 'startTimer(' not in debug

print("RPG runtime debug validation OK")
