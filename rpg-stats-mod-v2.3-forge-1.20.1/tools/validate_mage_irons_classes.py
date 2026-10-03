#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


compat = (SRC / "compat/irons/IronsSpellsCompat.java").read_text(encoding="utf-8")
bridge = (SRC / "compat/irons/IronsMageClassBridge.java").read_text(encoding="utf-8")
mage_handler = (SRC / "combat/MageCombatHandler.java").read_text(encoding="utf-8")
abilities = (SRC / "ability/MageAbilityRegistry.java").read_text(encoding="utf-8")
integration = bridge + "\n" + compat + "\n" + mage_handler

# Real Iron cast lifecycle must reach the class bridge before old per-spell hooks.
require("IronsMageClassBridge.additionalManaReduction" in compat,
        "Iron cast cost is not using Mage conditional mana mechanics")
require("IronsMageClassBridge.onCast" in compat,
        "Iron casts are not driving Mage class progression")
require("IronsMageClassBridge.modifyDamage" in compat,
        "Iron damage is not receiving specialization mechanics")
require("IronsMageClassBridge.onResolvedHit" in compat,
        "Accepted Iron hits are not feeding Mage post-hit mechanics")
require("IronsMageClassBridge.applyIronsAttributes" in compat,
        "Iron native attributes are not receiving Conjuration integration")

# Five Houses. Elemental mechanics intentionally remain in MageCombatHandler.onExternalSpellHit;
# the other cast-lifecycle gaps are filled by the new Iron class bridge.
for token in (
    "mag_ele_resonance",
    "mag_arc_sequence",
    "summon_damage",
    "mag_occ_forbidden",
    "mag_time_rhythm",
):
    require(token in integration,
            f"Mage House is disconnected from Iron lifecycle: {token}")

# Fifteen specialization families: at least one runtime Iron-facing mechanic each.
required_spec_tokens = {
    "Pyromancer": "irons_fire_heat",
    "Cryomancer": "winterHeartTicks",
    "Stormcaller": "irons_lightning_area",
    "Runist": "mag_rune_amplifier",
    "Illusionist": "ILLUSION_ECHO_READY",
    "Telemancer": "irons_gravity_control",
    "Conjurer": "SUMMON_DAMAGE",
    "Animist": "mag_anim_life_spirit",
    "Astral Forger": "irons_astral_offense",
    "Bloodmancer": "irons_blood_drain",
    "Curseweaver": "mag_curse_ruin",
    "Hexblade": "irons_hex_slash",
    "Accelerator": "mag_acc_momentum",
    "Stagnator": "mag_stag_time_lock",
    "Reverser": "temporalEchoReady",
}
for name, token in required_spec_tokens.items():
    require(token in integration,
            f"{name} has no runtime Iron-facing integration token: {token}")
    require(token in abilities or token in bridge or token in mage_handler or token in compat,
            f"{name} integration token disappeared from Mage definitions/runtime: {token}")

# Core mechanics that were previously mostly tied to legacy mage-active casts.
for token in (
    "mag_core_flow",
    "mag_core_guard",
    "mag_core_echo",
    "recentArcaneCategories",
    "temporalFragments",
):
    require(token in bridge, f"Mage core lifecycle missing from Iron casts: {token}")

# Nature is wider than summons: only spells profiled as SUMMON may receive summon-damage multiplier.
require("IronsMageClassBridge.isCombatSummon(spellPath)" in compat,
        "Nature spells are still all treated as Conjuration damage")
require("profileFor(spellId) == IronsSpellBalance.Profile.SUMMON" in bridge,
        "Combat summons are not detected through the audited Iron profile")

# Echo/specialization damage is intentionally before the existing spell profile + per-target budget.
idx_class = compat.index("IronsMageClassBridge.modifyDamage")
idx_profile = compat.index("IronsSpellBalance.applyRpgScaling")
idx_budget = compat.index("IronsSpellBalance.applyTargetBudget")
require(idx_class < idx_profile < idx_budget,
        "Mage specialization damage must remain constrained by Iron profile and target budget")

# Summon scaling must be bounded; Iron itself multiplies summoned damage by SUMMON_DAMAGE.
require("bonus = Math.min(0.35f, bonus)" in bridge,
        "RPG Conjuration modifier on Iron SUMMON_DAMAGE needs an explicit cap")

# Multi-hit spells must consume cast-level procs only once, not once per projectile.
for token in ("consumeIfMatches(CORE_ECHO_READY", "consumeIfMatches(MIRROR_ECHO_READY",
              "consumeIfMatches(ILLUSION_ECHO_READY"):
    require(token in bridge, f"Per-cast proc is not single-consumption: {token}")

print("Mage Houses + 15 specializations -> Iron lifecycle validation OK")

# Concentration was intentionally removed from the Mage economy.
require("Concentração insuficiente" not in (SRC / "combat/MageCombatHandler.java").read_text(encoding="utf-8"), "Concentration cost still active")
require("stats.concentration" not in bridge, "Iron bridge still generates Concentration")
