package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraftforge.gametest.*;

/** Same legal Arcana build/level/spell/equipment; only Intelligence differs. */
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MageIntelligenceGameTests {
 @GameTest(templateName="empty",tickLimit=100)
 public static void intelligenceImprovesRealIronDamageOnUnarmoredTargets(TestContext c){
  if(!net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")){c.complete();return;}
  var players=new java.util.ArrayList<ServerPlayerEntity>();
  var targets=new java.util.ArrayList<MobEntity>();
  var forced=new java.util.HashSet<ChunkPos>();
  for(int intelligence:new int[]{10,50}){
   var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,RPGClass.MAGO.name());
   int xp=0;for(int l=1;l<50;l++)xp+=PlayerStats.xpToNext(l);StatsManager.addXp(p,xp);
   while(StatsManager.get(p).stats.get(Stat.INTELIGENCIA)<intelligence && StatsManager.get(p).statPoints>0) StatsManager.allocate(p,"INTELIGENCIA");
   c.assertTrue(StatsManager.get(p).stats.get(Stat.INTELIGENCIA)==intelligence,"Illegal Intelligence allocation");
   for(var n:RPGClass.MAGO.nodes)StatsManager.unlockNode(p,n.id());
   StatsManager.selectPath(p,RPGPath.MAGE_ARCANA.name());
   for(var n:RPGPath.MAGE_ARCANA.nodes)StatsManager.unlockNode(p,n.id());
   players.add(p);
   for(int spell=0;spell<3;spell++){
    var pos=c.getAbsolutePos(new BlockPos(12288+targets.size()*8,3,6144));var chunk=new ChunkPos(pos);
    if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
    var target=EntityType.COW.create(c.getWorld());target.setAiDisabled(true);target.setNoGravity(true);target.setInvulnerable(true);
    target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(400);target.setHealth(400);
    target.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);c.getWorld().spawnEntity(target);targets.add(target);
   }
  }
  c.runAtTick(40,()->{
   var damage=new float[2][3];
   String[] spells={"MAGIC_ARROW_SPELL","FIREBALL_SPELL","ELDRITCH_BLAST_SPELL"};
   try{
    var registry=Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
    var projectileType=Class.forName("io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile");
    var apply=Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class);
    for(int build=0;build<2;build++)for(int i=0;i<spells.length;i++){
     var p=players.get(build);var target=targets.get(build*3+i);target.setInvulnerable(false);target.setHealth(400);
     p.refreshPositionAndAngles(target.getX(),target.getY(),target.getZ()+3,180,0);p.getRandom().setSeed(1L);
     Object spell=((net.minecraftforge.registries.RegistryObject<?>)registry.getField(spells[i]).get(null)).get();
     Entity projectile=(Entity)projectileType.getConstructor(net.minecraft.world.World.class,LivingEntity.class).newInstance(c.getWorld(),p);
     DamageSource source=(DamageSource)spell.getClass().getMethod("getDamageSource",Entity.class,Entity.class).invoke(spell,projectile,p);
     float input=((Number)spell.getClass().getMethod("getSpellPower",int.class,Entity.class).invoke(spell,10,p)).floatValue();
     float before=target.getHealth();boolean accepted=(boolean)apply.invoke(null,target,input,source);damage[build][i]=before-target.getHealth();
     RPGStatsMod.LOGGER.info("RPG_MAGE_INT class=ARCANA level=50 intelligence={} spell={} nativeInput={} accepted={} actualDamage={}",StatsManager.get(p).stats.get(Stat.INTELIGENCIA),spells[i],input,accepted,damage[build][i]);projectile.discard();
    }
    for(int i=0;i<spells.length;i++)c.assertTrue(damage[0][i]>0 && damage[1][i]>damage[0][i]*1.30f,"Intelligence erased by spell guards: "+spells[i]+" -> "+damage[0][i]+" / "+damage[1][i]);
   }catch(ReflectiveOperationException e){throw new AssertionError("Native Iron Intelligence fixture failed",e);}
   finally{targets.forEach(Entity::discard);for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
   c.complete();
  });
 }
 private MageIntelligenceGameTests(){}
}
