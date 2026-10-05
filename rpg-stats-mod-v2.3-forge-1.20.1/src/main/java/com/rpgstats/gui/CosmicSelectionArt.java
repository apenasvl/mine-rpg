package com.rpgstats.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Text-free atlas: each card remains a separate widget and all labels are live text. */
final class CosmicSelectionArt {
    static final Identifier ATLAS = new Identifier("rpgstats", "textures/gui/class_selection/cosmic_atlas.png");
    static final int TEXT = 0xFFD5F2FF;
    static final int CYAN = 0xFF67DFFF;

    static void texture(DrawContext context, ClassSelectionLayout.Rect source) {
        context.drawTexture(ATLAS, 0, 0, source.x(), source.y(), source.width(), source.height(),
                ClassSelectionLayout.WIDTH, ClassSelectionLayout.HEIGHT);
    }

    static void begin(DrawContext context, ClassSelectionLayout.Rect target, ClassSelectionLayout.Rect source) {
        context.getMatrices().push();
        context.getMatrices().translate(target.x(), target.y(), 0);
        context.getMatrices().scale(target.width() / (float) source.width(), target.height() / (float) source.height(), 1);
    }

    static void label(DrawContext context, TextRenderer font, String value, int x, int y, float size, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(size, size, 1);
        context.drawText(font, Text.literal(value), 0, 0, color, false);
        context.getMatrices().pop();
    }

    static void centered(DrawContext context, TextRenderer font, String value, int cx, int y,
                         float size, int color, int maxWidth) {
        float fit = Math.min(size, maxWidth / (float) Math.max(1, font.getWidth(value)));
        label(context, font, value, Math.round(cx - font.getWidth(value) * fit / 2), y, fit, color);
    }

    static void wrapped(DrawContext context, TextRenderer font, String value, int x, int y,
                        float size, int color, int width, int maxLines) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(size, size, 1);
        var lines = font.wrapLines(Text.literal(value), (int) (width / size));
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            context.drawText(font, lines.get(i), 0, i * 10, color, false);
        }
        context.getMatrices().pop();
    }

    static void outline(DrawContext context, int width, int height, int color) {
        // Stepped corners follow the original pixel-art frame; no fullscreen shader needed.
        context.fill(18, 4, width - 18, 7, color);
        context.fill(18, height - 7, width - 18, height - 4, color);
        context.fill(4, 18, 7, height - 18, color);
        context.fill(width - 7, 18, width - 4, height - 18, color);
        for (int i = 0; i < 4; i++) {
            int p = 5 + i * 3;
            context.fill(p, 17 - i * 3, p + 3, 20 - i * 3, color);
            context.fill(width - p - 3, 17 - i * 3, width - p, 20 - i * 3, color);
            context.fill(p, height - 20 + i * 3, p + 3, height - 17 + i * 3, color);
            context.fill(width - p - 3, height - 20 + i * 3, width - p, height - 17 + i * 3, color);
        }
    }

    private CosmicSelectionArt() {}
}
