package com.rpgstats.classes;

import com.rpgstats.stats.Stat;
import com.rpgstats.tree.SkillNode;
import java.util.List;
import java.util.Map;
import static com.rpgstats.stats.Stat.*;

public enum RPGClass {
    GUERREIRO("Guerreiro", "Combate marcial · Furia + Guarda", ClassTrees.core("war", "Fúria", "Guerra")),
    MAGO("Mago", "Magia profunda · Mana + Concentração", MageTrees.core()),
    ARQUEIRO("Arqueiro", "Precisão, distância e posicionamento · Foco", ClassTrees.core("arc", "Foco", "Caça")),
    ASSASSINO("Assassino", "Combos, mobilidade e execução · Energia + Combo", ClassTrees.core("ass", "Energia", "Sombras"));

    public final String display;
    public final String desc;
    public final List<SkillNode> nodes;

    RPGClass(String display, String desc, List<SkillNode> nodes) {
        this.display = display;
        this.desc = desc;
        this.nodes = nodes;
    }

    public SkillNode findNode(String id) {
        for (SkillNode n : nodes) {
            if (n.id().equals(id)) return n;
        }
        return null;
    }

    /** Profundidade real da arvore: raiz=1, filho direto=2. */
    public int depthOf(String id) {
        SkillNode node = findNode(id);
        if (node == null) return Integer.MAX_VALUE;
        int depth = 1;
        String prereq = node.prereqNode();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        while (prereq != null && seen.add(prereq)) {
            depth++;
            SkillNode parent = findNode(prereq);
            if (parent == null) break;
            prereq = parent.prereqNode();
        }
        return depth;
    }

    public String resourceName() {
        return switch (this) {
            case GUERREIRO -> "Furia";
            case MAGO -> "Mana";
            case ARQUEIRO -> "Foco";
            case ASSASSINO -> "Energia";
        };
    }

    private static SkillNode node(String id, String name, String description, int cost, String prereq, Stat reqStat, int reqVal, Map<Stat, Integer> bonus) {
        return new SkillNode(id, name, description, cost, prereq, reqStat, reqVal, bonus);
    }
}
