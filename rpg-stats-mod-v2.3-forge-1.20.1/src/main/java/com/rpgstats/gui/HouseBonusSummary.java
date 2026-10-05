package com.rpgstats.gui;

import com.rpgstats.stats.Stat;
import com.rpgstats.tree.SkillNode;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.ability.AbilityType;
import com.rpgstats.ability.ClassAbilityRegistry;
import java.util.*;

/** Reads the actual curriculum; never invents attributes or treats a conditional effect as permanent. */
public final class HouseBonusSummary {
    public static Map<Stat,Integer> attributes(List<SkillNode> nodes,Set<String> learned) {
        Map<Stat,Integer> result=new EnumMap<>(Stat.class);
        for(var node:nodes) if(learned==null || learned.contains(node.id()))
            node.bonus().forEach((stat,value)->{if(stat!=Stat.FE && value!=0)result.merge(stat,value,Integer::sum);});
        return Map.copyOf(result);
    }
    public static String attributeText(List<SkillNode> nodes,Set<String> learned) {
        var values=attributes(nodes,learned);List<String> out=new ArrayList<>();
        for(var stat:Stat.activeValues())if(values.containsKey(stat))out.add(stat.display+" +"+values.get(stat));
        return out.isEmpty()?"sem pontos extras de atributo":String.join(" · ",out);
    }
    public static List<String> lines(List<SkillNode> nodes,Set<String> learned) {
        List<String> out=new ArrayList<>();
        out.add("Atributos aprendidos: "+attributeText(nodes,learned));
        out.add("Ao completar a árvore: "+attributeText(nodes,null));
        out.add("Os bônus exigem talentos comprados; condições e limites continuam valendo.");
        for(var node:nodes) {
            boolean active=AbilityRegistry.hasActive(node.id());
            String status=learned.contains(node.id())?"[Aprendido] ":"[Por aprender] ";
            if(!node.description().isBlank())out.add(status+node.name()+": "+node.description());
            for(var effect:AbilityRegistry.get(node.id())) {
                if(effect.type()==AbilityType.ACTIVE)continue;
                String numeric=EffectText.describe(node.id(),effect);
                if(!numeric.isBlank())out.add("Bônus de "+node.name()+": "+numeric);
            }
            if(active)out.add(status+node.name()+": "+AbilityRegistry.activeSummary(node.id()));
        }
        return List.copyOf(out);
    }
    private HouseBonusSummary(){}
}
