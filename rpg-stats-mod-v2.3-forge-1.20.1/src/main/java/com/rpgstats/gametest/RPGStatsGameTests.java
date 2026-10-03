package com.rpgstats.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.combat.ClassMechanics;
import com.rpgstats.combat.CombatHandler;
import com.rpgstats.combat.CombatState;
import com.rpgstats.guide.RpgGuideBook;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import com.rpgstats.tree.SkillNode;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtElement;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runtime tests that deliberately pass through Minecraft's real LivingEntity.damage pipeline.
 *
 * These tests complement the fast Python balance validators: they are meant to catch cases where
 * a formula/registry looks correct but Forge events, Mixins or runtime state never apply it in game.
 */
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RPGStatsGameTests {
    private static final float DIRECT_HIT = 6.0f;
    private static final float EPSILON = 0.01f;
    private static final AtomicInteger TEST_PLAYER_IDS = new AtomicInteger();

    private RPGStatsGameTests() {}

    /**
     * Automatic registry smoke test. Every specialization must survive one real player damage
     * event. Adding a specialization to the enum automatically adds it to this runtime coverage.
     */
    @GameTest(templateName = "empty", tickLimit = 200)
    public static void everySpecializationRunsThroughRealDamage(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        int checked = 0;

        for (RPGSpecialization specialization : RPGSpecialization.values()) {
            reset(player, configured(specialization, false));
            CowEntity target = freshTarget(context, 2 + (checked % 3), 1, 2 + ((checked / 3) % 3));

            float dealt = damageTarget(player, target, DIRECT_HIT);
            context.assertTrue(Float.isFinite(dealt) && dealt > 0.0f,
                    specialization.name() + " did not deal finite positive runtime damage: " + dealt);

            target.discard();
            checked++;
        }

        context.assertTrue(checked == RPGSpecialization.values().length,
                "Specialization registry coverage was incomplete: " + checked + "/"
                        + RPGSpecialization.values().length);
        complete(context);
    }

    /**
     * Regression for the exact failure class that motivated runtime GameTests: Colosso da Dor
     * must turn damage actually received by the ServerPlayerEntity into war_pain, and that state
     * must increase a later real melee damage event.
     */
    @GameTest(templateName = "empty", tickLimit = 180)
    public static void painColossusConvertsRealDamageTakenIntoPressure(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);

        // Baseline: same level/spec/nodes, but no stored Pain.
        reset(player, configured(RPGSpecialization.PAIN_COLOSSUS, true));
        CowEntity baselineTarget = freshTarget(context, 2, 1, 2);
        float baseline = damageTarget(player, baselineTarget, DIRECT_HIT);
        context.assertTrue(baseline > 0.0f, "Colosso baseline hit dealt no damage");
        baselineTarget.discard();

        // A freshly connected ServerPlayerEntity has vanilla join invulnerability. Wait until that
        // protection expires so this test measures the real RPG incoming-damage lifecycle.
        reset(player, configured(RPGSpecialization.PAIN_COLOSSUS, true));
        context.waitAndRun(65, () -> {
            player.setHealth(player.getMaxHealth());
            float before = player.getHealth();
            boolean accepted = player.damage(player.getDamageSources().generic(), 8.0f);
            float incoming = before - player.getHealth();

            context.assertTrue(accepted && incoming > 0.0f,
                    "ServerPlayerEntity did not receive survival damage after join invulnerability");

            CombatState state = CombatState.get(player.getUuid());
            float pain = state.gauge("war_pain");
            context.assertTrue(pain > 0.0f,
                    "Colosso da Dor received damage but war_pain stayed at " + pain);

            CowEntity poweredTarget = freshTarget(context, 4, 1, 2);
            float powered = damageTarget(player, poweredTarget, DIRECT_HIT);
            context.assertTrue(powered > baseline + EPSILON,
                    "Colosso stored Pain did not increase real damage. baseline=" + baseline
                            + ", powered=" + powered + ", pain=" + pain);
            context.assertTrue(state.gauge("war_pain") < pain,
                    "Colosso confirmed-hit payoff did not consume stored Pain. before=" + pain
                            + ", after=" + state.gauge("war_pain"));

            poweredTarget.discard();
            complete(context);
        });
    }

    /** Regression: Berserker path techniques spend base cost + their extra Fury and persist both. */
    @GameTest(templateName = "empty", tickLimit = 80)
    public static void berserkerExtraFuryCostPersists(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = RPGClass.GUERREIRO;
        stats.path = RPGPath.WAR_BERSERKER;

        SkillNode technique = RPGPath.WAR_BERSERKER.nodes.stream()
                .filter(node -> node.id().endsWith("_technique"))
                .findFirst().orElseThrow();
        stats.unlockedNodes.add(technique.id());
        stats.refreshResourceMax();
        stats.resource = stats.resourceMax;
        stats.setActiveSlot(0, technique.id());
        float before = stats.resource;
        float baseCost = AbilityRegistry.activeForNode(technique.id()).resourceCost();
        reset(player, stats);

        CombatHandler.activateAbility(player, 0);
        PlayerStats after = StatsManager.get(player);
        float spent = before - after.resource;
        context.assertTrue(spent >= baseCost + 8.5f,
                "Berserker extra Fury was refunded/reloaded. base=" + baseCost + ", spent=" + spent);
        complete(context);
    }

    /** Regression: a prepared arrow cannot be consumed by a melee hit that never received its bonus. */
    @GameTest(templateName = "empty", tickLimit = 80)
    public static void preparedShotOnlyConsumesOnProjectile(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = RPGClass.ARQUEIRO;
        stats.path = RPGPath.ARC_MARKSMAN;
        reset(player, stats);

        CombatState state = CombatState.get(player.getUuid());
        state.startTimer("arc_prepared_shot", 100);
        ClassMechanics.consumePreparedHit(player, stats, false, false);
        context.assertTrue(state.timer("arc_prepared_shot") > 0,
                "Melee hit incorrectly consumed arc_prepared_shot");
        ClassMechanics.consumePreparedHit(player, stats, true, false);
        context.assertTrue(state.timer("arc_prepared_shot") == 0,
                "Projectile hit did not consume arc_prepared_shot");
        complete(context);
    }

    /** Regression: Opening must affect the confirmed Assassin hit before being cleaned up. */
    @GameTest(templateName = "empty", tickLimit = 100)
    public static void assassinOpeningSurvivesUntilConfirmedHit(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.NIGHTBLADE, true);
        stats.resource = 0f;
        reset(player, stats);
        CombatState state = CombatState.get(player.getUuid());
        state.startTimer("ass_opening", 80);

        CowEntity target = freshTarget(context, 2, 1, 2);
        float dealt = damageTarget(player, target, DIRECT_HIT);
        PlayerStats after = StatsManager.get(player);

        context.assertTrue(dealt > 0f, "Nightblade confirmed hit dealt no damage");
        context.assertTrue(after.resource > 1.5f,
                "Opening was cleared before Assassin resource gain; resource=" + after.resource);
        context.assertTrue(state.timer("ass_opening") == 0,
                "Opening was not consumed after confirmed hit");
        context.assertTrue(state.timer("ass_escape") > 0,
                "Nightblade confirmed Opening did not arm escape");
        target.discard();
        complete(context);
    }

    /**
     * Regression: same-target proc damage is queued until vanilla hurt resistance can accept it.
     * The direct melee hit must land first; the queued Colosso proc must reduce health later.
     */
    @GameTest(templateName = "empty", tickLimit = 120)
    public static void deferredSameTargetProcLandsAfterHurtResistance(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.PAIN_COLOSSUS, true);
        reset(player, stats);

        CombatState state = CombatState.get(player.getUuid());
        state.setGauge("war_pain", 4.0f, 10.0f);
        state.startTimer("war_pain_release", 60);

        CowEntity target = freshTarget(context, 2, 1, 2);
        float direct = damageTarget(player, target, DIRECT_HIT);
        float healthAfterDirect = target.getHealth();

        context.assertTrue(direct > 0.0f, "Direct hit did not land before deferred proc test");
        context.assertTrue(state.gauge("war_pain") < 4.0f,
                "Colosso did not consume Pain to queue the same-target proc");

        context.waitAndRun(14, () -> {
            context.assertTrue(target.getHealth() < healthAfterDirect - EPSILON,
                    "Deferred same-target proc never landed. afterDirect=" + healthAfterDirect
                            + ", afterDelay=" + target.getHealth());
            target.discard();
            complete(context);
        });
    }

    /** Regression: RPG state written to the player NBT must survive a real StatsManager save/get round-trip. */
    @GameTest(templateName = "empty", tickLimit = 80)
    public static void playerStatsRoundTripThroughPersistentNbt(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.RAGEBORN, true);

        SkillNode technique = RPGPath.WAR_BERSERKER.nodes.stream()
                .filter(node -> node.id().endsWith("_technique"))
                .findFirst().orElseThrow();
        stats.unlockedNodes.add(technique.id());
        stats.setActiveSlot(0, technique.id());
        stats.refreshResourceMax();
        float expectedResource = stats.resourceMax * 0.37f;
        stats.resource = expectedResource;

        StatsManager.save(player, stats);
        PlayerStats loaded = StatsManager.get(player);

        context.assertTrue(loaded.clazz == RPGClass.GUERREIRO, "Class was lost after NBT round-trip");
        context.assertTrue(loaded.path == RPGPath.WAR_BERSERKER, "Path was lost after NBT round-trip");
        context.assertTrue(loaded.specialization == RPGSpecialization.RAGEBORN,
                "Specialization was lost after NBT round-trip");
        context.assertTrue(Math.abs(loaded.resource - expectedResource) <= EPSILON,
                "Resource changed after NBT round-trip. expected=" + expectedResource
                        + ", actual=" + loaded.resource);
        context.assertTrue(loaded.unlockedNodes.contains(technique.id()),
                "Unlocked technique was lost after NBT round-trip");
        context.assertTrue(technique.id().equals(loaded.activeSlot(0)),
                "Active slot was lost after NBT round-trip");
        complete(context);
    }

    /** Duelist parry must ignore non-melee damage and only arm Riposte on a direct melee source. */
    @GameTest(templateName = "empty", tickLimit = 80)
    public static void duelistParryOnlyConsumesOnDirectMelee(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = RPGClass.ASSASSINO;
        stats.path = RPGPath.ASS_DUELIST;
        addNode(stats, RPGPath.ASS_DUELIST.nodes.get(0));
        reset(player, stats);

        CombatState state = CombatState.get(player.getUuid());
        state.startTimer("ass_parry", 100);
        float generic = ClassMechanics.modifyIncomingDamage(
                player, stats, 10.0f, player.getDamageSources().generic());
        context.assertTrue(Math.abs(generic - 10.0f) <= EPSILON,
                "Generic damage incorrectly received Duelist parry reduction: " + generic);
        context.assertTrue(state.timer("ass_parry") > 0,
                "Generic damage incorrectly consumed Duelist parry");

        CowEntity attacker = freshTarget(context, 3, 1, 2);
        float melee = ClassMechanics.modifyIncomingDamage(
                player, stats, 10.0f, player.getDamageSources().mobAttack(attacker));
        context.assertTrue(melee < 10.0f,
                "Direct melee damage did not receive Duelist parry reduction: " + melee);
        context.assertTrue(state.timer("ass_parry") > 0 && state.timer("ass_riposte") == 0,
                "Damage scaling consumed Parry before confirmation");
        ClassMechanics.onHurt(player, stats, melee, player.getDamageSources().mobAttack(attacker));
        context.assertTrue(state.timer("ass_parry") == 0,
                "Direct melee damage did not consume Duelist parry");
        context.assertTrue(state.timer("ass_riposte") > 0,
                "Successful Duelist parry did not arm Riposte");
        attacker.discard();
        complete(context);
    }

    /** Regression: Fencer Engine must inspect cadence before the Duelist path refreshes that timer. */
    @GameTest(templateName = "empty", tickLimit = 100)
    public static void fencerEngineTriggersOnFirstHitAfterCadenceBreak(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = RPGClass.ASSASSINO;
        stats.path = RPGPath.ASS_DUELIST;
        stats.specialization = RPGSpecialization.FENCER;
        addNode(stats, RPGPath.ASS_DUELIST.nodes.get(0));
        addNode(stats, RPGSpecialization.FENCER.nodes.get(0));
        addNode(stats, RPGSpecialization.FENCER.nodes.get(1));
        stats.refreshResourceMax();
        stats.resource = stats.resourceMax;
        reset(player, stats);

        CombatState state = CombatState.get(player.getUuid());
        context.assertTrue(state.timer("ass_duel_cadence") == 0,
                "Fresh Duelist unexpectedly started inside cadence");

        CowEntity target = freshTarget(context, 2, 1, 2);
        float dealt = damageTarget(player, target, DIRECT_HIT);
        context.assertTrue(dealt > 0.0f, "Fencer test hit dealt no damage");
        context.assertTrue(state.gauge("ass_advantage") >= 1.2f,
                "Fencer Engine missed the cadence break. advantage=" + state.gauge("ass_advantage"));
        context.assertTrue(state.timer("ass_duel_cadence") > 0,
                "Duelist hit did not refresh cadence after Fencer Engine evaluation");
        target.discard();
        complete(context);
    }

    /** Core Tempo and Determinacao must be actual runtime states, not description-only nodes. */
    @GameTest(templateName = "empty", tickLimit = 100)
    public static void archerCoreTempoAndResolveAreLive(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = RPGClass.ARQUEIRO;
        stats.unlockedNodes.add("arc_core_tempo");
        stats.unlockedNodes.add("arc_core_resolve");
        stats.refreshResourceMax();
        stats.resource = stats.resourceMax;
        reset(player, stats);

        var utility = AbilityRegistry.activeForNode("arc_core_utility");
        context.assertTrue(utility != null, "Archer core utility is not active");
        ClassMechanics.activate(player, stats, "arc_core_utility", utility);
        CombatState state = CombatState.get(player.getUuid());
        context.assertTrue(state.timer("class_tempo") > 0, "Core Tempo did not arm after an Archer technique");

        CowEntity target = freshTarget(context, 4, 1, 2);
        float tempoGain = ClassMechanics.resourceGainOnHit(player, target, stats, true, false);
        state.setTimer("class_tempo", 0);
        float normalGain = ClassMechanics.resourceGainOnHit(player, target, stats, true, false);
        context.assertTrue(tempoGain > normalGain, "Core Tempo did not add Focus to the prepared projectile");

        player.setHealth(player.getMaxHealth() * .30f);
        float incoming = ClassMechanics.modifyIncomingDamage(
                player, stats, 10f, player.getDamageSources().generic());
        context.assertTrue(incoming < 10f, "Core Resolve did not reduce low-health damage");
        context.assertTrue(state.timer("class_resolve") > 0 && state.timer("class_resolve_cd") > 0,
                "Core Resolve did not create its window/cooldown");
        target.discard();
        complete(context);
    }

    /** Arcane Discipline and Synergy must feed deterministic fractional mark progress. */
    @GameTest(templateName = "empty", tickLimit = 100)
    public static void archerArcaneDisciplineAndSynergyBuildMarks(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = RPGClass.ARQUEIRO;
        stats.path = RPGPath.ARC_ARCANE;
        stats.unlockedNodes.add("arc_magic_foundation");
        stats.unlockedNodes.add("arc_magic_discipline");
        stats.unlockedNodes.add("arc_magic_synergy");
        reset(player, stats);

        CombatState state = CombatState.get(player.getUuid());
        state.startTimer("arc_magic_synergy", 80);
        CowEntity target = freshTarget(context, 3, 1, 2);
        ClassMechanics.onHit(player, target, stats, 5f, player.getDamageSources().generic(), true, false);

        CombatState.ClassTargetState ts = state.classTarget(target.getUuid());
        context.assertTrue(ts.stacks == 1 && ts.stackProgress > .90f,
                "Arcane fractional progress was not preserved. stacks=" + ts.stacks + ", progress=" + ts.stackProgress);
        context.assertTrue(state.timer("arc_magic_synergy") == 0,
                "Arcane Synergy was not consumed by the projectile");

        ClassMechanics.onHit(player, target, stats, 5f, player.getDamageSources().generic(), true, false);
        context.assertTrue(ts.stacks >= 3,
                "Arcane Discipline did not convert fractional progress into extra marks. stacks=" + ts.stacks);
        target.discard();
        complete(context);
    }

    /** Venom Setup and Signature must form a complete Ready -> Consume loop. */
    @GameTest(templateName = "empty", tickLimit = 100)
    public static void assassinVenomPreparedSignatureConsumesDoses(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = RPGClass.ASSASSINO;
        stats.path = RPGPath.ASS_VENOM;
        stats.unlockedNodes.add("ass_venom_foundation");
        stats.unlockedNodes.add("ass_venom_setup");
        stats.unlockedNodes.add("ass_venom_signature");
        reset(player, stats);

        CombatState state = CombatState.get(player.getUuid());
        CowEntity target = freshTarget(context, 2, 1, 2);
        CombatState.ClassTargetState ts = state.classTarget(target.getUuid());
        ts.doses = 4;
        ts.statusTicks = 140;
        state.startTimer("ass_venom_ready", 80);

        var signature = AbilityRegistry.activeForNode("ass_venom_signature");
        context.assertTrue(signature != null, "Venom signature is not active");
        ClassMechanics.activate(player, stats, "ass_venom_signature", signature);
        context.assertTrue(state.timer("ass_venom_ready") == 0
                        && state.timer("ass_venom_consume") > 0
                        && state.timer("ass_venom_empowered") > 0,
                "Venom Ready was not converted into an empowered consumable signature");

        ClassMechanics.onHit(player, target, stats, 5f, player.getDamageSources().generic(), false, false);
        context.assertTrue(state.timer("ass_venom_consume") == 0,
                "Venom signature was not consumed by the next melee hit");
        context.assertTrue(ts.doses <= 1,
                "Empowered Venom signature did not spend up to four doses. remaining=" + ts.doses);
        target.discard();
        complete(context);
    }

    /** The guide must be a valid vanilla written book and reflect the player's current build. */
    @GameTest(templateName = "empty", tickLimit = 100)
    public static void guideBookIsValidAndContextAware(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats();
        stats.level = 30;
        stats.awakened = true;
        stats.clazz = RPGClass.ARQUEIRO;
        stats.path = RPGPath.ARC_ARCANE;
        stats.specialization = RPGSpecialization.FLAMEBOW;
        stats.affinityHouse = RPGPath.ARC_MARKSMAN;
        reset(player, stats);

        ItemStack book = RpgGuideBook.create(player);
        context.assertTrue(RpgGuideBook.isGuide(book), "Guide marker is missing");
        context.assertTrue(WrittenBookItem.isValid(book.getNbt()), "Generated guide is not a valid written book");

        var pages = book.getNbt().getList("pages", NbtElement.STRING_TYPE);
        context.assertTrue(pages.size() >= 90 && pages.size() <= 100,
                "Codex page count must stay encyclopedia-sized but vanilla-safe: " + pages.size());
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) joined.append(pages.getString(i));
        String all = joined.toString();

        context.assertTrue(all.contains("change_page"),
                "Codex does not contain clickable CHANGE_PAGE navigation");
        context.assertTrue(all.contains("Guerreiro") && all.contains("Mago")
                        && all.contains("Arqueiro") && all.contains("Assassino"),
                "Codex omitted one or more classes");
        context.assertTrue(all.contains("Arqueiro Arcano"), "Codex omitted current House");
        context.assertTrue(all.contains("Arco") && all.contains("gneo"), "Codex omitted current specialization");
        context.assertTrue(all.contains("Atirador"), "Codex omitted secondary House");
        context.assertTrue(all.contains("Contra-L") && all.contains("Piromante"),
                "Codex omitted specialization encyclopedia entries");
        context.assertTrue(all.contains("Fun") && all.contains("Motor")
                        && all.contains("T") && all.contains("Ascens"),
                "Codex specialization pages lost the standardized ability specification");
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 80)
    public static void crossbowTechniqueRejectsBowWithoutSpending(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.CROSSBOW_EXPERT, true);
        String id = "arc_art_cross_technique";
        stats.unlockedNodes.add(id);
        stats.setActiveSlot(0, id);
        reset(player, stats);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        float before = StatsManager.get(player).resource;
        CombatHandler.activateAbility(player, 0);
        context.assertTrue(Math.abs(StatsManager.get(player).resource - before) < EPSILON,
                "Crossbow technique spent resource with a bow equipped");
        context.assertTrue(CombatState.get(player.getUuid()).cooldown(id) == 0,
                "Rejected crossbow technique started cooldown");
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 80)
    public static void simultaneousArrowsRewardOnlyOneShot(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.DEADEYE, true);
        stats.resource = 0f;
        reset(player, stats);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        CowEntity first = freshTarget(context, 2, 1, 2);
        CowEntity second = freshTarget(context, 4, 1, 2);
        net.minecraft.entity.projectile.ArrowEntity a = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
        net.minecraft.entity.projectile.ArrowEntity b = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
        com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack());
        player.getServerWorld().spawnEntity(a);
        player.getServerWorld().spawnEntity(b);
        com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
        first.damage(player.getDamageSources().arrow(a, player), 2f);
        float resource = StatsManager.get(player).resource;
        float precision = CombatState.get(player.getUuid()).gauge("arc_precision");
        context.assertTrue(resource > 0, "First real arrow did not generate Focus");
        second.damage(player.getDamageSources().arrow(b, player), 2f);
        context.assertTrue(Math.abs(StatsManager.get(player).resource - resource) < EPSILON,
                "Multishot generated resource per arrow");
        context.assertTrue(Math.abs(CombatState.get(player.getUuid()).gauge("arc_precision") - precision) < EPSILON,
                "Multishot advanced precision per arrow");
        a.discard(); b.discard(); first.discard(); second.discard();
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 120)
    public static void nativeOwnedTrapCaptureMarksOnlyOwnersPrey(TestContext context) {
        if (!Boolean.getBoolean("rpgstats.archerCompatTests")) { complete(context); return; }
        context.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded("beartrapmod"), "Compat profile omitted Simply Bear Traps");
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.TRAPPER, true);
        String id = "arc_ward_trap_ascension";
        String signature = "arc_ward_trap_signature";
        stats.unlockedNodes.add(signature); stats.setActiveSlot(1, signature);
        stats.unlockedNodes.add(id); stats.setActiveSlot(0, id); reset(player, stats);
        var type = net.minecraft.registry.Registries.ENTITY_TYPE.get(new net.minecraft.util.Identifier("beartrapmod", "bear_trap"));
        var own = type.create(player.getWorld());
        var foreign = type.create(player.getWorld());
        context.assertTrue(own != null && foreign != null, "Installed native trap entity did not create");
        BlockPos a = context.getAbsolutePos(new BlockPos(3, 1, 3));
        BlockPos b = context.getAbsolutePos(new BlockPos(6, 1, 3));
        own.refreshPositionAndAngles(a.getX() + .5, a.getY(), a.getZ() + .5, 0, 0);
        foreign.refreshPositionAndAngles(b.getX() + .5, b.getY(), b.getZ() + .5, 0, 0);
        try {
            own.getClass().getMethod("setOwnerUuid", UUID.class).invoke(own, player.getUuid());
            foreign.getClass().getMethod("setOwnerUuid", UUID.class).invoke(foreign, UUID.randomUUID());
        } catch (ReflectiveOperationException e) { throw new AssertionError("Pinned trap owner API changed", e); }
        player.getServerWorld().spawnEntity(own); player.getServerWorld().spawnEntity(foreign);
        CombatHandler.activateAbility(player, 0);
        var prey = context.spawnMob(EntityType.ZOMBIE, 3, 1, 3); prey.setAiDisabled(true);
        var other = context.spawnMob(EntityType.ZOMBIE, 6, 1, 3); other.setAiDisabled(true);
        prey.refreshPositionAndAngles(own.getX(), own.getY(), own.getZ(), 0, 0);
        other.refreshPositionAndAngles(foreign.getX(), foreign.getY(), foreign.getZ(), 0, 0);
        context.waitAndRun(6, () -> {
            context.assertTrue(CombatState.get(player.getUuid()).classTarget(prey.getUuid()).markTicks > 0,
                    "Real owned trap captured prey but RPG did not mark it");
            context.assertTrue(CombatState.get(player.getUuid()).classTarget(other.getUuid()).markTicks == 0,
                    "Foreign trap capture granted an owner reward");
            // A signature has no reposition requirement even while the ascension is active.
            // Refill the fixture to isolate overlapping skill semantics from their combined cost.
            PlayerStats ready = StatsManager.get(player); ready.resource = ready.resourceMax; StatsManager.save(player, ready);
            context.assertTrue(com.rpgstats.integration.ArcherTrapCompat.isMarked(player, prey), "Capture adapter lost its own prey mark");
            context.assertTrue(signature.equals(StatsManager.get(player).activeSlot(1)), "Capture signature slot did not persist");
            context.assertTrue(com.rpgstats.combat.ArcherTechniqueHandler.canActivate(player, ready, signature),
                    "Capture prerequisite failed: visible=" + player.canSee(prey) + ", alive=" + prey.isAlive() + ", distance=" + player.squaredDistanceTo(prey));
            CombatHandler.activateAbility(player, 1);
            context.assertTrue(CombatState.get(player.getUuid()).cooldown(signature) > 0, "Capture signature was not accepted: resource=" + ready.resource + "/" + ready.resourceMax);
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
            var arrow = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
            com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack());
            player.getServerWorld().spawnEntity(arrow);
            com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
            prey.timeUntilRegen = 0;
            prey.damage(player.getDamageSources().arrow(arrow, player), 1f);
            context.assertTrue(!com.rpgstats.integration.ArcherTrapCompat.isMarked(player, prey),
                    "Territorial ascension blocked stationary Capture signature");
            own.discard(); foreign.discard(); prey.discard(); other.discard(); arrow.discard();
            complete(context);
        });
    }

    @GameTest(templateName = "empty", tickLimit = 80)
    public static void cancelledArrowDoesNotApplyControl(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        reset(player, configured(RPGSpecialization.FROSTBOW, true));
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        CowEntity target = freshTarget(context, 3, 1, 3);
        CombatState.get(player.getUuid()).startTimer("arc_elemental_reaction", 100);
        net.minecraft.entity.projectile.ArrowEntity arrow = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
        com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack());
        player.getServerWorld().spawnEntity(arrow);
        com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
        java.util.function.Consumer<net.minecraftforge.event.entity.living.LivingHurtEvent> cancel = event -> {
            if (event.getEntity() == target) event.setCanceled(true);
        };
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST,
                false, net.minecraftforge.event.entity.living.LivingHurtEvent.class, cancel);
        float before = target.getHealth();
        try {
            target.damage(player.getDamageSources().arrow(arrow, player), 2f);
        } finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(cancel); }
        context.assertTrue(target.getHealth() == before, "Cancellation fixture did not cancel damage");
        context.assertTrue(!target.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS),
                "Cancelled arrow applied specialization control");
        target.discard(); arrow.discard(); complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 120)
    public static void installedBowsKeepNativeVolleyAndAmmo(TestContext context) {
        if (!Boolean.getBoolean("rpgstats.archerCompatTests")) { complete(context); return; }
        context.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded("beartrapmod"), "Compat profile omitted Simply Bear Traps");
        context.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded("too_many_bows"), "Compat profile omitted Too Many Bows");
        var more = net.minecraftforge.fml.ModList.get().getMods().stream().filter(m ->
                m.getDisplayName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]", "").contains("morebowsandarrows"))
                .findFirst().orElseThrow(() -> new AssertionError("Compat profile omitted More Bows and Arrows"));
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats nativeStats = configured(RPGSpecialization.DEADEYE, true);
        nativeStats.resource = 0f; reset(player, nativeStats);
        net.minecraft.item.Item nativeBow = net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier("too_many_bows", "arcane_bow"));
        context.assertTrue(nativeBow instanceof net.minecraft.item.BowItem, "Pinned Arcane Bow is missing");
        var tntId = new net.minecraft.util.Identifier("more_bows_and_arrows", "tnt_arrow");
        context.assertTrue(net.minecraft.registry.Registries.ENTITY_TYPE.containsId(tntId), "Pinned More Bows TNT ammo registry changed");
        context.assertTrue(net.minecraft.registry.Registries.ENTITY_TYPE.get(tntId).isIn(net.minecraft.registry.tag.TagKey.of(
                net.minecraft.registry.RegistryKeys.ENTITY_TYPE, new net.minecraft.util.Identifier("rpgstats", "native_area_projectiles"))),
                "TNT ammo lacks native area classification");
        ItemStack stack = new ItemStack(nativeBow);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, stack);
        player.getInventory().setStack(5, new ItemStack(net.minecraft.item.Items.ARROW, 8));
        com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, stack);
        ((net.minecraft.item.BowItem) nativeBow).onStoppedUsing(stack, player.getWorld(), player, nativeBow.getMaxUseTime(stack) - 20);
        com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
        var volley = player.getServerWorld().getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class,
                player.getBoundingBox().expand(4), e -> e.getOwner() == player);
        context.assertTrue(volley.size() > 1, "Native Arcane Bow lost its multishot volley");
        context.assertTrue(player.getInventory().getStack(5).getCount() == 7, "Native bow did not consume exactly one ammo");
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.IRON_SWORD));
        float resource = 0f;
        for (int i = 0; i < volley.size(); i++) {
            CowEntity target = freshTarget(context, 2 + i % 3, 1, 4);
            target.damage(player.getDamageSources().arrow((net.minecraft.entity.projectile.PersistentProjectileEntity) volley.get(i), player), 2f);
            if (i == 0) resource = StatsManager.get(player).resource;
            else context.assertTrue(Math.abs(StatsManager.get(player).resource - resource) < EPSILON,
                    "Native volley generated per-pellet RPG resource after swapping weapon");
            target.discard(); volley.get(i).discard();
        }
        var extraBow = net.minecraft.registry.Registries.ITEM.stream().filter(item ->
                net.minecraft.registry.Registries.ITEM.getId(item).getNamespace().equals(more.getModId())
                && item instanceof net.minecraft.item.BowItem).findFirst()
                .orElseThrow(() -> new AssertionError("More Bows registered no compatible bow"));
        ItemStack extra = new ItemStack(extraBow);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, extra);
        player.getInventory().setStack(5, new ItemStack(net.minecraft.item.Items.ARROW, 8));
        com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, extra);
        ((net.minecraft.item.BowItem) extraBow).onStoppedUsing(extra, player.getWorld(), player, extraBow.getMaxUseTime(extra) - 20);
        com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
        var shots = player.getServerWorld().getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class,
                player.getBoundingBox().expand(4), e -> e.getOwner() == player);
        context.assertTrue(!shots.isEmpty(), "More Bows did not launch its actual projectile");
        context.assertTrue(player.getInventory().getStack(5).getCount() < 8, "More Bows did not consume native ammunition");
        shots.forEach(net.minecraft.entity.Entity::discard); complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 80)
    public static void trapTechniqueRejectsWithoutOwnedTrap(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.TRAPPER, true);
        String id = "arc_ward_trap_technique";
        stats.unlockedNodes.add(id); stats.setActiveSlot(0, id); reset(player, stats);
        float before = StatsManager.get(player).resource;
        CombatHandler.activateAbility(player, 0);
        context.assertTrue(Math.abs(StatsManager.get(player).resource - before) < EPSILON,
                "Trap preparation spent resource without any owned placed trap");
        context.assertTrue(CombatState.get(player.getUuid()).cooldown(id) == 0,
                "Failed trap preparation started cooldown");
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 100)
    public static void offhandCrossbowRecordsActualWeapon(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        reset(player, configured(RPGSpecialization.CROSSBOW_EXPERT, true));
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        ItemStack crossbow = new ItemStack(net.minecraft.item.Items.CROSSBOW);
        net.minecraft.nbt.NbtList ammo = new net.minecraft.nbt.NbtList();
        ammo.add(new ItemStack(net.minecraft.item.Items.ARROW).writeNbt(new net.minecraft.nbt.NbtCompound()));
        crossbow.getOrCreateNbt().put("ChargedProjectiles", ammo); crossbow.getOrCreateNbt().putBoolean("Charged", true);
        player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, crossbow);
        net.minecraft.item.CrossbowItem.shootAll(player.getWorld(), player, net.minecraft.util.Hand.OFF_HAND, crossbow, 3.15f, 1f);
        var arrows = player.getServerWorld().getEntitiesByClass(net.minecraft.entity.projectile.PersistentProjectileEntity.class,
                player.getBoundingBox().expand(4), e -> e.getOwner() == player);
        context.assertTrue(!arrows.isEmpty(), "Offhand crossbow launched no arrow");
        var source = player.getDamageSources().arrow(arrows.get(0), player);
        context.assertTrue(com.rpgstats.combat.ArcherShotTracker.launchWeapon(source, player).isOf(net.minecraft.item.Items.CROSSBOW),
                "Offhand crossbow was misclassified as held mainhand bow");
        arrows.forEach(net.minecraft.entity.Entity::discard); complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 100)
    public static void thrownItemDoesNotClaimBowTechnique(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.DEADEYE, true); stats.resource = 0; reset(player, stats);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        var snowball = new net.minecraft.entity.projectile.thrown.SnowballEntity(player.getWorld(), player);
        player.getServerWorld().spawnEntity(snowball);
        CowEntity target = freshTarget(context, 3, 1, 3);
        var source = player.getDamageSources().thrown(snowball, player);
        context.assertTrue(!com.rpgstats.combat.ArcherShotTracker.isBowShot(source), "Throwable was classified as a bow shot");
        target.damage(source, 2f);
        context.assertTrue(StatsManager.get(player).resource == 0, "Throwable generated Archer resource");
        snowball.discard(); target.discard(); complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 100)
    public static void realBowReleaseRecordsLaunchBeforeWeaponSwap(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        reset(player, configured(RPGSpecialization.SNIPER, true));
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        player.getInventory().setStack(1, new ItemStack(net.minecraft.item.Items.ARROW, 8));
        player.setCurrentHand(net.minecraft.util.Hand.MAIN_HAND);
        // No movement packets arrive through EmbeddedChannel. Drive the real player tick that
        // ServerPlayNetworkHandler normally invokes, so vanilla's draw countdown advances.
        for (int tick = 0; tick < 20; tick++) player.playerTick();
        context.assertTrue(player.isUsingItem() && player.getItemUseTime() >= 20,
                "Fixture did not advance the real item-use countdown: " + player.getItemUseTime());
        {
            player.stopUsingItem();
            var arrows = player.getServerWorld().getEntitiesByClass(net.minecraft.entity.projectile.PersistentProjectileEntity.class,
                    player.getBoundingBox().expand(4), e -> e.getOwner() == player);
            context.assertTrue(!arrows.isEmpty(), "Actual drawn-bow release launched no arrow");
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.IRON_SWORD));
            var source = player.getDamageSources().arrow(arrows.get(0), player);
            context.assertTrue(com.rpgstats.combat.ArcherShotTracker.isBowShot(source), "Actual release bypassed launch tracking");
            context.assertTrue(com.rpgstats.combat.ArcherShotTracker.launchWeapon(source, player).isOf(net.minecraft.item.Items.BOW),
                    "Weapon swap replaced the actual launch weapon");
            arrows.forEach(net.minecraft.entity.Entity::discard);
            complete(context);
        }
    }

    private static ServerPlayerEntity testPlayer(TestContext context) { return TestPlayers.create(context); }
    private static void complete(TestContext context) {
        TestPlayers.finish(context);
        context.complete();
    }

    private static PlayerStats configured(RPGSpecialization specialization, boolean fullEngine) {
        PlayerStats stats = new PlayerStats();
        stats.level = StatsManager.MAX_LEVEL;
        stats.awakened = true;
        stats.clazz = specialization.parent.parent;
        stats.path = specialization.parent;
        stats.specialization = specialization;

        // Initiation is the universal enable switch for specialization runtime mechanics.
        addNode(stats, specialization.nodes.get(0));
        if (fullEngine) {
            for (SkillNode node : specialization.nodes) {
                if (node.id().endsWith("_engine") || node.id().endsWith("_conversion")) addNode(stats, node);
            }
        }

        stats.refreshResourceMax();
        stats.resource = stats.resourceMax;
        stats.stamina = stats.staminaMax;
        return stats;
    }

    private static void addNode(PlayerStats stats, SkillNode node) {
        if (node != null) stats.unlockedNodes.add(node.id());
    }

    private static void reset(ServerPlayerEntity player, PlayerStats stats) {
        CombatState.remove(player.getUuid());
        StatsManager.save(player, stats);
        player.setHealth(player.getMaxHealth());
    }

    private static CowEntity freshTarget(TestContext context, int x, int y, int z) {
        CowEntity target = context.spawnMob(EntityType.COW, x, y, z);
        target.setAiDisabled(true);
        target.setHealth(target.getMaxHealth());
        return target;
    }

    private static float damageTarget(ServerPlayerEntity player, CowEntity target, float amount) {
        float before = target.getHealth();
        boolean accepted = target.damage(target.getDamageSources().playerAttack(player), amount);
        float dealt = before - target.getHealth();
        if (!accepted) return 0.0f;
        return dealt;
    }
}
