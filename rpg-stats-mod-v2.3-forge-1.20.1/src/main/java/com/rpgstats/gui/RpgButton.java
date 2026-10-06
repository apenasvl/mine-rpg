package com.rpgstats.gui;

import net.minecraft.client.MinecraftClient;
import com.rpgstats.ClientStatsStore;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Botao customizado para evitar o visual vanilla cinza dentro do menu RPG. */
public class RpgButton extends ButtonWidget {
    public enum Kind { TAB, ACTION, SMALL, CARD, NODE, CHOICE }
    public enum State { DEFAULT, BLOCKED, UNLOCKED, EQUIPPED }

    private final Kind kind;
    private final boolean illustratedStyle;
    private int accent;
    private String subtitle = "";
    private String footer = "";
    private String icon = "";
    private boolean selected = false;
    private String illustration = "";
    public RpgButton illustration(String id) {illustration=id;return this;}
    private State state = State.DEFAULT;

    public RpgButton(int x, int y, int width, int height, Text message, PressAction action, Kind kind, int accent) {
        super(x, y, width, height, message, action, DEFAULT_NARRATION_SUPPLIER);
        this.kind = kind;
        this.illustratedStyle = false;
        this.accent = accent;
    }

    public RpgButton subtitle(String value) {
        this.subtitle = value == null ? "" : value;
        return this;
    }

    public RpgButton footer(String value) {
        this.footer = value == null ? "" : value;
        return this;
    }

    /** Pequeno sigilo procedural; evita depender de assets raster por resolucao. */
    public RpgButton icon(String value) {
        this.icon = value == null ? "" : value;
        return this;
    }

    public RpgButton selected(boolean value) {
        this.selected = value;
        return this;
    }

    public RpgButton state(State value) {
        this.state = value == null ? State.DEFAULT : value;
        return this;
    }

    public RpgButton accent(int value) {
        this.accent = value;
        return this;
    }

    public State state() {
        return state;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        int x = getX();
        int y = getY();

        int pastel=accent;
        int fill=RpgUiTheme.mix(CleanRpgUi.PANEL,accent,selected||state==State.EQUIPPED?.11f:.025f);
        if(kind==Kind.ACTION||kind==Kind.SMALL) fill=0xFF30353D;
        if(hovered&&active) fill=RpgUiTheme.lighten(fill,.07f);
        if(!active||state==State.BLOCKED) fill=0xFF181D22;
        int edge=selected||isFocused()||hovered?pastel:CleanRpgUi.BORDER;
        CleanRpgUi.surface(context,x,y,width,height,fill,edge);
        int titleColor=state==State.BLOCKED?CleanRpgUi.MUTED:CleanRpgUi.TEXT;
        if(kind==Kind.CHOICE) {
            int size=Math.min(66,height-50);
            ReferenceIcons.draw(context,icon,x+(width-size)/2,y+13,size,accent);
            if(!illustration.isBlank() && illustration.matches(".*_[123]$"))
                CleanRpgUi.text(context,font,illustration.substring(illustration.length()-1),x+width-14,y+8,CleanRpgUi.MUTED,8);
            var lines=font.wrapLines(getMessage(),Math.max(1,width-12));
            for(int i=0;i<Math.min(3,lines.size());i++) context.drawText(font,lines.get(i),x+(width-font.getWidth(lines.get(i)))/2,y+height-42+i*11,titleColor,false);
            if(selected) context.fill(x+5,y+height-4,x+width-5,y+height-2,accent);
            return;
        }
        if(kind==Kind.TAB||kind==Kind.SMALL||kind==Kind.ACTION||height<=24) {
            drawCentered(font,context,getMessage().getString(),x+width/2,y+(height-8)/2,titleColor,width-10);
            return;
        }
        int inset=0;
        if(!icon.isBlank()&&width>=76) {
            int size=Math.min(24,height-12);
            ReferenceIcons.draw(context,icon,x+8,y+(height-size)/2,size,accent);
            inset=size+16;
        }
        int cx=x+inset+(width-inset)/2;
        int textWidth=width-inset-10;
        drawCentered(font,context,getMessage().getString(),cx,y+7,titleColor,textWidth);
        if(height>=45) drawCentered(font,context,subtitle,cx,y+21,CleanRpgUi.MUTED,textWidth);
        if(!footer.isBlank()) drawCentered(font,context,footer,cx,y+height-12,CleanRpgUi.MUTED,textWidth);
    }

    private void panel(DrawContext context,int x,int y,int w,int h,int bg,int border) {
        CleanRpgUi.panel(context,x,y,w,h,border);
    }

    private static void drawCentered(TextRenderer font, DrawContext context, String raw, int cx, int y, int color, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (font.getWidth(text) > maxWidth) {
            String trimmed = font.trimToWidth(text, Math.max(0, maxWidth - font.getWidth("...")));
            text = trimmed + "...";
        }
        context.drawText(font,text,cx-font.getWidth(text)/2,y,color,false);
    }
}
