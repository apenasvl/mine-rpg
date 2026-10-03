package com.rpgstats.combat;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.compat.CompatManager;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mecânicas profundas do Mago v1.5. Mantém os números próximos da escala vanilla:
 * spells comuns 4-9 HP, ultimates ~12-20 HP e caps rígidos de amplificação.
 */
public final class MageCombatHandler {
    private static final UUID TEMP_MOVE = UUID.fromString("4ad8a1a0-8d7a-4b2e-baaa-000000000101");
    private static final UUID TEMP_ATTACK = UUID.fromString("4ad8a1a0-8d7a-4b2e-baaa-000000000102");

    private static final java.util.Set<String> MOBILITY_NODES = java.util.Set.of(
            "mag_time_shift", "mag_pyr_ash_step", "mag_storm_lightning_step", "mag_illu_illusory_step",
            "mag_tele_blink", "mag_hex_blink_strike", "mag_acc_temporal_step");

    public static boolean isMageNode(String nodeId) {
        if (nodeId == null) return false;
        String id = HouseRules.real(nodeId);
        return id.startsWith("mag_");
    }

    public static void remove(UUID playerId) {
        MageState.remove(playerId);
    }

    /** Versão correta quando há jogador/estado disponível. */
    public static int effectiveCooldown(ServerPlayerEntity player, PlayerStats stats, String nodeId, SkillEffect effect) {
        float cdr = Math.min(MageBalance.PERMANENT_CDR_CAP,
                Math.max(0f, AbilityRegistry.sumPassive(stats.unlockedNodes, "cooldown_recovery", false)));
        MageState state = MageState.get(player.getUuid());
        if (state.momentumTicks > 0 && stats.hasNode("mag_acc_momentum"))
            cdr += 0.03f * Math.min(3, state.momentumStacks);
        if (state.distortedTimeTicks > 0) cdr += 0.25f;
        if (state.accelerationTicks > 0) cdr += 0.10f;
        cdr = Math.min(MageBalance.TEMPORARY_CDR_CAP, Math.max(0f, cdr));
        int result = Math.round(effect.cooldownTicks() * (1f - cdr));
        if (state.instantCastTicks > 0 && isOffensiveMageActive(nodeId) && !isAscension(nodeId)) {
            result = Math.round(result * 0.80f);
            state.instantCastTicks = 0;
        }
        return Math.max(isAscension(nodeId) ? 600 : 10, result);
    }

    /** Cobra Mana/Fragmentos/vida conforme o node. Concentração foi removida. */
    public static boolean tryPayCost(ServerPlayerEntity player, PlayerStats stats, String nodeId, SkillEffect effect) {
        MageState state = MageState.get(player.getUuid());
        String id = real(nodeId);

        if (id.equals("mag_acc_overclock")) {
            if (stats.temporalFragments < 1) {
                player.sendMessage(Text.literal("§cVocê precisa de 1 Fragmento Temporal."), true);
                return false;
            }
            stats.temporalFragments--;
            onFragmentSpent(player, stats, state);
            return true;
        }


        float manaCost = effectiveManaCost(stats, state, id, effect.resourceCost())*(1f+HouseRules.surcharge(stats));
        float healthExtra = id.equals("mag_blood_lance") ? 2f : 0f;
        if (healthExtra > 0 && player.getHealth() - healthExtra < 3f) {
            player.sendMessage(Text.literal("§cVida insuficiente para esta magia de sangue."), true);
            return false;
        }

        float availableMana = CompatManager.mageMana(player, stats);
        if (availableMana < manaCost) {
            boolean canConvert = stats.hasNode("mag_blood_vital_conversion") || state.bloodEclipseTicks > 0;
            float missing = manaCost - availableMana;
            float hpPerMana = state.bloodEclipseTicks > 0 ? 0.20f : 0.25f; // 1 HP = 5 ou 4 Mana
            float hpCost = missing * hpPerMana;
            if (!canConvert || player.getHealth() - hpCost - healthExtra < 3f) {
                player.sendMessage(Text.literal("§cMana insuficiente: " + (int) availableMana + "/" + (int) Math.ceil(manaCost)), true);
                return false;
            }
            CompatManager.setMageMana(player, stats, 0f);
            player.setHealth(Math.max(3f, player.getHealth() - hpCost));
        } else {
            CompatManager.setMageMana(player, stats, availableMana - manaCost);
        }

        if (healthExtra > 0) player.setHealth(Math.max(3f, player.getHealth() - healthExtra));

        if (manaCost > 0) {
            state.manaSpentWindow += manaCost;
            state.manaSpentWindowTicks = 60;
            if (stats.hasNode("mag_core_guard") && state.manaSpentWindow >= 35f) {
                CombatState combat = CombatState.get(player.getUuid());
                if (combat.cooldown("internal_mage_guard") <= 0) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 120, 0, false, false, true));
                    combat.startCooldown("internal_mage_guard", 300);
                    state.manaSpentWindow = 0f;
                }
            }
        }

        state.flowReady = false;
        state.causalLoopReady = false;
        return true;
    }

    private static float effectiveManaCost(PlayerStats stats, MageState state, String nodeId, float base) {
        if (base <= 0f) return 0f;
        float reduction = AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_cost_reduction", false);
        if (isElementalNode(nodeId)) reduction += AbilityRegistry.sumPassive(stats.unlockedNodes, "elemental_cost_reduction", false);
        if (stats.corruption >= 50f && stats.hasNode("mag_occ_efficiency")) reduction += 0.06f;
        if (state.arcaneFormTicks > 0) reduction += 0.15f;
        if (state.flowReady) reduction += 0.12f;
        if (state.causalLoopReady) reduction += 0.30f;
        if (state.idleTicks >= 60 && stats.hasNode("mag_arc_preparation")) reduction += 0.10f;
        reduction = Math.min(MageBalance.MAX_MANA_COST_REDUCTION, Math.max(0f, reduction));
        return Math.max(0f, base * (1f - reduction));
    }

    /** Executa um node ativo já pago e em cooldown. */
    public static void activate(ServerPlayerEntity player, PlayerStats stats, String nodeId, SkillEffect effect) {
        String id = real(nodeId);
        MageState state = MageState.get(player.getUuid());
        MageState.School school = schoolFor(id);
        recordCastStart(stats, state, id);
        state.beginCast(id, school, true);

        try {
            switch (id) {
                case "mag_ele_burst" -> elementalBurst(player, stats, effect);
                case "mag_arc_pulse" -> pulseAround(player, stats, effect.value(), 4.0, MageState.School.ARCANE, 0.7f);
                case "mag_arc_counterspell" -> { state.counterspellTicks = effect.durationTicks(); state.counterspellReduction = effect.value(); }
                case "mag_conj_focus_order" -> {
                    LivingEntity focus = rayTarget(player, 18);
                    if (focus != null) {
                        if (CompatManager.usesExternalMageMana()) CompatManager.focusNativeSummons(player, stats, focus);
                        else {
                            state.summonFocusTarget = focus.getUuid();
                            state.summonFocusTicks = Math.max(100, effect.durationTicks());
                        }
                        player.sendMessage(Text.literal("§dInvocações focando: §f" + focus.getName().getString()), true);
                    } else {
                        player.sendMessage(Text.literal("§7Nenhum alvo válido para focar."), true);
                    }
                }
                case "mag_occ_purification" -> stats.corruption = Math.max(0f, stats.corruption - 40f);
                case "mag_time_shift" -> dash(player, effect.value());

                case "mag_pyr_ember_lance" -> directBolt(player, stats, 18, effect.value(), MageState.School.FIRE);
                case "mag_pyr_flame_wall" -> addZoneAtAim(player, MageState.ZoneType.FLAME_WALL, 3.0, effect.durationTicks(), effect.value());
                case "mag_pyr_ash_step" -> { dash(player, effect.value()); addZone(player, MageState.ZoneType.FLAME_WALL, player.getPos(), 1.8, 60, 1.2f); }

                case "mag_cryo_ice_shard" -> directBolt(player, stats, 18, effect.value(), MageState.School.ICE);
                case "mag_cryo_ice_barrier" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, effect.durationTicks(), 0, false, false, true));
                case "mag_cryo_frost_nova" -> { pulseAround(player, stats, effect.value(), 5.0, MageState.School.ICE, 0f); applyFrostAround(player, 5.0, 25f); }

                case "mag_storm_arc_bolt" -> chainLightning(player, stats, effect.value(), state.stormAvatarTicks > 0 ? 3 : 2);
                case "mag_storm_lightning_step" -> { dash(player, effect.value()); state.accelerationTicks = Math.max(state.accelerationTicks, effect.durationTicks()); }
                case "mag_storm_static_field" -> addZone(player, MageState.ZoneType.STATIC_FIELD, aimPoint(player, 10), 4.0, effect.durationTicks(), effect.value());
                case "mag_storm_asc_avatar" -> state.stormAvatarTicks = effect.durationTicks();

                case "mag_rune_inscribe" -> inscribeRune(player, stats);
                case "mag_rune_asc_grand_circle" -> addZone(player, MageState.ZoneType.RUNE_CIRCLE, player.getPos(), 8.0, effect.durationTicks(), effect.value());

                case "mag_illu_mirror_image" -> { state.illusionCharges = Math.max(state.illusionCharges, 2); state.illusionTicks = effect.durationTicks(); }
                case "mag_illu_illusory_step" -> {
                    blink(player, 5.0);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, effect.durationTicks(), 0, false, false, true));
                    if (stats.hasNode("mag_illu_ghost_attack")) state.ghostAttackTicks = 80;
                }
                case "mag_illu_confusion" -> confuseTarget(player, effect.durationTicks());
                case "mag_illu_asc_mirror_hall" -> { state.illusionCharges = 5; state.illusionTicks = effect.durationTicks(); state.mirrorHallTicks = effect.durationTicks(); state.mirrorCastCounter = 0; }

                case "mag_tele_blink" -> { blink(player, effect.value()); state.teleportBonusTicks = 80; }
                case "mag_tele_repulsion" -> repulsion(player, stats, effect.value());
                case "mag_tele_anchor" -> toggleSpatialAnchor(player, state, effect.durationTicks());
                case "mag_tele_double_portal" -> placePortalPoint(player, state, effect.durationTicks());
                case "mag_tele_singularity" -> singularity(player, stats, effect.value(), 5.0, false);
                case "mag_tele_asc_collapse" -> singularity(player, stats, effect.value(), 8.0, true);

                case "mag_summ_familiar" -> summonVirtual(player, stats, MageState.SummonType.FAMILIAR, effect.durationTicks(), 1);
                case "mag_summ_guardian" -> summonVirtual(player, stats, MageState.SummonType.GUARDIAN, effect.durationTicks(), 2);
                case "mag_summ_assault_order" -> state.summonAssaultTicks = effect.durationTicks();
                case "mag_summ_transfer" -> {
                    if (CompatManager.usesExternalMageMana()) CompatManager.transferNativeSummons(player, stats);
                    else healVirtualSummons(player, stats, effect.value());
                }
                case "mag_summ_great_conjuration" -> summonVirtual(player, stats, MageState.SummonType.GREAT, effect.durationTicks(), 4);
                case "mag_summ_asc_ephemeral_army" -> state.ephemeralArmyTicks = effect.durationTicks();

                case "mag_anim_life_spirit" -> state.spiritLifeTicks = effect.durationTicks();
                case "mag_anim_earth_spirit" -> state.spiritEarthTicks = effect.durationTicks();
                case "mag_anim_hunt_spirit" -> { state.spiritHuntTicks = effect.durationTicks(); markHuntTarget(player, effect.durationTicks()); }
                case "mag_anim_spirit_totem" -> { state.spiritTotemTicks = effect.durationTicks(); addZone(player, MageState.ZoneType.SPIRIT_TOTEM, player.getPos(), 6.0, effect.durationTicks(), 1f); }
                case "mag_anim_spirit_swap" -> rotateSpirit(player, state);
                case "mag_anim_asc_council" -> { state.spiritLifeTicks = effect.durationTicks(); state.spiritEarthTicks = effect.durationTicks(); state.spiritHuntTicks = effect.durationTicks(); }

                case "mag_astral_blade" -> summonVirtual(player, stats, MageState.SummonType.ASTRAL_BLADE, effect.durationTicks(), 1);
                case "mag_astral_shield" -> summonVirtual(player, stats, MageState.SummonType.ASTRAL_SHIELD, effect.durationTicks(), 2);
                case "mag_astral_turret" -> summonVirtual(player, stats, MageState.SummonType.ASTRAL_TURRET, effect.durationTicks(), 2);
                case "mag_astral_formation" -> state.astralFormation = (state.astralFormation + 1) % 3;
                case "mag_astral_asc_arsenal" -> state.astralArsenalTicks = effect.durationTicks();

                case "mag_blood_lance" -> directBolt(player, stats, 16, effect.value(), MageState.School.BLOOD);
                case "mag_blood_transfusion" -> transfusion(player, stats, effect.value());
                case "mag_blood_asc_eclipse" -> state.bloodEclipseTicks = effect.durationTicks();

                case "mag_curse_weakness" -> curseWeakness(player, effect.durationTicks());
                case "mag_curse_fragility" -> curseFragility(player, effect.value(), effect.durationTicks());
                case "mag_curse_ruin" -> curseRuin(player, effect.durationTicks());
                case "mag_curse_spread" -> spreadCurse(player, stats);
                case "mag_curse_asc_great_curse" -> greatCurse(player, stats, effect.durationTicks());

                case "mag_hex_blink_strike" -> { blinkTowardTarget(player, 8.0); state.blinkStrikeTicks = effect.durationTicks(); }
                case "mag_hex_mystic_parry" -> state.parryTicks = effect.durationTicks();
                case "mag_hex_elemental_imbue" -> state.hexElement = (state.hexElement + 1) % 3;
                case "mag_hex_dimensional_cut" -> lineCut(player, stats, effect.value());
                case "mag_hex_asc_arcane_form" -> state.arcaneFormTicks = effect.durationTicks();

                case "mag_acc_acceleration" -> state.accelerationTicks = effect.durationTicks();
                case "mag_acc_instant_cast" -> state.instantCastTicks = effect.durationTicks();
                case "mag_acc_temporal_step" -> dash(player, effect.value());
                case "mag_acc_overclock" -> {
                    String reduced = CombatState.get(player.getUuid()).reduceLongestCooldown(effect.value());
                    if (reduced.isBlank()) player.sendMessage(Text.literal("§7Nenhum cooldown para acelerar."), true);
                }
                case "mag_acc_asc_distorted_time" -> state.distortedTimeTicks = effect.durationTicks();

                case "mag_stag_temporal_slow" -> slowTarget(player, effect.durationTicks(), 0);
                case "mag_stag_stasis_bubble" -> addZone(player, MageState.ZoneType.STASIS, aimPoint(player, 10), 5.0, effect.durationTicks(), effect.value());
                case "mag_stag_temporal_anchor" -> slowTarget(player, effect.durationTicks(), 1);
                case "mag_stag_asc_time_stop" -> addZone(player, MageState.ZoneType.TIME_STOP, player.getPos(), 8.0, effect.durationTicks(), effect.value());

                case "mag_rev_temporal_mark" -> saveTemporalMark(player, stats, state, effect.durationTicks());
                case "mag_rev_rewind" -> rewind(player, stats, state);
                case "mag_rev_temporal_echo" -> { state.temporalEchoReady = true; state.temporalEchoTicks = effect.durationTicks(); }
                case "mag_rev_asc_rewrite" -> saveRewrite(player, stats, state, effect.durationTicks());
                default -> { }
            }
        } finally {
            recordCastComplete(player, stats, state, id);
            state.endCast();
        }
    }

    /** Camada adicional de dano mágico, aplicada depois do magic_power genérico. */
    public static float modifyMagicDamage(ServerPlayerEntity player, LivingEntity target, float amount) {
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return amount;
        MageState state = MageState.get(player.getUuid());
        if (state.internalDamage) return amount;

        float mult = 1f;
        MageState.TargetState targetState = state.target(target.getUuid());

        // Bônus condicionais não entram no AbilityRegistry genérico para não ficarem ativos fora da condição.
        if (stats.hasNode("mag_anim_harmony") && activeSpiritCount(state) >= 2) mult += 0.05f;
        if (stats.hasNode("mag_astral_resonant")) {
            float cap = AbilityRegistry.sumPassive(stats.unlockedNodes, "astral_resonance", false);
            int arsenal = CompatManager.usesExternalMageMana()
                    ? CompatManager.nativeSummonCount(player, stats) + (state.astralGuardTicks > 0 ? 1 : 0)
                    : astralConstructCount(state);
            mult += Math.min(cap, 0.04f * arsenal);
        }
        if (stats.hasNode("mag_hex_spellblade_rhythm") && state.spellbladeTicks > 0) {
            float perStack = AbilityRegistry.sumPassive(stats.unlockedNodes, "spellblade_rhythm", false);
            mult += perStack * Math.min(3, state.spellbladeStacks);
        }

        if (isElementalSchool(state.currentSchool)) {
            mult += Math.min(0.20f, AbilityRegistry.sumPassive(stats.unlockedNodes, "elemental_damage", false));
            if (state.currentSchool == MageState.School.FIRE && targetState.heat >= 50f)
                mult += AbilityRegistry.sumPassive(stats.unlockedNodes, "fire_damage_marked", false);
        }

        // Casa Oculta: poder cresce em patamares, com cap pequeno e explícito.
        if (stats.hasNode("mag_occ_forbidden")) {
            if (stats.corruption >= 75f && stats.hasNode("mag_occ_forbidden_power")) mult += 0.12f;
            else if (stats.corruption >= 50f && stats.hasNode("mag_occ_instability")) mult += 0.08f;
            else if (stats.corruption >= 25f && stats.hasNode("mag_occ_temptation")) mult += 0.04f;
        }

        if (state.weavingTicks > 0 && isOffensiveSchool(state.currentSchool)) {
            mult += 0.06f;
            state.weavingTicks = 0;
        }
        if (state.teleportBonusTicks > 0 && isOffensiveSchool(state.currentSchool)) {
            mult += AbilityRegistry.sumPassive(stats.unlockedNodes, "teleport_spell_bonus", false);
            state.teleportBonusTicks = 0;
        }
        if (state.ghostAttackTicks > 0 && isOffensiveSchool(state.currentSchool)) {
            mult += 0.20f;
            state.ghostAttackTicks = 0;
        }
        if (state.arcaneSeals > 0 && stats.hasNode("mag_arc_finisher") && isOffensiveSchool(state.currentSchool)) {
            mult += 0.12f;
            state.arcaneSeals--;
        }
        if (targetState.fateMarkReady && stats.hasNode("mag_curse_fate_mark")
                && isOffensiveSchool(state.currentSchool)) {
            mult += 0.25f;
            targetState.fateMarkReady = false;
            targetState.fateMarkTicks = 0;
        }
        if (targetState.fragilityTicks > 0) {
            mult += targetState.fragilityAmp;
            // Fragilidade é consumida pela próxima magia direta significativa.
            targetState.fragilityTicks = 0;
            targetState.fragilityAmp = 0f;
        }
        if (targetState.markedByHuntTicks > 0) mult += 0.05f;
        if (isInsideZone(state, target.getPos(), MageState.ZoneType.STASIS)
                && stats.hasNode("mag_stag_entropy")) mult += 0.06f;
        if (state.bloodEclipseTicks > 0) mult += 0.10f;
        if (stats.hasNode("mag_blood_crimson_desperation")
                && player.getHealth() / Math.max(1f, player.getMaxHealth()) < 0.40f) mult += 0.10f;

        // Crítico mágico separado do crítico físico. Cap permanente 20%; efeitos situacionais podem chegar a 30%.
        float crit = Math.min(MageBalance.PERMANENT_SPELL_CRIT_CAP, AbilityRegistry.sumPassive(stats.unlockedNodes, "spell_crit", false));
        if (state.currentSchool == MageState.School.FIRE && targetState.heat >= 60f)
            crit += AbilityRegistry.sumPassive(stats.unlockedNodes, "fire_crit_hot", false);
        crit = Math.min(MageBalance.SITUATIONAL_SPELL_CRIT_CAP, crit);
        state.lastSpellCrit = player.getRandom().nextFloat() < crit;
        if (state.lastSpellCrit) mult *= 1.5f;

        return amount * Math.min(2.25f, Math.max(0f, mult));
    }

    /** Bônus físico específico do Hexblade. Chamado antes do dano ser aplicado. */
    public static float modifyPhysicalDamage(ServerPlayerEntity player, float amount, boolean projectile, boolean magic) {
        if (projectile || magic) return amount;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO || !stats.hasNode("mag_hex_spellblade_rhythm")) return amount;
        MageState state = MageState.get(player.getUuid());
        if (state.spellbladeTicks <= 0 || state.spellbladeStacks <= 0) return amount;
        float perStack = AbilityRegistry.sumPassive(stats.unlockedNodes, "spellblade_rhythm", false);
        return amount * (1f + perStack * Math.min(3, state.spellbladeStacks));
    }

    /** Defesas especiais antes da redução genérica de dano. */
    public static float modifyIncomingDamage(ServerPlayerEntity player, float amount, DamageSource source) {
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return amount;
        MageState state = MageState.get(player.getUuid());
        CombatState combat = CombatState.get(player.getUuid());

        // Maldição da Fraqueza usa redução percentual própria em vez do Weakness vanilla (-4 dano),
        // que seria desproporcional para a escala de Minecraft e para bosses de mods.
        if (source.getAttacker() instanceof LivingEntity attacker) {
            MageState.TargetState cursed = state.targets.get(attacker.getUuid());
            if (cursed != null && cursed.weaknessTicks > 0) amount *= 0.90f;
        }

        // Phoenix e Segunda Chance impedem o golpe fatal, sem stackar entre si no mesmo hit.
        if (amount >= player.getHealth()) {
            if (stats.hasNode("mag_pyr_asc_phoenix") && combat.cooldown("internal_phoenix") <= 0) {
                combat.startCooldown("internal_phoenix", 3600);
                player.setHealth(Math.max(1f, player.getMaxHealth() * 0.30f));
                CompatManager.setMageMana(player, stats,
                        Math.max(CompatManager.mageMana(player, stats), stats.resourceMax * 0.25f));
                StatsManager.saveAndSync(player, stats);
                player.getServerWorld().spawnParticles(ParticleTypes.FLAME, player.getX(), player.getBodyY(0.5), player.getZ(), 45, 0.8, 1.0, 0.8, 0.04);
                return 0f;
            }
            if (stats.hasNode("mag_rev_second_chance") && combat.cooldown("internal_second_chance") <= 0) {
                combat.startCooldown("internal_second_chance", 2400);
                return Math.max(0f, player.getHealth() - 1f);
            }
        }

        if (state.counterspellTicks > 0 && (source.getSource() instanceof ProjectileEntity || source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC))) {
            amount *= (1f - Math.min(0.60f, state.counterspellReduction));
            state.counterspellTicks = 0;
            state.counterspellReduction = 0f;
        }
        if (state.parryTicks > 0 && !(source.getSource() instanceof ProjectileEntity) && !source.isOf(DamageTypes.MAGIC) && !source.isOf(DamageTypes.INDIRECT_MAGIC)) {
            amount *= 0.30f;
            state.parryTicks = 0;
            StatsManager.saveAndSync(player, stats);
        }
        if (state.illusionCharges > 0 && state.illusionTicks > 0 && source.getAttacker() instanceof LivingEntity
                && combat.cooldown("internal_illusion_decoy") <= 0) {
            boolean perfect = stats.hasNode("mag_illu_perfect_decoy");
            boolean dodge = perfect || player.getRandom().nextFloat() < 0.20f;
            if (dodge) {
                state.illusionCharges--;
                combat.startCooldown("internal_illusion_decoy", perfect ? 240 : 40);
                player.getServerWorld().spawnParticles(ParticleTypes.POOF, player.getX(), player.getBodyY(0.5), player.getZ(), 24, 0.6, 0.8, 0.6, 0.05);
                return 0f;
            }
        }
        if (state.iceGuardTicks > 0 && stats.hasNode("mag_cryo_ice_barrier")) amount *= 0.90f;
        if (state.spiritEarthTicks > 0) amount *= 0.92f;
        amount *= CompatManager.nativeIncomingMultiplier(player, stats);
        if (!state.summons.isEmpty() && stats.hasNode("mag_conj_defensive"))
            amount *= (1f - Math.min(0.05f, AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_defense", false)));
        if (state.summons.containsKey(MageState.SummonType.GUARDIAN)) amount *= 0.94f;
        if (state.summons.containsKey(MageState.SummonType.ASTRAL_SHIELD)) amount *= state.astralFormation == 1 ? 0.90f : 0.94f;
        if (state.arcaneFormTicks > 0) amount *= 0.90f;

        if (stats.corruption >= 75f && stats.hasNode("mag_occ_forbidden_power")) amount *= 1.08f;

        // Momento Guardado: consome 1 fragmento apenas quando realmente protege.
        if (stats.hasNode("mag_time_stored_moment") && stats.temporalFragments > 0
                && combat.cooldown("internal_stored_moment") <= 0) {
            stats.temporalFragments--;
            onFragmentSpent(player, stats, state);
            combat.startCooldown("internal_stored_moment", 360);
            amount *= 0.80f;
            StatsManager.saveAndSync(player, stats);
        }
        return amount;
    }

    /** Efeitos do Hexblade em ataques físicos. */
    public static void onPhysicalHit(ServerPlayerEntity player, LivingEntity target, float physicalDamage) {
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return;
        MageState state = MageState.get(player.getUuid());

        if (stats.hasNode("mag_hex_spellblade_rhythm")) {
            if (state.spellbladeTicks <= 0) {
                state.spellbladeStage = 0;
                state.spellbladeStacks = 0;
            }
            if (state.spellbladeStage == 2) {
                state.spellbladeStacks = Math.min(3, state.spellbladeStacks + 1);
                state.spellbladeStage = 1; // o mesmo melee já inicia a próxima sequência
            } else {
                state.spellbladeStage = 1;
            }
            state.spellbladeTicks = 100;
        }

        if (!stats.hasNode("mag_hex_imbue") && state.blinkStrikeTicks <= 0 && state.arcaneFormTicks <= 0) return;

        float bonus = 0f;
        MageState.School school = switch (state.hexElement) {
            case 1 -> MageState.School.ICE;
            case 2 -> MageState.School.LIGHTNING;
            default -> MageState.School.FIRE;
        };

        float currentMana = CompatManager.mageMana(player, stats);
        if (stats.hasNode("mag_hex_imbue") && currentMana >= 2f) {
            CompatManager.setMageMana(player, stats, currentMana - 2f);
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "hex_melee_magic", false);
        }
        if (state.blinkStrikeTicks > 0) { bonus += 4f; state.blinkStrikeTicks = 0; }
        if (state.arcaneFormTicks > 0) bonus += 2f;

        if (bonus > 0f) {
            MageState prev = MageState.get(player.getUuid());
            prev.beginCast("hex_melee_proc", school, false);
            dealMagic(player, stats, target, bonus, school, false);
            prev.endCast();
            StatsManager.saveAndSync(player, stats);
        }
    }

    /** Aplica às spells reais do Iron's as reações e marcas mantidas pelo RPG Stats. */
    public static void onExternalSpellHit(ServerPlayerEntity player, LivingEntity target, float damage,
                                          MageState.School school) {
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO || damage <= 0f) return;
        applySchoolMechanics(player, stats, target, school);
        MageState state = MageState.get(player.getUuid());
        MageState.TargetState targetState = state.target(target.getUuid());
        if ((school == MageState.School.BLOOD || school == MageState.School.OCCULT)
                && AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_curse_weakness") > 0f) {
            targetState.weaknessTicks = Math.max(targetState.weaknessTicks, curseDuration(stats, 120));
        }
        if ((school == MageState.School.BLOOD || school == MageState.School.OCCULT)
                && AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_curse_fragility") > 0f) {
            targetState.fragilityTicks = Math.max(targetState.fragilityTicks, 120);
            targetState.fragilityAmp = Math.max(targetState.fragilityAmp,
                    Math.min(0.08f, AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_curse_fragility")));
        }
        if (school == MageState.School.SUMMON
                && AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_spirit_hunt") > 0f) {
            targetState.markedByHuntTicks = Math.max(targetState.markedByHuntTicks, 120);
        }
        onMagicHitResolved(player, target, damage);
    }

    /** Cura mágica e echoes depois de um hit que o mixin confirmou. */
    public static void onMagicHitResolved(ServerPlayerEntity player, LivingEntity target, float damage) {
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO || damage <= 0f) return;
        MageState state = MageState.get(player.getUuid());
        if (state.internalDamage) return;

        if (stats.hasNode("mag_hex_spellblade_rhythm") && state.spellbladeStage == 1) {
            state.spellbladeStage = 2;
            state.spellbladeTicks = 100;
        }

        float lifesteal = AbilityRegistry.sumPassive(stats.unlockedNodes, "spell_lifesteal");
        if (state.bloodEclipseTicks > 0) lifesteal = Math.max(lifesteal, 0.08f);
        if (lifesteal > 0f) {
            float efficiency = BossScaler.getTier(target) > 0 ? 0.50f : 1f;
            player.heal(Math.min(4f, damage * Math.min(MageBalance.SPELL_LIFESTEAL_CAP, lifesteal) * efficiency));
        }
    }

    /** Reações que dependem de um hit realmente aceito pelo LivingEntity.damage. */
    public static void onPlayerHurtResolved(ServerPlayerEntity player, float damage) {
        if (damage <= 0f) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return;
        MageState state = MageState.get(player.getUuid());
        CombatState combat = CombatState.get(player.getUuid());


        if (damage >= 6f && stats.corruption > 0f && stats.hasNode("mag_occ_backlash")
                && combat.cooldown("internal_occult_backlash") <= 0) {
            stats.corruption = Math.max(0f, stats.corruption - 10f);
            combat.startCooldown("internal_occult_backlash", 80);
        }
        StatsManager.saveAndSync(player, stats);
    }

    /** Shatter e outros efeitos de morte que precisam do estado do alvo antes da limpeza por tick. */
    public static void onPlayerKill(ServerPlayerEntity player, LivingEntity victim) {
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return;
        MageState state = MageState.get(player.getUuid());
        MageState.TargetState target = state.targets.get(victim.getUuid());
        if (target != null && target.frozenTicks > 0 && stats.hasNode("mag_cryo_shatter")) {
            float damage = AbilityRegistry.sumPassive(stats.unlockedNodes, "shatter_damage", false);
            if (damage <= 0f) damage = 3f;
            explodeAroundTarget(player, stats, victim, damage, 3.0, MageState.School.ICE);
        }
        state.targets.remove(victim.getUuid());
    }

    public static void tick(ServerPlayerEntity player) {
        PlayerStats stats = StatsManager.get(player);
        MageState state = MageState.get(player.getUuid());
        state.tickCounter++;
        state.idleTicks++;
        boolean rewriteEnding = state.rewriteTicks == 1 && state.rewritePos != null;

        dec(state);
        if (rewriteEnding) finishRewriteIfNeeded(player, stats, state);
        tickTargetStates(player, stats, state);
        tickZones(player, stats, state);
        tickRunes(player, stats, state);
        tickVirtualSummons(player, stats, state);
        tickSpirits(player, stats, state);
        tickDelayedTemporalEffects(player, stats, state);
        if (stats.hasNode("mag_stag_slow_horizon") && state.tickCounter % 10 == 0) {
            slowHostileProjectiles(player, player.getBoundingBox().expand(6.0), 0.88);
        }
        applyTransientAttributes(player, state);

        boolean sync = state.statsDirty;
        state.statsDirty = false;
        if (stats.clazz == RPGClass.MAGO && state.tickCounter % 20 == 0) {
            state.summonManaThisSecond = 0f;
            if (state.idleTicks > 120 && stats.corruption > 0f) {
                stats.corruption = Math.max(0f, stats.corruption - 1f);
                sync = true;
            }
            if (state.idleTicks > 240 && stats.temporalFragments > 0 && !stats.hasNode("mag_time_continuity")) {
                stats.temporalFragments--;
                sync = true;
            }
        }
        if (sync) StatsManager.saveAndSync(player, stats);
    }

    /** Ajustes na regen principal do Mago; CombatHandler chama isso a cada tick. */
    public static float mageRegenPerTick(PlayerStats stats, ServerPlayerEntity player) {
        float perSecond = 3.0f + AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_regen_flat", false);
        float pct = AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_regen_pct", false);
        MageState state = MageState.get(player.getUuid());
        if (stats.hasNode("mag_blood_crimson_desperation")
                && player.getHealth() / Math.max(1f, player.getMaxHealth()) < 0.40f) pct -= 0.10f;
        if (stats.hasNode("mag_anim_harmony") && activeSpiritCount(state) >= 2) pct += 0.08f;
        if (stats.temporalFragments >= 3 && stats.hasNode("mag_acc_fast_recovery")) pct += 0.20f;
        if (state.temporalExhaustionTicks > 0) pct -= 0.20f;
        pct = Math.max(-0.50f, Math.min(0.75f, pct));
        return (perSecond * (1f + pct)) / 20f;
    }

    private static void dec(MageState s) {
        if (s.spellWindowTicks > 0 && --s.spellWindowTicks == 0) s.spellsInWindow = 0;
        if (s.temporalRhythmCooldown > 0) s.temporalRhythmCooldown--;
        if (s.manaSpentWindowTicks > 0 && --s.manaSpentWindowTicks == 0) s.manaSpentWindow = 0f;
        if (s.counterspellTicks > 0) s.counterspellTicks--;
        if (s.illusionTicks > 0 && --s.illusionTicks == 0) s.illusionCharges = 0;
        if (s.mirrorHallTicks > 0) s.mirrorHallTicks--;
        if (s.ghostAttackTicks > 0) s.ghostAttackTicks--;
        if (s.teleportBonusTicks > 0) s.teleportBonusTicks--;
        if (s.weavingTicks > 0) s.weavingTicks--;
        if (s.parryTicks > 0) s.parryTicks--;
        if (s.blinkStrikeTicks > 0) s.blinkStrikeTicks--;
        if (s.arcaneFormTicks > 0) s.arcaneFormTicks--;
        if (s.accelerationTicks > 0) s.accelerationTicks--;
        if (s.distortedTimeTicks > 0) s.distortedTimeTicks--;
        if (s.winterHeartTicks > 0) s.winterHeartTicks--;
        if (s.iceGuardTicks > 0) s.iceGuardTicks--;
        if (s.fireStepTicks > 0) s.fireStepTicks--;
        if (s.lightningStepTicks > 0) s.lightningStepTicks--;
        if (s.astralGuardTicks > 0) s.astralGuardTicks--;
        if (s.stormAvatarTicks > 0) s.stormAvatarTicks--;
        if (s.bloodEclipseTicks > 0) s.bloodEclipseTicks--;
        if (s.instantCastTicks > 0) s.instantCastTicks--;
        if (s.temporalEchoTicks > 0 && --s.temporalEchoTicks == 0) s.temporalEchoReady = false;
        if (s.anchorTicks > 0 && --s.anchorTicks == 0) s.anchorPos = null;
        if (s.portalTicks > 0 && --s.portalTicks == 0) { s.portalA = null; s.portalB = null; }
        if (s.temporalMarkTicks > 0 && --s.temporalMarkTicks == 0) s.temporalMarkPos = null;
        if (s.rewriteTicks > 0) s.rewriteTicks--;
        if (s.spiritLifeTicks > 0) s.spiritLifeTicks--;
        if (s.spiritEarthTicks > 0) s.spiritEarthTicks--;
        if (s.spiritHuntTicks > 0) s.spiritHuntTicks--;
        if (s.spiritTotemTicks > 0) s.spiritTotemTicks--;
        if (s.summonAssaultTicks > 0) s.summonAssaultTicks--;
        if (s.summonFocusTicks > 0 && --s.summonFocusTicks == 0) s.summonFocusTarget = null;
        if (s.ephemeralArmyTicks > 0) s.ephemeralArmyTicks--;
        if (s.astralArsenalTicks > 0) s.astralArsenalTicks--;
        if (s.spellbladeTicks > 0 && --s.spellbladeTicks == 0) { s.spellbladeStage = 0; s.spellbladeStacks = 0; }
        if (s.momentumTicks > 0 && --s.momentumTicks == 0) s.momentumStacks = 0;
        if (s.temporalEchoDelay > 0) s.temporalEchoDelay--;
        if (s.temporalDebtTicks > 0) s.temporalDebtTicks--;
        if (s.temporalExhaustionTicks > 0) s.temporalExhaustionTicks--;
    }

    private static void tickTargetStates(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        // Damage/death callbacks can remove, add or replace target states during this tick.
        // Copy entries and preserve their state identities; newly applied states wait until next tick.
        for (Map.Entry<UUID, MageState.TargetState> entry : new HashMap<>(state.targets).entrySet()) {
            MageState.TargetState t = entry.getValue();
            if (state.targets.get(entry.getKey()) != t) continue;
            Entity raw = player.getServerWorld().getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity target) || !target.isAlive()) {
                state.targets.remove(entry.getKey(), t);
                continue;
            }
            if (t.fragilityTicks > 0) t.fragilityTicks--;
            if (t.weaknessTicks > 0) t.weaknessTicks--;
            if (t.frozenTicks > 0) t.frozenTicks--;
            if (t.reactionCooldown > 0) t.reactionCooldown--;
            if (t.timeLockCooldown > 0) t.timeLockCooldown--;
            if (t.fateMarkTicks > 0 && --t.fateMarkTicks == 0) t.fateMarkReady = false;
            if (t.markedByHuntTicks > 0) t.markedByHuntTicks--;

            if (t.ruinTicks > 0) {
                t.ruinTicks--;
                if (++t.ruinPulse >= 20) {
                    t.ruinPulse = 0;
                    periodicDamage(player, stats, target, 1.5f, MageState.School.OCCULT);
                }
            }
            if (!target.isAlive() || state.targets.get(entry.getKey()) != t) {
                state.targets.remove(entry.getKey(), t);
                continue;
            }
            if (t.bleedTicks > 0) {
                t.bleedTicks--;
                if (++t.bleedPulse >= 40) {
                    t.bleedPulse = 0;
                    float bleed = Math.min(3f, Math.max(1, t.bleedStacks));
                    periodicDamage(player, stats, target, bleed, MageState.School.BLOOD);
                }
            } else {
                t.bleedStacks = 0;
            }
            if (!target.isAlive() || state.targets.get(entry.getKey()) != t) {
                state.targets.remove(entry.getKey(), t);
                continue;
            }

            if (state.tickCounter % 20 == 0) {
                t.heat = Math.max(0f, t.heat - 5f);
                t.frost = Math.max(0f, t.frost - 5f);
                if (t.charge > 0 && state.tickCounter % 60 == 0) t.charge--;
            }
            if (t.heat <= 0f && t.frost <= 0f && t.charge <= 0 && t.fragilityTicks <= 0
                    && t.weaknessTicks <= 0 && t.ruinTicks <= 0 && t.bleedTicks <= 0 && t.frozenTicks <= 0
                    && t.reactionCooldown <= 0 && t.timeLockCooldown <= 0 && t.fateMarkTicks <= 0
                    && t.markedByHuntTicks <= 0) state.targets.remove(entry.getKey(), t);
        }
    }

    private static void tickZones(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        Iterator<MageState.Zone> it = state.zones.iterator();
        while (it.hasNext()) {
            MageState.Zone z = it.next();
            if (--z.ticks <= 0) { it.remove(); continue; }
            if (state.tickCounter % 10 != 0) continue;

            ServerWorld world = player.getServerWorld();
            Box box = new Box(z.center, z.center).expand(z.radius);
            switch (z.type) {
                case FLAME_WALL -> {
                    world.spawnParticles(ParticleTypes.FLAME, z.center.x, z.center.y + 0.2, z.center.z, 8, z.radius * 0.6, 0.15, z.radius * 0.6, 0.01);
                    if (state.tickCounter % 20 == 0) for (LivingEntity target : hostiles(world, box, player))
                        periodicDamage(player, stats, target, z.power, MageState.School.FIRE);
                }
                case STATIC_FIELD -> {
                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, z.center.x, z.center.y + 0.4, z.center.z, 8, z.radius * 0.5, 0.4, z.radius * 0.5, 0.02);
                    for (LivingEntity target : hostiles(world, box, player)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 25, 0, false, false, true));
                        if (state.tickCounter % 20 == 0) periodicDamage(player, stats, target, z.power, MageState.School.LIGHTNING);
                    }
                }
                case STASIS -> {
                    world.spawnParticles(ParticleTypes.PORTAL, z.center.x, z.center.y + 0.5, z.center.z, 6, z.radius * 0.45, 0.5, z.radius * 0.45, 0.01);
                    for (LivingEntity target : hostiles(world, box, player)) {
                        boolean alreadySlowed = target.hasStatusEffect(StatusEffects.SLOWNESS);
                        int amp = BossScaler.getTier(target) > 0 ? 0 : 1;
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 25, amp, false, false, true));
                        if (alreadySlowed) maybeTimeLock(player, stats, state, target);
                    }
                    slowHostileProjectiles(player, box, 0.70);
                }
                case TIME_STOP -> {
                    world.spawnParticles(ParticleTypes.REVERSE_PORTAL, z.center.x, z.center.y + 0.5, z.center.z, 10, z.radius * 0.5, 0.5, z.radius * 0.5, 0.01);
                    for (LivingEntity target : hostiles(world, box, player)) {
                        int amp = BossScaler.getTier(target) > 0 ? 1 : 4;
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 25, amp, false, false, true));
                    }
                    slowHostileProjectiles(player, box, 0.25);
                }
                case RUNE_CIRCLE -> world.spawnParticles(ParticleTypes.ENCHANT, z.center.x, z.center.y + 0.1, z.center.z, 8, z.radius * 0.5, 0.1, z.radius * 0.5, 0.01);
                case SPIRIT_TOTEM -> {
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, z.center.x, z.center.y + 0.5, z.center.z, 4, z.radius * 0.35, 0.5, z.radius * 0.35, 0.01);
                    if (state.tickCounter % 40 == 0) healAllies(player, z.center, z.radius, 1f);
                }
            }
        }
    }

    private static void tickRunes(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        Iterator<MageState.Rune> it = state.runes.iterator();
        List<MageState.Rune> chain = new ArrayList<>();
        while (it.hasNext()) {
            MageState.Rune rune = it.next();
            if (--rune.ticks <= 0 || rune.triggered) { it.remove(); continue; }
            ServerWorld world = player.getServerWorld();
            switch (rune.type) {
                case ARCANE -> {
                    world.spawnParticles(ParticleTypes.ENCHANT, rune.pos.x, rune.pos.y + 0.15, rune.pos.z, 2, 0.15, 0.05, 0.15, 0.01);
                    LivingEntity target = nearestHostile(player, rune.pos, 2.0);
                    if (target != null) {
                        periodicDamage(player, stats, target, 3f * runeAmplifier(state, rune.pos), MageState.School.ARCANE);
                        rune.triggered = true;
                    }
                }
                case EXPLOSIVE -> {
                    world.spawnParticles(ParticleTypes.ENCHANTED_HIT, rune.pos.x, rune.pos.y + 0.15, rune.pos.z, 2, 0.15, 0.05, 0.15, 0.01);
                    LivingEntity target = nearestHostile(player, rune.pos, 2.2);
                    if (target != null) {
                        float amp = runeAmplifier(state, rune.pos);
                        periodicDamage(player, stats, target, 6f * amp, MageState.School.ARCANE);
                        rune.triggered = true;
                        if (stats.hasNode("mag_rune_network")) {
                            for (MageState.Rune other : state.runes) {
                                if (other != rune && !other.triggered && other.type == MageState.RuneType.EXPLOSIVE
                                        && other.pos.squaredDistanceTo(rune.pos) <= 36.0) chain.add(other);
                            }
                        }
                    }
                }
                case PROTECTION -> {
                    world.spawnParticles(ParticleTypes.WAX_ON, rune.pos.x, rune.pos.y + 0.15, rune.pos.z, 2, 0.2, 0.05, 0.2, 0.01);
                    if (state.tickCounter % 40 == 0) {
                        for (ServerPlayerEntity ally : allies(player, rune.pos, 3.0))
                            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 60, 0, false, false, true));
                    }
                }
                case AMPLIFIER -> world.spawnParticles(ParticleTypes.ENCHANT, rune.pos.x, rune.pos.y + 0.15, rune.pos.z, 2, 0.15, 0.05, 0.15, 0.01);
            }
        }
        for (MageState.Rune rune : chain) {
            LivingEntity target = nearestHostile(player, rune.pos, 2.5);
            if (target != null) periodicDamage(player, stats, target, 4.2f * runeAmplifier(state, rune.pos), MageState.School.ARCANE);
            rune.triggered = true;
        }
    }

    private static void tickVirtualSummons(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        Iterator<Map.Entry<MageState.SummonType, Integer>> it = state.summons.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MageState.SummonType, Integer> e = it.next();
            int next = e.getValue() - 1;
            if (next <= 0) {
                refundSummon(player, stats, e.getKey());
                it.remove();
            } else e.setValue(next);
        }

        if (state.tickCounter % 20 != 0) return;
        float summonMult = 1f + Math.min(0.30f, AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_damage", false));
        if (stats.hasNode("mag_summ_pack_tactics")) {
            int distinct = conjurerSummonTypeCount(state);
            float diversityCap = AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_diversity_damage", false);
            if (distinct >= 2) summonMult += distinct >= 3 ? diversityCap : diversityCap * 0.5f;
        }
        if (state.summonAssaultTicks > 0) summonMult += 0.12f;
        if (state.ephemeralArmyTicks > 0) summonMult += 0.15f;
        if (state.astralFormation == 0) summonMult += 0.05f;

        if (state.summons.containsKey(MageState.SummonType.FAMILIAR)) {
            LivingEntity t = summonTarget(player, state, 12);
            if (t != null) summonDamage(player, stats, state, t, 2.0f * summonMult);
        }
        if (state.summons.containsKey(MageState.SummonType.GREAT)) {
            LivingEntity t = summonTarget(player, state, 10);
            if (t != null) {
                summonDamage(player, stats, state, t, 4.0f * summonMult);
                for (LivingEntity other : hostiles(player.getServerWorld(), t.getBoundingBox().expand(2.5), player)) {
                    if (other != t) summonDamage(player, stats, state, other, 1.5f * summonMult);
                }
            }
        }
        if (state.summons.containsKey(MageState.SummonType.ASTRAL_BLADE)) {
            LivingEntity t = summonTarget(player, state, 5);
            if (t != null) summonDamage(player, stats, state, t, 2.4f * summonMult);
        }
        if (state.summons.containsKey(MageState.SummonType.ASTRAL_TURRET)) {
            LivingEntity t = summonTarget(player, state, 15);
            if (t != null) summonDamage(player, stats, state, t, 2.1f * summonMult);
        }
        if (state.astralArsenalTicks > 0 && state.tickCounter % 10 == 0) {
            LivingEntity t = summonTarget(player, state, 14);
            if (t != null) summonDamage(player, stats, state, t, 1.3f);
        }

        if (state.summons.containsKey(MageState.SummonType.GUARDIAN) && state.tickCounter % 40 == 0) {
            for (HostileEntity mob : player.getServerWorld().getEntitiesByClass(HostileEntity.class,
                    player.getBoundingBox().expand(8), Entity::isAlive)) mob.setTarget(player);
        }
    }

    private static void tickSpirits(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        if (state.tickCounter % 80 == 0 && state.spiritLifeTicks > 0) {
            player.heal(1f);
            for (ServerPlayerEntity ally : allies(player, player.getPos(), 6.0)) if (ally != player) ally.heal(1f);
        }
        if (state.spiritHuntTicks > 0 && state.tickCounter % 40 == 0) markHuntTarget(player, 60);
    }

    private static void applyTransientAttributes(ServerPlayerEntity player, MageState state) {
        double move = 0d;
        double attack = 0d;
        if (state.accelerationTicks > 0) move += 0.15;
        if (state.distortedTimeTicks > 0) move += 0.15;
        if (state.stormAvatarTicks > 0) move += 0.10;
        if (state.astralFormation == 2 && (CompatManager.usesExternalMageMana()
                ? state.astralGuardTicks > 0 || CompatManager.nativeSummonCount(player, StatsManager.get(player)) > 0
                : astralConstructCount(state) > 0)) move += 0.08;
        if (state.arcaneFormTicks > 0) attack += 0.15;
        applyTemp(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, TEMP_MOVE, move);
        applyTemp(player, EntityAttributes.GENERIC_ATTACK_SPEED, TEMP_ATTACK, attack);
    }

    private static void applyTemp(ServerPlayerEntity player, EntityAttribute attribute, UUID id, double amount) {
        EntityAttributeInstance inst = player.getAttributeInstance(attribute);
        if (inst == null) return;
        if (inst.getModifier(id) != null) inst.removeModifier(id);
        if (amount != 0d) inst.addTemporaryModifier(new EntityAttributeModifier(id, "RPGStats Mage temp", amount,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void recordCastStart(PlayerStats stats, MageState state, String nodeId) {
        int gap = state.idleTicks;
        if (stats.hasNode("mag_acc_momentum")) {
            state.momentumStacks = gap <= 60 ? Math.min(3, state.momentumStacks + 1) : 1;
            state.momentumTicks = 100;
        }
        state.idleTicks = 0;
        state.spellsInWindow++;
        state.spellWindowTicks = 100;
        if (stats.hasNode("mag_time_rhythm") && state.spellsInWindow >= 3 && state.temporalRhythmCooldown <= 0) {
            int max = stats.temporalFragmentMax();
            stats.temporalFragments = Math.min(max, stats.temporalFragments + 1);
            state.spellsInWindow = 0;
            state.temporalRhythmCooldown = 60;
        }
        if (MOBILITY_NODES.contains(nodeId) && stats.hasNode("mag_core_weaving")) state.weavingTicks = 80;

        if (stats.hasNode("mag_core_flow")) {
            state.flowRecent.add(nodeId);
            if (state.flowRecent.size() >= 3) {
                state.flowReady = true;
                state.flowRecent.clear();
            }
        }
    }

    private static void recordCastComplete(ServerPlayerEntity player, PlayerStats stats, MageState state, String nodeId) {
        if (isOccultNode(nodeId) && !nodeId.equals("mag_occ_purification")) {
            float gain = 4f;
            stats.corruption = Math.min(100f, stats.corruption + gain);
        }

        if (stats.hasNode("mag_arc_sequence")) {
            String category = category(nodeId);
            if (!state.recentArcaneCategories.contains(category)) state.recentArcaneCategories.addLast(category);
            while (state.recentArcaneCategories.size() > 3) state.recentArcaneCategories.removeFirst();
            if (stats.hasNode("mag_arc_triad") && state.recentArcaneCategories.size() == 3) {
                state.arcaneSeals = Math.min(2, state.arcaneSeals + 1);
                if (stats.hasNode("mag_arc_perfect_cycle")) CompatManager.addMageMana(player, stats, 4f);
                state.recentArcaneCategories.clear();
            }
        }
        if (state.mirrorHallTicks > 0 && isOffensiveMageActive(nodeId)) state.mirrorCastCounter = (state.mirrorCastCounter + 1) % 3;
        state.lastSpellNode = nodeId;
    }

    private static boolean dealMagic(ServerPlayerEntity player, PlayerStats stats, LivingEntity target, float base,
                                     MageState.School school, boolean concentration) {
        if (target == null || !target.isAlive() || !isOffensiveTarget(player, target)) return false;
        MageState state = MageState.get(player.getUuid());
        MageState.School oldSchool = state.currentSchool;
        state.currentSchool = school;
        boolean damaged = target.damage(player.getDamageSources().indirectMagic(player, player), Math.max(0f, base));
        if (damaged) {
            applySchoolMechanics(player, stats, target, school);

            // Eco Arcano: proc não gera novo proc/concentração.
            if (!state.internalDamage && stats.hasNode("mag_core_echo")
                    && CombatState.get(player.getUuid()).cooldown("internal_arcane_echo") <= 0
                    && player.getRandom().nextFloat() < 0.08f) {
                CombatState.get(player.getUuid()).startCooldown("internal_arcane_echo", 160);
                ProcDamageQueue.queueSameTarget(player, target, base * 0.35f);
            }
            // Salão dos Espelhos: a cada terceira magia ofensiva, 25% extra.
            if (!state.internalDamage && state.mirrorHallTicks > 0 && state.mirrorCastCounter == 2) {
                ProcDamageQueue.queueSameTarget(player, target, base * 0.25f);
            }
            if (!state.internalDamage && stats.hasNode("mag_illu_arcane_reflection")
                    && state.illusionTicks > 0
                    && CombatState.get(player.getUuid()).cooldown("internal_illusion_reflection") <= 0
                    && player.getRandom().nextFloat() < AbilityRegistry.sumPassive(stats.unlockedNodes, "illusion_echo", false)) {
                CombatState.get(player.getUuid()).startCooldown("internal_illusion_reflection", 120);
                ProcDamageQueue.queueSameTarget(player, target, base * 0.20f);
            }
            if (!state.internalDamage && state.temporalEchoReady && isOffensiveSchool(school)) {
                state.temporalEchoReady = false;
                state.temporalEchoTicks = 0;
                state.temporalEchoTarget = target.getUuid();
                state.temporalEchoDamage = base * 0.40f;
                state.temporalEchoSchool = school;
                state.temporalEchoDelay = 20;
            }
        }
        state.currentSchool = oldSchool;
        return damaged;
    }

    private static void periodicDamage(ServerPlayerEntity player, PlayerStats stats, LivingEntity target, float base, MageState.School school) {
        MageState state = MageState.get(player.getUuid());
        boolean old = state.internalDamage;
        state.internalDamage = true;
        MageState.School oldSchool = state.currentSchool;
        state.currentSchool = school;
        target.damage(player.getDamageSources().indirectMagic(player, player), Math.max(0f, base));
        state.currentSchool = oldSchool;
        state.internalDamage = old;
    }

    private static void applySchoolMechanics(ServerPlayerEntity player, PlayerStats stats, LivingEntity target, MageState.School school) {
        MageState state = MageState.get(player.getUuid());
        MageState.TargetState t = state.target(target.getUuid());

        if (school == MageState.School.FIRE) {
            if (stats.hasNode("mag_ele_resonance")) {
                elementalReaction(player, stats, target, t, true, false, false);
                float heat = 10f + AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_fire_heat");
                if (isExternalSpell(state, "wall_of_fire", "blaze_storm", "raise_hell"))
                    heat += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_fire_area_heat");
                t.heat = Math.min(100f, t.heat + heat);
            }
            if (stats.hasNode("mag_pyr_combustion") && t.heat >= 100f) {
                String cd = "internal_combustion_" + target.getUuid();
                int gate = BossScaler.getTier(target) > 0 ? 80 : 20;
                if (CombatState.get(player.getUuid()).cooldown(cd) <= 0) {
                    CombatState.get(player.getUuid()).startCooldown(cd, gate);
                    t.heat = 0f;
                    explodeAroundTarget(player, stats, target, 6f, 3.0, MageState.School.FIRE);
                }
            }
        } else if (school == MageState.School.ICE) {
            if (stats.hasNode("mag_ele_resonance")) {
                elementalReaction(player, stats, target, t, false, true, false);
                float frost = 15f + AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_ice_chill");
                if (isExternalSpell(state, "frostwave", "cone_of_cold", "blizzard"))
                    frost += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_ice_area_chill");
                if (state.winterHeartTicks > 0) frost *= 1.25f;
                t.frost = Math.min(100f, t.frost + frost);
            }
            MageFrostCycle.Result frostCycle = MageFrostCycle.resolve(t.frost,
                    stats.hasNode("mag_cryo_fragility"), stats.hasNode("mag_cryo_deep_freeze"),
                    BossScaler.getTier(target) > 0);
            if (frostCycle.triggered()) {
                t.frost = 0f;
                if (frostCycle.fragility()) {
                    t.fragilityTicks = Math.max(t.fragilityTicks, 60);
                    t.fragilityAmp = Math.max(t.fragilityAmp, 0.08f);
                }
                if (frostCycle.slowTicks() > 0)
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                            frostCycle.slowTicks(), frostCycle.slowAmplifier(), false, false, true));
                t.frozenTicks = Math.max(t.frozenTicks, frostCycle.freezeTicks());
            }
        } else if (school == MageState.School.LIGHTNING) {
            if (stats.hasNode("mag_ele_resonance")) elementalReaction(player, stats, target, t, false, false, true);
            if (stats.hasNode("mag_storm_static_charge") || stats.hasNode("mag_storm_arc_bolt")) {
                if (t.charge >= 4 && stats.hasNode("mag_storm_overload")) {
                    t.charge = 0;
                    ProcDamageQueue.queueSameTarget(player, target,
                            BossScaler.getTier(target) > 0 ? 3f : 5f);
                } else t.charge = Math.min(stats.hasNode("mag_storm_static_charge") ? 4 : 1, t.charge + 1);
            }
        } else if (school == MageState.School.BLOOD && (stats.hasNode("mag_blood_hemorrhage")
                || AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_blood_hemorrhage", false) > 0f)) {
            // Refresh duration without postponing the next pulse during a channel/volley.
            if (t.bleedTicks <= 0) t.bleedPulse = 0;
            int maxStacks = stats.hasNode("mag_blood_hemorrhage") ? 3 : 1;
            t.bleedStacks = Math.min(maxStacks, t.bleedStacks + 1);
            t.bleedTicks = 120;
        }
    }

    private static void elementalReaction(ServerPlayerEntity player, PlayerStats stats, LivingEntity target,
                                          MageState.TargetState t, boolean fire, boolean ice, boolean lightning) {
        if (!stats.hasNode("mag_ele_opposites") || t.reactionCooldown > 0) return;
        boolean reaction = (fire && (t.frost > 0 || t.charge > 0))
                || (ice && (t.heat > 0 || t.charge > 0))
                || (lightning && (t.heat > 0 || t.frost > 0));
        if (!reaction) return;
        t.reactionCooldown = 40;
        float mult = 1f + AbilityRegistry.sumPassive(stats.unlockedNodes, "reaction_damage", false)
                + AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_elemental_burst");
        ProcDamageQueue.queueSameTarget(player, target, 2f * mult);
        if (fire) t.frost = Math.max(0f, t.frost - 20f);
        if (ice) t.heat = Math.max(0f, t.heat - 20f);
        if (lightning) { t.heat = Math.max(0f, t.heat - 10f); t.frost = Math.max(0f, t.frost - 10f); }
        if (stats.hasNode("mag_ele_perfect_reaction")) CompatManager.addMageMana(player, stats, 3f);
        if (stats.hasNode("mag_ele_instability"))
            explodeAroundTarget(player, stats, target, 2.5f, 2.5, MageState.School.ARCANE);
    }

    private static void directBolt(ServerPlayerEntity player, PlayerStats stats, double range, float damage, MageState.School school) {
        LivingEntity target = rayTarget(player, range);
        if (target == null) {
            player.sendMessage(Text.literal("§7Nenhum alvo na mira."), true);
            return;
        }
        dealMagic(player, stats, target, damage, school, true);
        particleLine(player.getServerWorld(), player.getEyePos(), target.getEyePos(), particleFor(school));
    }

    private static void chainLightning(ServerPlayerEntity player, PlayerStats stats, float damage, int extraJumps) {
        LivingEntity first = rayTarget(player, 18);
        if (first == null) { player.sendMessage(Text.literal("§7Nenhum alvo na mira."), true); return; }
        List<LivingEntity> hit = new ArrayList<>();
        LivingEntity current = first;
        float currentDamage = damage;
        for (int jump = 0; jump <= extraJumps && current != null; jump++) {
            hit.add(current);
            dealMagic(player, stats, current, currentDamage, MageState.School.LIGHTNING, true);
            particleLine(player.getServerWorld(), jump == 0 ? player.getEyePos() : hit.get(hit.size()-2).getEyePos(), current.getEyePos(), ParticleTypes.ELECTRIC_SPARK);
            currentDamage *= stats.hasNode("mag_storm_conductor") ? 0.70f : 0.60f;
            current = nearestHostileExcluding(player, current.getPos(), 7.0, hit);
        }
    }

    private static void elementalBurst(ServerPlayerEntity player, PlayerStats stats, SkillEffect effect) {
        LivingEntity target = rayTarget(player, 15);
        if (target == null) { pulseAround(player, stats, effect.value() * 0.65f, 4.0, MageState.School.ARCANE, 0f); return; }
        MageState.TargetState t = MageState.get(player.getUuid()).target(target.getUuid());
        MageState.School school = t.heat >= t.frost && t.heat > 0 ? MageState.School.FIRE
                : t.frost > 0 ? MageState.School.ICE
                : t.charge > 0 ? MageState.School.LIGHTNING : MageState.School.ARCANE;
        dealMagic(player, stats, target, effect.value(), school, true);
    }

    private static void pulseAround(ServerPlayerEntity player, PlayerStats stats, float damage, double radius,
                                    MageState.School school, float knockback) {
        for (LivingEntity target : hostiles(player.getServerWorld(), player.getBoundingBox().expand(radius), player)) {
            dealMagic(player, stats, target, damage, school, true);
            if (knockback > 0 && BossScaler.getTier(target) == 0) {
                Vec3d dir = target.getPos().subtract(player.getPos()).normalize().multiply(knockback);
                target.addVelocity(dir.x, 0.15, dir.z);
            }
        }
    }

    private static void explodeAroundTarget(ServerPlayerEntity player, PlayerStats stats, LivingEntity center,
                                            float damage, double radius, MageState.School school) {
        for (LivingEntity target : hostiles(player.getServerWorld(), center.getBoundingBox().expand(radius), player))
            periodicDamage(player, stats, target, damage, school);
    }

    private static void applyFrostAround(ServerPlayerEntity player, double radius, float amount) {
        MageState state = MageState.get(player.getUuid());
        for (LivingEntity target : hostiles(player.getServerWorld(), player.getBoundingBox().expand(radius), player)) {
            MageState.TargetState t = state.target(target.getUuid());
            t.frost = Math.min(100f, t.frost + amount);
        }
    }

    private static void repulsion(ServerPlayerEntity player, PlayerStats stats, float damage) {
        for (LivingEntity target : hostiles(player.getServerWorld(), player.getBoundingBox().expand(4.5), player)) {
            dealMagic(player, stats, target, damage, MageState.School.SPACE, true);
            if (BossScaler.getTier(target) == 0) {
                Vec3d dir = target.getPos().subtract(player.getPos()).normalize().multiply(1.0);
                target.addVelocity(dir.x, 0.25, dir.z);
            }
        }
    }

    private static void singularity(ServerPlayerEntity player, PlayerStats stats, float damage, double radius, boolean ultimate) {
        Vec3d center = aimPoint(player, ultimate ? 16 : 12);
        Box box = new Box(center, center).expand(radius);
        for (LivingEntity target : hostiles(player.getServerWorld(), box, player)) {
            if (BossScaler.getTier(target) == 0) {
                Vec3d pull = center.subtract(target.getPos());
                if (pull.lengthSquared() > 0.01) {
                    pull = pull.normalize().multiply(ultimate ? 0.9 : 0.55);
                    target.addVelocity(pull.x, Math.max(0.05, pull.y * 0.25), pull.z);
                }
            }
            dealMagic(player, stats, target, damage, MageState.School.SPACE, true);
        }
        player.getServerWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.5, center.z,
                ultimate ? 55 : 30, radius * 0.5, 0.8, radius * 0.5, 0.03);
    }

    private static void inscribeRune(ServerPlayerEntity player, PlayerStats stats) {
        MageState state = MageState.get(player.getUuid());
        int limit = 3 + Math.round(AbilityRegistry.sumPassive(stats.unlockedNodes, "rune_limit", false));
        while (state.runes.size() >= limit && !state.runes.isEmpty()) state.runes.remove(0);

        List<MageState.RuneType> available = new ArrayList<>();
        // Inscrever Runa já cria uma runa arcana básica; Runa Explosiva é um upgrade real de dano.
        available.add(stats.hasNode("mag_rune_explosive")
                ? MageState.RuneType.EXPLOSIVE : MageState.RuneType.ARCANE);
        if (stats.hasNode("mag_rune_protection")) available.add(MageState.RuneType.PROTECTION);
        if (stats.hasNode("mag_rune_amplifier")) available.add(MageState.RuneType.AMPLIFIER);
        MageState.RuneType type = available.get(Math.floorMod(state.runeMode, available.size()));
        state.runeMode++;
        Vec3d pos = aimPoint(player, 10);
        state.runes.add(new MageState.Rune(pos, type, 1200));
        player.sendMessage(Text.literal("§dRuna: §f" + switch (type) {
            case ARCANE -> "Arcana"; case EXPLOSIVE -> "Explosiva"; case PROTECTION -> "Proteção"; case AMPLIFIER -> "Amplificadora";
        }), true);
    }

    private static float runeAmplifier(MageState state, Vec3d pos) {
        float mult = 1f;
        for (MageState.Rune r : state.runes) {
            if (r.type == MageState.RuneType.AMPLIFIER && !r.triggered && r.pos.squaredDistanceTo(pos) <= 16.0) {
                mult *= 1.15f;
                break;
            }
        }
        if (isInsideZone(state, pos, MageState.ZoneType.RUNE_CIRCLE)) mult *= 1.25f;
        return Math.min(1.45f, mult);
    }

    private static void confuseTarget(ServerPlayerEntity player, int duration) {
        LivingEntity target = rayTarget(player, 14);
        if (target == null) return;
        if (target instanceof HostileEntity mob && BossScaler.getTier(target) == 0) {
            mob.setTarget(null);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0, false, false, true));
        } else {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, false, false, true));
        }
    }

    private static void toggleSpatialAnchor(ServerPlayerEntity player, MageState state, int duration) {
        if (state.anchorPos != null && state.anchorTicks > 0) {
            teleport(player, state.anchorPos);
            state.anchorPos = null;
            state.anchorTicks = 0;
            state.teleportBonusTicks = 80;
        } else {
            state.anchorPos = player.getPos();
            state.anchorTicks = duration;
            player.sendMessage(Text.literal("§bÂncora espacial marcada."), true);
        }
    }

    private static void placePortalPoint(ServerPlayerEntity player, MageState state, int duration) {
        Vec3d point = aimPoint(player, 12);
        if (state.portalA == null || state.portalTicks <= 0) {
            state.portalA = point;
            state.portalB = null;
            state.portalTicks = duration;
            player.sendMessage(Text.literal("§bPortal A marcado. Use novamente para marcar B."), true);
        } else if (state.portalB == null) {
            state.portalB = point;
            state.portalTicks = duration;
            player.sendMessage(Text.literal("§bPortal B marcado. Use novamente perto de um ponto para atravessar."), true);
        } else {
            double da = player.getPos().squaredDistanceTo(state.portalA);
            double db = player.getPos().squaredDistanceTo(state.portalB);
            if (da <= 9.0) teleport(player, state.portalB);
            else if (db <= 9.0) teleport(player, state.portalA);
            else player.sendMessage(Text.literal("§7Aproxime-se de um dos portais para atravessar."), true);
            state.teleportBonusTicks = 80;
        }
    }

    private static void summonVirtual(ServerPlayerEntity player, PlayerStats stats, MageState.SummonType type,
                                      int duration, int bondCost) {
        MageState state = MageState.get(player.getUuid());
        int max = bondMax(stats, state);
        int current = bondUsed(state);
        if (!state.summons.containsKey(type) && current + bondCost > max) {
            // O custo de Mana já foi pago no pipeline. Devolve 80% para evitar punição pesada por erro de capacidade.
            CompatManager.addMageMana(player, stats, refundForFailedSummon(type));
            player.sendMessage(Text.literal("§cVínculo insuficiente: " + current + "/" + max), true);
            return;
        }
        float durationBonus = Math.min(0.35f, AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_duration", false));
        int scaledDuration = Math.round(Math.max(duration, 400) * (1f + durationBonus));
        state.summons.put(type, scaledDuration);
        player.sendMessage(Text.literal("§dVínculo: §f" + Math.min(max, bondUsed(state)) + "/" + max), true);
    }

    private static int bondMax(PlayerStats stats, MageState state) {
        int max = stats.unlockedNodes.contains("mag_conj_pact") ? 4 : (stats.hasNode("mag_conj_pact") ? 2 : 0);
        max += Math.round(AbilityRegistry.sumPassive(stats.unlockedNodes, "bond_flat", false));
        if (state.ephemeralArmyTicks > 0) max += 3;
        return max;
    }

    private static int bondUsed(MageState state) {
        int used = 0;
        for (MageState.SummonType type : state.summons.keySet()) used += switch (type) {
            case FAMILIAR, ASTRAL_BLADE -> 1;
            case GUARDIAN, ASTRAL_SHIELD, ASTRAL_TURRET -> 2;
            case GREAT -> 4;
        };
        return used;
    }

    private static float refundForFailedSummon(MageState.SummonType type) {
        return switch (type) {
            case FAMILIAR -> 20f; case GUARDIAN -> 32f; case GREAT -> 52f;
            case ASTRAL_BLADE -> 18f; case ASTRAL_SHIELD -> 22f; case ASTRAL_TURRET -> 28f;
        };
    }

    private static void refundSummon(ServerPlayerEntity player, PlayerStats stats, MageState.SummonType type) {
        float pct = AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_refund", false);
        if (pct <= 0f) return;
        CombatState combat = CombatState.get(player.getUuid());
        // Várias invocações podem expirar no mesmo tick. Um gate curto evita explosões de Mana por expiração em lote.
        if (combat.cooldown("internal_summon_refund") > 0) return;
        float original = switch (type) {
            case FAMILIAR -> 25f; case GUARDIAN -> 40f; case GREAT -> 65f;
            case ASTRAL_BLADE -> 22f; case ASTRAL_SHIELD -> 28f; case ASTRAL_TURRET -> 35f;
        };
        CompatManager.addMageMana(player, stats, original * pct);
        combat.startCooldown("internal_summon_refund", 20);
        StatsManager.saveAndSync(player, stats);
    }

    private static void healVirtualSummons(ServerPlayerEntity player, PlayerStats stats, float fraction) {
        // Invocações virtuais não têm HP próprio; Transferência prolonga a duração restante em 20%, com teto.
        MageState state = MageState.get(player.getUuid());
        for (Map.Entry<MageState.SummonType, Integer> e : state.summons.entrySet()) {
            int extended = Math.round(e.getValue() * (1f + Math.max(0f, fraction)));
            e.setValue(Math.min(1600, extended));
        }
    }

    private static void markHuntTarget(ServerPlayerEntity player, int duration) {
        LivingEntity target = rayTarget(player, 18);
        if (target == null) target = nearestHostile(player, player.getPos(), 12);
        if (target != null) MageState.get(player.getUuid()).target(target.getUuid()).markedByHuntTicks = duration;
    }

    private static void transfusion(ServerPlayerEntity player, PlayerStats stats, float damage) {
        LivingEntity target = rayTarget(player, 14);
        if (target == null) return;
        if (dealMagic(player, stats, target, damage, MageState.School.BLOOD, true))
            player.heal(BossScaler.getTier(target) > 0 ? 1f : 2f);
    }

    private static void curseWeakness(ServerPlayerEntity player, int duration) {
        LivingEntity target = rayTarget(player, 16);
        if (target == null) return;
        PlayerStats stats = StatsManager.get(player);
        MageState.TargetState t = MageState.get(player.getUuid()).target(target.getUuid());
        t.weaknessTicks = curseDuration(stats, duration);
        refreshFateMark(stats, t);
    }

    private static void curseFragility(ServerPlayerEntity player, float amp, int duration) {
        LivingEntity target = rayTarget(player, 16);
        if (target == null) return;
        PlayerStats stats = StatsManager.get(player);
        MageState.TargetState t = MageState.get(player.getUuid()).target(target.getUuid());
        t.fragilityTicks = curseDuration(stats, duration);
        t.fragilityAmp = BossScaler.getTier(target) > 0 ? 0.04f : Math.min(0.08f, amp);
        refreshFateMark(stats, t);
    }

    private static void curseRuin(ServerPlayerEntity player, int duration) {
        LivingEntity target = rayTarget(player, 16);
        if (target == null) return;
        MageState.TargetState t = MageState.get(player.getUuid()).target(target.getUuid());
        PlayerStats stats = StatsManager.get(player);
        t.ruinTicks = curseDuration(stats, duration);
        t.ruinPulse = 0;
        refreshFateMark(StatsManager.get(player), t);
    }

    private static void spreadCurse(ServerPlayerEntity player, PlayerStats stats) {
        LivingEntity source = rayTarget(player, 16);
        if (source == null) return;
        MageState state = MageState.get(player.getUuid());
        MageState.TargetState src = state.target(source.getUuid());
        List<LivingEntity> candidates = new ArrayList<>(hostiles(player.getServerWorld(), source.getBoundingBox().expand(7), player));
        candidates.remove(source);
        candidates.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(source)));
        int count = 0;
        for (LivingEntity target : candidates) {
            if (count++ >= 3) break;
            MageState.TargetState t = state.target(target.getUuid());
            if (src.fragilityTicks > 0) { t.fragilityTicks = Math.round(src.fragilityTicks * 0.7f); t.fragilityAmp = src.fragilityAmp; }
            if (src.weaknessTicks > 0) t.weaknessTicks = Math.round(src.weaknessTicks * 0.7f);
            if (src.ruinTicks > 0) t.ruinTicks = Math.round(src.ruinTicks * 0.7f);
            refreshFateMark(stats, t);
        }
    }

    private static void greatCurse(ServerPlayerEntity player, PlayerStats stats, int duration) {
        duration = curseDuration(stats, duration);
        for (LivingEntity target : hostiles(player.getServerWorld(), player.getBoundingBox().expand(7), player)) {
            MageState.TargetState t = MageState.get(player.getUuid()).target(target.getUuid());
            t.weaknessTicks = duration;
            t.fragilityTicks = duration;
            t.fragilityAmp = BossScaler.getTier(target) > 0 ? 0.03f : 0.06f;
            t.ruinTicks = duration;
            refreshFateMark(stats, t);
        }
    }

    private static void lineCut(ServerPlayerEntity player, PlayerStats stats, float damage) {
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1f).normalize();
        Vec3d end = start.add(dir.multiply(10));
        Box box = new Box(start, end).expand(1.1);
        List<LivingEntity> targets = hostiles(player.getServerWorld(), box, player);
        targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));
        int hit = 0;
        for (LivingEntity target : targets) {
            if (target.getBoundingBox().expand(0.6).raycast(start, end).isPresent()) {
                dealMagic(player, stats, target, damage, MageState.School.HEXBLADE, true);
                if (++hit >= 4) break;
            }
        }
        particleLine(player.getServerWorld(), start, end, ParticleTypes.ENCHANTED_HIT);
    }

    private static void maybeTimeLock(ServerPlayerEntity player, PlayerStats stats, MageState state, LivingEntity target) {
        if (!stats.hasNode("mag_stag_time_lock")) return;
        MageState.TargetState ts = state.target(target.getUuid());
        if (ts.timeLockCooldown > 0) return;
        ts.timeLockCooldown = 100;
        if (BossScaler.getTier(target) > 0) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1, false, false, true));
        } else {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 15, 9, false, false, true));
        }
    }

    private static void slowTarget(ServerPlayerEntity player, int duration, int amplifier) {
        LivingEntity target = rayTarget(player, 16);
        if (target == null) return;
        PlayerStats stats = StatsManager.get(player);
        MageState state = MageState.get(player.getUuid());
        boolean alreadySlowed = target.hasStatusEffect(StatusEffects.SLOWNESS);
        int amp = BossScaler.getTier(target) > 0 ? 0 : amplifier;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, amp, false, false, true));
        if (alreadySlowed) maybeTimeLock(player, stats, state, target);
    }

    /** Efeito de Recuperação: cada Fragmento gasto encurta um pouco o maior cooldown real. */
    private static void onFragmentSpent(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        if (!stats.hasNode("mag_time_recovery")) return;
        CombatState.get(player.getUuid()).reduceLongestCooldown(0.05f);
    }

    private static int curseDuration(PlayerStats stats, int baseTicks) {
        float mult = 1f + AbilityRegistry.sumPassive(stats.unlockedNodes, "curse_duration", false);
        return Math.max(1, Math.round(baseTicks * Math.min(1.25f, mult)));
    }

    private static void refreshFateMark(PlayerStats stats, MageState.TargetState target) {
        if (!stats.hasNode("mag_curse_fate_mark")) return;
        if (target.weaknessTicks > 0 && target.fragilityTicks > 0 && target.ruinTicks > 0) {
            target.fateMarkReady = true;
            target.fateMarkTicks = 80;
        }
    }

    private static int activeSpiritCount(MageState state) {
        int count = 0;
        if (state.spiritLifeTicks > 0) count++;
        if (state.spiritEarthTicks > 0) count++;
        if (state.spiritHuntTicks > 0) count++;
        return count;
    }

    /** Troca um único espírito ativo preservando sua duração restante. Conselho Ancestral não é desmontado. */
    private static void rotateSpirit(ServerPlayerEntity player, MageState state) {
        int count = activeSpiritCount(state);
        if (count == 0) {
            player.sendMessage(Text.literal("§7Nenhum espírito ativo para trocar."), true);
            return;
        }
        if (count >= 2) {
            player.sendMessage(Text.literal("§aHarmonia ativa: múltiplos espíritos não podem ser trocados agora."), true);
            return;
        }
        int duration;
        String next;
        if (state.spiritLifeTicks > 0) {
            duration = state.spiritLifeTicks;
            state.spiritLifeTicks = 0;
            state.spiritEarthTicks = duration;
            next = "Terra";
        } else if (state.spiritEarthTicks > 0) {
            duration = state.spiritEarthTicks;
            state.spiritEarthTicks = 0;
            state.spiritHuntTicks = duration;
            next = "Caça";
        } else {
            duration = state.spiritHuntTicks;
            state.spiritHuntTicks = 0;
            state.spiritLifeTicks = duration;
            next = "Vida";
        }
        player.sendMessage(Text.literal("§aEspírito ativo: §f" + next), true);
    }

    private static int astralConstructCount(MageState state) {
        int count = 0;
        if (state.summons.containsKey(MageState.SummonType.ASTRAL_BLADE)) count++;
        if (state.summons.containsKey(MageState.SummonType.ASTRAL_SHIELD)) count++;
        if (state.summons.containsKey(MageState.SummonType.ASTRAL_TURRET)) count++;
        return count;
    }

    private static int conjurerSummonTypeCount(MageState state) {
        int count = 0;
        if (state.summons.containsKey(MageState.SummonType.FAMILIAR)) count++;
        if (state.summons.containsKey(MageState.SummonType.GUARDIAN)) count++;
        if (state.summons.containsKey(MageState.SummonType.GREAT)) count++;
        return count;
    }

    private static LivingEntity summonTarget(ServerPlayerEntity player, MageState state, double radius) {
        if (state.summonFocusTarget != null && state.summonFocusTicks > 0) {
            Entity raw = player.getServerWorld().getEntity(state.summonFocusTarget);
            if (raw instanceof LivingEntity focus && focus.isAlive() && isOffensiveTarget(player, focus)
                    && focus.squaredDistanceTo(player) <= radius * radius * 2.25) return focus;
        }
        return nearestHostile(player, player.getPos(), radius);
    }

    private static void summonDamage(ServerPlayerEntity player, PlayerStats stats, MageState state,
                                     LivingEntity target, float damage) {
        if (target == null) return;
        periodicDamage(player, stats, target, damage, MageState.School.SUMMON);
        if (stats.hasNode("mag_conj_shared_flow")
                && state.summonManaThisSecond < MageBalance.SUMMON_MANA_PER_SECOND_CAP) {
            float restore = Math.min(MageBalance.SUMMON_MANA_PER_HIT,
                    MageBalance.SUMMON_MANA_PER_SECOND_CAP - state.summonManaThisSecond);
            if (restore > 0f) {
                CompatManager.addMageMana(player, stats, restore);
                state.summonManaThisSecond += restore;
                state.statsDirty = true;
            }
        }
    }

    private static void tickDelayedTemporalEffects(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        if (state.temporalEchoTarget != null && state.temporalEchoDelay <= 0) {
            Entity raw = player.getServerWorld().getEntity(state.temporalEchoTarget);
            if (raw instanceof LivingEntity target && target.isAlive() && isOffensiveTarget(player, target)) {
                periodicDamage(player, stats, target, state.temporalEchoDamage, state.temporalEchoSchool);
                player.getServerWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.25, 0.4, 0.25, 0.02);
            }
            state.temporalEchoTarget = null;
            state.temporalEchoDamage = 0f;
        }

        if (state.temporalDebt > 0f && state.temporalDebtTicks > 0) {
            state.temporalDebtPulse++;
            if (state.temporalDebtPulse >= 20) {
                state.temporalDebtPulse = 0;
                int pulsesLeft = Math.max(1, (int) Math.ceil(state.temporalDebtTicks / 20.0));
                float payment = Math.min(state.temporalDebt, state.temporalDebt / pulsesLeft);
                state.temporalDebt -= payment;
                player.setHealth(Math.max(1f, player.getHealth() - payment));
            }
        } else if (state.temporalDebtTicks <= 0) {
            state.temporalDebt = 0f;
            state.temporalDebtPulse = 0;
        }
    }

    private static boolean areAllies(ServerPlayerEntity player, ServerPlayerEntity other) {
        return other == player || player.isTeammate(other);
    }

    /** Horizonte Lento não interfere nos próprios projéteis do Mago nem nos de aliados jogadores. */
    private static void slowHostileProjectiles(ServerPlayerEntity player, Box box, double factor) {
        for (ProjectileEntity projectile : player.getServerWorld().getEntitiesByClass(ProjectileEntity.class, box, Entity::isAlive)) {
            Entity owner = projectile.getOwner();
            if (owner == player) continue;
            if (owner instanceof ServerPlayerEntity ally && areAllies(player, ally)) continue;
            projectile.setVelocity(projectile.getVelocity().multiply(factor));
        }
    }

    private static void saveTemporalMark(ServerPlayerEntity player, PlayerStats stats, MageState state, int duration) {
        state.temporalMarkPos = player.getPos();
        state.temporalMarkDimension = dimensionId(player);
        state.temporalMarkHealth = player.getHealth();
        state.temporalMarkMana = CompatManager.mageMana(player, stats);
        state.temporalMarkTicks = duration;
        player.sendMessage(Text.literal("§bEstado temporal marcado por " + (duration / 20f) + "s."), true);
    }

    private static void rewind(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        if (state.temporalMarkPos == null || state.temporalMarkTicks <= 0) {
            player.sendMessage(Text.literal("§cNenhuma Marca Temporal ativa."), true);
            return;
        }
        if (!player.isAlive() || !dimensionId(player).equals(state.temporalMarkDimension)) {
            clearTemporalMark(state);
            player.sendMessage(Text.literal("§cA Marca Temporal foi invalidada por morte ou mudança de dimensão."), true);
            return;
        }
        teleport(player, state.temporalMarkPos);
        float lost = Math.max(0f, state.temporalMarkHealth - player.getHealth());
        float heal = Math.min(4f, lost);
        if (heal > 0) player.heal(heal);
        if (stats.hasNode("mag_rev_temporal_debt") && heal > 0) {
            state.temporalDebt += heal * 0.50f;
            state.temporalDebtTicks = Math.max(state.temporalDebtTicks, 100);
            state.temporalDebtPulse = 0;
        }
        state.causalLoopReady = stats.hasNode("mag_rev_causal_loop");
        clearTemporalMark(state);
    }

    private static void clearTemporalMark(MageState state) {
        state.temporalMarkPos = null;
        state.temporalMarkDimension = "";
        state.temporalMarkTicks = 0;
    }

    private static void saveRewrite(ServerPlayerEntity player, PlayerStats stats, MageState state, int duration) {
        state.rewritePos = player.getPos();
        state.rewriteDimension = dimensionId(player);
        state.rewriteHealth = player.getHealth();
        state.rewriteMana = CompatManager.mageMana(player, stats);
        state.rewriteTicks = duration;
        player.sendMessage(Text.literal("§5Destino gravado por " + (duration / 20f) + "s. A reversão ocorrerá ao fim."), true);
    }

    private static void finishRewriteIfNeeded(ServerPlayerEntity player, PlayerStats stats, MageState state) {
        if (state.rewritePos == null) return;
        if (!CompatManager.canUseRpgTechnique(player)) {
            state.rewriteTicks = 2;
            return;
        }
        if (!player.isAlive() || !dimensionId(player).equals(state.rewriteDimension)) {
            clearRewrite(state);
            return;
        }
        teleport(player, state.rewritePos);
        float currentMana = CompatManager.mageMana(player, stats);
        float lostHp = Math.max(0f, state.rewriteHealth - player.getHealth());
        float lostMana = Math.max(0f, state.rewriteMana - currentMana);
        if (lostHp > 0) player.heal(lostHp * 0.60f);
        if (lostMana > 0) CompatManager.setMageMana(player, stats, currentMana + lostMana * 0.60f);
        clearRewrite(state);
        state.temporalExhaustionTicks = 400;
        player.sendMessage(Text.literal("§5Destino reescrito. §7Exaustão temporal: -20% regen de Mana por 20s."), true);
        StatsManager.saveAndSync(player, stats);
    }

    private static void clearRewrite(MageState state) {
        state.rewritePos = null;
        state.rewriteDimension = "";
        state.rewriteTicks = 0;
    }

    private static String dimensionId(ServerPlayerEntity player) {
        return player.getWorld().getRegistryKey().getValue().toString();
    }

    private static void dash(ServerPlayerEntity player, double strength) {
        Vec3d look = player.getRotationVec(1f).normalize().multiply(Math.max(1.0, strength));
        player.addVelocity(look.x, 0.12, look.z);
        player.velocityModified = true;
    }

    private static void blink(ServerPlayerEntity player, double distance) {
        Vec3d target = safeBlinkPoint(player, distance);
        teleport(player, target);
    }

    private static void blinkTowardTarget(ServerPlayerEntity player, double range) {
        LivingEntity target = rayTarget(player, range);
        if (target == null) { blink(player, Math.min(5.0, range)); return; }
        Vec3d dir = target.getPos().subtract(player.getPos()).normalize();
        Vec3d pos = target.getPos().subtract(dir.multiply(1.5));
        teleport(player, pos);
    }

    private static Vec3d safeBlinkPoint(ServerPlayerEntity player, double distance) {
        Vec3d start = player.getPos();
        Vec3d dir = player.getRotationVec(1f).normalize();
        // Conservador: reduz em passos até achar espaço livre para o bounding box do jogador.
        for (double d = Math.max(1.0, distance); d >= 1.0; d -= 0.5) {
            Vec3d p = start.add(dir.multiply(d));
            Box moved = player.getBoundingBox().offset(p.subtract(start));
            if (player.getServerWorld().isSpaceEmpty(player, moved)) return p;
        }
        return start;
    }

    private static void teleport(ServerPlayerEntity player, Vec3d pos) {
        if (pos == null) return;
        player.teleport(player.getServerWorld(), pos.x, pos.y, pos.z, player.getYaw(), player.getPitch());
        player.fallDistance = 0f;
    }

    private static Vec3d aimPoint(ServerPlayerEntity player, double range) {
        LivingEntity target = rayTarget(player, range);
        if (target != null) return target.getPos();
        return player.getEyePos().add(player.getRotationVec(1f).normalize().multiply(range * 0.65)).add(0, -1.0, 0);
    }

    private static LivingEntity rayTarget(ServerPlayerEntity player, double range) {
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1f).normalize().multiply(range));
        Box search = new Box(start, end).expand(1.5);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity target : hostiles(player.getServerWorld(), search, player)) {
            Optional<Vec3d> hit = target.getBoundingBox().expand(0.35).raycast(start, end);
            if (hit.isEmpty()) continue;
            double dist = start.squaredDistanceTo(hit.get());
            if (dist < bestDist) { bestDist = dist; best = target; }
        }
        return best;
    }

    private static LivingEntity nearestHostile(ServerPlayerEntity player, Vec3d center, double radius) {
        List<LivingEntity> list = hostiles(player.getServerWorld(), new Box(center, center).expand(radius), player);
        return list.stream().min(Comparator.comparingDouble(e -> e.getPos().squaredDistanceTo(center))).orElse(null);
    }

    private static LivingEntity nearestHostileExcluding(ServerPlayerEntity player, Vec3d center, double radius, List<LivingEntity> exclude) {
        return hostiles(player.getServerWorld(), new Box(center, center).expand(radius), player).stream()
                .filter(e -> !exclude.contains(e))
                .min(Comparator.comparingDouble(e -> e.getPos().squaredDistanceTo(center))).orElse(null);
    }

    private static List<LivingEntity> hostiles(ServerWorld world, Box box, ServerPlayerEntity player) {
        return world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && isOffensiveTarget(player, e));
    }

    private static boolean isOffensiveTarget(ServerPlayerEntity player, LivingEntity target) {
        if (target == player || target instanceof PlayerEntity) return false;
        if (target instanceof PassiveEntity) return false;
        if (target instanceof TameableEntity tameable && tameable.getOwner() == player) return false;
        return true;
    }

    private static LivingEntity nearestHostile(ServerPlayerEntity player, Vec3d center, double radius, boolean includePassive) {
        return nearestHostile(player, center, radius);
    }

    private static List<ServerPlayerEntity> allies(ServerPlayerEntity player, Vec3d center, double radius) {
        Box box = new Box(center, center).expand(radius);
        return player.getServerWorld().getEntitiesByClass(ServerPlayerEntity.class, box,
                p -> p.isAlive() && (p == player || player.getScoreboardTeam() == null || player.isTeammate(p)));
    }

    private static void healAllies(ServerPlayerEntity player, Vec3d center, double radius, float amount) {
        for (ServerPlayerEntity ally : allies(player, center, radius)) ally.heal(amount);
    }

    private static void addZoneAtAim(ServerPlayerEntity player, MageState.ZoneType type, double radius, int ticks, float power) {
        addZone(player, type, aimPoint(player, 10), radius, ticks, power);
    }

    private static void addZone(ServerPlayerEntity player, MageState.ZoneType type, Vec3d center, double radius, int ticks, float power) {
        MageState state = MageState.get(player.getUuid());
        state.zones.add(new MageState.Zone(type, center, radius, Math.max(20, ticks), power));
        while (state.zones.size() > 12) state.zones.remove(0);
    }

    private static boolean isInsideZone(MageState state, Vec3d pos, MageState.ZoneType type) {
        for (MageState.Zone z : state.zones) {
            if (z.type == type && z.ticks > 0 && z.center.squaredDistanceTo(pos) <= z.radius * z.radius) return true;
        }
        return false;
    }


    private static void particleLine(ServerWorld world, Vec3d from, Vec3d to, net.minecraft.particle.ParticleEffect particle) {
        Vec3d delta = to.subtract(from);
        int steps = Math.max(2, Math.min(24, (int) (delta.length() * 2)));
        for (int i = 0; i <= steps; i++) {
            Vec3d p = from.add(delta.multiply(i / (double) steps));
            world.spawnParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static net.minecraft.particle.ParticleEffect particleFor(MageState.School school) {
        return switch (school) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case BLOOD, OCCULT -> ParticleTypes.DAMAGE_INDICATOR;
            case TEMPORAL, SPACE -> ParticleTypes.REVERSE_PORTAL;
            default -> ParticleTypes.ENCHANTED_HIT;
        };
    }

    private static boolean isExternalSpell(MageState state, String... paths) {
        String node = state.currentNode == null ? "" : state.currentNode;
        int separator = node.lastIndexOf(':');
        String path = separator >= 0 ? node.substring(separator + 1) : node;
        for (String candidate : paths) if (candidate.equals(path)) return true;
        return false;
    }

    private static boolean isElementalSchool(MageState.School school) {
        return school == MageState.School.FIRE || school == MageState.School.ICE || school == MageState.School.LIGHTNING;
    }

    private static boolean isOffensiveSchool(MageState.School school) {
        return school != MageState.School.NONE && school != MageState.School.SUMMON;
    }

    private static boolean isElementalNode(String id) {
        return id.startsWith("mag_ele_") || id.startsWith("mag_pyr_") || id.startsWith("mag_cryo_") || id.startsWith("mag_storm_");
    }

    private static boolean isOccultNode(String id) {
        return id.startsWith("mag_occ_") || id.startsWith("mag_blood_") || id.startsWith("mag_curse_") || id.startsWith("mag_hex_");
    }

    private static boolean isAscension(String id) {
        return id.contains("_asc_");
    }

    private static boolean isOffensiveMageActive(String id) {
        return !(id.contains("barrier") || id.contains("counterspell") || id.contains("purification")
                || id.contains("mirror_image") || id.contains("anchor") || id.contains("formation")
                || id.contains("mark") || id.contains("acceleration") || id.contains("instant_cast"));
    }

    private static String category(String id) {
        if (MOBILITY_NODES.contains(id) || id.contains("blink") || id.contains("step") || id.contains("shift")) return "MOVE";
        if (id.contains("barrier") || id.contains("counterspell") || id.contains("parry") || id.contains("shield")) return "DEFENSE";
        if (id.contains("slow") || id.contains("stasis") || id.contains("confusion") || id.contains("curse") || id.contains("anchor")) return "CONTROL";
        return "ATTACK";
    }

    private static MageState.School schoolFor(String id) {
        if (id.startsWith("mag_pyr_")) return MageState.School.FIRE;
        if (id.startsWith("mag_cryo_")) return MageState.School.ICE;
        if (id.startsWith("mag_storm_")) return MageState.School.LIGHTNING;
        if (id.startsWith("mag_blood_")) return MageState.School.BLOOD;
        if (id.startsWith("mag_occ_") || id.startsWith("mag_curse_")) return MageState.School.OCCULT;
        if (id.startsWith("mag_time_") || id.startsWith("mag_acc_") || id.startsWith("mag_stag_") || id.startsWith("mag_rev_")) return MageState.School.TEMPORAL;
        if (id.startsWith("mag_tele_")) return MageState.School.SPACE;
        if (id.startsWith("mag_summ_") || id.startsWith("mag_anim_") || id.startsWith("mag_astral_")) return MageState.School.SUMMON;
        if (id.startsWith("mag_hex_")) return MageState.School.HEXBLADE;
        return MageState.School.ARCANE;
    }

    private static String real(String nodeId) { return nodeId==null?null:HouseRules.real(nodeId); }

    private MageCombatHandler() {}
}
