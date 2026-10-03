package com.rpgstats.compat.irons;

import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.MageState;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Bounded ownership adapter for real Iron summons. Never changes native despawn timers. */
public final class IronsNativeSummons {
    private static final double RANGE = 48.0;
    private static final int TRACK_LIMIT = 32;
    private static final Class<?> SUMMON_TYPE = summonType();
    private static final Method SUMMONER = summonerMethod();

    private static Class<?> summonType() {
        // Installed 3.16.3 uses IMagicSummon; earlier supported Iron versions used MagicSummon.
        for (String name : new String[]{"IMagicSummon", "MagicSummon"}) {
            try { return Class.forName("io.redspace.ironsspellbooks.entity.mobs." + name); }
            catch (ClassNotFoundException ignored) { }
        }
        return null;
    }

    private static Method summonerMethod() {
        if (SUMMON_TYPE == null) return null;
        try { return SUMMON_TYPE.getMethod("getSummoner"); }
        catch (NoSuchMethodException ignored) { return null; }
    }

    private static LivingEntity owner(Entity entity) {
        if (SUMMONER == null || !SUMMON_TYPE.isInstance(entity)) return null;
        try {
            Object owner = SUMMONER.invoke(entity);
            return owner instanceof LivingEntity living ? living : null;
        } catch (ReflectiveOperationException | LinkageError ignored) { return null; }
    }

    private static boolean owned(ServerPlayerEntity player, Entity entity) {
        if (!(entity instanceof MobEntity) || entity instanceof HorseEntity || !entity.isAlive()
                || entity.isRemoved() || entity.getWorld() != player.getWorld()
                || entity.squaredDistanceTo(player) > RANGE * RANGE) return false;
        LivingEntity owner = owner(entity);
        return owner != null && player.getUuid().equals(owner.getUuid());
    }

    public static void sync(ServerPlayerEntity player, PlayerStats stats) {
        MageState state = MageState.get(player.getUuid());
        state.nativeSummons.clear();
        if (stats.clazz != RPGClass.MAGO || SUMMONER == null) return;
        var entities = new ArrayList<>(player.getServerWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(RANGE), entity -> owned(player, entity)));
        entities.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)));
        for (var entity : entities) {
            if (state.nativeSummons.size() >= TRACK_LIMIT) break;
            state.nativeSummons.put(entity.getUuid(), Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        }
        if (state.summonFocusTicks > 0 && state.summonFocusTarget != null) {
            Entity raw = player.getServerWorld().getEntity(state.summonFocusTarget);
            if (raw instanceof LivingEntity target && validTarget(player, target)) applyFocus(player, state, target);
        }
    }

    public static int count(ServerPlayerEntity player, PlayerStats stats) {
        sync(player, stats);
        return MageState.get(player.getUuid()).nativeSummons.size();
    }

    public static float diversityBonus(ServerPlayerEntity player, PlayerStats stats) {
        sync(player, stats);
        int distinct = new HashSet<>(MageState.get(player.getUuid()).nativeSummons.values()).size();
        if (distinct < 2) return 0f;
        float cap = Math.min(0.15f, Math.max(0f,
                AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_diversity_damage")));
        return Math.min(cap, 0.06f * (distinct - 1));
    }

    private static boolean validTarget(ServerPlayerEntity player, LivingEntity target) {
        return target != player && !(target instanceof net.minecraft.entity.player.PlayerEntity)
                && !(target instanceof net.minecraft.entity.passive.PassiveEntity)
                && target.isAlive() && target.getWorld() == player.getWorld()
                && !target.isTeammate(player) && !owned(player, target)
                && target.squaredDistanceTo(player) <= RANGE * RANGE;
    }

    private static void applyFocus(ServerPlayerEntity player, MageState state, LivingEntity target) {
        for (UUID id : state.nativeSummons.keySet()) {
            Entity entity = player.getServerWorld().getEntity(id);
            if (entity instanceof MobEntity mob && owned(player, mob)) mob.setTarget(target);
        }
    }

    public static boolean focus(ServerPlayerEntity player, PlayerStats stats, LivingEntity target) {
        if (!stats.hasNode("mag_conj_focus_order") || target == null || !validTarget(player, target)) return false;
        sync(player, stats);
        MageState state = MageState.get(player.getUuid());
        if (state.nativeSummons.isEmpty()) return false;
        state.summonFocusTarget = target.getUuid();
        state.summonFocusTicks = Math.max(state.summonFocusTicks, 160);
        applyFocus(player, state, target);
        return true;
    }

    /** Transfer supports existing real summons by healing, without fabricating extra lifetime. */
    public static boolean transfer(ServerPlayerEntity player, PlayerStats stats) {
        if (!stats.hasNode("mag_summ_transfer")) return false;
        sync(player, stats);
        boolean supported = false;
        for (UUID id : MageState.get(player.getUuid()).nativeSummons.keySet()) {
            Entity entity = player.getServerWorld().getEntity(id);
            if (entity instanceof LivingEntity living && owned(player, living)) {
                living.heal(living.getMaxHealth() * 0.20f);
                supported = true;
            }
        }
        return supported;
    }

    public static float incomingMultiplier(ServerPlayerEntity player, PlayerStats stats) {
        if (stats.clazz != RPGClass.MAGO) return 1f;
        sync(player, stats);
        MageState state = MageState.get(player.getUuid());
        boolean nearby = false;
        boolean guardian = false;
        for (Map.Entry<UUID, String> entry : state.nativeSummons.entrySet()) {
            Entity entity = player.getServerWorld().getEntity(entry.getKey());
            if (entity != null && owned(player, entity) && entity.squaredDistanceTo(player) <= 144.0) {
                nearby = true;
                if (entry.getValue().endsWith(":summoned_polar_bear")) guardian = true;
            }
        }
        float result = 1f;
        if (nearby) result *= 1f - Math.min(0.05f, Math.max(0f,
                AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_defense")));
        if (guardian) result *= 1f - Math.min(0.08f, Math.max(0f,
                AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_summon_guard")));
        if (state.astralGuardTicks > 0) {
            float guard = Math.min(0.06f, Math.max(0f,
                    AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_astral_defense")));
            if (state.astralFormation == 1) guard *= 5f / 3f;
            result *= 1f - Math.min(0.10f, guard);
        }
        return Math.max(0.80f, result);
    }

    private IronsNativeSummons() { }
}
