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
    public enum Kind { TAB, ACTION, SMALL, CARD, NODE }
    public enum State { DEFAULT, BLOCKED, UNLOCKED, EQUIPPED }

    private final Kind kind;
    private final boolean illustratedStyle;
    private int accent;
    private String subtitle = "";
    private String footer = "";
    private String icon = "";
    private boolean selected = false;
    private State state = State.DEFAULT;

    public RpgButton(int x, int y, int width, int height, Text message, PressAction action, Kind kind, int accent) {
        super(x, y, width, height, message, action, DEFAULT_NARRATION_SUPPLIER);
        this.kind = kind;
        this.illustratedStyle = CharacterArt.supported(ClientStatsStore.stats.clazz);
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

        if (kind == Kind.TAB) {
            int bg = selected ? RpgUiTheme.alpha(accent, 46) : (hovered ? 0xB5363E52 : 0x96303949);
            int border = selected ? accent : (hovered ? RpgUiTheme.lighten(RpgUiTheme.BORDER, 0.18f) : RpgUiTheme.BORDER_SOFT);
            panel(context, x, y, width, height, bg, border);
            int tabBg = RpgUiTheme.composite(bg, RpgUiTheme.PANEL_DARK);
            int tabText = active ? RpgUiTheme.themedText(tabBg, accent) : RpgUiTheme.DIM;
            int tabAccentText = active ? RpgUiTheme.accessibleAccent(accent, tabBg) : RpgUiTheme.DIM;
            if (selected) {
                context.fill(x + 5, y + height - 2, x + width - 5, y + height, accent);
                context.fill(x + 5, y + 1, x + width - 5, y + 2, RpgUiTheme.alpha(RpgUiTheme.lighten(accent, 0.25f), 170));
            }
            if (!icon.isBlank() && width >= 54) {
                int size = Math.min(12, height - 6);
                RpgUiTheme.drawIcon(context, icon, x + 6, y + (height - size) / 2, size,
                        tabAccentText);
                drawCentered(font, context, getMessage().getString(), x + width / 2 + 5, y + (height - 8) / 2,
                        tabText, width - 28);
            } else {
                drawCentered(font, context, getMessage().getString(), x + width / 2, y + (height - 8) / 2,
                        tabText, width - 10);
            }
            return;
        }

        if (kind == Kind.SMALL || kind == Kind.ACTION) {
            int base = active ? (hovered ? RpgUiTheme.lighten(accent, 0.12f) : accent) : 0xFF4A5162;
            int bg = active ? RpgUiTheme.alpha(base, kind == Kind.SMALL ? 115 : 145) : 0xA6323745;
            int border = active ? base : 0xFF555D70;
            panel(context, x, y, width, height, bg, border);
            int effectiveBg = RpgUiTheme.composite(bg, RpgUiTheme.PANEL_DARK);
            int actionText = active ? RpgUiTheme.themedText(effectiveBg, base) : RpgUiTheme.MUTED;
            drawCentered(font, context, getMessage().getString(), x + width / 2, y + (height - 8) / 2,
                    actionText, width - 8);
            return;
        }

        int stateAccent = switch (state) {
            case EQUIPPED -> illustratedStyle ? RpgUiTheme.lighten(accent,.25f) : RpgUiTheme.BLUE;
            case UNLOCKED -> illustratedStyle ? accent : RpgUiTheme.SUCCESS;
            case BLOCKED -> illustratedStyle ? RpgUiTheme.darken(accent,.48f) : 0xFF687086;
            case DEFAULT -> accent;
        };
        int bg = switch (state) {
            case EQUIPPED -> 0xE9223552;
            case UNLOCKED -> 0xDF20382F;
            case BLOCKED -> 0xD6222733;
            case DEFAULT -> 0xE92B3141;
        };
        if (hovered && active) bg = RpgUiTheme.lighten(bg, 0.09f);
        int border = active || state == State.UNLOCKED || state == State.EQUIPPED ? stateAccent : 0xFF464E61;
        panel(context, x, y, width, height, bg, border);
        int effectiveStateBg = RpgUiTheme.composite(bg, RpgUiTheme.PANEL_DARK);
        int readableStateAccent = RpgUiTheme.accessibleAccent(stateAccent, effectiveStateBg);
        int themedTitleText = state == State.BLOCKED
                ? RpgUiTheme.MUTED : RpgUiTheme.themedText(effectiveStateBg, stateAccent);

        if (kind == Kind.CARD) {
            context.fill(x + 1, y + 1, x + 4, y + height - 1, stateAccent);
            int iconPad = !icon.isBlank() && height >= 42 ? Math.min(30, height - 12) : 0;
            if (iconPad > 0) {
                int ix = x + 10;
                int iy = y + (height - iconPad) / 2;
                panel(context, ix - 3, iy - 3, iconPad + 6, iconPad + 6,
                        RpgUiTheme.alpha(stateAccent, 28), RpgUiTheme.alpha(stateAccent, 115));
                if(illustratedStyle) {
                    RPGPath house=java.util.Arrays.stream(RPGPath.values()).filter(p -> p.parent==ClientStatsStore.stats.clazz && RpgUiTheme.houseAccent(p)==accent).findFirst().orElse(ClientStatsStore.stats.path);
                    RPGSpecialization spec=java.util.Arrays.stream(RPGSpecialization.values()).filter(p -> p.display.equals(getMessage().getString())).findFirst().orElse(null);
                    CharacterArt.emblem(context,ClientStatsStore.stats.clazz,house,spec,ix-2,iy-2,iconPad+4,1);
                } else RpgUiTheme.drawIcon(context, icon, ix, iy, iconPad, readableStateAccent);
            }
            int textX = x + (iconPad > 0 ? iconPad + 17 : 0);
            int textW = width - (iconPad > 0 ? iconPad + 20 : 0);
            int titleY = height < 68 ? y + 8 : y + 11;
            int subtitleY = height < 68 ? y + 22 : y + 31;
            int footerY = height < 68 ? y + height - 12 : y + height - 15;
            drawCentered(font, context, getMessage().getString(), textX + textW / 2, titleY,
                    active ? themedTitleText : RpgUiTheme.MUTED, textW - 14);
            drawCentered(font, context, subtitle, textX + textW / 2, subtitleY, RpgUiTheme.MUTED, textW - 12);
            drawCentered(font, context, footer, textX + textW / 2, footerY, readableStateAccent, textW - 12);
            return;
        }

        // NODE
        if (!icon.isBlank() && height >= 28 && width >= 76) {
            int size = Math.min(20, height - 8);
            int ix = x + 7;
            int iy = y + (height - size) / 2;
            panel(context, ix - 2, iy - 2, size + 4, size + 4,
                    RpgUiTheme.alpha(stateAccent, state == State.BLOCKED ? 18 : 34),
                    RpgUiTheme.alpha(stateAccent, state == State.BLOCKED ? 65 : 125));
            RpgUiTheme.drawIcon(context, icon, ix, iy, size,
                    state == State.BLOCKED ? RpgUiTheme.DIM : readableStateAccent);
        }
        if (state == State.EQUIPPED) {
            context.fill(x + 4, y + 3, x + width - 4, y + 5, RpgUiTheme.BLUE);
        } else if (state == State.UNLOCKED) {
            context.fill(x + 4, y + 3, x + width - 4, y + 5, RpgUiTheme.SUCCESS);
        }
        int nodeInset = !icon.isBlank() && height >= 28 && width >= 76 ? Math.min(31, height + 1) : 0;
        int nodeCx = x + nodeInset + (width - nodeInset) / 2;
        int nodeW = width - nodeInset - 8;
        if (height <= 24) {
            drawCentered(font, context, getMessage().getString(), x + width / 2, y + (height - 8) / 2,
                    state == State.BLOCKED ? RpgUiTheme.MUTED : RpgUiTheme.TEXT, width - 8);
        } else if (height <= 38 || (illustratedStyle && height <= 44)) {
            drawCentered(font, context, getMessage().getString(), nodeCx, y + 5,
                    state == State.BLOCKED ? RpgUiTheme.MUTED : themedTitleText, nodeW);
            drawCentered(font, context, footer, nodeCx, y + height - 11, readableStateAccent, nodeW);
        } else {
            drawCentered(font, context, getMessage().getString(), nodeCx, y + 8,
                    state == State.BLOCKED ? RpgUiTheme.MUTED : themedTitleText, nodeW);
            drawCentered(font, context, subtitle, nodeCx, y + 22, RpgUiTheme.MUTED, nodeW);
            drawCentered(font, context, footer, nodeCx, y + height - 12, readableStateAccent, nodeW);
        }
    }

    private void panel(DrawContext context,int x,int y,int w,int h,int bg,int border) {
        if(illustratedStyle) MageArt.frame(context,x,y,w,h,border);
        else RpgUiTheme.panel(context,x,y,w,h,bg,border);
    }

    private static void drawCentered(TextRenderer font, DrawContext context, String raw, int cx, int y, int color, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (font.getWidth(text) > maxWidth) {
            String trimmed = font.trimToWidth(text, Math.max(0, maxWidth - font.getWidth("...")));
            text = trimmed + "...";
        }
        context.drawCenteredTextWithShadow(font, text, cx, y, color);
    }
}
