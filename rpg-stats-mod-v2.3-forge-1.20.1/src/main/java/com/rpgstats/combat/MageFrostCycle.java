package com.rpgstats.combat;

/** Resolves purchased Frost talents independently of the native spell engine. */
public final class MageFrostCycle {
    public record Result(boolean fragility, int slowTicks, int slowAmplifier, int freezeTicks) {
        public boolean triggered() { return fragility || slowTicks > 0; }
    }

    public static Result resolve(float frost, boolean fragility, boolean deepFreeze, boolean boss) {
        if (!(frost >= 100f) || (!fragility && !deepFreeze)) return new Result(false, 0, 0, 0);
        if (!deepFreeze) return new Result(fragility, 0, 0, 0);
        return new Result(fragility, boss ? 60 : 25, boss ? 0 : 9, boss ? 0 : 25);
    }

    private MageFrostCycle() { }
}
