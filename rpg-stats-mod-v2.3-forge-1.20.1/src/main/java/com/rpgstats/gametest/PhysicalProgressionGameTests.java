package com.rpgstats.gametest;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.stats.*;
import com.rpgstats.RPGStatsMod;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.gametest.*;

@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhysicalProgressionGameTests {
 @GameTest(templateName="empty",tickLimit=80)
 public static void physicalLevelGrowthIsVisibleWithTheSameWeaponAndAttribute(TestContext c){
  var players=new java.util.ArrayList<ServerPlayerEntity>();
  var targets=new java.util.ArrayList<MobEntity>();
  var forced=new java.util.HashSet<net.minecraft.util.math.ChunkPos>();
  for(var clazz:new RPGClass[]{RPGClass.GUERREIRO,RPGClass.ASSASSINO,RPGClass.ARQUEIRO})for(int level:new int[]{25,50}){
   var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,clazz.name());
   int xp=0;for(int l=1;l<level;l++)xp+=PlayerStats.xpToNext(l);StatsManager.addXp(p,xp);
   Stat primary=clazz==RPGClass.GUERREIRO?Stat.FORCA:Stat.DESTREZA;
   while(StatsManager.get(p).stats.get(primary)<25 && StatsManager.get(p).statPoints>0)StatsManager.allocate(p,primary.name());
   c.assertTrue(StatsManager.get(p).stats.get(primary)==25,"Not enough legal points for the equal-attribute fixture");
   StatsManager.unlockNode(p,clazz.nodes.get(0).id());
   p.setStackInHand(Hand.MAIN_HAND,new ItemStack(clazz==RPGClass.ARQUEIRO?Items.BOW:Items.IRON_SWORD));
   p.setNoGravity(true);p.playerTick();p.tick();
   var target=EntityType.COW.create(c.getWorld());target.setAiDisabled(true);target.setNoGravity(true);
   target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(200);target.setHealth(200);
   // GameTests run together: keep unarmored targets outside other tests' AoE/sweeping attacks.
   var pos=c.getAbsolutePos(new BlockPos(8192+players.size()*8,3,4096));
   var chunk=new net.minecraft.util.math.ChunkPos(pos);
   if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
   target.setInvulnerable(true);target.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);
   c.getWorld().spawnEntity(target);p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);
   players.add(p);targets.add(target);
  }
  for(int tick=1;tick<40;tick++)c.runAtTick(tick,()->players.forEach(p->{p.playerTick();p.tick();}));
  c.runAtTick(40,()->{
   var damage=new java.util.ArrayList<Float>();
   try{
    for(int i=0;i<players.size();i++){
     var p=players.get(i);var target=targets.get(i);target.setInvulnerable(false);target.setHealth(200);p.getRandom().setSeed(5123L);float before=target.getHealth();
     if(StatsManager.get(p).clazz==RPGClass.ARQUEIRO){
      var arrow=new net.minecraft.entity.projectile.ArrowEntity(c.getWorld(),p);
      target.damage(p.getDamageSources().arrow(arrow,p),8f);arrow.discard();
     }else p.attack(target);
     float hit=before-target.getHealth();damage.add(hit);
     RPGStatsMod.LOGGER.info("RPG_PHYSICAL_PROGRESS class={} level={} primary=25 sameWeapon=true damage={}",StatsManager.get(p).clazz,StatsManager.get(p).level,hit);
    }
    for(int i=0;i<players.size();i+=2)c.assertTrue(damage.get(i)>0 && damage.get(i+1)>damage.get(i)*1.10f,"Physical level progression is invisible: "+StatsManager.get(players.get(i)).clazz+" -> "+damage.get(i)+" / "+damage.get(i+1));
   }finally{targets.forEach(Entity::discard);for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
   c.complete();
  });
 }
 @GameTest(templateName="empty",tickLimit=150)
 public static void playerBowCanDamageProjectileBlockingMariumBosses(TestContext c){
  if(!net.minecraftforge.fml.ModList.get().isLoaded("soulsweapons")){c.complete();return;}
  var players=new java.util.ArrayList<ServerPlayerEntity>();var bosses=new java.util.ArrayList<MobEntity>();
  var forced=new java.util.HashSet<net.minecraft.util.math.ChunkPos>();
  for(String id:new String[]{"returning_knight","moonknight"}){
   var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,RPGClass.ARQUEIRO.name());
   int xp=0;for(int l=1;l<50;l++)xp+=PlayerStats.xpToNext(l);StatsManager.addXp(p,xp);
   while(StatsManager.get(p).stats.get(Stat.DESTREZA)<50 && StatsManager.get(p).statPoints>0)StatsManager.allocate(p,"DESTREZA");
   for(var n:RPGClass.ARQUEIRO.nodes)StatsManager.unlockNode(p,n.id());p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.BOW));
   var pos=c.getAbsolutePos(new BlockPos(512+players.size()*32,3,448));var chunk=new net.minecraft.util.math.ChunkPos(pos);
   if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
   var b=(MobEntity)net.minecraft.registry.Registries.ENTITY_TYPE.get(new net.minecraft.util.Identifier("soulsweapons",id)).create(c.getWorld());
   b.setAiDisabled(true);b.setNoGravity(true);b.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+12,180,0);
   c.getWorld().spawnEntity(b);b.setHealth(b.getMaxHealth());players.add(p);bosses.add(b);
   try{b.getClass().getMethod("setSpawning",boolean.class).invoke(b,false);}catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException e){throw new AssertionError(e);}
  }
  c.runAtTick(100,()->{
   var failed=new java.util.ArrayList<String>();
   try{
    for(int i=0;i<players.size();i++){
     var p=players.get(i);var b=bosses.get(i);var arrow=new net.minecraft.entity.projectile.ArrowEntity(c.getWorld(),p);
     float before=b.getHealth();boolean hit=b.damage(p.getDamageSources().arrow(arrow,p),8f);float loss=before-b.getHealth();
     String id=net.minecraft.registry.Registries.ENTITY_TYPE.getId(b.getType()).toString();
     RPGStatsMod.LOGGER.info("RPG_ARCHER_BOSS boss={} level=50 nativeInput=8 accepted={} healthLoss={}",id,hit,loss);
     if(!hit || loss<=0)failed.add(id);arrow.discard();
     var unowned=new net.minecraft.entity.projectile.ArrowEntity(c.getWorld(),b.getX(),b.getY(),b.getZ());
     c.assertTrue(!(boolean)b.getClass().getMethod("isProjectileWhitelisted",Entity.class).invoke(b,unowned),"Compatibility allowed unowned/NPC arrows");unowned.discard();
    }
   }catch(ReflectiveOperationException e){throw new AssertionError(e);}
   finally{for(var b:bosses){b.discard();com.rpgstats.boss.BossScaler.untrack(b);}for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
   c.assertTrue(failed.isEmpty(),"Player bow was rejected by Marium bosses: "+failed);c.complete();
  });
 }
 private PhysicalProgressionGameTests(){}
}
