package com.rpgstats.combat;

/** Small dependency-free angular policy shared by live guidance and geometry regressions. */
public final class ArcherAimPolicy {
    private static final double CONE_COS = Math.cos(Math.toRadians(6));
    private static final double TURN = Math.toRadians(1.5);
    public static boolean inCone(double[] aim, double[] target) {
        double a=length(aim), b=length(target);
        return a>1e-9 && b>1e-9 && Double.isFinite(a) && Double.isFinite(b)
                && dot(aim,target)/(a*b) >= CONE_COS;
    }
    public static double[] turn(double[] velocity, double[] target) {
        double speed=length(velocity), distance=length(target);
        if (!(speed>1e-9 && distance>1e-9 && Double.isFinite(speed) && Double.isFinite(distance))) return velocity.clone();
        double cosine=Math.max(-1,Math.min(1,dot(velocity,target)/(speed*distance)));
        double angle=Math.acos(cosine);
        if (angle<1e-9) return velocity.clone();
        // Antipodal vectors have no unique shortest turn and never constitute an acquired target.
        if (cosine < -.999999) return velocity.clone();
        double fraction=Math.min(1,TURN/angle);
        double a=Math.sin((1-fraction)*angle)/Math.sin(angle), b=Math.sin(fraction*angle)/Math.sin(angle);
        double[] result=new double[3];
        for(int i=0;i<3;i++) result[i]=a*velocity[i]/speed+b*target[i]/distance;
        double scale=speed/length(result); for(int i=0;i<3;i++) result[i]*=scale;
        return result;
    }
    private static double dot(double[] a,double[] b) { return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; }
    private static double length(double[] a) { return Math.sqrt(dot(a,a)); }
    private ArcherAimPolicy() {}
}
