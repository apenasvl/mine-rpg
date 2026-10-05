package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import net.minecraft.client.gui.DrawContext;

/** Small hand-built pixel illustrations, drawn at the same scale as their card. */
public final class CartoonClassArt {
    private static final String[] SWORD={
        "................", ".............OO.", "............OWO.", "...........OWSO.",
        "..........OWSO..", ".........OWSO...", "........OWSO....", ".......OWSO.....",
        "..OO..OWSO......", "..OGOOWSO.......", "...OGWSO........", "....OGOO........",
        "...OSOGO........", "..OSO.OO........", ".OSO............", ".OO............."};
    private static final String[] HAT={
        "................", ".........OO.....", "........OAO.....", ".......OAHO.....",
        "......OAAHO.....", ".....OAAAHO.....", "....OASAAHO.....", "....OASAAAHO....",
        "...OASSAAAHO....", "...OGGGGGGGO....", "..OAHAAAAAAHO...", ".OAAAAAAAAAAAO..",
        "OAAAAAAAAAAAAAO.", ".OOOOOOOOOOOOO..", "...OWWWWWWO.....", "....OOOOOO......"};
    private static final String[] BOW={
        "................", "...OO...........", "...OGO..........", "....OSO.........",
        "....OWAO........", "....OWSAO.......", "....OW.AAO......", "..OOOW..AO......",
        "..OGGGGGGGGGO...", "..OOOW..AO......", "....OW.AAO......", "....OWSAO.......",
        "....OWAO........", "....OSO.........", "...OGO..........", "...OO..........."};
    private static final String[] DAGGER={
        "................", ".........OO.....", "........OWO.....", ".......OWAO.....",
        "......OWASO.....", ".....OWASO......", "....OWASO.......", "...OWASO........",
        "..OOWSO.........", "..OGGO..........", "...OGGO.........", "...OSOO.........",
        "..OSO...........", ".OASO...........", ".OAO............", "..O............."};
    private static final String[] CRYSTAL={
        "................", ".......OO.......", "......OWHO......", ".....OWHHHO.....",
        "....OWHHHAAO....", "...OWHHHAAAAO...", "..OWHHHAAAASAO..", "..OWHHAAAAASAO..",
        "..OHHAAAAAASAO..", "...OHAAAAASAO...", "....OAAAASAO....", ".....OAASAO.....",
        "......OASO......", ".......OO.......", "................", "................"};
    public static void emblem(DrawContext c,RPGClass clazz,int x,int y,int size) {
        if(clazz==null) return;
        sprite(c,switch(clazz) {case GUERREIRO->SWORD;case MAGO->HAT;case ARQUEIRO->BOW;case ASSASSINO->DAGGER;},x,y,size,CleanRpgUi.accent(clazz));
    }
    public static void essence(DrawContext c,int x,int y,int size) {
        sprite(c,CRYSTAL,x,y,size,0xFFB9ACED);
        sparkle(c,x-13,y+18,8,0xFF9EC9EE);sparkle(c,x+size+5,y+48,10,0xFFEDA5A0);
        sparkle(c,x+size-8,y-9,6,0xFFE2BD75);
    }
    private static void sparkle(DrawContext c,int x,int y,int size,int color) {
        c.fill(x+size/2-1,y,x+size/2+1,y+size,color);
        c.fill(x,y+size/2-1,x+size,y+size/2+1,color);
    }
    private static void sprite(DrawContext c,String[] rows,int x,int y,int size,int accent) {
        c.getMatrices().push();c.getMatrices().translate(x,y,0);c.getMatrices().scale(size/16f,size/16f,1);
        for(int row=0;row<rows.length;row++) for(int col=0;col<rows[row].length();col++) {
            char pixel=rows[row].charAt(col);
            int color=switch(pixel) {case 'O'->0xFF51455F;case 'W'->0xFFFFF5DB;case 'H'->RpgUiTheme.lighten(accent,.45f);case 'S'->RpgUiTheme.darken(accent,.24f);case 'G'->0xFFEAC47C;case 'A'->accent;default->0;};
            if(color!=0) c.fill(col,row,col+1,row+1,color);
        }
        c.getMatrices().pop();
    }
    private CartoonClassArt() {}
}
