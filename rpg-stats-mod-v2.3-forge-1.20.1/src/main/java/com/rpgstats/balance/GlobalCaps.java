package com.rpgstats.balance;

/** Caps finais compartilhados pelo core e por toda integração opcional. */
public final class GlobalCaps {
    public static final float DAMAGE_MULTIPLIER_MAX = 2.25f;
    public static final float CRIT_CHANCE_MAX = 0.25f;
    public static final float LIFESTEAL_MAX = 0.12f;
    public static final float COOLDOWN_REDUCTION_MAX = 0.35f;
    public static final float ATTACK_SPEED_BONUS_MAX = 0.40f;
    public static final float CAST_SPEED_BONUS_MAX = 0.35f;
    public static final float DAMAGE_REDUCTION_MAX = 0.45f;

    public static float damageMultiplier(float value) { return clamp(value, 0.10f, DAMAGE_MULTIPLIER_MAX); }
    public static float physicalDamageMultiplier(float value, int level) {
        return clamp(value, 0.10f, ClassBalance.physicalDamageCeiling(level));
    }
    public static float crit(float value) { return clamp(value, 0f, CRIT_CHANCE_MAX); }
    public static float lifesteal(float value) { return clamp(value, 0f, LIFESTEAL_MAX); }
    public static float cooldownReduction(float value) { return clamp(value, 0f, COOLDOWN_REDUCTION_MAX); }
    public static float attackSpeedBonus(float value) { return clamp(value, 0f, ATTACK_SPEED_BONUS_MAX); }
    public static float castSpeedBonus(float value) { return clamp(value, 0f, CAST_SPEED_BONUS_MAX); }
    public static float damageReduction(float value) { return clamp(value, 0f, DAMAGE_REDUCTION_MAX); }
    public static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private GlobalCaps() {}
}
