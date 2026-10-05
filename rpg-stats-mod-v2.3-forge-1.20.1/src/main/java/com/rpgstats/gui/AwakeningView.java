package com.rpgstats.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.util.function.Consumer;

public final class AwakeningView {
    private final ClassSelectionState<Boolean> request=new ClassSelectionState<>();
    private ClassSelectionLayout layout;
    private ButtonWidget button;
    public void init(int width,int height,Consumer<ButtonWidget> add,Runnable send) {
        layout=ClassSelectionLayout.fit(width,height);request.select(Boolean.TRUE);
        var r=layout.project(new ClassSelectionLayout.Rect(300,330,200,34));
        button=new RpgButton(r.x(),r.y(),r.width(),r.height(),Text.literal("Despertar"),
                b->request.confirm(Util.getMeasuringTimeMs(),ignored->send.run()),RpgButton.Kind.ACTION,0xFF91C178);
        add.accept(button);update();
    }
    private void update() {request.tick(Util.getMeasuringTimeMs());if(button!=null) button.active=!request.pending();}
    public void reset() {request.reset();}
    public void render(DrawContext c,int width,int height) {
        update();c.fill(0,0,width,height,CleanRpgUi.BACKGROUND);
        c.getMatrices().push();c.getMatrices().translate(layout.x(),layout.y(),0);c.getMatrices().scale((float)layout.scale(),(float)layout.scale(),1);
        var font=MinecraftClient.getInstance().textRenderer;
        CleanRpgUi.panel(c,190,54,420,342,0xFFB9ACDE);
        CosmicSelectionArt.centered(c,font,"DESPERTE SUA ESSÊNCIA",400,87,2f,CleanRpgUi.TEXT,450);
        CosmicSelectionArt.centered(c,font,"O primeiro passo da sua jornada.",400,125,1.2f,CleanRpgUi.MUTED,450);
        CartoonClassArt.essence(c,351,167,98);
        CosmicSelectionArt.centered(c,font,"+3 ao recurso da classe que você escolher.",400,285,1.2f,CleanRpgUi.TEXT,450);
        String hint=request.pending()?"Aguardando o servidor...":request.timedOut()?"Sem resposta. Tente novamente.":"Gratuito · Uma única vez";
        CosmicSelectionArt.centered(c,font,hint,400,374,1f,CleanRpgUi.MUTED,450);
        c.getMatrices().pop();
    }
}
