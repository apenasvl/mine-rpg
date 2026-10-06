package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.CombatHandler;
import com.rpgstats.forge.ForgeEvents;
import com.rpgstats.stats.*;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.gametest.*;

@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class BossFallDamageGameTests {
    private static void build(ServerPlayerEntity p,RPGClass clazz) {
        var s=new PlayerStats();s.awakened=true;s.clazz=clazz;s.level=50;
        s.stats.put(Stat.VITALIDADE,32);s.stats.put(Stat.TENACIDADE,25);
        s.refreshResourceMax();StatsManager.finish(p,s);p.setNoGravity(true);
        // Connected players retain vanilla spawn protection until server ticks expire it.
        for(int i=0;i<80;i++){p.playerTick();p.tick();}
        p.setHealth(p.getMaxHealth());
    }
    private static void impulse(ServerPlayerEntity p,net.minecraft.entity.LivingEntity boss) {
        double before=p.getVelocity().y;p.setVelocity(0,1,0);
        com.rpgstats.boss.BossLaunchTracker.recordImpulse(p,boss,before,p.getVelocity().y);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void acceptedBossLaunchProtectsOnlyItsFirstLanding(TestContext c) {
        var boss=c.spawnMob(EntityType.WITHER,3,1,3);
        try {
            for(var clazz:RPGClass.values()) {
                var p=TestPlayers.create(c);build(p,clazz);
                var fall=p.getDamageSources().fall();float ordinary=CombatHandler.modifyIncomingDamage(p,40,fall);
                p.setVelocity(Vec3d.ZERO);p.timeUntilRegen=0;
                c.assertTrue(p.damage(p.getDamageSources().mobAttack(boss),1),"Boss hit was not accepted");
                impulse(p,boss);
                float launched=CombatHandler.modifyIncomingDamage(p,40,fall);
                c.assertTrue(launched<ordinary*.9f,"Boss launch landing bypassed class defense: "+clazz+" ordinary="+ordinary+" launch="+launched);
                p.timeUntilRegen=0;c.assertTrue(p.damage(fall,40),"Landing damage was not accepted");
                float second=CombatHandler.modifyIncomingDamage(p,40,fall);
                c.assertTrue(Math.abs(second-ordinary)<.001f,"Boss defense leaked into the next ordinary fall: "+clazz);
            }
        }finally {boss.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void rejectedAndOrdinaryHitsNeverGrantBossFallDefense(TestContext c) {
        var boss=c.spawnMob(EntityType.WITHER,3,1,3);var cow=c.spawnMob(EntityType.COW,4,1,3);
        try {
            var p=TestPlayers.create(c);build(p,RPGClass.GUERREIRO);
            var fall=p.getDamageSources().fall();float ordinary=CombatHandler.modifyIncomingDamage(p,40,fall);
            p.setVelocity(Vec3d.ZERO);
            p.setInvulnerable(true);
            c.assertTrue(!p.damage(p.getDamageSources().mobAttack(boss),1),"Invulnerable hit must be rejected");
            impulse(p,boss);p.setInvulnerable(false);
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001f,"Rejected hit granted protection");
            com.rpgstats.boss.BossLaunchTracker.remove(p.getUuid());p.setVelocity(Vec3d.ZERO);
            p.timeUntilRegen=0;p.damage(p.getDamageSources().mobAttack(cow),1);impulse(p,cow);
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001f,"Ordinary mob granted boss protection");
            p.setVelocity(Vec3d.ZERO);p.timeUntilRegen=0;p.damage(p.getDamageSources().mobAttack(boss),1);impulse(p,boss);
            c.assertTrue(CombatHandler.modifyIncomingDamage(p,40,fall)<ordinary*.9,"Fixture never activated launch protection");
            ForgeEvents.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001f,"Logout retained launch protection");
        }finally {boss.discard();cow.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=260)
    public static void expiredLaunchNeverProtectsLaterFall(TestContext c) {
        var boss=c.spawnMob(EntityType.WITHER,3,1,3);var p=TestPlayers.create(c);build(p,RPGClass.GUERREIRO);
        var fall=p.getDamageSources().fall();float ordinary=CombatHandler.modifyIncomingDamage(p,40,fall);
        p.setVelocity(Vec3d.ZERO);p.timeUntilRegen=0;p.damage(p.getDamageSources().mobAttack(boss),1);impulse(p,boss);
            c.assertTrue(CombatHandler.modifyIncomingDamage(p,40,fall)<ordinary*.9,"Fixture never activated launch protection");
        c.runAtTick(220,()-> {
            try {c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001f,"Expired launch retained protection");}
            finally {boss.discard();TestPlayers.finish(c);}c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void jumpingDuringBossHitWithoutImpulseDoesNotProtectFall(TestContext c) {
        var boss=c.spawnMob(EntityType.WITHER,3,1,3);var p=TestPlayers.create(c);build(p,RPGClass.GUERREIRO);
        try {
            p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
            p.setVelocity(0,.42,0);p.timeUntilRegen=0;
            var fall=p.getDamageSources().fall();float ordinary=CombatHandler.modifyIncomingDamage(p,40,fall);
            c.assertTrue(p.damage(p.getDamageSources().mobAttack(boss),1),"Hit must be accepted");
            c.assertTrue(Math.abs(p.getVelocity().y-.42)<.001,"Fixture unexpectedly received an impulse");
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001,"An existing jump was attributed to the boss");
        }finally {boss.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void safeClimbingEndsLaunchBeforeLaterFall(TestContext c) {
        var boss=c.spawnMob(EntityType.WITHER,3,1,3);var p=TestPlayers.create(c);build(p,RPGClass.GUERREIRO);
        var pos=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(1,2,1));
        try {
            p.setVelocity(Vec3d.ZERO);p.timeUntilRegen=0;p.damage(p.getDamageSources().mobAttack(boss),1);impulse(p,boss);
            var fall=p.getDamageSources().fall();c.assertTrue(com.rpgstats.boss.BossLaunchTracker.isBossFall(p,fall),"Fixture never activated");
            c.getWorld().setBlockState(pos,net.minecraft.block.Blocks.LADDER.getDefaultState());
            p.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
            c.assertTrue(p.isClimbing(),"Fixture is not climbing");
            c.assertTrue(!com.rpgstats.boss.BossLaunchTracker.isBossFall(p,fall),"Safe climbing retained launch");
            c.getWorld().setBlockState(pos,net.minecraft.block.Blocks.AIR.getDefaultState());
            c.assertTrue(!com.rpgstats.boss.BossLaunchTracker.isBossFall(p,fall),"Leaving ladder restored stale launch");
        }finally {boss.discard();c.getWorld().setBlockState(pos,net.minecraft.block.Blocks.AIR.getDefaultState());TestPlayers.finish(c);}c.complete();
    }
    private BossFallDamageGameTests(){}
}
