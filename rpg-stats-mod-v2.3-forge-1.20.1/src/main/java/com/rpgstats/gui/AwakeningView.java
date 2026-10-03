package com.rpgstats.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import java.util.function.Consumer;

/** Illustrated first-run entry, using the existing server-authoritative awakening action. */
public final class AwakeningView {
    private static final Identifier ART = new Identifier("rpgstats", "textures/gui/awakening/essence.png");
    private final ClassSelectionState<Boolean> request = new ClassSelectionState<>();
    private ClassSelectionLayout layout;
    private ButtonWidget button;

    public void init(int width,int height,Consumer<ButtonWidget> addWidget,Runnable send) {
        layout=ClassSelectionLayout.fit(width,height);
        request.select(Boolean.TRUE);
        var rect=layout.project(new ClassSelectionLayout.Rect(546,798,580,80));
        button=new ButtonWidget(rect.x(),rect.y(),rect.width(),rect.height(),Text.literal("Despertar"),
                b->request.confirm(Util.getMeasuringTimeMs(), ignored->send.run()), narration->narration.get()) {
            @Override public void renderButton(DrawContext context,int mouseX,int mouseY,float delta) {
                context.getMatrices().push();
                context.getMatrices().translate(rect.x(),rect.y(),0);
                context.getMatrices().scale(rect.width()/400f,rect.height()/70f,1);
                CosmicSelectionArt.texture(context,new ClassSelectionLayout.Rect(1230,840,400,70));
                if(!active) context.fill(10,9,389,61,0x92020B15);
                if(active && (hovered || isFocused())) CosmicSelectionArt.outline(context,400,70,0xFFFFFFFF);
                CosmicSelectionArt.centered(context,MinecraftClient.getInstance().textRenderer,
                        request.pending()?"DESPERTANDO...":"DESPERTAR",192,25,2.6f,0xFFFFFFFF,310);
                context.getMatrices().pop();
            }
        };
        addWidget.accept(button);
        update();
    }
    private void update() {
        request.tick(Util.getMeasuringTimeMs());
        if(button!=null) button.active=!request.pending();
    }
    public void reset() { request.reset(); }
    public void render(DrawContext context,int width,int height) {
        update();
        context.fill(0,0,width,height,0xFF020711);
        context.getMatrices().push();
        context.getMatrices().translate(layout.x(),layout.y(),0);
        context.getMatrices().scale((float)layout.scale(),(float)layout.scale(),1);
        context.drawTexture(ART,0,0,1672,941,0,0,1672,941,1672,941);
        var font=MinecraftClient.getInstance().textRenderer;
        CosmicSelectionArt.centered(context,font,"DESPERTE SUA ESSÊNCIA",836,80,4.3f,0xFFF1FAFF,1350);
        CosmicSelectionArt.centered(context,font,"Sua jornada começa com o primeiro despertar.",836,145,2.2f,0xFF8FCEEE,1300);
        CosmicSelectionArt.centered(context,font,"RESERVA DE ESSÊNCIA",836,720,2.5f,0xFFF1FAFF,1200);
        CosmicSelectionArt.centered(context,font,"+3 ao recurso da classe que você escolher.",836,760,2.2f,0xFF8FCEEE,1300);
        String hint=request.pending()?"Aguardando o servidor...":request.timedOut()?"Sem resposta. Clique para tentar novamente.":"Gratuito · Uma única vez · Escolha sua classe a seguir";
        CosmicSelectionArt.centered(context,font,hint,836,899,1.7f,0xFFC2D6E5,1450);
        context.getMatrices().pop();
    }
}
