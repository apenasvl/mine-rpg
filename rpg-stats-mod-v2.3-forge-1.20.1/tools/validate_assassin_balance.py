#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"

state = (SRC / "combat/CombatState.java").read_text(encoding="utf-8")
mechanics = (SRC / "combat/ClassMechanics.java").read_text(encoding="utf-8")
balance = (SRC / "balance/ClassBalance.java").read_text(encoding="utf-8")

# Assassin burst must still be conditional. Do not sneak in an Archer-style permanent raw multiplier.
assert "ASSASSIN_PERSISTENT_DAMAGE" not in balance
assert 'if (stats.clazz == RPGClass.ASSASSINO && state.gauge("ass_combo") >= 3f) bonus += .035f;' in mechanics
assert 'state.setGauge("ass_combo", state.gauge("ass_combo") - 1.5f, GAUGE_CAP);' in mechanics

# Previously dead/isolated gauges now converge into one bounded Combo economy.
for token in (
    'feedAssassinCombo(key, amount)',
    'case "ass_advantage" -> 0.35f',
    'case "ass_echo" -> 0.30f',
    'case "ass_preparation" -> 0.25f',
    'case "ass_dance" -> 0.20f',
    'case "ass_energy_loop" -> 0.50f',
    'setGauge("ass_combo", gauge("ass_combo") + amount * conversion, 5f)',
):
    assert token in state, f"Assassin Combo economy disconnected: {token}"

# Shadow's passive out-of-combat preparation must NOT feed Combo; only an actual Opening does.
assert 'case "ass_shadow_prep"' not in state
assert '"ass_opening".equals(key)' in state
assert 'gauge("ass_combo") + 0.55f' in state

# Prepared burst cannot be pre-stocked forever between fights.
for token in (
    'tickAssassinTacticalGauges()',
    'decayGauge("ass_combo", 0.018f, 5f)',
    'decayGauge("ass_advantage", 0.012f, 5f)',
    'decayGauge("ass_echo", 0.010f, 6f)',
    'decayGauge("ass_preparation", 0.014f, 5f)',
    'decayGauge("ass_dance", 0.015f, 5f)',
    'decayGauge("ass_energy_loop", 0.025f, 10f)',
):
    assert token in state, f"Assassin out-of-combat decay disconnected: {token}"

# Sanity model: normal class actions should build Combo gradually, not in one button press.
combo = 0.0
for key, amount, conversion in (
    ("advantage", 0.8, 0.35),
    ("echo", 0.6, 0.30),
    ("preparation", 0.55, 0.25),
    ("dance", 1.0, 0.20),
    ("energy", 0.8, 0.50),
):
    combo = min(5.0, combo + amount * conversion)
assert 0.0 < combo < 3.0, "one mixed rotation must not instantly enable max-risk burst"

# An actual Opening helps bridge toward a burst window but is still bounded.
combo = min(5.0, combo + 0.55)
assert combo < 3.0

# Out-of-combat decay eventually clears a fully stocked Combo.
combo = 5.0
for _ in range(300):
    combo = max(0.0, combo - 0.018)
assert combo == 0.0

print("Assassin balance pass 1 validation OK")
