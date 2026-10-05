package com.rpgstats.gui;

import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGSpecialization;
import net.minecraft.client.gui.DrawContext;

/** Crisp, shaded pixel silhouettes for classes and all twenty Houses. */
public final class ReferenceIcons {
    private static final int INK=0xFF1D202A, METAL=0xFFD6D2DA, GOLD=0xFFE3C08B;
    public static String house(RPGPath p) {
        if(p==null)return "star";
        return switch(p) {
            case WAR_VANGUARD->"helmet";case WAR_BERSERKER->"axe";case WAR_WEAPONMASTER->"swords";case WAR_RUNIC->"rune";case WAR_COMMANDER->"banner";
            case MAGE_ELEMENTAL->"flame";case MAGE_ARCANA->"star";case MAGE_CONJURATION->"book";case MAGE_OCCULT->"skull";case MAGE_TEMPORAL->"hourglass";
            case ARC_MARKSMAN->"target";case ARC_WARDEN->"wolf";case ARC_SKIRMISHER->"feather";case ARC_ARCANE->"arcane_bow";case ARC_ARTIFICER->"trap";
            case ASS_SHADOW->"hood";case ASS_VENOM->"poison";case ASS_DUELIST->"rapier";case ASS_SABOTEUR->"bomb";case ASS_MYSTIC->"dagger";
        };
    }
    public static String specialization(RPGSpecialization spec) {
        var roster=RPGSpecialization.forPath(spec.parent);
        int index=roster.indexOf(spec);
        return switch(spec.parent) {
            case WAR_VANGUARD->new String[]{"helmet","banner","shield"}[index];
            case WAR_BERSERKER->new String[]{"skull","flame","axe"}[index];
            case WAR_WEAPONMASTER->new String[]{"swords","rapier","axe"}[index];
            case WAR_RUNIC->new String[]{"sword","rune","flame"}[index];
            case WAR_COMMANDER->new String[]{"banner","book","helmet"}[index];
            case MAGE_ELEMENTAL->new String[]{"flame","snowflake","bolt"}[index];
            case MAGE_ARCANA->new String[]{"rune","eye","star"}[index];
            case MAGE_CONJURATION->new String[]{"wolf","book","swords"}[index];
            case MAGE_OCCULT->new String[]{"poison","skull","dagger"}[index];
            case MAGE_TEMPORAL->new String[]{"bolt","hourglass","rune"}[index];
            case ARC_MARKSMAN->new String[]{"target","feather","bow"}[index];
            case ARC_WARDEN->new String[]{"wolf","book","trap"}[index];
            case ARC_SKIRMISHER->new String[]{"feather","bow","bolt"}[index];
            case ARC_ARCANE->new String[]{"arcane_bow","star","rune"}[index];
            case ARC_ARTIFICER->new String[]{"trap","bomb","poison"}[index];
            case ASS_SHADOW->new String[]{"hood","dagger","eye"}[index];
            case ASS_VENOM->new String[]{"poison","skull","bomb"}[index];
            case ASS_DUELIST->new String[]{"rapier","swords","shield"}[index];
            case ASS_SABOTEUR->new String[]{"bomb","trap","rune"}[index];
            case ASS_MYSTIC->new String[]{"skull","dagger","star"}[index];
        };
    }
    public static void draw(DrawContext c,String icon,int x,int y,int size,int accent) {
        c.getMatrices().push();c.getMatrices().translate(x,y,0);c.getMatrices().scale(size/32f,size/32f,1);
        int light=RpgUiTheme.lighten(accent,.36f),shade=RpgUiTheme.darken(accent,.30f);
        switch(icon) {
            case "shield" -> {
                poly(c,accent,4,5,16,2,28,5,27,20,23,26,16,31,9,26,5,20);
                poly(c,shade,16,5,25,7,24,20,21,24,16,28);line(c,7,8,16,5,2,light);rect(c,14,9,4,17,GOLD);rect(c,8,14,16,4,GOLD);
            }
            case "snowflake" -> {
                line(c,16,2,16,30,2,light);line(c,3,9,28,24,2,accent);line(c,3,24,28,9,2,accent);
                line(c,11,5,16,9,2,light);line(c,16,9,21,5,2,light);line(c,11,27,16,23,2,light);line(c,16,23,21,27,2,light);
            }
            case "bolt" -> {poly(c,accent,19,1,8,17,15,17,12,31,26,12,19,12);poly(c,light,19,3,10,15,17,15);}
            case "eye" -> {poly(c,accent,2,16,10,9,22,9,30,16,22,23,10,23);poly(c,METAL,5,16,11,12,21,12,27,16,21,20,11,20);rect(c,13,11,6,11,shade);rect(c,15,13,2,7,INK);}
            case "helmet" -> {
                poly(c,shade,4,4,9,10,10,19,6,17,3,8);poly(c,accent,28,4,23,10,22,19,26,17,29,8);
                poly(c,accent,16,7,23,12,25,20,20,25,18,30,14,30,12,25,7,20,9,12);
                poly(c,light,16,8,16,20,9,18,10,12);rect(c,15,11,2,17,light);
                rect(c,9,19,6,3,INK);rect(c,18,19,6,3,INK);rect(c,11,20,3,1,METAL);rect(c,19,20,3,1,METAL);
            }
            case "axe" -> {
                line(c,7,28,23,4,3,GOLD);line(c,8,27,24,4,1,light);
                poly(c,accent,18,5,23,5,28,9,29,15,24,19,20,16,17,17,15,13);
                poly(c,light,24,6,28,9,29,15,26,17,25,12,22,8);rect(c,18,7,3,4,shade);
            }
            case "swords" -> {sword(c,accent);c.getMatrices().push();c.getMatrices().translate(32,0,0);c.getMatrices().scale(-1,1,1);sword(c,GOLD);c.getMatrices().pop();}
            case "sword","dagger","rapier" -> {sword(c,icon.equals("rapier")?METAL:accent);if(icon.equals("rapier")) {line(c,6,24,12,28,2,GOLD);line(c,12,28,15,22,2,GOLD);}}
            case "rune","star","spec","crystal" -> {
                if(icon.equals("rune")) {int[][] points={{16,3},{23,7},{27,14},{25,23},{17,28},{8,24},{5,17},{8,10},{15,8},{21,12},{21,18},{16,21},{12,18},{13,14},{17,14}};for(int i=1;i<points.length;i++)line(c,points[i-1][0],points[i-1][1],points[i][0],points[i][1],3,i%3==0?light:accent);}
                else {poly(c,accent,16,2,20,11,30,16,20,20,16,30,12,20,2,16,12,11);poly(c,light,16,4,16,16,5,16,13,12);rect(c,14,14,4,4,METAL);}
            }
            case "banner" -> {
                rect(c,7,5,18,2,METAL);rect(c,15,1,2,30,GOLD);poly(c,GOLD,16,0,19,3,16,6,13,3);
                poly(c,accent,8,8,24,8,24,28,16,24,8,28);rect(c,9,9,2,14,light);rect(c,22,10,2,15,shade);
                poly(c,INK,16,12,20,16,16,20,12,16);rect(c,15,14,2,4,light);
            }
            case "flame" -> {
                poly(c,accent,16,2,18,10,24,5,23,15,28,18,27,25,22,29,10,29,5,24,6,17,11,10,12,17);
                poly(c,light,16,10,18,19,21,16,23,25,18,29,12,28,10,23,12,18);poly(c,METAL,16,19,18,23,18,28,14,28,13,25);
            }
            case "book" -> {
                poly(c,accent,3,7,14,9,16,12,18,9,29,7,29,25,18,27,16,29,14,27,3,25);
                poly(c,METAL,5,9,13,11,15,13,15,25,12,23,5,22);poly(c,light,18,13,20,11,27,9,27,22,20,23,18,25);
                rect(c,15,12,2,17,GOLD);line(c,7,13,12,14,1,shade);line(c,7,17,12,18,1,shade);
            }
            case "skull" -> {
                poly(c,accent,10,5,22,5,27,10,27,20,23,23,22,29,10,29,9,23,5,20,5,10);rect(c,9,7,9,2,light);
                rect(c,8,14,6,5,INK);rect(c,18,14,6,5,INK);poly(c,INK,16,20,19,24,13,24);rect(c,12,27,2,3,shade);rect(c,18,27,2,3,shade);
            }
            case "hourglass","clock" -> {
                rect(c,6,3,20,3,GOLD);rect(c,6,27,20,3,GOLD);line(c,8,6,24,26,2,accent);line(c,24,6,8,26,2,accent);
                poly(c,light,10,7,22,7,16,14);poly(c,accent,16,19,22,26,10,26);rect(c,15,14,2,7,GOLD);
            }
            case "target" -> {
                for(int i=0;i<16;i++){double a=i*Math.PI/8;int px=16+(int)Math.round(Math.cos(a)*12),py=16+(int)Math.round(Math.sin(a)*12);rect(c,px-1,py-1,3,3,accent);}
                rect(c,13,13,6,6,light);line(c,1,16,10,16,2,GOLD);line(c,22,16,30,16,2,GOLD);line(c,16,1,16,10,2,GOLD);line(c,16,22,16,30,2,GOLD);
            }
            case "wolf","hood" -> {
                poly(c,accent,5,4,12,9,16,7,20,9,27,4,26,20,21,28,11,28,6,20);poly(c,shade,16,9,24,13,24,21,19,26,16,26);
                if(icon.equals("hood")) poly(c,INK,16,10,24,19,22,27,10,27,8,19);
                rect(c,9,17,5,2,light);rect(c,18,17,5,2,light);poly(c,METAL,13,24,19,24,16,27);
            }
            case "bow","arcane_bow" -> {
                int[][] points={{8,3},{16,6},{23,12},{25,16},{23,20},{16,26},{8,29}};for(int i=1;i<points.length;i++)line(c,points[i-1][0],points[i-1][1],points[i][0],points[i][1],3,accent);
                line(c,8,4,8,28,1,METAL);line(c,3,16,28,16,2,GOLD);poly(c,light,28,12,32,16,28,20);
                if(icon.equals("arcane_bow")){rect(c,17,2,3,3,light);rect(c,27,25,3,3,light);}
            }
            case "feather","arrow" -> {
                poly(c,accent,26,3,28,7,27,17,18,26,8,27,11,17,20,5);line(c,7,29,26,5,2,GOLD);
                line(c,13,18,20,18,1,light);line(c,18,12,24,12,1,light);line(c,12,22,17,22,1,shade);
            }
            case "poison" -> {
                rect(c,13,2,6,4,GOLD);rect(c,12,6,8,5,METAL);poly(c,accent,12,11,7,17,7,27,11,30,21,30,25,27,25,17,20,11);
                rect(c,9,20,14,7,shade);rect(c,10,16,2,9,light);rect(c,18,23,2,2,light);rect(c,13,26,2,2,light);
            }
            case "bomb" -> {
                line(c,18,9,20,4,2,GOLD);line(c,20,4,25,4,2,GOLD);rect(c,25,1,3,3,light);
                poly(c,accent,11,10,22,10,28,17,28,25,23,30,9,30,4,25,4,17);rect(c,8,15,3,9,light);rect(c,10,13,5,2,light);rect(c,22,24,3,4,shade);
            }
            case "trap" -> {
                poly(c,shade,3,10,7,7,12,11,16,7,21,11,26,7,30,10,27,16,5,16);
                poly(c,accent,3,22,7,26,12,22,16,26,21,22,26,26,30,22,27,17,5,17);rect(c,5,16,23,2,METAL);rect(c,14,14,4,6,GOLD);
            }
            default -> {poly(c,accent,16,4,27,12,24,25,16,30,8,25,5,12);rect(c,14,10,4,15,light);}
        }
        c.getMatrices().pop();
    }
    private static void sword(DrawContext c,int color) {
        poly(c,color,27,2,29,2,29,6,12,24,8,20);line(c,11,20,27,4,1,METAL);
        line(c,6,19,14,27,3,GOLD);line(c,3,29,9,23,3,RpgUiTheme.darken(color,.25f));rect(c,2,27,3,3,GOLD);
    }
    private static void rect(DrawContext c,int x,int y,int w,int h,int color){c.fill(x,y,x+w,y+h,color);}
    private static void line(DrawContext c,int x,int y,int ex,int ey,int thickness,int color) {
        int dx=Math.abs(ex-x),dy=-Math.abs(ey-y),sx=x<ex?1:-1,sy=y<ey?1:-1,e=dx+dy;
        while(true){rect(c,x,y,thickness,thickness,color);if(x==ex&&y==ey)break;int e2=2*e;if(e2>=dy){e+=dy;x+=sx;}if(e2<=dx){e+=dx;y+=sy;}}
    }
    private static void poly(DrawContext c,int color,int... p) {
        for(int y=0;y<32;y++){java.util.ArrayList<Double> xs=new java.util.ArrayList<>();double scan=y+.5;
            for(int i=0,j=p.length-2;i<p.length;j=i,i+=2)if((p[i+1]>scan)!=(p[j+1]>scan))xs.add(p[i]+(scan-p[i+1])*(p[j]-p[i])/(p[j+1]-p[i+1]));
            xs.sort(Double::compare);for(int i=0;i+1<xs.size();i+=2)c.fill((int)Math.ceil(xs.get(i)),y,(int)Math.ceil(xs.get(i+1)),y+1,color);
        }
    }
    public static void scene(DrawContext c,RPGClass clazz,String icon,int x,int y,int w,int h,int accent) {
        // Illustrative voxel vignette, not a screenshot or a promise of a specific skill effect.
        CleanRpgUi.surface(c,x,y,w,h,0xFF273646,CleanRpgUi.BORDER);
        c.fill(x+2,y+h/2,x+w-2,y+h-2,0xFF344A39);
        for(int i=0;i<5;i++){int tx=x+8+i*(w-16)/5;int ty=y+20+(i%2)*13;c.fill(tx+4,ty+17,tx+8,y+h-3,0xFF3A3432);c.fill(tx,ty,tx+13,ty+23,0xFF263D33);}
        int cx=x+w/2-8,cy=y+h-53;c.fill(cx,cy,cx+16,cy+16,0xFFBEB1A0);c.fill(cx+2,cy+5,cx+14,cy+11,0xFF4A4C5A);
        c.fill(cx-3,cy+17,cx+19,cy+35,0xFF777987);c.fill(cx,cy+35,cx+6,cy+49,0xFF292F3B);c.fill(cx+10,cy+35,cx+16,cy+49,0xFF292F3B);
        draw(c,icon,x+w-51,y+9,39,accent);c.fill(cx+18,cy+22,cx+38,cy+25,accent);
    }
    private ReferenceIcons(){}
}
