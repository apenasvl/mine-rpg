package com.rpgstats.tree;

import com.rpgstats.stats.Stat;
import java.util.Map;

/**
 * Um no de progressao. Os campos extras (nivel/tier/crossPath) permitem arvores
 * grandes sem transformar custo de PH em substituto de progressao.
 */
public record SkillNode(
        String id,
        String name,
        String description,
        int cost,
        String prereqNode,
        Stat reqStat,
        int reqValue,
        Map<Stat, Integer> bonus,
        int reqLevel,
        int tier,
        boolean crossPathEligible
) {
    /** Compatibilidade com as arvores antigas das outras classes. */
    public SkillNode(String id, String name, String description, int cost, String prereqNode,
                     Stat reqStat, int reqValue, Map<Stat, Integer> bonus) {
        this(id, name, description, cost, prereqNode, reqStat, reqValue, bonus, 1, 1, false);
    }

    public SkillNode {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("SkillNode sem id");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("SkillNode sem nome");
        description = description == null ? "" : description;
        cost = Math.max(0, cost);
        reqValue = Math.max(0, reqValue);
        bonus = bonus == null ? Map.of() : Map.copyOf(bonus);
        reqLevel = Math.max(1, reqLevel);
        tier = Math.max(1, tier);
    }
}
