package com.rpgstats.gui;

import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Twenty assassin emblems; labels and player state remain live GUI elements. */
public final class AssassinArt {
    private static final Identifier ATLAS = new Identifier("rpgstats", "textures/gui/assassin/emblems.png");
    // Boundaries of the original artwork, preserving every complete emblem.
    private static final int[] X={0,281,561,844,1122}, Y={0,269,538,804,1071,1402};
    public static void emblem(DrawContext c,RPGPath house,RPGSpecialization spec,int x,int y,int size,float opacity) {
        AssassinTheme theme=AssassinTheme.forHouse(house==null?null:house.name());
        int col=theme.cell(spec==null?null:spec.name()), row=theme.ordinal();
        int sw=X[col+1]-X[col], sh=Y[row+1]-Y[row];
        RenderSystem.enableBlend();
        c.setShaderColor(1,1,1,opacity);
        c.getMatrices().push();
        float scale=Math.min(size/(float)sw,size/(float)sh);
        c.getMatrices().translate(x+(size-sw*scale)/2,y+(size-sh*scale)/2,0);
        c.getMatrices().scale(scale,scale,1);
        c.drawTexture(ATLAS,0,0,X[col],Y[row],sw,sh,1122,1402);
        c.getMatrices().pop();
        c.setShaderColor(1,1,1,1);
    }
    private AssassinArt() {}
}
