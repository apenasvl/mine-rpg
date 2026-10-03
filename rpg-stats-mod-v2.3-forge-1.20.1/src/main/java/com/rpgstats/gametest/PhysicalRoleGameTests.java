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
@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class PhysicalRoleGameTests {
 static ServerPlayerEntity player(TestContext c,RPGClass clazz,int level,Stat primary){
  var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,clazz.name());
  int xp=0;for(int l=1;l<level;l++)xp+=PlayerStats.xpToNext(l);StatsManager.addXp(p,xp);
  while(StatsManager.get(p).stats.get(primary)<50 && StatsManager.get(p).statPoints>0)StatsManager.allocate(p,primary.name());
  if(primary==Stat.TENACIDADE)while(StatsManager.get(p).stats.get(Stat.VITALIDADE)<50 && StatsManager.get(p).statPoints>0)StatsManager.allocate(p,"VITALIDADE");
  for(var n:clazz.nodes)StatsManager.unlockNode(p,n.id());p.setNoGravity(true);p.setHealth(p.getMaxHealth());return p;
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void mobileClassesMoveFasterAndOnlyArcherDrawsFaster(TestContext c){
  var a=player(c,RPGClass.ARQUEIRO,50,Stat.DESTREZA);var m=player(c,RPGClass.MAGO,50,Stat.INTELIGENCIA);var s=player(c,RPGClass.ASSASSINO,50,Stat.DESTREZA);
  try{
   double mage=m.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED),archer=a.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED),assassin=s.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
   c.assertTrue(archer>mage*1.15 && assassin>mage*1.20,"Ranged/Assassin movement still matches Mage");
   for(int i=0;i<3;i++)StatsApplier.apply(a);
   c.assertTrue(Math.abs(a.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)-archer)<.00001,"Movement modifiers stack when stats refresh");
   for(var p:new ServerPlayerEntity[]{a,m,s}){p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.BOW));p.getInventory().setStack(5,new ItemStack(Items.ARROW,16));p.setCurrentHand(Hand.MAIN_HAND);for(int tick=0;tick<15;tick++)p.playerTick();}
   RPGStatsMod.LOGGER.info("RPG_ROLES_MOBILITY archerMove={} assassinMove={} mageMove={} archerDraw={} mageDraw={} assassinDraw={} realTicks=15",archer,assassin,mage,a.getItemUseTime(),m.getItemUseTime(),s.getItemUseTime());
   c.assertTrue(a.getItemUseTime()>=20 && m.getItemUseTime()==15 && s.getItemUseTime()==15,"Archer cannot fully draw sooner / other classes acquired Archer draw cadence");
  }finally{for(var p:new ServerPlayerEntity[]{a,m,s})p.clearActiveItem();TestPlayers.finish(c);}
  c.complete();
 }
 @GameTest(templateName="empty",tickLimit=160)
 public static void warriorSurvivesNativeKnightHitBetterAtEndgame(TestContext c){
  if(!net.minecraftforge.fml.ModList.get().isLoaded("soulsweapons")){c.complete();return;}
  var players=new java.util.ArrayList<ServerPlayerEntity>();var bosses=new java.util.ArrayList<MobEntity>();var forced=new java.util.HashSet<ChunkPos>();
  for(int i=0;i<3;i++){
   var p=player(c,i==0?RPGClass.MAGO:RPGClass.GUERREIRO,i==1?25:50,Stat.TENACIDADE);
   var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
   var armor=new Item[]{Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};
   for(int n=0;n<4;n++){var stack=new ItemStack(armor[n]);stack.addEnchantment(net.minecraft.enchantment.Enchantments.PROTECTION,4);p.equipStack(slots[n],stack);}p.playerTick();p.tick();p.setHealth(p.getMaxHealth());
   var pos=c.getAbsolutePos(new BlockPos(24576+i*64,3,12288));var chunk=new ChunkPos(pos);if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
   for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
   var b=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());b.setAiDisabled(true);b.setNoGravity(true);b.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);c.getWorld().spawnEntity(b);players.add(p);bosses.add(b);
  }
  c.runAtTick(100,()->{
   float[] loss=new float[3];
   try{
    for(int i=0;i<3;i++){var p=players.get(i);p.setHealth(p.getMaxHealth());float before=p.getHealth();BossPressureGameTests.commonHit(bosses.get(i),p,true);loss[i]=before-p.getHealth();RPGStatsMod.LOGGER.info("RPG_ROLES_DEFENSE class={} level={} sameNetheriteProtection4=true nativeAttack=true loss={} hearts={}",StatsManager.get(p).clazz,StatsManager.get(p).level,loss[i],loss[i]/2);}
    c.assertTrue(loss[2]>0 && players.get(2).isAlive() && loss[2]<loss[0]*.8f && loss[2]<loss[1]*.9f,"Warrior close-boss defense does not follow level / match melee role");
   }catch(ReflectiveOperationException e){throw new AssertionError(e);}
   finally{for(var b:bosses){b.discard();com.rpgstats.boss.BossScaler.untrack(b);}for(var ch:forced)c.getWorld().setChunkForced(ch.x,ch.z,false);TestPlayers.finish(c);}
   c.complete();
  });
 }
 @GameTest(templateName="empty",tickLimit=100)
 public static void assassinOpeningRewardsPositionInsteadOfUnconditionalDamage(TestContext c){
  var front=player(c,RPGClass.ASSASSINO,50,Stat.DESTREZA);var rear=player(c,RPGClass.ASSASSINO,50,Stat.DESTREZA);
  var targets=new java.util.ArrayList<MobEntity>();
  try{
   float[] damage=new float[2];int i=0;
   for(var p:new ServerPlayerEntity[]{front,rear}){
    var t=EntityType.COW.create(c.getWorld());t.setAiDisabled(true);t.setNoGravity(true);t.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(200);t.setHealth(200);t.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(6+i*5,3,6)),0,0);c.getWorld().spawnEntity(t);targets.add(t);
    p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.refreshPositionAndAngles(t.getX(),t.getY(),t.getZ()+(i==0?2:-2),0,0);p.getRandom().setSeed(5123L);float before=t.getHealth();t.damage(p.getDamageSources().playerAttack(p),8f);damage[i]=before-t.getHealth();i++;
   }
   RPGStatsMod.LOGGER.info("RPG_ROLES_ASSASSIN level=50 front={} opening={} source=realPlayerAttack",damage[0],damage[1]);
   c.assertTrue(damage[0]>0 && damage[1]>=damage[0]*1.16f,"Assassin opening is too weak for its burst role");
  }finally{targets.forEach(Entity::discard);TestPlayers.finish(c);}
  c.complete();
 }
 private PhysicalRoleGameTests(){}
}
