import com.rpgstats.gui.MageCorruptionText;
import java.util.Set;

/** Run with java's javac module; no Minecraft runtime required. */
public final class MageCorruptionTextTest {
    private static final Set<String> ALL = Set.of("mag_occ_forbidden", "mag_occ_temptation",
            "mag_occ_instability", "mag_occ_forbidden_power", "mag_occ_efficiency");

    public static void main(String[] args) {
        expect(0f, ALL, "sem bônus");
        expect(24.99f, ALL, "sem bônus");
        expect(25f, ALL, "DM+4%");
        expect(49.99f, ALL, "DM+4%");
        expect(50f, ALL, "DM+8% M-6%");
        expect(74.99f, ALL, "DM+8% M-6%");
        expect(75f, ALL, "DM+12% M-6% R+8%");
        expect(100f, ALL, "DM+12% M-6% R+8%");
        expect(100f, Set.of(), "sem bônus");
        expect(100f, Set.of("mag_occ_forbidden", "mag_occ_temptation"), "DM+4%");
        expect(100f, Set.of("mag_occ_forbidden", "mag_occ_instability"), "DM+8%");
        expect(100f, Set.of("mag_occ_temptation"), "sem bônus");
        expect(50f, Set.of("mag_occ_efficiency"), "M-6%");
        expect(75f, Set.of("mag_occ_forbidden_power"), "R+8%");
        var pages = MageCorruptionText.guidePages();
        check(pages.size() == 5, "five bounded Codex pages");
        String copy = pages.toString();
        for (String fact : new String[]{"Blood/Eldritch", "1 vez/s", "Máximo: 100", "6s", "perde 1/s",
                "não somam", "-6%", "+8% dano recebido", "-40", "20s", "remove 10", "4s", "Bleed", "INT"}) {
            check(copy.contains(fact), "guide missing " + fact);
        }
        for (var page : pages) {
            check(page.body().length() <= 180, "compact body: " + page.title());
            check(page.title().length() <= 18, "compact heading: " + page.title());
        }
        System.out.println("MageCorruptionTextTest: 14 HUD cases + guide copy checks passed");
    }

    private static void expect(float value, Set<String> nodes, String expected) {
        String actual = MageCorruptionText.hudEffects(value, nodes::contains);
        check(actual.equals(expected), value + " expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
