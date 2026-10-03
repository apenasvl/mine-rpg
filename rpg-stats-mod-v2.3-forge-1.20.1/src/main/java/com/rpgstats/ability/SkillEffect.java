package com.rpgstats.ability;

/**
 * Efeito de combate ligado a um nó da árvore.
 *
 * effectId conhecidos:
 *  melee_damage, ranged_damage, magic_power, lifesteal, damage_reduction,
 *  crit_chance, poison_hit, thorns, attack_speed, move_speed,
 *  resource_on_hit, resource_on_hurt, heal_on_kill,
 *  active_self_heal, active_group_heal, active_aoe, active_power, active_guard,
 *  active_guard_taunt, active_dash, active_arrow_rain, active_smite, active_smoke, active_bloodlust
 */
public record SkillEffect(
        AbilityType type,
        String effectId,
        float value,
        int durationTicks,
        int cooldownTicks,
        float resourceCost
) {
    public static SkillEffect passive(String id, float value) {
        return new SkillEffect(AbilityType.PASSIVE, id, value, 0, 0, 0f);
    }

    public static SkillEffect active(String id, float value, int duration, int cooldown, float cost) {
        return new SkillEffect(AbilityType.ACTIVE, id, value, duration, cooldown, cost);
    }
}
