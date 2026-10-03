package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import net.minecraft.client.gui.DrawContext;

/** Class-specific illustrations with live labels and ordered House color accents. */
public final class CharacterArt {
    public static boolean supported(RPGClass clazz) { return clazz!=null; }
    public static void emblem(DrawContext c,RPGClass clazz,RPGPath house,RPGSpecialization spec,int x,int y,int size,float opacity) {
        if(clazz==null) return;
        switch(clazz) {
            case GUERREIRO -> WarriorArt.emblem(c,house,spec,x,y,size,opacity);
            case MAGO -> MageArt.emblem(c,house,spec,x,y,size,opacity);
            case ARQUEIRO -> ArcherArt.emblem(c,house,spec,x,y,size,opacity);
            case ASSASSINO -> AssassinArt.emblem(c,house,spec,x,y,size,opacity);
        }
    }
    public static void frame(DrawContext c,RPGClass clazz,int x,int y,int w,int h,int accent) {
        MageArt.frame(c,x,y,w,h,accent);
        int edge=Math.min(12,Math.min(w,h)/3);
        c.fill(x+edge,y+edge,x+w-edge,y+h-edge,RpgUiTheme.classNeutral(clazz));
    }
    public static void background(DrawContext c,RPGClass clazz,RPGPath house,RPGSpecialization spec,int x,int y,int w,int h,int accent) {
        frame(c,clazz,x,y,w,h,accent);
        int size=Math.min(w-20,h-20);
        emblem(c,clazz,house,spec,x+w-size-8,y+(h-size)/2,size,.32f);
    }
    private CharacterArt() {}
}
