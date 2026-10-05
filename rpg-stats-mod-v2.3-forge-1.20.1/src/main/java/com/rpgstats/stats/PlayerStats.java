package com.rpgstats.stats;

import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.classes.RPGSubclass;
import com.rpgstats.combat.MageBalance;
import com.rpgstats.tree.SkillNode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class PlayerStats {
    public static final int DATA_VERSION = 11;
    public static final int ACTIVE_SLOTS = 4;

    public int level = 1;
    public int xp = 0;
    public int statPoints = 0;
    public int skillPoints = 0;

    /** Classe principal. */
    public RPGClass clazz = null;
    /** Sistema antigo ainda usado pelas quatro classes ainda não migradas. */
    public RPGSubclass subclass = null;

    /** Novo sistema de profundidade, inaugurado pelo Mago na v1.5. */
    public RPGPath path = null;
    public RPGSpecialization specialization = null;

    /** Classe e disciplina secundárias: poder complementar, nunca uma segunda build completa. */
    public RPGPath affinityHouse = null;
    public boolean awakened = false;

    /** Última habilidade comprada: a interface mantém os números visíveis após a compra. */
    public String lastUnlockedNode = "";

    public final EnumMap<Stat, Integer> stats = new EnumMap<>(Stat.class);
    public final Set<String> unlockedNodes = new HashSet<>();
    /** Proteções de progressão: repetição de mobs e recompensa única de boss. */
    public final Map<String, Integer> killCounts = new HashMap<>();
    public final Set<String> defeatedBossTypes = new HashSet<>();

    public float resource = 0f;
    public float resourceMax = 0f;

    /** Recurso universal de combate: corrida, ataques fisicos e esquiva. */
    public float stamina = 100f;
    public float staminaMax = 100f;

    /** Ecos perdidos na ultima morte e recuperaveis uma unica vez. */
    public int recoverableXp = 0;
    public boolean hasRecovery = false;
    public String recoveryDimension = "";
    public double recoveryX = 0;
    public double recoveryY = 0;
    public double recoveryZ = 0;

    /** Recursos táticos exclusivos do Mago. Concentração foi removida; campo legado fica só para migração. */
    @Deprecated public float concentration = 0f;
    public float corruption = 0f;
    public int temporalFragments = 0;

    /** Quatro slots de ativa: R / Z / X / C por padrão. */
    public final String[] activeSlots = new String[ACTIVE_SLOTS];

    public PlayerStats() {
        for (Stat stat : Stat.values()) stats.put(stat, 0);
        for (int i = 0; i < activeSlots.length; i++) activeSlots[i] = "";
    }

    public static int xpToNext(int level) {
        return (int) (100 * Math.pow(level, 1.55));
    }

    public String activeSlot(int slot) {
        if (slot < 0 || slot >= ACTIVE_SLOTS) return "";
        return activeSlots[slot] == null ? "" : activeSlots[slot];
    }

    public void setActiveSlot(int slot, String nodeId) {
        if (slot < 0 || slot >= ACTIVE_SLOTS) return;
        activeSlots[slot] = nodeId == null ? "" : nodeId;
    }

    public Map<Stat, Integer> totalStats() {
        EnumMap<Stat, Integer> total = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) total.put(stat, stats.get(stat));

        if (clazz != null) {
            for (String id : unlockedNodes) {
                if (id.startsWith("sec_")) continue;
                SkillNodeHolder.addBonus(total, this, id);
            }
        }
        return total;
    }

    /** Recalcula pools sem conceder recurso gratuito. */
    public void refreshResourceMax() {
        Map<Stat, Integer> totals = totalStats();
        int intel = totals.getOrDefault(Stat.INTELIGENCIA, 0);
        int tenacity = totals.getOrDefault(Stat.TENACIDADE, 0);
        staminaMax = staminaCapacity(tenacity);
        stamina = Math.max(0f, Math.min(stamina, staminaMax));

        if (clazz == null) {
            resourceMax = 0f;
            resource = 0f;
        } else if (clazz == RPGClass.MAGO) {
            float flat = AbilityRegistry.sumPassive(unlockedNodes, "mana_flat", false);
            float pct = Math.min(0.35f, Math.max(0f, AbilityRegistry.sumPassive(unlockedNodes, "mana_max_pct", false)));
            resourceMax = (MageBalance.BASE_MANA + intel * MageBalance.MANA_PER_INTELLIGENCE + flat) * (1f + pct);
            resourceMax = Math.min(MageBalance.MAX_MANA, resourceMax);
        } else {
            resourceMax = baseResource(clazz) + resourceStatBonus(clazz) + AbilityRegistry.sumPassive(unlockedNodes,"resource_flat");
        }

        if (clazz != null && awakened) resourceMax += 3f;
        if (clazz == RPGClass.MAGO) resourceMax = Math.min(MageBalance.MAX_MANA, resourceMax);
        resource = Math.max(0f, Math.min(resource, resourceMax));
        concentration = 0f;
        corruption = Math.max(0f, Math.min(MageBalance.MAX_CORRUPTION, corruption));
        int fragmentMax = temporalFragmentMax();
        temporalFragments = Math.max(0, Math.min(fragmentMax, temporalFragments));
    }

    public int temporalFragmentMax() {
        if (!unlockedNodes.contains("mag_time_sensitivity")) return 0;
        return unlockedNodes.contains("mag_time_expanded_memory") ? 7 : 5;
    }

    public static float baseResource(RPGClass clazz) {
        return switch (clazz) {
            case GUERREIRO -> 40f;
            case MAGO -> MageBalance.BASE_MANA;
            case ARQUEIRO -> 35f;
            case ASSASSINO -> 45f;
        };
    }

    private float resourceStatBonus(RPGClass targetClass) {
        return switch (targetClass) {
            case GUERREIRO -> stats.get(Stat.FORCA) * 0.30f + stats.get(Stat.TENACIDADE) * 0.30f;
            case MAGO -> stats.get(Stat.INTELIGENCIA) * MageBalance.MANA_PER_INTELLIGENCE;
            case ARQUEIRO -> stats.get(Stat.DESTREZA) * 0.40f + stats.get(Stat.TENACIDADE) * 0.15f;
            case ASSASSINO -> stats.get(Stat.DESTREZA) * 0.30f + stats.get(Stat.ARCANO) * 0.25f;
        };
    }

    public static float staminaCapacity(int tenacity) {
        int value = Math.max(0, Math.min(StatsManager.MAX_STAT, tenacity));
        int early = Math.min(value, 20);
        int middle = Math.min(Math.max(0, value - 20), 15);
        int late = Math.min(Math.max(0, value - 35), 15);
        return 100f + early * 1.5f + middle + late * 0.5f;
    }

    public static float staminaRegenPerSecond(int tenacity) {
        return Math.min(18f, 12f + Math.max(0, tenacity) * 0.12f);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("dataVersion", DATA_VERSION);
        nbt.putInt("level", level);
        nbt.putInt("xp", xp);
        nbt.putInt("points", statPoints);
        nbt.putInt("skillPoints", skillPoints);
        if (clazz != null) nbt.putString("class", clazz.name());
        if (subclass != null) nbt.putString("subclass", subclass.name());
        if (path != null) nbt.putString("path", path.name());
        if (specialization != null) nbt.putString("specialization", specialization.name());
        if (affinityHouse != null) nbt.putString("affinityHouse", affinityHouse.name());
        nbt.putBoolean("awakened", awakened);
        nbt.putString("lastUnlockedNode", lastUnlockedNode == null ? "" : lastUnlockedNode);

        NbtCompound storedStats = new NbtCompound();
        for (Stat stat : Stat.activeValues()) storedStats.putInt(stat.name(), stats.getOrDefault(stat,0));
        nbt.put("stats", storedStats);

        NbtList nodes = new NbtList();
        for (String id : unlockedNodes) nodes.add(NbtString.of(id));
        nbt.put("nodes", nodes);

        nbt.putFloat("resource", resource);
        nbt.putFloat("resourceMax", resourceMax);
        nbt.putFloat("stamina", stamina);
        nbt.putFloat("staminaMax", staminaMax);
        nbt.putInt("recoverableXp", recoverableXp);
        nbt.putBoolean("hasRecovery", hasRecovery);
        nbt.putString("recoveryDimension", recoveryDimension == null ? "" : recoveryDimension);
        nbt.putDouble("recoveryX", recoveryX);
        nbt.putDouble("recoveryY", recoveryY);
        nbt.putDouble("recoveryZ", recoveryZ);
        nbt.putFloat("corruption", corruption);
        nbt.putInt("temporalFragments", temporalFragments);

        NbtList actives = new NbtList();
        for (String node : activeSlots) actives.add(NbtString.of(node == null ? "" : node));
        nbt.put("activeSlots", actives);
        NbtCompound kills = new NbtCompound();
        killCounts.forEach(kills::putInt);
        nbt.put("killCounts", kills);
        NbtList defeated = new NbtList();
        for (String id : defeatedBossTypes) defeated.add(NbtString.of(id));
        nbt.put("defeatedBossTypes", defeated);
        return nbt;
    }

    public static PlayerStats fromNbt(NbtCompound nbt) {
        PlayerStats result = new PlayerStats();
        int oldVersion = nbt.contains("dataVersion") ? nbt.getInt("dataVersion") : 0;
        result.level = Math.max(1, nbt.getInt("level"));
        result.xp = Math.max(0, nbt.getInt("xp"));
        result.statPoints = Math.max(0, nbt.getInt("points"));
        result.skillPoints = Math.max(0, nbt.getInt("skillPoints"));
        if (nbt.contains("class")) {
            try { result.clazz = RPGClass.valueOf(nbt.getString("class")); } catch (IllegalArgumentException ignored) { }
        }
        if (nbt.contains("subclass")) {
            try {
                result.subclass = RPGSubclass.valueOf(nbt.getString("subclass"));
                if (result.clazz == null || result.subclass.parent != result.clazz) result.subclass = null;
            } catch (IllegalArgumentException ignored) { }
        }
        if (nbt.contains("path")) {
            try {
                result.path = RPGPath.valueOf(nbt.getString("path"));
                if (result.clazz == null || result.path.parent != result.clazz) result.path = null;
            } catch (IllegalArgumentException ignored) { }
        }
        if (nbt.contains("specialization")) {
            try {
                result.specialization = RPGSpecialization.valueOf(nbt.getString("specialization"));
                if (result.path == null || result.specialization.parent != result.path) result.specialization = null;
            } catch (IllegalArgumentException ignored) { }
        }
        if(nbt.contains("affinityHouse")) {
            try { result.affinityHouse=RPGPath.valueOf(nbt.getString("affinityHouse")); }
            catch(IllegalArgumentException ignored) {}
            if(result.affinityHouse!=null && (result.path==null || result.affinityHouse==result.path
                    || result.affinityHouse.parent!=result.clazz)) result.affinityHouse=null;
        }
        result.awakened=nbt.getBoolean("awakened") || result.clazz!=null;
        result.lastUnlockedNode = nbt.getString("lastUnlockedNode");

        NbtCompound storedStats = nbt.getCompound("stats");
        for (Stat stat : Stat.values()) {
            int value = storedStats.getInt(stat.name());
            if (oldVersion < 7 && stat == Stat.TENACIDADE && !storedStats.contains(stat.name())) {
                value = storedStats.getInt("AGILIDADE");
            }
            result.stats.put(stat, Math.max(0, value));
        }
        NbtList nodes = nbt.getList("nodes", NbtElement.STRING_TYPE);
        if (oldVersion < 11) {
            // Return base Faith (including origin points), never tree/equipment bonuses.
            result.statPoints = (int)Math.min(Integer.MAX_VALUE,
                    (long)result.statPoints + result.stats.getOrDefault(Stat.FE,0));
        }
        result.stats.put(Stat.FE,0);
        for (int i = 0; i < nodes.size(); i++) result.unlockedNodes.add(nodes.getString(i));

        result.resource = Math.max(0f, nbt.getFloat("resource"));
        result.resourceMax = Math.max(0f, nbt.getFloat("resourceMax"));
        boolean hadStamina = nbt.contains("stamina");
        result.stamina = hadStamina ? Math.max(0f, nbt.getFloat("stamina")) : 100f;
        result.staminaMax = Math.max(100f, nbt.getFloat("staminaMax"));
        result.recoverableXp = Math.max(0, nbt.getInt("recoverableXp"));
        result.hasRecovery = nbt.getBoolean("hasRecovery") && result.recoverableXp > 0;
        result.recoveryDimension = nbt.getString("recoveryDimension");
        result.recoveryX = nbt.getDouble("recoveryX");
        result.recoveryY = nbt.getDouble("recoveryY");
        result.recoveryZ = nbt.getDouble("recoveryZ");
        result.concentration = 0f; // recurso removido; saves antigos migram sem custo
        result.corruption = Math.max(0f, nbt.getFloat("corruption"));
        result.temporalFragments = Math.max(0, nbt.getInt("temporalFragments"));

        if (nbt.contains("activeSlots")) {
            NbtList actives = nbt.getList("activeSlots", NbtElement.STRING_TYPE);
            for (int i = 0; i < Math.min(ACTIVE_SLOTS, actives.size()); i++) result.activeSlots[i] = actives.getString(i);
        } else if (nbt.contains("selectedActiveNode")) {
            result.activeSlots[0] = nbt.getString("selectedActiveNode");
        }

        NbtCompound kills = nbt.getCompound("killCounts");
        for (String key : kills.getKeys()) result.killCounts.put(key, Math.max(0, kills.getInt(key)));
        NbtList defeated = nbt.getList("defeatedBossTypes", NbtElement.STRING_TYPE);
        for (int i = 0; i < defeated.size(); i++) result.defeatedBossTypes.add(defeated.getString(i));

        migrate(result, oldVersion);
        migrateMulticlass(result, nbt, oldVersion);
        for (int i = 0; i < ACTIVE_SLOTS; i++) {
            if (!AbilityRegistry.hasActive(result.activeSlots[i])) result.activeSlots[i] = "";
        }
        result.refreshResourceMax();
        if (!hadStamina) result.stamina = result.staminaMax;
        return result;
    }

    public boolean hasNode(String id) { return unlockedNodes.contains(id) || unlockedNodes.contains(HouseRules.PREFIX+id); }

    private static void migrateMulticlass(PlayerStats s, NbtCompound nbt, int oldVersion) {
        if(oldVersion>=10)return;
        RPGClass oldClass=null; RPGPath oldPath=null;
        try { oldClass=RPGClass.valueOf(nbt.getString("secondaryClass")); }catch(IllegalArgumentException ignored){}
        try { oldPath=RPGPath.valueOf(nbt.getString("secondaryPath")); }catch(IllegalArgumentException ignored){}
        java.util.Iterator<String> it=s.unlockedNodes.iterator();
        while(it.hasNext()) {
            String key=it.next(); if(!key.startsWith("sec_"))continue;
            String real=key.substring(4);
            SkillNode n=oldClass==null?null:oldClass.findNode(real);
            if(n==null && oldPath!=null)n=oldPath.findNode(real);
            if(n!=null)s.skillPoints+=n.cost()+1;
            it.remove();
        }
        for(int i=0;i<ACTIVE_SLOTS;i++)if(s.activeSlot(i).startsWith("sec_"))s.activeSlots[i]="";
        if(s.lastUnlockedNode.startsWith("sec_"))s.lastUnlockedNode="";
    }

    private static void migrate(PlayerStats result, int oldVersion) {
        if (oldVersion < 6) {
            // v1.5: PH passa a ser 1 por nível. Reembolsa a diferença apenas uma vez.
            int oldNaturalPoints = result.level / 2;
            int newNaturalPoints = Math.max(1, result.level);
            result.skillPoints += Math.max(0, newNaturalPoints - oldNaturalPoints);

            // A antiga árvore curta do Mago foi substituída. Reembolsa PH gasto nela.
            if (result.clazz == RPGClass.MAGO) {
                java.util.Map<String, Integer> legacyCost = java.util.Map.ofEntries(
                        java.util.Map.entry("foco_arcano", 1), java.util.Map.entry("sabedoria", 1),
                        java.util.Map.entry("barreira", 1), java.util.Map.entry("mente", 2), java.util.Map.entry("arquimago", 2),
                        java.util.Map.entry("arc_reserva", 1), java.util.Map.entry("arc_fluxo", 1), java.util.Map.entry("arc_escudo", 1),
                        java.util.Map.entry("arc_sobrecarga", 2), java.util.Map.entry("arc_ascensao", 3),
                        java.util.Map.entry("nec_sussurro", 1), java.util.Map.entry("nec_ossos", 1), java.util.Map.entry("nec_dreno", 1),
                        java.util.Map.entry("nec_lich", 2), java.util.Map.entry("nec_senhor", 3),
                        java.util.Map.entry("mb_impacto", 1), java.util.Map.entry("mb_armadura", 1), java.util.Map.entry("mb_cadencia", 1),
                        java.util.Map.entry("mb_encantada", 2), java.util.Map.entry("mb_cometa", 3));
                int refund = 0;
                java.util.Iterator<String> it = result.unlockedNodes.iterator();
                while (it.hasNext()) {
                    String id = it.next();
                    Integer cost = legacyCost.get(id);
                    if (cost != null) {
                        refund += cost;
                        it.remove();
                    }
                }
                result.skillPoints += refund;
                result.subclass = null;
                for (int i = 0; i < ACTIVE_SLOTS; i++) {
                    if (legacyCost.containsKey(result.activeSlots[i])) result.activeSlots[i] = "";
                }
            }
        }
        if (oldVersion < 8 && result.path != null) {
            int refund = 0;
            java.util.Iterator<String> it = result.unlockedNodes.iterator();
            while (it.hasNext()) {
                String id = it.next();
                if (id.startsWith("sec_")) continue;
                RPGPath owner = RPGPath.ownerOfNode(id);
                if (owner != null && owner != result.path) {
                    SkillNode node = owner.findNode(id);
                    refund += node == null ? 0 : node.cost();
                    it.remove();
                    for (int i = 0; i < ACTIVE_SLOTS; i++) if (id.equals(result.activeSlots[i])) result.activeSlots[i] = "";
                }
            }
            result.skillPoints += refund;
        }
    }
}
