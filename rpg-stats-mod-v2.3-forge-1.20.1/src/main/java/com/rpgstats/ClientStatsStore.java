package com.rpgstats;

import com.rpgstats.gui.StatsScreen;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import java.util.HashMap;
import java.util.Map;

public class ClientStatsStore {
    public static PlayerStats stats = new PlayerStats();
    public static int archerAffinity;
    private static final Map<String, Integer> cooldowns = new HashMap<>();
    private static final Map<String, Integer> cooldownMax = new HashMap<>();

    public static void clear() {
        stats = new PlayerStats();
        archerAffinity = 0;
        cooldowns.clear();
        cooldownMax.clear();
    }


    public static int cooldown(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return 0;
        return cooldowns.getOrDefault(nodeId, 0);
    }

    public static int cooldownMax(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return 0;
        return cooldownMax.getOrDefault(nodeId, 0);
    }

    /** Interpola os cooldowns entre os pacotes do servidor; o próximo sync corrige qualquer drift. */
    public static void tick() {
        java.util.Iterator<Map.Entry<String, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                cooldownMax.remove(entry.getKey());
                it.remove();
            } else entry.setValue(next);
        }
    }

    private static void readCooldowns(NbtCompound nbt) {
        cooldowns.clear();
        cooldownMax.clear();

        if (nbt.contains("uiCooldowns")) {
            NbtCompound current = nbt.getCompound("uiCooldowns");
            for (String key : current.getKeys()) {
                int value = Math.max(0, current.getInt(key));
                if (value > 0) cooldowns.put(key, value);
            }
        }
        if (nbt.contains("uiCooldownMax")) {
            NbtCompound max = nbt.getCompound("uiCooldownMax");
            for (String key : max.getKeys()) {
                int value = Math.max(0, max.getInt(key));
                if (value > 0) cooldownMax.put(key, value);
            }
        }
    }

    public static void update(NbtCompound nbt) {
        if (nbt == null) return;
        PlayerStats old = stats;
        PlayerStats next = PlayerStats.fromNbt(nbt);
        readCooldowns(nbt);
        archerAffinity = Math.floorMod(nbt.getInt("uiArcherAffinity"),3);
        stats = next;

        boolean rebuild = old.clazz != next.clazz
                || old.subclass != next.subclass
                || old.path != next.path
                || old.specialization != next.specialization
                || old.affinityHouse != next.affinityHouse
                || old.awakened != next.awakened
                || old.level != next.level
                || old.statPoints != next.statPoints
                || old.skillPoints != next.skillPoints
                || !old.stats.equals(next.stats)
                || !old.unlockedNodes.equals(next.unlockedNodes)
                || !java.util.Arrays.equals(old.activeSlots, next.activeSlots);

        if (rebuild && MinecraftClient.getInstance().currentScreen instanceof StatsScreen screen) {
            screen.rebuild();
        }
    }
}

