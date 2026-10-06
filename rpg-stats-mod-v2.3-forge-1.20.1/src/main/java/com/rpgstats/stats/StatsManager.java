package com.rpgstats.stats;

import com.rpgstats.IEntityDataSaver;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.classes.RPGSubclass;
import com.rpgstats.tree.SkillNode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;

public class StatsManager {
    public static final int MAX_STAT = 50;
    public static final int MAX_LEVEL = 50;
    public static final int SUBCLASS_LEVEL = 10; // legado para classes ainda não migradas
    public static final int PATH_LEVEL = 10;
    public static final int SPECIALIZATION_LEVEL = 25;
    public static final int STAT_POINTS_PER_LEVEL = 2;
    public static final int SKILL_POINTS_PER_LEVEL = 1;

    public static PlayerStats get(PlayerEntity player) {
        return PlayerStats.fromNbt(((IEntityDataSaver) player).rpgstats$getPersistentData().getCompound("rpgstats"));
    }

    public static void save(PlayerEntity player, PlayerStats stats) {
        ((IEntityDataSaver) player).rpgstats$getPersistentData().put("rpgstats", stats.toNbt());
    }

    public static void saveAndSync(ServerPlayerEntity player, PlayerStats stats) {
        stats.refreshResourceMax();
        save(player, stats);
        RPGStatsMod.syncStats(player);
    }

    public static void finish(ServerPlayerEntity player, PlayerStats stats) {
        stats.refreshResourceMax();
        save(player, stats);
        StatsApplier.apply(player);
        RPGStatsMod.syncStats(player);
    }

    public static void allocate(ServerPlayerEntity player, String statName) {
        PlayerStats stats = get(player);
        Stat stat = Stat.byName(statName);
        if (stat == null || stats.statPoints <= 0 || stats.stats.get(stat) >= MAX_STAT) return;
        stats.stats.merge(stat, 1, Integer::sum);
        stats.statPoints--;
        finish(player, stats);
    }

    public static void awaken(ServerPlayerEntity player) {
        PlayerStats s=get(player); if(s.awakened)return;
        s.awakened=true; saveAndSync(player,s);
    }

    public static void selectClass(ServerPlayerEntity player, String className) {
        PlayerStats stats = get(player);
        if (stats.clazz != null || !stats.awakened) return;
        try { stats.clazz = RPGClass.valueOf(className); }
        catch (IllegalArgumentException ignored) { return; }

        applyStartingAttributes(stats);
        // Cada personagem começa com 1 PH no nível 1 para desbloquear o primeiro node da classe.
        if (stats.level == 1 && stats.skillPoints == 0 && stats.unlockedNodes.isEmpty()) stats.skillPoints = 1;
        stats.refreshResourceMax();
        stats.resource = stats.resourceMax;
        stats.stamina = stats.staminaMax;
        stats.concentration = 0f;
        stats.corruption = 0f;
        stats.temporalFragments = 0;
        player.sendMessage(Text.literal("§6Classe escolhida: §e§l" + stats.clazz.display), false);
        finish(player, stats);
    }

    /** Classes sao origens balanceadas, nao prisoes: todas recebem 33 pontos iniciais em distribuicoes diferentes. */
    private static void applyStartingAttributes(PlayerStats stats) {
        int existing = stats.stats.values().stream().mapToInt(Integer::intValue).sum();
        if (existing > 0 || stats.clazz == null) return;
        switch (stats.clazz) {
            case GUERREIRO -> setStarting(stats, 8, 8, 10, 3, 1, 2, 1);
            case MAGO -> setStarting(stats, 5, 5, 2, 4, 10, 3, 4);
            case ARQUEIRO -> setStarting(stats, 6, 7, 3, 10, 2, 2, 3);
            case ASSASSINO -> setStarting(stats, 5, 7, 3, 9, 2, 1, 6);
        }
    }

    private static void setStarting(PlayerStats stats, int vitality, int tenacity, int strength,
                                    int dexterity, int intelligence, int faith, int arcane) {
        stats.stats.put(Stat.VITALIDADE, vitality);
        stats.stats.put(Stat.TENACIDADE, tenacity);
        stats.stats.put(Stat.FORCA, strength);
        stats.stats.put(Stat.DESTREZA, dexterity);
        stats.stats.put(Stat.INTELIGENCIA, intelligence);
        stats.stats.put(Stat.FE, 0);
        stats.statPoints += faith; // Original 33-point budget becomes six stats + free PA.
        stats.stats.put(Stat.ARCANO, arcane);
    }

    /** Mantido apenas para compatibilidade com saves anteriores à v1.7. */
    public static void selectSubclass(ServerPlayerEntity player, String subclassName) {
        PlayerStats stats = get(player);
        if (stats.clazz == null || stats.clazz == RPGClass.MAGO || stats.subclass != null || stats.level < SUBCLASS_LEVEL) return;
        RPGSubclass selected;
        try { selected = RPGSubclass.valueOf(subclassName); }
        catch (IllegalArgumentException ignored) { return; }
        if (selected.parent != stats.clazz) return;
        stats.subclass = selected;
        player.sendMessage(Text.literal("§6Especialização escolhida: §e§l" + selected.display), false);
        finish(player, stats);
    }

    /** Escolha de um dos cinco Caminhos internos da classe no nível 10. */
    public static void selectPath(ServerPlayerEntity player, String pathName) {
        PlayerStats stats = get(player);
        if (stats.clazz == null || stats.path != null || stats.level < PATH_LEVEL) return;
        String coreMastery = stats.clazz.nodes.get(stats.clazz.nodes.size() - 1).id();
        if (!stats.unlockedNodes.contains(coreMastery)) {
            player.sendMessage(Text.literal("§cDesbloqueie a Maestria da classe antes de escolher um Caminho."), false);
            return;
        }
        RPGPath selected;
        try { selected = RPGPath.valueOf(pathName); }
        catch (IllegalArgumentException ignored) { return; }
        if (selected.parent != stats.clazz) return;
        stats.path = selected;
        stats.subclass = null;
        player.sendMessage(Text.literal("§d§lCaminho escolhido: §f" + selected.display), false);
        finish(player, stats);
    }

    /** Especialização profunda dentro da Casa escolhida. */
    public static void selectSpecialization(ServerPlayerEntity player, String specializationName) {
        PlayerStats stats = get(player);
        if (stats.clazz == null || stats.path == null || stats.specialization != null
                || stats.level < SPECIALIZATION_LEVEL) return;
        RPGSpecialization selected;
        try { selected = RPGSpecialization.valueOf(specializationName); }
        catch (IllegalArgumentException ignored) { return; }
        if (selected.parent != stats.path) return;

        String mastery = pathMasteryNode(stats.path);
        if (!stats.unlockedNodes.contains(mastery)) {
            player.sendMessage(Text.literal("§cComplete a Maestria da sua Casa antes de se especializar."), false);
            return;
        }
        stats.specialization = selected;
        player.sendMessage(Text.literal("§5§lEspecialização: §f" + selected.display), false);
        finish(player, stats);
    }

    public static void selectAffinity(ServerPlayerEntity player,String name) {
        PlayerStats s=get(player);RPGPath selected;
        try{selected=RPGPath.valueOf(name);}catch(IllegalArgumentException ignored){return;}
        String error=HouseRules.chooseError(s,selected);
        if(!error.isEmpty()){player.sendMessage(Text.literal(error),false);return;}
        s.affinityHouse=selected;finish(player,s);
    }

    public static void unlockNode(ServerPlayerEntity player, String nodeId) {
        PlayerStats stats = get(player);
        if (stats.clazz == null || nodeId == null || nodeId.isBlank()) return;

        if(nodeId.startsWith("sec_"))return;
        boolean secondary=HouseRules.borrowed(nodeId);
        SkillNode node;
        if(secondary) {
            String error=HouseRules.purchaseError(stats,nodeId);
            if(!error.isEmpty()){player.sendMessage(Text.literal(error),false);return;}
            node=HouseRules.node(nodeId);
        } else node=resolveMainNode(stats,nodeId,player);

        if (node == null || stats.unlockedNodes.contains(nodeId)) return;
        if (stats.level < node.reqLevel()) {
            player.sendMessage(Text.literal("§cRequer nível " + node.reqLevel() + "."), false);
            return;
        }
        if (node.prereqNode() != null) {
            String prereqKey = node.prereqNode();
            if (!stats.unlockedNodes.contains(prereqKey)) {
                player.sendMessage(Text.literal("§cDesbloqueie o pré-requisito primeiro."), false);
                return;
            }
        }
        if (node.reqStat() != null && stats.stats.get(node.reqStat()) < node.reqValue()) {
            player.sendMessage(Text.literal("§cRequer " + node.reqValue() + " de " + node.reqStat().display + "."), false);
            return;
        }

        int cost = node.cost();
        if (stats.skillPoints < cost) {
            player.sendMessage(Text.literal("§cPontos de habilidade insuficientes."), false);
            return;
        }

        stats.skillPoints -= cost;
        stats.unlockedNodes.add(nodeId);
        stats.lastUnlockedNode = nodeId;
        if (AbilityRegistry.hasActive(nodeId)) equipFirstFree(stats, nodeId);
        player.sendMessage(Text.literal("§a§lHabilidade desbloqueada: §2" + node.name()), false);
        finish(player, stats);
    }

    /** Valida o dono do node: a classe principal só pode investir no seu Caminho escolhido. */
    private static SkillNode resolveMainNode(PlayerStats stats, String nodeId, ServerPlayerEntity player) {
        SkillNode node = stats.clazz.findNode(nodeId);
        if (node != null) return node;

        RPGPath pathOwner = RPGPath.ownerOfNode(nodeId);
        if (pathOwner != null && pathOwner.parent == stats.clazz) {
                node = pathOwner.findNode(nodeId);
                if (stats.path == null) {
                    player.sendMessage(Text.literal("§cEscolha seu Caminho primeiro."), false);
                    return null;
                }
                if (pathOwner == stats.path) return node;
                player.sendMessage(Text.literal("§cSua classe principal só pode aprender habilidades da sua Casa escolhida."), false);
                return null;
        }

        RPGSpecialization specOwner = RPGSpecialization.ownerOfNode(nodeId);
        if (specOwner != null && specOwner.parent.parent == stats.clazz) {
                if (stats.specialization != specOwner) {
                    player.sendMessage(Text.literal("§cEste node pertence à especialização " + specOwner.display + "."), false);
                    return null;
                }
                return specOwner.findNode(nodeId);
        }
        // Fallback exclusivo para saves legados ainda não migrados.
        if (stats.path == null && stats.subclass != null) return stats.subclass.findNode(nodeId);
        return null;
    }

    private static String pathMasteryNode(RPGPath path) {
        return path.nodes.get(path.nodes.size() - 1).id();
    }

    private static int depthOf(java.util.List<SkillNode> nodes, String id) {
        SkillNode node = null;
        for (SkillNode candidate : nodes) if (candidate.id().equals(id)) { node = candidate; break; }
        if (node == null) return Integer.MAX_VALUE;
        int depth = 1;
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        while (node.prereqNode() != null && seen.add(node.prereqNode())) {
            depth++;
            SkillNode parent = null;
            for (SkillNode candidate : nodes) if (candidate.id().equals(node.prereqNode())) { parent = candidate; break; }
            if (parent == null) break;
            node = parent;
        }
        return depth;
    }

    /** Equipa uma ativa em um dos quatro slots. */
    public static void selectActiveAbility(ServerPlayerEntity player, int slot, String nodeId) {
        PlayerStats stats = get(player);
        if (slot < 0 || slot >= PlayerStats.ACTIVE_SLOTS || nodeId == null
                || !stats.unlockedNodes.contains(nodeId) || !AbilityRegistry.hasActive(nodeId)) return;
        // Um mesmo node não ocupa dois slots.
        for (int i = 0; i < PlayerStats.ACTIVE_SLOTS; i++) {
            if (i != slot && nodeId.equals(stats.activeSlot(i))) stats.setActiveSlot(i, "");
        }
        stats.setActiveSlot(slot, nodeId);
        saveAndSync(player, stats);
        SkillNode node = findNode(stats, nodeId);
        player.sendMessage(Text.literal("§bSlot " + (slot + 1) + ": §f" + (node == null ? nodeId : node.name())), true);
    }

    public static void clearActiveAbility(ServerPlayerEntity player, int slot) {
        PlayerStats stats = get(player);
        if (slot < 0 || slot >= PlayerStats.ACTIVE_SLOTS) return;
        stats.setActiveSlot(slot, "");
        saveAndSync(player, stats);
    }

    private static void equipFirstFree(PlayerStats stats, String nodeId) {
        for (int i = 0; i < PlayerStats.ACTIVE_SLOTS; i++) {
            if (stats.activeSlot(i).isBlank()) {
                stats.setActiveSlot(i, nodeId);
                return;
            }
        }
    }

    public static SkillNode findNode(PlayerStats stats, String nodeId) {
        if (stats == null || nodeId == null || nodeId.isBlank()) return null;
        if(nodeId.startsWith("sec_"))return null;
        if(HouseRules.borrowed(nodeId))return HouseRules.owner(nodeId)==stats.affinityHouse?HouseRules.node(nodeId):null;
        String real=nodeId;
        if (stats.clazz == null) return null;

        SkillNode node = stats.clazz.findNode(real);
        if (node == null && stats.subclass != null) node = stats.subclass.findNode(real);
        if (node == null) {
            RPGPath owner = RPGPath.ownerOfNode(real);
            if (owner != null && owner.parent == stats.clazz) node = owner.findNode(real);
        }
        if (node == null) {
            RPGSpecialization owner = RPGSpecialization.ownerOfNode(real);
            if (owner != null && owner.parent.parent == stats.clazz) node = owner.findNode(real);
        }
        return node;
    }

    public static int computeKillXp(LivingEntity victim) {
        if (victim instanceof PlayerEntity) return 0;
        if (victim instanceof TameableEntity tameable && tameable.getOwner() != null) return 0;
        if (victim instanceof Ownable ownable && ownable.getOwner() != null) return 0;

        int bossTier = BossScaler.getTier(victim);
        if (bossTier > 0) {
            return BossScaler.getXpReward(victim);
        }

        float maxHp = victim.getMaxHealth();
        if (victim instanceof HostileEntity) return Math.max(2, Math.min(12, Math.round(maxHp * 0.18f)));
        if (victim instanceof PassiveEntity) return Math.max(1, Math.min(3, Math.round(maxHp * 0.05f)));
        return Math.max(1, Math.min(6, Math.round(maxHp * 0.10f)));
    }

    public static void addXp(ServerPlayerEntity player, int rawAmount) {
        if (rawAmount <= 0) return;
        PlayerStats stats = get(player);
        if (stats.level >= MAX_LEVEL) {
            stats.xp = 0;
            saveAndSync(player, stats);
            return;
        }

        stats.xp = (int) Math.min(Integer.MAX_VALUE, (long) stats.xp + rawAmount);
        int oldLevel = stats.level;
        int statGained = 0;
        int skillGained = 0;

        while (stats.level < MAX_LEVEL && stats.xp >= PlayerStats.xpToNext(stats.level)) {
            stats.xp -= PlayerStats.xpToNext(stats.level);
            stats.level++;
            stats.statPoints += STAT_POINTS_PER_LEVEL;
            stats.skillPoints += SKILL_POINTS_PER_LEVEL;
            statGained += STAT_POINTS_PER_LEVEL;
            skillGained += SKILL_POINTS_PER_LEVEL;
        }
        if (stats.level >= MAX_LEVEL) stats.xp = 0;

        boolean leveled = stats.level > oldLevel;
        if (leveled) {
            player.sendMessage(Text.literal("§6§lNível " + stats.level + "! §e+" + statGained
                    + " PA §d+" + skillGained + " PH"), false);
            if (stats.path == null) {
                if (oldLevel < PATH_LEVEL && stats.level >= PATH_LEVEL)
                    player.sendMessage(Text.literal("§d§lCaminho disponível! Complete a Maestria da classe e faça sua escolha."), false);
                if (oldLevel < SPECIALIZATION_LEVEL && stats.level >= SPECIALIZATION_LEVEL && stats.specialization == null)
                    player.sendMessage(Text.literal("§5§lEspecialização profunda disponível no nível 25."), false);
            }
            if(oldLevel<HouseRules.LEVEL && stats.level>=HouseRules.LEVEL)
                player.sendMessage(Text.literal("Casa secundária disponível: estude outra Casa da sua classe."),false);
            if (stats.level >= MAX_LEVEL)
                player.sendMessage(Text.literal("§6§lNível máximo (" + MAX_LEVEL + ") alcançado! Ascensões finais podem ser liberadas."), false);
        }
        finish(player, stats);
    }

    public static void addQuestXp(ServerPlayerEntity player, int amount) { addXp(player, amount); }

    public static void addBossCompletionXp(ServerPlayerEntity player,String bossType,int amount) {
        addBossCompletionXp(player,java.util.List.of(bossType),amount);
    }
    public static void addBossCompletionXp(ServerPlayerEntity player,java.util.List<String> bossTypes,int amount) {
        if(amount<=0)return;
        var stats=get(player);boolean first=false;
        for(String type:bossTypes)first|=stats.defeatedBossTypes.add(type);
        if(first)amount=Math.round(amount*1.5f);
        save(player,stats);addXp(player,amount);
    }

    public static void addXpFromKill(ServerPlayerEntity killer, LivingEntity victim) {
        if (victim == killer) return;
        int xp = computeKillXp(victim);
        if (xp <= 0) return;

        PlayerStats stats = get(killer);
        String type = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
        int bossTier = BossScaler.getTier(victim);
        if (bossTier > 0) {
            if (stats.defeatedBossTypes.add(type)) xp = Math.round(xp * 1.5f);
        } else {
            int previous = stats.killCounts.getOrDefault(type, 0);
            stats.killCounts.put(type, previous + 1);
            if (previous >= 40) xp = Math.max(1, xp / 4);
            else if (previous >= 15) xp = Math.max(1, xp / 2);
        }
        save(killer, stats);
        addXp(killer, xp);
    }

    /** Guarda apenas os Ecos ainda nao convertidos em nivel. Uma segunda morte substitui a perda anterior. */
    public static void onPlayerDeath(ServerPlayerEntity player) {
        PlayerStats stats = get(player);
        boolean erasedPrevious = stats.hasRecovery && stats.recoverableXp > 0;
        int lost = Math.max(0, stats.xp);
        stats.xp = 0;
        stats.recoverableXp = lost;
        stats.hasRecovery = lost > 0;
        stats.recoveryDimension = player.getWorld().getRegistryKey().getValue().toString();
        stats.recoveryX = player.getX();
        stats.recoveryY = player.getY();
        stats.recoveryZ = player.getZ();
        saveAndSync(player, stats);

        if (erasedPrevious) player.sendMessage(Text.literal("§4Seus Ecos perdidos anteriores se dissiparam."), false);
        if (lost > 0) {
            player.sendMessage(Text.literal("§6Voce perdeu §e" + lost
                    + " Ecos§6. Retorne ao local da morte para recupera-los."), false);
        }
    }

    /** Recuperacao automatica em raio curto; funciona em qualquer dimensao sem criar item ou equipamento. */
    public static void tickRecovery(ServerPlayerEntity player) {
        if (player.age % 10 != 0 || !player.isAlive() || player.isSpectator()) return;
        PlayerStats stats = get(player);
        if (!stats.hasRecovery || stats.recoverableXp <= 0) return;
        String currentDimension = player.getWorld().getRegistryKey().getValue().toString();
        if (!currentDimension.equals(stats.recoveryDimension)) return;

        double dx = player.getX() - stats.recoveryX;
        double dy = player.getY() - stats.recoveryY;
        double dz = player.getZ() - stats.recoveryZ;
        if (dx * dx + dy * dy + dz * dz > 6.25) return;

        int recovered = stats.recoverableXp;
        stats.recoverableXp = 0;
        stats.hasRecovery = false;
        stats.recoveryDimension = "";
        save(player, stats);
        addXp(player, recovered);
        player.sendMessage(Text.literal("§6§lEcos recuperados: §e" + recovered), false);
    }
}
