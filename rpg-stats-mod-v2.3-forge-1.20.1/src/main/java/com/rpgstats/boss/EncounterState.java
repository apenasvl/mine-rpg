package com.rpgstats.boss;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative state for one active boss encounter.
 *
 * Damage contribution is normalized by the participant's RPG level, while tanking,
 * healing and protection are normalized against health pools instead of raw numbers.
 * Passive grace-period time never counts as activity.
 */
public final class EncounterState {
    /** Only short gaps between real combat events count as active combat time. */
    private static final long MAX_ACTIVE_GAP_TICKS = 5L * 20L;
    private static final double ACTIVE_SECONDS_WEIGHT = 0.05;
    private static final double TANK_HEALTH_FRACTION_WEIGHT = 5.0;
    private static final double HEAL_HEALTH_FRACTION_WEIGHT = 8.0;
    private static final double PROTECTION_HEALTH_FRACTION_WEIGHT = 6.0;
    /** A single support pulse cannot score more than half an ally health bar per component. */
    private static final double MAX_SUPPORT_FRACTION_PER_EVENT = 0.50;

    private final UUID bossId;
    private final int tier;
    private final Map<UUID, Participant> participants = new HashMap<>();

    public EncounterState(UUID bossId, int tier) {
        this.bossId = bossId;
        this.tier = Math.max(1, tier);
    }

    public UUID bossId() {
        return bossId;
    }

    public int tier() {
        return tier;
    }

    public void touch(UUID playerId, int level, long gameTime) {
        Participant participant = participant(playerId);
        if (participant == null) return;
        refresh(participant, level, gameTime);
    }

    /** Record effective damage dealt to the boss, already clamped against overkill by the caller. */
    public void recordDamageDealt(UUID playerId, int level, long gameTime, float damage) {
        Participant participant = participant(playerId);
        if (participant == null) return;
        refresh(participant, level, gameTime);
        double safeDamage = Math.max(0.0, damage);
        participant.rawDamageDealt += safeDamage;
        participant.normalizedDamageDealt += safeDamage / expectedOutputFactor(participant.level);
    }

    /** Record effective damage received from the boss, normalized by this player's current max health. */
    public void recordDamageTaken(UUID playerId, int level, long gameTime, float damage, float playerMaxHealth) {
        Participant participant = participant(playerId);
        if (participant == null) return;
        refresh(participant, level, gameTime);
        double safeDamage = Math.max(0.0, damage);
        double safeMaxHealth = Math.max(1.0, playerMaxHealth);
        participant.rawDamageTaken += safeDamage;
        participant.normalizedTank += Math.min(1.0, safeDamage / safeMaxHealth) * TANK_HEALTH_FRACTION_WEIGHT;
    }

    /**
     * Record support that actually changed an ally: effective healing and newly granted absorption only.
     * Raw heal size is never compared directly between players; it is normalized by the ally's health pool.
     */
    public void recordSupport(UUID playerId, int level, long gameTime, float effectiveHealing,
                              float grantedProtection, float allyMaxHealth) {
        Participant participant = participant(playerId);
        if (participant == null) return;
        double healing = Math.max(0.0, effectiveHealing);
        double protection = Math.max(0.0, grantedProtection);
        if (healing <= 0.0 && protection <= 0.0) return;

        refresh(participant, level, gameTime);
        double safeMaxHealth = Math.max(1.0, allyMaxHealth);
        double healingFraction = Math.min(MAX_SUPPORT_FRACTION_PER_EVENT, healing / safeMaxHealth);
        double protectionFraction = Math.min(MAX_SUPPORT_FRACTION_PER_EVENT, protection / safeMaxHealth);

        participant.rawHealing += healing;
        participant.rawProtection += protection;
        participant.normalizedHealing += healingFraction * HEAL_HEALTH_FRACTION_WEIGHT;
        participant.normalizedProtection += protectionFraction * PROTECTION_HEALTH_FRACTION_WEIGHT;
    }

    public boolean hasParticipant(UUID playerId) {
        return participants.containsKey(playerId);
    }

    public int participantCount() {
        return participants.size();
    }

    public int participantLevel(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0 : participant.level;
    }

    public long lastActiveTick(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? Long.MIN_VALUE : participant.lastActiveTick;
    }

    public double rawDamageDealt(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.rawDamageDealt;
    }

    public double rawDamageTaken(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.rawDamageTaken;
    }

    public double rawHealing(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.rawHealing;
    }

    public double rawProtection(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.rawProtection;
    }

    public double normalizedDamageDealt(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.normalizedDamageDealt;
    }

    public double normalizedTank(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.normalizedTank;
    }

    public double normalizedHealing(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.normalizedHealing;
    }

    public double normalizedProtection(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0.0 : participant.normalizedProtection;
    }

    public long activeCombatTicks(UUID playerId) {
        Participant participant = participants.get(playerId);
        return participant == null ? 0L : participant.activeCombatTicks;
    }

    /**
     * Internal contribution value for later reward eligibility. Damage, tanking and support
     * all use normalized components; raw absolute damage/healing are diagnostics only.
     */
    public double contributionScore(UUID playerId) {
        Participant participant = participants.get(playerId);
        if (participant == null) return 0.0;
        double activeSeconds = participant.activeCombatTicks / 20.0;
        return participant.normalizedDamageDealt
                + participant.normalizedTank
                + participant.normalizedHealing
                + participant.normalizedProtection
                + activeSeconds * ACTIVE_SECONDS_WEIGHT;
    }

    /** Stable copy: nearby players and touch-only participants are never reward candidates. */
    public java.util.Set<UUID> contributors() {
        return participants.keySet().stream().filter(id->Double.isFinite(contributionScore(id)) && contributionScore(id)>0)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public double averageLevel() {
        if (participants.isEmpty()) return 0.0;
        long total = 0L;
        for (Participant participant : participants.values()) total += participant.level;
        return (double) total / participants.size();
    }

    /**
     * Remove only participants that stayed inactive longer than the configured grace period.
     * Grace-period time itself never increases contributionScore.
     */
    public int pruneInactive(long gameTime, long graceTicks) {
        long safeGrace = Math.max(0L, graceTicks);
        int before = participants.size();
        participants.entrySet().removeIf(entry -> {
            long lastActive = entry.getValue().lastActiveTick;
            return gameTime >= lastActive && gameTime - lastActive > safeGrace;
        });
        return before - participants.size();
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    public void removeParticipant(UUID playerId) {
        participants.remove(playerId);
    }

    /**
     * Expected output grows with RPG level. Dividing damage by this factor means a level-10
     * player's smaller hits remain meaningful next to a level-40 player's larger hits.
     * This is deliberately isolated so recommended-boss-level logic can refine it later.
     */
    static double expectedOutputFactor(int level) {
        int safeLevel = Math.max(1, Math.min(50, level));
        return 1.0 + (safeLevel - 1) * 0.10;
    }

    private Participant participant(UUID playerId) {
        if (playerId == null) return null;
        return participants.computeIfAbsent(playerId, ignored -> new Participant());
    }

    private static void refresh(Participant participant, int level, long gameTime) {
        if (participant.seenActivity && gameTime >= participant.lastActiveTick) {
            long gap = gameTime - participant.lastActiveTick;
            participant.activeCombatTicks += Math.min(MAX_ACTIVE_GAP_TICKS, gap);
        }
        participant.level = Math.max(1, Math.min(50, level));
        participant.lastActiveTick = Math.max(participant.lastActiveTick, gameTime);
        participant.seenActivity = true;
    }

    private static final class Participant {
        private int level;
        private long lastActiveTick;
        private boolean seenActivity;
        private long activeCombatTicks;
        private double rawDamageDealt;
        private double rawDamageTaken;
        private double rawHealing;
        private double rawProtection;
        private double normalizedDamageDealt;
        private double normalizedTank;
        private double normalizedHealing;
        private double normalizedProtection;
    }
}

