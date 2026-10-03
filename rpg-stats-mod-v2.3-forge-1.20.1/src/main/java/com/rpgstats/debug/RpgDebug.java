package com.rpgstats.debug;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.combat.CombatState;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Debug runtime opt-in para validar mecanicas do RPG dentro do Minecraft.
 * Nao altera dano, recursos ou progressao; apenas observa estado server-side e envia mensagens ao jogador.
 */
@Mod.EventBusSubscriber(modid = RPGStatsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RpgDebug {
    private static final Set<UUID> ENABLED = new java.util.HashSet<>();
    private static final Map<UUID, Snapshot> LAST = new HashMap<>();

    private static final String[] GAUGES = {
            "war_impact", "war_guard", "war_fury_loop", "war_runes", "war_morale", "war_pain", "war_inertia",
            "war_thirst", "war_blade_cadence", "war_storm_charge", "war_spell_seal", "war_tactic_progress",
            "arc_aim", "arc_focus", "arc_instinct", "arc_momentum", "arc_device",
            "ass_combo", "ass_advantage", "ass_echo", "ass_preparation", "ass_dance", "ass_energy_loop", "ass_shadow_prep",
    };

    private static final String[] TIMERS = {
            "war_frenzy", "war_exhaustion", "war_riposte", "war_runic_strike", "war_order_offense",
            "war_order_defense", "war_order_advance", "war_order_empowered", "war_parry", "war_banner",
            "war_bulwark_barrier", "war_juggernaut", "war_pain_release", "war_spell_seal",
            "arc_survival", "arc_prepared_shot", "arc_ambush", "arc_reposition_guard",
            "ass_opening", "ass_riposte", "ass_soul_strike", "ass_execute_window", "ass_void_debt", "ass_escape", "ass_demolition",
            "internal_ass_combo_spend", "internal_ass_nightblade_escape", "internal_ass_counterblade",
            "internal_ass_soulknife", "internal_ass_void_breach", "internal_ass_demo_detonation", "internal_ass_wiremaster",
    };

    public static boolean isEnabled(ServerPlayerEntity player) {
        return player != null && ENABLED.contains(player.getUuid());
    }

    public static void enable(ServerPlayerEntity player) {
        ENABLED.add(player.getUuid());
        LAST.put(player.getUuid(), Snapshot.capture(player));
        player.sendMessage(Text.literal("§a[RPG DEBUG] Ativado. §7Use /rpg debug state para o estado atual."), false);
    }

    public static void disable(ServerPlayerEntity player) {
        ENABLED.remove(player.getUuid());
        LAST.remove(player.getUuid());
        player.sendMessage(Text.literal("§c[RPG DEBUG] Desativado."), false);
    }

    public static void clear(ServerPlayerEntity player) {
        LAST.put(player.getUuid(), Snapshot.capture(player));
        player.sendMessage(Text.literal("§e[RPG DEBUG] Snapshot reiniciado."), false);
    }

    public static void log(ServerPlayerEntity player, String channel, String message) {
        if (!isEnabled(player)) return;
        player.sendMessage(Text.literal("§8[§dRPG DEBUG§8][§b" + channel + "§8] §f" + message), false);
    }

    public static void sendState(ServerPlayerEntity player) {
        PlayerStats stats = StatsManager.get(player);
        CombatState state = CombatState.get(player.getUuid());
        String clazz = stats.clazz == null ? "Nenhuma" : stats.clazz.display;
        String path = stats.path == null ? "-" : stats.path.display;
        String spec = stats.specialization == null ? "-" : stats.specialization.display;

        player.sendMessage(Text.literal("§d===== RPG DEBUG STATE ====="), false);
        player.sendMessage(Text.literal("§fClasse: §e" + clazz + " §7| Caminho: §e" + path + " §7| Spec: §e" + spec), false);
        player.sendMessage(Text.literal("§fNivel: §e" + stats.level
                + " §7| Recurso: §e" + fmt(stats.resource) + "/" + fmt(stats.resourceMax)
                + " §7| Stamina: §e" + fmt(stats.stamina) + "/" + fmt(stats.staminaMax)), false);

        StringBuilder gauges = new StringBuilder();
        for (String key : GAUGES) {
            float value = state.gauge(key);
            if (value <= .001f) continue;
            if (gauges.length() > 0) gauges.append(" §8| §f");
            gauges.append(key).append('=').append(fmt(value));
        }
        player.sendMessage(Text.literal("§bGauges: §f" + (gauges.length() == 0 ? "nenhum ativo" : gauges)), false);

        StringBuilder timers = new StringBuilder();
        for (String key : TIMERS) {
            int value = state.timer(key);
            if (value <= 0) continue;
            if (timers.length() > 0) timers.append(" §8| §f");
            timers.append(key).append('=').append(value).append('t');
        }
        player.sendMessage(Text.literal("§6Timers: §f" + (timers.length() == 0 ? "nenhum ativo" : timers)), false);

        if (state.lastClassTarget != null) {
            CombatState.ClassTargetState ts = state.classTargets.get(state.lastClassTarget);
            if (ts != null) {
                player.sendMessage(Text.literal("§aAlvo atual: §f"
                        + "stored=" + fmt(ts.stored)
                        + " stacks=" + ts.stacks
                        + " doses=" + ts.doses
                        + " opening=" + ts.openingTicks + "t"
                        + " judgment=" + ts.judgment), false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getAttacker() instanceof ServerPlayerEntity attacker && isEnabled(attacker)) {
            String target = event.getEntity().getName().getString();
            log(attacker, "HIT", target + " dano=" + fmt(event.getAmount())
                    + " hp=" + fmt(Math.max(0f, event.getEntity().getHealth() - event.getAmount()))
                    + "/" + fmt(event.getEntity().getMaxHealth()));
        }
        if (event.getEntity() instanceof ServerPlayerEntity player && isEnabled(player)) {
            String attacker = event.getSource().getAttacker() == null
                    ? event.getSource().getName()
                    : event.getSource().getAttacker().getName().getString();
            log(player, "HURT", "recebido=" + fmt(event.getAmount()) + " de=" + attacker);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getAttacker() instanceof ServerPlayerEntity player && isEnabled(player))
            log(player, "KILL", event.getEntity().getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity player) || !isEnabled(player)) return;
        if (player.getWorld().getTime() % 5L != 0L) return;

        Snapshot now = Snapshot.capture(player);
        Snapshot old = LAST.put(player.getUuid(), now);
        if (old == null) return;

        if (Math.abs(now.resource - old.resource) >= .45f)
            log(player, "RESOURCE", fmt(old.resource) + " -> " + fmt(now.resource));

        for (String key : GAUGES) {
            float before = old.gauges.getOrDefault(key, 0f);
            float after = now.gauges.getOrDefault(key, 0f);
            if (Math.abs(after - before) >= .24f)
                log(player, "GAUGE", key + ": " + fmt(before) + " -> " + fmt(after));
        }

        for (String key : TIMERS) {
            int before = old.timers.getOrDefault(key, 0);
            int after = now.timers.getOrDefault(key, 0);
            if (before <= 0 && after > 0) log(player, "TIMER", key + " ON (" + after + "t)");
            else if (before > 0 && after <= 0) log(player, "TIMER", key + " OFF");
        }
    }

    private static String fmt(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private record Snapshot(float resource, Map<String, Float> gauges, Map<String, Integer> timers) {
        static Snapshot capture(ServerPlayerEntity player) {
            PlayerStats stats = StatsManager.get(player);
            CombatState state = CombatState.get(player.getUuid());
            Map<String, Float> gauges = new LinkedHashMap<>();
            Map<String, Integer> timers = new LinkedHashMap<>();
            for (String key : GAUGES) gauges.put(key, state.gauge(key));
            for (String key : TIMERS) timers.put(key, state.timer(key));
            return new Snapshot(stats.resource, gauges, timers);
        }
    }

    private RpgDebug() {}
}
