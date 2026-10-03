package com.rpgstats.ability;

import java.util.Collections;
import com.rpgstats.classes.HouseRules;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapeia id do nó -> lista de efeitos reais de combate.
 * Nós sem entrada aqui só concedem atributos (bonus do SkillNode).
 */
public final class AbilityRegistry {
    private static final Map<String, List<SkillEffect>> EFFECTS = new HashMap<>();

    static {
        // ===== GUERREIRO =====
        put("golpe_esmagador", SkillEffect.passive("melee_damage", 0.08f));
        put("furia", SkillEffect.passive("resource_on_hit", 4f), SkillEffect.passive("resource_on_hurt", 3f));
        put("pele_aco", SkillEffect.passive("damage_reduction", 0.06f));
        put("veterano", SkillEffect.passive("melee_damage", 0.10f), SkillEffect.passive("damage_reduction", 0.04f));
        put("colosso", SkillEffect.passive("damage_reduction", 0.10f), SkillEffect.passive("thorns", 0.15f));

        // ===== MAGO v1.5: ver MageAbilityRegistry =====

        // ===== ARQUEIRO =====
        put("olho_aguia", SkillEffect.passive("ranged_damage", 0.08f));
        put("reflexos", SkillEffect.passive("move_speed", 0.04f));
        put("tiro_certeiro", SkillEffect.passive("ranged_damage", 0.10f), SkillEffect.passive("crit_chance", 0.05f));
        put("vento", SkillEffect.passive("move_speed", 0.06f), SkillEffect.passive("attack_speed", 0.05f));
        put("lenda", SkillEffect.passive("ranged_damage", 0.15f),
                SkillEffect.active("active_arrow_rain", 5f, 0, 360, 20f));

        // ===== ASSASSINO =====
        put("lamina", SkillEffect.passive("melee_damage", 0.06f), SkillEffect.passive("crit_chance", 0.04f));
        put("passo", SkillEffect.passive("move_speed", 0.08f));
        put("veneno", SkillEffect.passive("poison_hit", 1f));
        put("sombra", SkillEffect.passive("crit_chance", 0.08f),
                SkillEffect.active("active_smoke", 0f, 60, 280, 15f));
        put("ceifador", SkillEffect.passive("melee_damage", 0.12f), SkillEffect.passive("lifesteal", 0.08f));

        // ===== SUBCLASSES GUERREIRO =====
        put("ber_frenesi", SkillEffect.passive("attack_speed", 0.08f), SkillEffect.passive("melee_damage", 0.06f));
        put("ber_sangue", SkillEffect.passive("lifesteal", 0.10f));
        put("ber_resistencia", SkillEffect.passive("damage_reduction", 0.05f), SkillEffect.passive("resource_on_hurt", 5f));
        put("ber_massacre", SkillEffect.passive("melee_damage", 0.12f),
                SkillEffect.active("active_bloodlust", 0.25f, 120, 400, 25f));
        put("ber_avatar", SkillEffect.passive("melee_damage", 0.15f), SkillEffect.passive("lifesteal", 0.08f));

        put("gua_postura", SkillEffect.passive("damage_reduction", 0.10f),
                SkillEffect.active("active_guard_taunt", 0.40f, 100, 360, 15f));
        put("gua_aco", SkillEffect.passive("damage_reduction", 0.08f), SkillEffect.passive("thorns", 0.10f));
        put("gua_vigilia", SkillEffect.passive("crit_chance", 0.05f));
        put("gua_bastiao", SkillEffect.passive("damage_reduction", 0.12f));
        put("gua_inabalavel", SkillEffect.passive("damage_reduction", 0.15f), SkillEffect.passive("thorns", 0.20f));

        put("run_marca", SkillEffect.passive("melee_damage", 0.06f), SkillEffect.passive("magic_power", 0.06f));
        put("run_lamina", SkillEffect.passive("melee_damage", 0.10f), SkillEffect.passive("magic_power", 0.05f));
        put("run_protecao", SkillEffect.passive("damage_reduction", 0.07f));
        put("run_tempestade", SkillEffect.passive("magic_power", 0.10f),
                SkillEffect.active("active_aoe", 6f, 0, 280, 22f));
        put("run_mestre", SkillEffect.passive("melee_damage", 0.12f), SkillEffect.passive("magic_power", 0.12f));

        // ===== MAGO expandido: ver MageAbilityRegistry =====

        // ===== SUBCLASSES ARQUEIRO =====
        put("ati_mira", SkillEffect.passive("ranged_damage", 0.12f));
        put("ati_foco", SkillEffect.passive("ranged_damage", 0.10f), SkillEffect.passive("crit_chance", 0.06f));
        put("ati_passo", SkillEffect.passive("move_speed", 0.05f));
        put("ati_perfurante", SkillEffect.passive("ranged_damage", 0.15f));
        put("ati_mestre", SkillEffect.passive("ranged_damage", 0.20f), SkillEffect.passive("crit_chance", 0.10f));

        put("cac_instinto", SkillEffect.passive("ranged_damage", 0.06f), SkillEffect.passive("move_speed", 0.04f));
        put("cac_trilha", SkillEffect.passive("move_speed", 0.08f));
        put("cac_presa", SkillEffect.passive("ranged_damage", 0.10f));
        put("cac_feroz", SkillEffect.passive("melee_damage", 0.08f), SkillEffect.passive("heal_on_kill", 3f));
        put("cac_alfa", SkillEffect.passive("ranged_damage", 0.12f), SkillEffect.passive("move_speed", 0.08f));

        put("pat_duplo", SkillEffect.passive("move_speed", 0.08f));
        put("pat_emboscada", SkillEffect.passive("crit_chance", 0.08f), SkillEffect.passive("melee_damage", 0.06f));
        put("pat_vigor", SkillEffect.passive("damage_reduction", 0.05f));
        put("pat_chuva", SkillEffect.passive("ranged_damage", 0.10f),
                SkillEffect.active("active_arrow_rain", 7f, 0, 320, 22f));
        put("pat_lenda", SkillEffect.passive("move_speed", 0.10f), SkillEffect.passive("damage_reduction", 0.06f));

        // ===== SUBCLASSES ASSASSINO =====
        put("nin_selo", SkillEffect.passive("move_speed", 0.10f), SkillEffect.passive("attack_speed", 0.06f));
        put("nin_shuriken", SkillEffect.passive("ranged_damage", 0.10f));
        put("nin_fumaca", SkillEffect.passive("move_speed", 0.06f),
                SkillEffect.active("active_smoke", 0f, 80, 260, 12f));
        put("nin_combo", SkillEffect.passive("attack_speed", 0.12f), SkillEffect.passive("crit_chance", 0.08f));
        put("nin_mestre", SkillEffect.passive("move_speed", 0.12f), SkillEffect.passive("crit_chance", 0.12f));

        put("alq_formula", SkillEffect.passive("poison_hit", 1.5f), SkillEffect.passive("magic_power", 0.06f));
        put("alq_resistencia", SkillEffect.passive("damage_reduction", 0.07f));
        put("alq_corrosao", SkillEffect.passive("poison_hit", 2f), SkillEffect.passive("melee_damage", 0.05f));
        put("alq_nuvem", SkillEffect.passive("poison_hit", 2.5f),
                SkillEffect.active("active_aoe", 5f, 0, 300, 20f));
        put("alq_peste", SkillEffect.passive("poison_hit", 3f), SkillEffect.passive("magic_power", 0.12f));

        put("esp_vulto", SkillEffect.passive("move_speed", 0.08f), SkillEffect.passive("melee_damage", 0.06f));
        put("esp_intangivel", SkillEffect.passive("damage_reduction", 0.08f),
                SkillEffect.active("active_dash", 1.6f, 0, 200, 10f));
        put("esp_corte", SkillEffect.passive("melee_damage", 0.12f));
        put("esp_assombro", SkillEffect.passive("melee_damage", 0.10f), SkillEffect.passive("crit_chance", 0.08f));
        put("esp_morte", SkillEffect.passive("melee_damage", 0.15f), SkillEffect.passive("move_speed", 0.10f));

    }

    private static void put(String id, SkillEffect... effects) {
        EFFECTS.put(id, List.of(effects));
    }

    public static List<SkillEffect> get(String nodeId) {
        if (HouseRules.borrowed(nodeId)) return get(HouseRules.real(nodeId)).stream().map(HouseRules::scaled).toList();
        List<SkillEffect> direct = EFFECTS.get(nodeId);
        if (direct != null) return direct;
        List<SkillEffect> mage = MageAbilityRegistry.get(nodeId);
        if (!mage.isEmpty()) return mage;
        return ClassAbilityRegistry.get(nodeId);
    }

    public static List<SkillEffect> allFor(java.util.Set<String> unlockedNodes) {
        java.util.ArrayList<SkillEffect> list = new java.util.ArrayList<>();
        for (String id : unlockedNodes) {
            list.addAll(get(id));
        }
        return list;
    }

    public static SkillEffect activeForNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return null;
        for (SkillEffect effect : get(nodeId)) {
            if (effect.type() == AbilityType.ACTIVE) return effect;
        }
        return null;
    }

    public static boolean hasActive(String nodeId) {
        return activeForNode(nodeId) != null;
    }

    public static float sumPassive(java.util.Set<String> unlockedNodes, String effectId) {
        return sumPassive(unlockedNodes, effectId, null);
    }

    /** secondaryOnly: null=todas, false=primaria, true=secundaria. */
    public static float sumPassive(java.util.Set<String> unlockedNodes, String effectId, Boolean secondaryOnly) {
        float total = 0f;
        for (String nodeId : unlockedNodes) {
            boolean secondary = HouseRules.borrowed(nodeId);
            if (secondaryOnly != null && secondaryOnly.booleanValue() != secondary) continue;
            for (SkillEffect effect : get(nodeId)) {
                if (effect.type() == AbilityType.PASSIVE && effect.effectId().equals(effectId)) {
                    total += effect.value();
                }
            }
        }
        return total;
    }

    public static String activeSummary(String nodeId) {
        String real = realId(nodeId);
        SkillEffect effect = activeForNode(nodeId);
        if (real.startsWith("mag_")) {
            String mage = MageAbilityRegistry.activeSummary(real, effect);
            if (!mage.isBlank()) return mage;
        }
        if (ClassAbilityRegistry.isExpandedClassNode(real) && effect != null) {
            String classSummary = ClassAbilityRegistry.activeSummary(real, effect.value());
            if (!classSummary.isBlank()) {
                return classSummary + " · " + (int) effect.resourceCost() + " recurso base · "
                        + Math.max(0, effect.cooldownTicks() / 20) + "s CD";
            }
        }
        return activeSummaryForEffect(effect);
    }

    /** Resumo reutilizavel pela HUD/UI para uma habilidade ativa ja resolvida. */
    public static String activeSummaryForEffect(SkillEffect effect) {
        if (effect == null || effect.type() != AbilityType.ACTIVE) return "";
        String label = switch (effect.effectId()) {
            case "active_self_heal" -> "Cura propria";
            case "active_group_heal" -> "Cura em area";
            case "active_aoe" -> "Explosao magica";
            case "active_guard" -> "Guarda";
            case "active_guard_taunt" -> "Guarda + provocacao";
            case "active_dash" -> "Avanco";
            case "active_arrow_rain" -> "Chuva de flechas";
            case "active_smite" -> "Golpe sagrado";
            case "active_smoke" -> "Fumaca";
            case "active_bloodlust" -> "Sede de sangue";
            case "active_power" -> "Poder";
            case "mage_active" -> "Habilidade de Mago";
            default -> effect.effectId().replace("active_", "").replace('_', ' ');
        };
        String magnitude = switch(effect.effectId()) {
            case "active_self_heal", "active_group_heal" -> String.format(java.util.Locale.ROOT, "%.1f HP", effect.value());
            case "active_guard", "active_guard_taunt", "active_power", "active_bloodlust" -> String.format(java.util.Locale.ROOT, "%.1f%%", effect.value()*100);
            case "active_smite" -> String.format(java.util.Locale.ROOT, "x%.2f no próximo golpe", effect.value());
            case "active_arrow_rain" -> Math.max(1, Math.round(effect.value())) + " flechas";
            case "active_aoe" -> String.format(java.util.Locale.ROOT, "%.1f dano base", effect.value());
            case "active_dash" -> String.format(java.util.Locale.ROOT, "impulso %.1f", effect.value());
            case "active_smoke" -> "invisibilidade e velocidade II";
            default -> "";
        };
        return label + " · " + magnitude + (effect.durationTicks()>0 ? " · "+(effect.durationTicks()/20f)+"s duração" : "") + " · " + (int) effect.resourceCost() + " recurso base · "
                + Math.max(0, effect.cooldownTicks() / 20) + "s CD";
    }

    private static String realId(String id) {
        return HouseRules.real(id);
    }

    private AbilityRegistry() {}
}
