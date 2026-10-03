#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"
DATA = ROOT / "src/main/resources/data/rpgstats"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def level_progress(level: float) -> float:
    return max(0.0, min(1.0, (level - 10.0) / 40.0))


def hp_bonus(level: float) -> float:
    return 0.75 * level_progress(level)


def damage_mult(level: float) -> float:
    return 1.0 + 0.30 * level_progress(level)


def diminishing_party_bonus(count: int, first: float, decay: float, cap: float) -> float:
    capped = max(1, min(8, count))
    extras = capped - 1
    total = 0.0
    increment = first
    for _ in range(extras):
        total += increment
        increment *= decay
    return min(cap, total)


def party_hp_bonus(count: int) -> float:
    return diminishing_party_bonus(count, 0.25, 0.72, 0.85)


def party_damage_bonus(count: int) -> float:
    return diminishing_party_bonus(count, 0.07, 0.70, 0.22)


def expected_output_factor(level: int) -> float:
    safe = max(1, min(50, level))
    return 1.0 + (safe - 1) * 0.10


def normalized_damage(damage: float, level: int) -> float:
    return max(0.0, damage) / expected_output_factor(level)


boss_scaler = (SRC / "boss/BossScaler.java").read_text(encoding="utf-8")
require("SEGUNDA FASE" not in boss_scaler and "FASE FINAL" not in boss_scaler,
        "RPG Stats nao pode criar fases artificiais de boss")
require("Map<UUID, Integer> PHASES" not in boss_scaler,
        "Estado de fases artificiais ainda existe")
require("instanceof HostileEntity" not in boss_scaler,
        "Mobs hostis comuns nao podem entrar no boss scaling apenas por serem hostis")
require("hasBossHint" not in boss_scaler,
        "Boss scaling deve continuar limitado a bosses conhecidos/data-driven antes do Threat Score")
require("EncounterManager.pruneInactive(entity)" in boss_scaler,
        "Bosses ativos precisam validar participantes conhecidos periodicamente")
require("tickCounter % 100" in boss_scaler,
        "Validacao de encounter deve continuar leve, aproximadamente a cada 5 segundos")

# Level scaling continua separado e limitado.
require("MAX_LEVEL_HEALTH_BONUS = 0.75" in boss_scaler,
        "Cap de HP do scaling por nivel medio deve permanecer em +75%")
require("MAX_LEVEL_DAMAGE_BONUS = 0.30" in boss_scaler,
        "Cap de dano do scaling por nivel medio deve permanecer em +30%")
require("LEVEL_SCALING_START = 10.0" in boss_scaler and "LEVEL_SCALING_RANGE = 40.0" in boss_scaler,
        "Curva de level scaling precisa ir de nivel medio 10 ate 50")
require("state.averageLevel()" in boss_scaler,
        "Boss scaling precisa usar o nivel RPG medio dos participantes")

# Party scaling somente por participantes reais, com diminishing returns e caps.
require("state.participantCount()" in boss_scaler,
        "Party scaling precisa usar apenas participantes reais do EncounterState")
require("PARTY_SCALING_CAP = 8" in boss_scaler,
        "Party scaling precisa ter cap de participantes")
require("FIRST_EXTRA_PLAYER_HEALTH_BONUS = 0.25" in boss_scaler and "PARTY_HEALTH_DECAY = 0.72" in boss_scaler,
        "Curva de HP por party deve usar diminishing returns")
require("FIRST_EXTRA_PLAYER_DAMAGE_BONUS = 0.07" in boss_scaler and "PARTY_DAMAGE_DECAY = 0.70" in boss_scaler,
        "Curva de dano por party deve usar diminishing returns")
require("MAX_PARTY_HEALTH_BONUS = 0.85" in boss_scaler and "MAX_PARTY_DAMAGE_BONUS = 0.22" in boss_scaler,
        "Party scaling precisa ter caps proprios")
require("MAX_TOTAL_HEALTH_BONUS = 1.60" in boss_scaler and "MAX_TOTAL_DAMAGE_MULTIPLIER = 1.50f" in boss_scaler,
        "Level + party scaling precisa de cap total")
require("PARTY_HP_MOD" in boss_scaler and "LEVEL_HP_MOD" in boss_scaler and "MULTIPLY_BASE" in boss_scaler,
        "Level e party HP precisam usar modifiers separados e controlados")
require("newMax * ratio" in boss_scaler,
        "Reescala de HP precisa preservar a porcentagem atual de vida")
require("DAMAGE_MULTIPLIERS.getOrDefault" in boss_scaler,
        "Dano de boss deve usar multiplicador server-side do encounter")

# Contratos numericos da curva de nivel.
require(level_progress(1) == 0.0 and level_progress(10) == 0.0,
        "Nivel 1-10 deve permanecer vanilla em solo")
require(abs(level_progress(20) - 0.25) < 1e-9,
        "Nivel medio 20 deve estar em 25% da curva")
require(abs(level_progress(30) - 0.50) < 1e-9,
        "Nivel medio 30 deve estar em 50% da curva")
require(abs(level_progress(40) - 0.75) < 1e-9,
        "Nivel medio 40 deve estar em 75% da curva")
require(level_progress(50) == 1.0 and level_progress(80) == 1.0,
        "Scaling de nivel deve ter cap no equivalente ao nivel medio 50")
require(abs(hp_bonus(50) - 0.75) < 1e-9 and abs(damage_mult(50) - 1.30) < 1e-9,
        "Caps de nivel devem ser +75% HP e x1.30 dano")

# Contratos numericos da party.
require(party_hp_bonus(1) == 0.0 and party_damage_bonus(1) == 0.0,
        "Solo nao pode receber bonus de party")
require(abs(party_hp_bonus(2) - 0.25) < 1e-9 and abs(party_damage_bonus(2) - 0.07) < 1e-9,
        "Segundo jogador deve aplicar o primeiro incremento configurado")
require(party_hp_bonus(3) > party_hp_bonus(2) and party_hp_bonus(3) - party_hp_bonus(2) < party_hp_bonus(2),
        "HP de party precisa ter diminishing returns")
require(party_damage_bonus(3) > party_damage_bonus(2) and party_damage_bonus(3) - party_damage_bonus(2) < party_damage_bonus(2),
        "Dano de party precisa ter diminishing returns")
require(abs(party_hp_bonus(8) - party_hp_bonus(20)) < 1e-9,
        "Party acima de 8 jogadores nao pode continuar aumentando HP")
require(abs(party_damage_bonus(8) - party_damage_bonus(20)) < 1e-9,
        "Party acima de 8 jogadores nao pode continuar aumentando dano")
require(1.0 + hp_bonus(50) + party_hp_bonus(8) <= 2.60 + 1e-9,
        "Cap combinado de HP ficou alto demais")
require(min(1.50, damage_mult(50) + party_damage_bonus(8)) <= 1.50 + 1e-9,
        "Cap combinado de dano deve permanecer x1.50")

# Phase 2e: contribution nao compara dano bruto entre niveis diferentes.
require(expected_output_factor(10) < expected_output_factor(40),
        "Expected output precisa crescer com o nivel RPG")
low_raw = 3.0
high_raw = 10.0
require(low_raw < high_raw,
        "Cenario de teste precisa representar dano bruto menor do jogador low level")
low_score = normalized_damage(low_raw, 10)
high_score = normalized_damage(high_raw, 40)
require(low_score > 0.0 and high_score > 0.0,
        "Dano real precisa gerar contribuicao positiva")
require(high_score / low_score < 2.0,
        "Normalizacao por nivel ainda esta punindo demais o jogador low level")
require(normalized_damage(3.0, 10) > normalized_damage(3.0, 40),
        "Mesmo dano bruto deve representar mais esforco relativo para o jogador de nivel menor")

quest_api = SRC / "api/QuestXpService.java"
require(not quest_api.exists(), "QuestXpService deve permanecer removido: nao havera XP por quests")
compat_context = (SRC / "compat/CompatContext.java").read_text(encoding="utf-8")
compat_manager = (SRC / "compat/CompatManager.java").read_text(encoding="utf-8")
require("QuestXpService" not in compat_context and "ftb_quests" not in compat_manager and "ftbquests" not in compat_manager,
        "Compatibilidade nao pode reintroduzir ponte de XP por quests")

bosses = json.loads((DATA / "rpgstats/bosses.json").read_text(encoding="utf-8"))
entries = bosses.get("entries", [])
warden = next((e for e in entries if e.get("id") == "minecraft:warden"), None)
require(warden is not None, "Warden precisa de override explicito")
require(int(warden.get("tier", 0)) == 5, "Warden deve ser tier 5/APEX")
require(int(warden.get("xp", 0)) >= 2500, "XP do Warden continua baixo demais")
require(float(warden.get("health_bonus", -1)) == 0.0, "Warden nao deve receber HP fixo artificial")
require(float(warden.get("damage_bonus", -1)) == 0.0, "Warden nao deve receber dano fixo artificial")

for entry in entries:
    require(float(entry.get("health_bonus", 0)) == 0.0,
            "Boss profiles nao devem voltar a aplicar HP fixo independente do encounter")
    require(float(entry.get("damage_bonus", 0)) == 0.0,
            "Boss profiles nao devem voltar a aplicar dano fixo independente do encounter")

legacy_tag = json.loads((DATA / "tags/entity_types/bosses/tier_3.json").read_text(encoding="utf-8"))
require("minecraft:warden" not in legacy_tag.get("values", []),
        "Warden nao pode voltar ao perfil tier_3 antigo")

encounter_state = (SRC / "boss/EncounterState.java").read_text(encoding="utf-8")
encounter_manager = (SRC / "boss/EncounterManager.java").read_text(encoding="utf-8")
forge_events = (SRC / "forge/ForgeEvents.java").read_text(encoding="utf-8")
require("Map<UUID, Participant> participants" in encounter_state,
        "EncounterState precisa manter participantes por UUID")
require("averageLevel()" in encounter_state and "lastActiveTick" in encounter_state,
        "EncounterState precisa guardar nivel medio e atividade")
require("participantCount()" in encounter_state,
        "EncounterState precisa expor quantidade real de participantes")
require("pruneInactive(long gameTime, long graceTicks)" in encounter_state and "isEmpty()" in encounter_state,
        "EncounterState precisa remover participantes inativos de forma controlada")
require("expectedOutputFactor(participant.level)" in encounter_state and "normalizedDamageDealt" in encounter_state,
        "Dano de contribuicao precisa ser normalizado pelo nivel RPG do proprio participante")
require("safeDamage / safeMaxHealth" in encounter_state and "normalizedTank" in encounter_state,
        "Tanking precisa ser normalizado pela vida maxima do proprio jogador")
require("MAX_ACTIVE_GAP_TICKS = 5L * 20L" in encounter_state and "Math.min(MAX_ACTIVE_GAP_TICKS, gap)" in encounter_state,
        "Tempo ativo nao pode crescer durante todo o grace period/AFK")
require("contributionScore(UUID playerId)" in encounter_state,
        "EncounterState precisa expor Contribution Score server-side")
require("PARTICIPANT_GRACE_TICKS = 20L * 20L" in encounter_manager,
        "Grace period de participante deve continuar em 20 segundos")
require("state.pruneInactive(world.getTime(), PARTICIPANT_GRACE_TICKS)" in encounter_manager,
        "EncounterManager precisa aplicar o grace period usando tempo server-side")
require("BossScaler.isCandidate(boss)" in encounter_manager,
        "Encounter nao pode ser criado para mobs comuns")
require("StatsManager.get(player).level" in encounter_manager,
        "Nivel RPG do participante precisa ser capturado server-side")
require("recordDamageDealt" in encounter_manager and "recordDamageTaken" in encounter_manager,
        "EncounterManager precisa separar contribuicao ofensiva e tank")
require("BossScaler.updateEncounterScaling(boss, state)" in encounter_manager,
        "EncounterManager precisa atualizar scaling quando participantes mudarem")
require("BossScaler.clearEncounterScaling(boss)" in encounter_manager,
        "Encounter vazio/removido precisa limpar scaling sem deixar modifier preso")
require("confirmedDamage" in forge_events and "EncounterManager.recordDamageDealt" in forge_events
        and "EncounterManager.recordDamageTaken" in forge_events,
        "Perda confirmada de vida precisa alimentar Contribution Score")
require("Math.max(0,before-self.getHealth())" in (SRC / "mixin/LivingEntityDamageMixin.java").read_text(encoding="utf-8"),
        "Overkill e absorcao nao podem inflar contribuicao")
require("StatsManager.addXpFromKill(killer,entity)" in forge_events,
        "Mortes comuns ainda precisam recompensar o jogador; bosses usam a conclusao do encontro")
require("EncounterManager.removeBoss" in forge_events and "EncounterManager.clear()" in forge_events,
        "EncounterState precisa ser limpo ao remover boss/parar servidor")

print("Progression phase 2e validation OK")

