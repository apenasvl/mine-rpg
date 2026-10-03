package com.rpgstats.compat;
/** Native positive weight remains a penalty; negative weight cannot grant free mobility. */
public final class ArmorBonusPolicy {
    public static float weight(float nativeWeight) { return Math.max(0f,nativeWeight); }
    private ArmorBonusPolicy() {}
}
