package com.rpgstats.integration;

/** Perfil data-driven. Multiplicadores são bônus sobre o valor base. */
public record BossProfile(int tier, int xp, float healthBonus, float damageBonus,
                          float healthPerExtraPlayer, float damagePerExtraPlayer,
                          float statusResistance, float staggerResistance, FixedBossProfile fixed) {
    public BossProfile(int tier, int xp, float healthBonus, float damageBonus,
                       float healthPerExtraPlayer, float damagePerExtraPlayer,
                       float statusResistance, float staggerResistance) {
        this(tier, xp, healthBonus, damageBonus, healthPerExtraPlayer, damagePerExtraPlayer,
                statusResistance, staggerResistance, null);
    }
    public BossProfile {
        tier = Math.max(1, Math.min(5, tier));
        xp = Math.max(1, Math.min(25_000, xp));
        healthBonus = clamp(healthBonus, 0f, 1.50f);
        damageBonus = clamp(damageBonus, 0f, 0.45f);
        healthPerExtraPlayer = clamp(healthPerExtraPlayer, 0f, 0.40f);
        damagePerExtraPlayer = clamp(damagePerExtraPlayer, 0f, 0.08f);
        statusResistance = clamp(statusResistance, 0f, 0.90f);
        staggerResistance = clamp(staggerResistance, 0f, 0.90f);
    }
    private static float clamp(float n, float min, float max) { return Math.max(min, Math.min(max, n)); }
}

