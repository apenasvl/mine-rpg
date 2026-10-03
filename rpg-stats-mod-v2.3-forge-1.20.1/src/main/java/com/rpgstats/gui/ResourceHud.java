package com.rpgstats.gui;

import com.rpgstats.ClientStatsStore;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.compat.CompatManager;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import com.rpgstats.tree.SkillNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * HUD de combate v1.9.
 *
 * Objetivos:
 * - centro da tela completamente livre;
 * - recurso principal no canto inferior esquerdo, próximo da hotbar;
 * - recursos táticos do Mago em micro-indicadores, sem painel alto;
 * - quatro slots ativos no canto inferior direito;
 * - cooldown real sincronizado com o servidor e mostrado visualmente.
 */
public final class ResourceHud {
    private static final String[] KEYS = {"R", "Z", "X", "C"};

    private static final int RESOURCE_W = 122;
    private static final int SKILL_W = 25;
    private static final int SKILL_H = 23;
    private static final int SKILL_GAP = 3;

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null) return;

        PlayerStats stats = ClientStatsStore.stats;
        if (stats.clazz == null || stats.resourceMax <= 0) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        renderResources(context, client, stats, sw, sh);
        renderActiveSlots(context, client, stats, sw, sh);
        renderArcaneHelp(context, client, stats, sw, sh);
    }

    private static void renderResources(DrawContext context, MinecraftClient client, PlayerStats stats, int sw, int sh) {
        // Fica à esquerda da hotbar quando há espaço; em resoluções pequenas recua para a borda.
        int hotbarLeft = sw / 2 - 91;
        int preferredX = hotbarLeft - RESOURCE_W - 8;
        int x = Math.max(6, preferredX);
        int y = preferredX < 6 ? sh - 54 : sh - 31;

        // Quando Iron's está ativo ele é a autoridade de Mana e já desenha a própria barra.
        // stats.resource continua espelhando esse mesmo pool apenas para sync/mecânicas internas;
        // desenhar outra barra aqui faria a mesma Mana aparecer duas vezes.
        boolean ironOwnsMageMana = stats.clazz == RPGClass.MAGO && CompatManager.usesExternalMageMana();
        if (!ironOwnsMageMana) {
            drawResourceBar(context, client, stats.clazz, stats.resource, stats.resourceMax,
                    x, y, RESOURCE_W, true);
        }

        int tacticalY = y - 10;
        drawStaminaMicroBar(context, client, stats, x, tacticalY, RESOURCE_W);
        tacticalY -= 10;
        if (stats.clazz == RPGClass.MAGO) {
            drawMageTacticalStrip(context, client, stats, x, tacticalY, RESOURCE_W);
            tacticalY -= 10;
            if (stats.path == RPGPath.MAGE_OCCULT) tacticalY -= 10;
        }

        if (stats.hasRecovery && stats.recoverableXp > 0) drawRecoveryHint(context, client, stats, x, tacticalY, RESOURCE_W);
    }

    private static void drawStaminaMicroBar(DrawContext context, MinecraftClient client, PlayerStats stats,
                                            int x, int y, int width) {
        int color = stats.stamina < 30f ? RpgUiTheme.WARNING : 0xFF73D673;
        String value = "ST " + (int) stats.stamina + "/" + (int) stats.staminaMax;
        context.drawTextWithShadow(client.textRenderer, Text.literal(value), x + 1, y - 1, color);
        int textW = client.textRenderer.getWidth(value);
        int barX = Math.min(x + width - 35, x + textW + 4);
        float ratio = stats.staminaMax <= 0f ? 0f : stats.stamina / stats.staminaMax;
        drawThinBar(context, barX, y + 7, x + width - barX, 2, ratio, color);
    }

    private static void drawRecoveryHint(DrawContext context, MinecraftClient client, PlayerStats stats,
                                         int x, int y, int width) {
        String suffix = "outra dimensao";
        if (client.world != null && client.player != null
                && client.world.getRegistryKey().getValue().toString().equals(stats.recoveryDimension)) {
            double dx = client.player.getX() - stats.recoveryX;
            double dy = client.player.getY() - stats.recoveryY;
            double dz = client.player.getZ() - stats.recoveryZ;
            suffix = Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)) + "m";
        }
        String label = "ECOS " + stats.recoverableXp + " · " + suffix;
        if (client.textRenderer.getWidth(label) > width) {
            label = client.textRenderer.trimToWidth(label, width - client.textRenderer.getWidth("...")) + "...";
        }
        context.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 1, y - 1, RpgUiTheme.WARNING);
    }

    private static void drawResourceBar(DrawContext context, MinecraftClient client, RPGClass clazz,
                                        float value, float max, int x, int y, int width, boolean showName) {
        int accent = RpgUiTheme.accent(clazz);
        float ratio = max <= 0f ? 0f : Math.max(0f, Math.min(1f, value / max));

        // Sem "card" grande: só uma moldura fina e a barra.
        RpgUiTheme.roundRect(context, x, y, width, 17, 0xB90A0E16);
        context.fill(x + 1, y + 1, x + 3, y + 16, accent);

        String label = showName ? clazz.resourceName().toUpperCase() : clazz.resourceName();
        String amount = ((int) value) + "/" + ((int) max);
        context.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 7, y + 2, accent);
        context.drawTextWithShadow(client.textRenderer, Text.literal(amount),
                x + width - 5 - client.textRenderer.getWidth(amount), y + 2, RpgUiTheme.TEXT);

        drawThinBar(context, x + 7, y + 12, width - 12, 3, ratio, accent);
    }

    private static void drawMageTacticalStrip(DrawContext context, MinecraftClient client, PlayerStats stats,
                                               int x, int y, int width) {
        final int corruption = 0xFFE25578;
        boolean occult = stats.path == RPGPath.MAGE_OCCULT;
        boolean temporal = stats.path == RPGPath.MAGE_TEMPORAL;
        int fragmentMax = temporal ? stats.temporalFragmentMax() : 0;

        int fragmentTotal = fragmentMax > 0 ? fragmentMax * 3 + (fragmentMax - 1) * 2 : 0;
        int fragmentX = fragmentMax > 0 ? x + width - fragmentTotal : x + width;

        if (occult) {
            String label = "CORR " + (int) stats.corruption;
            context.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 1, y,
                    stats.corruption >= 75f ? RpgUiTheme.DANGER : corruption);
            int barX = x + client.textRenderer.getWidth(label) + 5;
            drawThinBar(context, barX, y + 7, Math.max(10, x + width - barX), 2,
                    stats.corruption / 100f, stats.corruption >= 75f ? RpgUiTheme.DANGER : corruption);
            drawArcaneLine(context, client, MageCorruptionText.hudEffects(stats.corruption, stats::hasNode),
                    x + 1, y - 10, width - 1, corruption);
        }

        if (temporal && fragmentMax > 0) {
            for (int i = 0; i < fragmentMax; i++) {
                int color = i < stats.temporalFragments ? 0xFF73C9FF : 0xFF2D3546;
                int dx = fragmentX + i * 5;
                context.fill(dx, y + 5, dx + 3, y + 8, color);
            }
        }
    }


    private static void renderActiveSlots(DrawContext context, MinecraftClient client, PlayerStats stats, int sw, int sh) {
        int totalW = SKILL_W * 4 + SKILL_GAP * 3;
        int x0 = sw - totalW - 7;
        int y = sh - SKILL_H - 7;

        // Se uma resolução muito estreita encostar na hotbar, sobe os slots um pouco.
        int hotbarRight = sw / 2 + 91;
        if (x0 < hotbarRight + 6) y = sh - SKILL_H - 31;

        for (int i = 0; i < 4; i++) {
            int sx = x0 + i * (SKILL_W + SKILL_GAP);
            drawAbilitySlot(context, client, stats, i, sx, y);
        }
    }

    private static void drawAbilitySlot(DrawContext context, MinecraftClient client, PlayerStats stats,
                                        int slot, int x, int y) {
        String nodeId = stats.activeSlot(slot);
        SkillNode node = StatsManager.findNode(stats, nodeId);
        boolean empty = node == null || nodeId == null || nodeId.isBlank();
        int accent = RpgUiTheme.accent(stats.clazz);
        int border = empty ? 0xFF30384A : RpgUiTheme.alpha(accent, 220);
        int bg = empty ? 0xA80A0E16 : 0xC70D1320;

        RpgUiTheme.panel(context, x, y, SKILL_W, SKILL_H, bg, border);

        // Tecla pequena no canto, sem competir com o nome/estado da habilidade.
        context.drawTextWithShadow(client.textRenderer, Text.literal(KEYS[slot]), x + 3, y + 2,
                empty ? RpgUiTheme.DIM : accent);

        if (empty) {
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("·"),
                    x + SKILL_W / 2, y + 12, RpgUiTheme.DIM);
            return;
        }

        var arcane = ArcaneHudText.entry(nodeId);
        String compact = arcane == null ? shortAbilityName(node) : arcane.code();
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(compact),
                x + SKILL_W / 2, y + 12, RpgUiTheme.TEXT);

        int current = ClientStatsStore.cooldown(nodeId);
        int max = ClientStatsStore.cooldownMax(nodeId);
        if (max <= 0) {
            SkillEffect effect = AbilityRegistry.activeForNode(nodeId);
            if (effect != null) max = Math.max(1, effect.cooldownTicks());
        }

        if (current > 0) {
            float ratio = max <= 0 ? 1f : Math.max(0f, Math.min(1f, current / (float) max));
            int usableH = SKILL_H - 2;
            int overlayH = Math.max(1, Math.round(usableH * ratio));
            int top = y + SKILL_H - 1 - overlayH;
            context.fill(x + 1, top, x + SKILL_W - 1, y + SKILL_H - 1, 0xA6000000);

            String seconds = Integer.toString((current + 19) / 20);
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(seconds),
                    x + SKILL_W / 2, y + 8, 0xFFFFFFFF);
        } else {
            // Linha de pronto muito discreta.
            context.fill(x + 3, y + SKILL_H - 3, x + SKILL_W - 3, y + SKILL_H - 2,
                    RpgUiTheme.alpha(accent, 185));
        }
    }

    /** Names and actual actions stay readable without opening the skill tree. */
    private static void renderArcaneHelp(DrawContext context, MinecraftClient client, PlayerStats stats, int sw, int sh) {
        if (stats.path != RPGPath.ARC_ARCANE) return;
        int rows=0;
        for(int i=0;i<4;i++) if(StatsManager.findNode(stats,stats.activeSlot(i))!=null) rows++;
        boolean detailed=com.rpgstats.RPGStatsClient.abilityDetailsHeld();
        int rowHeight=detailed?34:14;
        int headerHeight=detailed?22:30;
        int width=Math.min(detailed?280:190,sw-14),height=headerHeight+rows*rowHeight;
        int slotY=sh-SKILL_H-7;
        int totalW=SKILL_W*4+SKILL_GAP*3;
        if(sw-totalW-7<sw/2+97)slotY=sh-SKILL_H-31;
        int x=sw-width-7,y=Math.max(7,slotY-height-5),accent=RpgUiTheme.accent(stats.clazz);
        RpgUiTheme.panel(context,x,y,width,height,0xB80D1320,RpgUiTheme.alpha(accent,160));
        drawArcaneLine(context,client,"Afinidade: "+ArcaneHudText.affinity(ClientStatsStore.archerAffinity),x+6,y+5,width-12,accent);
        if(!detailed)drawArcaneLine(context,client,com.rpgstats.RPGStatsClient.abilityDetailsKey()+": detalhes",x+6,y+15,width-12,RpgUiTheme.DIM);
        int rowY=y+headerHeight-2;
        for(int slot=0;slot<4;slot++) {
            String id=stats.activeSlot(slot);SkillNode node=StatsManager.findNode(stats,id);
            if(node==null)continue;
            var entry=ArcaneHudText.entry(id);
            String name=entry==null?node.name():entry.name();
            String hint=entry==null?AbilityRegistry.activeSummary(id):entry.hint();
            drawArcaneLine(context,client,KEYS[slot]+" · "+name,x+6,rowY,width-12,RpgUiTheme.TEXT);
            if(detailed)drawArcaneLine(context,client,hint,x+6,rowY+10,width-12,RpgUiTheme.DIM);
            SkillEffect effect=AbilityRegistry.activeForNode(id);
            if(detailed && effect!=null) {
                float reduction=Math.min(.25f,AbilityRegistry.sumPassive(stats.unlockedNodes,"resource_cost_reduction"));
                float cost=effect.resourceCost()*(1f-reduction)*(1f+com.rpgstats.classes.HouseRules.surcharge(stats));
                int cd=ClientStatsStore.cooldown(id);
                String state=cd>0?"recarga "+((cd+19)/20)+"s":"pronta";
                String details=String.format(java.util.Locale.ROOT,"%.1f Foco · CD base %.1fs · %s",cost,effect.cooldownTicks()/20f,state);
                drawArcaneLine(context,client,details,x+6,rowY+20,width-12,cd>0?RpgUiTheme.DIM:accent);
            }
            rowY+=rowHeight;
        }
    }

    private static void drawArcaneLine(DrawContext context,MinecraftClient client,String line,int x,int y,int width,int color) {
        String shown=line;
        if(client.textRenderer.getWidth(shown)>width)
            shown=client.textRenderer.trimToWidth(shown,Math.max(1,width-client.textRenderer.getWidth("…")))+"…";
        context.drawTextWithShadow(client.textRenderer,Text.literal(shown),x,y,color);
    }

    private static String shortAbilityName(SkillNode node) {
        if (node == null || node.name() == null || node.name().isBlank()) return "?";
        String cleaned = node.name().replace("ASCENSÃO · ", "").replace("ATIVA · ", "").trim();
        String[] parts = cleaned.replace('-', ' ').split("\\s+");
        if (parts.length >= 2) {
            String a = parts[0].isBlank() ? "" : parts[0].substring(0, 1);
            String b = parts[1].isBlank() ? "" : parts[1].substring(0, 1);
            if (!a.isBlank() || !b.isBlank()) return (a + b).toUpperCase();
        }
        String compact = cleaned.replace(" ", "");
        return compact.length() <= 2 ? compact.toUpperCase() : compact.substring(0, 2).toUpperCase();
    }

    private static void drawThinBar(DrawContext context, int x, int y, int width, int height,
                                    float ratio, int color) {
        if (width <= 0 || height <= 0) return;
        ratio = Math.max(0f, Math.min(1f, ratio));
        context.fill(x, y, x + width, y + height, 0xB8121720);
        int fill = Math.round(width * ratio);
        if (fill > 0) context.fill(x, y, x + fill, y + height, color);
    }

    private ResourceHud() {}
}
