package com.rpgstats.integration;
import java.util.Map;
import java.util.Set;

/** Eligibility and native damage budget are separate from legacy stat weights. */
public record EquipmentRules(int minLevel, Set<String> classes, Map<String,Integer> stats,
                             float damageFactor, float attackSpeedFactor, boolean enabled) {
    public EquipmentRules(int level,Set<String> classes,Map<String,Integer> stats,float damage,float speed){
        this(level,classes,stats,damage,speed,true);
    }
    private static final Set<String> VALID_CLASSES=Set.of("GUERREIRO","MAGO","ARQUEIRO","ASSASSINO");
    private static final Set<String> VALID_STATS=Set.of("str","dex","int","faith","arc","vig","end");
    public EquipmentRules {
        classes=Set.copyOf(classes);stats=Map.copyOf(stats);
        if(minLevel<1 || minLevel>50 || classes.isEmpty() || !VALID_CLASSES.containsAll(classes))
            throw new IllegalArgumentException("Invalid equipment progression/class");
        if(!VALID_STATS.containsAll(stats.keySet()) || stats.values().stream().anyMatch(x->x<0))
            throw new IllegalArgumentException("Invalid equipment attribute requirement");
        if(stats.containsKey("faith")) {
            var migrated=new java.util.HashMap<>(stats);
            int requirement=migrated.remove("faith");
            if(requirement>0)migrated.merge("int",requirement,Math::max);
            stats=Map.copyOf(migrated);
        }
        if(!Float.isFinite(damageFactor)||damageFactor<=0||!Float.isFinite(attackSpeedFactor)||attackSpeedFactor<=0)
            throw new IllegalArgumentException("Equipment factors must be finite and positive");
    }
}
