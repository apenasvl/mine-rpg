package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Presentation embedded in StatsScreen, preserving its existing server-sync rebuild path. */
public final class ClassSelectionView {
    private final ClassSelectionState<RPGClass> selection = new ClassSelectionState<>();
    private final List<ClassCardWidget> cards = new ArrayList<>();
    private ClassSelectionLayout layout;
    private ButtonWidget confirm;

    public void init(int width, int height, Consumer<ButtonWidget> addWidget, Consumer<RPGClass> send) {
        layout = ClassSelectionLayout.fit(width, height);
        cards.clear();
        RPGClass[] roster = {RPGClass.GUERREIRO, RPGClass.MAGO, RPGClass.ARQUEIRO, RPGClass.ASSASSINO};
        for (int i = 0; i < roster.length; i++) {
            var card = new ClassCardWidget(layout.card(i), i, roster[i], selection);
            cards.add(card);
            addWidget.accept(card);
        }
        var rect = layout.confirm();
        confirm = new ButtonWidget(rect.x(), rect.y(), rect.width(), rect.height(), Text.literal("Confirmar classe"),
                button -> selection.confirm(Util.getMeasuringTimeMs(), send), narration -> narration.get()) {
            @Override
            public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                var source = new ClassSelectionLayout.Rect(1230, 840, 400, 70);
                CosmicSelectionArt.begin(context, rect, source);
                CosmicSelectionArt.texture(context, source);
                if (!active) context.fill(10, 9, 389, 61, 0x92020B15);
                if (active && (hovered || isFocused())) CosmicSelectionArt.outline(context, 400, 70, 0xFFFFFFFF);
                CosmicSelectionArt.centered(context, MinecraftClient.getInstance().textRenderer,
                        selection.pending() ? "CONFIRMANDO..." : "CONFIRMAR CLASSE", 184, 25, 2.6f,
                        active ? 0xFFFFFFFF : 0xFF9AB7C8, 305);
                context.getMatrices().pop();
            }
        };
        addWidget.accept(confirm);
        update();
    }

    private void update() {
        selection.tick(Util.getMeasuringTimeMs());
        if (confirm != null) confirm.active = selection.selected() != null && !selection.pending();
        for (var card : cards) card.active = !selection.pending();
    }

    public void reset() { selection.reset(); }

    public void render(DrawContext context, int width, int height) {
        update();
        context.fill(0, 0, width, height, 0xFF020711);
        var font = MinecraftClient.getInstance().textRenderer;
        context.getMatrices().push();
        context.getMatrices().translate(layout.x(), layout.y(), 0);
        context.getMatrices().scale((float) layout.scale(), (float) layout.scale(), 1);
        // Draw only the surroundings. The four independent widgets render their atlas regions.
        drawRegion(context, 0, 0, 1672, 177);
        drawRegion(context, 0, 177, 125, 656);
        drawRegion(context, 1549, 177, 123, 656);
        drawRegion(context, 828, 177, 12, 656);
        drawRegion(context, 125, 502, 1424, 10);
        drawRegion(context, 0, 832, 1672, 109);
        CosmicSelectionArt.centered(context, font, "ESCOLHA SUA CLASSE", 836, 76, 4.7f, 0xFFFFFFFF, 960);
        CosmicSelectionArt.centered(context, font, "Uma classe, uma Casa principal e uma afinidade opcional.",
                836, 136, 2.5f, CosmicSelectionArt.CYAN, 1300);
        String help = selection.timedOut() ? "Sem resposta do servidor. Você pode confirmar novamente."
                : selection.selected() == null ? "Selecione uma classe para começar sua jornada."
                : selection.pending() ? "Aguardando a confirmação do servidor..."
                : selection.selected().display + " selecionado. Confirme para iniciar sua jornada.";
        CosmicSelectionArt.label(context, font, help, 108, 861, 1.9f, CosmicSelectionArt.TEXT);
        CosmicSelectionArt.label(context, font, "5 Casas por classe · afinidade da mesma classe · sem multiclass",
                108, 887, 1.6f, CosmicSelectionArt.CYAN);
        context.getMatrices().pop();
    }

    private static void drawRegion(DrawContext context, int x, int y, int width, int height) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        CosmicSelectionArt.texture(context, new ClassSelectionLayout.Rect(x, y, width, height));
        context.getMatrices().pop();
    }

    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        for (var card : cards) {
            List<Text> lines = card.houseTooltip(mouseX, mouseY);
            if (!lines.isEmpty()) {
                context.drawTooltip(MinecraftClient.getInstance().textRenderer, lines, mouseX, mouseY);
                return;
            }
        }
    }
}
