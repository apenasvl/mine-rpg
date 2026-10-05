package com.rpgstats.compat;

import java.util.Set;

/** Native Better Combat categories, independent of item inheritance and display names. */
public final class WeaponTypePolicy {
    public enum Kind { RAPID, TWO_HANDED, MELEE, UNKNOWN }
    private static final Set<String> RAPID = Set.of("dagger", "soul_knife", "rapier", "sai");
    private static final Set<String> HEAVY = Set.of("battlestaff", "double_axe", "claymore", "glaive", "greataxe", "greathammer", "halberd", "scythe", "spear", "twinblade", "warglaive", "staff");
    private static final Set<String> MELEE = Set.of("sword", "longsword", "katana", "cutlass", "axe", "mace", "hammer", "chakram", "sickle", "trident");
    public static Kind classify(String category, boolean twoHanded) {
        if (twoHanded) return Kind.TWO_HANDED;
        if (RAPID.contains(category)) return Kind.RAPID;
        if (MELEE.contains(category) || HEAVY.contains(category)) return Kind.MELEE;
        return Kind.UNKNOWN;
    }
    /** Used only when Better Combat's own parent resource is absent. */
    public static Boolean defaultTwoHanded(String parent) {
        if (parent == null || !parent.startsWith("bettercombat:")) return null;
        String category = parent.substring("bettercombat:".length());
        if (HEAVY.contains(category)) return true;
        if (RAPID.contains(category) || MELEE.contains(category)) return false;
        return null;
    }
    private WeaponTypePolicy() {}
}
