package com.rpgstats.gui;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import net.minecraft.client.gui.DrawContext;

/** Paleta e primitivas visuais compartilhadas pela UI do RPG. */
public final class RpgUiTheme {
    public static final int SCREEN_TOP = 0xD90A0D16;
    public static final int SCREEN_BOTTOM = 0xED111827;
    public static final int PANEL = 0xF21A2030;
    public static final int PANEL_SOFT = 0xE9242B3D;
    public static final int PANEL_DARK = 0xF4141824;
    public static final int BORDER = 0xFF39425A;
    public static final int BORDER_SOFT = 0xFF30384D;
    public static final int TEXT = 0xFFF2F5FF;
    public static final int INK = 0xFF11141B;
    public static final int MUTED = 0xFFA7B0C4;
    public static final int DIM = 0xFF737D94;
    public static final int SUCCESS = 0xFF58D68D;
    public static final int WARNING = 0xFFFFC857;
    public static final int DANGER = 0xFFFF667A;
    public static final int BLUE = 0xFF67A8FF;

    /**
     * Cores-base de classe propositalmente mais suaves que neon puro.
     * A classe continua reconhecivel sem transformar a tela inteira em uma cor saturada.
     */
    public static int accent(RPGClass clazz) {
        if (clazz == null) return 0xFFAEBBDF;
        return switch (clazz) {
            case GUERREIRO -> 0xFF985C5A; // vermelho queimado pastel
            case MAGO -> 0xFF8EBBFF;      // azul arcano pastel
            case ARQUEIRO -> 0xFF91D1A4;  // verde natural pastel
            case ASSASSINO -> 0xFF6F5AA8; // roxo escuro / azul-noite
        };
    }

    /**
     * Neutro escuro da classe. Ele tinge os paineis sem competir com a Casa.
     * O Assassino usa grafite/azul-noite para manter a identidade mais furtiva.
     */
    public static int classNeutral(RPGClass clazz) {
        if (clazz == null) return 0xFF171B26;
        return switch (clazz) {
            case GUERREIRO -> 0xFF241A1D;
            case MAGO -> 0xFF171F2D;
            case ARQUEIRO -> 0xFF18241E;
            case ASSASSINO -> 0xFF232733;
        };
    }

    /** Identidade visual de cada Casa. A Casa nunca muda a classe do jogador. */
    public static int houseAccent(RPGPath path) {
        if (path == null) return 0xFFAEBBDF;
        return switch (path) {
            // MAGO — azul como familia, com Casas puxando para suas escolas.
            case MAGE_ELEMENTAL -> 0xFFF3A58E;    // coral/fogo suave
            case MAGE_ARCANA -> 0xFFB9ACF2;      // lavanda arcana
            case MAGE_CONJURATION -> 0xFF91D6BE; // menta eterea
            case MAGE_OCCULT -> 0xFFD89ABF;      // malva oculto
            case MAGE_TEMPORAL -> 0xFF8FCEEE;    // ciano temporal

            // GUERREIRO — vermelho/ferro, variando entre defesa, furia e tecnica.
            case WAR_VANGUARD -> 0xFF985C5A;     // rosa-ferro defensivo
            case WAR_BERSERKER -> 0xFFAD414B;    // carmesim mais vivo
            case WAR_WEAPONMASTER -> 0xFFA36A46; // terracota/aco quente
            case WAR_RUNIC -> 0xFF79588F;        // violeta runico
            case WAR_COMMANDER -> 0xFF8D515D;    // vermelho nobre

            // ARQUEIRO — verdes naturais, com variacoes de mobilidade/magia/engenharia.
            case ARC_MARKSMAN -> 0xFFA5D3A2;     // verde-salvia preciso
            case ARC_WARDEN -> 0xFF82C69D;       // verde floresta suave
            case ARC_SKIRMISHER -> 0xFF98D0B7;   // verde-agua de mobilidade
            case ARC_ARCANE -> 0xFF8BBFC2;       // teal magico
            case ARC_ARTIFICER -> 0xFFBEC184;    // oliva/bronze claro

            // ASSASSINO — roxo escuro + azul-noite + grafite.
            case ASS_SHADOW -> 0xFF53679C;       // azul-noite frio
            case ASS_VENOM -> 0xFF708A5A;        // verde toxina dessaturado
            case ASS_DUELIST -> 0xFF79659B;      // roxo elegante escuro
            case ASS_SABOTEUR -> 0xFF686876;     // grafite azulado
            case ASS_MYSTIC -> 0xFF66558F;       // violeta profundo
        };
    }

    /**
     * Cor dominante da tela.
     * Classe fornece a familia; Casa principal assume o protagonismo; Casa 2 entra apenas como assinatura.
     * A ordem importa: Temporal + Elemental nunca fica igual a Elemental + Temporal.
     */
    public static int themedAccent(RPGClass clazz, RPGPath primary, RPGPath affinity) {
        int base = primary == null ? accent(clazz) : houseAccent(primary);
        return affinity == null ? base : mix(base, houseAccent(affinity), 0.25f);
    }

    public static int primaryAccent(RPGClass clazz, RPGPath primary) {
        return primary == null ? accent(clazz) : houseAccent(primary);
    }

    public static int affinityAccent(RPGPath affinity) {
        return affinity == null ? 0 : houseAccent(affinity);
    }

    public static int screenTop(RPGClass clazz, RPGPath primary, RPGPath affinity) {
        int neutral = classNeutral(clazz);
        return alpha(mix(neutral, darken(themedAccent(clazz, primary, affinity), 0.58f), 0.22f), 236);
    }

    public static int screenBottom(RPGClass clazz, RPGPath primary, RPGPath affinity) {
        int neutral = darken(classNeutral(clazz), 0.10f);
        return alpha(mix(neutral, darken(themedAccent(clazz, primary, affinity), 0.72f), 0.14f), 244);
    }

    public static int panelTint(int basePanel, RPGClass clazz, RPGPath primary, RPGPath affinity, float amount) {
        int neutralized = mix(basePanel, classNeutral(clazz), clazz == RPGClass.ASSASSINO ? 0.24f : 0.14f);
        return mix(neutralized, themedAccent(clazz, primary, affinity), amount);
    }

    public static int accentDark(RPGClass clazz) {
        return darken(accent(clazz), 0.45f);
    }

    /** WCAG AA para texto pequeno. A fonte do Minecraft e compacta, entao usamos 4.5:1. */
    public static final double MIN_TEXT_CONTRAST = 4.5d;

    /** Escolhe automaticamente texto claro ou escuro para um fundo solido. */
    public static int contrastText(int background) {
        int light = TEXT;
        int dark = INK;
        return contrastRatio(light, background) >= contrastRatio(dark, background) ? light : dark;
    }

    /**
     * Texto tematico: preserva um pouco da cor da classe/Casa em vez de cair direto em branco/preto.
     * Em fundo escuro tende a virar um pastel frio/quente; em fundo claro, um grafite tingido.
     */
    public static int themedText(int background, int accent) {
        int lightTint = mix(TEXT, accent, 0.22f);
        int darkTint = mix(INK, accent, 0.18f);

        double lightRatio = contrastRatio(lightTint, background);
        double darkRatio = contrastRatio(darkTint, background);

        if (lightRatio >= MIN_TEXT_CONTRAST || darkRatio >= MIN_TEXT_CONTRAST) {
            if (lightRatio >= MIN_TEXT_CONTRAST && darkRatio >= MIN_TEXT_CONTRAST) {
                return lightRatio >= darkRatio ? lightTint : darkTint;
            }
            return lightRatio >= MIN_TEXT_CONTRAST ? lightTint : darkTint;
        }

        // Mantem o hue do acento e vai aproximando do claro/escuro ate atingir AA.
        int lighter = accessibleAccent(accent, background);
        if (contrastRatio(lighter, background) >= MIN_TEXT_CONTRAST) return lighter;

        // Ultimo fallback apenas quando a paleta nao consegue atingir o contraste exigido.
        return contrastText(background);
    }

    /**
     * Mantem a identidade do acento, mas garante que ele continue legivel quando usado como TEXTO.
     * Bordas, barras e fundos continuam usando a cor original.
     */
    public static int accessibleAccent(int accent, int background) {
        if (contrastRatio(accent, background) >= MIN_TEXT_CONTRAST) return accent;

        int best = accent;
        double bestRatio = contrastRatio(accent, background);
        for (int i = 1; i <= 10; i++) {
            float amount = i / 10f;
            int lighter = mix(accent, TEXT, amount);
            double lightRatio = contrastRatio(lighter, background);
            if (lightRatio >= MIN_TEXT_CONTRAST) return lighter;
            if (lightRatio > bestRatio) {
                best = lighter;
                bestRatio = lightRatio;
            }

            int darker = mix(accent, INK, amount);
            double darkRatio = contrastRatio(darker, background);
            if (darkRatio >= MIN_TEXT_CONTRAST) return darker;
            if (darkRatio > bestRatio) {
                best = darker;
                bestRatio = darkRatio;
            }
        }
        return best;
    }

    /** Cor final de um layer ARGB semitransparente sobre um fundo opaco. */
    public static int composite(int foreground, int background) {
        float a = ((foreground >>> 24) & 0xFF) / 255f;
        int fr = (foreground >>> 16) & 0xFF;
        int fg = (foreground >>> 8) & 0xFF;
        int fb = foreground & 0xFF;
        int br = (background >>> 16) & 0xFF;
        int bg = (background >>> 8) & 0xFF;
        int bb = background & 0xFF;
        int r = Math.round(fr * a + br * (1f - a));
        int g = Math.round(fg * a + bg * (1f - a));
        int b = Math.round(fb * a + bb * (1f - a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static double contrastRatio(int first, int second) {
        double l1 = relativeLuminance(first);
        double l2 = relativeLuminance(second);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05d) / (darker + 0.05d);
    }

    private static double relativeLuminance(int color) {
        double r = srgb(((color >>> 16) & 0xFF) / 255d);
        double g = srgb(((color >>> 8) & 0xFF) / 255d);
        double b = srgb((color & 0xFF) / 255d);
        return 0.2126d * r + 0.7152d * g + 0.0722d * b;
    }

    private static double srgb(double channel) {
        return channel <= 0.04045d
                ? channel / 12.92d
                : Math.pow((channel + 0.055d) / 1.055d, 2.4d);
    }

    /** Mistura ARGB linear; weightB=0 usa a, weightB=1 usa b. */
    public static int mix(int a, int b, float weightB) {
        float t = Math.max(0f, Math.min(1f, weightB));
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int oa = Math.round(aa + (ba - aa) * t);
        int or = Math.round(ar + (br - ar) * t);
        int og = Math.round(ag + (bg - ag) * t);
        int ob = Math.round(ab + (bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    /** Barra principal 75/25: a Casa principal domina e a afinidade aparece como assinatura secundaria. */
    public static void dualAccentBar(DrawContext context, int x, int y, int w, int h, int primary, int affinity) {
        if (w <= 0 || h <= 0) return;
        if (affinity == 0) {
            context.fill(x, y, x + w, y + h, primary);
            return;
        }
        int split = x + Math.max(1, Math.round(w * 0.75f));
        context.fill(x, y, split, y + h, primary);
        context.fill(split, y, x + w, y + h, affinity);
        int blend = mix(primary, affinity, 0.50f);
        context.fill(Math.max(x, split - 3), y, Math.min(x + w, split + 3), y + h, blend);
    }

    public static int darken(int argb, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int) (r * (1f - amount));
        g = (int) (g * (1f - amount));
        b = (int) (b * (1f - amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lighten(int argb, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int alpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    /** Retangulo com cantos de 2px, suficiente para manter o estilo pixel-art do Minecraft. */
    public static void roundRect(DrawContext context, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        context.fill(x + 2, y, x + w - 2, y + h, color);
        context.fill(x, y + 2, x + w, y + h - 2, color);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    public static void panel(DrawContext context, int x, int y, int w, int h, int background, int border) {
        roundRect(context, x + 2, y + 3, w, h, 0x65000000);
        roundRect(context, x, y, w, h, border);
        roundRect(context, x + 1, y + 1, w - 2, h - 2, background);
    }

    public static void line(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2) {
            context.fill(x1, Math.min(y1, y2), x1 + 2, Math.max(y1, y2) + 1, color);
        } else if (y1 == y2) {
            context.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 2, color);
        }
    }

    public static void bar(DrawContext context, int x, int y, int w, int h, float ratio, int color) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        roundRect(context, x, y, w, h, 0xFF0E121C);
        int fill = Math.max(0, (int) ((w - 2) * ratio));
        if (fill > 0) roundRect(context, x + 1, y + 1, fill, Math.max(2, h - 2), color);
    }

    /**
     * Sigilos vetoriais/pixel-art. Sao intencionalmente simples para escalar bem com o GUI scale
     * do Minecraft sem depender de PNGs separados.
     */
    public static void drawIcon(DrawContext context, String icon, int x, int y, int size, int color) {
        if (icon == null || icon.isBlank() || size < 6) return;
        int s = Math.max(6, size);
        int midX = x + s / 2;
        int midY = y + s / 2;
        int t = Math.max(1, s / 8);
        int soft = alpha(color, 95);

        switch (icon) {
            case "stats" -> {
                context.fill(x + t, y + s / 2, x + 2 * t, y + s - t, color);
                context.fill(x + 3 * t, y + s / 3, x + 4 * t, y + s - t, color);
                context.fill(x + 5 * t, y + t, x + 6 * t, y + s - t, color);
            }
            case "class" -> {
                context.fill(midX - t, y + t, midX + t, y + s - t, color);
                context.fill(x + t, midY - t, x + s - t, midY + t, color);
                context.fill(midX - 2 * t, y + 2 * t, midX + 2 * t, y + 3 * t, soft);
            }
            case "house", "clock" -> {
                context.fill(midX - t, y + t, midX + t, y + 3 * t, color);
                context.fill(midX - t, y + s - 3 * t, midX + t, y + s - t, color);
                context.fill(x + t, midY - t, x + 3 * t, midY + t, color);
                context.fill(x + s - 3 * t, midY - t, x + s - t, midY + t, color);
                context.fill(midX, midY - t, midX + t, y + 2 * t, color);
                context.fill(midX, midY, x + s - 2 * t, midY + t, color);
            }
            case "spec", "star" -> {
                context.fill(midX - t, y, midX + t, y + s, color);
                context.fill(x, midY - t, x + s, midY + t, color);
                context.fill(midX - 2 * t, midY - 2 * t, midX + 2 * t, midY + 2 * t, soft);
            }
            case "affinity", "link" -> {
                context.fill(x + t, y + 2 * t, midX, y + 3 * t, color);
                context.fill(x + t, y + 2 * t, x + 2 * t, midY + t, color);
                context.fill(midX, y + s - 3 * t, x + s - t, y + s - 2 * t, color);
                context.fill(x + s - 2 * t, midY - t, x + s - t, y + s - 2 * t, color);
                context.fill(midX - t, midY - t, midX + t, midY + t, soft);
            }
            case "flame" -> {
                context.fill(midX - t, y + t, midX + t, midY, color);
                context.fill(midX - 2 * t, midY - t, midX + 2 * t, y + s - t, soft);
                context.fill(midX - t, midY, midX + t, y + s - t, color);
            }
            case "shield" -> {
                context.fill(x + 2 * t, y + t, x + s - 2 * t, y + 2 * t, color);
                context.fill(x + 2 * t, y + 2 * t, x + 3 * t, y + s - 3 * t, color);
                context.fill(x + s - 3 * t, y + 2 * t, x + s - 2 * t, y + s - 3 * t, color);
                context.fill(midX - t, y + s - 3 * t, midX + t, y + s - t, color);
            }
            case "bow" -> {
                context.fill(x + 2 * t, y + t, x + 3 * t, y + s - t, color);
                context.fill(x + 3 * t, y + t, x + s - 2 * t, y + 2 * t, soft);
                context.fill(x + 3 * t, y + s - 2 * t, x + s - 2 * t, y + s - t, soft);
                context.fill(midX, midY - t, x + s - t, midY + t, color);
            }
            case "dagger" -> {
                context.fill(midX - t, y + t, midX + t, y + s - 3 * t, color);
                context.fill(midX - 2 * t, y + s - 3 * t, midX + 2 * t, y + s - 2 * t, color);
                context.fill(midX - t, y + s - 2 * t, midX + t, y + s, color);
            }
            case "sun" -> {
                context.fill(midX - 2 * t, midY - 2 * t, midX + 2 * t, midY + 2 * t, soft);
                context.fill(midX - t, y, midX + t, y + s, color);
                context.fill(x, midY - t, x + s, midY + t, color);
            }
            case "swirl" -> {
                context.fill(x + t, y + t, x + s - 2 * t, y + 2 * t, color);
                context.fill(x + t, y + 2 * t, x + 2 * t, y + s - 2 * t, color);
                context.fill(x + 2 * t, y + s - 2 * t, x + s - t, y + s - t, color);
                context.fill(x + s - 2 * t, midY - t, x + s - t, y + s - 2 * t, color);
                context.fill(midX - t, midY - t, x + s - 2 * t, midY + t, soft);
            }
            case "crystal" -> {
                context.fill(midX - t, y, midX + t, y + s, color);
                context.fill(midX - 2 * t, y + 2 * t, midX + 2 * t, y + s - 2 * t, soft);
                context.fill(x + t, midY - t, x + s - t, midY + t, color);
            }
            case "loop" -> {
                context.fill(x + t, y + 2 * t, x + s - t, y + 3 * t, color);
                context.fill(x + t, y + s - 3 * t, x + s - t, y + s - 2 * t, color);
                context.fill(x + t, y + 3 * t, x + 2 * t, y + s - 3 * t, color);
                context.fill(x + s - 2 * t, y + 3 * t, x + s - t, y + s - 3 * t, color);
            }
            case "arrow" -> {
                context.fill(x + t, midY - t, x + s - 2 * t, midY + t, color);
                context.fill(x + s - 4 * t, midY - 3 * t, x + s - 2 * t, midY + 3 * t, soft);
                context.fill(x + s - 3 * t, midY - 2 * t, x + s - t, midY + 2 * t, color);
            }
            default -> {
                context.fill(midX - t, y + t, midX + t, y + s - t, color);
                context.fill(x + t, midY - t, x + s - t, midY + t, color);
                context.fill(midX - 2 * t, midY - 2 * t, midX + 2 * t, midY + 2 * t, soft);
            }
        }
    }

    /** Fundo sutil de arvore: lembra um portal/sigilo sem competir com os cards. */
    public static void treeSigil(DrawContext context, int x, int y, int w, int h, int primary, int affinity) {
        if (w < 80 || h < 60) return;
        int cx = x + w / 2;
        int cy = y + h / 2 + 4;
        int blend = affinity == 0 ? primary : mix(primary, affinity, 0.28f);

        for (int i = 0; i < 4; i++) {
            int insetX = 26 + i * 18;
            int insetY = 16 + i * 11;
            int left = x + insetX;
            int right = x + w - insetX;
            int top = y + insetY;
            int bottom = y + h - insetY;
            if (right <= left || bottom <= top) break;
            int c = alpha(i % 2 == 0 ? primary : blend, 18 - i * 2);
            context.fill(left, top, right, top + 1, c);
            context.fill(left, bottom - 1, right, bottom, c);
            context.fill(left, top, left + 1, bottom, c);
            context.fill(right - 1, top, right, bottom, c);
        }

        context.fill(cx, y + 14, cx + 1, y + h - 12, alpha(primary, 18));
        context.fill(x + 18, cy, x + w - 18, cy + 1, alpha(blend, 15));
        drawIcon(context, "star", cx - 12, cy - 12, 24, alpha(lighten(blend, 0.12f), 48));
    }

    private RpgUiTheme() {}
}
