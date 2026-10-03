package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.compat.WeaponTypePolicy;
import com.rpgstats.compat.WeaponTypeResolver;
import com.rpgstats.compat.bosses.BossEquipmentService;
import com.rpgstats.compat.bosses.WeaponAffinity;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Native registered items and loaded metadata, including uniques whose IDs hide their category. */
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SimplyWeaponGameTests {
    @GameTest(templateName = "empty", tickLimit = 20)
    public static void nativeRapidAndTwoHandedWeaponsKeepAffinityForEveryClass(TestContext c) {
        if (!ModList.get().isLoaded("simplyswords")) { c.complete(); return; }
        WeaponTypeResolver.reload(c.getWorld().getServer().getResourceManager());
        for (String id : new String[]{"iron_sai", "netherite_rapier", "shadowsting", "soulstealer"}) {
            ItemStack stack = nativeStack(c, id);
            c.assertTrue(WeaponTypeResolver.isDagger(stack), "Native rapid metadata ignored for " + id);
            c.assertTrue(!WeaponTypeResolver.isTwoHanded(stack), "Rapid weapon became two-handed: " + id);
            assertAffinity(c, stack, RPGClass.ASSASSINO);
        }
        for (String id : new String[]{"netherite_claymore", "soulkeeper"}) {
            ItemStack stack = nativeStack(c, id);
            c.assertTrue(WeaponTypeResolver.isTwoHanded(stack), "Native two-handed metadata ignored for " + id);
            assertAffinity(c, stack, RPGClass.GUERREIRO);
        }
        c.assertTrue(WeaponTypeResolver.classify(nativeStack(c, "netherite_longsword")) == WeaponTypePolicy.Kind.MELEE,
                "One-handed native longsword lost its known category");
        c.assertTrue(!WeaponTypeResolver.isTwoHanded(nativeStack(c, "netherite_longsword")),
                "One-handed native longsword was guessed two-handed");
        c.assertTrue(WeaponTypeResolver.classify(nativeStack(c, "netherite_spear")) == WeaponTypePolicy.Kind.MELEE
                && !WeaponTypeResolver.isTwoHanded(nativeStack(c,"netherite_spear")),
                "Native spear lost its explicit one-handed trident parent");
        assertAffinity(c,nativeStack(c,"netherite_spear"),RPGClass.GUERREIRO);
        TestPlayers.finish(c);
        c.complete();
    }

    @GameTest(templateName = "empty", tickLimit = 20)
    public static void coreTagsRemainCompatibleWithNativeResolver(TestContext c) {
        c.assertTrue(WeaponTypeResolver.isDagger(new ItemStack(Items.GOLDEN_SWORD)), "Core dagger tag lost");
        c.assertTrue(WeaponTypeResolver.isTwoHanded(new ItemStack(Items.NETHERITE_SWORD)), "Core greatsword tag lost");
        c.assertTrue(!WeaponTypeResolver.isDagger(ItemStack.EMPTY) && !WeaponTypeResolver.isTwoHanded(ItemStack.EMPTY),
                "Empty hand acquired a native category");
        c.complete();
    }

    @GameTest(templateName = "empty", tickLimit = 40)
    public static void nativeLichbladeHealingSharesCoreBudget(TestContext c) {
        if (!ModList.get().isLoaded("simplyswords")) { c.complete(); return; }
        WeaponTypeResolver.reload(c.getWorld().getServer().getResourceManager());
        var player = TestPlayers.create(c);
        var stats = new PlayerStats(); stats.awakened = true; stats.clazz = RPGClass.GUERREIRO;
        StatsManager.save(player, stats);
        ItemStack stack = nativeStack(c, "waking_lichblade");
        c.assertTrue(WeaponTypeResolver.isTwoHanded(stack), "Lichblade native heavy metadata missing");
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, stack);
        player.setHealth(4);
        player.age = 0;
        var target = net.minecraft.entity.EntityType.ZOMBIE.create(c.getWorld());
        target.setAiDisabled(true); target.setNoGravity(true);
        target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100000);
        target.setHealth(100000);
        var pos = c.getAbsolutePos(new net.minecraft.util.math.BlockPos(2, 80, 2));
        player.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        target.refreshPositionAndAngles(pos.getX()+1, pos.getY(), pos.getZ(), 0, 0);
        c.getWorld().spawnEntity(target);
        try {
            // Invokes the shipped native channel tick, which performs its own heal call.
            // Optional parameter classes are discovered at runtime and never linked into the mod.
            Class<?> ability = Class.forName("net.sweenus.simplyswords.util.AbilityMethods");
            java.lang.reflect.Method nativeTick = java.util.Arrays.stream(ability.getDeclaredMethods())
                    .filter(m -> m.getName().equals("tickAbilitySoulAnguish") && m.getParameterCount() == 10)
                    .findFirst().orElseThrow(() -> new AssertionError("Native Soul Anguish channel signature missing"));
            float start = player.getHealth();
            for (int seed = 0; seed < 200 && player.getHealth() == start; seed++) {
                player.getRandom().setSeed(seed);
                nativeTick.invoke(null, stack, c.getWorld(), player, 0f, 2,
                        target.getX(), target.getY(), target.getZ(), 1f, target);
            }
            c.assertTrue(Math.abs(player.getHealth()-start-.7f) < .001f,
                    "Actual native Lichblade heal bypassed two-handed sustain penalty");
            com.rpgstats.combat.WarriorSustain.heal(player, .8f, new ItemStack(Items.IRON_SWORD));
            for (int seed = 0; seed < 100; seed++) {
                player.getRandom().setSeed(seed);
                nativeTick.invoke(null, stack, c.getWorld(), player, 0f, 2,
                        target.getX(), target.getY(), target.getZ(), 1f, target);
            }
            c.assertTrue(Math.abs(player.getHealth()-start-1.5f) < .001f,
                    "Native and core weapon sustain split their rolling budget");
            player.heal(2);
            c.assertTrue(Math.abs(player.getHealth()-start-3.5f) < .001f,
                    "Ordinary heal while holding native weapon was incorrectly capped");
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Could not execute shipped native Lichblade healing", ex);
        } finally {
            target.discard(); TestPlayers.finish(c);
        }
        c.complete();
    }

    private static ItemStack nativeStack(TestContext c, String path) {
        Identifier id = new Identifier("simplyswords", path);
        c.assertTrue(Registries.ITEM.containsId(id), "Native Simply Swords fixture missing: " + id);
        return new ItemStack(Registries.ITEM.get(id));
    }

    private static void assertAffinity(TestContext c, ItemStack stack, RPGClass preferred) {
        for (RPGClass clazz : new RPGClass[]{RPGClass.GUERREIRO, RPGClass.ASSASSINO, RPGClass.MAGO, RPGClass.ARQUEIRO}) {
            PlayerStats stats = new PlayerStats();
            stats.clazz = clazz;
            c.assertTrue(Math.abs(WeaponAffinity.damageFactor(stats, stack) - (clazz == preferred ? 1f : .625f)) < .0001f,
                    "Native affinity changed for " + clazz + " using " + Registries.ITEM.getId(stack.getItem()));
            var player = TestPlayers.create(c);
            StatsManager.awaken(player);
            StatsManager.selectClass(player, clazz.name());
            c.assertTrue(BossEquipmentService.check(player, stack).allowed(), "Native weapon denied to class " + clazz);
        }
    }

    private SimplyWeaponGameTests() {}
}
