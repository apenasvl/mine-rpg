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
        CosmicSelectionArt.centered(c,font,"ESCOLHA SUA CLASSE",400,36,2f,CleanRpgUi.TEXT,730);
        CosmicSelectionArt.centered(c,font,"Compare os estilos. A escolha só acontece ao confirmar.",400,70,1.2f,CleanRpgUi.MUTED,730);
        var chosen=selection.selected();
        int accent=chosen==null?CleanRpgUi.BORDER:RpgUiTheme.accent(chosen);
        CleanRpgUi.panel(c,34,188,732,168,accent);
        if(chosen==null) CosmicSelectionArt.centered(c,font,"Selecione uma classe para ver sua ficha.",400,256,1.4f,CleanRpgUi.MUTED,700);
        else {
            RpgUiTheme.drawIcon(c,CleanRpgUi.icon(chosen),57,214,45,RpgUiTheme.accessibleAccent(accent,CleanRpgUi.PANEL));
            CosmicSelectionArt.label(c,font,chosen.display.toUpperCase(java.util.Locale.ROOT),122,211,1.8f,CleanRpgUi.TEXT);
            CosmicSelectionArt.wrapped(c,font,ClassCardWidget.description(chosen),122,243,1.3f,CleanRpgUi.MUTED,600,3);
            CosmicSelectionArt.label(c,font,"RECURSO  "+chosen.resourceName(),58,307,1.2f,CleanRpgUi.TEXT);
            CosmicSelectionArt.label(c,font,"ATRIBUTOS  "+ClassCardWidget.attributes(chosen),300,307,1.2f,CleanRpgUi.TEXT);
            CosmicSelectionArt.label(c,font,"Casa no nível 10 · Especialização no 25 · Afinidade opcional no 30 · Requer Maestria",58,334,1f,CleanRpgUi.MUTED);
        }
        String hint=selection.pending()?"Aguardando o servidor...":selection.timedOut()?"Sem resposta. Confirme para tentar novamente.":"Uma classe, cinco Casas possíveis. Sua build evolui com você.";
        CosmicSelectionArt.wrapped(c,font,hint,34,392,1.1f,CleanRpgUi.MUTED,520,2);
        c.getMatrices().pop();
    }
    public void renderTooltip(DrawContext c,int x,int y) {}
}
