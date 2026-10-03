package com.rpgstats.boss;

import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight registry for active boss encounters.
 *
 * Encounters are created by real combat activity, never by scanning all players or
 * every hostile mob. Phase 2f also lets explicit ally healing/protection join an
 * encounter and contribute without using raw damage as the only eligibility signal.
 */
public final class EncounterManager {
    /** 20 seconds. BossScaler validates active bosses every 5 seconds, so expiry is intentionally coarse and cheap. */
    public static final long PARTICIPANT_GRACE_TICKS = 20L * 20L;

    private static final Map<UUID, EncounterState> ACTIVE = new HashMap<>();

    public static void recordParticipation(ServerPlayerEntity player, LivingEntity boss) {
        EncounterState state = stateForCombat(player, boss);
        if (state == null || !(boss.getWorld() instanceof ServerWorld world)) return;
        state.touch(player.getUuid(), StatsManager.get(player).level, world.getTime());
        BossScaler.updateEncounterScaling(boss, state);
    }

    public static void recordDamageDealt(ServerPlayerEntity player, LivingEntity boss, float effectiveDamage) {
        EncounterState state = stateForCombat(player, boss);
        if (state == null || !(boss.getWorld() instanceof ServerWorld world)) return;
        state.recordDamageDealt(player.getUuid(), StatsManager.get(player).level, world.getTime(), effectiveDamage);
        BossScaler.updateEncounterScaling(boss, state);
    }

    public static void recordDamageTaken(ServerPlayerEntity player, LivingEntity boss, float effectiveDamage) {
        EncounterState state = stateForCombat(player, boss);
        if (state == null || !(boss.getWorld() instanceof ServerWorld world)) return;
        state.recordDamageTaken(player.getUuid(), StatsManager.get(player).level, world.getTime(),
                effectiveDamage, player.getMaxHealth());
        BossScaler.updateEncounterScaling(boss, state);
    }

    /**
     * Records real support delivered to an ally who is already fighting one or more bosses.
     * Self-healing is intentionally excluded here because tanking already rewards personal survival.
     * Only encounters that already contain the assisted ally are touched; there is no world/player scan.
     */
    public static int recordSupport(ServerPlayerEntity supporter, ServerPlayerEntity ally,
                                    float effectiveHealing, float grantedProtection) {
        if (supporter == null || ally == null || supporter == ally) return 0;
        if (supporter.getWorld() != ally.getWorld()) return 0;
        if (effectiveHealing <= 0f && grantedProtection <= 0f) return 0;
        if (!(supporter.getWorld() instanceof ServerWorld world)) return 0;

        int level = StatsManager.get(supporter).level;
        int matched = 0;
        for (EncounterState state : ACTIVE.values()) {
            if (!state.hasParticipant(ally.getUuid())) continue;
            state.recordSupport(supporter.getUuid(), level, world.getTime(),
                    effectiveHealing, grantedProtection, ally.getMaxHealth());
            matched++;
        }
        return matched;
    }

    public static Optional<EncounterState> get(UUID bossId) {
        return Optional.ofNullable(ACTIVE.get(bossId));
    }

    public static double contributionScore(UUID bossId, UUID playerId) {
        EncounterState state = ACTIVE.get(bossId);
        return state == null ? 0.0 : state.contributionScore(playerId);
    }

    public static int activeEncounterCount() {
        return ACTIVE.size();
    }

    /**
     * Lightweight periodic validation for one already-tracked boss.
     * It never searches for new players: it only expires participants already known to this encounter.
     */
    public static void pruneInactive(LivingEntity boss) {
        if (boss == null) return;
        EncounterState state = ACTIVE.get(boss.getUuid());
        if (state == null) {
            BossScaler.clearEncounterScaling(boss);
            return;
        }
        if (!(boss.getWorld() instanceof ServerWorld world)) {
            BossScaler.clearEncounterScaling(boss);
            ACTIVE.remove(boss.getUuid());
            return;
        }

        state.pruneInactive(world.getTime(), PARTICIPANT_GRACE_TICKS);
        if (state.isEmpty()) {
            BossScaler.clearEncounterScaling(boss);
            ACTIVE.remove(boss.getUuid());
            return;
        }
        BossScaler.updateEncounterScaling(boss, state);
    }

    /** A fixed encounter XP pool, paid once; damage, tanking and support have equal eligibility. */
    public static void rewardCompletion(LivingEntity boss, ServerPlayerEntity killer) {
        if(!(boss.getWorld() instanceof ServerWorld world))return;
        var data=boss.getPersistentData();
        if(data.getBoolean("rpgstats.bossCompletionPaid"))return;
        data.putBoolean("rpgstats.bossCompletionPaid",true);
        var state=ACTIVE.get(boss.getUuid());
        java.util.Map<UUID,Double> scores=new java.util.HashMap<>();
        var carried=data.getCompound("rpgstats.pairedContributors");
        for(String id:carried.getKeys())try{scores.put(UUID.fromString(id),carried.getDouble(id));}catch(IllegalArgumentException ignored){}
        if(state!=null) {
            state.pruneInactive(world.getTime(),PARTICIPANT_GRACE_TICKS);
            for(var id:state.contributors())scores.merge(id,state.contributionScore(id),Double::sum);
        }
        if(scores.isEmpty() && killer!=null)scores.put(killer.getUuid(),1.0);
        int pool=StatsManager.computeKillXp(boss)+Math.max(0,data.getInt("rpgstats.pairedXp"));
        String type=net.minecraft.registry.Registries.ENTITY_TYPE.getId(boss.getType()).toString();
        var types=new java.util.ArrayList<String>();types.add(type);
        String previous=data.getString("rpgstats.pairedType");if(!previous.isEmpty())types.add(previous);
        var partner=com.rpgstats.compat.bosses.NativeBossPartners.livingPartner(boss);
        if(partner!=null) {
            var target=partner.getPersistentData();var saved=target.getCompound("rpgstats.pairedContributors");
            scores.forEach((id,score)->saved.putDouble(id.toString(),saved.getDouble(id.toString())+score));
            target.put("rpgstats.pairedContributors",saved);target.putInt("rpgstats.pairedXp",pool);target.putString("rpgstats.pairedType",type);
            return;
        }
        java.util.List<ServerPlayerEntity> eligible=scores.keySet().stream()
                .filter(id->Double.isFinite(scores.get(id)) && scores.get(id)>0)
                .sorted(java.util.Comparator.<UUID>comparingDouble(scores::get).reversed().thenComparing(UUID::toString))
                .map(id->world.getServer().getPlayerManager().getPlayer(id))
                .filter(p->p!=null && p.getWorld()==world).limit(8).toList();
        if(eligible.isEmpty())return;
        int each=pool/eligible.size(), extra=pool%eligible.size();
        for(int i=0;i<eligible.size();i++)StatsManager.addBossCompletionXp(eligible.get(i),types,each+(i<extra?1:0));
    }

    public static void removeBoss(LivingEntity boss) {
        if (boss == null) return;
        BossScaler.clearEncounterScaling(boss);
        ACTIVE.remove(boss.getUuid());
    }

    /**
     * Logout is definitive, so an offline player leaves all EncounterStates immediately.
     * The boss attribute update happens on the next lightweight boss validation (at most ~5 seconds).
     */
    public static void removePlayer(UUID playerId) {
        if (playerId == null) return;
        for (EncounterState state : ACTIVE.values()) state.removeParticipant(playerId);
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static EncounterState stateForCombat(ServerPlayerEntity player, LivingEntity boss) {
        if (player == null || boss == null || !(boss.getWorld() instanceof ServerWorld)) return null;
        if (!BossScaler.isCandidate(boss)) return null;

        int tier = BossScaler.getTier(boss);
        if (tier <= 0) return null;
        return ACTIVE.computeIfAbsent(boss.getUuid(), id -> new EncounterState(id, tier));
    }

    private EncounterManager() {}
}

