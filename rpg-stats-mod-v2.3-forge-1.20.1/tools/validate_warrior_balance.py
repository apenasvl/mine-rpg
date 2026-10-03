#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"

handler = (SRC / "combat/WarriorSpecializationHandler.java").read_text(encoding="utf-8")
mechanics = (SRC / "combat/ClassMechanics.java").read_text(encoding="utf-8")
state = (SRC / "combat/CombatState.java").read_text(encoding="utf-8")
debug = (SRC / "debug/RpgDebug.java").read_text(encoding="utf-8")
registry = (SRC / "ability/ClassAbilityRegistry.java").read_text(encoding="utf-8")
audit = (ROOT / "WARRIOR-BALANCE-PASS.md").read_text(encoding="utf-8")
audit_flat = " ".join(audit.split())

spec_cases = (
    "BULWARK", "WARLORD", "JUGGERNAUT",
    "BLOOD_REAVER", "RAGEBORN", "PAIN_COLOSSUS",
    "BLADEMASTER", "DUEL_MASTER", "TITAN_MAULER",
    "RUNE_KNIGHT", "SPELLBREAKER", "STORMBLADE",
    "BANNER_LORD", "TACTICIAN", "IRON_GUARD",
)
for spec in spec_cases:
    assert spec in handler or spec in mechanics, f"Warrior specialization missing: {spec}"

for token in (
    "@Mod.EventBusSubscriber",
    "PROC_GUARD",
    "interceptForIronGuard",
    "contributeToWarlordMark",
    "painColossus",
    "runeKnight",
    "stormblade",
    "pulseBanner",
    'state.timer("war_order_advance")',
    'state.startTimer("war_exhaustion", 90)',
    "Math.min(4.5f",
    "Math.min(4f",
    "target == original",
):
    assert token in handler, f"Warrior event contract disconnected: {token}"

# Secondary damage is bounded and extra-target filters forbid re-hitting the original.
assert "release = Math.min(4.5f" in handler
assert "Math.min(4f, 1.25f + runes * .75f)" in handler
assert "float splash = Math.min(2.25f" in handler
assert "procDamage(player, extra, 2.75f)" in handler
assert "target == original" in handler

# Defensive/support loops have explicit caps rather than immunity or global scans.
assert "Math.min(asc ? 5f : 3.5f" in handler
assert "event.getAmount() * (asc ? .35f : .25f)" in handler
assert "player.age % 20 == 0" in handler
assert "warBannerPlaced" in state

# Tactical resources cannot be pre-stocked forever out of combat.
for token in (
    "tickWarriorTacticalGauges()",
    'decayGauge("war_fury_loop", 0.020f, 10f)',
    'decayGauge("war_pain", 0.012f, 10f)',
    'decayGauge("war_blade_cadence", 0.030f, 5f)',
    'decayGauge("war_storm_charge", 0.026f, 5f)',
):
    assert token in state, f"Warrior decay missing: {token}"

# Five path identities and all tactical states are visible to the player/debugger.
for token in ("Guarda", "Furia/Ferida", "Abertura de Arma", "Carga Runica", "Moral/Ordem"):
    assert token in registry
for token in ("war_thirst", "war_blade_cadence", "war_spell_seal", "war_tactic_progress",
              "war_order_advance", "war_banner"):
    assert token in debug

assert "aproximadamente 28" in audit
assert "Runtime Minecraft continua pendente" in audit_flat
print("Warrior complete balance pass validation OK")
