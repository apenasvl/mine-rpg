package com.rpgstats.compat.irons;

import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.balance.GlobalCaps;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.CombatState;
import com.rpgstats.combat.MageBalance;
import com.rpgstats.combat.MageCombatHandler;
import com.rpgstats.combat.MageState;
import com.rpgstats.combat.SoulslikeCombat;
import com.rpgstats.compat.CompatManager;
import com.rpgstats.compat.MageManaBridge;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.Stat;
import com.rpgstats.stats.StatsManager;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.Set;
import java.util.UUID;

/**
 * Optional bridge for Iron's Spells 'n Spellbooks 1.20.1.
 *
 * This class is loaded reflectively only when irons_spellbooks is present. The RPG core therefore
 * remains fully usable without Iron's. Iron's supplies the actual spells while RPG Stats supplies
 * class/House progression and bounded scaling.
 */
public final class IronsSpellsCompat {
    private static final UUID RPG_MAX_MANA = UUID.fromString("d546c6b8-58b9-4f31-a192-31ae39385f20");
    private static final UUID RPG_MANA_REGEN = UUID.fromString("da1d9af1-bdc8-4a44-9d77-3ae45afc2a09");
    private static final Set<String> MOBILITY_SPELLS = Set.of("teleport", "blood_step", "burning_dash", "frost_step", "thunder_step", "charge", "portal");
    private static final Set<String> ILLUSION_SPELLS = Set.of("scapegoat", "invisibility");
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        CompatManager.registerMageManaBridge(new IronsManaBridge());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, IronsSpellsCompat::preCast);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, IronsSpellsCompat::onCast);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, IronsSpellsCompat::spellDamage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, IronsSpellsCompat::cooldown);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IronsSpellsCompat::damageResolved);
    }

    /** Iron's spellbooks are the spell arsenal of the Mage class. */
    private static void preCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) event.setCanceled(true);
    }

    /** Apply RPG mana-efficiency rules to the amount Iron's actually consumes. */
    private static void onCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return;

        String spellId = event.getSpellId();
        float reduction = AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_cost_reduction");
        MageState.School school = mapSchool(event.getSchoolType());
        if (isElemental(school)) {
            reduction += AbilityRegistry.sumPassive(stats.unlockedNodes, "elemental_cost_reduction");
        }
        reduction += IronsMageClassBridge.additionalManaReduction(player, stats, spellId, event.getSchoolType());
        reduction = Math.min(MageBalance.MAX_MANA_COST_REDUCTION, Math.max(0f, reduction));
        reduction = IronsSpellBalance.balanceManaReduction(spellId, reduction);
        float surcharge = 1f + HouseRules.surcharge(stats);
        int cost = Math.max(0, Math.round(event.getOriginalManaCost() * (1f - reduction) * surcharge));
        event.setManaCost(cost);

        // Real Iron casts are the authoritative trigger for Mage core/House/spec progression.
        IronsMageClassBridge.onCast(player, stats, spellId, event.getSchoolType(), cost);
        applyCastModifiers(player, stats, spellId, school);
    }

    /**
     * Iron's emits SpellDamageEvent before school resistance and vanilla hurt processing. This is
     * the correct place to add RPG offensive scaling; the normal RPG damage mixin skips this source
     * so the spell is never mistaken for a bow/physical hit and never pays physical stamina.
     */
    private static void spellDamage(SpellDamageEvent event) {
        SpellDamageSource source = event.getSpellDamageSource();
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return;

        MageState state = MageState.get(player.getUuid());
        MageState.School school = mapSchool(source.spell().getSchoolType());
        String spellId = source.spell().getSpellId();
        String path = spellPath(spellId);
        String node = "irons:" + spellId;
        state.beginCast(node, school, true);
        try {
            float original = Math.max(0f, event.getAmount());
            float magicPower = Math.min(MageBalance.MAGIC_POWER_CAP,
                    Math.max(0f, AbilityRegistry.sumPassive(stats.unlockedNodes, "magic_power")));
            float persistentMultiplier = SoulslikeCombat.magicAttributeMultiplier(stats);
            float result = original * (1f + magicPower) * persistentMultiplier;

            // MageCombatHandler already applies primary House mechanics. This supplement adds only
            // the reduced secondary-House portion so primary Elemental bonuses are not counted twice.
            result *= secondarySchoolMultiplier(stats, path, school);
            result *= ironNodeMultiplier(stats, path, school);
            LivingEntity target = event.getEntity();
            result = MageCombatHandler.modifyMagicDamage(player, target, result);
            result *= 1f - HouseRules.offensePenalty(stats);

            // Core/Houses/specializations can transform the real Iron hit, but they are deliberately
            // applied before the per-spell and per-target guards so echoes/multi-hit cannot bypass balance.
            result = IronsMageClassBridge.modifyDamage(
                    player, stats, target, spellId, source.spell().getSchoolType(), result);

            // Spell shape constrains conditional bursts; earned INT/level/core is retained once.
            result = IronsSpellBalance.applyRpgScaling(spellId, original, result, persistentMultiplier);
            // Hemomancy is earned school mastery, rather than another conditional proc discarded
            // by sustain normalization. The emergency cap and shared target bucket still apply.
            float bloodMultiplier = school == MageState.School.BLOOD
                    ? IronsSpellBalance.bloodmancerMultiplier(spellId, stats.level,
                            stats.totalStats().getOrDefault(Stat.INTELIGENCIA, 0),
                            AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_blood_hemorrhage", false) > 0f,
                            target instanceof net.minecraft.entity.player.PlayerEntity) : 1f;
            result *= bloodMultiplier;
            if (original > 0f) result = original * persistentMultiplier
                    * GlobalCaps.damageMultiplier(result / (original * persistentMultiplier));

            // Final guard is per caster + target + spell. This keeps AoE valuable across many mobs,
            // while volleys/recasts/continuous spells cannot stack every projectile into one boss.
            boolean fixedBoss = com.rpgstats.integration.IntegrationServices.INSTANCE.boss(target)
                    .map(com.rpgstats.integration.BossProfile::fixed).isPresent();
            boolean mariumBoss = fixedBoss && "soulsweapons".equals(
                    net.minecraft.registry.Registries.ENTITY_TYPE.getId(target.getType()).getNamespace());
            float bossBudgetScale = mariumBoss
                    ? MageBossBalance.mariumBudgetScale(stats.level, stats.stats.get(Stat.INTELIGENCIA))
                    : MageBossBalance.budgetScale(stats.level, stats.stats.get(Stat.INTELIGENCIA), fixedBoss);
            float targetBudgetScale = target instanceof net.minecraft.entity.player.PlayerEntity ? 1f
                    : MageBossBalance.progressionBudgetScale(persistentMultiplier, bossBudgetScale);
            result = IronsSpellBalance.applyTargetBudget(
                    spellId, player.getUuid(), target.getUuid(), player.age, result, targetBudgetScale);
            // Ray natively drains 100% and Blood Slash/Needles also drain. Increasing offense
            // must not multiply that native healing at the same time. Native casts use a fresh
            // damage source for each hit; RPG drain remains based on accepted damage as before.
            if (bloodMultiplier > 1f && source.getLifestealPercent() > 0f)
                source.setLifestealPercent(source.getLifestealPercent() / bloodMultiplier);
            event.setAmount(Math.max(0f, result));
        } finally {
            state.endCast();
        }
    }

    /** Iron's cooldown event lets the RPG Temporal/core CDR affect real spellbook cooldowns. */
    private static void cooldown(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return;
        // sumPassive without a House filter includes the primary bonus at full value and the
        // secondary bonus after HouseRules.scaled has applied the borrowed-House penalty.
        float permanent = Math.min(MageBalance.PERMANENT_CDR_CAP,
                Math.max(0f, AbilityRegistry.sumPassive(stats.unlockedNodes, "cooldown_recovery")));
        MageState state = MageState.get(player.getUuid());
        float temporary = 0f;
        if (state.momentumTicks > 0 && stats.hasNode("mag_acc_momentum"))
            temporary += 0.03f * Math.min(3, state.momentumStacks);
        if (state.distortedTimeTicks > 0) temporary += 0.25f;
        if (state.accelerationTicks > 0) temporary += 0.10f;
        float cdr = Math.min(MageBalance.TEMPORARY_CDR_CAP, permanent + temporary);
        String spellId = event.getSpell() == null ? "" : event.getSpell().getSpellId();
        cdr = IronsSpellBalance.balanceCooldownReduction(spellId, cdr);
        int original = event.getEffectiveCooldown();
        int result = Math.max(1, Math.round(original * (1f - cdr)));
        event.setEffectiveCooldown(result);
    }

    /** Final accepted damage: feed resolved-hit hooks without entering the physical pipeline. */
    private static void damageResolved(LivingDamageEvent event) {
        if (!(event.getSource() instanceof SpellDamageSource source)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
        LivingEntity target = event.getEntity();
        if (event.getAmount() <= 0f) return;
        PlayerStats stats = StatsManager.get(player);
        if (stats.clazz != RPGClass.MAGO) return;

        MageState state = MageState.get(player.getUuid());
        state.beginCast("irons:" + source.spell().getSpellId(), mapSchool(source.spell().getSchoolType()), true);
        try {
            MageCombatHandler.onExternalSpellHit(player, target, event.getAmount(),
                    mapSchool(source.spell().getSchoolType()));
            IronsMageClassBridge.onResolvedHit(player, stats, target, source.spell().getSpellId(),
                    source.spell().getSchoolType(), event.getAmount());

            // MageCombatHandler already sums primary + scaled borrowed House lifesteal exactly once.
        } finally {
            state.endCast();
        }
    }

    private static void applyCastModifiers(ServerPlayerEntity player, PlayerStats stats, String spellId,
                                           MageState.School school) {
        String path = spellPath(spellId);
        MageState state = MageState.get(player.getUuid());
        if ((path.equals("ice_block") || path.equals("shield")) && stats.hasNode("mag_cryo_ice_barrier"))
            state.iceGuardTicks = Math.max(state.iceGuardTicks, 120);
        if ((path.equals("shield") || path.equals("fang_ward")) && stats.hasNode("mag_astral_shield"))
            state.astralGuardTicks = Math.max(state.astralGuardTicks, stats.hasNode("mag_astral_core") ? 138 : 120);
        if (path.equals("burning_dash") && stats.hasNode("mag_pyr_ash_step"))
            state.fireStepTicks = Math.max(state.fireStepTicks, 80);
        if ((path.equals("thunder_step") || path.equals("charge")) && stats.hasNode("mag_storm_lightning_step")) {
            state.lightningStepTicks = Math.max(state.lightningStepTicks, 80);
            state.accelerationTicks = Math.max(state.accelerationTicks, 60);
        }
        if (MOBILITY_SPELLS.contains(path)) {
            if (AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_mobility_weaving") > 0f)
                state.weavingTicks = Math.max(state.weavingTicks, 80);
            if (AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_teleport_weaving") > 0f)
                state.teleportBonusTicks = Math.max(state.teleportBonusTicks, 80);
            if (stats.hasNode("mag_illu_illusory_step") && stats.hasNode("mag_illu_ghost_attack"))
                state.ghostAttackTicks = Math.max(state.ghostAttackTicks, 80);
            if (AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_hex_mobility") > 0f)
                state.blinkStrikeTicks = Math.max(state.blinkStrikeTicks, 100);
            if (AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_temporal_mobility") > 0f)
                state.momentumTicks = Math.max(state.momentumTicks, 100);
        }
        if (path.equals("invisibility") && stats.hasNode("mag_illu_ghost_attack"))
            state.ghostAttackTicks = Math.max(state.ghostAttackTicks, 80);
        if (ILLUSION_SPELLS.contains(path)) {
            int charges = Math.round(AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_illusion_charges"));
            if (charges > 0) {
                state.illusionCharges = Math.max(state.illusionCharges, charges);
                state.illusionTicks = Math.max(state.illusionTicks, 120);
            }
        }
        if ((path.equals("haste") || path.equals("charge"))
                && AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_haste_momentum") > 0f) {
            state.accelerationTicks = Math.max(state.accelerationTicks, 100);
        }
        if (path.equals("counterspell")
                && AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_counterspell_guard") > 0f) {
            CombatState combat = CombatState.get(player.getUuid());
            combat.castGuardTicks = Math.max(combat.castGuardTicks, 40);
        }
        if (school == MageState.School.SUMMON) {
            switch (state.spiritMode) {
                case 0 -> {
                    if (AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_spirit_life") > 0f)
                        state.spiritLifeTicks = Math.max(state.spiritLifeTicks, 240);
                }
                case 1 -> {
                    if (AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_spirit_earth") > 0f)
                        state.spiritEarthTicks = Math.max(state.spiritEarthTicks, 240);
                }
                default -> {
                    if (AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_spirit_hunt") > 0f)
                        state.spiritHuntTicks = Math.max(state.spiritHuntTicks, 240);
                }
            }
        }
    }

    private static String spellPath(String spellId) {
        if (spellId == null) return "";
        int separator = spellId.indexOf(':');
        return separator >= 0 ? spellId.substring(separator + 1) : spellId;
    }

    private static final class IronsManaBridge implements MageManaBridge {
        @Override
        public boolean sync(ServerPlayerEntity player, PlayerStats stats) {
            IronsMageClassBridge.applyTemporaryCastAttributes(player, stats);
            if (player.age % 20 == 0) applyAttributes(player, stats);
            MagicData data = MagicData.getPlayerMagicData(player);
            float max = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
            if (data.getMana() > max) data.setMana(max);
            float current = data.getMana();
            boolean changed = Math.abs(stats.resource - current) > 0.01f
                    || Math.abs(stats.resourceMax - max) > 0.01f;
            stats.resource = current;
            stats.resourceMax = max;
            return changed;
        }

        @Override
        public int nativeSummonCount(ServerPlayerEntity player, PlayerStats stats) { return IronsNativeSummons.count(player, stats); }
        @Override
        public float nativeSummonDiversityBonus(ServerPlayerEntity player, PlayerStats stats) { return IronsNativeSummons.diversityBonus(player, stats); }
        @Override
        public boolean focusNativeSummons(ServerPlayerEntity player, PlayerStats stats, net.minecraft.entity.LivingEntity target) { return IronsNativeSummons.focus(player, stats, target); }
        @Override
        public boolean transferNativeSummons(ServerPlayerEntity player, PlayerStats stats) { return IronsNativeSummons.transfer(player, stats); }
        @Override
        public float nativeIncomingMultiplier(ServerPlayerEntity player, PlayerStats stats) { return IronsNativeSummons.incomingMultiplier(player, stats); }

        @Override
        public float current(ServerPlayerEntity player) {
            return MagicData.getPlayerMagicData(player).getMana();
        }

        @Override
        public void set(ServerPlayerEntity player, float amount) {
            MagicData data = MagicData.getPlayerMagicData(player);
            float max = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
            data.setMana(Math.max(0f, Math.min(max, amount)));
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
        }

        @Override
        public boolean isCasting(ServerPlayerEntity player) {
            return MagicData.getPlayerMagicData(player).isCasting();
        }

        private static void applyAttributes(ServerPlayerEntity player, PlayerStats stats) {
            EntityAttributeInstance maxMana = player.getAttributeInstance(AttributeRegistry.MAX_MANA.get());
            if (maxMana == null) return;
            if (maxMana.getModifier(RPG_MAX_MANA) != null) maxMana.removeModifier(RPG_MAX_MANA);
            float flat = AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_flat");
            float pct = Math.min(0.35f, Math.max(0f,
                    AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_max_pct")));
            int intelligence = stats.totalStats().getOrDefault(Stat.INTELIGENCIA, 0);
            float rpgBase = (MageBalance.BASE_MANA
                    + intelligence * MageBalance.MANA_PER_INTELLIGENCE + flat) * (1f + pct);
            if (stats.awakened) rpgBase += 3f;

            // Iron's continua sendo a autoridade do pool. Removemos primeiro somente o nosso
            // modifier; tudo o que restar (base do Iron, armadura, curios, spellbook etc.) e preservado.
            // O cap de 280 vale para a parcela do RPG, nunca para os +500/+N fornecidos por equipamento.
            double withoutRpg = maxMana.getValue();
            double rpgAddition = Math.max(0d, rpgBase - MageBalance.BASE_MANA);
            rpgAddition = Math.min(MageBalance.MAX_RPG_MANA_BONUS_WITH_IRONS, rpgAddition);
            if (rpgAddition > 0.001d) {
                maxMana.addTemporaryModifier(new EntityAttributeModifier(RPG_MAX_MANA,
                        "RPG Stats Mage mana", rpgAddition, EntityAttributeModifier.Operation.ADDITION));
            }

            // Conjuration must influence real Iron summons through the attribute Iron itself reads.
            IronsMageClassBridge.applyIronsAttributes(player, stats);

            EntityAttributeInstance regen = player.getAttributeInstance(AttributeRegistry.MANA_REGEN.get());
            if (regen == null) return;
            if (regen.getModifier(RPG_MANA_REGEN) != null) regen.removeModifier(RPG_MANA_REGEN);
            float max = Math.max(1f, (float) maxMana.getValue());
            float regenFlat = AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_regen_flat");
            float regenPct = Math.max(0f, AbilityRegistry.sumPassive(stats.unlockedNodes, "mana_regen_pct"));
            float desiredPerSecond = (MageBalance.BASE_MANA_REGEN_PER_SECOND + regenFlat) * (1f + regenPct);
            double rpgBonus = Math.max(0d, desiredPerSecond / (max * 0.02f) - 1d);
            rpgBonus = Math.min(2d, rpgBonus);
            if (rpgBonus > 0.001d) {
                regen.addTemporaryModifier(new EntityAttributeModifier(RPG_MANA_REGEN,
                        "RPG Stats Mage mana regen", rpgBonus, EntityAttributeModifier.Operation.ADDITION));
            }
        }
    }

    private static float ironNodeMultiplier(PlayerStats stats, String spellPath, MageState.School school) {
        float bonus = 0f;
        if (Set.of("gust", "fang_swirl", "shockwave").contains(spellPath))
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_evocation_pulse");
        if (school == MageState.School.LIGHTNING
                && Set.of("ball_lightning", "thunderstorm", "shockwave").contains(spellPath))
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_lightning_area");
        if (Set.of("black_hole", "gravity_fissure").contains(spellPath)) {
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_gravity_control");
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_gravity_ascension");
        }
        if (spellPath.equals("shadow_slash"))
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_hex_slash");
        if (spellPath.equals("echoing_strikes"))
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_astral_offense");
        if (Set.of("magic_missile", "magic_arrow").contains(spellPath))
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_astral_projectile");
        if (Set.of("echoing_strikes", "magic_missile", "magic_arrow").contains(spellPath))
            bonus += AbilityRegistry.sumPassive(stats.unlockedNodes, "irons_astral_ascension");
        return 1f + Math.min(0.18f, Math.max(0f, bonus));
    }

    private static float secondarySchoolMultiplier(PlayerStats stats, String spellPath, MageState.School school) {
        float bonus = 0f;
        if (isElemental(school)) {
            bonus += Math.min(0.20f, AbilityRegistry.sumPassive(stats.unlockedNodes, "elemental_damage", true));
        }
        if (school == MageState.School.SUMMON && IronsMageClassBridge.isCombatSummon(spellPath)) {
            // Nature is broader than summons in Iron's. Only actual combat-summon spells receive
            // Conjuration damage here; Animist Nature/Holy interactions are handled by the class bridge.
            bonus += Math.min(0.20f, AbilityRegistry.sumPassive(stats.unlockedNodes, "summon_damage"));
        }
        return 1f + Math.max(0f, bonus);
    }

    private static boolean isElemental(MageState.School school) {
        return school == MageState.School.FIRE || school == MageState.School.ICE || school == MageState.School.LIGHTNING;
    }

    private static MageState.School mapSchool(SchoolType school) {
        if (school == null || school.getId() == null) return MageState.School.ARCANE;
        return switch (school.getId().getPath()) {
            case "fire" -> MageState.School.FIRE;
            case "ice" -> MageState.School.ICE;
            case "lightning" -> MageState.School.LIGHTNING;
            case "blood" -> MageState.School.BLOOD;
            case "eldritch" -> MageState.School.OCCULT;
            case "ender" -> MageState.School.SPACE;
            case "nature" -> MageState.School.SUMMON;
            case "evocation", "holy" -> MageState.School.ARCANE;
            default -> MageState.School.ARCANE;
        };
    }

    private IronsSpellsCompat() {}
}
