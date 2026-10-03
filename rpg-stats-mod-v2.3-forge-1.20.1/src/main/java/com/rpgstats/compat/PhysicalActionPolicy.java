package com.rpgstats.compat;
/** Closed map: mobility from unrelated classes and borrowed unknown nodes stays in the core. */
public final class PhysicalActionPolicy {
    public static boolean isRollNode(String id) {
        return "arc_ward_surv_technique".equals(id) || "arc_skirm_guer_technique".equals(id);
    }
    public static boolean canRoll(boolean alive, boolean grounded, boolean wet, boolean mounted, boolean usingItem, boolean flying) {
        return alive && grounded && !wet && !mounted && !usingItem && !flying;
    }
    private PhysicalActionPolicy() {}
}
