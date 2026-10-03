package com.rpgstats.combat;

/** Geometry regressions: removing turn limits or normalization must fail these tests. */
public final class ArcherAimPolicyTests {
    public static void main(String[] args) {
        double[] velocity = ArcherAimPolicy.turn(new double[]{0, 0, 3}, new double[]{1, 0, 10});
        double length = Math.sqrt(velocity[0]*velocity[0]+velocity[1]*velocity[1]+velocity[2]*velocity[2]);
        require(Math.abs(length-3) < 1e-10, "steering changed projectile speed");
        double angle = Math.toDegrees(Math.atan2(velocity[0], velocity[2]));
        require(angle > 1.49 && angle <= 1.5000001, "correction did not respect 1.5 degree limit");
        require(ArcherAimPolicy.inCone(new double[]{0,0,1}, new double[]{.08,0,1}), "valid near-aim target excluded");
        require(!ArcherAimPolicy.inCone(new double[]{0,0,1}, new double[]{.12,0,1}), "target beyond six degrees accepted");
        require(!ArcherAimPolicy.inCone(new double[]{0,0,0}, new double[]{0,0,1}), "zero velocity accepted");
        require(!ArcherAimPolicy.inCone(new double[]{0,0,1}, new double[]{Double.NaN,0,1}), "nonfinite direction accepted");
        double[] opposite = ArcherAimPolicy.turn(new double[]{0,0,3}, new double[]{0,0,-1});
        require(opposite[2] == 3, "opposite target reversed projectile");
        System.out.println("ArcherAimPolicyTests passed");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
