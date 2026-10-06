package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.stats.*;
import com.rpgstats.combat.SoulslikeCombat;
import java.util.*;

/** Preview uses the same progression functions as combat; excludes weapon and situational damage. */
public final class AttributeDetails {
    private record Metric(String name,double value,String unit){}
    public static List<String> lines(PlayerStats stats,Stat stat) {
        var current=metrics(stats,stat);
        PlayerStats next=PlayerStats.fromNbt(stats.toNbt());
        boolean capped=next.stats.get(stat)>=StatsManager.MAX_STAT;
        if(!capped)next.stats.merge(stat,1,Integer::sum);
        next.refreshResourceMax();var after=metrics(next,stat);
        List<String> out=new ArrayList<>();
        out.add("Atual: "+join(current.stream().limit(2).toList(),null));
        out.add(capped?"Limite de 50 PA neste atributo.":"Próximo PA: "+join(after.stream().limit(2).toList(),current));
        for(int i=2;i<current.size();i++) {
            Metric m=current.get(i);
            out.add(m.name+": "+fmt(m.value)+m.unit+(capped?"":" · próximo PA +"+fmt(after.get(i).value-m.value)+m.unit));
        }
        return List.copyOf(out);
    }
    private static List<Metric> metrics(PlayerStats s,Stat stat) {
        int n=s.totalStats().getOrDefault(stat,0);List<Metric> out=new ArrayList<>();
        switch(stat) {
            case VITALIDADE -> out.add(new Metric("Vida extra",StatsApplier.vitalityHealthBonus(n)," HP"));
            case TENACIDADE -> {
                out.add(new Metric("Stamina",PlayerStats.staminaCapacity(n),""));
                out.add(new Metric("Regeneração",PlayerStats.staminaRegenPerSecond(n),"/s"));
                if(s.clazz==RPGClass.GUERREIRO||s.clazz==RPGClass.ARQUEIRO)out.add(new Metric("Recurso máximo",s.resourceMax,""));
            }
            case FORCA -> {
                out.add(new Metric("Dano físico extra",StatsApplier.strengthDamageBonus(n)," HP"));
                if(s.clazz==RPGClass.GUERREIRO)out.add(new Metric("Fúria máxima",s.resourceMax,""));
            }
            case DESTREZA -> {
                out.add(new Metric("Vel. ataque extra",StatsApplier.dexteritySpeedBonus(n),"/s"));
                if(s.clazz==RPGClass.ARQUEIRO)out.add(new Metric("Escala do arco",100*(SoulslikeCombat.rangedAttributeMultiplier(s)-1),"%"));
                if(s.clazz==RPGClass.ASSASSINO)out.add(new Metric("Escala corpo a corpo",100*(SoulslikeCombat.meleeAttributeMultiplier(s)-1),"%"));
                if(s.clazz==RPGClass.ARQUEIRO||s.clazz==RPGClass.ASSASSINO)out.add(new Metric("Recurso máximo",s.resourceMax,""));
            }
            case INTELIGENCIA -> {
                out.add(new Metric("Escala mágica",100*(SoulslikeCombat.magicAttributeMultiplier(s)-1),"%"));
                if(s.clazz==RPGClass.MAGO)out.add(new Metric("Mana máxima",s.resourceMax,""));
            }
            case ARCANO -> {
                out.add(new Metric("Sorte",Math.min(StatsManager.MAX_STAT,n)*StatsApplier.ARCANE_LUCK_PER_POINT,""));
                if(s.clazz==RPGClass.ASSASSINO)out.add(new Metric("Energia máxima",s.resourceMax,""));
                if(s.clazz==RPGClass.ASSASSINO||s.path==RPGPath.MAGE_OCCULT)out.add(new Metric("Escala mágica",100*(SoulslikeCombat.magicAttributeMultiplier(s)-1),"%"));
                if(com.rpgstats.ability.AbilityRegistry.sumPassive(s.unlockedNodes,"poison_hit")>0)
                    out.add(new Metric("Duração do veneno de talento legado",100*(SoulslikeCombat.afflictionDurationMultiplier(s)-1),"%"));
            }
            case FE -> { }
        }
        Stat primary=s.clazz==null?null:s.clazz==RPGClass.GUERREIRO?Stat.FORCA:s.clazz==RPGClass.MAGO?Stat.INTELIGENCIA:Stat.DESTREZA;
        if(stat==primary)out.add(new Metric("Cura ativa",100*(SoulslikeCombat.healingMultiplier(s)-1),"%"));
        return out;
    }
    private static String join(List<Metric> metrics,List<Metric> before) {
        List<String> out=new ArrayList<>();
        for(int i=0;i<metrics.size();i++){var m=metrics.get(i);double value=before==null?m.value:m.value-before.get(i).value;out.add(m.name+" "+(before==null?"":"+")+fmt(value)+m.unit);}
        return String.join(" · ",out);
    }
    private static String fmt(double n){return String.format(Locale.ROOT,"%.2f",Math.abs(n)<.00001?0:n).replaceAll("0+$","").replaceAll("\\.$","");}
    private AttributeDetails(){}
}
