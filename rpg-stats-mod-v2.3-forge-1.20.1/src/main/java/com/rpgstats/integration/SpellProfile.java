package com.rpgstats.integration;

import java.util.Set;

public record SpellProfile(Set<String> categories, float baseResourceCost, float powerScale) {
    public SpellProfile {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        baseResourceCost = Math.max(0f, Math.min(500f, baseResourceCost));
        powerScale = Math.max(0.1f, Math.min(2f, powerScale));
    }
    public boolean has(String category) { return categories.contains(category.toLowerCase(java.util.Locale.ROOT)); }
}
