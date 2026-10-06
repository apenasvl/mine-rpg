import com.rpgstats.gui.ClassSelectionState;
import com.rpgstats.gui.ProgressionSelectionState;
import com.rpgstats.gui.ClassSelectionLayout;
import java.util.ArrayList;

public class ClassSelectionTests {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var state = new ClassSelectionState<String>();
        var sent = new ArrayList<String>();
        check(!state.confirm(0, sent::add), "An empty selection must never send a request");
        state.select("MAGO");
        check(sent.isEmpty(), "Browsing classes must not commit the choice");
        state.select("ARQUEIRO");
        check(state.confirm(100, sent::add), "Confirm must send the selected class");
        check(sent.equals(java.util.List.of("ARQUEIRO")), "Confirm must send the most recent selection");
        state.select("GUERREIRO");
        check(!state.confirm(101, sent::add) && sent.size() == 1, "Double clicks must not send duplicate requests");
        check(state.selected().equals("ARQUEIRO"), "Selection must not change during the server request");
        state.tick(8099);
        check(state.pending(), "A request must remain pending before its deadline");
        state.tick(8100);
        check(!state.pending() && state.timedOut(), "A missing server reply must permit a manual retry");
        check(sent.size() == 1, "A timeout must never automatically resend a class choice");
        check(state.confirm(8200, sent::add) && sent.size() == 2, "The player must be able to retry explicitly");
        state.reset();
        check(state.selected() == null && !state.pending(), "A completed class choice must clear transient UI state");
        state.select("MAGO");
        try { state.confirm(9000, value -> { throw new IllegalStateException("offline"); }); }
        catch (IllegalStateException expected) { }
        check(!state.pending(), "A send failure must not leave confirmation permanently disabled");

        var choices=new ProgressionSelectionState<String,String>();
        var spec=choices.forPage("specialization");
        spec.select("JUGGERNAUT");
        var requests=new ArrayList<String>();
        spec.confirm(100,requests::add);
        choices.forPage("affinity").select("BERSERKER");
        var returning=choices.forPage("specialization");
        returning.select("OTHER_SPEC");
        check(returning.pending() && returning.selected().equals("JUGGERNAUT"),"Changing choice pages must retain the in-flight permanent selection");
        check(!returning.confirm(101,requests::add) && requests.size()==1,"Returning to a choice page must not permit duplicate permanent requests");
        returning.tick(8100);
        check(returning.confirm(8101,requests::add),"A retained pending page must still allow explicit retry after timeout");
        check(choices.forPage("affinity").selected().equals("BERSERKER"),"Independent choice previews must remain available");

        check(ClassSelectionLayout.sourceCard(0).height() >= 160, "Illustrated cards need room for a large emblem, title and readable tagline");
        int[][] sizes = {{960,540},{640,360},{480,270},{320,240},{427,240},{854,480},{1280,360}};
        for (var size : sizes) {
            var layout = ClassSelectionLayout.fit(size[0], size[1]);
            var previous = new ArrayList<ClassSelectionLayout.Rect>();
            for (int i = 0; i < 4; i++) {
                var card = layout.card(i);
                check(card.x() >= 0 && card.y() >= 0 && card.right() <= size[0] && card.bottom() <= size[1],
                        "Every card must remain inside the viewport at " + size[0] + "x" + size[1]);
                check(card.width() > 0 && card.height() > 0, "Cards must remain usable");
                check(card.contains(card.x() + card.width() / 2.0, card.y() + card.height() / 2.0), "Visible center must be clickable");
                for (var other : previous) check(!card.overlaps(other), "Class hitboxes must never overlap");
                previous.add(card);
            }
            var confirm = layout.confirm();
            check(confirm.bottom() <= size[1] && confirm.right() <= size[0], "Confirmation must stay onscreen");
            for (var card : previous) check(!confirm.overlaps(card), "Confirmation must not overlap a class");
            var a = layout.card(0); var b = layout.card(1); var c = layout.card(2);
            check(a.x() < b.x() && b.x() < c.x() && a.y() == c.y(), "Compact class previews must share a row");
            var detail = layout.project(new ClassSelectionLayout.Rect(110,294,580,77));
            for(var card : previous) check(!detail.overlaps(card), "The selected detail sheet must not cover a class hitbox");
            check(!detail.overlaps(confirm), "Details must not cover confirmation");
        }
        System.out.println("PASS: selection confirmation, retry, duplicate prevention and seven viewport layouts");
    }
}
