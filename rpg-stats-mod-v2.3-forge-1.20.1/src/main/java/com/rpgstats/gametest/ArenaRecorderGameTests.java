package com.rpgstats.gametest;

import com.google.gson.JsonObject;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.stats.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraftforge.gametest.*;

/** Recorder correctness fixtures, never full encounters or balance evidence. */
@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class ArenaRecorderGameTests {
    private static Object call(String method,Class<?>[] types,Object... args) {
        try{return Class.forName("com.rpgstats.debug.ArenaRecorder").getMethod(method,types).invoke(null,args);}
        catch(ReflectiveOperationException e){throw new AssertionError("Real arena recorder unavailable: "+method,e);}
    }
    private static ServerPlayerEntity player(TestContext c) {
        var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.clazz=RPGClass.GUERREIRO;s.level=50;
        s.stats.put(Stat.VITALIDADE,32);s.refreshResourceMax();StatsManager.finish(p,s);
        p.setNoGravity(true);for(int i=0;i<80;i++){p.playerTick();p.tick();}p.setHealth(p.getMaxHealth());return p;
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void recorderCountsOnlyConfirmedHealthLossAndNoStopWin(TestContext c) {
        var p=player(c);var b=c.spawnMob(EntityType.WITHER,3,3,3);b.setAiDisabled(true);b.setNoGravity(true);
        // Stabilize vanilla encounter level scaling before measuring a fixed health delta.
        com.rpgstats.boss.EncounterManager.recordParticipation(p,b);
        try {
            c.assertTrue((boolean)call("start",new Class[]{ServerPlayerEntity.class,LivingEntity.class,String.class},p,b,"balanced"),"Recorder refused fixture");
            b.setInvulnerable(true);c.assertTrue(!b.damage(p.getDamageSources().playerAttack(p),5),"Rejected fixture accepted");b.setInvulnerable(false);
            b.timeUntilRegen=0;float before=b.getHealth();c.assertTrue(b.damage(p.getDamageSources().playerAttack(p),5),"Accepted fixture rejected");float dealt=before-b.getHealth();
            p.timeUntilRegen=0;before=p.getHealth();c.assertTrue(p.damage(p.getDamageSources().mobAttack(b),2),"Incoming fixture rejected");float received=before-p.getHealth();
            var report=(JsonObject)call("stop",new Class[]{ServerPlayerEntity.class,String.class},p,"STOPPED");
            c.assertTrue(Math.abs(report.get("damage_dealt").getAsFloat()-dealt)<.001,"Recorder counted rejected/unmitigated outgoing damage: report="+report.get("damage_dealt")+" healthDelta="+dealt);
            c.assertTrue(Math.abs(report.get("damage_received").getAsFloat()-received)<.001,"Recorder did not measure confirmed incoming health loss");
            c.assertTrue(report.get("ttk_seconds").isJsonNull(),"Stopped fixture fabricated boss kill TTK");
        }finally {b.setInvulnerable(false);b.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void recorderRejectsInvalidStartAndCleansOnDimensionEvent(TestContext c) {
        var p=player(c);var b=c.spawnMob(EntityType.WITHER,3,3,3);b.setAiDisabled(true);var cow=c.spawnMob(EntityType.COW,2,2,2);
        try {
            c.assertTrue(!(boolean)call("start",new Class[]{ServerPlayerEntity.class,LivingEntity.class,String.class},p,cow,"balanced"),"Non-boss recording accepted");
            c.assertTrue(!(boolean)call("start",new Class[]{ServerPlayerEntity.class,LivingEntity.class,String.class},p,b,"invalid"),"Invalid build accepted");
            c.assertTrue((boolean)call("start",new Class[]{ServerPlayerEntity.class,LivingEntity.class,String.class},p,b,"defensive"),"Valid recording rejected");
            com.rpgstats.forge.ForgeEvents.dimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,net.minecraft.world.World.OVERWORLD,net.minecraft.world.World.NETHER));
            c.assertTrue(call("stop",new Class[]{ServerPlayerEntity.class,String.class},p,"STOPPED")==null,"Dimension retained active recorder");
        }finally {b.discard();cow.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void lethalPlayerHitIsCountedBeforeDeathReport(TestContext c) {
        var p=player(c);var b=c.spawnMob(EntityType.WITHER,3,3,3);b.setAiDisabled(true);
        try {
            c.assertTrue((boolean)call("start",new Class[]{ServerPlayerEntity.class,LivingEntity.class,String.class},p,b,"balanced"),"Recorder refused fixture");
            float hp=p.getHealth();p.timeUntilRegen=0;
            c.assertTrue(p.damage(p.getDamageSources().outOfWorld(),Float.MAX_VALUE),"Lethal hit rejected");
            c.assertTrue(!p.isAlive(),"Lethal fixture survived");
            var report=(JsonObject)call("stop",new Class[]{ServerPlayerEntity.class,String.class},p,"STOPPED");
            c.assertTrue(report!=null,"Player death finalized before lethal damage returned");
            c.assertTrue(report.get("outcome").getAsString().equals("PLAYER_DEAD"),"Confirmed death not preserved");
            c.assertTrue(Math.abs(report.get("damage_received").getAsFloat()-hp)<.001,"Fatal health loss omitted");
            c.assertTrue(report.get("net_health_recovered").getAsDouble()<.001,"Fatal damage counted as healing");
            c.assertTrue(report.get("ttk_seconds").isJsonNull(),"Player death fabricated boss win");
        }finally {b.discard();TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void bossDeathTtkRequiresFullStartingHealth(TestContext c) {
        var p=player(c);
        try {
            for(boolean full:new boolean[]{true,false}) {
                var b=c.spawnMob(EntityType.WITHER,3,3,3);b.setAiDisabled(true);
                try {
                    if(!full)b.setHealth(b.getMaxHealth()/2);
                    c.assertTrue((boolean)call("start",new Class[]{ServerPlayerEntity.class,LivingEntity.class,String.class},p,b,"balanced"),"Recorder refused fixture");
                    c.assertTrue(b.damage(b.getDamageSources().outOfWorld(),Float.MAX_VALUE),"Boss lethal hit rejected");
                    var report=(JsonObject)call("stop",new Class[]{ServerPlayerEntity.class,String.class},p,"STOPPED");
                    c.assertTrue(report!=null,"Boss death finalized before damage return");
                    c.assertTrue(report.get("outcome").getAsString().equals("BOSS_DEAD"),"Confirmed boss death not recorded");
                    c.assertTrue(report.get("ttk_seconds").isJsonNull()!=full,"Full/partial initial health TTK incorrect");
                }finally {b.discard();}
            }
        }finally {TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void lethalOwnedFallKeepsPreDeathBossAttribution(TestContext c) {
        var p=player(c);var b=c.spawnMob(EntityType.WITHER,3,3,3);b.setAiDisabled(true);b.setNoGravity(true);
        try {
            p.timeUntilRegen=0;c.assertTrue(p.damage(p.getDamageSources().mobAttack(b),1),"Fixture launch hit rejected");
            p.setVelocity(0,1,0);com.rpgstats.boss.BossLaunchTracker.recordImpulse(p,b,0,1);p.setHealth(1);
            c.assertTrue((boolean)call("start",new Class[]{ServerPlayerEntity.class,LivingEntity.class,String.class},p,b,"balanced"),"Recorder refused fixture");
            p.timeUntilRegen=0;c.assertTrue(p.damage(p.getDamageSources().fall(),Float.MAX_VALUE),"Lethal fall rejected");
            c.assertTrue(!p.isAlive(),"Fatal fixture survived");
            var report=(JsonObject)call("stop",new Class[]{ServerPlayerEntity.class,String.class},p,"STOPPED");
            c.assertTrue(report!=null,"Fatal fall report disappeared");
            c.assertTrue(Math.abs(report.get("boss_attributed_damage_received").getAsFloat()-1)<.001,"Death cleanup erased proven fall owner");
        }finally {b.discard();TestPlayers.finish(c);}c.complete();
    }
    private ArenaRecorderGameTests(){}
}
