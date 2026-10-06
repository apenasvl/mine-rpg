package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

/** Soft pixel corners and opaque pastel surfaces; no shader or runtime dependency. */
public final class CleanRpgUi {
    public static final int BACKGROUND=0x8020252C, PANEL=0xF511171B, BORDER=0xFF56606B;
    public static final int TEXT=0xFFE7E5ED, MUTED=0xFFB7B5C1;
    public static int accent(RPGClass clazz) {
        if(clazz==null) return 0xFFB9ACDE;
        return switch(clazz) {case GUERREIRO->0xFFEDA5A0;case MAGO->0xFF9EC9EE;case ARQUEIRO->0xFFA6D5AE;case ASSASSINO->0xFFBCA6DE;};
    }
    public static void surface(DrawContext c,int x,int y,int w,int h,int fill,int edge) {
        // Stepped corners preserve Minecraft's pixel silhouette, with a small offset shadow.
        round(c,x+2,y+3,w,h,0x40342E49);
        round(c,x,y,w,h,edge);
        round(c,x+2,y+2,w-4,h-4,fill);
        c.fill(x+6,y+3,x+w-6,y+4,0x306B7885);
    }
    private static void round(DrawContext c,int x,int y,int w,int h,int color) {
        if(w<8||h<8) {c.fill(x,y,x+w,y+h,color);return;}
        c.fill(x+4,y,x+w-4,y+h,color);
        c.fill(x+2,y+2,x+w-2,y+h-2,color);
        c.fill(x,y+4,x+w,y+h-4,color);
    }
    public static void panel(DrawContext c,int x,int y,int w,int h,int accent) {
        surface(c,x,y,w,h,RpgUiTheme.mix(PANEL,accent,.035f),RpgUiTheme.mix(BORDER,accent,.22f));
    }
    public static String icon(RPGClass clazz) {
        return switch(clazz) {case GUERREIRO->"sword";case MAGO->"flame";case ARQUEIRO->"bow";case ASSASSINO->"dagger";};
    }
    public static void text(DrawContext c,TextRenderer font,String text,int x,int y,int color,int maxWidth) {
        c.drawText(font,font.trimToWidth(text,Math.max(0,maxWidth)),x,y,color,false);
    }
    private CleanRpgUi() {}
}
