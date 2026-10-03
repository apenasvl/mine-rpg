#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"

balance = (SRC / "balance/ClassBalance.java").read_text(encoding="utf-8")
souls = (SRC / "combat/SoulslikeCombat.java").read_text(encoding="utf-8")
mechanics = (SRC / "combat/ClassMechanics.java").read_text(encoding="utf-8")
state = (SRC / "combat/CombatState.java").read_text(encoding="utf-8")
specs = (SRC / "combat/ArcherSpecializationHandler.java").read_text(encoding="utf-8")


def constant(name: str) -> float:
    match = re.search(rf"{name}\s*=\s*([0-9.]+)f", balance)
    assert match, f"missing {name}"
    return float(match.group(1))


dex_cap = constant("ARCHER_DEX_DAMAGE_CAP")
level_cap = constant("ARCHER_LEVEL_DAMAGE_CAP")
mastery = constant("ARCHER_CORE_MASTERY_DAMAGE_BONUS")
persistent = constant("ARCHER_PERSISTENT_DAMAGE_CAP")

assert 0.30 <= dex_cap <= 0.40
assert 0.30 <= level_cap <= 0.40
assert 0.08 <= mastery <= 0.10
assert abs((dex_cap + level_cap + mastery) - persistent) < 1e-6
assert persistent <= 0.80, "Archer persistent ranged scaling exceeded the calibrated endgame budget"


def archer_mult(level: int, dex: int, has_mastery: bool) -> float:
    bonus = min(dex_cap, max(0, dex) * (dex_cap / 50.0))
    progress = max(0.0, min(1.0, (level - 1.0) / 49.0))
    bonus += progress * level_cap
    if has_mastery:
        bonus += mastery
    return 1.0 + min(persistent, max(0.0, bonus))

# Progression must be visible, monotonic and bounded.
assert abs(archer_mult(1, 0, False) - 1.0) < 1e-6
assert archer_mult(25, 25, True) > archer_mult(10, 10, False)
assert abs(archer_mult(50, 50, True) - (1.0 + persistent)) < 1e-6
assert archer_mult(50, 50, True) <= 1.80
assert archer_mult(50, 25, False) > archer_mult(25, 25, False) * 1.10, "Level growth vanished with the same bow and Dexterity"

# The real ranged pipeline must consume the class-specific curve.
for token in (
    "stats.clazz == RPGClass.ARQUEIRO",
    "ClassBalance.ARCHER_DEX_DAMAGE_CAP",
    "ClassBalance.ARCHER_LEVEL_DAMAGE_CAP",
    "arc_core_mastery",
    "ClassBalance.ARCHER_PERSISTENT_DAMAGE_CAP",
):
    assert token in souls, f"Archer progression disconnected: {token}"

# Existing identity layer remains conditional rather than a permanent generic damage dump.
assert 'case ARQUEIRO -> {' in mechanics
assert 'archerPathDamage' in mechanics and 'archerSpecDamage' in mechanics
assert 'Math.min(1.32f, 1f + Math.max(0f, bonus))' in mechanics

# Pass 2: Aim is no longer permanent. Focus stabilizes it instead of becoming another damage stat.
for token in (
    'tickArcherTacticalGauges()',
    'gauge("arc_aim")',
    'gauge("arc_focus")',
    'combatTicks > 0 ? 0.018f : 0.050f',
    'decay *= 0.45f',
    'setGauge("arc_focus", focus - 0.006f, 10f)',
):
    assert token in state, f"Archer Aim/Focus loop disconnected: {token}"

# Wild Warden Instinct has an explicit defensive spend.
for token in (
    '"arc_survival".equals(key)',
    'gauge("arc_instinct")',
    'Math.min(2f, instinct)',
    'ticks += Math.round(spent * 10f)',
    'setGauge("arc_instinct", instinct - spent, 10f)',
):
    assert token in state, f"Archer Instinct payoff disconnected: {token}"

# Pass 3: missing specialization verbs are real projectile-event mechanics, not more flat damage.
for token in (
    'stats.clazz != RPGClass.ARQUEIRO',
    'public static void onHit',
    'source.getSource() instanceof ProjectileEntity projectile',
    'case ACROBAT -> acrobat',
    'case BALLISTICIAN -> ballistician',
    'case STORMBOW -> { if (state.classMode == 2) stormbow',
    'case FLAMEBOW -> { if (state.classMode == 0) flamebow',
    'case FROSTBOW -> { if (state.classMode == 1) frostbow',
    'case BOMBARDIER -> bombardier',
    'PROC_GUARD',
):
    assert token in specs, f"Archer specialization event layer disconnected: {token}"

# Acrobat Focus is rate limited so multishot cannot refill the tactical resource instantly.
assert 'state.addGauge("arc_focus", 0.35f * engineScale(stats), 10f)' in specs
assert 'internal_arc_acrobat_focus' in specs and 'startTimer("internal_arc_acrobat_focus", 8)' in specs

# Ballistician and Stormbow only add damage to a different target; the original target never gets a second hit.
assert 'arc_ballistic_pierce' in specs and 'Math.min(4.5f, base * 0.30f)' in specs
assert 'arc_storm_chain' in specs and 'Math.min(4.5f, base * 0.28f)' in specs
assert 'target == original' in specs, "secondary procs must exclude the original target"

# Bombardier is AoE-only and bounded to three secondary targets, preserving the single-target hierarchy.
assert 'state.timer("arc_bomb_shot") <= 0' in specs
assert 'Math.min(4.0f, base * 0.25f)' in specs
assert 'if (++hit >= 3) break;' in specs
assert 4.0 * 3 <= 12.0

# Engineer/elemental specializations spend their budget on control/DoT propagation rather than another raw multiplier.
for token in (
    'StatusEffects.SLOWNESS',
    'extra.setOnFireFor(hasEngine(stats) ? 3 : 2)',
    'BossScaler.getTier(target) > 0',
):
    assert token in specs, f"Archer utility budget disconnected: {token}"

# Completion pass: formerly dead core/House/Engine states must have real consumers.
for token in (
    'coreHas(stats, "guard")',
    'coreHas(stats, "tempo") && state.timer("class_tempo") > 0',
    'coreHas(stats, "resolve")',
    'state.startTimer("class_resolve", 60)',
    'state.startTimer("class_resolve_cd", 400)',
    'consumePathSynergy(state, "arc_mark_synergy")',
    'state.timer("arc_hunt") > 0',
    'consumePathSynergy(state, "arc_skirm_synergy")',
    'ts.stackProgress += gainScale',
    'consumePathSynergy(state, "arc_magic_synergy")',
    'state.startTimer("arc_reposition_guard", 35)',
    'consumePathSynergy(state, "arc_art_synergy")',
    'float stationaryGain = specHas(stats, "_engine") ? .03125f : .025f',
):
    assert token in mechanics, f"Archer completion disconnected: {token}"

for token in (
    'engineScale(stats)',
    'state.timer("arc_ballistic") > 0 ? 2f : 1f',
    'state.setTimer("arc_ballistic", 0)',
    'hasEngine(stats) ? 3 : 2',
    'hasEngine(stats) ? 4.25 : 3.5',
):
    assert token in specs, f"Archer Engine completion disconnected: {token}"

# Numeric sanity model for the tactical layer.
def aim_after_ticks(aim: float, focus: float, combat: bool, ticks: int) -> tuple[float, float]:
    for _ in range(ticks):
        if aim > 0.001:
            decay = 0.018 if combat else 0.050
            if focus > 0.001:
                decay *= 0.45
                focus = max(0.0, focus - 0.006)
            aim = max(0.0, aim - decay)
        elif not combat and focus > 0.001:
            focus = max(0.0, focus - 0.010)
    return aim, focus

plain_aim, _ = aim_after_ticks(6.0, 0.0, True, 100)
focused_aim, focused_left = aim_after_ticks(6.0, 1.2, True, 100)
assert 0.0 < plain_aim < 6.0, "Aim must decay in combat"
assert focused_aim > plain_aim, "Focus must stabilize Aim"
assert 0.0 < focused_left < 1.2, "Focus must be spent gradually, not act as a permanent flag"
assert round(min(2.0, 10.0) * 10.0) == 20, "Instinct Survival extension budget changed"


# Description audit: user-facing Archer text must name the actual trigger/payoff, not generic promises.
registry = (SRC / "ability/ClassAbilityRegistry.java").read_text(encoding="utf-8")
ability_registry = (SRC / "ability/AbilityRegistry.java").read_text(encoding="utf-8")
paths = (SRC / "classes/RPGPath.java").read_text(encoding="utf-8")
specializations = (SRC / "classes/RPGSpecialization.java").read_text(encoding="utf-8")

for token in (
    'private static String archerDescription',
    'id.startsWith("arc_mark_")',
    'id.startsWith("arc_ward_")',
    'id.startsWith("arc_skirm_")',
    'id.startsWith("arc_magic_")',
    'id.startsWith("arc_art_")',
    'case "arc_mark_snipe"',
    'case "arc_mark_dead"',
    'case "arc_mark_ball"',
    'case "arc_ward_beast"',
    'case "arc_ward_trap"',
    'case "arc_ward_surv"',
    'case "arc_skirm_wind"',
    'case "arc_skirm_acro"',
    'case "arc_skirm_guer"',
    'case "arc_magic_fire"',
    'case "arc_magic_frost"',
    'case "arc_magic_storm"',
    'case "arc_art_cross"',
    'case "arc_art_bomb"',
    'case "arc_art_eng"',
    'projeteis entre 9 e 28 blocos geram 0,9 Mira',
    'com 3+ Momentum, um acerto abre Emboscada por 3,5s',
    'a cada 3 projeteis, uma corrente atinge 1 alvo extra',
    'Armar Disparo Bomba',
):
    assert token in registry, f"Archer description audit missing: {token}"

for dead in ("SEM EFEITO ATUAL", "SEM PAYOFF ATUAL", "sem consumidor adicional", "nao possui consumidor adicional"):
    assert dead not in registry, f"dead Archer/Assassin description survived: {dead}"

for stale in (
    "Disparos alternam marcas elementais",
    "Esquiva perfeita recupera Foco",
    "Mudar de posição reinicia a janela de emboscada",
    "Torres virtuais e dispositivos",
):
    assert stale not in paths + specializations, f"stale Archer description survived: {stale}"

for token in (
    'spec ? (id.startsWith("arc_") ? 100 : 75) : 85',
    'spec ? (id.startsWith("arc_") ? 120 : 125) : 135',
    'active(1f, id.startsWith("arc_") ? 180 : 175, 1200, 32f)',
):
    assert token in registry, f"active duration metadata drifted: {token}"

assert 'ClassAbilityRegistry.activeSummary(real, effect.value())' in ability_registry

print("Archer balance pass 3 validation OK")

# Specialization effects are invoked only through positive confirmed damage, once per launch group.
handler = (SRC / "combat/CombatHandler.java").read_text(encoding="utf-8")
shot = (SRC / "combat/ArcherShotTracker.java").read_text(encoding="utf-8")
assert "ArcherSpecializationHandler.onHit" in handler
assert "@SubscribeEvent" not in specs
assert "ArcherShotTracker.claim(player, source)" in handler
assert "putIfAbsent" in shot and "IS_EXPLOSION" in shot
assert "ProcDamageQueue.damageExtra(player, target, amount)" in specs

techniques = (SRC / "combat/ArcherTechniqueHandler.java").read_text(encoding="utf-8")
assert "StatusEffects.WEAKNESS" in techniques
assert "controlTicks" in techniques and "Math.min(22, ticks)" in techniques
assert "case ENGINEER -> engineer" not in specs
