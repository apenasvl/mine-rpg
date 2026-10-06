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

public final class ClassSelectionView {
    private final ClassSelectionState<RPGClass> selection=new ClassSelectionState<>();
    private final List<ClassCardWidget> cards=new ArrayList<>();
    private ClassSelectionLayout layout;
    private ButtonWidget confirm;
    public void init(int width,int height,Consumer<ButtonWidget> add,Consumer<RPGClass> send) {
        layout=ClassSelectionLayout.fit(width,height);cards.clear();
        RPGClass[] roster={RPGClass.GUERREIRO,RPGClass.MAGO,RPGClass.ARQUEIRO,RPGClass.ASSASSINO};
        for(int i=0;i<roster.length;i++) {var card=new ClassCardWidget(layout.card(i),i,roster[i],selection);cards.add(card);add.accept(card);}
        var r=layout.confirm();
        confirm=new RpgButton(r.x(),r.y(),r.width(),r.height(),Text.literal("Confirmar classe"),
                b->selection.confirm(Util.getMeasuringTimeMs(),send),RpgButton.Kind.ACTION,0xFF8EBBFF);
        add.accept(confirm);update();
    }
    private void update() {
        selection.tick(Util.getMeasuringTimeMs());
        if(confirm!=null) confirm.active=selection.selected()!=null&&!selection.pending();
        for(var card:cards) card.active=!selection.pending();
    }
    public void reset() {selection.reset();}
    public void render(DrawContext c,int width,int height) {
        update();c.fill(0,0,width,height,CleanRpgUi.BACKGROUND);
        var font=MinecraftClient.getInstance().textRenderer;
        c.getMatrices().push();c.getMatrices().translate(layout.x(),layout.y(),0);c.getMatrices().scale((float)layout.scale(),(float)layout.scale(),1);
        CosmicSelectionArt.centered(c,font,"RPG",400,23,2.8f,CleanRpgUi.TEXT,730);
        CosmicSelectionArt.centered(c,font,"ESCOLHA SUA CLASSE",400,59,1.5f,CleanRpgUi.TEXT,730);
        CosmicSelectionArt.centered(c,font,"Escolha o caminho que define sua jornada.",400,83,1f,CleanRpgUi.MUTED,730);
        var chosen=selection.selected();
        int accent=chosen==null?CleanRpgUi.BORDER:CleanRpgUi.accent(chosen);
        CleanRpgUi.panel(c,110,294,580,77,accent);
        if(chosen==null) CosmicSelectionArt.centered(c,font,"Selecione uma classe para conhecer seu estilo.",400,326,1.1f,CleanRpgUi.MUTED,550);
        else {
            CosmicSelectionArt.centered(c,font,chosen.display,400,306,1.4f,RpgUiTheme.accessibleAccent(accent,CleanRpgUi.PANEL),550);
            CosmicSelectionArt.wrapped(c,font,ClassCardWidget.description(chosen),130,330,1.1f,CleanRpgUi.MUTED,540,2);
        }
        String hint=selection.pending()?"Aguardando o servidor...":selection.timedOut()?"Sem resposta. Confirme para tentar novamente.":"Casa no nível 10 · Especialização no 25 · Afinidade no 30";
        CosmicSelectionArt.centered(c,font,hint,400,436,.9f,CleanRpgUi.MUTED,700);
        c.getMatrices().pop();
    }
    public void renderTooltip(DrawContext c,int x,int y) {}
}
