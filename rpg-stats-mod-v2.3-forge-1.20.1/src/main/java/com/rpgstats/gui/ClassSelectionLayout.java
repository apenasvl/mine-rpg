package com.rpgstats.gui;

/** Fits the reference canvas into GUI coordinates, preserving art and native hitboxes. */
public record ClassSelectionLayout(double scale, int x, int y) {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 450;

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }
        public boolean overlaps(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }

    public static ClassSelectionLayout fit(int width, int height) {
        double scale = Math.min(Math.max(1, width) / (double) WIDTH, Math.max(1, height) / (double) HEIGHT);
        return new ClassSelectionLayout(scale, (int) ((width - WIDTH * scale) / 2),
                (int) ((height - HEIGHT * scale) / 2));
    }

    public static Rect sourceCard(int index) {
        if (index < 0 || index > 3) throw new IllegalArgumentException("Class card index: " + index);
        return new Rect(34 + index * 186, 100, 174, 74);
    }

    public Rect project(Rect source) {
        int left = x + (int) Math.round(source.x * scale);
        int top = y + (int) Math.round(source.y * scale);
        int right = x + (int) Math.round(source.right() * scale);
        int bottom = y + (int) Math.round(source.bottom() * scale);
        return new Rect(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    public Rect card(int index) { return project(sourceCard(index)); }
    public Rect confirm() { return project(new Rect(580, 384, 186, 32)); }
}
