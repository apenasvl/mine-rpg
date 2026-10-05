package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

/** Lightweight pixel panels: no blur, shaders or new runtime dependencies. */
public final class CleanRpgUi {
    public static final int BACKGROUND=0xF014161B, PANEL=0xFA202329, BORDER=0xFF464951;
    public static final int TEXT=0xFFF1ECE1, MUTED=0xFFADB0B5;
    public static void panel(DrawContext c,int x,int y,int w,int h,int accent) {
        c.fill(x,y,x+w,y+h,PANEL);
        c.fill(x,y,x+w,y+1,accent); c.fill(x,y+h-1,x+w,y+h,BORDER);
        c.fill(x,y,x+1,y+h,BORDER); c.fill(x+w-1,y,x+w,y+h,BORDER);
    }
    public static String icon(RPGClass clazz) {
        return switch(clazz) {case GUERREIRO->"sword";case MAGO->"star";case ARQUEIRO->"bow";case ASSASSINO->"dagger";};
    }
    public static void text(DrawContext c,TextRenderer font,String text,int x,int y,int color,int maxWidth) {
        c.drawTextWithShadow(font,font.trimToWidth(text,Math.max(0,maxWidth)),x,y,color);
    }
    private CleanRpgUi() {}
}
