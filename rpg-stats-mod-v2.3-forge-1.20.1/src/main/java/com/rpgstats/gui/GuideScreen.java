package com.rpgstats.gui;

import com.rpgstats.ClientStatsStore;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.ability.ClassAbilityRegistry;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.tree.SkillNode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Codex deterministico do RPG Stats.
 *
 * A versao anterior usava clickEvent CHANGE_PAGE do WrittenBook vanilla. Com quase 100
 * paginas e muito texto formatado, hitboxes invisiveis podiam interceptar cliques em paginas
 * cheias. Aqui a navegacao e controlada exclusivamente por botoes reais da Screen.
 */
public final class GuideScreen extends Screen {
    private static final int PAPER = 0xFFF1E3BF;
    private static final int PAPER_DARK = 0xFFE0C995;
    private static final int INK = 0xFF2B2118;
    private static final int MUTED = 0xFF6A5842;
    private static final int EDGE = 0xFF7A5E3C;

    private String pageKey = "index";
    private final List<String> history = new ArrayList<>();
    private int rightScroll,rightMaxScroll,maxRenderedY;

    public GuideScreen() {
        super(Text.literal("Codex do RPG Stats"));
    }

    @Override
    protected void init() {
        clearChildren();
        addGlobalNavigation();
        addPageButtons();
    }

    private void addGlobalNavigation() {
        BookRect b = book();
        addDrawableChild(new RpgButton(b.x + 18, b.y + b.h - 28, 72, 18,
                Text.literal("Índice"), btn -> open("index"), RpgButton.Kind.SMALL, 0xFF8A6A3E));
        addDrawableChild(new RpgButton(b.x + 96, b.y + b.h - 28, 72, 18,
                Text.literal("Voltar"), btn -> back(), RpgButton.Kind.SMALL, 0xFF8A6A3E));

        List<String> order = orderedKeys();
        int at = order.indexOf(pageKey);
        RpgButton prev = new RpgButton(b.x + b.w - 170, b.y + b.h - 28, 72, 18,
                Text.literal("◀ Ant."), btn -> {
                    if (at > 0) openDirect(order.get(at - 1));
                }, RpgButton.Kind.SMALL, 0xFF8A6A3E);
        prev.active = at > 0;
        addDrawableChild(prev);

        RpgButton next = new RpgButton(b.x + b.w - 92, b.y + b.h - 28, 72, 18,
                Text.literal("Próx. ▶"), btn -> {
                    if (at >= 0 && at + 1 < order.size()) openDirect(order.get(at + 1));
                }, RpgButton.Kind.SMALL, 0xFF8A6A3E);
        next.active = at >= 0 && at + 1 < order.size();
        addDrawableChild(next);
    }

    private void addPageButtons() {
        BookRect b = book();
        int leftX = b.x + 24;
        int leftY = b.y + 72;
        int leftW = b.pageW - 48;

        if (pageKey.startsWith("corruption:")) {
            int y = leftY;
            for (int i = 0; i < MageCorruptionText.guidePages().size(); i++) {
                final int section = i;
                String label = MageCorruptionText.guidePages().get(i).title().replace("CORRUPÇÃO: ", "");
                RpgButton button = new RpgButton(leftX, y, leftW, 24, Text.literal(label),
                        btn -> open(corruptionKey(section)), RpgButton.Kind.ACTION,
                        RpgUiTheme.houseAccent(RPGPath.MAGE_OCCULT));
                button.active = !pageKey.equals(corruptionKey(i));
                addDrawableChild(button);
                y += 29;
            }
            addIndexButton("Casa Ocultista", houseKey(RPGPath.MAGE_OCCULT), leftX, y + 5, leftW);
            return;
        }

        if ("index".equals(pageKey)) {
            int y = leftY;
            addIndexButton("★ Minha build", "current", leftX, y, leftW); y += 27;
            addIndexButton("• Progressão", "progression", leftX, y, leftW); y += 27;
            addIndexButton("• Casa secundária", "secondary", leftX, y, leftW); y += 35;
            for (RPGClass clazz : RPGClass.values()) {
                int accent = RpgUiTheme.accent(clazz);
                addDrawableChild(new RpgButton(leftX, y, leftW, 24, Text.literal(clazz.display),
                        btn -> open(classKey(clazz)), RpgButton.Kind.ACTION, accent));
                y += 29;
            }
            return;
        }

        if (pageKey.startsWith("class:")) {
            RPGClass clazz = parseClass(pageKey);
            if (clazz == null) return;
            int y = leftY;
            for (RPGPath house : RPGPath.forClass(clazz)) {
                RPGPath target = house;
                addDrawableChild(new RpgButton(leftX, y, leftW, 23, Text.literal(house.display),
                        btn -> open(houseKey(target)), RpgButton.Kind.ACTION, RpgUiTheme.houseAccent(house)));
                y += 27;
            }
            y += 5;
            addDrawableChild(new RpgButton(leftX, y, leftW, 24, Text.literal("15 Especializações"),
                    btn -> open(classSpecsKey(clazz)), RpgButton.Kind.ACTION, RpgUiTheme.accent(clazz)));
            return;
        }

        if (pageKey.startsWith("classspec:")) {
            RPGClass clazz = parseClassSpec(pageKey);
            if (clazz == null) return;
            int y = leftY;
            for (RPGPath house : RPGPath.forClass(clazz)) {
                for (RPGSpecialization spec : RPGSpecialization.forPath(house)) {
                    RPGSpecialization target = spec;
                    addDrawableChild(new RpgButton(leftX, y, leftW, 18, Text.literal(spec.display),
                            btn -> open(specKey(target)), RpgButton.Kind.SMALL, RpgUiTheme.houseAccent(house)));
                    y += 20;
                }
                y += 4;
            }
            return;
        }

        if (pageKey.startsWith("house:")) {
            RPGPath house = parseHouse(pageKey);
            if (house == null) return;
            int y = leftY;
            for (RPGSpecialization spec : RPGSpecialization.forPath(house)) {
                RPGSpecialization target = spec;
                addDrawableChild(new RpgButton(leftX, y, leftW, 24, Text.literal(spec.display),
                        btn -> open(specKey(target)), RpgButton.Kind.ACTION, RpgUiTheme.houseAccent(house)));
                y += 29;
            }
            if (house == RPGPath.MAGE_OCCULT) {
                addIndexButton("Entender Corrupção", corruptionKey(0), leftX, y + 5, leftW);
            }
            return;
        }

        if (pageKey.startsWith("spec:")) {
            RPGSpecialization spec = parseSpec(pageKey);
            if (spec == null) return;
            List<RPGSpecialization> siblings = RPGSpecialization.forPath(spec.parent);
            int y = leftY;
            for (RPGSpecialization sibling : siblings) {
                RPGSpecialization target = sibling;
                RpgButton button = new RpgButton(leftX, y, leftW, 24, Text.literal(sibling.display),
                        btn -> open(specKey(target)), RpgButton.Kind.ACTION, RpgUiTheme.houseAccent(spec.parent));
                button.active = sibling != spec;
                addDrawableChild(button);
                y += 29;
            }
            if (spec == RPGSpecialization.BLOODMANCER) {
                addIndexButton("Entender Corrupção", corruptionKey(0), leftX, y + 5, leftW);
            }
        }
    }

    private void addIndexButton(String label, String target, int x, int y, int w) {
        addDrawableChild(new RpgButton(x, y, w, 23, Text.literal(label),
                btn -> open(target), RpgButton.Kind.ACTION, 0xFF9A7444));
    }

    private void open(String key) {
        if (key == null || key.equals(pageKey)) return;
        history.add(pageKey);
        pageKey = key;
        rightScroll=rightMaxScroll=0;
        clearAndInit();
    }

    private void openDirect(String key) { open(key); }

    private void back() {
        if (history.isEmpty()) {
            openDirect("index");
            return;
        }
        pageKey = history.remove(history.size() - 1);
        rightScroll=rightMaxScroll=0;
        clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0,0,width,height,0,0xFF15100C,0xFF2A2016);
        var v=MageViewport.fit(width,height);
        int localX=(int)Math.floor(v.localX(mouseX)),localY=(int)Math.floor(v.localY(mouseY));
        pushViewport(context,v);drawBook(context);renderLeftHeading(context);context.getMatrices().pop();
        BookRect b=book();
        // Scissor is set with an identity matrix, in actual GUI coordinates.
        context.enableScissor((int)Math.floor(v.x()+(b.x+b.pageW+12)*v.scale()),
                (int)Math.floor(v.y()+GuideLayout.CONTENT_TOP*v.scale()),
                (int)Math.ceil(v.x()+(b.x+b.w-12)*v.scale()),
                (int)Math.ceil(v.y()+GuideLayout.CONTENT_BOTTOM*v.scale()));
        pushViewport(context,v);maxRenderedY=GuideLayout.CONTENT_TOP;
        renderRightPage(context);
        rightMaxScroll=GuideLayout.scrollLimit(maxRenderedY+rightScroll);
        rightScroll=Math.min(rightScroll,rightMaxScroll);
        context.getMatrices().pop();context.disableScissor();
        pushViewport(context,v);
        if(rightMaxScroll>0)context.drawText(textRenderer,Text.literal("Roda: ler · "+(rightScroll==rightMaxScroll?"fim":"mais abaixo")),
                b.x+b.pageW+24,b.y+b.h-39,MUTED,false);
        super.render(context,localX,localY,delta);context.getMatrices().pop();
    }
    private void pushViewport(DrawContext c,MageViewport v) {
        c.getMatrices().push();c.getMatrices().translate(v.x(),v.y(),0);
        c.getMatrices().scale((float)v.scale(),(float)v.scale(),1);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        var v=MageViewport.fit(width,height);return super.mouseClicked(v.localX(x),v.localY(y),button);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        var v=MageViewport.fit(width,height);return super.mouseReleased(v.localX(x),v.localY(y),button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        var v=MageViewport.fit(width,height);return super.mouseDragged(v.localX(x),v.localY(y),button,dx/v.scale(),dy/v.scale());
    }
    @Override public boolean mouseScrolled(double x,double y,double amount) {
        var v=MageViewport.fit(width,height);BookRect b=book();double lx=v.localX(x),ly=v.localY(y);
        if(lx>=b.x+b.pageW+12&&lx<=b.x+b.w-12&&ly>=GuideLayout.CONTENT_TOP&&ly<=GuideLayout.CONTENT_BOTTOM) {
            rightScroll=Math.max(0,Math.min(rightMaxScroll,rightScroll-(int)Math.round(amount*27)));return true;
        }
        return super.mouseScrolled(lx,ly,amount);
    }

    private void drawBook(DrawContext context) {
        BookRect b = book();
        context.fill(b.x, b.y, b.x + b.w, b.y + b.h, 0xFF4A3522);
        context.fill(b.x + 5, b.y + 5, b.x + b.pageW - 2, b.y + b.h - 5, PAPER);
        context.fill(b.x + b.pageW + 2, b.y + 5, b.x + b.w - 5, b.y + b.h - 5, PAPER);
        context.fill(b.x + b.pageW - 2, b.y + 6, b.x + b.pageW + 2, b.y + b.h - 6, PAPER_DARK);
        context.fill(b.x + 5, b.y + 5, b.x + b.w - 5, b.y + 7, EDGE);
        context.fill(b.x + 5, b.y + b.h - 7, b.x + b.w - 5, b.y + b.h - 5, EDGE);
    }

    private void renderLeftHeading(DrawContext context) {
        BookRect b = book();
        int x = b.x + 24;
        int y = b.y + 24;
        context.drawTextWithShadow(textRenderer, Text.literal("RPG STATS").formatted(Formatting.BOLD),
                x, y, 0xFF5E3E20);
        context.drawTextWithShadow(textRenderer, Text.literal("Codex de Campo"), x, y + 14, MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal(leftCaption()), x, y + 30, MUTED);
    }

    private void renderRightPage(DrawContext context) {
        BookRect b = book();
        int x = b.x + b.pageW + 24;
        int y = b.y + 24-rightScroll;
        int w = b.pageW - 48;
        PlayerStats stats = ClientStatsStore.stats;

        if (pageKey.startsWith("corruption:")) {
            int section = parseCorruption(pageKey);
            if (section < 0) return;
            MageCorruptionText.GuidePage page = MageCorruptionText.guidePages().get(section);
            drawTitle(context, page.title(), x, y, RpgUiTheme.houseAccent(RPGPath.MAGE_OCCULT));
            int yy = y + 28;
            for (String paragraph : page.body().split("\n")) {
                yy = drawWrapped(context, paragraph, x, yy, w, INK, 9) + 6;
            }
            return;
        }

        if ("index".equals(pageKey)) {
            drawTitle(context, "ÍNDICE", x, y, 0xFF5E3E20);
            drawWrapped(context, "Escolha uma seção na página esquerda. Nas páginas longas, use a roda do mouse sobre o texto para continuar a leitura.", x, y + 28, w, INK, 9);
            drawWrapped(context, "Use Próx./Ant. para folhear em ordem ou Índice/Voltar para navegar pela hierarquia.", x, y + 92, w, MUTED, 9);
            return;
        }

        if ("current".equals(pageKey)) {
            drawTitle(context, "MINHA BUILD", x, y, 0xFF5E3E20);
            if (stats.clazz == null) {
                drawWrapped(context, "Você ainda não escolheu uma classe. Abra /rpg, escolha a classe e volte ao Codex.", x, y + 30, w, INK, 9);
                return;
            }
            int yy = y + 30;
            yy = line(context, "Classe", stats.clazz.display, x, yy, w);
            yy = line(context, "Nível", Integer.toString(stats.level), x, yy, w);
            yy = line(context, "Recurso", stats.clazz.resourceName(), x, yy, w);
            yy = line(context, "Casa", stats.path == null ? "não escolhida" : stats.path.display, x, yy, w);
            yy = line(context, "Especialização", stats.specialization == null ? "não escolhida" : stats.specialization.display, x, yy, w);
            if (stats.affinityHouse != null) {
                yy = line(context, "Casa 2", stats.affinityHouse.display, x, yy, w);
                line(context, "Talentos Casa 2", HouseRules.count(stats) + "/" + HouseRules.LIMIT, x, yy, w);
            }
            return;
        }

        if ("progression".equals(pageKey)) {
            drawTitle(context, "PROGRESSÃO", x, y, 0xFF5E3E20);
            drawWrapped(context, "Nível máximo: 50. Cada nível concede 2 PA e 1 PH.", x, y + 28, w, INK, 9);
            drawWrapped(context, "Nível 10: Casa principal. Nível 25: especialização. Nível 30: Casa secundária.", x, y + 74, w, INK, 9);
            drawWrapped(context, "PA melhora atributos. PH compra nodes, passivas e habilidades.", x, y + 130, w, MUTED, 9);
            return;
        }

        if ("secondary".equals(pageKey)) {
            drawTitle(context, "CASA SECUNDÁRIA", x, y, 0xFF5E3E20);
            drawWrapped(context, "Outra Casa da MESMA classe. Libera no nível " + HouseRules.LEVEL + ", aceita até " + HouseRules.LIMIT + " talentos e cada talento custa 2 PH.", x, y + 28, w, INK, 9);
            drawWrapped(context, "Os efeitos são reduzidos e podem adicionar penalidades. Ela complementa a build; não substitui sua Casa principal.", x, y + 100, w, MUTED, 9);
            return;
        }

        if (pageKey.startsWith("class:")) {
            RPGClass clazz = parseClass(pageKey);
            if (clazz == null) return;
            drawTitle(context, clazz.display.toUpperCase(Locale.ROOT), x, y, RpgUiTheme.accent(clazz));
            drawWrapped(context, classSummary(clazz), x, y + 28, w, INK, 9);
            drawWrapped(context, "Recurso: " + clazz.resourceName(), x, y + 100, w, MUTED, 9);
            drawWrapped(context, "A página esquerda lista as 5 Casas. Cada Casa leva às 3 especializações correspondentes.", x, y + 132, w, MUTED, 9);
            return;
        }

        if (pageKey.startsWith("classspec:")) {
            RPGClass clazz = parseClassSpec(pageKey);
            if (clazz == null) return;
            drawTitle(context, clazz.display + " · 15 ESPECIALIZAÇÕES", x, y, RpgUiTheme.accent(clazz));
            int yy = y + 28;
            for (RPGPath house : RPGPath.forClass(clazz)) {
                context.drawTextWithShadow(textRenderer, Text.literal(house.display).formatted(Formatting.BOLD), x, yy, RpgUiTheme.houseAccent(house));
                yy += 13;
                String names = RPGSpecialization.forPath(house).stream().map(s -> s.display).reduce((a,bn) -> a + " · " + bn).orElse("");
                yy = drawWrapped(context, names, x, yy, w, INK, 9) + 6;
            }
            return;
        }

        if (pageKey.startsWith("house:")) {
            RPGPath house = parseHouse(pageKey);
            if (house == null) return;
            drawTitle(context, house.display.toUpperCase(Locale.ROOT), x, y, RpgUiTheme.houseAccent(house));
            int houseY=drawWrapped(context,house.desc,x,y+28,w,INK,9)+9;
            for(String bonus:HouseBonusSummary.lines(house.nodes,stats.unlockedNodes))
                houseY=drawWrapped(context,bonus,x,houseY,w,INK,9)+7;
            SkillNode foundation = nodeBySuffix(house.nodes, "_foundation");
            if (foundation == null && !house.nodes.isEmpty()) foundation = house.nodes.get(0);
            if (foundation != null) {
                int yy = houseY+7;
                context.drawTextWithShadow(textRenderer, Text.literal("LOOP PRINCIPAL").formatted(Formatting.BOLD), x, yy, MUTED);
                drawWrapped(context, detailFor(foundation), x, yy + 14, w, INK, 9);
            }
            return;
        }

        if (pageKey.startsWith("spec:")) {
            RPGSpecialization spec = parseSpec(pageKey);
            if (spec == null) return;
            drawSpecialization(context, spec, x, y, w);
        }
    }

    private void drawSpecialization(DrawContext context, RPGSpecialization spec, int x, int y, int w) {
        int accent = RpgUiTheme.houseAccent(spec.parent);
        drawTitle(context, spec.display.toUpperCase(Locale.ROOT), x, y, accent);
        int yy = y + 24;

        context.drawTextWithShadow(textRenderer, Text.literal("Função").formatted(Formatting.BOLD), x, yy, MUTED);
        yy = drawWrapped(context, spec.desc, x + 48, yy, w - 48, INK, 9) + 5;

        SkillNode[] nodes = {
                primaryNode(spec), engineNode(spec), techniqueNode(spec), signatureNode(spec), ascensionNode(spec)
        };
        String[] labels = {"Base", "Motor", "Técnica", "Domínio", "Ascensão"};

        for (int i = 0; i < labels.length; i++) {
            SkillNode node = nodes[i];
            if (node == null) continue;
            context.drawTextWithShadow(textRenderer, Text.literal(labels[i]).formatted(Formatting.BOLD), x, yy, MUTED);
            yy += 11;
            String detail = detailFor(node);
            yy = drawWrapped(context, detail, x + 6, yy, w - 6, INK, 9);
            if (AbilityRegistry.hasActive(node.id())) {
                yy = drawWrapped(context, AbilityRegistry.activeSummary(node.id()),
                        x + 6, yy, w - 6, 0xFF6B3F1E, 9);
            }
            yy += 4;
        }
    }

    private int line(DrawContext context, String label, String value, int x, int y, int w) {
        context.drawTextWithShadow(textRenderer, Text.literal(label + ":").formatted(Formatting.BOLD), x, y, MUTED);
        return drawWrapped(context, value, x + 82, y, w - 82, INK, 9) + 6;
    }

    private void drawTitle(DrawContext context, String value, int x, int y, int color) {
        context.drawTextWithShadow(textRenderer, Text.literal(value).formatted(Formatting.BOLD), x, y, RpgUiTheme.accessibleAccent(color,PAPER));
        maxRenderedY=Math.max(maxRenderedY,y+16);
        context.fill(x, y + 13, x + Math.min(book().pageW - 48, textRenderer.getWidth(value) + 28), y + 14, color);
    }

    private int drawWrapped(DrawContext context, String raw, int x, int y, int maxWidth, int color, int lineHeight) {
        int yy = y;
        for (String line : wrap(raw, maxWidth)) {
            context.drawTextWithShadow(textRenderer, Text.literal(line), x, yy, color);
            yy += lineHeight;
        }
        maxRenderedY=Math.max(maxRenderedY,yy);
        return yy;
    }

    private List<String> wrap(String raw, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        String normalized = raw.replace("\n", " ").replaceAll("\\s+", " ").trim();
        StringBuilder line = new StringBuilder();
        for (String word : normalized.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && textRenderer.getWidth(candidate) > maxWidth) {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    private String detailFor(SkillNode node) {
        String concrete = ClassAbilityRegistry.description(node.id());
        if (concrete != null && !concrete.isBlank()) return concrete;
        if (node.description() != null && !node.description().isBlank()) return node.description();
        String active = AbilityRegistry.activeSummary(node.id());
        return active.isBlank() ? "Progressão passiva desta build." : active;
    }

    private String compact(String value, int max) {
        if (value == null) return "";
        String clean = value.replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() <= max) return clean;
        int cut = clean.lastIndexOf(' ', max - 1);
        if (cut < max / 2) cut = max - 1;
        return clean.substring(0, cut).trim() + "…";
    }

    private List<String> orderedKeys() {
        List<String> out = new ArrayList<>();
        out.add("index");
        out.add("current");
        out.add("progression");
        out.add("secondary");
        for (RPGClass clazz : RPGClass.values()) {
            out.add(classKey(clazz));
            out.add(classSpecsKey(clazz));
            for (RPGPath house : RPGPath.forClass(clazz)) {
                out.add(houseKey(house));
                if (house == RPGPath.MAGE_OCCULT) {
                    for (int i = 0; i < MageCorruptionText.guidePages().size(); i++) out.add(corruptionKey(i));
                }
                for (RPGSpecialization spec : RPGSpecialization.forPath(house)) out.add(specKey(spec));
            }
        }
        return out;
    }

    private String leftCaption() {
        if ("index".equals(pageKey)) return "Navegação";
        if (pageKey.startsWith("corruption:")) return "Corrupção Arcana";
        if (pageKey.startsWith("class:")) return "Casas da classe";
        if (pageKey.startsWith("classspec:")) return "Especializações";
        if (pageKey.startsWith("house:")) return "Especializações da Casa";
        if (pageKey.startsWith("spec:")) return "Mesmo ramo";
        return "Seção";
    }

    private SkillNode primaryNode(RPGSpecialization spec) {
        SkillNode node = nodeBySuffix(spec.nodes, "_initiation");
        return node != null ? node : (spec.nodes.isEmpty() ? null : spec.nodes.get(0));
    }

    private SkillNode engineNode(RPGSpecialization spec) {
        SkillNode node = nodeBySuffix(spec.nodes, "_engine");
        return node != null ? node : (spec.nodes.size() > 1 ? spec.nodes.get(1) : null);
    }

    private SkillNode techniqueNode(RPGSpecialization spec) {
        SkillNode node = nodeBySuffix(spec.nodes, "_technique");
        if (node != null) return node;
        for (SkillNode n : spec.nodes) if (!isAscension(n) && AbilityRegistry.hasActive(n.id())) return n;
        return spec.nodes.size() > 2 ? spec.nodes.get(2) : null;
    }

    private SkillNode signatureNode(RPGSpecialization spec) {
        SkillNode node = nodeBySuffix(spec.nodes, "_signature");
        if (node != null) return node;
        boolean seen = false;
        for (SkillNode n : spec.nodes) {
            if (isAscension(n) || !AbilityRegistry.hasActive(n.id())) continue;
            if (seen) return n;
            seen = true;
        }
        return spec.nodes.size() > 4 ? spec.nodes.get(4) : null;
    }

    private SkillNode ascensionNode(RPGSpecialization spec) {
        SkillNode node = nodeBySuffix(spec.nodes, "_ascension");
        if (node != null) return node;
        for (SkillNode n : spec.nodes) if (isAscension(n)) return n;
        return spec.nodes.isEmpty() ? null : spec.nodes.get(spec.nodes.size() - 1);
    }

    private SkillNode nodeBySuffix(List<SkillNode> nodes, String suffix) {
        for (SkillNode node : nodes) if (node.id().endsWith(suffix)) return node;
        return null;
    }

    private boolean isAscension(SkillNode node) {
        return node.id().endsWith("_ascension") || node.id().contains("_asc_")
                || node.name().toLowerCase(Locale.ROOT).contains("ascensão");
    }

    private String classSummary(RPGClass clazz) {
        return switch (clazz) {
            case GUERREIRO -> "Classe marcial de linha de frente. Fúria alimenta pressão, guarda e técnicas; as Casas mudam o estilo entre defesa, Berserker, armas, runas e comando.";
            case MAGO -> "Classe de Mana. Iron's Spells fornece os feitiços; o RPG Stats transforma eficiência, passivas, Casas, especializações e buildcraft.";
            case ARQUEIRO -> "Classe de Foco, distância e posicionamento. As Casas alternam entre Mira, Instinto, Momentum, elementos e dispositivos.";
            case ASSASSINO -> "Classe de Energia, abertura e combo. As Casas aprofundam Sombra, Veneno, Duelista, Sabotador e Lâmina Mística.";
        };
    }

    private String classKey(RPGClass clazz) { return "class:" + clazz.name(); }
    private String classSpecsKey(RPGClass clazz) { return "classspec:" + clazz.name(); }
    private String houseKey(RPGPath house) { return "house:" + house.name(); }
    private String specKey(RPGSpecialization spec) { return "spec:" + spec.name(); }
    private String corruptionKey(int section) { return "corruption:" + section; }

    private int parseCorruption(String key) {
        try {
            int section = Integer.parseInt(key.substring("corruption:".length()));
            return section >= 0 && section < MageCorruptionText.guidePages().size() ? section : -1;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private RPGClass parseClass(String key) {
        try { return RPGClass.valueOf(key.substring("class:".length())); } catch (Exception e) { return null; }
    }
    private RPGClass parseClassSpec(String key) {
        try { return RPGClass.valueOf(key.substring("classspec:".length())); } catch (Exception e) { return null; }
    }
    private RPGPath parseHouse(String key) {
        try { return RPGPath.valueOf(key.substring("house:".length())); } catch (Exception e) { return null; }
    }
    private RPGSpecialization parseSpec(String key) {
        try { return RPGSpecialization.valueOf(key.substring("spec:".length())); } catch (Exception e) { return null; }
    }

    private BookRect book() {
        return new BookRect(GuideLayout.BOOK_X,GuideLayout.BOOK_Y,GuideLayout.BOOK_W,GuideLayout.BOOK_H,GuideLayout.BOOK_W/2);
    }

    private record BookRect(int x, int y, int w, int h, int pageW) {}

    @Override
    public boolean shouldPause() {
        return false;
    }
}
