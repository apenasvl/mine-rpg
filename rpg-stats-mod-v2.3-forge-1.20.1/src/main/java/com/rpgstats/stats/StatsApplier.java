package com.rpgstats.stats;

import com.rpgstats.ability.AbilityRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

/** Aplica atributos e passivas que mapeiam diretamente para atributos vanilla. */
public class StatsApplier {
    private static final UUID MOD_STR = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789001");
    /** UUID legado: aplicado com zero para remover o antigo bonus de movimento da Agilidade. */
    private static final UUID MOD_LEGACY_AGI = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789002");
    private static final UUID MOD_LEVEL_HEALTH = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789008");
    private static final UUID MOD_VIT = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789003");
    private static final UUID MOD_DEX = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789004");
    private static final UUID MOD_PASSIVE_MOVE = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789005");
    private static final UUID MOD_PASSIVE_ATTACK = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789006");
    private static final UUID MOD_ARCANE_LUCK = UUID.fromString("3a1b8c20-1111-4c2a-9f00-123456789007");

    private static final UUID MOD_ROLE_MOVE = UUID.fromString("83201b50-1fe8-4f25-9bdf-62a8e5e58d10");
    public static final UUID MOD_ARCHER_CADENCE = UUID.fromString("83201b50-1fe8-4f25-9bdf-62a8e5e58d11");
    private static final UUID MOD_ASSASSIN_CADENCE = UUID.fromString("83201b50-1fe8-4f25-9bdf-62a8e5e58d12");

    public static final double DEX_SPEED_PER_POINT = 0.010;
    public static final double ARCANE_LUCK_PER_POINT = 0.020;

    public static void apply(ServerPlayerEntity player) {
        PlayerStats ps = StatsManager.get(player);
        ps.refreshResourceMax();
        Map<Stat, Integer> totals = ps.totalStats();

        apply(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, MOD_STR,
                strengthDamageBonus(totals.get(Stat.FORCA)), EntityAttributeModifier.Operation.ADDITION);
        apply(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, MOD_LEGACY_AGI,
                0, EntityAttributeModifier.Operation.ADDITION);
        apply(player, EntityAttributes.GENERIC_MAX_HEALTH, MOD_VIT,
                vitalityHealthBonus(totals.get(Stat.VITALIDADE)), EntityAttributeModifier.Operation.ADDITION);
        // Visible maximum-health progression; boss damage never reads the victim's level.
        apply(player, EntityAttributes.GENERIC_MAX_HEALTH, MOD_LEVEL_HEALTH,
                Math.max(0, Math.min(50, ps.level) - 20) * .6, EntityAttributeModifier.Operation.ADDITION);
        apply(player, EntityAttributes.GENERIC_ATTACK_SPEED, MOD_DEX,
                dexteritySpeedBonus(totals.get(Stat.DESTREZA)), EntityAttributeModifier.Operation.ADDITION);
        apply(player, EntityAttributes.GENERIC_LUCK, MOD_ARCANE_LUCK,
                Math.min(StatsManager.MAX_STAT, totals.get(Stat.ARCANO)) * ARCANE_LUCK_PER_POINT,
                EntityAttributeModifier.Operation.ADDITION);

        // Esses efeitos existiam no registry do Grok, mas nao eram consumidos.
        apply(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, MOD_PASSIVE_MOVE,
                Math.min(0.25, AbilityRegistry.sumPassive(ps.unlockedNodes, "move_speed")), EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        apply(player, EntityAttributes.GENERIC_ATTACK_SPEED, MOD_PASSIVE_ATTACK,
                Math.min(0.30, AbilityRegistry.sumPassive(ps.unlockedNodes, "attack_speed")), EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

        boolean archer=ps.clazz==com.rpgstats.classes.RPGClass.ARQUEIRO;
        boolean assassin=ps.clazz==com.rpgstats.classes.RPGClass.ASSASSINO;
        int dexterity=totals.getOrDefault(Stat.DESTREZA,0);
        apply(player,EntityAttributes.GENERIC_MOVEMENT_SPEED,MOD_ROLE_MOVE,
                archer || assassin ? com.rpgstats.balance.ClassBalance.mobileMoveBonus(ps.level,dexterity,assassin) : 0,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        apply(player,EntityAttributes.GENERIC_ATTACK_SPEED,MOD_ARCHER_CADENCE,
                archer ? com.rpgstats.balance.ClassBalance.archerCadenceBonus(ps.level,dexterity) : 0,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        apply(player,EntityAttributes.GENERIC_ATTACK_SPEED,MOD_ASSASSIN_CADENCE,
                assassin ? com.rpgstats.balance.ClassBalance.assassinSpeedBonus(ps.level,dexterity) : 0,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        StatsManager.save(player, ps);
    }

    /** Retornos decrescentes em 20 e 35 evitam que um unico atributo domine a build. */
    public static double strengthDamageBonus(int points) {
        return softCap(points, 0.10, 0.07, 0.04);
    }

    public static double vitalityHealthBonus(int points) {
        return softCap(points, 0.40, 0.25, 0.10);
    }

    public static double dexteritySpeedBonus(int points) {
        return softCap(points, DEX_SPEED_PER_POINT, 0.007, 0.004);
    }

    private static double softCap(int points, double early, double middle, double late) {
        int value = Math.max(0, Math.min(StatsManager.MAX_STAT, points));
        int first = Math.min(value, 20);
        int second = Math.min(Math.max(0, value - 20), 15);
        int third = Math.min(Math.max(0, value - 35), 15);
        return first * early + second * middle + third * late;
    }

    private static void apply(PlayerEntity player, EntityAttribute attribute, UUID id, double amount,
                              EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        if (instance.getModifier(id) != null) instance.removeModifier(id);
        if (amount != 0) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, "Bonus RPGStats", amount, operation));
        }
    }
}

