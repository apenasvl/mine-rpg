package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.List;

/** Focusable class preview; selection never commits a server choice. */
public final class ClassCardWidget extends ButtonWidget {
    private final RPGClass clazz;
    private final ClassSelectionLayout.Rect source;
    private final ClassSelectionState<RPGClass> selection;
    public ClassCardWidget(ClassSelectionLayout.Rect rect,int index,RPGClass clazz,ClassSelectionState<RPGClass> selection) {
        super(rect.x(),rect.y(),rect.width(),rect.height(),Text.literal(clazz.display),b->selection.select(clazz),DEFAULT_NARRATION_SUPPLIER);
        this.clazz=clazz;this.source=ClassSelectionLayout.sourceCard(index);this.selection=selection;
    }
    public static String tagline(RPGClass c) {
        return switch(c) {case GUERREIRO->"Resistência e força";case MAGO->"Magia e controle";case ARQUEIRO->"Precisão e agilidade";case ASSASSINO->"Combos e execução";};
    }
    public static String description(RPGClass c) {
        return switch(c) {
            case GUERREIRO->"Combate corpo a corpo, resistência a impactos e controle da linha de frente.";
            case MAGO->"Feitiços, elementos e domínio do campo de batalha. Poder mágico com gestão de Mana.";
            case ARQUEIRO->"Ataques à distância, precisão e mobilidade para escolher seus alvos e manter distância.";
            case ASSASSINO->"Ataques rápidos, efeitos e finalizações. Mobilidade para entrar, atacar e recuar.";
        };
    }
    public static String attributes(RPGClass c) {
        return switch(c) {case GUERREIRO->"Força · Vitalidade · Tenacidade";case MAGO->"Inteligência · Arcano";case ARQUEIRO->"Destreza · Tenacidade";case ASSASSINO->"Destreza · Força";};
    }
    @Override public void renderButton(DrawContext c,int mx,int my,float delta) {
        var font=MinecraftClient.getInstance().textRenderer;
        CosmicSelectionArt.begin(c,new ClassSelectionLayout.Rect(getX(),getY(),width,height),source);
        boolean selected=selection.selected()==clazz;
        int accent=RpgUiTheme.accent(clazz);
        CleanRpgUi.panel(c,0,0,source.width(),source.height(),selected||hovered||isFocused()?accent:CleanRpgUi.BORDER);
        if(selected) c.fill(1,source.height()-3,source.width()-1,source.height()-1,accent);
        RpgUiTheme.drawIcon(c,CleanRpgUi.icon(clazz),12,14,21,RpgUiTheme.accessibleAccent(accent,CleanRpgUi.PANEL));
        CosmicSelectionArt.label(c,font,clazz.display,41,17,1.2f,CleanRpgUi.TEXT);
        CosmicSelectionArt.label(c,font,tagline(clazz),12,49,1f,CleanRpgUi.MUTED);
        c.getMatrices().pop();
    }
    public List<Text> houseTooltip(double x,double y) {return List.of();}
    @Override protected net.minecraft.text.MutableText getNarrationMessage() {
        return Text.literal(clazz.display+". "+description(clazz)+(selection.selected()==clazz?" Selecionada. Confirme para continuar.":" Selecione para comparar."));
    }
}
