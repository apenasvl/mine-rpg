#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"
handler = (SRC / "combat/AssassinSpecializationHandler.java").read_text(encoding="utf-8")
mechanics = (SRC / "combat/ClassMechanics.java").read_text(encoding="utf-8")

# Late-game specialist effects must remain gated by the progression tree.
for token in (
    'ass_shadow_night_engine',
    'ass_shadow_night_conversion',
    'ass_duel_counter_engine',
    'ass_duel_counter_conversion',
    'ass_sabo_demo_engine',
    'ass_sabo_demo_conversion',
    'ass_sabo_wire_engine',
    'ass_myst_soul_engine',
    'ass_myst_soul_conversion',
    'ass_myst_void_engine',
    'ass_myst_void_conversion',
):
    assert token in handler, f"missing progression gate: {token}"

# Five flagship Assassin specs must have genuinely different gameplay verbs.
# Nightblade: consume Opening, then escape/vanish; no second free hit.
for token in (
    'nightbladeEscape(player, stats, state)',
    'internal_ass_nightblade_escape',
    'StatusEffects.INVISIBILITY',
    'StatusEffects.SPEED, 42',
):
    assert token in handler, f"Nightblade identity disconnected: {token}"

# Counterblade: Riposte converts successful defense into control + brief resistance.
for token in (
    'state.timer("ass_riposte") <= 0',
    'internal_ass_counterblade',
    'StatusEffects.RESISTANCE, 24',
    'StatusEffects.WEAKNESS, controlTicks',
):
    assert token in handler, f"Counterblade identity disconnected: {token}"

# Demolitionist: bounded stored-damage detonation, then small capped splash/concussion.
assert 'Math.min(4.5f, stored * 0.75f)' in handler
assert 'Math.min(3.5f, stored * 0.60f)' in handler
assert 'Math.min(2.25f, release * 0.45f)' in handler
assert 'extra.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 25, 0))' in handler
assert 'if (++hit >= 3) break;' in handler

# Soulknife: Echo is consumed into a small mystical damage proc, with late sustain conversion.
assert 'Math.min(3.0f, 1.5f + echo * 0.75f)' in handler
assert 'internal_ass_soulknife' in handler
assert 'refundResource(player, stats, .30f + echo * .15f)' in handler

# Voidwalker: Void Debt stays a risk state; the payoff is mobility/control rather than another nuke.
for token in (
    'state.timer("ass_void_debt") <= 0',
    'internal_ass_void_breach',
    'StatusEffects.SLOWNESS, slowTicks',
    'StatusEffects.SPEED, 30',
    'StatusEffects.WEAKNESS, weaknessTicks',
):
    assert token in handler, f"Voidwalker identity disconnected: {token}"

# Wiremaster remains control-oriented, not an unconditional damage multiplier.
assert 'internal_ass_wiremaster' in handler

# Pass 3: Opening is a first-hit state, not a long free-damage window.
for token in (
    'OPENING_HIT_PENDING',
    'state.timer("ass_opening") > 0 || ts.openingTicks > 0',
    'state.setTimer("ass_opening", 0)',
    'state.classTarget(target.getUuid()).openingTicks = 0',
):
    assert token in handler, f"Assassin opening consumption disconnected: {token}"

# Combo is spent only on a genuinely prepared hit. Before the late Risk node its universal payoff
# is resource economy, so early/mid game does not receive another permanent damage multiplier.
for token in (
    'state.timer("ass_riposte") > 0',
    'state.timer("ass_soul_strike") > 0',
    'state.timer("ass_execute_window") > 0',
    'state.timer("ass_void_debt") > 0',
    'combo >= 3f',
    'float spent = Math.min(1.5f, combo)',
    'state.setGauge("ass_combo", combo - spent, 5f)',
    'refundResource(player, stats, spent * .70f)',
    'internal_ass_combo_spend',
):
    assert token in handler, f"Assassin prepared Combo spend disconnected: {token}"

# Duelist completion: every path/spec state introduced by the tree must have a real consumer.
for token in (
    'state.timer("ass_parry_ready") > 0',
    'state.setTimer("ass_parry_ready", 0)',
    'state.timer("class_synergy") > 0',
    'state.timer("ass_duel_synergy") > 0',
    'state.setTimer("ass_duel_synergy", 0)',
    'boolean duelCadenceWasActive = state.timer("ass_duel_cadence") > 0',
    'specHas(stats, "_engine") && !duelCadenceWasActive',
    'state.timer("ass_fencer_tempo") > 0',
    'state.setTimer("ass_fencer_tempo", 0)',
    'state.timer("ass_dance_window") > 0',
    'isDirectMeleeDamage(source)',
):
    assert token in mechanics, f"Duelist completion disconnected: {token}"

assert 'Golpes em até 2,5s mantêm a cadência; parries bem-sucedidos armam Riposta e geram Vantagem' in (
    ROOT / "src/main/java/com/rpgstats/classes/RPGPath.java"
).read_text(encoding="utf-8")

# Completion pass: formerly dead Assassin core/House states must now have real consumers.
for token in (
    'coreHas(stats, "guard") && state.timer("ass_escape") > 0',
    'coreHas(stats, "tempo") && state.timer("class_tempo") > 0',
    'state.startTimer("class_resolve", 60)',
    'pathHas(stats, RPGPath.ASS_SHADOW, "_discipline") ? 1.20f : 1f',
    'consumePathSynergy(state, "ass_shadow_synergy")',
    'ts.doseProgress += gainScale',
    'consumePathSynergy(state, "ass_venom_synergy")',
    'state.timer("ass_venom_ready") > 0',
    'state.startTimer("ass_venom_empowered", ticks + 20)',
    'state.timer("ass_venom_consume") > 0',
    'state.startTimer("ass_device_ready", 90)',
    'consumePathSynergy(state, "ass_sabo_synergy")',
    'state.timer("ass_soul_ready") > 0',
    'consumePathSynergy(state, "ass_myst_synergy")',
):
    assert token in mechanics, f"Assassin completion disconnected: {token}"

# Previously generic Engine fallbacks now have concrete runtime hooks for every Assassin spec.
for token in (
    'Math.round(45 * engine)',
    'state.timer("ass_formula") > 0 ? engine : 1f',
    'state.timer("ass_plague") > 0',
    'state.timer("ass_toxicology") > 0 ? Math.round(45 * engine) : 45',
    'Math.round(50 * engine)',
    'specHas(stats, "_engine") && ts.markTicks > 0 ? .02f : 0f',
):
    assert token in mechanics, f"Assassin Engine completion disconnected: {token}"

# Risk remains a late conditional damage payoff inside the capped ClassMechanics pipeline.
assert 'if (stats.clazz == RPGClass.ASSASSINO && state.gauge("ass_combo") >= 3f) bonus += .035f;' in mechanics
# Do not bypass the main multiplier cap by rewriting LivingHurtEvent amount here.
assert 'event.setAmount' not in handler


# Description audit: Assassin text must describe real triggers, one-hit windows and real durations.
registry = (SRC / "ability/ClassAbilityRegistry.java").read_text(encoding="utf-8")
ability_registry = (SRC / "ability/AbilityRegistry.java").read_text(encoding="utf-8")
paths = (SRC / "classes/RPGPath.java").read_text(encoding="utf-8")
specializations = (SRC / "classes/RPGSpecialization.java").read_text(encoding="utf-8")

for token in (
    'private static String assassinDescription',
    'id.startsWith("ass_shadow_")',
    'id.startsWith("ass_venom_")',
    'id.startsWith("ass_duel_")',
    'id.startsWith("ass_sabo_")',
    'id.startsWith("ass_myst_")',
    'primeiro golpe gera 0,3 Vantagem',
    'acertos dentro de 2,5s geram 0,75',
    'Parry de golpe corpo a corpo reduz 32% do dano',
    'arma Riposta diretamente por 6,75s',
    'case "ass_duel_fence"',
    'case "ass_duel_dance"',
    'case "ass_duel_counter"',
    'case "ass_sabo_demo"',
    'case "ass_sabo_wire"',
    'case "ass_myst_soul"',
    'case "ass_myst_void"',
):
    assert token in registry, f"Assassin description audit missing: {token}"

for dead in ("SEM EFEITO ATUAL", "SEM PAYOFF ATUAL", "sem consumidor adicional", "nao possui consumidor adicional"):
    assert dead not in registry, f"dead Archer/Assassin description survived: {dead}"

for stale in (
    "Golpes diferentes acumulam Doses",
    "Golpes roubam Ecos de Alma",
    "Golpes contra conjuradores roubam recurso limitado",
    "Disfarce, fumaça e objetivo",
):
    assert stale not in paths + specializations, f"stale Assassin description survived: {stale}"

for token in (
    'spec ? (id.startsWith("arc_") ? 100 : 75) : 85',
    'spec ? (id.startsWith("arc_") ? 120 : 125) : 135',
    'active(1f, id.startsWith("arc_") ? 180 : 175, 1200, 32f)',
):
    assert token in registry, f"active duration metadata drifted: {token}"

assert 'ClassAbilityRegistry.activeSummary(real, effect.value())' in ability_registry

print("Assassin distinct specialization validation OK")
