package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossLaunchTracker;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.CombatHandler;
import com.rpgstats.forge.ForgeEvents;
import com.rpgstats.stats.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class BossLaunchLifecycleGameTests {
    private static ServerPlayerEntity player(TestContext c) {
        var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.clazz=RPGClass.GUERREIRO;s.level=50;
        s.stats.put(Stat.VITALIDADE,32);s.stats.put(Stat.TENACIDADE,25);s.refreshResourceMax();StatsManager.finish(p,s);
        p.setNoGravity(true);p.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
        for(int i=0;i<80;i++){p.playerTick();p.tick();}p.setHealth(p.getMaxHealth());
        var pos=c.getAbsolutePos(new BlockPos(1,5,1));p.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        return p;
    }
    private static LivingEntity boss(TestContext c,int x) {
        var b=c.spawnMob(EntityType.WITHER,x,2,3);b.setAiDisabled(true);b.setNoGravity(true);return b;
    }
    private static void hit(TestContext c,ServerPlayerEntity p,LivingEntity b) {
        p.setHealth(p.getMaxHealth());p.timeUntilRegen=0;
        c.assertTrue(p.damage(p.getDamageSources().mobAttack(b),1),"Fixture boss damage rejected");
    }
    private static void impulse(ServerPlayerEntity p,LivingEntity b) {
        double old=p.getVelocity().y;p.setVelocity(0,Math.max(1,old+1),0);
        BossLaunchTracker.recordImpulse(p,b,old,p.getVelocity().y);
    }
    private static void launch(TestContext c,ServerPlayerEntity p,LivingEntity b) {
        BossLaunchTracker.remove(p.getUuid());p.setVelocity(Vec3d.ZERO);hit(c,p,b);impulse(p,b);
        c.assertTrue(BossLaunchTracker.isBossFall(p,p.getDamageSources().fall()),"Fixture launch not active");
    }
    private static void inactive(TestContext c,ServerPlayerEntity p,String reason) {
        c.assertTrue(!BossLaunchTracker.isBossFall(p,p.getDamageSources().fall()),reason);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void waterAndLavaEndLaunchEvenAfterLeavingFluid(TestContext c) {
        var p=player(c);var b=boss(c,3);var pos=p.getBlockPos();
        try {
            for(var fluid:new net.minecraft.block.Block[]{Blocks.WATER,Blocks.LAVA}) {
                launch(c,p,b);p.setVelocity(Vec3d.ZERO);
                c.getWorld().setBlockState(pos,fluid.getDefaultState());c.getWorld().setBlockState(pos.up(),fluid.getDefaultState());
                p.playerTick();p.tick();c.assertTrue(p.isTouchingWater()||p.isInLava(),"Player did not enter fixture fluid");
                inactive(c,p,"Fluid retained launch");
                c.getWorld().setBlockState(pos,Blocks.AIR.getDefaultState());c.getWorld().setBlockState(pos.up(),Blocks.AIR.getDefaultState());
                p.refreshPositionAndAngles(pos.getX()+3.5,pos.getY(),pos.getZ()+.5,0,0);p.playerTick();p.tick();
                inactive(c,p,"Leaving fluid restored stale launch");
                p.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);p.playerTick();p.tick();p.setFireTicks(0);
            }
        }finally {b.discard();c.getWorld().setBlockState(pos,Blocks.AIR.getDefaultState());c.getWorld().setBlockState(pos.up(),Blocks.AIR.getDefaultState());TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void flightVehicleAndLifecycleClearLaunchAndPendingPairs(TestContext c) {
        var p=player(c);var b=boss(c,3);var pig=c.spawnMob(EntityType.PIG,2,2,2);
        try {
            launch(c,p,b);p.getAbilities().flying=true;inactive(c,p,"Flight retained launch");p.getAbilities().flying=false;inactive(c,p,"Flight exit restored launch");
            launch(c,p,b);p.startFallFlying();inactive(c,p,"Elytra flight retained launch");p.stopFallFlying();inactive(c,p,"Elytra exit restored launch");
            launch(c,p,b);c.assertTrue(p.startRiding(pig,true),"Could not enter vehicle");inactive(c,p,"Vehicle retained launch");p.stopRiding();inactive(c,p,"Vehicle exit restored launch");
            launch(c,p,b);ForgeEvents.dimension(new PlayerEvent.PlayerChangedDimensionEvent(p,World.OVERWORLD,World.NETHER));inactive(c,p,"Dimension event retained launch");
            impulse(p,b);inactive(c,p,"Dimension cleanup retained old accepted hit");
            launch(c,p,b);ForgeEvents.respawn(new PlayerEvent.PlayerRespawnEvent(p,false));inactive(c,p,"Respawn retained launch");
            impulse(p,b);inactive(c,p,"Respawn cleanup retained old accepted hit");
            launch(c,p,b);p.timeUntilRegen=0;c.assertTrue(p.damage(p.getDamageSources().outOfWorld(),Float.MAX_VALUE),"Death was not accepted");
            c.assertTrue(!p.isAlive(),"Death fixture still alive");inactive(c,p,"Death retained launch");
            BossLaunchTracker.clear();
        }finally {b.discard();pig.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void competingBossHitAndPvpNeverBorrowOrReplaceLaunchOwner(TestContext c) {
        var p=player(c);var attacker=player(c);var a=boss(c,3);var b=boss(c,5);
        boolean oldPvp=p.getServer().isPvpEnabled();p.getServer().setPvpEnabled(true);
        try {
            var pvp=p.getDamageSources().playerAttack(attacker);float ordinary=CombatHandler.modifyIncomingDamage(p,20,pvp);
            launch(c,p,a);hit(c,p,b);
            c.assertTrue(BossLaunchTracker.fallBoss(p,p.getDamageSources().fall())==a,"Unrelated boss hit replaced launch owner");
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,20,pvp)-ordinary)<.001,"Launch defense leaked into PvP");
            p.timeUntilRegen=0;c.assertTrue(p.damage(pvp,1),"PvP hit rejected");
            c.assertTrue(BossLaunchTracker.fallBoss(p,p.getDamageSources().fall())==a,"PvP without impulse changed attribution");
            hit(c,p,b);impulse(p,b);c.assertTrue(BossLaunchTracker.fallBoss(p,p.getDamageSources().fall())==b,"Second boss launch not attributed to second boss");
            BossLaunchTracker.finishFall(p,p.getDamageSources().fall());inactive(c,p,"Second boss landing was not consumed");
        }finally {p.getServer().setPvpEnabled(oldPvp);a.discard();b.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nonBossImpulseInvalidatesEarlierBossFlight(TestContext c) {
        var p=player(c);var b=boss(c,3);var cow=c.spawnMob(EntityType.COW,2,2,2);
        try {launch(c,p,b);impulse(p,cow);inactive(c,p,"Non-boss impulse retained earlier boss attribution");}
        finally {b.discard();cow.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void newestUnconfirmedImpulseCannotBorrowEarlierHit(TestContext c) {
        var p=player(c);var a=boss(c,3);var b=boss(c,5);
        try {
            launch(c,p,a);impulse(p,b);inactive(c,p,"Unconfirmed second impulse borrowed first boss hit");
            hit(c,p,b);c.assertTrue(BossLaunchTracker.fallBoss(p,p.getDamageSources().fall())==b,"Confirmed second impulse not paired");
            impulse(p,b);inactive(c,p,"Repeated impulse reused consumed health loss");
            hit(c,p,b);c.assertTrue(BossLaunchTracker.fallBoss(p,p.getDamageSources().fall())==b,"Second confirmed repeated impulse lost owner");
        }finally {a.discard();b.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=240)
    public static void launchExpiresImmediatelyAfterInclusiveBoundary(TestContext c) {
        var p=player(c);var b=boss(c,3);launch(c,p,b);p.setVelocity(Vec3d.ZERO);long start=c.getWorld().getTime();
        c.waitAndRun(200,()-> {
            c.assertTrue(c.getWorld().getTime()-start==200,"Boundary fixture clock mismatch");
            c.assertTrue(BossLaunchTracker.isBossFall(p,p.getDamageSources().fall()),"Expired before inclusive200tick limit");
            c.waitAndRun(1,()-> {try {inactive(c,p,"Not expired at201ticks");}finally {b.discard();TestPlayers.finish(c);}c.complete();});
        });
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void groundLandingConsumesStateBeforeOrdinaryFall(TestContext c) {
        var p=player(c);var b=boss(c,3);launch(c,p,b);p.setVelocity(Vec3d.ZERO);
        c.waitAndRun(3,()-> {try {p.setOnGround(true);BossLaunchTracker.tick(p.getServer());inactive(c,p,"Ground landing retained state");p.setOnGround(false);inactive(c,p,"Leaving ground restored state");}
            finally {b.discard();TestPlayers.finish(c);}c.complete();});
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void rejectedSecondHitCannotBorrowEarlierSameBossDamage(TestContext c) {
        var p=player(c);var b=boss(c,3);
        try {
            p.setVelocity(Vec3d.ZERO);hit(c,p,b);
            p.setInvulnerable(true);p.timeUntilRegen=0;
            c.assertTrue(!p.damage(p.getDamageSources().mobAttack(b),1),"Second attack not rejected");
            p.setInvulnerable(false);impulse(p,b);
            inactive(c,p,"Rejected second attack borrowed earlier same-boss damage");
            double old=p.getVelocity().y;p.setVelocity(0,old+1,0);
            BossLaunchTracker.recordNativePostDamageImpulse(p,b,old,p.getVelocity().y);
            inactive(c,p,"Rejected attack retained native continuation token");
        }finally {p.setInvulnerable(false);b.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativeRejectedPreDamageLaunchDoesNotBorrowPriorHit(TestContext c) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var p=player(c);var pos=p.getPos();
        var b=(net.minecraft.entity.mob.MobEntity)net.minecraft.registry.Registries.ENTITY_TYPE.get(new net.minecraft.util.Identifier("soulsweapons:returning_knight")).create(c.getWorld());
        b.setAiDisabled(true);b.setNoGravity(true);b.refreshPositionAndAngles(pos.x,pos.y,pos.z+1,180,0);c.getWorld().spawnEntity(b);
        try {
            p.setVelocity(Vec3d.ZERO);hit(c,p,b);p.setInvulnerable(true);p.timeUntilRegen=0;
            WarriorBossDefenseGameTests.nativeHit(b,p,21);
            c.assertTrue(p.getVelocity().y>=.9,"Native rejected fixture did not launch");
            p.setInvulnerable(false);inactive(c,p,"Rejected native pre-damage impulse borrowed prior hit");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        finally {p.setInvulnerable(false);b.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void nativePostHurtImpulsePreservesAcceptedVanillaLaunch(TestContext c) {
        var p=player(c);var b=boss(c,3);
        try {
            p.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0);
            p.setVelocity(Vec3d.ZERO);p.setOnGround(true);hit(c,p,b);
            c.assertTrue(p.getVelocity().y>=.25,"Accepted hurt did not produce vanilla vertical knockback");
            c.assertTrue(BossLaunchTracker.isBossFall(p,p.getDamageSources().fall()),"Vanilla accepted launch not tracked");
            // Same post-hurt write used by Returning Knight phases36/52.
            double old=p.getVelocity().y;p.setVelocity(0,1.5,0);
            BossLaunchTracker.recordNativePostDamageImpulse(p,b,old,p.getVelocity().y);
            c.assertTrue(BossLaunchTracker.isBossFall(p,p.getDamageSources().fall()),"Native post-hurt impulse erased accepted vanilla launch");
        }finally {b.discard();TestPlayers.finish(c);}c.complete();
    }
    private BossLaunchLifecycleGameTests(){}
}
