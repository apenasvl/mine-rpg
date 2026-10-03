package com.rpgstats.combat;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payoffs event-driven das quinze especializacoes do Guerreiro.
 *
 * ClassMechanics continua dono dos Caminhos, custos, gauges e multiplicadores pequenos. Esta
 * camada resolve os verbos que exigem o evento real: interceptar, cumprir ordens, consumir Dor,
 * descarregar runas, encadear Tempestade e coordenar aliados. Todo dano secundario tem ICD/cap e
 * nunca reaplica este handler, preservando o maior hit fisico direto sem criar multi-hit infinito.
 */
@Mod.EventBusSubscriber(modid = RPGStatsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WarriorSpecializationHandler {
    private static final ThreadLocal<Boolean> PROC_GUARD = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, WarlordMark> WARLORD_MARKS = new HashMap<>();

    private record WarlordMark(UUID owner, long expiresAt) {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (PROC_GUARD.get() || ProcDamageQueue.isApplying()
                || event.getEntity().getWorld().isClient || event.getAmount() <= 0f) return;

        if (event.getEntity() instanceof ServerPlayerEntity victim) {
            interceptForIronGuard(event, victim);
            warriorDefended(event, victim);
        }

        LivingEntity target = event.getEntity();
        if (!(event.getSource().getAttacker() instanceof ServerPlayerEntity attacker)) {
            contributeToWarlordMark(null, target);
            return;
        }
        contributeToWarlordMark(attacker, target);
    }

    /**
     * Payoffs ofensivos rodam depois do dano confirmado. Isso deixa ClassMechanics.onHit gerar
     * gauges/timers do mesmo golpe antes da especializacao tentar consumi-los (ex.: Colosso da Dor).
     */
    public static void onConfirmedMeleeHit(ServerPlayerEntity attacker, LivingEntity target, float actualDamage) {
        if (actualDamage <= 0f) return;
        PlayerStats stats = StatsManager.get(attacker);
        if (stats.clazz != RPGClass.GUERREIRO || stats.specialization == null) return;
        CombatState state = CombatState.get(attacker.getUuid());

        switch (stats.specialization) {
            case WARLORD -> warlord(attacker, target, stats, state);
            case JUGGERNAUT -> juggernaut(target, stats, state);
            case BLOOD_REAVER -> bloodReaver(attacker, target, stats, state);
            case PAIN_COLOSSUS -> painColossus(attacker, target, stats, state, actualDamage);
            case BLADEMASTER -> blademaster(attacker, stats, state);
            case DUEL_MASTER -> duelMaster(attacker, target, stats, state);
            case TITAN_MAULER -> titanMauler(target, stats, state, actualDamage);
            case RUNE_KNIGHT -> runeKnight(attacker, target, stats, state);
            case SPELLBREAKER -> spellbreaker(target, stats, state);
            case STORMBLADE -> stormblade(attacker, target, stats, state);
            case TACTICIAN -> completeOffensiveOrder(attacker, stats, state);
            default -> { }
        }
    }

    private static void warriorDefended(LivingHurtEvent event, ServerPlayerEntity player) {
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.GUERREIRO || stats.specialization == null) return;
        CombatState state = CombatState.get(player.getUuid());

        if (stats.specialization == RPGSpecialization.BULWARK
                && has(stats, "war_van_bulw_engine")
                && state.timer("war_bulwark_barrier") > 0
                && state.gauge("war_guard") >= .5f) {
            float spent = Math.min(1f, state.gauge("war_guard"));
            state.setGauge("war_guard", state.gauge("war_guard") - spent, 10f);
            event.setAmount(event.getAmount() * (1f - .06f - spent * .02f));
            if (has(stats, "war_van_bulw_conversion"))
                protectNearby(player, asc(state, RPGSpecialization.BULWARK) ? 7d : 5d, 35);
        }

        if (stats.specialization == RPGSpecialization.DUEL_MASTER
                && has(stats, "war_weap_duel_engine")
                && state.timer("war_parry") > 0) {
            state.startTimer("war_riposte", 70);
            state.addGauge("war_guard", .8f, 10f);
        }

        if (stats.specialization == RPGSpecialization.SPELLBREAKER
                && has(stats, "war_rune_break_engine")
                && (event.getSource().getSource() != event.getSource().getAttacker()
                    || event.getSource().getAttacker() == null)) {
            float gain = has(stats, "war_rune_break_conversion") ? 1.25f : .8f;
            state.addGauge("war_spell_seal", gain, 3f);
            state.startTimer("war_spell_seal", 120);
        }

        if (stats.specialization == RPGSpecialization.TACTICIAN
                && has(stats, "war_cmd_tact_engine")
                && state.timer("war_order_defense") > 0) {
            completeOrder(player, state, 1f);
        }
    }

    /** Warlord: marca ameaca; acertos de aliados convertem coordenacao em Moral. */
    private static void warlord(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_van_lord_engine") || state.timer("internal_war_warlord_mark") > 0) return;
        long expiry = player.getWorld().getTime() + (asc(state, RPGSpecialization.WARLORD) ? 180L : 120L);
        WARLORD_MARKS.put(target.getUuid(), new WarlordMark(player.getUuid(), expiry));
        state.startTimer("internal_war_warlord_mark", 12);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 70, 0));
    }

    private static void contributeToWarlordMark(ServerPlayerEntity attacker, LivingEntity target) {
        WarlordMark mark = WARLORD_MARKS.get(target.getUuid());
        if (mark == null) return;
        if (target.getWorld().getTime() > mark.expiresAt()) {
            WARLORD_MARKS.remove(target.getUuid());
            return;
        }
        MinecraftServer server = target.getWorld().getServer();
        if (server == null) return;
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(mark.owner());
        if (owner == null || attacker == null || !allied(owner, attacker)) return;
        CombatState state = CombatState.get(owner.getUuid());
        if (state.timer("internal_war_warlord_ally") > 0) return;
        state.addGauge("war_morale", attacker == owner ? .2f : .65f, 10f);
        state.startTimer("internal_war_warlord_ally", 8);
        if (StatsManager.get(owner).specialization == RPGSpecialization.WARLORD
                && asc(state, RPGSpecialization.WARLORD) && attacker != owner)
            state.startTimer("war_order_empowered", 35);
    }

    /** Juggernaut: avanco sob pressao vira controle frontal, nao multiplicador permanente. */
    private static void juggernaut(LivingEntity target, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_van_jugg_engine") || state.timer("war_juggernaut") <= 0
                || state.timer("internal_war_juggernaut") > 0) return;
        float inertia = state.gauge("war_inertia");
        if (inertia < 2f) return;
        state.setGauge("war_inertia", Math.max(0f, inertia - 2f), 6f);
        state.startTimer("internal_war_juggernaut", 18);
        int ticks = BossScaler.getTier(target) > 0 ? 18 : 38;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 0));
    }

    /** Saqueador: Sede exige manter o alvo ferido; execucao renova a janela, com cura limitada. */
    private static void bloodReaver(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_bers_reav_engine")) return;
        float thirst = state.gauge("war_thirst");
        if (state.timer("war_blood_reaver") > 0 && thirst >= 2f
                && state.timer("internal_war_blood_heal") <= 0) {
            float heal = Math.min(1.25f, .35f + thirst * .12f);
            if (WarriorSustain.heal(player, heal, WarriorSustain.meleeWeapon(player)) > 0) {
                state.setGauge("war_thirst", Math.max(0f, thirst - .75f), 5f);
                state.startTimer("internal_war_blood_heal", 16);
            }
        }
        float healthRatio = target.getHealth() / Math.max(1f, target.getMaxHealth());
        if (healthRatio <= (BossScaler.getTier(target) > 0 ? .16f : .28f)) {
            state.startTimer("war_blood_reaver", asc(state, RPGSpecialization.BLOOD_REAVER) ? 90 : 55);
            if (has(stats, "war_bers_reav_conversion")) state.addGauge("war_fury_loop", .8f, 10f);
        }
    }

    /** Colosso: Dor so sai em golpe comprometido e possui cap/ICD. */
    private static void painColossus(ServerPlayerEntity player, LivingEntity target, PlayerStats stats,
                                     CombatState state, float baseDamage) {
        if (!has(stats, "war_bers_pain_engine") || state.timer("war_pain_release") <= 0
                || state.timer("internal_war_pain_release") > 0 || baseDamage < 5f) return;
        float pain = Math.min(asc(state, RPGSpecialization.PAIN_COLOSSUS) ? 6f : 4f,
                state.gauge("war_pain"));
        if (pain < 1.5f) return;
        state.setGauge("war_pain", state.gauge("war_pain") - pain, 10f);
        state.startTimer("internal_war_pain_release", 24);
        float release = Math.min(4.5f, 1.0f + pain * .55f);
        procDamageAfterHit(player, target, release);
        int ticks = BossScaler.getTier(target) > 0 ? 16 : 34;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ticks, 0));
    }

    /** Mestre das Laminas: cadencia fecha um finisher de rotacao e reduz uma recarga. */
    private static void blademaster(ServerPlayerEntity player, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_weap_blade_engine") || state.gauge("war_blade_cadence") < 3f
                || state.timer("internal_war_blade_finisher") > 0) return;
        boolean asc = asc(state, RPGSpecialization.BLADEMASTER);
        state.setGauge("war_blade_cadence", asc ? 1f : 0f, 5f);
        state.startTimer("internal_war_blade_finisher", 18);
        state.reduceLongestCooldown(has(stats, "war_weap_blade_conversion") ? .14f : .08f);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, asc ? 45 : 28, 0));
    }

    /** Mestre do Duelo: Riposta pune postura/ritmo; nao e apenas +dano. */
    private static void duelMaster(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_weap_duel_engine") || state.timer("war_riposte") <= 0
                || state.timer("internal_war_duel_riposte") > 0) return;
        state.startTimer("internal_war_duel_riposte", 16);
        int ticks = BossScaler.getTier(target) > 0 ? 16 : 36;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ticks, 0));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 0));
        if (has(stats, "war_weap_duel_conversion")) state.addGauge("war_guard", 1f, 10f);
        if (asc(state, RPGSpecialization.DUEL_MASTER)) state.startTimer("war_parry", 24);
    }

    /** Quebra-Titas: golpes grandes criam uma quebra resistente em elites/bosses. */
    private static void titanMauler(LivingEntity target, PlayerStats stats, CombatState state, float damage) {
        if (!has(stats, "war_weap_titan_engine") || damage < 7f
                || state.timer("internal_war_titan") > 0) return;
        CombatState.ClassTargetState ts = state.classTarget(target.getUuid());
        float threshold = BossScaler.getTier(target) > 0 ? 9f : 6f;
        if (ts.impact < threshold && state.timer("war_titan_impact") <= 0) return;
        state.startTimer("internal_war_titan", 24);
        ts.openingTicks = Math.max(ts.openingTicks,
                asc(state, RPGSpecialization.TITAN_MAULER) ? 55 : 38);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                BossScaler.getTier(target) > 0 ? 18 : 40, 0));
    }

    /** Cavaleiro Runico: consome carga no alvo principal e limita explosao a alvos extras. */
    private static void runeKnight(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_rune_knight_engine") || state.timer("war_runic_strike") <= 0
                || state.timer("internal_war_rune_release") > 0) return;
        float runes = Math.min(3f, state.gauge("war_runes"));
        if (runes < 1f) return;
        state.setGauge("war_runes", state.gauge("war_runes") - runes, 5f);
        state.startTimer("internal_war_rune_release", 16);
        procDamageAfterHit(player, target, Math.min(4f, 1.25f + runes * .75f));

        if (!has(stats, "war_rune_knight_conversion")) return;
        int limit = asc(state, RPGSpecialization.RUNE_KNIGHT) ? 3 : 2;
        float splash = Math.min(2.25f, .65f + runes * .35f);
        int hit = 0;
        for (LivingEntity extra : extras(player, target, 3.5)) {
            procDamage(player, extra, splash);
            if (++hit >= limit) break;
        }
    }

    /** Quebra-Feiticos: defesa indireta armazena Selo; o golpe seguinte interrompe/fragiliza. */
    private static void spellbreaker(LivingEntity target, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_rune_break_engine") || state.gauge("war_spell_seal") < 1f
                || state.timer("internal_war_spellbreak") > 0) return;
        boolean asc = asc(state, RPGSpecialization.SPELLBREAKER);
        state.setGauge("war_spell_seal", asc ? state.gauge("war_spell_seal") - .5f : 0f, 3f);
        state.startTimer("internal_war_spellbreak", 18);
        int ticks = BossScaler.getTier(target) > 0 ? 18 : 44;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ticks, 0));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 0));
    }

    /** Lamina da Tempestade: movimento + golpe gera uma unica corrente curta. */
    private static void stormblade(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_rune_storm_engine") || state.gauge("war_storm_charge") < 3f
                || state.timer("war_storm_moving") <= 0 || state.timer("internal_war_storm_chain") > 0) return;
        state.setGauge("war_storm_charge", state.gauge("war_storm_charge") - 3f, 5f);
        state.startTimer("internal_war_storm_chain", 14);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 32, 0));
        int limit = asc(state, RPGSpecialization.STORMBLADE) ? 2 : 1;
        int hit = 0;
        for (LivingEntity extra : extras(player, target, 5d)) {
            procDamage(player, extra, 2.75f);
            if (++hit >= limit) break;
        }
    }

    private static void completeOffensiveOrder(ServerPlayerEntity player, PlayerStats stats, CombatState state) {
        if (!has(stats, "war_cmd_tact_engine") || state.timer("war_order_offense") <= 0) return;
        completeOrder(player, state, 1f);
    }

    private static void completeOrder(ServerPlayerEntity player, CombatState state, float progress) {
        float total = state.addGauge("war_tactic_progress", progress, 3f);
        if (total < 2.99f || state.timer("internal_war_order_complete") > 0) return;
        state.setGauge("war_tactic_progress", 0f, 3f);
        state.addGauge("war_morale", 1.25f, 10f);
        state.startTimer("war_order_empowered", 100);
        state.startTimer("internal_war_order_complete", 20);
        if (asc(state, RPGSpecialization.TACTICIAN)) {
            state.startTimer("war_order_offense", 45);
            state.startTimer("war_order_defense", 45);
            state.startTimer("war_order_advance", 45);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 45, 0));
        }
    }

    /** Guarda de Ferro: intercepta parcela limitada e paga parte do dano, sem reflexao/loop. */
    private static void interceptForIronGuard(LivingHurtEvent event, ServerPlayerEntity victim) {
        Box area = victim.getBoundingBox().expand(8d);
        ServerPlayerEntity guard = victim.getServerWorld().getEntitiesByClass(ServerPlayerEntity.class, area,
                p -> p != victim && p.isAlive() && allied(p, victim)
                        && StatsManager.get(p).specialization == RPGSpecialization.IRON_GUARD
                        && has(StatsManager.get(p), "war_cmd_guard_engine")
                        && CombatState.get(p.getUuid()).timer("war_iron_guard") > 0
                        && CombatState.get(p.getUuid()).timer("internal_war_intercept") <= 0)
                .stream().min(Comparator.comparingDouble(victim::squaredDistanceTo)).orElse(null);
        if (guard == null) return;

        CombatState state = CombatState.get(guard.getUuid());
        boolean asc = asc(state, RPGSpecialization.IRON_GUARD);
        float intercepted = Math.min(asc ? 5f : 3.5f, event.getAmount() * (asc ? .35f : .25f));
        if (intercepted <= .1f) return;
        event.setAmount(Math.max(0f, event.getAmount() - intercepted));
        state.startTimer("internal_war_intercept", 10);
        state.addGauge("war_guard", Math.min(1.5f, intercepted * .35f), 10f);
        state.startTimer("war_riposte", 50);
        try {
            PROC_GUARD.set(true);
            guard.damage(event.getSource(), intercepted * .65f);
        } finally {
            PROC_GUARD.set(false);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().getWorld().isClient
                || !(event.getSource().getAttacker() instanceof ServerPlayerEntity player)) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.GUERREIRO) return;
        CombatState state = CombatState.get(player.getUuid());
        if (stats.specialization == RPGSpecialization.RAGEBORN
                && has(stats, "war_bers_rage_engine") && state.timer("war_frenzy") > 0
                && asc(state, RPGSpecialization.RAGEBORN)) {
            state.setTimer("war_frenzy", Math.min(120, state.timer("war_frenzy") + 24));
        }
        WARLORD_MARKS.remove(event.getEntity().getUuid());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity player)) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.GUERREIRO || stats.specialization == null) return;
        CombatState state = CombatState.get(player.getUuid());

        if (stats.specialization == RPGSpecialization.RAGEBORN
                && state.timer("war_frenzy") <= 0 && state.timer("war_frenzy_asc_pending") > 0) {
            state.setTimer("war_frenzy_asc_pending", 0);
            state.startTimer("war_exhaustion", 90);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 90, 0));
        }

        if (stats.specialization == RPGSpecialization.TACTICIAN
                && state.timer("war_order_advance") > 0 && player.age % 10 == 0) {
            double moved = Math.abs(player.getX() - state.lastClassX) + Math.abs(player.getZ() - state.lastClassZ);
            if (moved > .08) completeOrder(player, state, 1f);
        }

        if (stats.specialization == RPGSpecialization.BANNER_LORD
                && state.warBannerPlaced && state.timer("war_banner") > 0 && player.age % 20 == 0) {
            pulseBanner(player, stats, state);
        }
    }

    /** Estandarte e uma zona real: sair dela remove a vantagem; reposicionar exige nova ativacao. */
    private static void pulseBanner(ServerPlayerEntity owner, PlayerStats stats, CombatState state) {
        double radius = asc(state, RPGSpecialization.BANNER_LORD) ? 8d : 6d;
        Box area = new Box(state.warBannerX - radius, state.warBannerY - 3, state.warBannerZ - radius,
                state.warBannerX + radius, state.warBannerY + 3, state.warBannerZ + radius);
        for (ServerPlayerEntity ally : owner.getServerWorld().getEntitiesByClass(ServerPlayerEntity.class, area,
                p -> p.isAlive() && allied(owner, p))) {
            double dx = ally.getX() - state.warBannerX;
            double dz = ally.getZ() - state.warBannerZ;
            if (dx * dx + dz * dz > radius * radius) continue;
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 0));
            if (has(stats, "war_cmd_banner_conversion"))
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 30, 0));
        }
        state.addGauge("war_morale", .35f, 10f);
    }

    private static void protectNearby(ServerPlayerEntity owner, double radius, int ticks) {
        Box area = owner.getBoundingBox().expand(radius);
        for (ServerPlayerEntity ally : owner.getServerWorld().getEntitiesByClass(ServerPlayerEntity.class, area,
                p -> p.isAlive() && allied(owner, p)))
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, ticks, 0));
    }

    private static boolean asc(CombatState state, RPGSpecialization spec) {
        return state.timer("spec_" + spec.name().toLowerCase(java.util.Locale.ROOT) + "_asc") > 0;
    }

    private static boolean has(PlayerStats stats, String node) {
        return stats != null && stats.unlockedNodes.contains(node);
    }

    private static boolean allied(ServerPlayerEntity owner, ServerPlayerEntity other) {
        return owner == other || owner.getScoreboardTeam() == null || owner.isTeammate(other);
    }

    private static java.util.List<LivingEntity> extras(ServerPlayerEntity player, LivingEntity original, double radius) {
        return player.getServerWorld().getEntitiesByClass(LivingEntity.class,
                original.getBoundingBox().expand(radius), e -> validExtraTarget(player, original, e))
                .stream().sorted(Comparator.comparingDouble(original::squaredDistanceTo)).toList();
    }

    private static boolean validExtraTarget(ServerPlayerEntity player, LivingEntity original, LivingEntity target) {
        if (!target.isAlive() || target == original || target == player || target instanceof PlayerEntity) return false;
        if (target instanceof PassiveEntity) return false;
        return !(target instanceof TameableEntity tameable) || tameable.getOwner() != player;
    }

    private static void procDamageAfterHit(ServerPlayerEntity player, LivingEntity target, float amount) {
        ProcDamageQueue.queueSameTarget(player, target, amount);
    }

    private static void procDamage(ServerPlayerEntity player, LivingEntity target, float amount) {
        if (amount <= .01f || !target.isAlive()) return;
        try {
            PROC_GUARD.set(true);
            target.damage(player.getDamageSources().indirectMagic(player, player), amount);
        } finally {
            PROC_GUARD.set(false);
        }
    }

    private WarriorSpecializationHandler() {}
}

