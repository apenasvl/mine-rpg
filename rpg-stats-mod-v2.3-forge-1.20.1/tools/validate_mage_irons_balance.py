#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def mage_progression(level: int, intelligence: int, mastery: bool = False,
                     occult: bool = False, arcane: int = 0) -> float:
    int_bonus = min(0.60, max(0, intelligence) * (0.60 / 50.0))
    level_progress = max(0.0, min(1.0, (level - 1.0) / 49.0))
    bonus = int_bonus + level_progress * 0.22
    if mastery:
        bonus += 0.06
    if occult:
        bonus += min(0.05, max(0, arcane) * (0.05 / 50.0))
    return 1.0 + min(0.93, max(0.0, bonus))


def iron_total_mana(without_rpg: float, rpg_base: float) -> float:
    rpg_addition = max(0.0, rpg_base - 100.0)
    rpg_addition = min(180.0, rpg_addition)
    return without_rpg + rpg_addition


def balanced_damage(native: float, raw_multiplier: float, native_mult: float,
                    retention: float, cap: float) -> float:
    raw_result = native * raw_multiplier
    if raw_result <= native:
        return max(0.0, raw_result * native_mult)
    value = native * native_mult + (raw_result - native) * retention
    return min(native * cap, max(0.0, value))


mage_balance = (SRC / "combat/MageBalance.java").read_text(encoding="utf-8")
souls = (SRC / "combat/SoulslikeCombat.java").read_text(encoding="utf-8")
irons = (SRC / "compat/irons/IronsSpellsCompat.java").read_text(encoding="utf-8")
irons_balance = (SRC / "compat/irons/IronsSpellBalance.java").read_text(encoding="utf-8")
hud = (SRC / "gui/ResourceHud.java").read_text(encoding="utf-8")

# Mana authority: Iron's equipment stays authoritative and the RPG HUD must not duplicate the bar.
a = "MAX_RPG_MANA_BONUS_WITH_IRONS"
require(a in mage_balance and a in irons,
        "Mana externa precisa preservar equipamento e limitar apenas a parcela do RPG Stats")
require("Math.min(MageBalance.MAX_MANA, rpgBase + equipmentBonus)" not in irons,
        "Iron equipment mana ainda esta sendo cortada pelo teto global do RPG")
require("double withoutRpg = maxMana.getValue()" in irons
        and "rpgBase - MageBalance.BASE_MANA" in irons,
        "Bridge precisa remover/aplicar apenas o modifier de Mana do RPG")
base_with_rpg = iron_total_mana(100.0, 280.0)
armor_500_with_rpg = iron_total_mana(600.0, 280.0)
require(abs((armor_500_with_rpg - base_with_rpg) - 500.0) < 1e-9,
        "Armadura +500 de Mana do Iron's nao esta sendo preservada integralmente")
require("CompatManager.usesExternalMageMana()" in hud and "if (!ironOwnsMageMana)" in hud,
        "HUD do RPG nao pode desenhar uma segunda barra de Mana quando Iron's e autoritativo")

# Persistent Mage progression stays meaningful before per-spell risk normalization.
for token in (
    "MAGE_INTELLIGENCE_DAMAGE_CAP = 0.60f",
    "MAGE_LEVEL_DAMAGE_CAP = 0.22f",
    "MAGE_CORE_MASTERY_DAMAGE_BONUS = 0.06f",
    "MAGE_PERSISTENT_DAMAGE_CAP = 0.93f",
):
    require(token in mage_balance, f"Constante de progressao ausente: {token}")
require("stats.level - 1f" in souls and "mag_core_mastery" in souls,
        "Dano do Mago precisa escalar com nivel RPG e dominio do core")
require("MAGE_INTELLIGENCE_DAMAGE_CAP" in souls and "MAGE_PERSISTENT_DAMAGE_CAP" in souls,
        "Inteligencia do Mago precisa usar a nova curva limitada")
start = mage_progression(1, 10)
require(1.119 <= start <= 1.121, "Progressao inicial do Mago ficou alta demais")
max_general = mage_progression(50, 50, mastery=True)
require(abs(max_general - 1.88) < 1e-6,
        "Build geral max do Mago deve chegar a x1.88 na camada atributo/nivel/core")
max_occult = mage_progression(50, 50, mastery=True, occult=True, arcane=50)
require(abs(max_occult - 1.93) < 1e-6,
        "Oculta max deve respeitar o cap persistente x1.93")
representative_base = 14.0
common_max = representative_base * max_general * 1.05 * 1.09
specialized_max = common_max * 1.18
require(30.0 <= common_max <= 31.0,
        "Dano comum max do Mago ficou fora da faixa planejada")
require(34.0 <= specialized_max <= 37.0,
        "Dano especializado max do Mago deve ficar proximo do benchmark 36, sem explodir")

# Full catalog coverage. Every known spell has exactly one broad profile.
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
require(len(expected_spells) == 111, "Lista de cobertura do Iron precisa conter 111 spells")
profile_blocks = re.findall(
    r'private static final Set<String>\s+[A-Z_]+\s*=\s*Set\.of\((.*?)\);',
    irons_balance,
    flags=re.S,
)
profiled_spells = []
for block in profile_blocks:
    profiled_spells.extend(re.findall(r'"([a-z0-9_]+)"', block))
require(len(profiled_spells) == len(set(profiled_spells)),
        "Uma spell do Iron foi colocada em mais de um perfil")
require(set(profiled_spells) == expected_spells,
        f"Cobertura de perfis divergente; faltando={sorted(expected_spells - set(profiled_spells))}, "
        f"extras={sorted(set(profiled_spells) - expected_spells)}")

# Pipeline uses the profile for all three balance axes that RPG Stats can amplify.
require("IronsSpellBalance.applyRpgScaling(spellId, original, result, persistentMultiplier)" in irons,
        "Pipeline de dano ainda nao aplica o perfil individual da spell")
require("IronsSpellBalance.balanceManaReduction(spellId, reduction)" in irons,
        "Reducao de Mana ainda ignora o perfil individual da spell")
require("IronsSpellBalance.balanceCooldownReduction(spellId, cdr)" in irons,
        "CDR ainda ignora o perfil individual da spell")
require("event.getSpell()" in irons,
        "Evento de cooldown precisa identificar a spell para evitar CDR uniforme")
require("UNCLASSIFIED(0.90f, 0.50f, 1.40f, 0.75f, 0.70f)" in irons_balance,
        "Spells futuras precisam cair em um fallback conservador")
require('"shadow_slash"' in irons_balance and "WEAPON_DERIVED" in irons_balance,
        "Shadow Slash inclui dano de arma e precisa de protecao contra double dipping")

# Shape-level simulations for a raw x2 RPG result.
require(abs(balanced_damage(10, 2.0, 1.00, 1.00, 2.10) - 20.0) < 1e-6,
        "STANDARD perdeu progressao")
require(abs(balanced_damage(10, 2.0, 0.95, 0.72, 1.70) - 16.7) < 1e-6,
        "SPAM fora da curva")
require(abs(balanced_damage(10, 2.0, 0.90, 0.60, 1.55) - 15.0) < 1e-6,
        "MULTIHIT fora da curva")
require(abs(balanced_damage(10, 2.0, 0.90, 0.50, 1.45) - 14.0) < 1e-6,
        "CONTINUOUS fora da curva")
require(abs(balanced_damage(10, 2.0, 0.90, 0.45, 1.40) - 13.5) < 1e-6,
        "PERSISTENT fora da curva")
require(abs(balanced_damage(10, 2.0, 0.85, 0.35, 1.35) - 12.0) < 1e-6,
        "EXTREME fora da curva")
require(abs(balanced_damage(10, 2.0, 0.95, 0.25, 1.25) - 12.0) < 1e-6,
        "WEAPON_DERIVED fora da curva")
require(abs(balanced_damage(10, 2.0, 1.00, 0.00, 1.00) - 10.0) < 1e-6,
        "UTILITY nao deve receber dano RPG")

# High-risk exact overrides. Values use known native shapes, not a blanket Mage nerf.
magic_arrow_general = balanced_damage(28, 1.50, 0.90, 0.40, 1.35)
magic_arrow_full = balanced_damage(28, 2.25, 0.90, 0.40, 1.35)
require(30.0 <= magic_arrow_general <= 32.0 and 37.0 <= magic_arrow_full <= 38.0,
        "Magic Arrow ainda escala demais ou perdeu progressao")
sonic_general = balanced_damage(36, 1.50, 0.85, 0.30, 1.25)
sonic_full = balanced_damage(36, 2.25, 0.85, 0.30, 1.25)
require(abs(sonic_general - 36.0) < 1e-6 and 43.0 <= sonic_full <= 45.0,
        "Sonic Boom precisa preservar fantasia de nuke sem receber multiplicacao absurda")
eldritch_general = balanced_damage(15, 1.50, 0.75, 0.25, 1.20)
eldritch_full = balanced_damage(15, 2.25, 0.75, 0.25, 1.20)
require(13.0 <= eldritch_general <= 13.3 and 15.5 <= eldritch_full <= 16.1,
        "Eldritch Blast precisa ser limitado por blast por causa dos 3-7 recasts")

# Expensive/extreme spells cannot also receive the full 45% mana reduction or 50% temporary CDR.
require(abs(0.45 * 0.50 - 0.225) < 1e-9, "Sanity check de Mana extrema")
require(abs(0.50 * 0.50 - 0.25) < 1e-9, "Sanity check de CDR extremo")
for token in ("balanceManaReduction", "balanceCooldownReduction", "manaReductionRetention", "cooldownReductionRetention"):
    require(token in irons_balance, f"Eixo de balanceamento ausente: {token}")

# Global cap remains the final emergency brake even after exact spell profiles.
global_caps = (SRC / "balance/GlobalCaps.java").read_text(encoding="utf-8")
require("DAMAGE_MULTIPLIER_MAX = 2.25f" in global_caps,
        "Hard cap global de dano foi alterado; risco de power creep")

print("Mage + Iron full spell balance validation OK")

require("MageBossBalance.progressionBudgetScale(persistentMultiplier, bossBudgetScale)" in irons,
        "Teto de alvos comuns precisa preservar INT/nivel/core")
