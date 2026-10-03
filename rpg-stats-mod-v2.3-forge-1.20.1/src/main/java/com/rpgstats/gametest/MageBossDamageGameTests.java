package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;

/** Real Iron damage source and native boss hurt overrides, including projectile immunity. */
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MageBossDamageGameTests {
 @GameTest(templateName="empty",tickLimit=160)
 public static void arcaneMageProjectileCanDamageNativeMariumBosses(TestContext c) {
  if(!ModList.get().isLoaded("soulsweapons") || !ModList.get().isLoaded("irons_spellbooks")){c.complete();return;}
  var players=new java.util.ArrayList<ServerPlayerEntity>();
  var bosses=new java.util.ArrayList<MobEntity>();
  var forced=new java.util.HashSet<ChunkPos>();
  for(String id:new String[]{"returning_knight","moonknight","night_prowler","accursed_lord_boss","chaos_monarch"}){
   var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,RPGClass.MAGO.name());
   int xp=0;for(int i=1;i<50;i++)xp+=PlayerStats.xpToNext(i);StatsManager.addXp(p,xp);
   while(StatsManager.get(p).statPoints>0 && StatsManager.get(p).stats.get(Stat.INTELIGENCIA)<50)StatsManager.allocate(p,"INTELIGENCIA");
   for(var node:RPGClass.MAGO.nodes)StatsManager.unlockNode(p,node.id());
   StatsManager.selectPath(p,RPGPath.MAGE_ARCANA.name());
   for(var node:RPGPath.MAGE_ARCANA.nodes)StatsManager.unlockNode(p,node.id());
   var pos=c.getAbsolutePos(new BlockPos(256+players.size()*32,3,384));var chunk=new ChunkPos(pos);
   if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
   var b=(MobEntity)net.minecraft.registry.Registries.ENTITY_TYPE.get(new Identifier("soulsweapons",id)).create(c.getWorld());
   c.assertTrue(b!=null,"Missing native Marium boss: "+id);b.setAiDisabled(true);b.setNoGravity(true);
   b.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+3,180,0);
   c.getWorld().spawnEntity(b);b.setHealth(b.getMaxHealth());players.add(p);bosses.add(b);
   try{b.getClass().getMethod("setSpawning",boolean.class).invoke(b,false);}catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException e){throw new AssertionError(e);}
  }
  c.runAtTick(100,()->{
   var failures=new java.util.ArrayList<String>();
   try{
    Object spell=((net.minecraftforge.registries.RegistryObject<?>)Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry").getField("MAGIC_ARROW_SPELL").get(null)).get();
    var projectileType=Class.forName("io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile");
    var nativeDamage=Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class);
    for(int i=0;i<bosses.size();i++){
     var p=players.get(i);var b=bosses.get(i);
     Entity projectile=(Entity)projectileType.getConstructor(net.minecraft.world.World.class,LivingEntity.class).newInstance(c.getWorld(),p);
     projectile.refreshPositionAndAngles(b.getX(),b.getY()+1,b.getZ(),0,0);
     DamageSource source=(DamageSource)spell.getClass().getMethod("getDamageSource",Entity.class,Entity.class).invoke(spell,projectile,p);
     float input=((Number)spell.getClass().getMethod("getSpellPower",int.class,Entity.class).invoke(spell,10,p)).floatValue();
     // Fix the native teleport roll for this damage-path fixture; live dodge mechanics stay native.
     b.getRandom().setSeed(1L);
     float before=b.getHealth();boolean accepted=(boolean)nativeDamage.invoke(null,b,input,source);float loss=before-b.getHealth();
     String id=net.minecraft.registry.Registries.ENTITY_TYPE.getId(b.getType()).toString();
     if(id.endsWith(":returning_knight") || id.endsWith(":moonknight")){
      var arrow=new net.minecraft.entity.projectile.ArrowEntity(c.getWorld(),b.getX(),b.getY(),b.getZ());
      c.assertTrue(!(boolean)b.getClass().getMethod("isProjectileWhitelisted",Entity.class).invoke(b,arrow),"Compatibility removed native unowned-arrow immunity: "+id);
      arrow.discard();
     }
     RPGStatsMod.LOGGER.info("RPG_MAGE_BOSS boss={} level={} spell=magic_arrow nativeInput={} accepted={} healthLoss={} maxHP={}",id,StatsManager.get(p).level,input,accepted,loss,b.getMaxHealth());
     if(!accepted || loss<=0 || (id.endsWith(":returning_knight") && loss<43f) || (id.endsWith(":moonknight") && loss<41f))failures.add(id);projectile.discard();
    }
   }catch(ReflectiveOperationException e){throw new AssertionError("Native Iron spell fixture failed",e);}
   finally{for(var b:bosses){b.discard();BossScaler.untrack(b);}for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
   c.assertTrue(failures.isEmpty(),"Arcane Mage projectile was rejected by native bosses: "+failures);c.complete();
  });
 }
 private MageBossDamageGameTests(){}
}
