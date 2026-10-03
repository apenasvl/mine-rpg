package com.rpgstats.gui;

import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Resource-pack replaceable ornamental textures; never contains player data or labels. */
public final class MageArt {
    private static final Identifier FRAME = new Identifier("rpgstats", "textures/gui/mage/frame.png");
    private static final Identifier[] ATLASES = java.util.Arrays.stream(MageTheme.values())
            .map(t -> new Identifier("rpgstats", "textures/gui/mage/"+t.asset()+".png")).toArray(Identifier[]::new);
    public static MageTheme theme(RPGPath path) { return MageTheme.forHouse(path==null?null:path.name()); }

    private static void region(DrawContext c, Identifier id, int x,int y,int w,int h,int u,int v,int sw,int sh) {
        if(w<=0 || h<=0) return;
        c.getMatrices().push();
        c.getMatrices().translate(x,y,0);
        c.getMatrices().scale(w/(float)sw,h/(float)sh,1);
        c.drawTexture(id,0,0,u,v,sw,sh,1254,1254);
        c.getMatrices().pop();
    }
    public static void emblem(DrawContext c,RPGPath house,RPGSpecialization spec,int x,int y,int size,float opacity) {
        MageTheme theme=theme(house);
        int cell=theme.cell(spec==null?null:spec.name());
        RenderSystem.enableBlend();
        c.setShaderColor(1,1,1,opacity);
        region(c,ATLASES[theme.ordinal()],x,y,size,size,(cell%2)*627,(cell/2)*627,627,627);
        c.setShaderColor(1,1,1,1);
    }
    public static void frame(DrawContext c,int x,int y,int w,int h,int color) {
        int edge=Math.min(12,Math.min(w,h)/3);
        RenderSystem.enableBlend();
        c.setShaderColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);
        int[] ss={0,256,998}, sizes={256,742,256};
        int[] dx={x,x+edge,x+w-edge},dy={y,y+edge,y+h-edge};
        int[] dw={edge,w-2*edge,edge},dh={edge,h-2*edge,edge};
        for(int row=0;row<3;row++) for(int col=0;col<3;col++)
            region(c,FRAME,dx[col],dy[row],dw[col],dh[row],ss[col],ss[row],sizes[col],sizes[row]);
        c.setShaderColor(1,1,1,1);
    }
    public static void background(DrawContext c,RPGPath house,RPGSpecialization spec,int x,int y,int w,int h) {
        frame(c,x,y,w,h,theme(house).color());
        int size=Math.min(w-20,h-20);
        emblem(c,house,spec,x+w-size-8,y+(h-size)/2,size,.32f);
    }
    private MageArt() {}
}
