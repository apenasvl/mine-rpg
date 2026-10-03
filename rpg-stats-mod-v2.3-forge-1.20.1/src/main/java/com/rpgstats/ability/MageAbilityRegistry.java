package com.rpgstats.ability;

import com.rpgstats.compat.CompatManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Efeitos dos 162 nodes do Mago v1.5. Mecânicas complexas são consumidas por MageCombatHandler. */
public final class MageAbilityRegistry {
    private static final Map<String, List<SkillEffect>> EFFECTS = new HashMap<>();
    private static final Map<String, SkillEffect> STANDALONE_FALLBACKS = new HashMap<>();

    static {
        put("mag_core_awakening", SkillEffect.passive("mana_flat", 20.0000f));
        put("mag_core_channeling", SkillEffect.passive("mana_regen_flat", 0.4000f));
        put("mag_core_reserve", SkillEffect.passive("mana_max_pct", 0.1000f));
        put("mag_core_precision", SkillEffect.passive("spell_crit", 0.0300f));
        put("mag_core_efficiency", SkillEffect.passive("mana_cost_reduction", 0.0400f));
        put("mag_core_stable_mind", SkillEffect.passive("mana_regen_pct", 0.1000f));
        put("mag_core_flow");
        put("mag_core_weaving");
        put("mag_core_guard");
        put("mag_core_knowledge", SkillEffect.passive("magic_power", 0.0500f));
        put("mag_core_echo", SkillEffect.passive("mage_echo", 0.0800f));
        put("mag_core_mastery");
        put("mag_ele_affinity", SkillEffect.passive("elemental_damage", 0.0400f));
        put("mag_ele_resonance");
        put("mag_ele_catalyst", SkillEffect.passive("reaction_damage", 0.1000f));
        put("mag_ele_burst", SkillEffect.passive("irons_elemental_burst", 0.1000f));
        put("mag_ele_opposites");
        put("mag_ele_instability", SkillEffect.passive("reaction_aoe", 2.5000f));
        put("mag_ele_conservation", SkillEffect.passive("elemental_cost_reduction", 0.0500f));
        put("mag_ele_perfect_reaction", SkillEffect.passive("reaction_damage", 0.0800f));
        put("mag_ele_mastery", SkillEffect.passive("elemental_damage", 0.0500f));
        put("mag_arc_sequence");
        put("mag_arc_triad");
        put("mag_arc_precision", SkillEffect.passive("spell_crit", 0.0400f));
        put("mag_arc_pulse", SkillEffect.passive("irons_evocation_pulse", 0.0800f));
        put("mag_arc_preparation");
        put("mag_arc_finisher");
        put("mag_arc_counterspell", SkillEffect.passive("irons_counterspell_guard", 0.1000f));
        put("mag_arc_perfect_cycle");
        put("mag_arc_high_magic", SkillEffect.passive("magic_power", 0.0500f));
        put("mag_conj_pact");
        put("mag_conj_vitality", SkillEffect.passive("summon_duration", 0.1000f));
        put("mag_conj_power", SkillEffect.passive("summon_damage", 0.1200f));
        put("mag_conj_focus_order", SkillEffect.active("mage_active", 0.0000f, 100, 160, 0.0000f));
        put("mag_conj_shared_flow");
        put("mag_conj_defensive", SkillEffect.passive("summon_defense", 0.0500f));
        put("mag_conj_extended_bond", SkillEffect.passive("bond_flat", 2.0000f));
        put("mag_conj_recycling", SkillEffect.passive("summon_refund", 0.1500f));
        put("mag_conj_mastery", SkillEffect.passive("bond_flat", 1.0000f));
        put("mag_occ_forbidden");
        put("mag_occ_temptation");
        put("mag_occ_instability");
        put("mag_occ_forbidden_power");
        put("mag_occ_drain", SkillEffect.passive("spell_lifesteal", 0.0300f));
        put("mag_occ_efficiency");
        put("mag_occ_backlash");
        put("mag_occ_purification", SkillEffect.active("mage_active", 40.0000f, 0, 400, 0.0000f));
        put("mag_occ_ritual", SkillEffect.passive("magic_power", 0.0400f));
        put("mag_time_sensitivity");
        put("mag_time_rhythm");
        put("mag_time_efficiency", SkillEffect.passive("cooldown_recovery", 0.0500f));
        put("mag_time_shift", SkillEffect.passive("irons_mobility_weaving", 0.0600f));
        put("mag_time_stored_moment");
        put("mag_time_recovery");
        put("mag_time_expanded_memory", SkillEffect.passive("temporal_fragment_max", 2.0000f));
        put("mag_time_continuity");
        put("mag_time_mastery", SkillEffect.passive("cooldown_recovery", 0.0500f));
        put("mag_pyr_ember_lance", SkillEffect.passive("irons_fire_heat", 5.0000f));
        put("mag_pyr_fuel", SkillEffect.passive("fire_damage_marked", 0.0600f));
        put("mag_pyr_flame_wall", SkillEffect.passive("irons_fire_area_heat", 8.0000f));
        put("mag_pyr_combustion");
        put("mag_pyr_ash_step", SkillEffect.passive("irons_fire_mobility", 0.0600f));
        put("mag_pyr_controlled_inferno", SkillEffect.passive("fire_crit_hot", 0.0800f));
        put("mag_pyr_asc_phoenix");
        put("mag_cryo_ice_shard", SkillEffect.passive("irons_ice_chill", 5.0000f));
        put("mag_cryo_fragility", SkillEffect.passive("frozen_fragility", 0.0800f));
        put("mag_cryo_ice_barrier", SkillEffect.passive("irons_ice_guard", 0.1000f));
        put("mag_cryo_frost_nova", SkillEffect.passive("irons_ice_area_chill", 8.0000f));
        put("mag_cryo_deep_freeze");
        put("mag_cryo_shatter", SkillEffect.passive("shatter_damage", 3.0000f));
        put("mag_cryo_asc_winterheart");
        put("mag_storm_arc_bolt", SkillEffect.passive("irons_lightning_charge", 1.0000f));
        put("mag_storm_static_charge");
        put("mag_storm_lightning_step", SkillEffect.passive("irons_lightning_mobility", 0.0500f));
        put("mag_storm_static_field", SkillEffect.passive("irons_lightning_area", 0.0600f));
        put("mag_storm_overload");
        put("mag_storm_conductor");
        put("mag_storm_asc_avatar", SkillEffect.active("mage_active", 0.0000f, 160, 800, 100.0000f));
        put("mag_rune_inscribe", SkillEffect.active("mage_active", 0.0000f, 0, 40, 18.0000f));
        put("mag_rune_explosive");
        put("mag_rune_protection");
        put("mag_rune_amplifier");
        put("mag_rune_network");
        put("mag_rune_geometry", SkillEffect.passive("rune_limit", 2.0000f));
        put("mag_rune_asc_grand_circle", SkillEffect.active("mage_active", 0.2500f, 200, 1200, 60.0000f));
        put("mag_illu_mirror_image", SkillEffect.passive("irons_illusion_charges", 2.0000f));
        put("mag_illu_illusory_step", SkillEffect.passive("irons_illusion_weaving", 0.2000f));
        put("mag_illu_confusion", SkillEffect.passive("irons_illusion_control", 0.1000f));
        put("mag_illu_ghost_attack");
        put("mag_illu_perfect_decoy");
        put("mag_illu_arcane_reflection", SkillEffect.passive("illusion_echo", 0.1200f));
        put("mag_illu_asc_mirror_hall", SkillEffect.active("mage_active", 5.0000f, 160, 1200, 55.0000f));
        put("mag_tele_blink", SkillEffect.passive("irons_teleport_weaving", 0.0800f));
        put("mag_tele_repulsion", SkillEffect.passive("irons_displacement_control", 0.1000f));
        put("mag_tele_anchor", SkillEffect.active("mage_active", 0.0000f, 160, 240, 22.0000f));
        put("mag_tele_double_portal", SkillEffect.passive("irons_portal_mastery", 0.1000f));
        put("mag_tele_compression", SkillEffect.passive("teleport_spell_bonus", 0.1200f));
        put("mag_tele_singularity", SkillEffect.passive("irons_gravity_control", 0.1000f));
        put("mag_tele_asc_collapse", SkillEffect.passive("irons_gravity_ascension", 0.1500f));
        put("mag_summ_familiar", SkillEffect.passive("irons_summon_bond", 1.0000f));
        put("mag_summ_guardian", SkillEffect.passive("irons_summon_guard", 0.0800f));
        put("mag_summ_assault_order", SkillEffect.active("mage_active", 0.1200f, 100, 240, 0.0000f));
        put("mag_summ_pack_tactics", SkillEffect.passive("summon_diversity_damage", 0.1500f));
        put("mag_summ_transfer", SkillEffect.active("mage_active", 0.2000f, 0, 240, 15.0000f));
        put("mag_summ_great_conjuration", SkillEffect.passive("irons_summon_elite", 0.1500f));
        put("mag_summ_asc_ephemeral_army", SkillEffect.active("mage_active", 3.0000f, 200, 1200, 70.0000f));
        put("mag_anim_life_spirit", SkillEffect.passive("irons_spirit_life", 0.0500f));
        put("mag_anim_earth_spirit", SkillEffect.passive("irons_spirit_earth", 0.0500f));
        put("mag_anim_hunt_spirit", SkillEffect.passive("irons_spirit_hunt", 0.0500f));
        put("mag_anim_spirit_totem", SkillEffect.active("mage_active", 1.0000f, 160, 300, 25.0000f));
        put("mag_anim_spirit_swap", SkillEffect.active("mage_active", 1.0000f, 0, 80, 0.0000f));
        put("mag_anim_harmony");
        put("mag_anim_asc_council", SkillEffect.active("mage_active", 1.0000f, 240, 1500, 70.0000f));
        put("mag_astral_blade", SkillEffect.passive("irons_astral_offense", 0.0600f));
        put("mag_astral_shield", SkillEffect.passive("irons_astral_defense", 0.0600f));
        put("mag_astral_turret", SkillEffect.passive("irons_astral_projectile", 0.0600f));
        put("mag_astral_formation", SkillEffect.active("mage_active", 1.0000f, 0, 80, 0.0000f));
        put("mag_astral_resonant", SkillEffect.passive("astral_resonance", 0.1200f));
        put("mag_astral_core", SkillEffect.passive("summon_duration", 0.1500f), SkillEffect.passive("bond_flat", 1.0000f));
        put("mag_astral_asc_arsenal", SkillEffect.passive("irons_astral_ascension", 0.1200f));
        put("mag_blood_lance", SkillEffect.passive("irons_blood_hemorrhage", 1.0000f));
        put("mag_blood_vital_conversion");
        put("mag_blood_hemorrhage");
        put("mag_blood_transfusion", SkillEffect.passive("irons_blood_drain", 0.0200f));
        put("mag_blood_crimson_desperation");
        put("mag_blood_blood_pact", SkillEffect.passive("spell_lifesteal", 0.0300f));
        put("mag_blood_asc_eclipse", SkillEffect.active("mage_active", 0.1000f, 200, 1200, 0.0000f));
        put("mag_curse_weakness", SkillEffect.passive("irons_curse_weakness", 0.1000f));
        put("mag_curse_fragility", SkillEffect.passive("irons_curse_fragility", 0.0800f));
        put("mag_curse_ruin", SkillEffect.passive("irons_curse_ruin", 0.1000f));
        put("mag_curse_spread", SkillEffect.active("mage_active", 3.0000f, 0, 280, 24.0000f));
        put("mag_curse_fate_mark");
        put("mag_curse_master", SkillEffect.passive("curse_duration", 0.2000f));
        put("mag_curse_asc_great_curse", SkillEffect.active("mage_active", 1.0000f, 160, 1400, 60.0000f));
        put("mag_hex_imbue", SkillEffect.passive("hex_melee_magic", 1.5000f));
        put("mag_hex_blink_strike", SkillEffect.passive("irons_hex_mobility", 4.0000f));
        put("mag_hex_mystic_parry", SkillEffect.active("mage_active", 0.7000f, 12, 200, 20.0000f));
        put("mag_hex_elemental_imbue", SkillEffect.active("mage_active", 1.0000f, 0, 80, 0.0000f));
        put("mag_hex_spellblade_rhythm", SkillEffect.passive("spellblade_rhythm", 0.0300f));
        put("mag_hex_dimensional_cut", SkillEffect.passive("irons_hex_slash", 0.1000f));
        put("mag_hex_asc_arcane_form", SkillEffect.active("mage_active", 0.0000f, 200, 1200, 70.0000f));
        put("mag_acc_acceleration", SkillEffect.passive("irons_haste_momentum", 0.1000f));
        put("mag_acc_instant_cast", SkillEffect.active("mage_active", 0.0000f, 200, 300, 25.0000f));
        put("mag_acc_temporal_step", SkillEffect.passive("irons_temporal_mobility", 1.0000f));
        put("mag_acc_overclock", SkillEffect.active("mage_active", 0.2000f, 0, 160, 1.0000f));
        put("mag_acc_momentum");
        put("mag_acc_fast_recovery", SkillEffect.passive("mana_regen_pct", 0.2000f));
        put("mag_acc_asc_distorted_time", SkillEffect.active("mage_active", 0.0000f, 160, 1400, 70.0000f));
        put("mag_stag_temporal_slow", SkillEffect.passive("irons_slow_mastery", 0.1000f));
        put("mag_stag_stasis_bubble", SkillEffect.active("mage_active", 0.3500f, 100, 320, 30.0000f));
        put("mag_stag_temporal_anchor", SkillEffect.passive("irons_root_anchor", 0.1000f));
        put("mag_stag_time_lock");
        put("mag_stag_slow_horizon");
        put("mag_stag_entropy", SkillEffect.passive("stasis_magic_amp", 0.0600f));
        put("mag_stag_asc_time_stop", SkillEffect.active("mage_active", 0.8000f, 80, 1800, 80.0000f));
        put("mag_rev_temporal_mark", SkillEffect.active("mage_active", 0.0000f, 100, 300, 20.0000f));
        put("mag_rev_rewind", SkillEffect.active("mage_active", 4.0000f, 0, 240, 0.0000f));
        put("mag_rev_temporal_echo", SkillEffect.active("mage_active", 0.4000f, 200, 360, 25.0000f));
        put("mag_rev_second_chance", SkillEffect.passive("temporal_second_chance", 1.0000f));
        put("mag_rev_temporal_debt");
        put("mag_rev_causal_loop");
        put("mag_rev_asc_rewrite", SkillEffect.active("mage_active", 0.6000f, 120, 2400, 90.0000f));
        fallback("mag_ele_burst", SkillEffect.active("mage_active", 6.0000f, 0, 120, 20.0000f));
        fallback("mag_arc_pulse", SkillEffect.active("mage_active", 5.0000f, 0, 140, 18.0000f));
        fallback("mag_arc_counterspell", SkillEffect.active("mage_active", 0.3500f, 40, 280, 25.0000f));
        fallback("mag_time_shift", SkillEffect.active("mage_active", 4.5000f, 0, 200, 15.0000f));
        fallback("mag_pyr_ember_lance", SkillEffect.active("mage_active", 5.0000f, 0, 50, 14.0000f));
        fallback("mag_pyr_flame_wall", SkillEffect.active("mage_active", 2.0000f, 100, 240, 28.0000f));
        fallback("mag_pyr_ash_step", SkillEffect.active("mage_active", 5.0000f, 40, 160, 20.0000f));
        fallback("mag_cryo_ice_shard", SkillEffect.active("mage_active", 4.0000f, 0, 40, 12.0000f));
        fallback("mag_cryo_ice_barrier", SkillEffect.active("mage_active", 4.0000f, 120, 360, 30.0000f));
        fallback("mag_cryo_frost_nova", SkillEffect.active("mage_active", 3.0000f, 0, 200, 25.0000f));
        fallback("mag_storm_arc_bolt", SkillEffect.active("mage_active", 4.5000f, 0, 40, 13.0000f));
        fallback("mag_storm_lightning_step", SkillEffect.active("mage_active", 4.0000f, 40, 140, 18.0000f));
        fallback("mag_storm_static_field", SkillEffect.active("mage_active", 1.5000f, 100, 280, 30.0000f));
        fallback("mag_illu_mirror_image", SkillEffect.active("mage_active", 2.0000f, 120, 300, 25.0000f));
        fallback("mag_illu_illusory_step", SkillEffect.active("mage_active", 4.0000f, 30, 180, 18.0000f));
        fallback("mag_illu_confusion", SkillEffect.active("mage_active", 0.0000f, 120, 240, 24.0000f));
        fallback("mag_tele_blink", SkillEffect.active("mage_active", 5.0000f, 0, 120, 12.0000f));
        fallback("mag_tele_repulsion", SkillEffect.active("mage_active", 3.0000f, 0, 160, 20.0000f));
        fallback("mag_tele_double_portal", SkillEffect.active("mage_active", 0.0000f, 200, 360, 30.0000f));
        fallback("mag_tele_singularity", SkillEffect.active("mage_active", 7.0000f, 40, 400, 40.0000f));
        fallback("mag_tele_asc_collapse", SkillEffect.active("mage_active", 12.0000f, 60, 1400, 70.0000f));
        fallback("mag_summ_familiar", SkillEffect.active("mage_active", 1.0000f, 1200, 100, 25.0000f));
        fallback("mag_summ_guardian", SkillEffect.active("mage_active", 2.0000f, 1200, 200, 40.0000f));
        fallback("mag_summ_great_conjuration", SkillEffect.active("mage_active", 4.0000f, 800, 600, 65.0000f));
        fallback("mag_anim_life_spirit", SkillEffect.active("mage_active", 1.0000f, 240, 160, 25.0000f));
        fallback("mag_anim_earth_spirit", SkillEffect.active("mage_active", 1.0000f, 240, 160, 25.0000f));
        fallback("mag_anim_hunt_spirit", SkillEffect.active("mage_active", 1.0000f, 240, 160, 25.0000f));
        fallback("mag_astral_blade", SkillEffect.active("mage_active", 1.0000f, 1200, 120, 22.0000f));
        fallback("mag_astral_shield", SkillEffect.active("mage_active", 2.0000f, 1200, 200, 28.0000f));
        fallback("mag_astral_turret", SkillEffect.active("mage_active", 2.0000f, 1200, 240, 35.0000f));
        fallback("mag_astral_asc_arsenal", SkillEffect.active("mage_active", 6.0000f, 200, 1200, 65.0000f));
        fallback("mag_blood_lance", SkillEffect.active("mage_active", 6.0000f, 0, 60, 8.0000f));
        fallback("mag_blood_transfusion", SkillEffect.active("mage_active", 4.0000f, 0, 280, 25.0000f));
        fallback("mag_curse_weakness", SkillEffect.active("mage_active", 0.1000f, 120, 160, 15.0000f));
        fallback("mag_curse_fragility", SkillEffect.active("mage_active", 0.0800f, 120, 200, 18.0000f));
        fallback("mag_curse_ruin", SkillEffect.active("mage_active", 1.5000f, 160, 240, 22.0000f));
        fallback("mag_hex_blink_strike", SkillEffect.active("mage_active", 4.0000f, 100, 120, 15.0000f));
        fallback("mag_hex_dimensional_cut", SkillEffect.active("mage_active", 7.0000f, 0, 240, 30.0000f));
        fallback("mag_acc_acceleration", SkillEffect.active("mage_active", 0.0000f, 100, 240, 20.0000f));
        fallback("mag_acc_temporal_step", SkillEffect.active("mage_active", 4.0000f, 0, 100, 12.0000f));
        fallback("mag_stag_temporal_slow", SkillEffect.active("mage_active", 0.2000f, 80, 160, 18.0000f));
        fallback("mag_stag_temporal_anchor", SkillEffect.active("mage_active", 0.3500f, 100, 200, 20.0000f));
    }

    /** Resumo fiel ao tipo de recurso das ativas do Mago para HUD/tooltips. */
    public static String activeSummary(String nodeId, SkillEffect effect) {
        if (effect == null || effect.type() != AbilityType.ACTIVE) return "";
        String cost = switch (nodeId) {
            case "mag_storm_asc_avatar" -> "100 Mana";
            case "mag_summ_asc_ephemeral_army", "mag_anim_asc_council",
                    "mag_hex_asc_arcane_form", "mag_acc_asc_distorted_time" -> "70 Mana";
            case "mag_stag_asc_time_stop" -> "80 Mana";
            case "mag_rev_asc_rewrite" -> "90 Mana";
            case "mag_astral_asc_arsenal" -> "65 Mana";
            case "mag_acc_overclock" -> "1 Fragmento Temporal";
            case "mag_blood_lance" -> "8 Mana + 2 HP";
            default -> effect.resourceCost() > 0f
                    ? ((int) effect.resourceCost()) + " Mana"
                    : "sem custo de Mana";
        };
        return "ATIVA · " + cost + " · " + Math.max(0, effect.cooldownTicks() / 20) + "s CD";
    }

    private static void put(String id, SkillEffect... effects) { EFFECTS.put(id, List.of(effects)); }
    private static void fallback(String id, SkillEffect effect) { STANDALONE_FALLBACKS.put(id, effect); }
    public static List<SkillEffect> get(String nodeId) {
        SkillEffect fallback = STANDALONE_FALLBACKS.get(nodeId);
        if (fallback != null && !CompatManager.isActive("irons_spells")) return List.of(fallback);
        return EFFECTS.getOrDefault(nodeId, Collections.emptyList());
    }
    public static boolean contains(String nodeId) { return EFFECTS.containsKey(nodeId); }
    public static java.util.Set<String> ids() { return java.util.Collections.unmodifiableSet(EFFECTS.keySet()); }
    private MageAbilityRegistry() {}
}
