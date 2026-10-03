package com.rpgstats.integration;

/** Fixed native encounter strength; levels describe progression, never scale a victim's damage. */
public record FixedBossProfile(int minLevel, int maxLevel, int referenceLevel,
                               float healthFactor, float damageFactor) {
    public FixedBossProfile {
        if (minLevel < 1 || maxLevel > 50 || minLevel > maxLevel
                || referenceLevel < minLevel || referenceLevel > maxLevel)
            throw new IllegalArgumentException("Invalid boss progression range");
        if (!Float.isFinite(healthFactor) || healthFactor <= 0
                || !Float.isFinite(damageFactor) || damageFactor <= 0)
            throw new IllegalArgumentException("Boss factors must be finite and positive");
    }
}
