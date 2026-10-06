package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.*;
import com.rpgstats.combat.*;
import com.rpgstats.stats.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class WarriorBossDefenseGameTests {
    private static void configure(ServerPlayerEntity p,RPGSpecialization spec,int level) {
        var s=new PlayerStats();s.awakened=true;s.level=level;s.clazz=RPGClass.GUERREIRO;
        s.path=spec.parent;s.specialization=spec;
        s.stats.put(Stat.VITALIDADE,32);s.stats.put(Stat.TENACIDADE,25);s.stats.put(Stat.FORCA,47);s.stats.put(Stat.DESTREZA,24);
        for(var n:RPGClass.GUERREIRO.nodes)s.unlockedNodes.add(n.id());
        for(var n:spec.parent.nodes)s.unlockedNodes.add(n.id());for(var n:spec.nodes)s.unlockedNodes.add(n.id());
        s.refreshResourceMax();s.resource=s.resourceMax;s.stamina=s.staminaMax;
        StatsManager.finish(p,s);p.setNoGravity(true);
        p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(11);
        p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).setBaseValue(0);
    }
    private static void nativeHit(MobEntity boss,ServerPlayerEntity p,int phase) throws ReflectiveOperationException {
        if(phase==7){BossPressureGameTests.commonHit(boss,p,true);return;}
        boss.setTarget(p);boss.getClass().getMethod("setSpawning",boolean.class).invoke(boss,false);
        boss.getClass().getMethod(phase==21?"setMaceOfSpades":"setRupture",boolean.class).invoke(boss,true);
        var type=Class.forName("net.soulsweaponry.entity.ai.goal.ReturningKnightGoal");
        var goal=(net.minecraft.entity.ai.goal.Goal)type.getConstructor(boss.getClass()).newInstance(boss);
        for(var value:new Object[][]{{"attackCooldown",100},{"specialCooldown",100},{"summonCooldown",100},{"attackStatus",phase-1},{"targetPos",p.getBlockPos()},{"cordsRegistered",true}}) {
            var f=type.getDeclaredField((String)value[0]);f.setAccessible(true);f.set(goal,value[1]);
        }
        if(phase==52)p.refreshPositionAndAngles(boss.getX()+12,boss.getY(),boss.getZ(),0,0);
        goal.tick();
    }
    private static void encounter(TestContext c,int phase) {
        if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
        var players=new ArrayList<ServerPlayerEntity>();var bosses=new ArrayList<MobEntity>();var specs=new ArrayList<RPGSpecialization>();var forced=new HashSet<ChunkPos>();
        for(var spec:RPGSpecialization.values())if(spec.parent.parent==RPGClass.GUERREIRO) {
            var p=TestPlayers.create(c);configure(p,spec,50);
            var pos=c.getAbsolutePos(new BlockPos(65536+players.size()*64,3,24576+phase*64));var chunk=new ChunkPos(pos);
            if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
            var boss=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
            boss.setAiDisabled(true);boss.setNoGravity(true);boss.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);
            p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);c.getWorld().spawnEntity(boss);
            players.add(p);bosses.add(boss);specs.add(spec);
        }
        c.runAtTick(100,()->{
            var failures=new ArrayList<String>();
            try {
                for(int i=0;i<players.size();i++) {
                    var p=players.get(i);p.setHealth(p.getMaxHealth());p.setVelocity(0,-1,0);p.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);float hp=p.getHealth();nativeHit(bosses.get(i),p,phase);
                    float loss=hp-p.getHealth();
                    if(phase==21||phase==52) {
                        c.assertTrue(p.getVelocity().y>=0.9,"Native launch did not occur");
                        c.assertTrue(com.rpgstats.boss.BossLaunchTracker.isBossFall(p,p.getDamageSources().fall()),"Native launch was missed at phase "+phase+" spec="+specs.get(i));
                    }
                    RPGStatsMod.LOGGER.info("RPG_WARRIOR_BOSS_DEFENSE spec={} phase={} level=50 vitality=32 tenacity=25 armor={} maxHP={} loss={} remaining={}",specs.get(i),phase,p.getArmor(),hp,loss,p.getHealth());
                    if(loss<=0||!p.isAlive())failures.add(specs.get(i).name());
                }
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            finally {for(var b:bosses){b.discard();BossScaler.untrack(b);}for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
            c.assertTrue(failures.isEmpty(),"Warrior specializations died or received no confirmed native damage: "+failures);c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=240)
    public static void allWarriorsSurviveNativeKnightStrike(TestContext c){encounter(c,7);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void allWarriorsSurviveNativeKnightLaunch(TestContext c){encounter(c,21);}
    @GameTest(templateName="empty",tickLimit=240)
    public static void allWarriorsSurviveNativeKnightEruptionAt12Blocks(TestContext c){encounter(c,52);}
    @GameTest(templateName="empty",tickLimit=80)
    public static void bossProgressionDoesNotApplyToPlayersOrOrdinaryMobs(TestContext c) {
        var low=TestPlayers.create(c);var high=TestPlayers.create(c);var attacker=TestPlayers.create(c);var cow=c.spawnMob(EntityType.COW,1,1,2);
        try {
            for(var spec:RPGSpecialization.values())if(spec.parent.parent==RPGClass.GUERREIRO) {
                configure(low,spec,25);configure(high,spec,50);CombatState.remove(low.getUuid());CombatState.remove(high.getUuid());
                for(var source:new net.minecraft.entity.damage.DamageSource[]{high.getDamageSources().playerAttack(attacker),high.getDamageSources().mobAttack(cow)}) {
                    float a=CombatHandler.modifyIncomingDamage(low,100,source),b=CombatHandler.modifyIncomingDamage(high,100,source);
                    c.assertTrue(a>0&&Math.abs(a-b)<.001f,"Boss defense leaked into PvP or ordinary mobs: "+spec+" "+source);
                }
            }
        }finally{cow.discard();TestPlayers.finish(c);}c.complete();
    }
    private WarriorBossDefenseGameTests(){}
}
