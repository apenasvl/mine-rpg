package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.classes.*;
import com.rpgstats.combat.*;
import com.rpgstats.compat.*;
import com.rpgstats.compat.combatroll.CombatRollCompat;
import com.rpgstats.stats.*;
import net.minecraft.test.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatModGameTests {
    private static final String ROLL="arc_ward_surv_technique";
    @GameTest(templateName="empty",tickLimit=80)
    public static void guideShowsTheActiveExecutor(TestContext c) {
        String text=com.rpgstats.ability.ClassAbilityRegistry.description(ROLL);
        c.assertTrue(text.contains("Combat Roll")==ModList.get().isLoaded("combatroll"),"Guide does not reflect active route");
        c.assertTrue(AbilityRegistry.activeForNode(ROLL).resourceCost()==20 && AbilityRegistry.activeForNode(ROLL).cooldownTicks()==280,"Node economy changed");
        done(c);
    }
    private static PlayerStats configure(net.minecraft.server.network.ServerPlayerEntity p) {
        PlayerStats s=new PlayerStats(); s.clazz=RPGClass.ARQUEIRO; s.level=StatsManager.MAX_LEVEL;
        s.awakened=true; s.specialization=RPGSpecialization.SURVIVALIST; s.path=s.specialization.parent;
        s.unlockedNodes.add(s.specialization.nodes.get(0).id()); s.unlockedNodes.add(ROLL);
        s.refreshResourceMax(); s.resource=s.resourceMax; s.setActiveSlot(0,ROLL);
        CombatState.remove(p.getUuid()); StatsManager.save(p,s); p.setOnGround(true); p.setVelocity(Vec3d.ZERO);
        return s;
    }
    private static void done(TestContext c) { TestPlayers.finish(c); c.complete(); }
    @GameTest(templateName="empty",tickLimit=80)
    public static void rollPaysOnceAndSpamCannotMoveAgain(TestContext c) {
        var p=TestPlayers.create(c);var s=configure(p);float before=s.resource;
        CombatHandler.activateAbility(p,0);var velocity=p.getVelocity();var paid=StatsManager.get(p).resource;
        c.assertTrue(Math.abs(before-paid-AbilityRegistry.activeForNode(ROLL).resourceCost())<.01,"Roll cost changed or charged twice");
        c.assertTrue(velocity.horizontalLengthSquared()>.3 && velocity.horizontalLengthSquared()<.5,"Roll doubled or failed: "+velocity);
        c.assertTrue(CombatState.get(p.getUuid()).timer("arc_survival")>0,"Survival opening was lost");
        CombatHandler.activateAbility(p,0);
        c.assertTrue(p.getVelocity().equals(velocity) && Math.abs(StatsManager.get(p).resource-paid)<.01,"Spam executed or charged twice");
        done(c);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void missingResourceCannotMove(TestContext c) {
        var p=TestPlayers.create(c);var s=configure(p);s.resource=0;StatsManager.save(p,s);
        CombatHandler.activateAbility(p,0);
        c.assertTrue(p.getVelocity().equals(Vec3d.ZERO) && CombatState.get(p.getUuid()).cooldown(ROLL)==0,"Unpaid roll executed");
        done(c);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void rollPreflightRejectsClassNodeAndAir(TestContext c) {
        var p=TestPlayers.create(c);var s=configure(p);var bridge=new CombatRollCompat();var effect=AbilityRegistry.activeForNode(ROLL);
        c.assertTrue(bridge.preflight(p,s,ROLL,effect).route()==PhysicalActionBridge.Route.EXTERNAL,"Valid technique rejected");
        s.unlockedNodes.remove(ROLL);
        c.assertTrue(bridge.preflight(p,s,ROLL,effect).route()==PhysicalActionBridge.Route.REJECTED,"Unpurchased node accepted");
        s.unlockedNodes.add(ROLL);s.clazz=RPGClass.GUERREIRO;
        c.assertTrue(bridge.preflight(p,s,ROLL,effect).route()==PhysicalActionBridge.Route.REJECTED,"Wrong class accepted");
        s.clazz=RPGClass.ARQUEIRO;p.setOnGround(false);
        c.assertTrue(bridge.preflight(p,s,ROLL,effect).route()==PhysicalActionBridge.Route.REJECTED,"Air roll accepted");
        c.assertTrue(bridge.preflight(p,s,"arc_wind_run_technique",effect).route()==PhysicalActionBridge.Route.CORE,"Windrunner converted into roll");
        done(c);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void failedExecutorRefundsWithoutFallback(TestContext c) {
        var p=TestPlayers.create(c);var s=configure(p);float before=s.resource;
        PhysicalActionService.register(new PhysicalActionBridge() {
            public ActionDecision preflight(net.minecraft.server.network.ServerPlayerEntity p,PlayerStats s,String id,com.rpgstats.ability.SkillEffect e) { return new ActionDecision(Route.EXTERNAL,""); }
            public boolean execute(net.minecraft.server.network.ServerPlayerEntity p,PlayerStats s,String id,com.rpgstats.ability.SkillEffect e) { return false; }
        });
        try {
            CombatHandler.activateAbility(p,0);
            c.assertTrue(Math.abs(StatsManager.get(p).resource-before)<.01 && CombatState.get(p.getUuid()).cooldown(ROLL)==0 && p.getVelocity().equals(Vec3d.ZERO),"Failed executor charged or ran core fallback");
        } finally { PhysicalActionService.register(ModList.get().isLoaded("combatroll") ? new CombatRollCompat() : null); }
        done(c);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void forgedNativePacketIsStoppedBeforeDecode(TestContext c) throws Exception {
        if (!ModList.get().isLoaded("combatroll")) {done(c);return;}
        var p=TestPlayers.create(c);configure(p);
        var method=java.util.Arrays.stream(Class.forName("net.combatroll.network.ServerNetwork").getDeclaredMethods())
                .filter(m -> m.getName().equals("lambda$initializeHandlers$5")).findFirst().orElseThrow();
        method.setAccessible(true);
        var buffer=new net.minecraft.network.PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        try { method.invoke(null,p.getServer(),p,p.networkHandler,buffer,null); }
        finally {buffer.release();}
        c.assertTrue(p.getVelocity().equals(Vec3d.ZERO),"Native packet moved player");
        done(c);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeArmorPowersAreRemovedButProtectionRemains(TestContext c) throws Exception {
        if (!ModList.get().isLoaded("immersive_armors")) {done(c);return;}
        var item=Registries.ITEM.get(new Identifier("immersive_armors","divine_chestplate"));
        c.assertTrue(item instanceof net.minecraft.item.ArmorItem,"Native armor missing");
        Object material=item.getClass().getMethod("getMaterial").invoke(item);var type=material.getClass();
        c.assertTrue(((java.util.List<?>)type.getMethod("getEffects").invoke(material)).isEmpty(),"Free divine/berserk effects retained");
        c.assertTrue(((java.util.Map<?,?>)type.getMethod("getEnchantments").invoke(material)).isEmpty(),"Innate native enchantment retained");
        c.assertTrue(((Number)type.getMethod("getAttackDamage").invoke(material)).floatValue()==0,"Free native attack bonus retained");
        c.assertTrue(((net.minecraft.item.ArmorItem)item).getProtection()>0,"Armor protection removed");
        for (var candidate : Registries.ITEM) {
            if (!Registries.ITEM.getId(candidate).getNamespace().equals("immersive_armors") || !(candidate instanceof net.minecraft.item.ArmorItem)) continue;
            Object m=candidate.getClass().getMethod("getMaterial").invoke(candidate);
            c.assertTrue(((Number)m.getClass().getMethod("getWeight").invoke(m)).floatValue()>=0,"Armor granted free movement speed");
            c.assertTrue(((java.util.List<?>)m.getClass().getMethod("getEffects").invoke(m)).isEmpty(),"Native armor effect survived");
        }
        done(c);
    }
}
