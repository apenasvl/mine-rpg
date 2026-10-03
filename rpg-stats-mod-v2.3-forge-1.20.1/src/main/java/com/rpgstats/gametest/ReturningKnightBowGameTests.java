package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.stats.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReturningKnightBowGameTests {
 @GameTest(templateName="empty",tickLimit=170)
 public static void actualBowReleaseHurtsFreshKnightWithoutMelee(TestContext c){
  if(!net.minecraftforge.fml.ModList.get().isLoaded("soulsweapons")){c.complete();return;}
  var players=new java.util.ArrayList<ServerPlayerEntity>();var bosses=new java.util.ArrayList<MobEntity>();
  var weapons=new java.util.ArrayList<BowItem>();var forced=new java.util.HashSet<ChunkPos>();
  weapons.add((BowItem)Items.BOW);
  for(String id:new String[]{"galeforce","darkmoon_longbow","simons_bowblade"}){
   Item item=Registries.ITEM.get(new Identifier("soulsweapons",id));if(item instanceof BowItem bow)weapons.add(bow);
  }
  for(int i=0;i<weapons.size();i++){
   var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,"ARQUEIRO");
   int xp=0;for(int l=1;l<50;l++)xp+=PlayerStats.xpToNext(l);StatsManager.addXp(p,xp);
   while(StatsManager.get(p).stats.get(Stat.DESTREZA)<50 && StatsManager.get(p).statPoints>0)StatsManager.allocate(p,"DESTREZA");
   for(var n:RPGClass.ARQUEIRO.nodes)StatsManager.unlockNode(p,n.id());
   p.setStackInHand(Hand.MAIN_HAND,new ItemStack(weapons.get(i)));p.getInventory().insertStack(new ItemStack(Items.ARROW,64));p.setNoGravity(true);
   var pos=c.getAbsolutePos(new BlockPos(16384+i*64,3,8192));
   for(int dx=-1;dx<=1;dx++)for(int dz=0;dz<=1;dz++){
    var chunk=new ChunkPos(pos.add(dx*16,0,dz*16));if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
   }
   for(int x=-3;x<=3;x++)for(int z=-3;z<=20;z++)c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
   var b=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
   b.setNoGravity(true);b.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
   b.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);c.getWorld().spawnEntity(b);
   p.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+16.5,180,0);
   // Real native intro progresses through ticks; never disable AI, force the intro off or set a target.
   try{b.getClass().getMethod("setSpawning",boolean.class).invoke(b,true);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
   players.add(p);bosses.add(b);
  }
  float[] before=new float[players.size()];
  c.runAtTick(100,()->{
   for(int i=0;i<players.size();i++){
    var p=players.get(i);var b=bosses.get(i);p.refreshPositionAndAngles(b.getX(),b.getY(),b.getZ()+16,180,0);p.getRandom().setSeed(1L);
    // EmbeddedChannel has no movement packets to invoke ServerPlayerEntity.playerTick.
    // Advance the same native use countdown as the existing real-bow release fixtures.
    p.setCurrentHand(Hand.MAIN_HAND);for(int draw=0;draw<80;draw++)p.playerTick();
    c.assertTrue(p.isUsingItem() && p.getItemUseTime()>=80,"Bow fixture did not advance the native draw countdown");
    p.refreshPositionAndAngles(b.getX(),b.getY(),b.getZ()+16,180,0);
    before[i]=b.getHealth();
    c.assertTrue(!p.getProjectileType(p.getMainHandStack()).isEmpty(),"Bow flight fixture has no ammunition");
    p.stopUsingItem();
    c.assertTrue(!c.getWorld().getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class,p.getBoundingBox().expand(4),e->e.getOwner()==p).isEmpty(),"Actual bow release created no projectile");
   }
  });
  for(int tick:new int[]{101,105,110,115,125})c.runAtTick(tick,()->{
   for(int i=0;i<players.size();i++){
    var p=players.get(i);var b=bosses.get(i);
    try{
     var shots=c.getWorld().getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class,b.getBoundingBox().expand(40),e->e.getOwner()==p);
     RPGStatsMod.LOGGER.info("RPG_KNIGHT_FLIGHT tick={} bow={} bossPos={} bossAge={} spawning={} playerPos={} alive={} hp={} shots={}",tick,Registries.ITEM.getId(weapons.get(i)),b.getPos(),b.age,b.getClass().getMethod("isSpawning").invoke(b),p.getPos(),p.isAlive(),b.getHealth(),shots.size());
     for(var shot:shots)RPGStatsMod.LOGGER.info("RPG_KNIGHT_SHOT tick={} type={} pos={} velocity={} whitelist={}",tick,Registries.ENTITY_TYPE.getId(shot.getType()),shot.getPos(),shot.getVelocity(),b.getClass().getMethod("isProjectileWhitelisted",Entity.class).invoke(b,shot));
    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
   }
  });
  c.runAtTick(135,()->{
   var failures=new java.util.ArrayList<String>();
   try{
    for(int i=0;i<players.size();i++){
     float lost=before[i]-bosses.get(i).getHealth();String bow=Registries.ITEM.getId(weapons.get(i)).toString();
     RPGStatsMod.LOGGER.info("RPG_KNIGHT_FIRST_BOW weapon={} meleeAttacks=0 nativeAI=true actualRelease=true healthLoss={}",bow,lost);
     if(lost<=0)failures.add(bow);
    }
   }finally{
    for(var p:players)for(var shot:c.getWorld().getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class,p.getBoundingBox().expand(64),e->e.getOwner()==p))shot.discard();
    for(var b:bosses){b.discard();com.rpgstats.boss.BossScaler.untrack(b);}for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);
   }
   c.assertTrue(failures.isEmpty(),"Fresh Returning Knight blocked real bow shots before melee: "+failures);c.complete();
  });
 }
 private ReturningKnightBowGameTests(){}
}
