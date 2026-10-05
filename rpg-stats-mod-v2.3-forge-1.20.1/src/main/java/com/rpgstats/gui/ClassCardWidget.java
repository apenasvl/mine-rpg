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
        int accent=CleanRpgUi.accent(clazz);
        int fill=RpgUiTheme.mix(CleanRpgUi.PANEL,accent,selected?.10f:.035f);
        int edge=selected||hovered||isFocused()?accent:CleanRpgUi.BORDER;
        CleanRpgUi.surface(c,0,0,source.width(),source.height(),fill,edge);
        ReferenceIcons.draw(c,CleanRpgUi.icon(clazz),39,18,76,accent);
        CosmicSelectionArt.centered(c,font,clazz.display.toUpperCase(java.util.Locale.ROOT),source.width()/2,103,1.45f,CleanRpgUi.TEXT,source.width()-14);
        CosmicSelectionArt.centered(c,font,tagline(clazz),source.width()/2,130,.95f,CleanRpgUi.MUTED,source.width()-16);
        CosmicSelectionArt.centered(c,font,selected?"SELECIONADO":"VER CLASSE",source.width()/2,149,.8f,CleanRpgUi.MUTED,source.width()-16);
        c.getMatrices().pop();
    }
    public List<Text> houseTooltip(double x,double y) {return List.of();}
    @Override protected net.minecraft.text.MutableText getNarrationMessage() {
        return Text.literal(clazz.display+". "+description(clazz)+(selection.selected()==clazz?" Selecionada. Confirme para continuar.":" Selecione para comparar."));
    }
}
