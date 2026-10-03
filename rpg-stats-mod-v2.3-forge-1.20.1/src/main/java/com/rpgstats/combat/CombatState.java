package com.rpgstats.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Estado temporario de combate; buffs e cooldowns reiniciam ao morrer/relogar. */
public class CombatState {
    private static final Map<UUID, CombatState> STATES = new HashMap<>();

    /** Estado leve por alvo usado pelos loops das classes nao-Mago. */
    public static final class ClassTargetState {
        public float impact;
        public float stored;
        public int stacks;
        public int doses;
        /** Fractional progress lets Discipline/secondary-House scaling affect integer mechanics deterministically. */
        public float stackProgress;
        public float doseProgress;
        public int judgment;
        public int markTicks;
        public int openingTicks;
        public int statusTicks;

        public boolean active() {
            return impact > 0.01f || stored > 0.01f || stacks > 0 || doses > 0
                    || stackProgress > 0.01f || doseProgress > 0.01f || judgment > 0
                    || markTicks > 0 || openingTicks > 0 || statusTicks > 0;
        }

        private void tick() {
            if (markTicks > 0) markTicks--;
            if (openingTicks > 0) openingTicks--;
            if (statusTicks > 0) statusTicks--;
            if (markTicks == 0) {
                stacks = Math.max(0, stacks - 1);
                if (stacks == 0) stackProgress = 0f;
            }
            if (statusTicks == 0) {
                doses = Math.max(0, doses - 1);
                if (doses == 0) doseProgress = 0f;
            }
            if (openingTicks == 0) judgment = Math.max(0, judgment - 1);
            impact = Math.max(0f, impact - 0.035f);
            stored = Math.max(0f, stored - 0.02f);
        }
    }

    private final Map<String, Integer> cooldowns = new HashMap<>();
    private final Map<String, Integer> cooldownMax = new HashMap<>();

    /** Gauges/timers nomeados permitem estados distintos por classe sem NBT permanente. */
    private final Map<String, Float> classGauges = new HashMap<>();
    private final Map<String, Integer> mechanicTimers = new HashMap<>();
    public final Map<UUID, ClassTargetState> classTargets = new HashMap<>();
    public UUID lastClassTarget;
    public String lastWeaponKey = "";
    public int classMode;
    public double lastClassX;
    public double lastClassY;
    public double lastClassZ;
    /** Posicao server-side do estandarte do Comandante; valida apenas enquanto o timer esta ativo. */
    public double warBannerX;
    public double warBannerY;
    public double warBannerZ;
    public boolean warBannerPlaced;

    public int powerTicks;
    public float powerBonus;
    public int guardTicks;
    public float guardReduction;
    public int bloodlustTicks;
    public float bloodlustBonus;
    public int smokeTicks;
    public int smiteTicks;
    public float smiteMultiplier = 1f;

    /** Estado universal de stamina/exaustao; reinicia ao relogar ou morrer. */
    public int staminaRegenDelayTicks;
    public int exhaustionMessageTicks;
    public int lastAttackStaminaAge = -1;
    public float lastAttackStaminaMultiplier = 1f;

    /** Regen acumulada para evitar escrita NBT a cada tick sem perder regeneracao. */
    public float primaryRegenBuffer;
    public int combatTicks, rotationTicks, preparationTicks, castGuardTicks, resolveTicks, resolveCooldown, comboTicks, comboCooldown;
    public final java.util.Set<String> comboActions = new java.util.HashSet<>();

    public static CombatState get(UUID id) {
        return STATES.computeIfAbsent(id, u -> new CombatState());
    }

    public static void remove(UUID id) {
        STATES.remove(id);
    }

    public float gauge(String key) {
        return classGauges.getOrDefault(key, 0f);
    }

    public void setGauge(String key, float value, float cap) {
        float safe = Math.max(0f, Math.min(Math.max(0f, cap), value));
        if (safe <= 0.001f) classGauges.remove(key);
        else classGauges.put(key, safe);
    }

    public float addGauge(String key, float amount, float cap) {
        setGauge(key, gauge(key) + amount, cap);
        feedAssassinCombo(key, amount);
        return gauge(key);
    }

    /**
     * O Assassino nao recebe dano persistente gratis: executar corretamente Vantagem, Eco,
     * Preparacao, Danca ou uma eliminacao alimenta Combo. Combo e a ponte entre as Casas e as
     * janelas de burst, e nunca e alimentado por Shadow Prep passivo fora de combate.
     */
    private void feedAssassinCombo(String key, float amount) {
        if (amount <= 0f || "ass_combo".equals(key)) return;
        float conversion = switch (key) {
            case "ass_advantage" -> 0.35f;
            case "ass_echo" -> 0.30f;
            case "ass_preparation" -> 0.25f;
            case "ass_dance" -> 0.20f;
            case "ass_energy_loop" -> 0.50f;
            default -> 0f;
        };
        if (conversion > 0f)
            setGauge("ass_combo", gauge("ass_combo") + amount * conversion, 5f);
    }

    public boolean spendGauge(String key, float amount) {
        if (amount <= 0f) return true;
        float current = gauge(key);
        if (current + 0.001f < amount) return false;
        setGauge(key, current - amount, Float.MAX_VALUE);
        return true;
    }

    public int timer(String key) {
        return mechanicTimers.getOrDefault(key, 0);
    }

    public void startTimer(String key, int ticks) {
        if ("arc_survival".equals(key) && ticks > 0) {
            float instinct = gauge("arc_instinct");
            if (instinct > 0.001f) {
                float spent = Math.min(2f, instinct);
                ticks += Math.round(spent * 10f);
                setGauge("arc_instinct", instinct - spent, 10f);
            }
        }
        if ("ass_opening".equals(key) && ticks > 0 && timer("ass_opening") <= 0)
            setGauge("ass_combo", gauge("ass_combo") + 0.55f, 5f);
        if (ticks <= 0) mechanicTimers.remove(key);
        else mechanicTimers.put(key, Math.max(timer(key), ticks));
    }

    public void setTimer(String key, int ticks) {
        if (ticks <= 0) mechanicTimers.remove(key);
        else mechanicTimers.put(key, ticks);
    }

    public ClassTargetState classTarget(UUID id) {
        return classTargets.computeIfAbsent(id, u -> new ClassTargetState());
    }

    public int cooldown(String nodeId) {
        return cooldowns.getOrDefault(nodeId, 0);
    }

    public void startCooldown(String nodeId, int ticks) {
        if (ticks > 0) {
            cooldowns.put(nodeId, ticks);
            cooldownMax.put(nodeId, ticks);
        }
    }

    public int cooldownMax(String nodeId) {
        return cooldownMax.getOrDefault(nodeId, 0);
    }

    public void reduceCooldown(String nodeId, int ticks) {
        if (ticks <= 0) return;
        int current = cooldown(nodeId);
        if (current <= 0) return;
        int next = Math.max(0, current - ticks);
        if (next == 0) {
            cooldowns.remove(nodeId);
            cooldownMax.remove(nodeId);
        } else cooldowns.put(nodeId, next);
    }

    /** Reduz percentualmente o maior cooldown ativo e retorna o id afetado. */
    public String reduceLongestCooldown(float fraction) {
        if (cooldowns.isEmpty() || fraction <= 0f) return "";
        String best = "";
        int max = 0;
        for (Map.Entry<String, Integer> e : cooldowns.entrySet()) {
            if (e.getValue() > max && !e.getKey().startsWith("internal_")) {
                best = e.getKey();
                max = e.getValue();
            }
        }
        if (best.isBlank()) return "";
        int next = Math.max(0, Math.round(max * (1f - Math.min(0.9f, fraction))));
        if (next == 0) {
            cooldowns.remove(best);
            cooldownMax.remove(best);
        } else cooldowns.put(best, next);
        return best;
    }

    public void reduceAllCooldowns(int ticks) {
        if (ticks <= 0) return;
        java.util.ArrayList<String> keys = new java.util.ArrayList<>(cooldowns.keySet());
        for (String key : keys) reduceCooldown(key, ticks);
    }

    private void tickArcherTacticalGauges() {
        float aim = gauge("arc_aim");
        float focus = gauge("arc_focus");
        if (aim > 0.001f) {
            float decay = combatTicks > 0 ? 0.018f : 0.050f;
            if (focus > 0.001f) {
                decay *= 0.45f;
                setGauge("arc_focus", focus - 0.006f, 10f);
            }
            setGauge("arc_aim", aim - decay, 10f);
        } else if (combatTicks == 0 && focus > 0.001f) {
            setGauge("arc_focus", focus - 0.010f, 10f);
        }

        float instinct = gauge("arc_instinct");
        if (combatTicks == 0 && instinct > 0.001f)
            setGauge("arc_instinct", instinct - 0.012f, 10f);
    }

    /**
     * O burst do Assassino deve ser conquistado durante uma sequencia real. Quando sai de combate,
     * Combo e os gauges que o alimentam perdem carga; assim nao e possivel estocar um pico maximo
     * indefinidamente e iniciar todo boss com o mesmo burst pronto.
     */
    private void tickAssassinTacticalGauges() {
        if (combatTicks > 0) return;
        decayGauge("ass_combo", 0.018f, 5f);
        decayGauge("ass_advantage", 0.012f, 5f);
        decayGauge("ass_echo", 0.010f, 6f);
        decayGauge("ass_preparation", 0.014f, 5f);
        decayGauge("ass_dance", 0.015f, 5f);
        decayGauge("ass_energy_loop", 0.025f, 10f);
    }

    /**
     * Recursos do Guerreiro precisam ser conquistados durante a troca. Fora de combate, Furia,
     * Dor, Cadencia, Carga da Tempestade e progresso de Ordem recuam; Guarda e Moral persistem
     * um pouco mais para permitir proteger/reagrupar sem entrar em todo boss com a rotacao cheia.
     */
    private void tickWarriorTacticalGauges() {
        if (combatTicks > 0) return;
        decayGauge("war_fury_loop", 0.020f, 10f);
        decayGauge("war_pain", 0.012f, 10f);
        decayGauge("war_blade_cadence", 0.030f, 5f);
        decayGauge("war_storm_charge", 0.026f, 5f);
        decayGauge("war_tactic_progress", 0.025f, 3f);
        decayGauge("war_thirst", 0.018f, 5f);
        decayGauge("war_inertia", 0.018f, 6f);
        decayGauge("war_guard", 0.006f, 10f);
        decayGauge("war_morale", 0.006f, 10f);
    }

    private void decayGauge(String key, float amount, float cap) {
        float current = gauge(key);
        if (current > 0.001f) setGauge(key, current - amount, cap);
    }

    public void tick() {
        if (combatTicks > 0) combatTicks--;
        if (rotationTicks > 0) rotationTicks--;
        if (preparationTicks > 0) preparationTicks--;
        if (castGuardTicks > 0) castGuardTicks--;
        if (resolveTicks > 0) resolveTicks--;
        if (resolveCooldown > 0) resolveCooldown--;
        if (comboCooldown > 0) comboCooldown--;
        if (comboTicks > 0 && --comboTicks == 0) comboActions.clear();

        tickArcherTacticalGauges();
        tickAssassinTacticalGauges();
        tickWarriorTacticalGauges();

        Iterator<Map.Entry<String, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                cooldownMax.remove(entry.getKey());
                it.remove();
            } else entry.setValue(next);
        }

        Iterator<Map.Entry<String, Integer>> timers = mechanicTimers.entrySet().iterator();
        while (timers.hasNext()) {
            Map.Entry<String, Integer> entry = timers.next();
            int next = entry.getValue() - 1;
            if (next <= 0) timers.remove();
            else entry.setValue(next);
        }
        if (timer("war_banner") <= 0) warBannerPlaced = false;

        Iterator<Map.Entry<UUID, ClassTargetState>> targets = classTargets.entrySet().iterator();
        while (targets.hasNext()) {
            ClassTargetState target = targets.next().getValue();
            target.tick();
            if (!target.active()) targets.remove();
        }

        if (powerTicks > 0 && --powerTicks == 0) powerBonus = 0f;
        if (guardTicks > 0 && --guardTicks == 0) guardReduction = 0f;
        if (bloodlustTicks > 0 && --bloodlustTicks == 0) bloodlustBonus = 0f;
        if (smokeTicks > 0) smokeTicks--;
        if (smiteTicks > 0 && --smiteTicks == 0) smiteMultiplier = 1f;
        if (staminaRegenDelayTicks > 0) staminaRegenDelayTicks--;
        if (exhaustionMessageTicks > 0) exhaustionMessageTicks--;
    }

    public void consumeSmite() {
        smiteTicks = 0;
        smiteMultiplier = 1f;
    }
}
