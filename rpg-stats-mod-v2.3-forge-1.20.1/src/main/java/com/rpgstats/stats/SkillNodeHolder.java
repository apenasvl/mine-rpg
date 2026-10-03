package com.rpgstats.stats;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.tree.SkillNode;

import java.util.EnumMap;
import java.util.Map;

class SkillNodeHolder {
    static void addBonus(EnumMap<Stat, Integer> total, PlayerStats stats, String nodeId) {
        if (stats == null || stats.clazz == null) return;
        SkillNode node = stats.clazz.findNode(nodeId);
        if (node == null && stats.subclass != null) node = stats.subclass.findNode(nodeId);
        if (node == null) {
            RPGPath owner = RPGPath.ownerOfNode(nodeId);
            if (owner != null && owner.parent == stats.clazz) node = owner.findNode(nodeId);
        }
        if (node == null) {
            RPGSpecialization owner = RPGSpecialization.ownerOfNode(nodeId);
            if (owner != null && owner.parent.parent == stats.clazz) node = owner.findNode(nodeId);
        }
        add(total, node);
    }

    private static void add(EnumMap<Stat, Integer> total, SkillNode node) {
        if (node == null) return;
        for (Map.Entry<Stat, Integer> entry : node.bonus().entrySet()) total.merge(entry.getKey(), entry.getValue(), Integer::sum);
    }
}
