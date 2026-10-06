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
        s.refreshResourceMax();StatsManager.finish(p,s);p.setHealth(p.getMaxHealth());
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void acceptedBossLaunchProtectsOnlyItsFirstLanding(TestContext c) {
        var boss=c.spawnMob(EntityType.WITHER,3,1,3);
        try {
            for(var clazz:RPGClass.values()) {
                var p=TestPlayers.create(c);build(p,clazz);
                var fall=p.getDamageSources().fall();float ordinary=CombatHandler.modifyIncomingDamage(p,40,fall);
                p.setVelocity(new Vec3d(0,1,0));p.timeUntilRegen=0;
                c.assertTrue(p.damage(p.getDamageSources().mobAttack(boss),1),"Boss hit was not accepted");
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
            p.setVelocity(new Vec3d(0,1,0));
            ForgeEvents.confirmedDamage(p,p.getDamageSources().mobAttack(boss),0);
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001f,"Rejected hit granted protection");
            p.timeUntilRegen=0;p.damage(p.getDamageSources().mobAttack(cow),1);
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001f,"Ordinary mob granted boss protection");
            p.timeUntilRegen=0;p.damage(p.getDamageSources().mobAttack(boss),1);
            ForgeEvents.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            c.assertTrue(Math.abs(CombatHandler.modifyIncomingDamage(p,40,fall)-ordinary)<.001f,"Logout retained launch protection");
        }finally {boss.discard();cow.discard();TestPlayers.finish(c);}c.complete();
    }
    private BossFallDamageGameTests(){}
}
