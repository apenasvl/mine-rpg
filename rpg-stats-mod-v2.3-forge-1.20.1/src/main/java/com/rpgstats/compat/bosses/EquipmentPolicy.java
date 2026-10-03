package com.rpgstats.compat.bosses;
import com.rpgstats.integration.EquipmentRules;
import java.util.Map;

public final class EquipmentPolicy {
    public enum Reason { DISABLED, LEVEL, CLASS, ATTRIBUTE, ALLOWED }
    public record Eligibility(boolean allowed, Reason reason) {}
    public static Eligibility evaluate(int level,String clazz,Map<String,Integer> attributes,EquipmentRules rules) {
        if(rules==null)return new Eligibility(true,Reason.ALLOWED);
        if(!rules.enabled())return new Eligibility(false,Reason.DISABLED);
        if(level<rules.minLevel())return new Eligibility(false,Reason.LEVEL);
        for(var entry:rules.stats().entrySet())
            if(attributes.getOrDefault(entry.getKey(),0)<entry.getValue())return new Eligibility(false,Reason.ATTRIBUTE);
        return new Eligibility(true,Reason.ALLOWED);
    }
    /** Class is weapon affinity; progression remains an eligibility gate. */
    public static float classDamageFactor(String clazz,java.util.Set<String> preferredClasses) {
        return clazz!=null && preferredClasses.contains(clazz) ? 1f : .625f;
    }
    private EquipmentPolicy() {}
}
