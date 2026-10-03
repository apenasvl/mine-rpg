package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.*;
import com.rpgstats.combat.MageCombatHandler;
import com.rpgstats.combat.MageState;
import com.rpgstats.stats.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BloodMageGameTests {
    private static ServerPlayerEntity mage(TestContext c, boolean blood) {
        var p=PhysicalRoleGameTests.player(c,RPGClass.MAGO,50,Stat.INTELIGENCIA);
        var path=blood?RPGPath.MAGE_OCCULT:RPGPath.MAGE_TEMPORAL;
        StatsManager.selectPath(p,path.name());
        for(var n:path.nodes)StatsManager.unlockNode(p,n.id());
        if(blood){
            StatsManager.selectSpecialization(p,RPGSpecialization.BLOODMANCER.name());
            for(var n:RPGSpecialization.BLOODMANCER.nodes)StatsManager.unlockNode(p,n.id());
            c.assertTrue(StatsManager.get(p).hasNode("mag_blood_hemorrhage"),"Legal Bloodmancer fixture did not unlock hemorrhage");
        }
        p.setHealth(p.getMaxHealth()*.5f);
        return p;
    }
    private static Object spell(String field)throws ReflectiveOperationException {
        return ((net.minecraftforge.registries.RegistryObject<?>)Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry").getField(field).get(null)).get();
    }
    private static float input(Object spell, ServerPlayerEntity p,boolean ray)throws ReflectiveOperationException {
        if(ray){
            var m=spell.getClass().getDeclaredMethod("getTickDamage",int.class,LivingEntity.class);
            m.setAccessible(true);return ((Number)m.invoke(spell,10,p)).floatValue();
        }
        return ((Number)spell.getClass().getMethod("getSpellPower",int.class,Entity.class).invoke(spell,5,p)).floatValue();
    }
    private static boolean hit(Object spell,ServerPlayerEntity p,LivingEntity target,float input)throws ReflectiveOperationException {
        Entity direct=p;
        if(spell.getClass().getSimpleName().equals("BloodSlashSpell"))
            direct=(Entity)Class.forName("io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile").getConstructor(net.minecraft.world.World.class,LivingEntity.class).newInstance(p.getWorld(),p);
        var source=(DamageSource)spell.getClass().getMethod("getDamageSource",Entity.class,Entity.class).invoke(spell,direct,p);
        target.timeUntilRegen=0;p.getRandom().setSeed(5123L);target.getRandom().setSeed(1L);
        try{return (boolean)Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class).invoke(null,target,input,source);}
        finally{if(direct!=p)direct.discard();}
    }
    @GameTest(templateName="empty",tickLimit=160)
    public static void bloodmancerEmpowersNativeBloodSpellsAgainstKnight(TestContext c){
        if(!ModList.get().isLoaded("soulsweapons")||!ModList.get().isLoaded("irons_spellbooks")){c.complete();return;}
        var players=new ArrayList<ServerPlayerEntity>();var bosses=new ArrayList<MobEntity>();var forced=new HashSet<ChunkPos>();
        for(int i=0;i<4;i++){
            var p=mage(c,i%2==1);var pos=c.getAbsolutePos(new BlockPos(512+i*32,3,512));var chunk=new ChunkPos(pos);
            if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
            var b=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
            b.setAiDisabled(true);b.setNoGravity(true);b.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);
            p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+3,180,0);c.getWorld().spawnEntity(b);players.add(p);bosses.add(b);
            try{b.getClass().getMethod("setSpawning",boolean.class).invoke(b,false);}catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }
        c.runAtTick(100,()->{
            float[] loss=new float[4],heal=new float[4];
            try{
                for(int i=0;i<4;i++){
                    var p=players.get(i);var b=bosses.get(i);b.setHealth(b.getMaxHealth());p.setHealth(p.getMaxHealth()*.5f);
                    var s=spell(i<2?"BLOOD_SLASH_SPELL":"RAY_OF_SIPHONING_SPELL");
                    float before=b.getHealth(),hp=p.getHealth(),nativeInput=input(s,p,i>=2);
                    c.assertTrue(hit(s,p,b,nativeInput),"Native blood spell rejected by Returning Knight");
                    loss[i]=before-b.getHealth();heal[i]=p.getHealth()-hp;
                    RPGStatsMod.LOGGER.info("RPG_BLOOD_BOSS bloodmancer={} spell={} nativeInput={} healthLoss={} heal={}",i%2==1,i<2?"blood_slash":"ray_of_siphoning",nativeInput,loss[i],heal[i]);
                }
                c.assertTrue(loss[1]>=loss[0]*1.35f && loss[3]>=loss[2]*1.35f,"Bloodmancer has no earned blood-school damage advantage");
                c.assertTrue(heal[3]<=heal[2]*1.2f+.05f,"Blood damage bonus also multiplied native siphon healing");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            finally{for(var b:bosses){b.discard();BossScaler.untrack(b);}for(var ch:forced)c.getWorld().setChunkForced(ch.x,ch.z,false);TestPlayers.finish(c);}
            c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=100)
    public static void repeatedNativeBloodHitsDoNotPostponeHemorrhage(TestContext c){
        if(!ModList.get().isLoaded("irons_spellbooks")){c.complete();return;}
        var p=mage(c,true);var t=EntityType.COW.create(c.getWorld());
        t.setAiDisabled(true);t.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(500);t.setHealth(500);
        t.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(6,3,6)),0,0);c.getWorld().spawnEntity(t);
        try{
            var s=spell("RAY_OF_SIPHONING_SPELL");float amount=input(s,p,true);
            c.assertTrue(hit(s,p,t,amount),"First native blood hit rejected");
            for(int i=0;i<20;i++)MageCombatHandler.tick(p);
            var state=MageState.get(p.getUuid()).target(t.getUuid());int before=state.bleedPulse;
            c.assertTrue(hit(s,p,t,amount),"Second native blood hit rejected");
            RPGStatsMod.LOGGER.info("RPG_BLOOD_HEMORRHAGE pulseBefore={} pulseAfter={} stacks={}",before,state.bleedPulse,state.bleedStacks);
            c.assertTrue(before==20 && state.bleedPulse==before,"Repeated accepted blood hit restarted the hemorrhage timer");
            float hp=t.getHealth();t.timeUntilRegen=0;
            for(int i=0;i<20;i++)MageCombatHandler.tick(p);
            c.assertTrue(t.getHealth()<hp,"Hemorrhage never damaged the target during repeated blood attacks");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        finally{t.discard();TestPlayers.finish(c);}
        c.complete();
    }

    @GameTest(templateName="empty",tickLimit=240)
    public static void nativeRayChannelKeepsBloodMasteryAcrossPulses(TestContext c){
        if(!ModList.get().isLoaded("soulsweapons")||!ModList.get().isLoaded("irons_spellbooks")){c.complete();return;}
        var players=new ArrayList<ServerPlayerEntity>();var bosses=new ArrayList<MobEntity>();var forced=new HashSet<ChunkPos>();
        for(int i=0;i<2;i++){
            var p=mage(c,i==1);var pos=c.getAbsolutePos(new BlockPos(768+i*32,3,768));var chunk=new ChunkPos(pos);
            if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
            var b=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
            b.setAiDisabled(true);b.setNoGravity(true);b.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);
            p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+3,180,0);c.getWorld().spawnEntity(b);players.add(p);bosses.add(b);
            try{b.getClass().getMethod("setSpawning",boolean.class).invoke(b,false);}catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }
        float[] initial=new float[2],healing=new float[2];int[] accepted=new int[2];
        var closed=new java.util.concurrent.atomic.AtomicBoolean();
        Runnable cleanup=()->{
            if(!closed.compareAndSet(false,true))return;
            for(var b:bosses){b.discard();BossScaler.untrack(b);}
            for(var ch:forced)c.getWorld().setChunkForced(ch.x,ch.z,false);
            TestPlayers.finish(c);
        };
        // EmbeddedChannel supplies no movement packets; drive the native player tick like
        // the other sustained-casting fixtures so age, RPG timers and budgets advance.
        for(int tick=99;tick<=200;tick++)c.runAtTick(tick,()->{
            try{players.forEach(p->{p.playerTick();p.tick();});}
            catch(RuntimeException|Error e){cleanup.run();throw e;}
        });
        for(int pulse=0;pulse<10;pulse++){
            final int index=pulse;
            c.runAtTick(100+pulse*10,()->{
                try{
                    var s=spell("RAY_OF_SIPHONING_SPELL");
                    var dataClass=Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                    var castClass=Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
                    Object cast=Arrays.stream(castClass.getEnumConstants()).filter(v->v.toString().equals("SPELLBOOK")).findFirst().orElseThrow();
                    float[] pulseHealing=new float[2];
                    for(int i=0;i<2;i++){
                        var p=players.get(i);var b=bosses.get(i);
                        if(index==0){b.setHealth(b.getMaxHealth());initial[i]=b.getHealth();}
                        p.getRandom().setSeed(5123L);b.getRandom().setSeed(1L);
                        Object data=null;
                        for(var method:dataClass.getMethods())if(method.getName().equals("getPlayerMagicData")&&method.getParameterCount()==1&&method.getParameterTypes()[0].isInstance(p)){data=method.invoke(null,p);break;}
                        c.assertTrue(data!=null,"Missing native MagicData");
                        p.setHealth(p.getMaxHealth()*.5f);float hp=p.getHealth(),before=b.getHealth();
                        // Actual native beam callback: raycast, native source lifetime, hit, post-hit drain.
                        s.getClass().getMethod("onCast",net.minecraft.world.World.class,int.class,LivingEntity.class,castClass,dataClass).invoke(s,c.getWorld(),10,p,cast,data);
                        if(b.getHealth()<before)accepted[i]++;
                        pulseHealing[i]=p.getHealth()-hp;healing[i]+=pulseHealing[i];
                    }
                    c.assertTrue(pulseHealing[1]>=pulseHealing[0]*.9f&&pulseHealing[1]<=pulseHealing[0]*1.2f+.05f,
                            "Native Ray drain changed across repeated beam callbacks");
                }catch(ReflectiveOperationException e){cleanup.run();throw new AssertionError(e);}
                catch(RuntimeException|Error e){cleanup.run();throw e;}
            });
        }
        c.runAtTick(201,()->{
            try{
                float temporal=initial[0]-bosses.get(0).getHealth(),blood=initial[1]-bosses.get(1).getHealth();
                RPGStatsMod.LOGGER.info("RPG_BLOOD_CHANNEL nativeRayCallbacks=10 intervalTicks=10 temporalLoss={} bloodLossWithHemorrhage={} temporalHits={} bloodHits={} temporalHeal={} bloodHeal={}",temporal,blood,accepted[0],accepted[1],healing[0],healing[1]);
                c.assertTrue(accepted[0]==10&&accepted[1]==10,"Native beam missed/rejected a channel pulse");
                c.assertTrue(blood>=temporal*1.35f,"Blood mastery did not survive a real native beam sequence");
            }finally{cleanup.run();}
            c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=100)
    public static void hemomancyAloneStartsOneBleedStack(TestContext c){
        if(!ModList.get().isLoaded("irons_spellbooks")){c.complete();return;}
        var p=TestPlayers.create(c);MobEntity t=null;
        try{
            StatsManager.awaken(p);StatsManager.selectClass(p,RPGClass.MAGO.name());
            int xp=0;for(int i=1;i<25;i++)xp+=PlayerStats.xpToNext(i);StatsManager.addXp(p,xp);
            while(StatsManager.get(p).statPoints>0&&StatsManager.get(p).stats.get(Stat.INTELIGENCIA)<50)StatsManager.allocate(p,"INTELIGENCIA");
            // Buy the mastery chain rather than all optional core nodes: a level25 build
            // needs to keep its earned points for the first specialization talent.
            for(String id:new String[]{"mag_core_awakening","mag_core_channeling","mag_core_efficiency","mag_core_flow","mag_core_knowledge","mag_core_mastery"})StatsManager.unlockNode(p,id);
            StatsManager.selectPath(p,RPGPath.MAGE_OCCULT.name());for(var n:RPGPath.MAGE_OCCULT.nodes)StatsManager.unlockNode(p,n.id());
            StatsManager.selectSpecialization(p,RPGSpecialization.BLOODMANCER.name());StatsManager.unlockNode(p,"mag_blood_lance");
            c.assertTrue(StatsManager.get(p).hasNode("mag_blood_lance")&&!StatsManager.get(p).hasNode("mag_blood_hemorrhage"),"Invalid first-node-only fixture");
            t=EntityType.COW.create(c.getWorld());t.setAiDisabled(true);
            t.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(500);t.setHealth(500);
            t.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(12,3,6)),0,0);c.getWorld().spawnEntity(t);
            var s=spell("RAY_OF_SIPHONING_SPELL");
            for(int i=0;i<3;i++)c.assertTrue(hit(s,p,t,input(s,p,true)),"Native Hemomancy hit rejected");
            var state=MageState.get(p.getUuid()).target(t.getUuid());
            RPGStatsMod.LOGGER.info("RPG_BLOOD_FIRST_NODE level=25 stacks={} duration={}",state.bleedStacks,state.bleedTicks);
            c.assertTrue(state.bleedStacks==1&&state.bleedTicks==120,"Hemomancy alone should grant exactly one refreshed stack");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        finally{if(t!=null)t.discard();TestPlayers.finish(c);}
        c.complete();
    }
    private BloodMageGameTests(){}
}
