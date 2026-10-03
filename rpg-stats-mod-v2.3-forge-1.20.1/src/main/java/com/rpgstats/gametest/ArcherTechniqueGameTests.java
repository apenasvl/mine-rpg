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

@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcherTechniqueGameTests {
    private static final AtomicInteger TEST_PLAYER_IDS = new AtomicInteger();
    @GameTest(templateName = "empty", tickLimit = 80)
    public static void windTechniqueActuallyRepositions(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.WINDRUNNER, true);
        reset(player, stats);
        String id = RPGSpecialization.WINDRUNNER.nodes.stream().filter(n -> n.id().endsWith("_technique")).findFirst().orElseThrow().id();
        ClassMechanics.activate(player, stats, id, AbilityRegistry.activeForNode(id));
        context.assertTrue(player.getVelocity().horizontalLengthSquared() > .10,
                "Windrunner technique did not dash: " + player.getVelocity());
        complete(context);
    }
    @GameTest(templateName = "empty", tickLimit = 80)
    public static void survivalSignatureSpendsInstinctToHeal(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.SURVIVALIST, true);
        reset(player, stats);
        player.setHealth(player.getMaxHealth() * .4f);
        float before = player.getHealth();
        CombatState state = CombatState.get(player.getUuid());
        state.setGauge("arc_instinct", 4f, 10f);
        String id = RPGSpecialization.SURVIVALIST.nodes.stream().filter(n -> n.id().endsWith("_signature")).findFirst().orElseThrow().id();
        ClassMechanics.activate(player, stats, id, AbilityRegistry.activeForNode(id));
        context.assertTrue(player.getHealth() > before, "Survival signature did not heal");
        context.assertTrue(state.gauge("arc_instinct") < 4f, "Healing did not spend Instinct");
        complete(context);
    }
    @GameTest(templateName = "empty", tickLimit = 80)
    public static void allArcherActiveTooltipsExplainDistinctActions(TestContext context) {
        for (RPGSpecialization spec : RPGSpecialization.values()) {
            if (spec.parent.parent != RPGClass.ARQUEIRO) continue;
            String prefix = spec.nodes.get(0).id().replace("_initiation", "");
            String technique = com.rpgstats.ability.ClassAbilityRegistry.description(prefix + "_technique");
            String signature = com.rpgstats.ability.ClassAbilityRegistry.description(prefix + "_signature");
            String ascension = com.rpgstats.ability.ClassAbilityRegistry.description(prefix + "_ascension");
            // Removing labels and duration must still leave different player actions.
            String a = technique.replaceAll("ATIVA|DOMINIO|ASCENSAO|[0-9]+([,.][0-9]+)?s", "");
            String b = signature.replaceAll("ATIVA|DOMINIO|ASCENSAO|[0-9]+([,.][0-9]+)?s", "");
            String c = ascension.replaceAll("ATIVA|DOMINIO|ASCENSAO|[0-9]+([,.][0-9]+)?s", "");
            context.assertTrue(!a.equals(b) && !a.equals(c) && !b.equals(c),
                    spec.name() + " still advertises the same action with longer timers");
        }
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 80)
    public static void sniperSignatureMarksHostilePriorityTarget(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.SNIPER, true);
        reset(player, stats);
        net.minecraft.entity.mob.ZombieEntity hostile = context.spawnMob(EntityType.ZOMBIE, 1, 1, 4);
        hostile.setAiDisabled(true);
        CowEntity passive = context.spawnMob(EntityType.COW, 1, 1, 2);
        passive.setAiDisabled(true);
        String id = RPGSpecialization.SNIPER.nodes.stream().filter(n -> n.id().endsWith("_signature")).findFirst().orElseThrow().id();
        ClassMechanics.activate(player, stats, id, AbilityRegistry.activeForNode(id));
        CombatState state = CombatState.get(player.getUuid());
        context.assertTrue(state.classTarget(hostile.getUuid()).markTicks > 0, "Priority signature did not mark visible hostile");
        context.assertTrue(state.classTarget(passive.getUuid()).markTicks == 0, "Priority signature marked passive animal");
        hostile.discard(); passive.discard();
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 80)
    public static void elementalTechniquePreservesSelectedAffinity(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        for (RPGSpecialization spec : new RPGSpecialization[]{RPGSpecialization.FLAMEBOW, RPGSpecialization.FROSTBOW, RPGSpecialization.STORMBOW}) {
            PlayerStats stats = configured(spec, false);
            stats.unlockedNodes.add("arc_magic_foundation");
            stats.unlockedNodes.add("arc_magic_technique");
            String id = spec.nodes.stream().filter(n -> n.id().endsWith("_technique")).findFirst().orElseThrow().id();
            stats.unlockedNodes.add(id);
            stats.setActiveSlot(0, "arc_magic_technique"); stats.setActiveSlot(1, id);
            reset(player, stats);
            CombatState state = CombatState.get(player.getUuid());
            state.classMode = spec == RPGSpecialization.FLAMEBOW ? 0 : spec == RPGSpecialization.FROSTBOW ? 1 : 2;
            int expected = (state.classMode + 1) % 3;
            CombatHandler.activateAbility(player, 0);
            context.assertTrue(state.classMode == expected, "House selector failed to change affinity");
            stats = StatsManager.get(player); stats.resource = stats.resourceMax; StatsManager.save(player, stats);
            CombatHandler.activateAbility(player, 1);
            context.assertTrue(state.cooldown(id) > 0, "Specialization technique was not activated");
            context.assertTrue(state.classMode == expected, spec + " overwrote the selected affinity");
        }
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 600)
    public static void selectedAffinityControlsConfirmedArrows(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.FLAMEBOW, false);
        stats.unlockedNodes.add("arc_magic_foundation");
        stats.unlockedNodes.add("arc_magic_technique");
        stats.setActiveSlot(0, "arc_magic_technique");
        reset(player, stats);
        CombatState state = CombatState.get(player.getUuid());
        for (int i = 0; i < 3; i++) {
            int mode = (i + 1) % 3;
            // Respect the real selector cooldown and give each arrow its own launch tick.
            context.runAtTick(2 + i * 242, () -> {
                PlayerStats ready = StatsManager.get(player); ready.resource = ready.resourceMax; ready.stamina = ready.staminaMax; StatsManager.save(player, ready);
                CombatHandler.activateAbility(player, 0);
                context.assertTrue(state.classMode == mode, "Selector did not complete its three-element cycle");
                CowEntity target = context.spawnMob(EntityType.COW, 3, 1, 3); target.setAiDisabled(true);
                target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100);
                target.setHealth(100);
                var ts = state.classTarget(target.getUuid()); ts.stacks = 3; ts.markTicks = 100;
                player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
                var arrow = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
                com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack());
                player.getServerWorld().spawnEntity(arrow); com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
                context.assertTrue(target.damage(player.getDamageSources().arrow(arrow, player), 2f), "Confirmed arrow fixture did not hurt target");
                context.assertTrue(target.getHealth() < 100, "Arrow did not cause actual health loss");
                context.assertTrue(target.isOnFire() == (mode == 0), "Fire specialization masked the selected affinity " + mode);
                context.assertTrue(target.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS) == (mode == 1), "Selected frost affinity did not exclusively apply control");
                context.assertTrue(target.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.GLOWING) == (mode == 2), "Selected storm affinity did not reveal a marked target");
                target.discard(); arrow.discard();
                if (mode == 0) complete(context);
            });
        }
    }

    @GameTest(templateName = "empty", tickLimit = 80)
    public static void actualArrowDamageScalesWithArcherProgression(TestContext context) {
        ServerPlayerEntity player = testPlayer(context);
        float previous = 0;
        for (int level : new int[]{1, 25, 50}) {
            PlayerStats stats = new PlayerStats(); stats.awakened = true; stats.clazz = RPGClass.ARQUEIRO;
            stats.level = level; stats.stats.put(com.rpgstats.stats.Stat.DESTREZA, level);
            if (level > 1) stats.unlockedNodes.add("arc_core_mastery");
            stats.refreshResourceMax(); stats.resource = stats.resourceMax; stats.stamina = stats.staminaMax;
            reset(player, stats);
            CowEntity target = context.spawnMob(EntityType.COW, 3, 1, 3); target.setAiDisabled(true);
            target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100); target.setHealth(100);
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
            var arrow = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
            com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack());
            player.getServerWorld().spawnEntity(arrow); com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
            context.assertTrue(target.damage(player.getDamageSources().arrow(arrow, player), 8f), "Ranged progression arrow did not hurt target");
            float actual = 100 - target.getHealth();
            context.assertTrue(actual > previous, "Actual bow damage did not grow with level/Dexterity: " + level + " -> " + actual);
            context.assertTrue(actual <= 8f * 1.80f + .01f, "Persistent Archer damage exceeded its budget: " + actual);
            com.rpgstats.RPGStatsMod.LOGGER.info("ARCHER_ACTUAL_DAMAGE level={} dex={} raw=8 actual={}", level, level, actual);
            previous = actual; target.discard(); arrow.discard();
        }
        complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 100)
    public static void nativeInfiniteDummyRegistersElementalTraining(TestContext context) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("dummmmmmy")) { complete(context); return; }
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = new PlayerStats(); stats.awakened = true; stats.clazz = RPGClass.ARQUEIRO;
        stats.path = RPGPath.ARC_ARCANE; stats.level = 25; stats.stats.put(com.rpgstats.stats.Stat.DESTREZA, 25);
        stats.unlockedNodes.add("arc_magic_foundation"); stats.refreshResourceMax(); stats.resource = 0; stats.stamina = stats.staminaMax;
        reset(player, stats); CombatState state = CombatState.get(player.getUuid()); state.classMode = 1;
        var type = net.minecraft.registry.Registries.ENTITY_TYPE.get(new net.minecraft.util.Identifier("dummmmmmy", "target_dummy"));
        var entity = type.create(player.getWorld());
        context.assertTrue(entity instanceof net.minecraft.entity.LivingEntity, "Native Target Dummy registry missing");
        var dummy = (net.minecraft.entity.LivingEntity) entity;
        var pos = context.getAbsolutePos(new BlockPos(3, 1, 3)); dummy.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        player.getServerWorld().spawnEntity(dummy);
        float health = dummy.getHealth(); int xp = StatsManager.get(player).xp;
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        var arrow = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
        com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack()); player.getServerWorld().spawnEntity(arrow); com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
        context.assertTrue(dummy.damage(player.getDamageSources().arrow(arrow, player), 8f), "Native dummy rejected training arrow");
        float recorded = dummyDamageTotal(dummy);
        context.assertTrue(dummy.getHealth() == health, "Dummy fixture is not the native infinite-health mode");
        context.assertTrue(recorded > 8f, "Native dummy did not record RPG ranged scaling: " + recorded);
        context.assertTrue(state.classTarget(dummy.getUuid()).stacks == 1, "Infinite dummy suppressed the elemental mark");
        context.assertTrue(dummy.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS), "Infinite dummy suppressed selected frost control");
        context.assertTrue(StatsManager.get(player).resource > 0, "Infinite dummy suppressed training Focus");
        context.assertTrue(StatsManager.get(player).xp == xp, "Training dummy awarded RPG XP");
        com.rpgstats.RPGStatsMod.LOGGER.info("ARCHER_NATIVE_DUMMY raw=8 recorded={} healthBefore={} healthAfter={} xp={}", recorded, health, dummy.getHealth(), xp);
        dummy.discard(); arrow.discard(); complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 100)
    public static void nativeDummyCancelledAndAbsorbedArrowsGenerateNoTraining(TestContext context) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("dummmmmmy")) { complete(context); return; }
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.FLAMEBOW, false);
        stats.unlockedNodes.add("arc_magic_foundation"); stats.resource = 0; reset(player, stats);
        var type = net.minecraft.registry.Registries.ENTITY_TYPE.get(new net.minecraft.util.Identifier("dummmmmmy", "target_dummy"));
        var dummy = (net.minecraft.entity.LivingEntity) type.create(player.getWorld());
        var pos = context.getAbsolutePos(new BlockPos(3, 1, 3)); dummy.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0, 0); player.getServerWorld().spawnEntity(dummy);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        var arrow = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
        com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack()); player.getServerWorld().spawnEntity(arrow); com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
        java.util.function.Consumer<net.minecraftforge.event.entity.living.LivingHurtEvent> cancel = event -> { if (event.getEntity() == dummy) event.setCanceled(true); };
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, false, net.minecraftforge.event.entity.living.LivingHurtEvent.class, cancel);
        try { dummy.damage(player.getDamageSources().arrow(arrow, player), 8f); }
        finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(cancel); }
        dummy.timeUntilRegen = 0; dummy.setAbsorptionAmount(100);
        dummy.damage(player.getDamageSources().arrow(arrow, player), 8f);
        context.assertTrue(dummyDamageTotal(dummy) == 0, "Cancelled/absorbed dummy fixture recorded damage");
        context.assertTrue(CombatState.get(player.getUuid()).classTarget(dummy.getUuid()).stacks == 0 && !dummy.isOnFire(), "Cancelled/absorbed training arrow applied effects");
        context.assertTrue(StatsManager.get(player).resource == 0, "Cancelled/absorbed training arrow generated Focus");
        dummy.discard(); arrow.discard(); complete(context);
    }

    @GameTest(templateName = "empty", tickLimit = 100)
    public static void elementalSignatureConsumesNativeDummyMark(TestContext context) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("dummmmmmy")) { complete(context); return; }
        ServerPlayerEntity player = testPlayer(context);
        PlayerStats stats = configured(RPGSpecialization.FLAMEBOW, false);
        stats.unlockedNodes.add("arc_magic_foundation");
        stats.unlockedNodes.add("arc_magic_fire_technique"); stats.unlockedNodes.add("arc_magic_fire_signature");
        stats.setActiveSlot(0, "arc_magic_fire_technique"); stats.setActiveSlot(1, "arc_magic_fire_signature"); reset(player, stats);
        var type = net.minecraft.registry.Registries.ENTITY_TYPE.get(new net.minecraft.util.Identifier("dummmmmmy", "target_dummy"));
        var dummy = (net.minecraft.entity.LivingEntity) type.create(player.getWorld());
        // Keep the rotation inside the template and provide a clear, supported sightline.
        // Signature activation requires visibility; explicitly clear the fixture's corridor.
        for (int z = 1; z <= 2; z++) {
            context.setBlockState(new BlockPos(1, 0, z), net.minecraft.block.Blocks.STONE.getDefaultState());
            for (int y = 1; y <= 3; y++) context.setBlockState(new BlockPos(1, y, z), net.minecraft.block.Blocks.AIR.getDefaultState());
        }
        var pos = context.getAbsolutePos(new BlockPos(1, 1, 2)); dummy.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0); player.getServerWorld().spawnEntity(dummy);
        // Nearby queries see newly spawned native dummies only after their index is updated.
        context.runAtTick(2, () -> {
        context.assertTrue(player.getServerWorld().getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                player.getBoundingBox().expand(24), e -> e == dummy).contains(dummy), "Native dummy was not indexed before the signature rotation");
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(net.minecraft.item.Items.BOW));
        CombatHandler.activateAbility(player, 0);
        var first = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
        context.assertTrue(CombatState.get(player.getUuid()).cooldown("arc_magic_fire_technique") > 0, "Native dummy rotation did not start its fire technique");
        com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack()); player.getServerWorld().spawnEntity(first); com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
        dummy.damage(player.getDamageSources().arrow(first, player), 2f);
        context.assertTrue(dummyDamageTotal(dummy)>0, "Native dummy rotation's first arrow dealt no confirmed damage");
        context.assertTrue(player.canSee(dummy), "Native dummy rotation lost line of sight before its signature");
        PlayerStats ready = StatsManager.get(player); ready.resource = ready.resourceMax; StatsManager.save(player, ready);
        context.assertTrue(com.rpgstats.combat.ArcherTechniqueHandler.canActivate(player, ready, "arc_magic_fire_signature"), "Native dummy mark could not satisfy elemental signature");
        CombatHandler.activateAbility(player, 1);
        context.assertTrue(CombatState.get(player.getUuid()).cooldown("arc_magic_fire_signature") > 0, "Native dummy signature was rejected");
        first.discard();
        // Two arrows in one server tick are one volley. Use distinct launch ticks for the rotation.
        context.runAtTick(4, () -> {
            dummy.timeUntilRegen = 0;
            var second = new net.minecraft.entity.projectile.ArrowEntity(player.getWorld(), player);
            com.rpgstats.combat.ArcherShotTracker.beginLaunch(player, player.getMainHandStack()); player.getServerWorld().spawnEntity(second); com.rpgstats.combat.ArcherShotTracker.endLaunch(player);
            dummy.damage(player.getDamageSources().arrow(second, player), 2f);
            float beforeSupplement = dummyDamageTotal(dummy); float focus = StatsManager.get(player).resource;
            second.discard();
            // Same-target supplements wait 11 server ticks to clear hurt resistance.
            context.runAtTick(18, () -> {
                context.assertTrue(dummyDamageTotal(dummy) >= beforeSupplement + 3.99f, "Consumed elemental mark did not deal its +4 supplemental damage");
                context.assertTrue(StatsManager.get(player).xp == 0, "Elemental dummy rotation awarded XP");
                // Queued supplement must not generate another resource reward.
                context.assertTrue(StatsManager.get(player).resource <= focus + 2f, "Supplement generated an additional training reward");
                dummy.discard(); complete(context);
            });
        });
        });
    }

    private static float dummyDamageTotal(net.minecraft.entity.LivingEntity dummy) {
        try { var field = dummy.getClass().getDeclaredField("totalDamageTakenInCombat"); field.setAccessible(true); return field.getFloat(dummy); }
        catch (ReflectiveOperationException error) { throw new AssertionError("Native dummy damage counter changed", error); }
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

}
