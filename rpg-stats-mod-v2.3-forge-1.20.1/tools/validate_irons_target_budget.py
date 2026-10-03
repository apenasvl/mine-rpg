#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def apply_budget(amount: float, per_hit: float, cap: float, window: int,
                 state: tuple[float, int], tick: int) -> tuple[float, tuple[float, int]]:
    hit = min(max(0.0, amount), per_hit)
    if cap <= 0.0 or window <= 0:
        return hit, state

    spent, last = state
    if tick < last or tick - last > window * 4:
        spent, last = 0.0, tick
    elapsed = max(0, tick - last)
    if elapsed:
        spent = max(0.0, spent - cap * (elapsed / window))
    available = max(0.0, cap - spent)
    accepted = min(hit, available)
    return accepted, (spent + accepted, tick)


def burst_total(hits: int, amount: float, per_hit: float, cap: float, window: int) -> float:
    state = (0.0, 0)
    total = 0.0
    for _ in range(hits):
        accepted, state = apply_budget(amount, per_hit, cap, window, state, 0)
        total += accepted
    return total


balance = (SRC / "compat/irons/IronsSpellBalance.java").read_text(encoding="utf-8")
compat = (SRC / "compat/irons/IronsSpellsCompat.java").read_text(encoding="utf-8")

# Playtest contract: Warrior active hit with an iron sword is the single-target ceiling reference.
require("WARRIOR_IRON_SWORD_BURST_REFERENCE = 28f" in balance,
        "Warrior ~28 benchmark must remain explicit")
require("MAGE_SINGLE_HIT_CEILING = 27f" in balance,
        "Iron Mage single-hit ceiling must stay below the Warrior benchmark")
require("MAX_BUDGET_STATES = 4096" in balance,
        "Target budget cache needs a hard size bound")
require("LinkedHashMap" in balance and "removeEldestEntry" in balance,
        "Target budget state must be bounded instead of growing forever")

# Final target budget must run after spell-profile scaling and the global emergency cap.
profile_pos = compat.find("IronsSpellBalance.applyRpgScaling(spellId, original, result, persistentMultiplier)")
global_pos = compat.find("GlobalCaps.damageMultiplier")
budget_pos = compat.find("IronsSpellBalance.applyTargetBudget(")
set_pos = compat.find("event.setAmount(Math.max(0f, result))")
require(min(profile_pos, global_pos, budget_pos, set_pos) >= 0,
        "Iron damage pipeline is missing one of the balance stages")
require(profile_pos < global_pos < budget_pos < set_pos,
        "Per-target budget must be the final damage guard before event.setAmount")
require("player.getUuid(), target.getUuid(), player.age, result" in compat,
        "Budget key must be per caster + target and use server-side entity age")

# The catalog remains complete and every spell belongs to exactly one broad profile.
expected_spells = {
    "acupuncture", "blood_needles", "blood_slash", "blood_step", "devour", "heartstop",
    "raise_dead", "ray_of_siphoning", "wither_skull", "sacrifice",
    "counterspell", "dragon_breath", "evasion", "magic_arrow", "magic_missile", "starfall",
    "teleport", "summon_ender_chest", "recall", "portal", "echoing_strikes", "black_hole",
    "summon_swords", "shadow_slash", "arcane_shackle", "gravity_fissure",
    "chain_creeper", "fang_strike", "fang_ward", "firecracker", "gust", "invisibility",
    "lob_creeper", "shield", "spectral_hammer", "summon_horse", "summon_vex", "slow",
    "arrow_volley", "wololo", "throw", "fang_swirl", "scapegoat",
    "blaze_storm", "burning_dash", "fireball", "firebolt", "fire_breath", "magma_bomb",
    "wall_of_fire", "heat_surge", "flaming_strike", "scorch", "flaming_barrage", "fire_arrow", "raise_hell",
    "angel_wings", "blessing_of_life", "cloud_of_regeneration", "fortify", "greater_heal",
    "guiding_bolt", "healing_circle", "heal", "sunbeam", "wisp", "divine_smite", "haste", "cleanse",
    "cone_of_cold", "frost_step", "ice_block", "icicle", "summon_polar_bear", "ray_of_frost", "frostwave",
    "ice_spikes", "ice_tomb", "snowball", "frostbite", "blizzard",
    "ascension", "chain_lightning", "charge", "electrocute", "lightning_bolt", "lightning_lance",
    "shockwave", "thunderstorm", "ball_lightning", "volt_strike",
    "acid_orb", "blight", "poison_arrow", "poison_breath", "poison_splash", "root",
    "spider_aspect", "firefly_swarm", "oakskin", "earthquake", "stomp", "gluttony", "touch_dig",
    "abyssal_shroud", "sculk_tentacles", "sonic_boom", "planar_sight", "telekinesis", "eldritch_blast",
    "pocket_dimension",
}
require(len(expected_spells) == 111, "Iron catalog contract must contain 111 IDs")
profile_blocks = re.findall(
    r'private static final Set<String>\s+[A-Z_]+\s*=\s*Set\.of\((.*?)\);',
    balance,
    flags=re.S,
)
profiled = []
for block in profile_blocks:
    profiled.extend(re.findall(r'"([a-z0-9_]+)"', block))
require(len(profiled) == len(set(profiled)), "A spell appears in more than one profile")
require(set(profiled) == expected_spells,
        f"Profile coverage mismatch; missing={sorted(expected_spells-set(profiled))}, extras={sorted(set(profiled)-expected_spells)}")

# High-risk reclassifications based on real spell shape, not only native damage per hit.
profile_contracts = {
    "flaming_barrage": "MULTIHIT",
    "fang_strike": "MULTIHIT",
    "acupuncture": "SUSTAIN",
    "blood_needles": "SUSTAIN",
    "blood_slash": "SUSTAIN",
    "flaming_strike": "WEAPON_DERIVED",
    "divine_smite": "WEAPON_DERIVED",
    "raise_hell": "WEAPON_DERIVED",
    "wither_skull": "SPAM",
    "icicle": "SPAM",
    "wisp": "SPAM",
}
for spell, profile in profile_contracts.items():
    block_match = re.search(
        rf'private static final Set<String>\s+{profile}\s*=\s*Set\.of\((.*?)\);',
        balance, flags=re.S)
    require(block_match is not None and f'"{spell}"' in block_match.group(1),
            f"{spell} must stay in {profile}")

# Exact high-volume budgets. These values preserve AoE identity because the runtime key is per target.
required_budget_tokens = (
    'Map.entry("flaming_barrage", new DamageBudget(5.4f, 27f, 40))',
    'Map.entry("eldritch_blast", new DamageBudget(4.5f, 27f, 40))',
    'Map.entry("arrow_volley", new DamageBudget(1.5f, 27f, 40))',
    'Map.entry("chain_creeper", new DamageBudget(5f, 24f, 40))',
    'Map.entry("starfall", new DamageBudget(5f, 18f, 20))',
    'Map.entry("sonic_boom", new DamageBudget(MAGE_SINGLE_HIT_CEILING, 0f, 0))',
)
for token in required_budget_tokens:
    require(token in balance, f"Missing target budget: {token}")

# Five Flaming Barrage projectiles can all hit the same boss, but the burst budget remains <= 27.
require(burst_total(5, 50.0, 5.4, 27.0, 40) <= 27.0001,
        "Flaming Barrage can exceed 27 on one target in its initial burst")
# Chain Creeper can have up to eight projectiles/chains; one target remains capped.
require(burst_total(8, 50.0, 5.0, 24.0, 40) <= 24.0001,
        "Chain Creeper can stack too much damage into one target")
# Eldritch Blast and Arrow Volley are the largest recast/projectile risks.
require(burst_total(7, 50.0, 4.5, 27.0, 40) <= 27.0001,
        "Eldritch Blast can exceed its single-target burst budget")
require(burst_total(80, 50.0, 1.5, 27.0, 40) <= 27.0001,
        "Arrow Volley can exceed its single-target burst budget")
require(burst_total(20, 50.0, 5.0, 18.0, 20) <= 18.0001,
        "Starfall/continuous initial bucket is too large")

# True one-hit nukes stay just below the Warrior benchmark.
sonic, _ = apply_budget(100.0, 27.0, 0.0, 0, (0.0, 0), 0)
require(abs(sonic - 27.0) < 1e-6, "Sonic Boom single hit must cap at 27")
fireball, _ = apply_budget(100.0, 24.0, 0.0, 0, (0.0, 0), 0)
require(abs(fireball - 24.0) < 1e-6, "Fireball single hit should remain below Warrior burst")

# AoE is not globally nerfed: distinct targets have independent buckets.
target_a = burst_total(5, 50.0, 5.4, 27.0, 40)
target_b = burst_total(5, 50.0, 5.4, 27.0, 40)
require(target_a <= 27.0001 and target_b <= 27.0001 and target_a + target_b > 27.0,
        "Per-target budget must preserve AoE value across multiple enemies")

# Leaky bucket refills gradually instead of hard-resetting/spiking.
state = (0.0, 0)
first, state = apply_budget(100.0, 5.4, 27.0, 40, state, 0)
for _ in range(4):
    _, state = apply_budget(100.0, 5.4, 27.0, 40, state, 0)
refilled, state = apply_budget(100.0, 5.4, 27.0, 40, state, 40)
require(refilled > 5.0,
        "Damage budget should substantially refill after one full window")

# Generic profile defaults must never allow a one-hit Iron spell to pass the Mage ceiling.
for token in (
    "case STANDARD -> new DamageBudget(25.5f, 0f, 0)",
    "case BURST -> new DamageBudget(24f, 0f, 0)",
    "case HEAVY_BURST -> new DamageBudget(MAGE_SINGLE_HIT_CEILING, 0f, 0)",
    "case EXTREME -> new DamageBudget(MAGE_SINGLE_HIT_CEILING, 0f, 0)",
    "case UNCLASSIFIED -> new DamageBudget(20f, 24f, 20)",
):
    require(token in balance, f"Unsafe or missing default target budget: {token}")

print("Iron per-target damage budget validation OK")
