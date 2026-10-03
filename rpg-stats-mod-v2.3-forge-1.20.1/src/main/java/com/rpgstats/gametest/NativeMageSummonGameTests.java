package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import java.lang.reflect.*;
import java.util.*;

@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeMageSummonGameTests {
    @GameTest(templateName="empty",tickLimit=180)
    public static void ownedNativeSummonsCanBeTrackedFocusedAndSupported(TestContext c) {
        if (!ModList.get().isLoaded("irons_spellbooks")) { c.complete(); return; }
        var p=PhysicalRoleGameTests.player(c,RPGClass.MAGO,50,Stat.INTELIGENCIA);
        StatsManager.selectPath(p,RPGPath.MAGE_CONJURATION.name());
        for(var n:RPGPath.MAGE_CONJURATION.nodes) StatsManager.unlockNode(p,n.id());
        StatsManager.selectSpecialization(p,RPGSpecialization.CONJURER.name());
        for(var n:RPGSpecialization.CONJURER.nodes) StatsManager.unlockNode(p,n.id());
        var pos=c.getAbsolutePos(new BlockPos(5,3,5));
        var forced=new HashSet<ChunkPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            var chunk=new ChunkPos(pos.add(x*16,0,z*16));
            if(!c.getWorld().getForcedChunks().contains(chunk.toLong())) {
                forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);
            }
        }
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)
            c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
        p.refreshPositionAndAngles(pos,0,0);p.setNoGravity(true);
        var target=EntityType.ZOMBIE.create(c.getWorld());target.setAiDisabled(true);
        target.refreshPositionAndAngles(pos.add(0,0,7),0,0);c.getWorld().spawnEntity(target);
        var owned=new ArrayList<LivingEntity>();
        c.runAtTick(100,()->{
        try {
            p.refreshPositionAndAngles(pos,0,0);p.setHealth(p.getMaxHealth());
            // Native constructors are the supported ownership boundary. Spell spawning has
            // separate placement/casting prerequisites and is not part of this lifecycle regression.
            for(String name:List.of("SummonedZombie","SummonedSkeleton","SummonedVex")) {
                Class<?> type=Class.forName("io.redspace.ironsspellbooks.entity.mobs."+name);
                Constructor<?> constructor=Arrays.stream(type.getConstructors()).filter(k->{
                    var args=k.getParameterTypes();return (args.length==2||args.length==3)
                        &&args[0].isInstance(c.getWorld())&&args[1].isInstance(p)
                        &&(args.length==2||args[2]==boolean.class);
                }).findFirst().orElseThrow();
                LivingEntity entity=(LivingEntity)(constructor.getParameterCount()==3
                    ?constructor.newInstance(c.getWorld(),p,false):constructor.newInstance(c.getWorld(),p));
                entity.refreshPositionAndAngles(pos.add(owned.size()+1,0,0),0,0);
                entity.setNoGravity(true);if(entity instanceof MobEntity mob)mob.setAiDisabled(true);
                owned.add(entity);
                c.assertTrue(c.getWorld().spawnEntity(entity),"Native summon constructor entity was rejected by the world");
            }
        } catch(ReflectiveOperationException | RuntimeException | AssertionError error) {
            owned.forEach(Entity::discard);target.discard();for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);
            TestPlayers.finish(c);throw new AssertionError(error);
        }
        // ServerWorld inserts newly spawned entities into its query index at the tick boundary.
        // Inspect the real cast result on a later tick, rather than treating pending insertion as failure.
        c.runAtTick(105,()->{
        try {
            Class<?> summon=Class.forName("io.redspace.ironsspellbooks.entity.mobs.IMagicSummon");
            for(var entity:owned) {
                Object owner=summon.isInstance(entity)?summon.getMethod("getSummoner").invoke(entity):null;
                RPGStatsMod.LOGGER.info("RPG_NATIVE_SUMMON_REF class={} interfaces={} alive={} removed={} sameWorld={} magicSummon={} owner={} player={} indexed={}",
                    entity.getClass().getName(),Arrays.toString(entity.getClass().getInterfaces()),entity.isAlive(),entity.isRemoved(),entity.getWorld()==c.getWorld(),
                    summon.isInstance(entity),owner instanceof Entity e?e.getUuid():owner,p.getUuid(),c.getWorld().getEntity(entity.getUuid())!=null);
                c.assertTrue(entity.isAlive()&&summon.isInstance(entity)&&owner==p,"Actual native entity lost its owner or lifetime");
            }
            var discovered=new ArrayList<LivingEntity>();
            for(var raw:c.getWorld().iterateEntities()) {
                if(raw.getClass().getName().contains("Summoned")) RPGStatsMod.LOGGER.info("RPG_NATIVE_SUMMON_RAW class={} alive={} removed={} magicSummon={}",raw.getClass().getName(),raw.isAlive(),raw.isRemoved(),summon.isInstance(raw));
                if(summon.isInstance(raw)&&raw instanceof LivingEntity entity) {
                    Object owner=summon.getMethod("getSummoner").invoke(entity);
                    RPGStatsMod.LOGGER.info("RPG_NATIVE_SUMMON entity={} alive={} owner={} player={} distance={}",
                            entity.getType(),entity.isAlive(),owner instanceof Entity e?e.getUuid():owner,p.getUuid(),entity.squaredDistanceTo(p));
                    if(entity.isAlive()&&owner==p&&entity.squaredDistanceTo(p)<=48*48) discovered.add(entity);
                }
            }
            Class<?> bridge;
            try { bridge=Class.forName("com.rpgstats.compat.irons.IronsNativeSummons"); }
            catch(ClassNotFoundException absent) { throw new AssertionError("Native summon lifecycle bridge is missing",absent); }
            c.assertTrue(discovered.size()==owned.size(),"Owned native summons were absent from the loaded world index");
            bridge.getMethod("sync",ServerPlayerEntity.class,PlayerStats.class).invoke(null,p,StatsManager.get(p));
            int count=((Number)bridge.getMethod("count",ServerPlayerEntity.class,PlayerStats.class).invoke(null,p,StatsManager.get(p))).intValue();
            c.assertTrue(count==owned.size(),"Native tracker did not discover real owned summons");
            bridge.getMethod("focus",ServerPlayerEntity.class,PlayerStats.class,LivingEntity.class).invoke(null,p,StatsManager.get(p),target);
            c.assertTrue(owned.stream().filter(MobEntity.class::isInstance).map(MobEntity.class::cast).allMatch(m->m.getTarget()==target),"Focus did not change native mob targets");
            for(var entity:owned) entity.setHealth(entity.getMaxHealth()*.5f);
            float before=owned.get(0).getHealth();
            bridge.getMethod("transfer",ServerPlayerEntity.class,PlayerStats.class).invoke(null,p,StatsManager.get(p));
            c.assertTrue(owned.get(0).getHealth()>before,"Transfer did not support real summons");
            float bonus=((Number)bridge.getMethod("diversityBonus",ServerPlayerEntity.class,PlayerStats.class).invoke(null,p,StatsManager.get(p))).floatValue();
            c.assertTrue(bonus>0f&&bonus<=.15f,"Distinct real summons did not receive bounded Pack Tactics");
            var state=com.rpgstats.combat.MageState.get(p.getUuid());
            var attribute=(net.minecraft.entity.attribute.EntityAttribute)((net.minecraftforge.registries.RegistryObject<?>)
                    Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry").getField("SUMMON_DAMAGE").get(null)).get();
            Class<?> classBridge=Class.forName("com.rpgstats.compat.irons.IronsMageClassBridge");
            Method attributes=classBridge.getDeclaredMethod("applyIronsAttributes",ServerPlayerEntity.class,PlayerStats.class);
            attributes.setAccessible(true);
            state.summonAssaultTicks=0;state.ephemeralArmyTicks=0;
            attributes.invoke(null,p,StatsManager.get(p));double passive=p.getAttributeValue(attribute);
            state.summonAssaultTicks=100;
            attributes.invoke(null,p,StatsManager.get(p));double assault=p.getAttributeValue(attribute);
            state.ephemeralArmyTicks=200;
            attributes.invoke(null,p,StatsManager.get(p));double army=p.getAttributeValue(attribute);
            RPGStatsMod.LOGGER.info("RPG_NATIVE_SUMMON_COMMAND passive={} assault={} army={}",passive,assault,army);
            c.assertTrue(assault>passive+.10,"Assault added no useful damage over full native summon passives");
            c.assertTrue(army>assault+.02&&army<=1.5001,"Army did not replace Assault within the native command cap");
            state.summonAssaultTicks=0;state.ephemeralArmyTicks=0;
            owned.get(0).discard();
            bridge.getMethod("sync",ServerPlayerEntity.class,PlayerStats.class).invoke(null,p,StatsManager.get(p));
            int remaining=((Number)bridge.getMethod("count",ServerPlayerEntity.class,PlayerStats.class).invoke(null,p,StatsManager.get(p))).intValue();
            c.assertTrue(remaining==count-1,"Removed native summon remained tracked");
        } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
        finally { owned.forEach(Entity::discard); target.discard();
            for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c); }
        c.complete();
        });
        });
    }
}
