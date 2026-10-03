package com.rpgstats.compat.bosses;
import com.rpgstats.integration.FixedBossProfile;

public final class FixedBossPolicy {
    public record Scale(double healthFactor, float damageFactor) {}
    public static Scale scale(FixedBossProfile profile, int participants) {
        int extras = Math.max(1, Math.min(8, participants)) - 1;
        double hp = 0, damage = 0, hpStep = .25, damageStep = .07;
        for (int i = 0; i < extras; i++) {
            hp += hpStep; damage += damageStep;
            hpStep *= .72; damageStep *= .70;
        }
        return new Scale(profile.healthFactor() * (1 + Math.min(.85, hp)),
                (float) (profile.damageFactor() * (1 + Math.min(.22, damage))));
    }
    private FixedBossPolicy() {}
}
