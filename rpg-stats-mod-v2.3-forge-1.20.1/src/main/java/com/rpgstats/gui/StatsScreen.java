package com.rpgstats.gui;

import com.rpgstats.ClientStatsStore;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.ability.AbilityType;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.classes.RPGSubclass;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.Stat;
import com.rpgstats.stats.StatsManager;
import com.rpgstats.tree.SkillNode;
import com.rpgstats.network.RpgNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu principal do RPG Stats.
 *
 * v1.9: interface compacta e legivel para a progressao soulslike
 * Classe -> Casa -> Especializacao -> Afinidade.
 */
public class StatsScreen extends Screen {
    private enum Page { ATTRIBUTES, CLASS_TREE, PATH_TREE, SPECIALIZATION_TREE, AFFINITY }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        int centerX() { return x + w / 2; }
        int centerY() { return y + h / 2; }
    }

    private record Layout(
            int panelX, int panelY, int panelW, int panelH,
            int sidebarX, int sidebarY, int sidebarW, int sidebarH,
            int contentX, int contentY, int contentW, int contentH,
            int tabsY, int tabsH
    ) {}

    private record NodeVisual(SkillNode node, String key, boolean secondary, Rect rect, RpgButton button) {}
    private record HintVisual(RpgButton button, List<Text> lines) {}
    private record DetailTarget(SkillNode node, String key, boolean secondary) {}

    private static final String[] ACTIVE_KEYS = {"R", "Z", "X", "C"};

    private final List<RpgButton> attributeButtons = new ArrayList<>();
    private final Map<String, NodeVisual> nodeVisuals = new LinkedHashMap<>();
    private final List<HintVisual> hintVisuals = new ArrayList<>();
    private Page page = Page.ATTRIBUTES;
    private String inspectedNode = "";
    private int detailScroll, choiceScroll;
    private Page choicePage;
    private final ProgressionSelectionState<Page,String> choices = new ProgressionSelectionState<>();
    private ClassSelectionState<String> choice = new ClassSelectionState<>();
    private final List<RpgButton> choiceCards = new ArrayList<>();
    private RpgButton choiceConfirm;
    private record ChoiceOption(String id,String title,String description,List<SkillNode> nodes,int accent,String icon) {}
    private List<ChoiceOption> choiceOptions = List.of();
    private int activeEquipSlot = 0;
    private RPGPath viewingPath = null;
    private final ClassSelectionView classSelection = new ClassSelectionView();
    private final AwakeningView awakening = new AwakeningView();

    public StatsScreen() {
        super(Text.literal("RPG Stats"));
    }

    public void rebuild() {
        attributeButtons.clear();
        nodeVisuals.clear();
        hintVisuals.clear();
        clearAndInit();
    }

    @Override
    protected void init() {
        attributeButtons.clear();
        nodeVisuals.clear();
        hintVisuals.clear();
        choiceCards.clear();
        choiceConfirm = null;
        choiceOptions = List.of();
        PlayerStats stats = ClientStatsStore.stats;
        if(stats.awakened || stats.clazz != null) awakening.reset();

        if (stats.clazz == null) {
            if(!stats.awakened) addAwakeningButton();
            else addClassSelectionButtons();
            return;
        }
        classSelection.reset();
        Layout l = layout();
        int accent = RpgUiTheme.themedAccent(stats.clazz, stats.path, stats.affinityHouse);
        addTabs(l, stats, accent);
        addActiveSlotButtons(l, stats, accent);

        if (page == Page.ATTRIBUTES) {
            addAttributeButtons(l, stats);
            return;
        }
        if (page == Page.AFFINITY) {
            if(stats.affinityHouse==null) addAffinitySelection(l,stats);
            else addSkillButtons(l,HouseRules.nodes(stats.affinityHouse),false,stats.clazz);
            return;
        }
        if (page == Page.CLASS_TREE) {
            addSkillButtons(l, stats.clazz.nodes, false, stats.clazz);
            return;
        }

        if (page == Page.PATH_TREE) {
                if (stats.path == null) {
                    addPathSelectionButtons(l, stats);
                } else {
                    viewingPath = stats.path;
                    addSkillButtons(l, viewingPath.nodes, false, stats.clazz);
                }
        } else if (page == Page.SPECIALIZATION_TREE) {
                if (stats.specialization == null) addSpecializationSelectionButtons(l, stats);
                else addSkillButtons(l, stats.specialization.nodes, false, stats.clazz);
        }
    }

    private boolean illustratedScreen() { return CharacterArt.supported(ClientStatsStore.stats.clazz); }

    private MageViewport mageViewport() { return MageViewport.fit(width,height); }

    @Override
    public boolean mouseClicked(double x,double y,int button) {
        if(illustratedScreen()) { var v=mageViewport(); return super.mouseClicked(v.localX(x),v.localY(y),button); }
        return super.mouseClicked(x,y,button);
    }
    @Override
    public boolean mouseReleased(double x,double y,int button) {
        if(illustratedScreen()) { var v=mageViewport(); return super.mouseReleased(v.localX(x),v.localY(y),button); }
        return super.mouseReleased(x,y,button);
    }
    @Override
    public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(illustratedScreen()) { var v=mageViewport(); return super.mouseDragged(v.localX(x),v.localY(y),button,dx/v.scale(),dy/v.scale()); }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override
    public void mouseMoved(double x,double y) {
        if(illustratedScreen()) { var v=mageViewport(); super.mouseMoved(v.localX(x),v.localY(y)); }
        else super.mouseMoved(x,y);
    }
    @Override
    public boolean mouseScrolled(double x,double y,double amount) {
        double lx=x,ly=y;
        if(illustratedScreen()) {var v=mageViewport();lx=v.localX(x);ly=v.localY(y);}
        Layout l=layout();
        if(lx>=l.sidebarX && lx<l.sidebarX+l.sidebarW && ly>=l.sidebarY && ly<l.sidebarY+l.sidebarH) {
            detailScroll=Math.max(0,detailScroll-(int)Math.signum(amount)*3);return true;
        }
        if(!choiceOptions.isEmpty() && lx>=l.contentX && lx<l.contentX+l.contentW && ly>=l.contentY+123 && ly<l.contentY+l.contentH-37) {
            choiceScroll=Math.max(0,choiceScroll-(int)Math.signum(amount)*3);return true;
        }
        return super.mouseScrolled(lx,ly,amount);
    }

    private Layout layout() {
        if(illustratedScreen()) return new Layout(10,10,800,450,16,86,222,368,250,116,548,336,86,23);
        int panelW = Math.min(760, Math.max(350, width - 24));
        int panelH = Math.min(390, Math.max(300, height - 20));
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        int sidebarW = Math.min(180, Math.max(132, panelW / 4));
        int sidebarX = panelX + 10;
        int sidebarY = panelY + 62;
        int sidebarH = panelH - 72;

        int contentX = sidebarX + sidebarW + 9;
        int tabsY = panelY + 62;
        int tabsH = 20;
        int contentY = tabsY + tabsH + 7;
        int contentW = panelX + panelW - 10 - contentX;
        int contentH = panelY + panelH - 10 - contentY;
        return new Layout(panelX, panelY, panelW, panelH,
                sidebarX, sidebarY, sidebarW, sidebarH,
                contentX, contentY, contentW, contentH, tabsY, tabsH);
    }

    private Layout shiftContent(Layout l, int dy) {
        return new Layout(l.panelX, l.panelY, l.panelW, l.panelH,
                l.sidebarX, l.sidebarY, l.sidebarW, l.sidebarH,
                l.contentX, l.contentY + dy, l.contentW, Math.max(40, l.contentH - dy), l.tabsY, l.tabsH);
    }

    private void addTabs(Layout l, PlayerStats stats, int accent) {
        List<Page> pages = new ArrayList<>();
        pages.add(Page.ATTRIBUTES);
        pages.add(Page.CLASS_TREE);
        pages.add(Page.PATH_TREE);
        if (stats.path != null) pages.add(Page.SPECIALIZATION_TREE);
        if (stats.path != null && (stats.affinityHouse != null || stats.level >= HouseRules.LEVEL)) {
            pages.add(Page.AFFINITY);
        }
        if (!pages.contains(page)) page = Page.ATTRIBUTES;

        int gap = 3;
        int total = l.contentW - gap * (pages.size() - 1);
        int w = Math.max(36, total / pages.size());
        int x = l.contentX;

        for (int i = 0; i < pages.size(); i++) {
            Page target = pages.get(i);
            int bw = i == pages.size() - 1 ? l.contentX + l.contentW - x : w;
            String label = switch (target) {
                case ATTRIBUTES -> "Stats";
                case CLASS_TREE -> "Classe";
                case PATH_TREE -> "Casa";
                case SPECIALIZATION_TREE -> "Especial.";
                case AFFINITY -> "Afinidade";
            };
            int tabAccent = switch (target) {
                case CLASS_TREE -> RpgUiTheme.accent(stats.clazz);
                case PATH_TREE -> stats.path == null ? accent : pathColor(stats.path);
                case SPECIALIZATION_TREE -> stats.specialization == null
                        ? (stats.path == null ? accent : pathColor(stats.path)) : specColor(stats.specialization);
                case AFFINITY -> stats.affinityHouse == null ? accent : pathColor(stats.affinityHouse);
                default -> accent;
            };
            RpgButton button = new RpgButton(x, l.tabsY, bw, l.tabsH, Text.literal(label), b -> {
                changePage(target);
            }, RpgButton.Kind.TAB, tabAccent)
                    .icon(tabIcon(target))
                    .selected(page == target);

            if (target == Page.PATH_TREE) button.active = stats.path != null || stats.level >= StatsManager.PATH_LEVEL;
            if (target == Page.SPECIALIZATION_TREE) {
                button.active = stats.path != null && stats.level >= StatsManager.SPECIALIZATION_LEVEL;
            }
            if (target == Page.AFFINITY) button.active = stats.path!=null && stats.level>=HouseRules.LEVEL;
            addDrawableChild(button);
            addHint(button, pageName(target), pageDescription(target, stats), tabRequirement(target, stats));
            x += bw + gap;
        }
    }

    private void addActiveSlotButtons(Layout l, PlayerStats stats, int accent) {
        int gap = 3;
        int x = l.sidebarX + 10;
        int y = l.sidebarY + l.sidebarH - 43;
        int totalW = l.sidebarW - 20;
        int bw = (totalW - gap * 3) / 4;
        for (int i = 0; i < PlayerStats.ACTIVE_SLOTS; i++) {
            final int slot = i;
            RpgButton button = new RpgButton(x + i * (bw + gap), y, bw, 17, Text.literal(ACTIVE_KEYS[i]),
                    b -> { activeEquipSlot = slot; rebuild(); }, RpgButton.Kind.TAB, accent)
                    .selected(activeEquipSlot == i);
            addDrawableChild(button);
            addHint(button, "Slot " + ACTIVE_KEYS[i], "Selecione este slot e clique em uma habilidade ATIVA desbloqueada para equipa-la.", "Tecla de uso: " + ACTIVE_KEYS[i]);
        }
    }

    private void addClassSelectionButtons() {
        classSelection.init(width, height, this::addDrawableChild,
                clazz -> send(RPGStatsMod.SELECT_CLASS, clazz.name()));
    }

    private void addAwakeningButton() {
        awakening.init(width,height,this::addDrawableChild,()->send(RPGStatsMod.AWAKEN,""));
    }

    private void addAffinitySelection(Layout l,PlayerStats stats) {
        addChoiceButtons(l, RPGPath.forClass(stats.clazz).stream().filter(p->p!=stats.path)
                .map(p->new ChoiceOption(p.name(),shortPathName(p),p.desc,HouseRules.nodes(p),pathColor(p),houseIcon(p))).toList());
    }

    private void addSubclassSelectionButtons(RPGClass clazz) {
        Layout l = layout();
        List<RPGSubclass> subclasses = RPGSubclass.forClass(clazz);
        int gap = 12;
        int innerX = l.panelX + 30;
        int innerW = l.panelW - 60;
        int cardW = (innerW - gap * 2) / 3;
        int cardH = Math.min(166, l.panelH - 155);
        int y = l.panelY + 118;

        for (int i = 0; i < subclasses.size(); i++) {
            RPGSubclass subclass = subclasses.get(i);
            int x = innerX + i * (cardW + gap);
            RpgButton card = new RpgButton(x, y, cardW, cardH, Text.literal(subclass.display),
                    b -> send(RPGStatsMod.SELECT_SUBCLASS, subclass.name()),
                    RpgButton.Kind.CARD, RpgUiTheme.accent(clazz))
                    .subtitle(subclass.desc)
                    .footer("ESPECIALIZACAO");
            addDrawableChild(card);
            addHint(card, subclass.display, subclass.desc, "Clique para escolher esta especializacao.");
        }
    }

    private void addPathSelectionButtons(Layout l,PlayerStats stats) {
        addChoiceButtons(l,RPGPath.forClass(stats.clazz).stream()
                .map(p->new ChoiceOption(p.name(),shortPathName(p),p.desc,p.nodes,pathColor(p),houseIcon(p))).toList());
    }
    private void addSpecializationSelectionButtons(Layout l,PlayerStats stats) {
        if(stats.path==null) return;
        addChoiceButtons(l,RPGSpecialization.forPath(stats.path).stream()
                .map(p->new ChoiceOption(p.name(),p.display,p.desc,p.nodes,specColor(p),"star")).toList());
    }
    private void addChoiceButtons(Layout l,List<ChoiceOption> options) {
        if(choicePage!=page) {choice=choices.forPage(page);choicePage=page;choiceScroll=0;}
        choiceOptions=options;
        if(choice.selected()!=null && options.stream().noneMatch(o->o.id.equals(choice.selected()))) choice.reset();
        int gap=5, count=options.size(), cardW=(l.contentW-gap*(count-1))/Math.max(1,count);
        for(int i=0;i<count;i++) {
            ChoiceOption o=options.get(i);
            RpgButton card=new RpgButton(l.contentX+i*(cardW+gap),l.contentY+30,cardW,85,Text.literal(o.title),
                    b->{if(!choice.pending()) {choice.select(o.id);choiceScroll=0;}},RpgButton.Kind.CHOICE,o.accent).icon(o.icon);
            choiceCards.add(card);addDrawableChild(card);
        }
        choiceConfirm=new RpgButton(l.contentX+l.contentW-180,l.contentY+l.contentH-29,180,25,Text.literal("Confirmar escolha"),b->{
            if(choiceError(ClientStatsStore.stats).isEmpty()) choice.confirm(net.minecraft.util.Util.getMeasuringTimeMs(),id->
                    send(page==Page.AFFINITY?RPGStatsMod.SELECT_AFFINITY:page==Page.PATH_TREE?RPGStatsMod.SELECT_PATH:RPGStatsMod.SELECT_SPECIALIZATION,id));
        },RpgButton.Kind.ACTION,RpgUiTheme.accent(ClientStatsStore.stats.clazz));
        choiceConfirm.active=false;addDrawableChild(choiceConfirm);
    }
    private String choiceError(PlayerStats stats) {
        if(choice.selected()==null) return "Selecione uma opção para comparar.";
        if(page==Page.AFFINITY) return HouseRules.chooseError(stats,RPGPath.valueOf(choice.selected()));
        if(page==Page.PATH_TREE) return stats.level>=StatsManager.PATH_LEVEL && stats.unlockedNodes.contains(stats.clazz.nodes.get(stats.clazz.nodes.size()-1).id())
                ? "" : pathUnlockRequirement(stats);
        return stats.path!=null && stats.level>=StatsManager.SPECIALIZATION_LEVEL && stats.unlockedNodes.contains(pathMasteryNode(stats.path))
                ? "" : "Requer nível 25 e Maestria da Casa.";
    }

    private void addAttributeButtons(Layout l, PlayerStats stats) {
        for (int i = 0; i < Stat.values().length; i++) {
            Rect rect = attributeRect(l, i);
            Stat stat = Stat.values()[i];
            RpgButton plus = new RpgButton(rect.right() - 34, rect.y + (rect.h - 20) / 2, 25, 20, Text.literal("+"),
                    b -> send(RPGStatsMod.ALLOCATE, stat.name()), RpgButton.Kind.SMALL, statColor(stat));
            plus.active = stats.statPoints > 0 && stats.stats.get(stat) < StatsManager.MAX_STAT;
            attributeButtons.add(plus);
            addDrawableChild(plus);
            addHint(plus, stat.display, statDescription(stat),
                    plus.active ? "Clique para gastar 1 PA neste atributo." : "Sem PA disponivel ou atributo no limite.");
        }
    }

    private Rect attributeRect(Layout l, int index) {
        int top = l.contentY + 17;
        int gap = 4;
        int count = Stat.values().length;
        int rowH = Math.max(27, Math.min(39, (l.contentH - 17 - gap * (count - 1)) / count));
        return new Rect(l.contentX, top + index * (rowH + gap), l.contentW, rowH);
    }

    private void addSkillButtons(Layout l, List<SkillNode> nodes, boolean secondary, RPGClass ownerClass) {
        Map<String, Rect> positions = calculateTreeLayout(l, nodes, secondary);
        PlayerStats stats = ClientStatsStore.stats;
        int accent = switch (page) {
            case PATH_TREE -> viewingPath == null ? RpgUiTheme.accent(ownerClass) : pathColor(viewingPath);
            case SPECIALIZATION_TREE -> stats.specialization == null
                    ? (stats.path == null ? RpgUiTheme.accent(ownerClass) : pathColor(stats.path))
                    : specColor(stats.specialization);
            case AFFINITY -> stats.affinityHouse == null
                    ? RpgUiTheme.themedAccent(stats.clazz, stats.path, null) : pathColor(stats.affinityHouse);
            default -> RpgUiTheme.themedAccent(ownerClass, stats.path, stats.affinityHouse);
        };

        for (SkillNode node : nodes) {
            String key = node.id();
            Rect r = positions.get(key);
            if (r == null) continue;

            boolean unlocked = stats.unlockedNodes.contains(key);
            SkillEffect active = AbilityRegistry.activeForNode(key);
            int cost = node.cost();
            boolean canUnlock = canUnlock(stats, node, secondary, cost);
            int equippedSlot = equippedSlot(stats, key);
            boolean selectedEquipped = equippedSlot == activeEquipSlot;

            String footer;
            RpgButton.State state;
            boolean clickable;
            if (unlocked && active != null && selectedEquipped) {
                footer = "ATIVA · SLOT " + ACTIVE_KEYS[activeEquipSlot];
                state = RpgButton.State.EQUIPPED;
                clickable = false;
            } else if (unlocked && active != null) {
                footer = equippedSlot >= 0 ? "MOVER → " + ACTIVE_KEYS[activeEquipSlot] : "EQUIPAR → " + ACTIVE_KEYS[activeEquipSlot];
                state = equippedSlot >= 0 ? RpgButton.State.EQUIPPED : RpgButton.State.UNLOCKED;
                clickable = true;
            } else if (unlocked) {
                footer = "PASSIVA · OK";
                state = RpgButton.State.UNLOCKED;
                clickable = false;
            } else {
                footer = "NV " + node.reqLevel() + " · " + cost + " PH";
                state = canUnlock ? RpgButton.State.DEFAULT : RpgButton.State.BLOCKED;
                clickable = canUnlock;
            }

            String subtitle = active != null ? "ATIVA" : shortEffectLabel(key);
            RpgButton button = new RpgButton(r.x, r.y, r.w, r.h, Text.literal(node.name()), b -> {
                inspectedNode=key;detailScroll=0;
                PlayerStats current = ClientStatsStore.stats;
                if (current.unlockedNodes.contains(key) && AbilityRegistry.hasActive(key)) sendActive(activeEquipSlot, key);
                else if(canUnlock(current,node,false,cost))send(RPGStatsMod.UNLOCK,key);
            }, RpgButton.Kind.NODE, accent)
                    .icon(nodeIcon(node, ownerClass))
                    .subtitle(subtitle)
                    .footer(footer)
                    .state(state);
            // Botões bloqueados/desbloqueados permanecem consultáveis: o tooltip não some após comprar.
            button.active = true;
            addDrawableChild(button);
            nodeVisuals.put(key, new NodeVisual(node, key, secondary, r, button));
        }
    }

    private Map<String, Rect> calculateTreeLayout(Layout l, List<SkillNode> nodes, boolean secondary) {
        Map<String, Integer> depths = new HashMap<>();
        int maxDepth = 1;
        for (SkillNode node : nodes) maxDepth = Math.max(maxDepth, depthOf(nodes, node, depths));

        Map<Integer, List<SkillNode>> byDepth = new LinkedHashMap<>();
        for (int depth = 1; depth <= maxDepth; depth++) byDepth.put(depth, new ArrayList<>());
        for (SkillNode node : nodes) byDepth.get(depths.get(node.id())).add(node);

        int treeX = l.contentX + 4;
        int treeY = l.contentY + 48;
        int treeW = l.contentW - 8;
        int treeH = l.contentH - 52;
        int rowGap = illustratedScreen() ? 10 : maxDepth >= 7 ? 3 : 5;
        int buttonH = Math.max(18, Math.min(illustratedScreen()?42:34,
                (treeH - Math.max(0, maxDepth - 1) * rowGap) / Math.max(1, maxDepth)));

        Map<String, Rect> result = new LinkedHashMap<>();
        for (int depth = 1; depth <= maxDepth; depth++) {
            List<SkillNode> row = byDepth.get(depth);
            int count = row.size();
            if (count == 0) continue;
            int gapX = 6;
            int buttonW = Math.min(164, (treeW - gapX * (count - 1)) / count);
            int usedW = count * buttonW + gapX * (count - 1);
            int startX = treeX + (treeW - usedW) / 2;
            int y = treeY + (depth - 1) * (buttonH + rowGap);
            for (int i = 0; i < count; i++) {
                int x = startX + i * (buttonW + gapX);
                String key = row.get(i).id();
                result.put(key, new Rect(x, y, buttonW, buttonH));
            }
        }
        return result;
    }

    private int depthOf(List<SkillNode> nodes, SkillNode node, Map<String, Integer> cache) {
        Integer known = cache.get(node.id());
        if (known != null) return known;
        if (node.prereqNode() == null) {
            cache.put(node.id(), 1);
            return 1;
        }
        SkillNode parent = findNode(nodes, node.prereqNode());
        int depth = parent == null ? 1 : Math.min(7, depthOf(nodes, parent, cache) + 1);
        cache.put(node.id(), depth);
        return depth;
    }

    private SkillNode findNode(List<SkillNode> nodes, String id) {
        for (SkillNode node : nodes) if (node.id().equals(id)) return node;
        return null;
    }

    private boolean canUnlock(PlayerStats stats, SkillNode node, boolean secondary, int cost) {
        String key = node.id();
        if (stats.unlockedNodes.contains(key) || stats.skillPoints < cost || stats.level < node.reqLevel()) return false;
        if (node.prereqNode() != null) {
            String prereq = node.prereqNode();
            if (!stats.unlockedNodes.contains(prereq)) return false;
        }
        if (node.reqStat() != null && stats.stats.get(node.reqStat()) < node.reqValue()) return false;
        if(HouseRules.borrowed(key)) return HouseRules.purchaseError(stats,key).isEmpty();
        RPGPath pathOwner = RPGPath.ownerOfNode(node.id());
            if (pathOwner != null) {
                return stats.path == pathOwner;
            }
        RPGSpecialization specOwner = RPGSpecialization.ownerOfNode(node.id());
            if (specOwner != null) return stats.specialization == specOwner;
        return true;
    }

    private int equippedSlot(PlayerStats stats, String key) {
        for (int i = 0; i < PlayerStats.ACTIVE_SLOTS; i++) if (key.equals(stats.activeSlot(i))) return i;
        return -1;
    }

    private void changePage(Page target) {
        page = target;
        rebuild();
    }

    private void send(Identifier channel, String payload) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeString(payload);
        RpgNetwork.sendToServer(channel, buffer);
    }

    private void sendActive(int slot, String nodeId) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeVarInt(slot);
        buffer.writeString(nodeId);
        RpgNetwork.sendToServer(RPGStatsMod.SELECT_ACTIVE, buffer);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        PlayerStats stats = ClientStatsStore.stats;
        if(stats.clazz == null && !stats.awakened) {
            awakening.render(context,width,height);
            super.render(context,mouseX,mouseY,delta);
            return;
        }
        if (stats.clazz == null && stats.awakened) {
            classSelection.render(context, width, height);
            super.render(context, mouseX, mouseY, delta);
            classSelection.renderTooltip(context, mouseX, mouseY);
            return;
        }
        if (illustratedScreen()) {
            context.fill(0,0,width,height,CleanRpgUi.BACKGROUND);
            var viewport=mageViewport();
            int localX=(int)Math.floor(viewport.localX(mouseX));
            int localY=(int)Math.floor(viewport.localY(mouseY));
            context.getMatrices().push();
            context.getMatrices().translate(viewport.x(),viewport.y(),0);
            context.getMatrices().scale((float)viewport.scale(),(float)viewport.scale(),1);
            renderCharacterPage(context,stats,localX,localY);
            super.render(context,localX,localY,delta);
            context.getMatrices().pop();
            for(HintVisual hint:hintVisuals) if(hint.button.isMouseOver(localX,localY)) {
                context.drawTooltip(textRenderer,hint.lines,mouseX,mouseY);
                break;
            }
            return;
        }
        int top = stats.clazz == null ? RpgUiTheme.SCREEN_TOP
                : RpgUiTheme.screenTop(stats.clazz, stats.path, stats.affinityHouse);
        int bottom = stats.clazz == null ? RpgUiTheme.SCREEN_BOTTOM
                : RpgUiTheme.screenBottom(stats.clazz, stats.path, stats.affinityHouse);
        context.fillGradient(0, 0, width, height, 0, top, bottom);
        drawAmbientLines(context, stats);

        if (stats.clazz == null) renderClassSelection(context);
        else renderCharacterPage(context, stats, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
        // Nos nós da árvore, o painel lateral é a fonte principal de detalhes.
        renderHintTooltip(context, mouseX, mouseY);
    }

    private void drawAmbientLines(DrawContext context, PlayerStats stats) {
        int cx = width / 2;
        int primary = stats.clazz == null ? 0xFF6F86B5
                : RpgUiTheme.primaryAccent(stats.clazz, stats.path);
        int affinity = stats.affinityHouse == null ? primary : RpgUiTheme.affinityAccent(stats.affinityHouse);
        for (int i = -3; i <= 3; i++) {
            int x = cx + i * 110;
            int color = RpgUiTheme.alpha((i & 1) == 0 ? primary : affinity, 22);
            context.fill(x, 0, x + 1, height, color);
        }
        context.fill(0, height / 2, width, height / 2 + 1,
                RpgUiTheme.alpha(RpgUiTheme.mix(primary, affinity, 0.30f), 18));
    }

    private void renderClassSelection(DrawContext context) {
        PlayerStats stats = ClientStatsStore.stats;
        Layout l = layout();
        CleanRpgUi.surface(context, l.panelX, l.panelY, l.panelW, l.panelH, CleanRpgUi.PANEL, RpgUiTheme.BORDER);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(stats.awakened ? "ESCOLHA SUA CLASSE" : "DESPERTE SEU POTENCIAL"),
                width / 2, l.panelY + 25, CleanRpgUi.TEXT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(stats.awakened ? "Uma classe, uma Casa principal e uma afinidade opcional." : "Despertar concede +3 ao recurso da classe que escolher."),
                width / 2, l.panelY + 43, CleanRpgUi.MUTED);
    }

    private void renderSubclassSelection(DrawContext context, RPGClass clazz) {
        Layout l = layout();
        int accent = RpgUiTheme.accent(clazz);
        int accentText = RpgUiTheme.accessibleAccent(accent, CleanRpgUi.PANEL);
        CleanRpgUi.surface(context, l.panelX, l.panelY, l.panelW, l.panelH, CleanRpgUi.PANEL, RpgUiTheme.BORDER);
        context.fill(l.panelX, l.panelY, l.panelX + l.panelW, l.panelY + 4, accent);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("ESPECIALIZACAO DE " + clazz.display.toUpperCase()),
                width / 2, l.panelY + 27, CleanRpgUi.TEXT);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Nivel " + StatsManager.SUBCLASS_LEVEL + " alcancado · escolha um caminho para aprofundar sua build."),
                width / 2, l.panelY + 47, accentText);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Estas classes ainda usam a arvore v1.4 e serao migradas para o novo modelo depois do Mago."),
                width / 2, l.panelY + 66, CleanRpgUi.MUTED);
    }

    private void renderCharacterPage(DrawContext context, PlayerStats stats, int mouseX, int mouseY) {
        Layout l = layout();
        int accent = RpgUiTheme.themedAccent(stats.clazz, stats.path, stats.affinityHouse);
        int primary = RpgUiTheme.primaryAccent(stats.clazz, stats.path);
        int affinity = RpgUiTheme.affinityAccent(stats.affinityHouse);
        int panel = RpgUiTheme.panelTint(CleanRpgUi.PANEL, stats.clazz, stats.path, stats.affinityHouse, 0.07f);
        int border = RpgUiTheme.alpha(RpgUiTheme.lighten(accent, 0.08f), 220);
        CleanRpgUi.panel(context,l.panelX,l.panelY,l.panelW,l.panelH,CleanRpgUi.BORDER);
        CleanRpgUi.panel(context,l.contentX-5,l.tabsY-4,l.contentW+10,l.panelY+l.panelH-l.tabsY-6,CleanRpgUi.BORDER);

        renderHeader(context, l, stats, accent);
        renderSidebar(context, l, stats, accent, mouseX, mouseY);

        if (!choiceOptions.isEmpty()) {
            renderChoiceSheet(context,l,stats);
        } else if (page == Page.ATTRIBUTES) {
            renderAttributes(context, l, stats);
        } else if (page == Page.PATH_TREE && stats.path == null) {
            renderPathSelectionBackground(context, l, stats);
        } else if (page == Page.SPECIALIZATION_TREE && stats.specialization == null) {
            renderSpecializationSelectionBackground(context, l, stats);
        } else {
            renderTreeBackground(context, l, stats);
        }
    }

    private void renderHeader(DrawContext context, Layout l, PlayerStats stats, int accent) {
        if(illustratedScreen()) {
            CartoonClassArt.emblem(context,stats.clazz,l.panelX+13,l.panelY+10,43);
            CosmicSelectionArt.label(context,textRenderer,stats.clazz.display.toUpperCase(java.util.Locale.ROOT),l.panelX+69,l.panelY+9,2f,RpgUiTheme.accessibleAccent(accent,CleanRpgUi.PANEL));
            String house=stats.path==null?"Sem Casa":shortPathName(stats.path);
            String spec=stats.specialization==null?"Sem especialização":stats.specialization.display;
            drawTrimmed(context,house+" · "+spec,l.panelX+69,l.panelY+30,CleanRpgUi.TEXT,330);
            drawTrimmed(context,buildFlavor(stats),l.panelX+69,l.panelY+44,CleanRpgUi.MUTED,330);
            int x=l.panelX+416, w=l.panelW-434;
            drawTrimmed(context,"Nível "+stats.level+" / "+StatsManager.MAX_LEVEL,x,l.panelY+22,CleanRpgUi.TEXT,w);
            String points="PA "+stats.statPoints+" · PH "+stats.skillPoints;
            context.drawTextWithShadow(textRenderer,Text.literal(points),l.panelX+l.panelW-16-textRenderer.getWidth(points),l.panelY+9,0xFF91552E);
            int needed=stats.level>=StatsManager.MAX_LEVEL?Math.max(1,stats.xp):PlayerStats.xpToNext(stats.level);
            RpgUiTheme.bar(context,x,l.panelY+47,w,7,stats.level>=StatsManager.MAX_LEVEL?1f:Math.min(1f,stats.xp/(float)Math.max(1,needed)),accent);
            String xp=stats.level>=StatsManager.MAX_LEVEL?"NÍVEL MÁXIMO":stats.xp+" / "+needed+" ECOS";
            context.drawTextWithShadow(textRenderer,Text.literal(xp),x+w-textRenderer.getWidth(xp),l.panelY+35,CleanRpgUi.MUTED);
            return;
        }
        int headerBg = RpgUiTheme.panelTint(CleanRpgUi.PANEL, stats.clazz, stats.path, stats.affinityHouse, 0.07f);
        int accentText = RpgUiTheme.accessibleAccent(accent, headerBg);
        String progression;
        if (stats.path != null || RPGPath.forClass(stats.clazz).size() > 0) {
            String path = stats.path == null ? "Sem Casa" : shortPathName(stats.path);
            String spec = stats.specialization == null ? "Sem especializacao" : stats.specialization.display;
            progression = "/ " + path + " / " + spec;
        } else {
            progression = "/ " + (stats.subclass == null ? "Sem especializacao" : stats.subclass.display);
        }
        int left = l.panelX + 13;
        int right = l.panelX + l.panelW - 13;
        int leftWidth = Math.max(80, right - left - 116);
        drawTrimmed(context, stats.clazz.display + "  " + progression, left, l.panelY + 13, accentText, leftWidth);

        drawTrimmed(context, buildFlavor(stats), left, l.panelY + 27, CleanRpgUi.MUTED, leftWidth);

        String level = "NV " + stats.level + "/" + StatsManager.MAX_LEVEL;
        context.drawTextWithShadow(textRenderer, Text.literal(level), right - textRenderer.getWidth(level), l.panelY + 13, CleanRpgUi.TEXT);

        String points = "PA " + stats.statPoints + " · PH " + stats.skillPoints;
        context.drawTextWithShadow(textRenderer, Text.literal(points), right - textRenderer.getWidth(points), l.panelY + 27,
                (stats.statPoints > 0 || stats.skillPoints > 0) ? 0xFF91552E : CleanRpgUi.MUTED);

        int xpX = l.panelX + 13;
        int xpY = l.panelY + 47;
        int xpW = l.panelW - 26;
        int needed = stats.level >= StatsManager.MAX_LEVEL ? Math.max(1, stats.xp) : PlayerStats.xpToNext(stats.level);
        float ratio = stats.level >= StatsManager.MAX_LEVEL ? 1f : Math.min(1f, stats.xp / (float) Math.max(1, needed));
        RpgUiTheme.bar(context, xpX, xpY, xpW, 6, ratio, accent);
        String xpLabel = stats.level >= StatsManager.MAX_LEVEL ? "NIVEL MAXIMO" : stats.xp + " / " + needed + " ECOS";
        context.drawCenteredTextWithShadow(textRenderer, xpLabel, xpX + xpW / 2, xpY - 9, RpgUiTheme.DIM);
    }

    private void renderSidebar(DrawContext context, Layout l, PlayerStats stats, int accent, int mouseX, int mouseY) {
        int primary = RpgUiTheme.primaryAccent(stats.clazz, stats.path);
        int affinity = RpgUiTheme.affinityAccent(stats.affinityHouse);
        int panel = RpgUiTheme.panelTint(CleanRpgUi.PANEL, stats.clazz, stats.path, stats.affinityHouse, 0.09f);
        CleanRpgUi.panel(context,l.sidebarX,l.sidebarY,l.sidebarW,l.sidebarH,CleanRpgUi.BORDER);

        int pad = 8;
        int x = l.sidebarX + pad;
        int w = l.sidebarW - pad * 2;
        int y = l.sidebarY + 8;

        int sidebarAccentText = RpgUiTheme.accessibleAccent(accent, panel);
        context.drawTextWithShadow(textRenderer, Text.literal("BUILD ATUAL"), x, y, sidebarAccentText);
        RpgUiTheme.dualAccentBar(context, x, y + 11, w, 1, primary, affinity);
        y += 18;

        drawSidebarResource(context, stats.clazz, stats.resource, stats.resourceMax, x, y, w);
        y += 20;

        if (stats.clazz == RPGClass.MAGO && stats.path == RPGPath.MAGE_TEMPORAL && stats.temporalFragmentMax() > 0) {
            drawTrimmed(context, "Fragmentos  " + stats.temporalFragments + "/" + stats.temporalFragmentMax(),
                    x, y, RpgUiTheme.accessibleAccent(pathColor(stats.path), panel), w);
            y += 12;
        } else if (stats.clazz == RPGClass.MAGO && stats.path == RPGPath.MAGE_OCCULT) {
            drawTrimmed(context, "Corrupcao  " + Math.round(stats.corruption) + "/100",
                    x, y, RpgUiTheme.accessibleAccent(pathColor(stats.path), panel), w);
            y += 12;
        }

        drawTrimmed(context, "Casa: " + (stats.path == null ? "Nenhuma" : shortPathName(stats.path)),
                x, y, stats.path == null ? RpgUiTheme.DIM : RpgUiTheme.accessibleAccent(pathColor(stats.path), panel), w);
        y += 12;
        drawTrimmed(context, "Especial.: " + (stats.specialization == null ? "Nenhuma" : stats.specialization.display),
                x, y, stats.specialization == null ? RpgUiTheme.DIM : RpgUiTheme.accessibleAccent(specColor(stats.specialization), panel), w);
        y += 12;
        drawTrimmed(context, "Casa 2: " + (stats.affinityHouse == null ? "Nenhuma" : shortPathName(stats.affinityHouse)),
                x, y, stats.affinityHouse == null ? RpgUiTheme.DIM : RpgUiTheme.accessibleAccent(pathColor(stats.affinityHouse), panel), w);
        y += 15;

        int slotY = l.sidebarY + l.sidebarH - 91;
        context.fill(x, y, x + w, y + 1, RpgUiTheme.alpha(accent, 120));
        y += 7;

        DetailTarget detail = resolveDetailTarget(stats, mouseX, mouseY);
        renderAbilityDetailPanel(context, stats, detail, x, y, w, Math.max(18, slotY - y - 4), accent);

        context.fill(x, slotY, x + w, slotY + 1, RpgUiTheme.BORDER_SOFT);
        context.drawCenteredTextWithShadow(textRenderer, "ATIVA · SLOT " + ACTIVE_KEYS[activeEquipSlot],
                l.sidebarX + l.sidebarW / 2, slotY + 7, CleanRpgUi.MUTED);

        String activeId = stats.activeSlot(activeEquipSlot);
        SkillNode active = StatsManager.findNode(stats, activeId);
        int activeColor = active == null ? RpgUiTheme.DIM : accent;
        int activeY = slotY + 19;
        int cardH = 30;
        int activeBg = RpgUiTheme.mix(CleanRpgUi.PANEL,accent,.18f);
        CleanRpgUi.surface(context, x, activeY, w, cardH, activeBg, activeColor);
        String activeName = active == null ? "Nenhuma equipada" : active.name();
        int activeTitleColor = active == null ? CleanRpgUi.MUTED
                : RpgUiTheme.themedText(RpgUiTheme.composite(activeBg, panel), activeColor);
        drawCenteredTrimmed(context, activeName, x + w / 2, activeY + 5,
                activeTitleColor, w - 8);
        String activeHint = active == null ? "Escolha na arvore" : "Use com " + ACTIVE_KEYS[activeEquipSlot];
        int activeTextColor = active == null ? RpgUiTheme.DIM
                : RpgUiTheme.accessibleAccent(activeColor, RpgUiTheme.composite(activeBg, panel));
        drawCenteredTrimmed(context, activeHint, x + w / 2, activeY + 17, activeTextColor, w - 8);

        context.drawCenteredTextWithShadow(textRenderer, "Clique: fixar · Roda: ler", l.sidebarX + l.sidebarW / 2,
                l.sidebarY + l.sidebarH - 14, RpgUiTheme.DIM);
    }

    private DetailTarget resolveDetailTarget(PlayerStats stats, int mouseX, int mouseY) {
        SkillNode inspected=StatsManager.findNode(stats,inspectedNode);
        if(inspected!=null) return new DetailTarget(inspected,inspectedNode,HouseRules.borrowed(inspectedNode));
        for (NodeVisual visual : nodeVisuals.values()) {
            Rect r = visual.rect;
            if (mouseX >= r.x && mouseX < r.right() && mouseY >= r.y && mouseY < r.bottom()) {
                return new DetailTarget(visual.node, visual.key, visual.secondary);
            }
        }

        String activeId = stats.activeSlot(activeEquipSlot);
        SkillNode active = StatsManager.findNode(stats, activeId);
        if (active != null) return new DetailTarget(active, activeId, HouseRules.borrowed(activeId));

        if (stats.lastUnlockedNode != null && !stats.lastUnlockedNode.isBlank()) {
            SkillNode last = StatsManager.findNode(stats, stats.lastUnlockedNode);
            if (last != null) return new DetailTarget(last, stats.lastUnlockedNode,
                    HouseRules.borrowed(stats.lastUnlockedNode));
        }
        return null;
    }

    private void renderAbilityDetailPanel(DrawContext context, PlayerStats stats, DetailTarget target,
                                          int x, int y, int w, int h, int accent) {
        int bottom = y + h;
        int detailBg = RpgUiTheme.panelTint(CleanRpgUi.PANEL, stats.clazz, stats.path, stats.affinityHouse, 0.09f);
        int detailAccent = RpgUiTheme.accessibleAccent(accent, detailBg);
        context.drawTextWithShadow(textRenderer, Text.literal("DETALHES DA HABILIDADE"), x, y, detailAccent);
        y += 13;
        if (target == null) {
            drawTrimmed(context, "Passe o mouse sobre uma habilidade.", x, y, RpgUiTheme.DIM, w);
            return;
        }

        SkillNode node = target.node;
        String key = target.key;
        boolean unlocked = stats.unlockedNodes.contains(key);
        int slot = equippedSlot(stats, key);
        boolean available = !unlocked && canUnlock(stats, node, target.secondary, node.cost());

        int statusColor = slot >= 0 ? detailAccent
                : unlocked ? RpgUiTheme.accessibleAccent(RpgUiTheme.SUCCESS, detailBg)
                : available ? RpgUiTheme.accessibleAccent(0xFF91552E, detailBg) : RpgUiTheme.DIM;
        String status = slot >= 0 ? "EQUIPADA · " + ACTIVE_KEYS[slot]
                : unlocked ? "DESBLOQUEADA"
                : available ? "DISPONIVEL" : "BLOQUEADA";

        drawTrimmed(context, node.name(), x, y, RpgUiTheme.themedText(detailBg, accent), w);
        y += 11;
        if (y < bottom) {
            drawTrimmed(context, status, x, y, statusColor, w);
            y += 11;
        }

        List<String> lines=new ArrayList<>(wrapText(node.description(),w));
        for(String effect:detailEffects(key,node)) lines.addAll(wrapText("• "+effect,w));
        lines.add("NV "+node.reqLevel()+" · "+node.cost()+" PH");
        if(!unlocked) lines.addAll(wrapText("Consulte os requisitos na árvore antes de aprender.",w));
        int visible=Math.max(1,(bottom-y-12)/10);
        detailScroll=Math.max(0,Math.min(detailScroll,Math.max(0,lines.size()-visible)));
        for(int i=detailScroll;i<Math.min(lines.size(),detailScroll+visible);i++) {
            drawTrimmed(context,lines.get(i),x,y,CleanRpgUi.MUTED,w);y+=10;
        }
        if(lines.size()>visible) drawTrimmed(context,"Roda: ler "+(detailScroll+1)+"–"+Math.min(lines.size(),detailScroll+visible)+" / "+lines.size(),x,bottom-9,RpgUiTheme.DIM,w);
    }

    private void renderChoiceSheet(DrawContext c,Layout l,PlayerStats stats) {
        choice.tick(net.minecraft.util.Util.getMeasuringTimeMs());
        String error=choiceError(stats);
        choiceConfirm.active=error.isEmpty()&&!choice.pending();
        for(int i=0;i<choiceCards.size();i++) {
            var b=choiceCards.get(i);b.active=!choice.pending();b.selected(choiceOptions.get(i).id.equals(choice.selected()));
        }
        String title=page==Page.AFFINITY?"AFINIDADE SECUNDÁRIA":page==Page.PATH_TREE?"ESCOLHA SUA CASA":"ESCOLHA SUA ESPECIALIZAÇÃO";
        drawTrimmed(c,title,l.contentX+3,l.contentY+2,CleanRpgUi.TEXT,l.contentW-6);
        drawTrimmed(c,"Compare antes de confirmar. A escolha é permanente.",l.contentX+3,l.contentY+16,CleanRpgUi.MUTED,l.contentW-6);
        int x=l.contentX,y=l.contentY+123,w=l.contentW,h=l.contentH-160;
        CleanRpgUi.panel(c,x,y,w,h,CleanRpgUi.BORDER);
        ChoiceOption selected=choiceOptions.stream().filter(o->o.id.equals(choice.selected())).findFirst().orElse(null);
        if(selected==null) {drawTrimmed(c,"Selecione um emblema para abrir a ficha.",x+12,y+16,CleanRpgUi.MUTED,w-24);return;}
        drawTrimmed(c,selected.title,x+12,y+10,CleanRpgUi.TEXT,w-24);
        int leftW=page==Page.AFFINITY?(w-36)/2:w-24;
        List<String> lines=new ArrayList<>(wrapText(selected.description,leftW));
        lines.add("");
        for(SkillNode n:selected.nodes) {
            lines.addAll(wrapText(n.name()+" · NV "+n.reqLevel()+" · "+n.cost()+" PH",leftW));
            lines.addAll(wrapText(n.description(),leftW));
            for(String effect:detailEffects(n.id(),n)) lines.addAll(wrapText("• "+effect,leftW));
            lines.add("");
        }
        int visible=Math.max(1,(h-51)/10);
        choiceScroll=Math.max(0,Math.min(choiceScroll,Math.max(0,lines.size()-visible)));
        for(int i=choiceScroll;i<Math.min(lines.size(),choiceScroll+visible);i++) drawTrimmed(c,lines.get(i),x+12,y+29+(i-choiceScroll)*10,CleanRpgUi.MUTED,leftW);
        if(page==Page.AFFINITY) {
            int rx=x+w/2+6,rw=w/2-18;
            drawTrimmed(c,"CUSTOS E PENALIDADES",rx,y+29,0xFF91552E,rw);
            String penalty=HouseRules.penaltyText(RPGPath.valueOf(selected.id));
            int py=y+44;
            for(String line:wrapText(penalty,rw)) {if(py+9>y+h-22) break;drawTrimmed(c,line,rx,py,CleanRpgUi.MUTED,rw);py+=10;}
            drawTrimmed(c,"Até 4 talentos · 2 PH cada",rx,y+h-19,CleanRpgUi.MUTED,rw);
        }
        if(lines.size()>visible) drawTrimmed(c,"Roda: ver todos os talentos e efeitos",x+12,y+h-17,RpgUiTheme.DIM,leftW);
        String status=choice.pending()?"Aguardando o servidor...":choice.timedOut()?"Sem resposta. Confirme novamente.":error.isEmpty()?"Pronto para confirmar.":error;
        drawTrimmed(c,status,l.contentX+3,l.contentY+l.contentH-20,error.isEmpty()?CleanRpgUi.MUTED:0xFF91552E,l.contentW-192);
    }

    private void drawSidebarResource(DrawContext context, RPGClass clazz, float value, float max, int x, int y, int w) {
        PlayerStats stats = ClientStatsStore.stats;
        int color = RpgUiTheme.themedAccent(clazz, stats.path, stats.affinityHouse);
        String label = clazz.resourceName();
        context.drawTextWithShadow(textRenderer, Text.literal(label), x, y, color);
        String values = (int) value + "/" + (int) max;
        context.drawTextWithShadow(textRenderer, Text.literal(values), x + w - textRenderer.getWidth(values), y, CleanRpgUi.MUTED);
        float ratio = max <= 0 ? 0f : value / max;
        RpgUiTheme.bar(context, x, y + 10, w, 6, ratio, color);
    }

    private void renderAttributes(DrawContext context, Layout l, PlayerStats stats) {
        Map<Stat, Integer> totals = stats.totalStats();
        for (int i = 0; i < Stat.values().length; i++) {
            Stat stat = Stat.values()[i];
            Rect r = attributeRect(l, i);
            int color = statColor(stat);
            CleanRpgUi.surface(context, r.x, r.y, r.w, r.h, CleanRpgUi.PANEL, RpgUiTheme.BORDER_SOFT);
            context.fill(r.x + 1, r.y + 1, r.x + 4, r.bottom() - 1, color);

            int base = stats.stats.get(stat);
            int total = totals.getOrDefault(stat, base);
            int iconSize = Math.min(26, r.h - 10);
            int iconX = r.x + 8;
            int iconY = r.y + (r.h - iconSize) / 2;
            CleanRpgUi.surface(context, iconX, iconY, iconSize, iconSize, RpgUiTheme.alpha(color, 60), color);
            context.drawCenteredTextWithShadow(textRenderer, statAbbr(stat), iconX + iconSize / 2, iconY + (iconSize - 8) / 2, color);

            int textX = iconX + iconSize + 8;
            context.drawTextWithShadow(textRenderer, Text.literal(stat.display), textX, r.y + 6, CleanRpgUi.TEXT);
            String amount = total == base ? String.valueOf(base) : base + "  (" + total + ")";
            int amountX = textX + Math.min(112, textRenderer.getWidth(stat.display) + 12);
            context.drawTextWithShadow(textRenderer, Text.literal("Base/total: " + amount), amountX, r.y + 6,
                    total > base ? RpgUiTheme.SUCCESS : color);
            drawTrimmed(context, statDescription(stat), textX, r.y + 19, CleanRpgUi.MUTED, Math.max(30, r.w - (textX - r.x) - 49));

            if (r.h >= 40) {
                int barX = textX;
                int barY = r.bottom() - 8;
                int barW = Math.max(28, r.w - (barX - r.x) - 47);
                RpgUiTheme.bar(context, barX, barY, barW, 4, base / (float) StatsManager.MAX_STAT, color);
            }
        }

        String tip = stats.statPoints > 0
                ? "Voce tem " + stats.statPoints + " ponto(s) de atributo para distribuir."
                : "Atributos base + bonus verdes das arvores desbloqueadas.";
        context.drawTextWithShadow(textRenderer, Text.literal(tip), l.contentX + 3, l.contentY + 1,
                stats.statPoints > 0 ? 0xFF91552E : RpgUiTheme.DIM);
    }

    private void renderPathSelectionBackground(DrawContext context, Layout l, PlayerStats stats) {
        int accent = RpgUiTheme.accent(stats.clazz);
        int contentBg = RpgUiTheme.panelTint(CleanRpgUi.PANEL, stats.clazz, stats.path, stats.affinityHouse, 0.06f);
        int accentText = RpgUiTheme.accessibleAccent(accent, contentBg);
        String mastery = stats.clazz.nodes.get(stats.clazz.nodes.size() - 1).id();
        boolean unlocked = stats.unlockedNodes.contains(mastery);
        context.drawTextWithShadow(textRenderer, Text.literal("ESCOLHA SUA CASA"), l.contentX + 3, l.contentY + 2, accentText);
        String state = unlocked
                ? "Compare os estilos abaixo. Passe o mouse para ler todos os detalhes."
                : pathUnlockRequirement(stats);
        context.drawTextWithShadow(textRenderer, Text.literal(state), l.contentX + 3, l.contentY + 16,
                unlocked ? CleanRpgUi.MUTED : 0xFF91552E);
    }

    private void renderSpecializationSelectionBackground(DrawContext context, Layout l, PlayerStats stats) {
        int contentBg = RpgUiTheme.panelTint(CleanRpgUi.PANEL, stats.clazz, stats.path, stats.affinityHouse, 0.06f);
        int specAccent = stats.path == null ? RpgUiTheme.DIM
                : RpgUiTheme.accessibleAccent(pathColor(stats.path), contentBg);
        context.drawTextWithShadow(textRenderer, Text.literal("ESCOLHA SEU RAMO FINAL"), l.contentX + 3, l.contentY + 2,
                specAccent);
        if (stats.path == null) {
            context.drawTextWithShadow(textRenderer, Text.literal("Escolha sua Casa primeiro."), l.contentX + 3, l.contentY + 17, 0xFF91552E);
            return;
        }
        boolean mastery = stats.unlockedNodes.contains(pathMasteryNode(stats.path));
        String line = mastery ? "Tres estilos finais. Passe o mouse para comparar antes de escolher."
                : "Complete a Maestria da Casa antes de escolher uma especializacao.";
        context.drawTextWithShadow(textRenderer, Text.literal(line), l.contentX + 3, l.contentY + 17,
                mastery ? CleanRpgUi.MUTED : 0xFF91552E);
    }

    private void renderTreeBackground(DrawContext context, Layout l, PlayerStats stats) {
        int accent = RpgUiTheme.themedAccent(stats.clazz, stats.path, stats.affinityHouse);
        int primary = RpgUiTheme.primaryAccent(stats.clazz, stats.path);
        int affinity = RpgUiTheme.affinityAccent(stats.affinityHouse);
        if(!illustratedScreen()) RpgUiTheme.treeSigil(context, l.contentX + 3, l.contentY + 30,
                l.contentW - 6, l.contentH - 32, primary, affinity);
        String title;
        if (page == Page.AFFINITY) {
            title = stats.affinityHouse==null?"ESCOLHA UMA CASA SECUNDÁRIA":stats.affinityHouse.display+" · AFINIDADE";
            if (stats.affinityHouse != null) accent = pathColor(stats.affinityHouse);
        } else if (page == Page.PATH_TREE && stats.path != null) {
            title = shortPathName(stats.path).toUpperCase() + " · CASA PRINCIPAL";
            accent = pathColor(stats.path);
        } else if (page == Page.SPECIALIZATION_TREE && stats.specialization != null) {
            title = stats.specialization.display.toUpperCase() + " · ESPECIALIZACAO";
            accent = specColor(stats.specialization);
        } else {
            title = stats.clazz == RPGClass.MAGO ? "MAGO · NUCLEO ARCANO" : stats.clazz.display.toUpperCase() + " · ARVORE BASE";
        }
        int contentBg = RpgUiTheme.panelTint(CleanRpgUi.PANEL, stats.clazz, stats.path, stats.affinityHouse, 0.06f);
        int titleColor = RpgUiTheme.accessibleAccent(accent, contentBg);
        String right = stats.skillPoints + " PH";
        drawTrimmed(context, title, l.contentX + 3, l.contentY + 2, titleColor,
                l.contentW - textRenderer.getWidth(right) - 16);
        context.drawTextWithShadow(textRenderer, Text.literal(right), l.contentX + l.contentW - textRenderer.getWidth(right),
                l.contentY + 2, stats.skillPoints > 0 ? 0xFF91552E : RpgUiTheme.DIM);

        String help = page == Page.AFFINITY
                ? "Até 4 talentos · +custo "+clean(HouseRules.surcharge(stats)*100)+"% · +dano recebido "+clean(HouseRules.vulnerability(stats)*100)+"% · -dano "+clean(HouseRules.offensePenalty(stats)*100)+"%"
                : "Clique: fixar e aprender · Ativa: equipar · Roda na ficha: detalhes";
        drawTrimmed(context, help, l.contentX + 3, l.contentY + 16, RpgUiTheme.DIM, l.contentW - 6);

        for (NodeVisual visual : nodeVisuals.values()) {
            SkillNode node = visual.node;
            if (node.prereqNode() == null) continue;
            String parentKey = visual.node.prereqNode();
            NodeVisual parent = nodeVisuals.get(parentKey);
            if (parent == null) continue;
            boolean pathUnlocked = stats.unlockedNodes.contains(parentKey) && stats.unlockedNodes.contains(visual.key);
            int lineColor = pathUnlocked ? RpgUiTheme.alpha(accent, 240) : (illustratedScreen() ? RpgUiTheme.darken(accent,.38f) : 0xFF4A5368);
            drawConnection(context, parent.rect, visual.rect, lineColor);
        }

        if (page == Page.AFFINITY && stats.affinityHouse == null) {
            context.drawCenteredTextWithShadow(textRenderer, "Cada Casa oferece talentos próprios com penalidades.",
                    l.contentX + l.contentW / 2, l.contentY + l.contentH / 2, CleanRpgUi.MUTED);
        }
    }

    private void drawConnection(DrawContext context, Rect from, Rect to, int color) {
        int x1 = from.centerX();
        int y1 = from.bottom();
        int x2 = to.centerX();
        int y2 = to.y;
        int mid = y1 + Math.max(2, (y2 - y1) / 2);
        RpgUiTheme.line(context, x1, y1, x1, mid, color);
        RpgUiTheme.line(context, x1, mid, x2, mid, color);
        RpgUiTheme.line(context, x2, mid, x2, y2, color);
    }

    private void renderNodeTooltip(DrawContext context, int mouseX, int mouseY) {
        PlayerStats stats = ClientStatsStore.stats;
        for (NodeVisual visual : nodeVisuals.values()) {
            if (mouseX < visual.rect.x || mouseX >= visual.rect.right()
                    || mouseY < visual.rect.y || mouseY >= visual.rect.bottom()) continue;
            SkillNode node = visual.node;
            String key = visual.key;
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal(node.name()).formatted(Formatting.BOLD, Formatting.WHITE));
            addWrapped(lines, node.description(), Formatting.GRAY);

            boolean secondaryEffect = false;
            List<SkillEffect> effects = AbilityRegistry.get(secondaryEffect ? key.substring(4) : key);
            if (!effects.isEmpty()) {
                lines.add(Text.literal("Efeitos").formatted(Formatting.DARK_AQUA));
                for (SkillEffect effect : effects) {
                    SkillEffect shown = secondaryEffect ? effect : effect;
                    addWrapped(lines, "• " + effectDescription(secondaryEffect ? key.substring(4) : key, shown),
                            effect.type() == AbilityType.ACTIVE ? Formatting.AQUA : Formatting.GREEN);
                }
            }
            if (HouseRules.borrowed(key)) {
                addWrapped(lines, HouseRules.penaltyText(stats.affinityHouse), Formatting.RED);
                addWrapped(lines, "Valores efetivos da afinidade, já reduzidos por categoria.", Formatting.YELLOW);
            }

            int cost = visual.node.cost();
            lines.add(Text.literal("Custo: " + cost + " PH").formatted(Formatting.GOLD));
            boolean levelOk = stats.level >= node.reqLevel();
            lines.add(Text.literal("Nivel: " + node.reqLevel()).formatted(levelOk ? Formatting.GREEN : Formatting.RED));
            if (node.reqStat() != null) {
                boolean ok = stats.stats.get(node.reqStat()) >= node.reqValue();
                lines.add(Text.literal("Requer " + node.reqValue() + " " + node.reqStat().display)
                        .formatted(ok ? Formatting.GREEN : Formatting.RED));
            }
            if (node.prereqNode() != null) {
                String prereqKey = visual.node.prereqNode();
                SkillNode prereq = findNodeForVisual(prereqKey);
                String name = prereq == null ? node.prereqNode() : prereq.name();
                boolean ok = stats.unlockedNodes.contains(prereqKey);
                lines.add(Text.literal("Pre-requisito: " + name).formatted(ok ? Formatting.GREEN : Formatting.RED));
            }
            boolean unlocked = stats.unlockedNodes.contains(key);
            int slot = equippedSlot(stats, key);
            if (slot >= 0) {
                lines.add(Text.literal("Equipada no slot " + ACTIVE_KEYS[slot]).formatted(Formatting.AQUA));
                if (slot != activeEquipSlot) lines.add(Text.literal("Clique para mover ao slot " + ACTIVE_KEYS[activeEquipSlot]).formatted(Formatting.YELLOW));
            } else if (unlocked) {
                lines.add(Text.literal(AbilityRegistry.hasActive(key) ? "Clique para equipar em " + ACTIVE_KEYS[activeEquipSlot] : "Desbloqueado")
                        .formatted(Formatting.GREEN));
            } else if (!canUnlock(stats, node, visual.secondary, visual.node.cost())) {
                lines.add(Text.literal("Bloqueado").formatted(Formatting.RED));
            } else {
                lines.add(Text.literal("Clique para desbloquear").formatted(Formatting.YELLOW));
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY);
            return;
        }
    }

    private void renderHintTooltip(DrawContext context, int mouseX, int mouseY) {
        for (HintVisual hint : hintVisuals) {
            if (!hint.button.isMouseOver(mouseX, mouseY)) continue;
            context.drawTooltip(textRenderer, hint.lines, mouseX, mouseY);
            return;
        }
    }

    private void addHint(RpgButton button, String title, String description, String action) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(title).formatted(Formatting.BOLD, Formatting.WHITE));
        addWrapped(lines, description, Formatting.GRAY);
        if (action != null && !action.isBlank()) addWrapped(lines, action, Formatting.YELLOW);
        hintVisuals.add(new HintVisual(button, List.copyOf(lines)));
    }

    private void addWrapped(List<Text> lines, String raw, Formatting formatting) {
        for (String line : wrapText(raw, 230)) lines.add(Text.literal(line).formatted(formatting));
    }

    private List<String> wrapText(String raw, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        StringBuilder line = new StringBuilder();
        for (String word : raw.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && textRenderer.getWidth(candidate) > maxWidth) {
                result.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result;
    }

    private SkillNode findNodeForVisual(String key) {
        NodeVisual visual = nodeVisuals.get(key);
        return visual == null ? null : visual.node;
    }

    private String shortEffectLabel(String key) {
        List<SkillEffect> effects = AbilityRegistry.get(key.startsWith("sec_") ? key.substring(4) : key);
        if (effects.isEmpty()) return "MECANICA";
        SkillEffect first = effects.get(0);
        return first.type() == AbilityType.ACTIVE ? "ATIVA" : shortEffectName(first.effectId());
    }

    private String shortEffectName(String id) {
        return switch (id) {
            case "melee_damage" -> "DANO MELEE";
            case "ranged_damage" -> "DANO RANGED";
            case "magic_power" -> "PODER MAGICO";
            case "lifesteal", "spell_lifesteal" -> "ROUBO DE VIDA";
            case "damage_reduction" -> "DEFESA";
            case "crit_chance", "spell_crit" -> "CRITICO";
            case "poison_hit" -> "VENENO";
            case "thorns" -> "ESPINHOS";
            case "attack_speed" -> "VEL. ATAQUE";
            case "move_speed" -> "MOVIMENTO";
            case "resource_on_hit", "resource_on_hurt", "mana_flat", "mana_regen_flat", "mana_max_pct" -> "RECURSO";
            case "heal_on_kill" -> "CURA/KILL";
            case "cooldown_recovery" -> "COOLDOWN";
            case "elemental_damage", "reaction_damage" -> "ELEMENTAL";
            case "summon_damage", "bond_flat" -> "CONJURACAO";
            default -> "PASSIVA";
        };
    }

    private String effectDescription(String nodeId, SkillEffect effect) {
        if (effect.type() == AbilityType.ACTIVE) {
            String summary = AbilityRegistry.activeSummary(nodeId);
            if (!summary.isBlank()) return summary;
        }
        float v = effect.value();
        return switch (effect.effectId()) {
            case "melee_damage" -> percent(v) + " dano corpo a corpo";
            case "ranged_damage" -> percent(v) + " dano a distancia";
            case "magic_power" -> percent(v) + " poder magico";
            case "lifesteal", "spell_lifesteal" -> percent(v) + " roubo de vida";
            case "damage_reduction" -> percent(v) + " reducao de dano";
            case "crit_chance", "spell_crit" -> percent(v) + " chance de critico";
            case "poison_hit" -> "Veneno em ataques (potencia " + clean(v) + ")";
            case "thorns" -> percent(v) + " espinhos";
            case "attack_speed" -> percent(v) + " velocidade de ataque";
            case "move_speed" -> percent(v) + " velocidade de movimento";
            case "resource_on_hit" -> "+" + clean(v) + " recurso ao acertar";
            case "resource_on_hurt" -> "+" + clean(v) + " recurso ao receber dano";
            case "heal_on_kill" -> "+" + clean(v) + " HP ao eliminar inimigo";
            case "mana_flat" -> "+" + clean(v) + " Mana maxima";
            case "mana_regen_flat" -> "+" + clean(v) + " Mana/s";
            case "mana_max_pct" -> percent(v) + " Mana maxima";
            case "mana_cost_reduction" -> percent(v) + " eficiencia de Mana";
            case "cooldown_recovery" -> percent(v) + " recuperacao de cooldown";
            case "elemental_damage" -> percent(v) + " dano elemental";
            case "reaction_damage" -> percent(v) + " dano de reacoes";
            case "summon_damage" -> percent(v) + " dano de invocacoes";
            case "summon_duration" -> percent(v) + " duracao/estabilidade de invocacoes";
            case "summon_defense" -> percent(v) + " defesa com invocacao proxima";
            case "summon_refund" -> percent(v) + " do custo devolvido ao expirar (gate 1s)";
            case "summon_diversity_damage" -> "+" + percent(v) + " dano por variedade de invocacoes (cap da skill)";
            case "astral_resonance" -> "+" + percent(v) + " poder por constructo diferente (cap da skill)";
            case "bond_flat" -> "+" + clean(v) + " Pontos de Vinculo";
            case "concentration_retention" -> percent(v) + " menos perda de Concentracao ao sofrer dano";
            case "concentration_reaction" -> "+" + clean(v) + " Concentracao por reacao (com cap por cast)";
            case "elemental_cost_reduction" -> percent(v) + " menor custo de Mana elemental";
            case "fire_damage_marked" -> percent(v) + " dano de fogo contra alvos aquecidos";
            case "fire_crit_hot" -> percent(v) + " critico de fogo contra alvos muito aquecidos";
            case "frozen_fragility" -> percent(v) + " dano no proximo golpe contra alvo congelado";
            case "reaction_aoe" -> "+" + clean(v) + " HP na explosao de reacao em area";
            case "shatter_damage" -> "+" + clean(v) + " HP na explosao ao estilhacar";
            case "chain_concentration" -> "+" + clean(v) + " Concentracao por salto de raio (com cap por cast)";
            case "rune_limit" -> "+" + clean(v) + " limite de runas";
            case "illusion_echo" -> percent(v) + " chance controlada de eco ilusorio";
            case "mage_echo" -> percent(v) + " chance de Eco Arcano (35% do dano; CD interno)";
            case "teleport_spell_bonus" -> percent(v) + " dano da proxima magia apos mobilidade espacial";
            case "curse_duration" -> percent(v) + " duracao de maldicoes";
            case "mana_regen_pct" -> percent(v) + " regeneracao de Mana quando a condicao da skill estiver ativa";
            case "stasis_magic_amp" -> percent(v) + " dano magico contra alvos em Stasis";
            case "temporal_second_chance" -> "Segunda Chance temporal com cooldown interno";
            case "hex_melee_magic" -> "+" + clean(v) + " dano magico em ataques imbuídos";
            case "spellblade_rhythm" -> percent(v) + " dano por stack do ritmo melee-spell-melee";
            case "temporal_fragment_max" -> "+" + clean(v) + " Fragmentos Temporais maximos";
            default -> com.rpgstats.ability.ClassAbilityRegistry.describe(effect);
        };
    }

    private String pageName(Page target) {
        return switch (target) {
            case ATTRIBUTES -> "Atributos";
            case CLASS_TREE -> "Arvore da Classe";
            case PATH_TREE -> "Casa principal";
            case SPECIALIZATION_TREE -> "Ramo de Especializacao";
            case AFFINITY -> "Casa secundária";
        };
    }

    private String classOriginStats(RPGClass clazz) {
        return switch (clazz) {
            case GUERREIRO -> "Origem: VIT 8 · TEN 8 · FOR 10 · DES 3 · INT 1 · FE 2 · ARC 1.";
            case MAGO -> "Origem: VIT 5 · TEN 5 · FOR 2 · DES 4 · INT 10 · FE 3 · ARC 4.";
            case ARQUEIRO -> "Origem: VIT 6 · TEN 7 · FOR 3 · DES 10 · INT 2 · FE 2 · ARC 3.";
            case ASSASSINO -> "Origem: VIT 5 · TEN 7 · FOR 3 · DES 9 · INT 2 · FE 1 · ARC 6.";
        };
    }

    private String pageDescription(Page target, PlayerStats stats) {
        return switch (target) {
            case ATTRIBUTES -> "Distribua PA entre Vitalidade, Tenacidade, Forca, Destreza, Inteligencia, Fe e Arcano.";
            case CLASS_TREE -> "Fundamentos da classe. Esta arvore abre o recurso principal e libera a escolha de Casa.";
            case PATH_TREE -> "Mostra somente a Casa principal escolhida. Define a mecanica central e libera tres especializacoes.";
            case SPECIALIZATION_TREE -> "Aprofunda sua Casa em um estilo de combate especializado.";
            case AFFINITY -> "Uma outra Casa da sua classe. Quatro talentos com efeitos reduzidos e penalidades permanentes enquanto aprendidos.";
        };
    }

    private String tabRequirement(Page target, PlayerStats stats) {
        return switch (target) {
            case ATTRIBUTES, CLASS_TREE -> "Disponivel agora.";
            case PATH_TREE -> stats.level >= StatsManager.PATH_LEVEL
                    ? "Disponivel; a escolha exige a Maestria da Classe."
                    : "Desbloqueia no nivel " + StatsManager.PATH_LEVEL + ".";
            case SPECIALIZATION_TREE -> stats.path != null && stats.level >= StatsManager.SPECIALIZATION_LEVEL
                    ? "Disponivel; a escolha exige a Maestria da Casa."
                    : "Exige uma Casa e nivel " + StatsManager.SPECIALIZATION_LEVEL + ".";
            case AFFINITY -> "Requer nível 30 e Casa principal. Confira os custos e penalidades antes de escolher.";
        };
    }

    private boolean nodeBelongsToCurrentPage(PlayerStats stats, String key) {
        boolean secondary = key.startsWith("sec_");
        if (page == Page.AFFINITY) return HouseRules.borrowed(key);
        if (secondary) return false;
        String id = key;
        if (page == Page.CLASS_TREE) return stats.clazz != null && stats.clazz.findNode(id) != null;
        if (page == Page.PATH_TREE) return stats.path != null && stats.path.findNode(id) != null;
        return page == Page.SPECIALIZATION_TREE && stats.specialization != null && stats.specialization.findNode(id) != null;
    }

    private String pathUnlockRequirement(PlayerStats stats) {
        SkillNode mastery = stats.clazz.nodes.get(stats.clazz.nodes.size() - 1);
        if (stats.level < StatsManager.PATH_LEVEL) return "Alcance o nivel " + StatsManager.PATH_LEVEL + ".";
        return "Aprenda " + mastery.name() + " na aba Classe.";
    }

    private String shortPathName(RPGPath path) {
        return path.display.replace("Casa da ", "").replace("Casa do ", "").replace("Casa dos ", "").replace("Casa de ", "").replace("Casa ", "");
    }

    private int pathColor(RPGPath path) {
        return path == null ? RpgUiTheme.DIM : RpgUiTheme.houseAccent(path);
    }

    private String tabIcon(Page target) {
        return switch (target) {
            case ATTRIBUTES -> "stats";
            case CLASS_TREE -> "class";
            case PATH_TREE -> "house";
            case SPECIALIZATION_TREE -> "spec";
            case AFFINITY -> "affinity";
        };
    }

    private String classIcon(RPGClass clazz) {
        return switch (clazz) {
            case GUERREIRO -> "shield";
            case MAGO -> "star";
            case ARQUEIRO -> "bow";
            case ASSASSINO -> "dagger";
        };
    }

    private String uiClassDescription(RPGClass clazz) {
        return switch (clazz) {
            case GUERREIRO -> "Combate marcial · Furia · Guarda";
            case MAGO -> "Magia profunda · Mana · Casas arcanas";
            case ARQUEIRO -> "Precisao · Foco · Mobilidade";
            case ASSASSINO -> "Combos · Energia · Execucao";
        };
    }

    private String houseIcon(RPGPath path) {
        if (path == null) return "house";
        return switch (path) {
            case MAGE_TEMPORAL -> "clock";
            case MAGE_ELEMENTAL -> "flame";
            case MAGE_ARCANA -> "star";
            case MAGE_CONJURATION -> "link";
            case MAGE_OCCULT -> "dagger";
            case WAR_VANGUARD -> "shield";
            case WAR_BERSERKER -> "flame";
            case WAR_WEAPONMASTER -> "dagger";
            case WAR_RUNIC -> "star";
            case WAR_COMMANDER -> "shield";
            case ARC_MARKSMAN, ARC_SKIRMISHER -> "bow";
            case ARC_WARDEN -> "link";
            case ARC_ARCANE -> "star";
            case ARC_ARTIFICER -> "crystal";
            case ASS_SHADOW, ASS_MYSTIC -> "dagger";
            case ASS_VENOM -> "flame";
            case ASS_DUELIST -> "shield";
            case ASS_SABOTEUR -> "crystal";
        };
    }

    private String nodeIcon(SkillNode node, RPGClass ownerClass) {
        String id = node.id().toLowerCase(java.util.Locale.ROOT);
        String name = node.name().toLowerCase(java.util.Locale.ROOT);
        if (id.contains("master") || id.contains("asc_") || name.contains("maestr")) return "star";
        if (id.contains("time") || name.contains("temporal") || name.contains("tempo") || name.contains("cron")) {
            if (name.contains("fragment") || name.contains("memoria") || name.contains("momento")) return "crystal";
            if (name.contains("ritmo") || name.contains("eco")) return "swirl";
            if (name.contains("desloc") || name.contains("recuper")) return "arrow";
            if (name.contains("continuidade")) return "loop";
            return "clock";
        }
        if (name.contains("fogo") || name.contains("brasa") || name.contains("furia") || name.contains("sangue")) return "flame";
        if (name.contains("escudo") || name.contains("guarda") || name.contains("defesa") || name.contains("bastiao")) return "shield";
        if (name.contains("runa") || name.contains("arcano") || name.contains("magia")) return "star";
        if (name.contains("invoc") || name.contains("vinculo") || name.contains("elo")) return "link";
        if (name.contains("flecha") || name.contains("mira") || name.contains("tiro")) return "bow";
        if (name.contains("lamina") || name.contains("golpe") || name.contains("execu") || name.contains("corte")) return "dagger";
        if (name.contains("luz") || name.contains("fe") || name.contains("sagrado")) return "sun";
        return classIcon(ownerClass);
    }

    private String buildFlavor(PlayerStats stats) {
        if (stats.clazz == null) return "";
        return switch (stats.clazz) {
            case MAGO -> {
                String resource = "Magia profunda · Mana";
                if (stats.path == RPGPath.MAGE_TEMPORAL) resource += " · Fragmentos Temporais";
                else if (stats.path == RPGPath.MAGE_OCCULT) resource += " · Corrupcao";
                else if (stats.path == RPGPath.MAGE_CONJURATION) resource += " · Vinculo";
                else if (stats.path == RPGPath.MAGE_ELEMENTAL) resource += " · Reacoes elementais";
                else if (stats.path == RPGPath.MAGE_ARCANA) resource += " · Selos arcanos";
                yield resource;
            }
            case GUERREIRO -> "Combate marcial · Furia · Guarda";
            case ARQUEIRO -> "Precisao · Foco · Mobilidade";
            case ASSASSINO -> "Combos · Energia · Execucao";
        };
    }

    private List<String> detailEffects(String key, SkillNode node) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Stat, Integer> bonus : node.bonus().entrySet()) {
            if (bonus.getValue() == 0) continue;
            String value = bonus.getValue() > 0 ? "+" + bonus.getValue() : Integer.toString(bonus.getValue());
            lines.add(value + " " + bonus.getKey().display);
        }
        for (SkillEffect effect : AbilityRegistry.get(key)) {
            String line = effectDescription(key, effect);
            if (line != null && !line.isBlank() && !lines.contains(line)) lines.add(line);
        }
        if (HouseRules.borrowed(key)) {
            lines.add("Afinidade com valores reduzidos");
        }
        return lines;
    }

    private int specColor(RPGSpecialization spec) {
        return pathColor(spec.parent);
    }

    private String pathMasteryNode(RPGPath path) {
        return path.nodes.get(path.nodes.size() - 1).id();
    }

    private String percent(float value) {
        return (value >= 0 ? "+" : "") + Math.round(value * 100f) + "%";
    }

    private String clean(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) return Integer.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private int statColor(Stat stat) {
        return switch (stat) {
            case VITALIDADE -> 0xFFFF8DA1;
            case TENACIDADE -> 0xFF73D673;
            case FORCA -> 0xFFFF667A;
            case DESTREZA -> 0xFFFFC857;
            case INTELIGENCIA -> 0xFF67A8FF;
            case FE -> 0xFFFFE28A;
            case ARCANO -> 0xFFC57AFF;
        };
    }

    private String statAbbr(Stat stat) {
        return switch (stat) {
            case VITALIDADE -> "VIT";
            case TENACIDADE -> "TEN";
            case FORCA -> "FOR";
            case DESTREZA -> "DES";
            case INTELIGENCIA -> "INT";
            case FE -> "FE";
            case ARCANO -> "ARC";
        };
    }

    private String statDescription(Stat stat) {
        return switch (stat) {
            case VITALIDADE -> "Aumenta HP; retornos menores depois de 20 e 35.";
            case TENACIDADE -> "Aumenta Stamina maxima e regeneracao; caps em 20/35.";
            case FORCA -> "Dano corpo a corpo; Guerreiro tambem escala com nivel.";
            case DESTREZA -> "Velocidade; dano de arco e golpes do Assassino.";
            case INTELIGENCIA -> "Mana; dano magico cresce com Inteligencia e nivel.";
            case FE -> "Afinidade espiritual, cura e efeitos sagrados.";
            case ARCANO -> "Veneno/ocultismo, duracao de aflicoes e Sorte.";
        };
    }

    private void drawTrimmed(DrawContext context, String raw, int x, int y, int color, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (textRenderer.getWidth(text) > maxWidth) {
            text = textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("..."))) + "...";
        }
        context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, color);
    }

    private void drawCenteredTrimmed(DrawContext context, String raw, int cx, int y, int color, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (textRenderer.getWidth(text) > maxWidth) {
            text = textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("..."))) + "...";
        }
        context.drawCenteredTextWithShadow(textRenderer, text, cx, y, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
