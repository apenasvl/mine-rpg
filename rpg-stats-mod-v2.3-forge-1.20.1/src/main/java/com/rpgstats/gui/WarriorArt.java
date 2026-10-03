package com.rpgstats.gui;

import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Twenty martial emblems; labels and player state remain live GUI elements. */
public final class WarriorArt {
    private static final Identifier ATLAS = new Identifier("rpgstats", "textures/gui/warrior/emblems.png");
    // Boundaries of the original artwork, preserving every complete emblem.
    private static final int[] X={0,299,562,842,1122}, Y={0,259,513,784,1051,1402};
    public static void emblem(DrawContext c,RPGPath house,RPGSpecialization spec,int x,int y,int size,float opacity) {
        WarriorTheme theme=WarriorTheme.forHouse(house==null?null:house.name());
        int col=theme.cell(spec==null?null:spec.name()), row=theme.ordinal();
        int sw=X[col+1]-X[col], sh=Y[row+1]-Y[row];
        RenderSystem.enableBlend();
        c.setShaderColor(1,1,1,opacity);
        c.getMatrices().push();
        c.getMatrices().translate(x,y,0);
        c.getMatrices().scale(size/(float)sw,size/(float)sh,1);
        c.drawTexture(ATLAS,0,0,X[col],Y[row],sw,sh,1122,1402);
        c.getMatrices().pop();
        c.setShaderColor(1,1,1,1);
    }
    public static void background(DrawContext c,RPGPath house,RPGSpecialization spec,int x,int y,int w,int h) {
        MageArt.frame(c,x,y,w,h,WarriorTheme.forHouse(house==null?null:house.name()).color());
        int size=Math.min(w-20,h-20);
        emblem(c,house,spec,x+w-size-8,y+(h-size)/2,size,.32f);
    }
    private WarriorArt() {}
}
