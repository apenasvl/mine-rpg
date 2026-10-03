package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.*;
import com.rpgstats.combat.*;
import com.rpgstats.stats.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.function.Consumer;

/** Rejected damage must preserve prepared actions; accepted damage resolves them once. */
@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class PhysicalLifecycleGameTests {
    private static PlayerStats stats(RPGSpecialization spec) {
        var s=new PlayerStats(); s.awakened=true; s.level=50;
        s.clazz=spec.parent.parent; s.path=spec.parent; s.specialization=spec;
        for(var n:spec.nodes)s.unlockedNodes.add(n.id());
        s.refreshResourceMax();s.resource=0;s.stamina=s.staminaMax;return s;
    }
    private static void configure(ServerPlayerEntity p, RPGSpecialization spec) {
        CombatState.remove(p.getUuid());StatsManager.save(p,stats(spec));p.setNoGravity(true);p.setHealth(p.getMaxHealth());
    }
    private static CowEntity target(TestContext c) {
        var t=c.spawnMob(EntityType.COW,1,1,2);t.setAiDisabled(true);t.setNoGravity(true);
        t.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(200);t.setHealth(200);return t;
    }
    private static void cancelled(net.minecraft.entity.LivingEntity victim,Runnable action) {
        Consumer<LivingHurtEvent> cancel=e->{if(e.getEntity()==victim)e.setCanceled(true);};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,LivingHurtEvent.class,cancel);
        try{action.run();}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(cancel);}
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void warriorParryIgnoresEnvironmentalDamage(TestContext c) {
        var p=TestPlayers.create(c);configure(p,RPGSpecialization.DUEL_MASTER);
        try{
            var state=CombatState.get(p.getUuid());state.startTimer("war_parry",100);
            float damage=ClassMechanics.modifyIncomingDamage(p,StatsManager.get(p),10,p.getDamageSources().generic());
            c.assertTrue(Math.abs(damage-10)<.001f&&state.timer("war_parry")==100&&state.timer("war_riposte")==0,
                "Environmental damage consumed Warrior Parry or created Riposte");
        }finally{TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=120)
    public static void cancelledMeleePreservesBothParries(TestContext c) {
        var p=TestPlayers.create(c);p.setNoGravity(true);var t=target(c);
        c.runAtTick(65,()->{
            try{for(var spec:new RPGSpecialization[]{RPGSpecialization.DUEL_MASTER,RPGSpecialization.COUNTERBLADE}){
                configure(p,spec);String prefix=spec.parent.parent==RPGClass.GUERREIRO?"war":"ass";
                var state=CombatState.get(p.getUuid());state.startTimer(prefix+"_parry",100);p.timeUntilRegen=0;
                float before=p.getHealth();cancelled(p,()->p.damage(p.getDamageSources().mobAttack(t),8));
                c.assertTrue(p.getHealth()==before,"Cancelled parry fixture lost health");
                c.assertTrue(state.timer(prefix+"_parry")==100&&state.timer(prefix+"_riposte")==0,
                    spec+" consumed Parry or armed Riposte on cancelled melee");
            }}finally{t.discard();TestPlayers.finish(c);}c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=120)
    public static void acceptedMeleeArmsBothRipostesAndWarriorGuard(TestContext c) {
        var p=TestPlayers.create(c);p.setNoGravity(true);var t=target(c);
        c.runAtTick(65,()->{
            try{for(var spec:new RPGSpecialization[]{RPGSpecialization.DUEL_MASTER,RPGSpecialization.COUNTERBLADE}){
                configure(p,spec);String prefix=spec.parent.parent==RPGClass.GUERREIRO?"war":"ass";
                var state=CombatState.get(p.getUuid());state.startTimer(prefix+"_parry",100);p.timeUntilRegen=0;
                float before=p.getHealth();boolean accepted=p.damage(p.getDamageSources().mobAttack(t),8);
                c.assertTrue(accepted&&p.getHealth()<before,"Accepted parry fixture did not lose health");
                c.assertTrue(state.timer(prefix+"_parry")==0&&state.timer(prefix+"_riposte")>0,spec+" did not resolve confirmed Parry");
                if(spec==RPGSpecialization.DUEL_MASTER)c.assertTrue(state.gauge("war_guard")>=.79f,"Duel Master confirmed Parry lost its engine Guard reward");
            }}finally{t.discard();TestPlayers.finish(c);}c.complete();
        });
    }
    private static void prepare(ServerPlayerEntity p) {
        var s=CombatState.get(p.getUuid());s.setGauge("ass_combo",3.5f,5);s.setGauge("ass_echo",2,6);
        s.startTimer("ass_riposte",100);s.startTimer("ass_soul_strike",100);s.startTimer("ass_void_debt",100);
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void rejectedAssassinHitsPreserveResourcesAndEffects(TestContext c) {
        var p=TestPlayers.create(c);
        try{for(var spec:new RPGSpecialization[]{RPGSpecialization.COUNTERBLADE,RPGSpecialization.SOULKNIFE,RPGSpecialization.VOIDWALKER}){
            for(boolean absorb:new boolean[]{false,true}){
                configure(p,spec);prepare(p);var t=target(c);
                try{
                    if(absorb)t.setAbsorptionAmount(100);
                    float before=t.getHealth();
                    Runnable hit=()->t.damage(p.getDamageSources().playerAttack(p),6);
                    if(absorb)hit.run();else cancelled(t,hit);
                    c.assertTrue(t.getHealth()==before,"Rejected Assassin fixture lost health");
                    var state=CombatState.get(p.getUuid());
                    c.assertTrue(state.gauge("ass_combo")==3.5f&&state.gauge("ass_echo")==2&&StatsManager.get(p).resource==0,
                        spec+" spent Combo/Echo or refunded Energy on rejected damage (absorbed="+absorb+")");
                    c.assertTrue(!t.hasStatusEffect(StatusEffects.WEAKNESS)&&!t.hasStatusEffect(StatusEffects.SLOWNESS)
                        &&!p.hasStatusEffect(StatusEffects.RESISTANCE)&&!p.hasStatusEffect(StatusEffects.SPEED),
                        spec+" applied prepared effects before confirmed damage");
                }finally{t.discard();p.clearStatusEffects();}
            }
        }}finally{TestPlayers.finish(c);}c.complete();
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void acceptedAssassinHitsResolveTheirPreparedPayoffs(TestContext c) {
        var p=TestPlayers.create(c);
        try{for(var spec:new RPGSpecialization[]{RPGSpecialization.COUNTERBLADE,RPGSpecialization.SOULKNIFE,RPGSpecialization.VOIDWALKER}){
            configure(p,spec);prepare(p);var t=target(c);
            try{
                c.assertTrue(t.damage(p.getDamageSources().playerAttack(p),6)&&t.getHealth()<200,"Accepted Assassin fixture dealt no damage");
                var state=CombatState.get(p.getUuid());
                c.assertTrue(StatsManager.get(p).resource>0,"Confirmed prepared Assassin hit did not restore Energy");
                if(spec==RPGSpecialization.COUNTERBLADE)c.assertTrue(t.hasStatusEffect(StatusEffects.WEAKNESS)&&p.hasStatusEffect(StatusEffects.RESISTANCE),"Confirmed Counterblade Riposte lost its payoff");
                if(spec==RPGSpecialization.SOULKNIFE)c.assertTrue(state.gauge("ass_echo")<2&&state.timer("internal_ass_soulknife")>0,"Confirmed Soulknife hit did not consume Echo");
                if(spec==RPGSpecialization.VOIDWALKER)c.assertTrue(t.hasStatusEffect(StatusEffects.SLOWNESS)&&p.hasStatusEffect(StatusEffects.SPEED),"Confirmed Voidwalker hit lost its mobility/control payoff");
            }finally{t.discard();p.clearStatusEffects();}
        }}finally{TestPlayers.finish(c);}c.complete();
    }
    private PhysicalLifecycleGameTests(){}
}
