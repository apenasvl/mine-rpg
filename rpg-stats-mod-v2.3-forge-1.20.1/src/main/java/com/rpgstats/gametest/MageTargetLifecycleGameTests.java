package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.MageCombatHandler;
import com.rpgstats.combat.MageState;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.test.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class MageTargetLifecycleGameTests {
    private static CowEntity target(TestContext c, float health) {
        var t=c.spawnMob(EntityType.COW,1,1,2);
        t.setAiDisabled(true);t.setNoGravity(true);t.setHealth(health);return t;
    }
    private static void lethalPulse(TestContext c, boolean blood) {
        var p=TestPlayers.create(c);p.setNoGravity(true);
        var stats=new PlayerStats();stats.awakened=true;stats.level=50;stats.clazz=RPGClass.MAGO;
        stats.refreshResourceMax();stats.resource=stats.resourceMax;StatsManager.save(p,stats);
        var a=target(c,.25f);var b=target(c,.25f);var survivor=target(c,20);
        c.runAtTick(2,()->{
            try {
                var state=MageState.get(p.getUuid());state.targets.clear();
                for(var t:new CowEntity[]{a,b}) {
                    var s=state.target(t.getUuid());
                    if(blood){s.bleedTicks=80;s.bleedPulse=39;s.bleedStacks=1;}
                    else{s.ruinTicks=40;s.ruinPulse=19;}
                }
                state.target(survivor.getUuid()).fragilityTicks=60;
                MageCombatHandler.tick(p);
                c.assertTrue(!a.isAlive()&&!b.isAlive(),"Periodic damage did not kill both regression targets");
                c.assertTrue(!state.targets.containsKey(a.getUuid())&&!state.targets.containsKey(b.getUuid()),"Dead targets remain tracked");
                c.assertTrue(state.target(survivor.getUuid()).fragilityTicks==59,"Surviving target did not update exactly once");
                c.assertTrue(!state.internalDamage&&state.currentSchool==MageState.School.NONE,"Periodic damage leaked spell context");
            } finally {a.discard();b.discard();survivor.discard();TestPlayers.finish(c);}
            c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void ruinDeathsDoNotInvalidateTargetTick(TestContext c){lethalPulse(c,false);}
    @GameTest(templateName="empty",tickLimit=80)
    public static void bleedDeathsDoNotInvalidateTargetTick(TestContext c){lethalPulse(c,true);}

    @GameTest(templateName="empty",tickLimit=80)
    public static void damageCallbacksPreserveNewAndReplacementStates(TestContext c) {
        var p=TestPlayers.create(c);p.setNoGravity(true);
        var stats=new PlayerStats();stats.awakened=true;stats.level=50;stats.clazz=RPGClass.MAGO;
        stats.refreshResourceMax();stats.resource=stats.resourceMax;StatsManager.save(p,stats);
        var victim=target(c,20);var added=target(c,20);
        c.runAtTick(2,()->{
            var state=MageState.get(p.getUuid());state.targets.clear();
            var old=state.target(victim.getUuid());old.ruinTicks=1;old.ruinPulse=19;
            var replacement=new MageState.TargetState();replacement.fragilityTicks=60;
            var fired=new AtomicBoolean();
            Consumer<LivingHurtEvent> callback=e->{
                if(e.getEntity()==victim&&fired.compareAndSet(false,true)) {
                    state.targets.put(victim.getUuid(),replacement);
                    state.target(added.getUuid()).fragilityTicks=60;
                }
            };
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,LivingHurtEvent.class,callback);
            try {
                MageCombatHandler.tick(p);
                c.assertTrue(fired.get(),"Periodic damage did not enter Forge damage callback");
                c.assertTrue(state.targets.get(victim.getUuid())==replacement&&replacement.fragilityTicks==60,"Expired old state removed or ticked its replacement");
                c.assertTrue(state.target(added.getUuid()).fragilityTicks==60,"New target was ticked in the same update");
                MageCombatHandler.tick(p);
                c.assertTrue(replacement.fragilityTicks==59&&state.target(added.getUuid()).fragilityTicks==59,"Deferred targets did not update on next tick");
            } finally {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(callback);
                victim.discard();added.discard();TestPlayers.finish(c);
            }
            c.complete();
        });
    }
    private MageTargetLifecycleGameTests(){}
}
