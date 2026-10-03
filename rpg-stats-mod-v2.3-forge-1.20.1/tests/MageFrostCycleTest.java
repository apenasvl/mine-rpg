import com.rpgstats.combat.MageFrostCycle;

public class MageFrostCycleTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var early = MageFrostCycle.resolve(100f, true, false, true);
        check(early.triggered() && early.fragility(), "Purchased level-30 Fragility must work before Deep Freeze");
        check(early.slowTicks() == 0 && early.freezeTicks() == 0, "Fragility alone must not grant the level-45 control");
        var boss = MageFrostCycle.resolve(100f, true, true, true);
        check(boss.fragility() && boss.slowTicks() == 60 && boss.slowAmplifier() == 0 && boss.freezeTicks() == 0,
                "Bosses must receive bounded slow and Fragility, never hard freeze");
        var mob = MageFrostCycle.resolve(100f, true, true, false);
        check(mob.fragility() && mob.slowTicks() == 25 && mob.slowAmplifier() == 9 && mob.freezeTicks() == 25,
                "Ordinary mobs retain the short purchased freeze");
        check(!MageFrostCycle.resolve(99.9f, true, true, false).triggered(), "Do not resolve below 100 Frost");
        check(!MageFrostCycle.resolve(100f, false, false, false).triggered(), "Unpurchased talents must not trigger");
        var control = MageFrostCycle.resolve(100f, false, true, true);
        check(control.triggered() && !control.fragility(), "Deep Freeze must not grant unpurchased Fragility");
        System.out.println("MageFrostCycleTest: 6 progression and boss-control cases passed");
    }
}
