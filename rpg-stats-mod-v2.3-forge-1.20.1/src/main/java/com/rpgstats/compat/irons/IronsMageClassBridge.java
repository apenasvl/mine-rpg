package com.rpgstats.compat.irons;

import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.CombatState;
import com.rpgstats.combat.MageBalance;
import com.rpgstats.combat.MageState;
import com.rpgstats.stats.PlayerStats;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Connects the real Iron's spell lifecycle to the Mage class tree.
 *
 * Iron's remains the spell engine. This bridge only makes RPG Stats' Mage core, Houses and
 * specializations react to real Iron casts/hits so those trees are not limited to the old internal
 * mage-active pipeline.
 */
final class IronsMageClassBridge {
    private static final UUID RPG_SUMMON_DAMAGE = UUID.fromString("8ac1a04a-cfa1-4db8-8829-1cc97a1df149");

    private static final UUID RPG_STORM_CAST_SPEED = UUID.fromString("a3e94227-8e43-4c8e-9d8d-0b6fc8882980");

    private static final Map<UUID, String> CORE_ECHO_READY = new HashMap<>();
    private static final Map<UUID, String> MIRROR_ECHO_READY = new HashMap<>();
    private static final Map<UUID, String> ILLUSION_ECHO_READY = new HashMap<>();

    private static final Set<String> CURSE_RUIN_SPELLS = Set.of("blight", "wither_skull", "heartstop");
    private static final Set<String> STAGNATOR_CONTROL = Set.of("slow", "root", "arcane_shackle");
    private static final Set<String> EARTH_SPIRIT_SPELLS = Set.of("oakskin", "fortify", "earthquake", "stomp");
    private static final Set<String> LIFE_SPIRIT_SPELLS = Set.of(
            "heal", "greater_heal", "healing_circle", "cloud_of_regeneration", "blessing_of_life", "cleanse", "wisp");
    private static final Set<String> HUNT_SPIRIT_SPELLS = Set.of(
            "root", "spider_aspect", "poison_arrow", "poison_breath", "poison_splash", "acid_orb", "blight");

    /** Extra conditional cost reductions that the old Mage active pipeline already understood. */
    static float additionalManaReduction(ServerPlayerEntity player, PlayerStats stats, String spellId,
                                         SchoolType ironSchool) {
        if (stats.clazz != RPGClass.MAGO) return 0f;
        MageState state = MageState.get(player.getUuid());
        float reduction = 0f;

        if (stats.corruption >= 50f && stats.hasNode("mag_occ_efficiency")) reduction += 0.06f;
        if (state.arcaneFormTicks > 0) reduction += 0.15f;
        if (state.winterHeartTicks > 0 && schoolPath(ironSchool).equals("ice")) reduction += 0.08f;

        if (state.flowReady) {
            reduction += 0.12f;
            state.flowReady = false;
        }
        if (state.causalLoopReady) {
            reduction += 0.30f;
            state.causalLoopReady = false;
        }
        if (state.idleTicks >= 60 && stats.hasNode("mag_arc_preparation")) reduction += 0.10f;

        return Math.max(0f, reduction);
    }

    /** Called once for each real Iron cast, including utility spells that never deal damage. */
    static void onCast(ServerPlayerEntity player, PlayerStats stats, String spellId,
                       SchoolType ironSchool, int manaCost) {
        if (stats.clazz != RPGClass.MAGO) return;
        MageState state = MageState.get(player.getUuid());
        CombatState combat = CombatState.get(player.getUuid());
        UUID playerId = player.getUuid();
        String path = spellPath(spellId);
        String school = schoolPath(ironSchool);
        boolean offensive = isOffensiveSpell(spellId);
        boolean dirty = false;

        // Never let a missed cast arm an echo forever; the next cast replaces the pending proc.
        CORE_ECHO_READY.remove(playerId);
        MIRROR_ECHO_READY.remove(playerId);
        ILLUSION_ECHO_READY.remove(playerId);

        int gap = state.idleTicks;
        state.idleTicks = 0;
        state.spellsInWindow++;
        state.spellWindowTicks = 100;

        // Core + Temporal House: real Iron casts now build rhythm/fragments/momentum.
        if (stats.hasNode("mag_acc_momentum")) {
            state.momentumStacks = gap <= 60 ? Math.min(3, state.momentumStacks + 1) : 1;
            state.momentumTicks = 100;
        }
        if (stats.hasNode("mag_time_rhythm") && state.spellsInWindow >= 3 && state.temporalRhythmCooldown <= 0) {
            stats.temporalFragments = Math.min(stats.temporalFragmentMax(), stats.temporalFragments + 1);
            state.spellsInWindow = 0;
            state.temporalRhythmCooldown = 60;
            dirty = true;
        }

        // Core Flow: three different Iron spells prepare the following cast for the mana discount.
        if (stats.hasNode("mag_core_flow")) {
            state.flowRecent.add("irons:" + path);
            if (state.flowRecent.size() >= 3) {
                state.flowReady = true;
                state.flowRecent.clear();
            }
        }

        // Arcana House: Iron spells supply categories to Triad/Seals instead of requiring legacy actives.
        if (stats.hasNode("mag_arc_sequence")) {
            String category = castCategory(path, school, spellId);
            if (!state.recentArcaneCategories.contains(category)) state.recentArcaneCategories.addLast(category);
            while (state.recentArcaneCategories.size() > 3) state.recentArcaneCategories.removeFirst();
            if (stats.hasNode("mag_arc_triad") && state.recentArcaneCategories.size() == 3) {
                state.arcaneSeals = Math.min(2, state.arcaneSeals + 1);
                if (stats.hasNode("mag_arc_perfect_cycle")) {
                    com.rpgstats.compat.CompatManager.addMageMana(player, stats, 4f);
                }
                state.recentArcaneCategories.clear();
            }
        }

        // Cryomancer ascension no longer depends on removed Concentration: an Ice cast can open
        // the Winter Heart window when its internal cooldown is ready.
        if (school.equals("ice") && stats.hasNode("mag_cryo_asc_winterheart")
                && combat.cooldown("internal_winterheart") <= 0) {
            state.winterHeartTicks = Math.max(state.winterHeartTicks, 160);
            combat.startCooldown("internal_winterheart", 800);
        }

        // Occult House: Blood/Eldritch casts are what feed corruption in the Iron-based architecture.
        if ((school.equals("blood") || school.equals("eldritch")) && stats.hasNode("mag_occ_forbidden")
                && combat.cooldown("internal_irons_corruption") <= 0) {
            stats.corruption = Math.min(MageBalance.MAX_CORRUPTION, stats.corruption + 3f);
            combat.startCooldown("internal_irons_corruption", 20);
            dirty = true;
        }

        // Core Guard must count mana actually spent by Iron's, not only old RPG active skills.
        if (manaCost > 0) {
            state.manaSpentWindow += manaCost;
            state.manaSpentWindowTicks = 60;
            if (stats.hasNode("mag_core_guard") && state.manaSpentWindow >= 35f
                    && combat.cooldown("internal_mage_guard") <= 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 120, 0, false, false, true));
                combat.startCooldown("internal_mage_guard", 300);
                state.manaSpentWindow = 0f;
            }
        }

        // Echo-type specializations are armed on cast and consumed only by the first accepted damage hit.
        if (offensive) {
            if (stats.hasNode("mag_core_echo") && combat.cooldown("internal_arcane_echo") <= 0
                    && player.getRandom().nextFloat() < 0.08f) {
                CORE_ECHO_READY.put(playerId, path);
                combat.startCooldown("internal_arcane_echo", 160);
            }
            if (state.mirrorHallTicks > 0) {
                state.mirrorCastCounter = (state.mirrorCastCounter + 1) % 3;
                if (state.mirrorCastCounter == 0) MIRROR_ECHO_READY.put(playerId, path);
            }
            if (stats.hasNode("mag_illu_arcane_reflection") && state.illusionTicks > 0
                    && combat.cooldown("internal_illusion_reflection") <= 0
                    && player.getRandom().nextFloat() < AbilityRegistry.sumPassive(
                            stats.unlockedNodes, "illusion_echo", false)) {
                ILLUSION_ECHO_READY.put(playerId, path);
                combat.startCooldown("internal_illusion_reflection", 120);
            }
        }

        // Animist: real Nature/Holy spell choices drive the three stances.
        if (stats.hasNode("mag_anim_life_spirit") && (school.equals("holy") || LIFE_SPIRIT_SPELLS.contains(path)))
            state.spiritLifeTicks = Math.max(state.spiritLifeTicks, 240);
        if (stats.hasNode("mag_anim_earth_spirit") && EARTH_SPIRIT_SPELLS.contains(path))
            state.spiritEarthTicks = Math.max(state.spiritEarthTicks, 240);
        if (stats.hasNode("mag_anim_hunt_spirit") && (school.equals("nature") || HUNT_SPIRIT_SPELLS.contains(path)))
            state.spiritHuntTicks = Math.max(state.spiritHuntTicks, 240);

        // Utility spells never emit SpellDamageEvent; use Iron's authoritative cast target.
        if (STAGNATOR_CONTROL.contains(path)
                && MagicData.getPlayerMagicData(player).getAdditionalCastData() instanceof TargetEntityCastData data) {
            LivingEntity target = data.getTarget(player.getServerWorld());
            if (target != null && target != player && target.isAlive()
                    && !(target instanceof net.minecraft.entity.player.PlayerEntity)) {
                MageState.TargetState status = state.target(target.getUuid());
                if (stats.hasNode("mag_stag_time_lock") && status.timeLockCooldown <= 0) {
                    boolean boss = BossScaler.getTier(target) > 0;
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                            boss ? 40 : 15, boss ? 0 : 4, false, false, true));
                    status.timeLockCooldown = 60;
                }
                if ((path.equals("slow") && stats.hasNode("mag_stag_temporal_slow"))
                        || (path.equals("root") && stats.hasNode("mag_stag_temporal_anchor")))
                    status.weaknessTicks = Math.max(status.weaknessTicks, 120);
                if (stats.hasNode("mag_stag_entropy")) {
                    status.fragilityTicks = Math.max(status.fragilityTicks, 120);
                    status.fragilityAmp = Math.max(status.fragilityAmp, 0.06f);
                }
                if (stats.hasNode("mag_illu_confusion")) status.weaknessTicks = Math.max(status.weaknessTicks, 60);
            }
        }

        state.lastSpellNode = "irons:" + path;
        if (dirty) state.statsDirty = true;
    }

    /**
     * Adds class/spec mechanics to the current hit before per-spell normalization and target budget.
     * This ordering is deliberate: echoes and rune synergy are still constrained by the Iron balance caps.
     */
    static float modifyDamage(ServerPlayerEntity player, PlayerStats stats, LivingEntity target,
                              String spellId, SchoolType ironSchool, float amount) {
        if (stats.clazz != RPGClass.MAGO || amount <= 0f) return amount;
        MageState state = MageState.get(player.getUuid());
        UUID playerId = player.getUuid();
        String path = spellPath(spellId);
        String school = schoolPath(ironSchool);
        float result = amount;

        if (consumeIfMatches(CORE_ECHO_READY, playerId, path)) result *= 1.35f;
        if (consumeIfMatches(MIRROR_ECHO_READY, playerId, path)) result *= 1.25f;
        if (consumeIfMatches(ILLUSION_ECHO_READY, playerId, path)) result *= 1.20f;

        // Reverser's Temporal Echo now works on the real Iron offensive spell instead of only legacy damage.
        if (state.temporalEchoReady && state.temporalEchoTicks > 0 && isOffensiveSpell(spellId)) {
            state.temporalEchoReady = false;
            state.temporalEchoTicks = 0;
            result *= 1.40f;
        }

        // Cryomancer capstone is a bounded school-specific boost; target budget still wins afterwards.
        if (state.winterHeartTicks > 0 && school.equals("ice")) result *= 1.08f;

        if (state.fireStepTicks > 0 && school.equals("fire") && isEligibleNextSpell(spellId)) {
            result *= 1.06f;
        }
        if (state.lightningStepTicks > 0 && school.equals("lightning") && isEligibleNextSpell(spellId)) {
            result *= 1.05f;
        }
        if (stats.hasNode("mag_storm_conductor") && Set.of("chain_lightning", "ball_lightning", "thunderstorm").contains(path))
            result *= 1.06f;

        // Runist finally interacts with the Iron arsenal: casting close to one of your runes empowers the hit.
        if (stats.hasNode("mag_rune_amplifier") && isNearOwnRune(player, state, 6.0)) result *= 1.05f;

        return result;
    }

    /** Post-hit mechanics that require a real accepted Iron hit. */
    static void onResolvedHit(ServerPlayerEntity player, PlayerStats stats, LivingEntity target,
                              String spellId, SchoolType ironSchool, float damage) {
        if (stats.clazz != RPGClass.MAGO || damage <= 0f) return;
        MageState state = MageState.get(player.getUuid());
        String path = spellPath(spellId);
        boolean dirty = false;


        // Consume mobility preparation only after accepted eligible damage, never on the dash itself.
        if (isEligibleNextSpell(spellId)) {
            String school = schoolPath(ironSchool);
            if (school.equals("fire")) state.fireStepTicks = 0;
            if (school.equals("lightning")) state.lightningStepTicks = 0;
        }

        MageState.TargetState targetState = state.target(target.getUuid());

        // Curseweaver: Iron's curse spells now drive Ruin and the three-curse Fate Mark loop.
        if (stats.hasNode("mag_curse_ruin") && CURSE_RUIN_SPELLS.contains(path)) {
            float durationBonus = Math.max(0f,
                    AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_curse_ruin", false)
                            + AbilityRegistry.sumPassive(stats.unlockedNodes, "curse_duration", false));
            targetState.ruinTicks = Math.max(targetState.ruinTicks, Math.round(120f * (1f + durationBonus)));
            targetState.ruinPulse = 0;
        }
        if (stats.hasNode("mag_curse_fate_mark") && targetState.weaknessTicks > 0
                && targetState.fragilityTicks > 0 && targetState.ruinTicks > 0) {
            targetState.fateMarkReady = true;
            targetState.fateMarkTicks = Math.max(targetState.fateMarkTicks, 120);
        }

        // Bloodmancer Transfusion modifies Iron's sustain spells without bypassing boss efficiency/caps.
        if ((path.equals("ray_of_siphoning") || path.equals("devour"))) {
            float drain = Math.min(0.02f,
                    Math.max(0f, AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_blood_drain", false)));
            if (drain > 0f) {
                float efficiency = BossScaler.getTier(target) > 0 ? 0.50f : 1f;
                player.heal(Math.min(1.5f, damage * drain * efficiency));
            }
        }

        // Stagnator: control spells feed Time Lock instead of being disconnected from the specialization.
        if (STAGNATOR_CONTROL.contains(path) && stats.hasNode("mag_stag_time_lock")
                && targetState.timeLockCooldown <= 0) {
            boolean boss = BossScaler.getTier(target) > 0;
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    boss ? 40 : 15, boss ? 0 : 4, false, false, true));
            targetState.timeLockCooldown = 60;
        }

        if (dirty) state.statsDirty = true;
    }

    /** Iron summons read this attribute from their summoner, so Conjuration must modify it natively. */
    static void applyIronsAttributes(ServerPlayerEntity player, PlayerStats stats) {
        EntityAttributeInstance summon = player.getAttributeInstance(AttributeRegistry.SUMMON_DAMAGE.get());
        if (summon == null) return;
        if (summon.getModifier(RPG_SUMMON_DAMAGE) != null) summon.removeModifier(RPG_SUMMON_DAMAGE);

        MageState state = MageState.get(player.getUuid());
        float bonus = Math.max(0f, AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_damage"));
        bonus += Math.max(0f, AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_summon_elite"));
        bonus += IronsNativeSummons.diversityBonus(player, stats);
        bonus = Math.min(0.35f, bonus);
        // Commands retain headroom at full mastery; their bonuses never stack with each other.
        bonus += Math.max(state.summonAssaultTicks > 0 ? 0.12f : 0f,
                state.ephemeralArmyTicks > 0 ? 0.15f : 0f);
        bonus = Math.min(0.50f, bonus);

        if (bonus > 0.001f) {
            summon.addTemporaryModifier(new EntityAttributeModifier(RPG_SUMMON_DAMAGE,
                    "RPG Stats Conjuration summon damage", bonus, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    static void applyTemporaryCastAttributes(ServerPlayerEntity player, PlayerStats stats) {
        EntityAttributeInstance speed = player.getAttributeInstance(AttributeRegistry.CAST_TIME_REDUCTION.get());
        if (speed != null) {
            if (speed.getModifier(RPG_STORM_CAST_SPEED) != null) speed.removeModifier(RPG_STORM_CAST_SPEED);
            if (stats.clazz == RPGClass.MAGO && MageState.get(player.getUuid()).stormAvatarTicks > 0)
                speed.addTemporaryModifier(new EntityAttributeModifier(RPG_STORM_CAST_SPEED,
                        "RPG Stats Storm Avatar casting speed", 0.15, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    static boolean isCombatSummon(String spellId) {
        return IronsSpellBalance.profileFor(spellId) == IronsSpellBalance.Profile.SUMMON;
    }

    private static boolean isEligibleNextSpell(String spellId) {
        IronsSpellBalance.Profile profile = IronsSpellBalance.profileFor(spellId);
        return profile != IronsSpellBalance.Profile.UTILITY
                && profile != IronsSpellBalance.Profile.MOBILITY_DAMAGE
                && profile != IronsSpellBalance.Profile.SUMMON;
    }

    private static boolean isOffensiveSpell(String spellId) {
        return IronsSpellBalance.profileFor(spellId) != IronsSpellBalance.Profile.UTILITY;
    }

    private static boolean consumeIfMatches(Map<UUID, String> map, UUID playerId, String path) {
        String armed = map.get(playerId);
        if (armed == null || !armed.equals(path)) return false;
        map.remove(playerId);
        return true;
    }

    private static boolean isNearOwnRune(ServerPlayerEntity player, MageState state, double radius) {
        double maxSq = radius * radius;
        for (MageState.Rune rune : state.runes) {
            if (!rune.triggered && rune.pos.squaredDistanceTo(player.getPos()) <= maxSq) return true;
        }
        return false;
    }

    private static String castCategory(String path, String school, String spellId) {
        if (Set.of("teleport", "blood_step", "burning_dash", "frost_step", "thunder_step", "charge", "portal").contains(path))
            return "mobility";
        if (isCombatSummon(spellId)) return "summon";
        if (IronsSpellBalance.profileFor(spellId) == IronsSpellBalance.Profile.UTILITY) return "utility";
        if (school.equals("fire") || school.equals("ice") || school.equals("lightning")) return school;
        if (school.equals("blood") || school.equals("eldritch")) return "occult";
        if (school.equals("ender")) return "space";
        if (school.equals("nature") || school.equals("holy")) return "spirit";
        return "arcane";
    }

    private static String schoolPath(SchoolType school) {
        return school == null || school.getId() == null ? "evocation" : school.getId().getPath();
    }

    private static String spellPath(String spellId) {
        if (spellId == null) return "";
        int separator = spellId.indexOf(':');
        return separator >= 0 ? spellId.substring(separator + 1) : spellId;
    }

    private IronsMageClassBridge() {}
}
