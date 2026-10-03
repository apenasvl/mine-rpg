package com.rpgstats.classes;

import com.rpgstats.ability.*;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.tree.SkillNode;
import java.util.*;

/** One external House. Explicit curriculum, shared caps and category-specific efficiency. */
public final class HouseRules {
    public static final int LEVEL = 30, LIMIT = 4;
    public static final String PREFIX = "borrowed_";

    public static String real(String id) { return id.startsWith(PREFIX) ? id.substring(PREFIX.length()) : id; }
    public static boolean borrowed(String id) { return id != null && id.startsWith(PREFIX); }

    public static List<String> curriculum(RPGPath house) {
        if (house == null) return List.of();
        return switch (house) {
            case MAGE_ELEMENTAL -> List.of("mag_ele_affinity", "mag_ele_catalyst", "mag_ele_conservation", "mag_ele_burst");
            case MAGE_ARCANA -> List.of("mag_arc_precision", "mag_arc_high_magic", "mag_arc_counterspell", "mag_arc_pulse");
            case MAGE_CONJURATION -> List.of("mag_conj_pact", "mag_summ_familiar", "mag_conj_vitality", "mag_conj_power");
            case MAGE_OCCULT -> List.of("mag_occ_drain", "mag_curse_weakness", "mag_curse_master", "mag_occ_ritual");
            case MAGE_TEMPORAL -> List.of("mag_time_efficiency", "mag_time_shift", "mag_time_mastery", "mag_acc_acceleration");
            default -> {
                String prefix = house.nodes.get(0).id().replace("_foundation", "");
                yield List.of(prefix + "_foundation", prefix + "_discipline", prefix + "_economy", prefix + "_technique");
            }
        };
    }

    private static SkillNode original(String id) {
        for (RPGPath p : RPGPath.values()) {
            SkillNode n = p.findNode(id);
            if (n != null) return n;
        }
        for (RPGSpecialization s : RPGSpecialization.values()) {
            SkillNode n = s.findNode(id);
            if (n != null) return n;
        }
        return null;
    }

    public static RPGPath owner(String key) {
        if (!borrowed(key)) return null;
        for (RPGPath p : RPGPath.values()) if (curriculum(p).contains(real(key))) return p;
        return null;
    }

    public static SkillNode node(String key) {
        RPGPath house = owner(key);
        if (house == null) return null;
        List<String> ids = curriculum(house);
        int index = ids.indexOf(real(key));
        SkillNode n = original(real(key));
        if (n == null) return null;
        return new SkillNode(key, n.name(), "Afinidade secundaria: " + house.display
                + ". Consulte os efeitos reduzidos abaixo. " + penaltyText(house),
                2, index == 0 ? null : PREFIX + ids.get(index - 1), null, 0, Map.of(), LEVEL, index + 1, false);
    }

    public static List<SkillNode> nodes(RPGPath house) {
        return curriculum(house).stream().map(id -> node(PREFIX + id)).toList();
    }

    public static String chooseError(PlayerStats s, RPGPath house) {
        if (s.path == null || s.clazz == null) return "Escolha sua Casa principal.";
        if (s.level < LEVEL) return "Requer nivel 30.";
        if (s.affinityHouse != null) return "Voce ja escolheu sua Casa secundaria.";
        if (house == null || house.parent != s.clazz || house == s.path) return "Escolha outra Casa da sua propria classe.";
        return "";
    }

    public static String purchaseError(PlayerStats s, String key) {
        SkillNode n = node(key);
        if (n == null || s.affinityHouse == null || owner(key) != s.affinityHouse
                || s.path == null || s.affinityHouse == s.path || s.affinityHouse.parent != s.clazz)
            return "Talento fora da sua Casa secundaria.";
        if (s.level < LEVEL) return "Requer nivel 30.";
        if (s.unlockedNodes.contains(key)) return "Ja aprendido.";
        if (count(s) >= LIMIT) return "Limite de quatro talentos secundarios.";
        if (n.prereqNode() != null && !s.unlockedNodes.contains(n.prereqNode())) return "Aprenda o talento anterior.";
        if (s.skillPoints < n.cost()) return "Requer 2 PH.";
        return "";
    }

    public static int count(PlayerStats s) {
        return (int) s.unlockedNodes.stream().filter(HouseRules::borrowed).count();
    }

    /**
     * A afinidade secundaria deve ser perceptivel sem competir com a Casa principal.
     * Valores deliberadamente variam por categoria; nao existe multiplicador global unico.
     */
    public static float passiveScale(String effect) {
        return switch (effect) {
            case "lifesteal", "spell_lifesteal" -> .55f;
            case "cooldown_recovery" -> .60f;
            case "damage_reduction", "summon_defense" -> .60f;
            case "resource_on_hit", "resource_on_hurt", "resource_on_kill", "resource_flat",
                    "mana_flat", "mana_regen_flat", "mana_max_pct", "bond_flat", "summon_refund" -> .70f;
            case "crit_chance", "spell_crit", "attack_speed", "move_speed",
                    "mana_cost_reduction", "elemental_cost_reduction" -> .70f;
            case "poison_hit", "reaction_damage", "frozen_fragility", "shatter_damage" -> .72f;
            case "summon_duration", "curse_duration", "reaction_aoe" -> .75f;
            case "melee_damage", "ranged_damage", "magic_power", "elemental_damage", "summon_damage" -> .65f;
            default -> .65f;
        };
    }

    public static SkillEffect scaled(SkillEffect e) {
        if (e.type() == AbilityType.PASSIVE)
            return SkillEffect.passive(e.effectId(), e.value() * passiveScale(e.effectId()));
        // Ativas mantem a identidade; a secundaria reduz magnitude/duracao, nao o tipo da tecnica.
        return SkillEffect.active(e.effectId(), e.effectId().equals("active_smite")
                        ? 1 + (e.value() - 1) * .6f : e.value() * .6f,
                Math.round(e.durationTicks() * .7f), e.cooldownTicks(), e.resourceCost());
    }

    public static float surcharge(PlayerStats s) {
        if (s.affinityHouse == null) return 0;
        return count(s) * (s.affinityHouse == RPGPath.MAGE_TEMPORAL ? .03f : .01f);
    }

    public static float vulnerability(PlayerStats s) {
        if (s.affinityHouse == null) return 0;
        return count(s) * (switch (s.affinityHouse) {
            case MAGE_OCCULT, WAR_BERSERKER, ASS_VENOM -> .02f;
            default -> 0f;
        });
    }

    public static float offensePenalty(PlayerStats s) {
        if (s.affinityHouse == null) return 0;
        return count(s) * (switch (s.affinityHouse) {
            case WAR_VANGUARD, ARC_WARDEN -> .01f;
            default -> 0f;
        });
    }

    public static String penaltyText(RPGPath house) {
        PlayerStats s = new PlayerStats();
        s.affinityHouse = house;
        s.unlockedNodes.add(PREFIX + "preview");
        return String.format(java.util.Locale.ROOT,
                "Por talento aprendido: custo +%.0f%%; dano recebido +%.0f%%; dano causado -%.0f%%. Penalidade permanente enquanto aprendido.",
                surcharge(s) * 100, vulnerability(s) * 100, offensePenalty(s) * 100);
    }

    private HouseRules() {}
}
