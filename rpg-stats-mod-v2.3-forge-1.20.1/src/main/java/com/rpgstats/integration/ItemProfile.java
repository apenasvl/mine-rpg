package com.rpgstats.integration;

import java.util.Set;

public record ItemProfile(Set<String> categories, float weight, int strength, int dexterity,
                          int intelligence, int faith, int arcane, float staminaCost,
                          float attackSpeed, float statusBuildUp) {
    public ItemProfile {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        intelligence = Math.max(intelligence, faith); // Accept old datapacks without an unusable gate.
        faith = 0;
        weight = Math.max(0f, Math.min(100f, weight));
        staminaCost = Math.max(0f, Math.min(100f, staminaCost));
        attackSpeed = Math.max(0.1f, Math.min(3f, attackSpeed));
        statusBuildUp = Math.max(0f, Math.min(100f, statusBuildUp));
    }
}
