package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.List;

/** An illustrated, keyboard-focusable class preview. Pressing it never sends a packet. */
public final class ClassCardWidget extends ButtonWidget {
    private final RPGClass clazz;
    private final ClassSelectionLayout.Rect source;
    private final ClassSelectionState<RPGClass> selection;
    private final List<RPGPath> houses;

    public ClassCardWidget(ClassSelectionLayout.Rect rect, int index, RPGClass clazz,
                           ClassSelectionState<RPGClass> selection) {
        super(rect.x(), rect.y(), rect.width(), rect.height(), Text.literal(clazz.display),
                button -> selection.select(clazz), DEFAULT_NARRATION_SUPPLIER);
        this.clazz = clazz;
        this.source = ClassSelectionLayout.sourceCard(index);
        this.selection = selection;
        this.houses = RPGPath.forClass(clazz);
    }

    private int accent() {
        return switch (clazz) {
            case GUERREIRO -> 0xFFFF998D;
            case MAGO -> 0xFF8DE7FF;
            case ARQUEIRO -> 0xFFB0ED9E;
            case ASSASSINO -> 0xFFE4A9FF;
        };
    }

    private String tagline() {
        return switch (clazz) {
            case GUERREIRO -> "Combate marcial · Força · Resistência";
            case MAGO -> "Magia profunda · Mana · Conhecimento";
            case ARQUEIRO -> "Precisão · Foco · Mobilidade";
            case ASSASSINO -> "Combos · Energia · Execução";
        };
    }

    private String description() {
        return switch (clazz) {
            case GUERREIRO -> "Especialista em combate corpo a corpo. Resiste a impactos e controla a linha de frente.";
            case MAGO -> "Manipula forças arcanas, elementos, espaço e tempo. Destrói, protege e transforma o campo de batalha.";
            case ARQUEIRO -> "Mestre do combate à distância, com precisão e mobilidade. Domina o terreno e escolhe seus alvos.";
            case ASSASSINO -> "Combate rápido e letal, com ataques precisos e finalizações. Sombra e agilidade são suas armas.";
        };
    }

    private String resourceDescription() {
        return switch (clazz) {
            case GUERREIRO -> "Fortalece habilidades e golpes de combate.";
            case MAGO -> "Alimenta seus feitiços e habilidades arcanas.";
            case ARQUEIRO -> "Precisão e controle à distância.";
            case ASSASSINO -> "Mobilidade, combos e execução.";
        };
    }

    private String[] tags() {
        return switch (clazz) {
            case GUERREIRO -> new String[]{"Tanque", "Dano", "Fúria"};
            case MAGO -> new String[]{"Mana", "Casas arcanas", "Controle"};
            case ARQUEIRO -> new String[]{"Precisão", "Mobilidade", "Foco"};
            case ASSASSINO -> new String[]{"Combos", "Execução", "Velocidade"};
        };
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        var font = MinecraftClient.getInstance().textRenderer;
        var target = new ClassSelectionLayout.Rect(getX(), getY(), width, height);
        CosmicSelectionArt.begin(context, target, source);
        CosmicSelectionArt.texture(context, source);
        int color = accent();
        boolean selected = selection.selected() == clazz;
        if (selected || hovered || isFocused()) {
            CosmicSelectionArt.outline(context, source.width(), source.height(), selected ? 0xFFFFFFFF : color);
        }
        CosmicSelectionArt.label(context, font, clazz.display.toUpperCase(java.util.Locale.ROOT), 156, 22, 3.6f, color);
        CosmicSelectionArt.label(context, font, tagline(), 158, 63, 1.9f, color);
        CosmicSelectionArt.wrapped(context, font, description(), 158, 95, 1.9f,
                CosmicSelectionArt.TEXT, 365, 3);
        CosmicSelectionArt.label(context, font, "RECURSO PRINCIPAL", 28, 164, 1.9f, CosmicSelectionArt.CYAN);
        String resource = clazz == RPGClass.GUERREIRO ? "Fúria" : clazz.resourceName();
        CosmicSelectionArt.label(context, font, resource, 112, 188, 2.2f, color);
        CosmicSelectionArt.wrapped(context, font, resourceDescription(), 112, 212, 1.9f,
                CosmicSelectionArt.TEXT, 232, 2);
        CosmicSelectionArt.label(context, font, houses.size() + " CASAS", 367, 164, 2.1f, CosmicSelectionArt.CYAN);
        String[] tags = tags();
        for (int i = 0; i < tags.length; i++) {
            CosmicSelectionArt.centered(context, font, tags[i], 168 + i * 216, source.height() - 43,
                    2f, CosmicSelectionArt.TEXT, 124);
        }
        if (selected) {
            context.fill(source.width() - 165, 10, source.width() - 24, 31, 0xE0081724);
            CosmicSelectionArt.label(context, font, "SELECIONADA", source.width() - 154, 14, 1.7f, 0xFFFFFFFF);
        }
        context.getMatrices().pop();
    }

    public List<Text> houseTooltip(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return List.of();
        double localX = (mouseX - getX()) * source.width() / width;
        double localY = (mouseY - getY()) * source.height() / height;
        if (localY < 188 || localY > 231) return List.of();
        int index = (int) Math.floor((localX - 363) / 41.0);
        if (index < 0 || index >= houses.size() || localX >= 363 + houses.size() * 41) return List.of();
        RPGPath house = houses.get(index);
        return List.of(Text.literal(house.display), Text.literal(house.desc),
                Text.literal("Prévia · a escolha da Casa é liberada pela progressão."));
    }

    @Override
    protected net.minecraft.text.MutableText getNarrationMessage() {
        return Text.literal(clazz.display + ". " + tagline() + ". " + description()
                + (selection.selected() == clazz ? " Selecionada. Use Confirmar classe." : " Selecione para comparar."));
    }
}
