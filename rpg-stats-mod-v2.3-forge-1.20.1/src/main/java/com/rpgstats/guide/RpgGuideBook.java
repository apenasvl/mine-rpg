package com.rpgstats.guide;

import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.ability.ClassAbilityRegistry;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.gui.MageCorruptionText;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import com.rpgstats.tree.SkillNode;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enciclopedia nativa do RPG Stats.
 *
 * Mantemos WrittenBookItem de proposito: ele ja oferece paginacao, suporte a clickEvent
 * CHANGE_PAGE e compatibilidade com resource packs/Forge sem criar uma segunda GUI.
 */
public final class RpgGuideBook {
    public static final String GUIDE_TAG = "rpgstatsGuide";
    public static final int GUIDE_VERSION = 4;
    private static final String TITLE = "Codex do RPG Stats";
    private static final String AUTHOR = "RPG Stats";
    private static final int MAX_BOOK_PAGES = 100;

    private RpgGuideBook() {}

    public static ItemStack create(ServerPlayerEntity player) {
        PlayerStats stats = StatsManager.get(player);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", TITLE);
        nbt.putString("author", AUTHOR);
        nbt.putInt("generation", 0);
        nbt.putBoolean("resolved", true);
        nbt.putBoolean(GUIDE_TAG, true);
        nbt.putInt("rpgstatsGuideVersion", GUIDE_VERSION);

        List<Text> rendered = pages(stats);
        if (rendered.size() > MAX_BOOK_PAGES) {
            throw new IllegalStateException("RPG guide exceeds vanilla written-book page limit: " + rendered.size());
        }

        NbtList pages = new NbtList();
        for (Text page : rendered) pages.add(NbtString.of(Text.Serializer.toJson(page)));
        nbt.put("pages", pages);
        book.setCustomName(Text.literal(TITLE).formatted(Formatting.GOLD));
        return book;
    }

    public static boolean isGuide(ItemStack stack) {
        if (stack == null || !stack.isOf(Items.WRITTEN_BOOK)) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(GUIDE_TAG);
    }

    /**
     * /rpg guia:
     * - segurando o Codex: atualiza e abre imediatamente;
     * - copia antiga no inventario: substitui pela versao atual;
     * - sem copia: entrega uma, sem apagar nenhum item.
     */
    public static void giveOrOpen(ServerPlayerEntity player) {
        ItemStack fresh = create(player);

        if (isGuide(player.getMainHandStack())) {
            player.setStackInHand(Hand.MAIN_HAND, fresh);
            player.useBook(fresh, Hand.MAIN_HAND);
            return;
        }
        if (isGuide(player.getOffHandStack())) {
            player.setStackInHand(Hand.OFF_HAND, fresh);
            player.useBook(fresh, Hand.OFF_HAND);
            return;
        }

        int existing = findGuideSlot(player);
        if (existing >= 0) {
            player.getInventory().setStack(existing, fresh);
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
            player.sendMessage(Text.literal("§6Codex atualizado. §7Segure o livro e use o botão direito."), false);
            return;
        }

        if (player.getMainHandStack().isEmpty()) {
            player.setStackInHand(Hand.MAIN_HAND, fresh);
            player.useBook(fresh, Hand.MAIN_HAND);
            return;
        }

        if (player.getInventory().insertStack(fresh)) {
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
            player.sendMessage(Text.literal("§6Codex do RPG Stats adicionado ao inventário. §7Use o botão direito."), false);
            return;
        }

        player.dropItem(fresh, false);
        player.sendMessage(Text.literal("§eInventário cheio: o Codex foi deixado aos seus pés."), false);
    }

    private static int findGuideSlot(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (isGuide(player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    /**
     * Layout fixo e previsivel:
     * 5 paginas gerais + 2 por classe + 1 por Casa + 1 por especializacao.
     * Mais 5 paginas curtas sobre Corrupcao: 98, abaixo do limite vanilla de 100.
     */
    private static List<Text> pages(PlayerStats stats) {
        List<PageDef> defs = new ArrayList<>();

        defs.add(new PageDef("cover", pages -> cover(pages)));
        defs.add(new PageDef("index", pages -> index(pages)));
        defs.add(new PageDef("progression", pages -> progression(pages)));
        defs.add(new PageDef("secondary", pages -> secondaryHouse(pages)));
        defs.add(new PageDef("current", pages -> currentBuild(stats, pages)));
        for (int i = 0; i < MageCorruptionText.guidePages().size(); i++) {
            final int section = i;
            defs.add(new PageDef(corruptionKey(i), pages -> corruptionPage(section, pages)));
        }

        for (RPGClass clazz : RPGClass.values()) {
            final RPGClass c = clazz;
            defs.add(new PageDef(classKey(c), pages -> classOverview(c, pages)));
            defs.add(new PageDef(classAbilitiesKey(c), pages -> classAbilities(c, pages)));

            for (RPGPath house : RPGPath.forClass(c)) {
                final RPGPath h = house;
                defs.add(new PageDef(houseKey(h), pages -> housePage(h, pages)));
                for (RPGSpecialization specialization : RPGSpecialization.forPath(h)) {
                    final RPGSpecialization spec = specialization;
                    defs.add(new PageDef(specKey(spec), pages -> specializationPage(spec, pages)));
                }
            }
        }

        Map<String, Integer> pageNumbers = new LinkedHashMap<>();
        for (int i = 0; i < defs.size(); i++) pageNumbers.put(defs.get(i).key(), i + 1);

        List<Text> out = new ArrayList<>(defs.size());
        for (PageDef def : defs) out.add(def.renderer().render(pageNumbers));
        return out;
    }

    private static Text cover(Map<String, Integer> pages) {
        MutableText text = title("RPG STATS CODEX");
        text.append(Text.literal("\n\nGuia de classes, Casas, especializações e habilidades.")
                .formatted(Formatting.BLACK));
        text.append(Text.literal("\n\n"));
        text.append(link("§l[ ABRIR ÍNDICE ]", "index", pages));
        text.append(Text.literal("\n\n/rpg guia atualiza o Codex para a sua build atual.")
                .formatted(Formatting.DARK_GRAY));
        return text;
    }

    private static Text index(Map<String, Integer> pages) {
        MutableText text = title("ÍNDICE");
        text.append(Text.literal("\n\n"));
        text.append(link("★ Minha build", "current", pages));
        text.append(Text.literal("\n"));
        text.append(link("• Progressão", "progression", pages));
        text.append(Text.literal("\n"));
        text.append(link("• Casa secundária", "secondary", pages));
        text.append(Text.literal("\n\nCLASSES").formatted(Formatting.DARK_GRAY, Formatting.BOLD));

        for (RPGClass clazz : RPGClass.values()) {
            text.append(Text.literal("\n"));
            text.append(classLink(clazz, clazz.display, classKey(clazz), pages));
        }
        return text;
    }

    private static Text progression(Map<String, Integer> pages) {
        MutableText text = title("PROGRESSÃO");
        text.append(Text.literal("\n\nNível máximo: 50\n2 PA + 1 PH por nível.\n\n"
                + "Nível 10: Casa principal.\n"
                + "Nível 25: especialização.\n"
                + "Nível 30: Casa secundária.\n\n"
                + "PA melhora atributos. PH compra nodes e habilidades.\nApós nível 20: +0,6 vida máxima por nível.")
                .formatted(Formatting.BLACK));
        text.append(navFooter("index", pages));
        return text;
    }

    private static Text secondaryHouse(Map<String, Integer> pages) {
        MutableText text = title("CASA SECUNDÁRIA");
        text.append(Text.literal("\n\nOutra Casa da MESMA classe.\n\n"
                + "• libera no nível " + HouseRules.LEVEL + "\n"
                + "• máximo de " + HouseRules.LIMIT + " talentos\n"
                + "• cada talento custa 2 PH\n"
                + "• efeitos vêm reduzidos\n"
                + "• pode adicionar penalidades\n\n"
                + "Ela complementa a build; nunca substitui a Casa principal.")
                .formatted(Formatting.BLACK));
        text.append(navFooter("index", pages));
        return text;
    }

    private static Text currentBuild(PlayerStats stats, Map<String, Integer> pages) {
        MutableText text = title("MINHA BUILD");
        if (stats.clazz == null) {
            text.append(Text.literal("\n\nVocê ainda não escolheu uma classe.\n\nUse /rpg e depois volte ao Codex.")
                    .formatted(Formatting.BLACK));
            text.append(navFooter("index", pages));
            return text;
        }

        text.append(Text.literal("\n\nClasse: ").formatted(Formatting.DARK_GRAY));
        text.append(classLink(stats.clazz, stats.clazz.display, classKey(stats.clazz), pages));
        text.append(Text.literal("\nNível: " + stats.level + "\nRecurso: " + stats.clazz.resourceName())
                .formatted(Formatting.BLACK));

        text.append(Text.literal("\n\nCasa: ").formatted(Formatting.DARK_GRAY));
        if (stats.path == null) text.append(Text.literal("não escolhida").formatted(Formatting.GRAY));
        else text.append(classLink(stats.clazz, stats.path.display, houseKey(stats.path), pages));

        text.append(Text.literal("\nEspecialização: ").formatted(Formatting.DARK_GRAY));
        if (stats.specialization == null) text.append(Text.literal("não escolhida").formatted(Formatting.GRAY));
        else text.append(classLink(stats.clazz, stats.specialization.display, specKey(stats.specialization), pages));

        if (stats.affinityHouse != null) {
            text.append(Text.literal("\nCasa 2: ").formatted(Formatting.DARK_GRAY));
            text.append(classLink(stats.clazz, stats.affinityHouse.display, houseKey(stats.affinityHouse), pages));
            text.append(Text.literal("\nTalentos Casa 2: " + HouseRules.count(stats) + "/" + HouseRules.LIMIT)
                    .formatted(Formatting.BLACK));
        }

        text.append(navFooter("index", pages));
        return text;
    }

    /** Pagina 1/2 da classe: identidade, recurso e Casas clicaveis. */
    private static Text classOverview(RPGClass clazz, Map<String, Integer> pages) {
        MutableText text = classTitle(clazz, clazz.display.toUpperCase());
        text.append(Text.literal("\n\n" + classSummary(clazz)).formatted(Formatting.BLACK));
        text.append(Text.literal("\n\nRecurso: " + clazz.resourceName()).formatted(Formatting.DARK_GRAY, Formatting.BOLD));
        text.append(Text.literal("\n\nCASAS").formatted(Formatting.DARK_GRAY, Formatting.BOLD));

        for (RPGPath house : RPGPath.forClass(clazz)) {
            text.append(Text.literal("\n"));
            text.append(classLink(clazz, "• " + house.display, houseKey(house), pages));
        }

        text.append(Text.literal("\n"));
        text.append(link("→ Ver especializações", classAbilitiesKey(clazz), pages));
        text.append(navFooter("index", pages));
        return text;
    }

    /** Pagina 2/2 da classe: as 15 especializacoes, agrupadas por Casa e clicaveis. */
    private static Text classAbilities(RPGClass clazz, Map<String, Integer> pages) {
        MutableText text = classTitle(clazz, clazz.display + " · Especializações");
        text.append(Text.literal("\n\nClique em uma especialização para abrir a ficha de habilidades.")
                .formatted(Formatting.DARK_GRAY));

        for (RPGPath house : RPGPath.forClass(clazz)) {
            text.append(Text.literal("\n\n"));
            text.append(classLink(clazz, house.display, houseKey(house), pages)
                    .formatted(Formatting.BOLD));

            List<RPGSpecialization> specs = RPGSpecialization.forPath(house);
            text.append(Text.literal("\n"));
            for (int i = 0; i < specs.size(); i++) {
                if (i > 0) text.append(Text.literal(" · ").formatted(Formatting.GRAY));
                RPGSpecialization spec = specs.get(i);
                text.append(classLink(clazz, shortName(spec.display, 16), specKey(spec), pages));
            }
        }

        text.append(navFooter(classKey(clazz), pages));
        return text;
    }

    /** Uma pagina por Casa: loop real + tres especializacoes clicaveis. */
    private static Text housePage(RPGPath house, Map<String, Integer> pages) {
        RPGClass clazz = house.parent;
        MutableText text = classTitle(clazz, house.display);
        text.append(Text.literal("\n" + house.desc).formatted(Formatting.DARK_GRAY));

        SkillNode foundation = nodeBySuffix(house.nodes, "_foundation");
        if (foundation == null && !house.nodes.isEmpty()) foundation = house.nodes.get(0);

        text.append(Text.literal("\n\nLOOP DA CASA").formatted(Formatting.DARK_GRAY, Formatting.BOLD));
        if (foundation != null) {
            text.append(Text.literal("\n"));
            text.append(abilityEntry("Base", foundation, clazz, 46));
        }

        text.append(Text.literal("\n\nESPECIALIZAÇÕES").formatted(Formatting.DARK_GRAY, Formatting.BOLD));
        for (RPGSpecialization spec : RPGSpecialization.forPath(house)) {
            text.append(Text.literal("\n"));
            text.append(classLink(clazz, "• " + spec.display, specKey(spec), pages)
                    .styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Text.literal(spec.desc)))));
        }

        if (house == RPGPath.MAGE_OCCULT) {
            text.append(Text.literal("\n\n"));
            text.append(link("Corrupção", corruptionKey(0), pages));
            text.append(Text.literal("   "));
            text.append(link("Índice", "index", pages));
        } else {
            text.append(navFooter(classKey(clazz), pages));
        }
        return text;
    }

    /**
     * Ficha padronizada para TODAS as especializacoes.
     * A funcao muda de acordo com a classe, mas os campos permanecem iguais:
     * Funcao, Base, Motor, Tecnica, Dominio e Ascensao.
     */
    private static Text specializationPage(RPGSpecialization spec, Map<String, Integer> pages) {
        RPGClass clazz = spec.parent.parent;
        MutableText text = classTitle(clazz, spec.display);
        text.append(Text.literal("\nFunção: ").formatted(Formatting.DARK_GRAY, Formatting.BOLD));
        text.append(Text.literal(shortText(spec.desc, 42)).formatted(Formatting.BLACK)
                .styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal(spec.desc)))));

        SkillNode base = primaryNode(spec);
        SkillNode engine = engineNode(spec);
        SkillNode technique = techniqueNode(spec);
        SkillNode signature = signatureNode(spec);
        SkillNode ascension = ascensionNode(spec);

        appendSpecField(text, "Base", base, clazz);
        appendSpecField(text, "Motor", engine, clazz);
        appendSpecField(text, "Técnica", technique, clazz);
        appendSpecField(text, "Domínio", signature, clazz);
        appendSpecField(text, "Ascensão", ascension, clazz);

        text.append(Text.literal("\n\n"));
        text.append(link("← " + spec.parent.display, houseKey(spec.parent), pages));
        text.append(Text.literal("  "));
        text.append(spec == RPGSpecialization.BLOODMANCER
                ? link("Corrupção", corruptionKey(0), pages)
                : link("Índice", "index", pages));
        return text;
    }

    private static void appendSpecField(MutableText text, String label, SkillNode node, RPGClass clazz) {
        if (node == null) return;
        text.append(Text.literal("\n"));
        text.append(abilityEntry(label, node, clazz, 28));
    }

    private static String corruptionKey(int section) {
        return "mage_corruption_" + section;
    }

    private static Text corruptionPage(int section, Map<String, Integer> pages) {
        MageCorruptionText.GuidePage page = MageCorruptionText.guidePages().get(section);
        MutableText text = title(page.title());
        text.append(Text.literal("\n" + page.body()).formatted(Formatting.BLACK));
        text.append(Text.literal("\n"));
        text.append(link("←", section == 0 ? houseKey(RPGPath.MAGE_OCCULT) : corruptionKey(section - 1), pages));
        text.append(Text.literal("  "));
        text.append(link("Casa", houseKey(RPGPath.MAGE_OCCULT), pages));
        if (section + 1 < MageCorruptionText.guidePages().size()) {
            text.append(Text.literal("  "));
            text.append(link("Próxima →", corruptionKey(section + 1), pages));
        }
        return text;
    }

    private static MutableText abilityEntry(String label, SkillNode node, RPGClass clazz, int visibleLimit) {
        String detail = detailFor(node);
        String active = AbilityRegistry.hasActive(node.id()) ? AbilityRegistry.activeSummary(node.id()) : "";
        String hover = node.name() + "\n" + detail + (active.isBlank() ? "" : "\n\n" + active);

        MutableText line = Text.literal(label + ": ").formatted(Formatting.DARK_GRAY, Formatting.BOLD);
        line.append(Text.literal(shortText(detail, visibleLimit)).formatted(Formatting.BLACK));
        return line.styled(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    private static String detailFor(SkillNode node) {
        String concrete = ClassAbilityRegistry.description(node.id());
        if (concrete != null && !concrete.isBlank()) return concrete;
        if (node.description() != null && !node.description().isBlank()) return node.description();
        if (AbilityRegistry.hasActive(node.id())) return AbilityRegistry.activeSummary(node.id());
        return "Progressão passiva desta build.";
    }

    private static List<SkillNode> keyClassNodes(RPGClass clazz) {
        List<SkillNode> nodes = clazz.nodes;
        if (nodes.size() <= 6) return nodes;

        List<SkillNode> out = new ArrayList<>();
        addIfPresent(out, nodeBySuffix(nodes, "_awakening"));
        addIfPresent(out, nodeBySuffix(nodes, "_flow"));
        addIfPresent(out, nodeBySuffix(nodes, "_guard"));
        addIfPresent(out, nodeBySuffix(nodes, "_tempo"));
        addIfPresent(out, nodeBySuffix(nodes, "_utility"));
        addIfPresent(out, nodeBySuffix(nodes, "_mastery"));

        if (out.size() < 5) {
            for (SkillNode node : nodes) {
                if (!out.contains(node)) out.add(node);
                if (out.size() >= 6) break;
            }
        }
        return out.subList(0, Math.min(6, out.size()));
    }

    private static SkillNode primaryNode(RPGSpecialization spec) {
        SkillNode initiation = nodeBySuffix(spec.nodes, "_initiation");
        return initiation != null ? initiation : (spec.nodes.isEmpty() ? null : spec.nodes.get(0));
    }

    private static SkillNode engineNode(RPGSpecialization spec) {
        SkillNode engine = nodeBySuffix(spec.nodes, "_engine");
        if (engine != null) return engine;
        return spec.nodes.size() > 1 ? spec.nodes.get(1) : null;
    }

    private static SkillNode techniqueNode(RPGSpecialization spec) {
        SkillNode technique = nodeBySuffix(spec.nodes, "_technique");
        if (technique != null) return technique;
        for (SkillNode node : spec.nodes) {
            if (isAscension(node)) continue;
            if (AbilityRegistry.hasActive(node.id())) return node;
        }
        return spec.nodes.size() > 2 ? spec.nodes.get(2) : null;
    }

    private static SkillNode signatureNode(RPGSpecialization spec) {
        SkillNode signature = nodeBySuffix(spec.nodes, "_signature");
        if (signature != null) return signature;
        boolean firstActiveSeen = false;
        for (SkillNode node : spec.nodes) {
            if (isAscension(node) || !AbilityRegistry.hasActive(node.id())) continue;
            if (firstActiveSeen) return node;
            firstActiveSeen = true;
        }
        return spec.nodes.size() > 4 ? spec.nodes.get(4) : null;
    }

    private static SkillNode ascensionNode(RPGSpecialization spec) {
        SkillNode ascension = nodeBySuffix(spec.nodes, "_ascension");
        if (ascension != null) return ascension;
        for (SkillNode node : spec.nodes) if (isAscension(node)) return node;
        return spec.nodes.isEmpty() ? null : spec.nodes.get(spec.nodes.size() - 1);
    }

    private static boolean isAscension(SkillNode node) {
        String id = node.id();
        return id.endsWith("_ascension") || id.contains("_asc_")
                || node.name().toLowerCase(java.util.Locale.ROOT).contains("ascensão");
    }

    private static SkillNode nodeBySuffix(List<SkillNode> nodes, String suffix) {
        for (SkillNode node : nodes) if (node.id().endsWith(suffix)) return node;
        return null;
    }

    private static void addIfPresent(List<SkillNode> list, SkillNode node) {
        if (node != null && !list.contains(node)) list.add(node);
    }

    private static String shortName(String text, int limit) {
        if (text == null || text.length() <= limit) return text == null ? "" : text;
        int cut = text.lastIndexOf(' ', limit - 1);
        if (cut < limit / 2) cut = limit - 1;
        return text.substring(0, cut).trim() + "…";
    }

    private static String shortText(String text, int limit) {
        if (text == null || text.isBlank()) return "-";
        String clean = text.replace("\n", " ").replaceAll("\\s+", " ").trim()
                .replace("MECANICA:", "").replace("MODIFICADOR:", "")
                .replace("PREPARACAO:", "").replace("ATIVA:", "")
                .replace("MOTOR:", "").replace("KEYSTONE:", "").trim();
        if (clean.length() <= limit) return clean;
        int cut = clean.lastIndexOf(' ', limit - 1);
        if (cut < limit / 2) cut = limit - 1;
        return clean.substring(0, cut).trim() + "…";
    }

    private static MutableText link(String label, String key, Map<String, Integer> pages) {
        Integer page = pages.get(key);
        if (page == null) return Text.literal(label).formatted(Formatting.GRAY);
        return Text.literal(label).setStyle(Style.EMPTY
                .withColor(Formatting.DARK_BLUE)
                .withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Integer.toString(page)))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Ir para a página " + page))));
    }

    private static MutableText classLink(RPGClass clazz, String label, String key, Map<String, Integer> pages) {
        Integer page = pages.get(key);
        if (page == null) return Text.literal(label).formatted(Formatting.GRAY);
        return Text.literal(label).setStyle(
                classStyle(clazz)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Integer.toString(page)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Abrir " + label.replace("• ", "")))));
    }

    private static MutableText navFooter(String backKey, Map<String, Integer> pages) {
        MutableText footer = Text.literal("\n\n");
        footer.append(link("← Voltar", backKey, pages));
        footer.append(Text.literal("   "));
        footer.append(link("Índice", "index", pages));
        return footer;
    }

    private static MutableText title(String value) {
        return Text.literal(value).formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
    }

    private static MutableText classTitle(RPGClass clazz, String value) {
        return Text.literal(value).setStyle(classStyle(clazz).withBold(true));
    }

    private static Style classStyle(RPGClass clazz) {
        return Style.EMPTY.withColor(switch (clazz) {
            case GUERREIRO -> Formatting.RED;
            case MAGO -> Formatting.AQUA;
            case ARQUEIRO -> Formatting.DARK_GREEN;
            case ASSASSINO -> Formatting.DARK_PURPLE;
        });
    }

    private static String classSummary(RPGClass clazz) {
        return switch (clazz) {
            case GUERREIRO -> "Marcial de linha de frente. Fúria alimenta pressão, guarda e técnicas. As Casas variam entre defesa, Berserker, armas, runas e comando.";
            case MAGO -> "Mana e buildcraft mágico. Iron's Spells é o arsenal de feitiços; o RPG Stats transforma eficiência, passivas, Casas e especializações.";
            case ARQUEIRO -> "Foco, distância e posicionamento. As Casas trocam o loop entre Mira, Instinto, Momentum, elementos e dispositivos.";
            case ASSASSINO -> "Energia, abertura e combo. As Casas aprofundam Sombra, Veneno, Duelista, Sabotador ou Lâmina Mística.";
        };
    }

    private static String classKey(RPGClass clazz) {
        return "class_" + clazz.name();
    }

    private static String classAbilitiesKey(RPGClass clazz) {
        return "class_" + clazz.name() + "_abilities";
    }

    private static String houseKey(RPGPath house) {
        return "house_" + house.name();
    }

    private static String specKey(RPGSpecialization spec) {
        return "spec_" + spec.name();
    }

    @FunctionalInterface
    private interface PageRenderer {
        Text render(Map<String, Integer> pageNumbers);
    }

    private record PageDef(String key, PageRenderer renderer) {}
}
