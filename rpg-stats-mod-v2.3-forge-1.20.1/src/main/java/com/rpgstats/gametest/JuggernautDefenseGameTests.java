package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import com.rpgstats.boss.BossScaler;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;

/** Regression against a mixed offensive/defensive build, without Protection IV. */
@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class JuggernautDefenseGameTests {
    private static void encounter(TestContext c, int phase, boolean evade) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var p=TestPlayers.create(c);p.setNoGravity(true);
        var stats=new PlayerStats();stats.awakened=true;stats.level=50;stats.clazz=RPGClass.GUERREIRO;
        stats.path=RPGPath.WAR_VANGUARD;stats.specialization=RPGSpecialization.JUGGERNAUT;
        stats.stats.put(Stat.VITALIDADE,32);stats.stats.put(Stat.TENACIDADE,25);
        stats.stats.put(Stat.FORCA,47);stats.stats.put(Stat.DESTREZA,24);
        for(var n:RPGClass.GUERREIRO.nodes)stats.unlockedNodes.add(n.id());
        for(var n:RPGPath.WAR_VANGUARD.nodes)stats.unlockedNodes.add(n.id());
        for(var n:RPGSpecialization.JUGGERNAUT.nodes)stats.unlockedNodes.add(n.id());
        stats.refreshResourceMax();stats.resource=stats.resourceMax;stats.stamina=stats.staminaMax;
        StatsManager.finish(p,stats);
        // Represent armor attributes directly so the regression does not depend on a specific armor mod.
        p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(11);
        p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).setBaseValue(0);
        p.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(.5);
        var pos=c.getAbsolutePos(new BlockPos(28672+phase*64+(evade?32:0),3,14336));var chunk=new ChunkPos(pos);
        boolean unforce=!c.getWorld().getForcedChunks().contains(chunk.toLong());
        if(unforce)c.getWorld().setChunkForced(chunk.x,chunk.z,true);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
        var boss=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
        boss.setAiDisabled(true);boss.setNoGravity(true);
        boss.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);
        p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);c.getWorld().spawnEntity(boss);
        c.runAtTick(100,()->{
            try {
                p.setHealth(p.getMaxHealth());float before=p.getHealth();
                if(phase==7)BossPressureGameTests.commonHit(boss,p,true);
                else {
                    boss.setTarget(p);boss.getClass().getMethod("setSpawning",boolean.class).invoke(boss,false);
                    boss.getClass().getMethod(phase==21?"setMaceOfSpades":"setRupture",boolean.class).invoke(boss,true);
                    var type=Class.forName("net.soulsweaponry.entity.ai.goal.ReturningKnightGoal");
                    var goal=(net.minecraft.entity.ai.goal.Goal)type.getConstructor(boss.getClass()).newInstance(boss);
                    for(var value:new Object[][]{{"attackCooldown",100},{"specialCooldown",100},{"summonCooldown",100},{"attackStatus",phase-1},{"targetPos",p.getBlockPos()},{"cordsRegistered",true}}) {
                        var field=type.getDeclaredField((String)value[0]);field.setAccessible(true);field.set(goal,value[1]);
                    }
                    if(phase==52)p.refreshPositionAndAngles(boss.getX()+ (evade?19:12),boss.getY(),boss.getZ(),0,0);
                    goal.tick();
                }
                float loss=before-p.getHealth();
                RPGStatsMod.LOGGER.info("RPG_JUGGERNAUT_REFERENCE phase={} evade={} level=50 vitality=32 tenacity=25 strength=47 dexterity=24 armor={} toughness={} maxHP={} loss={} remaining={} velocity={}",phase,evade,p.getArmor(),p.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS),before,loss,p.getHealth(),p.getVelocity());
                if(evade)c.assertTrue(loss==0,"Leaving native eruption area did not evade its damage");
                else {
                    c.assertTrue(loss>0,"Native attack did not deal confirmed damage");
                    c.assertTrue(p.isAlive()&&loss<=before*(phase==52?.80f:.65f),"Mixed Juggernaut build cannot survive native attack with recovery room");
                    if(phase!=7)c.assertTrue(p.getVelocity().y>0,"Native launch no longer applies upward movement");
                }
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            finally {boss.discard();BossScaler.untrack(boss);if(unforce)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
            c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=160)
    public static void juggernautSurvivesNativeKnightStrikeWithMixedBuild(TestContext c){encounter(c,7,false);}
    @GameTest(templateName="empty",tickLimit=160)
    public static void juggernautSurvivesNativeKnightLaunchWithMixedBuild(TestContext c){encounter(c,21,false);}
    @GameTest(templateName="empty",tickLimit=160)
    public static void juggernautSurvivesNativeKnightEruptionBeyondMeleeRange(TestContext c){encounter(c,52,false);}
    @GameTest(templateName="empty",tickLimit=160)
    public static void nativeKnightEruptionCanBeEvadedOutsideArea(TestContext c){encounter(c,52,true);}
    @GameTest(templateName="empty",tickLimit=80)
    public static void juggernautGuardCapPreservesEarlyPressureAndProgressesLate(TestContext c) {
        for(int level:new int[]{1,25})for(int tenacity:new int[]{0,25,50})for(float guards:new float[]{10,35.75f,100}) {
            float old=com.rpgstats.balance.ClassBalance.warriorBossDamage(100,guards,
                    com.rpgstats.balance.ClassBalance.warriorBossReduction(level,tenacity));
            float juggernaut=com.rpgstats.balance.ClassBalance.juggernautBossDamage(100,guards,
                    com.rpgstats.balance.ClassBalance.juggernautBossReduction(level,tenacity),level);
            c.assertTrue(Math.abs(old-juggernaut)<.001f,"Juggernaut changed early-level stacked guard budget");
        }
        float middle=com.rpgstats.balance.ClassBalance.juggernautBossDamage(100,0,1,35);
        float end=com.rpgstats.balance.ClassBalance.juggernautBossDamage(100,0,1,50);
        c.assertTrue(middle<40&&middle>20&&Math.abs(end-20)<.001f,"Late defense lost progression or exceeded 80% cap");
        c.complete();
    }
    private JuggernautDefenseGameTests(){}
}
