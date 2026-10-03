package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.*;
import com.rpgstats.combat.*;
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
import java.lang.reflect.*;
import java.util.*;

@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class NativeMageSpecializationGameTests {
 static boolean iron(){return ModList.get().isLoaded("irons_spellbooks");}
 static ServerPlayerEntity mage(TestContext c,RPGSpecialization spec){
  var p=PhysicalRoleGameTests.player(c,RPGClass.MAGO,50,Stat.INTELIGENCIA);
  StatsManager.selectPath(p,spec.parent.name());
  for(var n:spec.parent.nodes)StatsManager.unlockNode(p,n.id());
  StatsManager.selectSpecialization(p,spec.name());
  for(var n:spec.nodes)StatsManager.unlockNode(p,n.id());
  c.assertTrue(StatsManager.get(p).specialization==spec,"Illegal specialization fixture "+spec);return p;
 }
 static Object spell(String field)throws ReflectiveOperationException{
  return ((net.minecraftforge.registries.RegistryObject<?>)Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry").getField(field).get(null)).get();
 }
 static void castEvent(ServerPlayerEntity p,String field)throws ReflectiveOperationException{
  Object s=spell(field);
  Class<?> spellApi=Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
  Class<?> event=Class.forName("io.redspace.ironsspellbooks.api.events.SpellOnCastEvent");
  var ctor=Arrays.stream(event.getConstructors()).filter(x->x.getParameterCount()==6).findFirst().orElseThrow();
  Class<?> source=ctor.getParameterTypes()[5];
  Object book=Arrays.stream(source.getEnumConstants()).filter(x->x.toString().equals("SPELLBOOK")).findFirst().orElseThrow();
  var e=(net.minecraftforge.eventbus.api.Event)ctor.newInstance(p,spellApi.getMethod("getSpellId").invoke(s),1,10,spellApi.getMethod("getSchoolType").invoke(s),book);
  net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(e);
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeMobilityDamagePreservesNextElementalSpell(TestContext c){
  if(!iron()){c.complete();return;}
  var specs=new RPGSpecialization[]{RPGSpecialization.PYROMANCER,RPGSpecialization.STORMCALLER};
  String[] triggers={"BURNING_DASH_SPELL","CHARGE_SPELL"};String[] next={"FIREBALL_SPELL","LIGHTNING_LANCE_SPELL"};String[] fields={"fireStepTicks","lightningStepTicks"};
  var targets=new ArrayList<LivingEntity>();
  try{for(int i=0;i<specs.length;i++){
   var p=mage(c,specs[i]);var t=EntityType.COW.create(c.getWorld());
   t.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(500);t.setHealth(500);t.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(4+i*5,3,5)),0,0);c.getWorld().spawnEntity(t);targets.add(t);
   castEvent(p,triggers[i]);Object s=spell(triggers[i]);
   Method source=Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell").getMethod("getDamageSource",Entity.class,Entity.class);
   Method apply=Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class);
   apply.invoke(null,t,1f,source.invoke(s,p,p));
   c.assertTrue(MageState.class.getField(fields[i]).getInt(MageState.get(p.getUuid()))>0,"Triggering mobility damage consumed its own next-spell bonus");
   var fresh=EntityType.COW.create(c.getWorld());fresh.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(4+i*5,3,9)),0,0);c.getWorld().spawnEntity(fresh);targets.add(fresh);
   apply.invoke(null,fresh,1f,source.invoke(spell(next[i]),p,p));
   c.assertTrue(MageState.class.getField(fields[i]).getInt(MageState.get(p.getUuid()))==0,"Accepted eligible elemental hit did not consume its bonus");
  }}catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{targets.forEach(Entity::discard);TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeMobilityArmsSpecializationBonuses(TestContext c){
  if(!iron()){c.complete();return;}var fire=mage(c,RPGSpecialization.PYROMANCER);var storm=mage(c,RPGSpecialization.STORMCALLER);var tele=mage(c,RPGSpecialization.TELEMANCER);
  try{castEvent(fire,"BURNING_DASH_SPELL");castEvent(storm,"CHARGE_SPELL");castEvent(tele,"PORTAL_SPELL");
   c.assertTrue(MageState.class.getField("fireStepTicks").getInt(MageState.get(fire.getUuid()))>0,"Burning Dash did not prepare next Fire spell");
   c.assertTrue(MageState.get(storm.getUuid()).accelerationTicks>0,"Thunder Step did not provide acceleration");
   c.assertTrue(MageState.get(tele.getUuid()).teleportBonusTicks>0,"Portal did not prepare Compression");
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeRootUsesItsRealCastTargetForTimeLock(TestContext c){
  if(!iron()){c.complete();return;}var p=mage(c,RPGSpecialization.STAGNATOR);var t=EntityType.ZOMBIE.create(c.getWorld());
  t.setAiDisabled(true);t.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(8,3,8)),0,0);c.getWorld().spawnEntity(t);
  try{
   Class<?> dataType=Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
   Object data=Arrays.stream(dataType.getMethods()).filter(m->m.getName().equals("getPlayerMagicData")&&m.getParameterCount()==1&&m.getParameterTypes()[0].isInstance(p)).findFirst().orElseThrow().invoke(null,p);
   Object targetData=Class.forName("io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData").getConstructor(LivingEntity.class).newInstance(t);
   Arrays.stream(dataType.getMethods()).filter(m->m.getName().equals("setAdditionalCastData")&&m.getParameterCount()==1).findFirst().orElseThrow().invoke(data,targetData);
   castEvent(p,"ROOT_SPELL");
   c.assertTrue(MageState.get(p.getUuid()).target(t.getUuid()).timeLockCooldown>0,"Root utility never reached Time Lock on its actual cast target");
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{t.discard();TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeIceFragilityWorksBeforeDeepFreeze(TestContext c){
  if(!iron()){c.complete();return;}var p=mage(c,RPGSpecialization.CRYOMANCER);
  var stats=StatsManager.get(p);stats.level=30;
  stats.unlockedNodes.removeAll(Set.of("mag_cryo_ice_barrier","mag_cryo_frost_nova","mag_cryo_deep_freeze","mag_cryo_shatter","mag_cryo_asc_winterheart"));
  StatsManager.save(p,stats);
  c.assertTrue(StatsManager.get(p).level==30&&!StatsManager.get(p).unlockedNodes.contains("mag_cryo_deep_freeze"),"Level-30 Ice fixture was not persisted");
  var t=EntityType.COW.create(c.getWorld());t.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(6,3,6)),0,0);c.getWorld().spawnEntity(t);
  try{
   var status=MageState.get(p.getUuid()).target(t.getUuid());status.frost=80f;
   DamageSource source=(DamageSource)Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell")
       .getMethod("getDamageSource",Entity.class,Entity.class).invoke(spell("CONE_OF_COLD_SPELL"),p,p);
   Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class).invoke(null,t,1f,source);
   c.assertTrue(status.fragilityTicks==60&&Math.abs(status.fragilityAmp-.08f)<.0001f&&status.frost==0f,
       "Level-30 native Ice hit did not resolve purchased Fragility without Deep Freeze");
   // Iron's can supply its own chill. Compare to the same native hit below the RPG Frost threshold.
   var control=EntityType.COW.create(c.getWorld());control.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(9,3,6)),0,0);c.getWorld().spawnEntity(control);
   try{
    DamageSource baselineSource=(DamageSource)Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell")
        .getMethod("getDamageSource",Entity.class,Entity.class).invoke(spell("CONE_OF_COLD_SPELL"),p,p);
    Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class).invoke(null,control,1f,baselineSource);
    var slow=t.getStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
    var baseline=control.getStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
    RPGStatsMod.LOGGER.info("RPG_NATIVE_FROST_EARLY frozen={} nativeSlow={} baselineSlow={}",status.frozenTicks,slow,baseline);
    c.assertTrue(status.frozenTicks==0&&(slow==null?baseline==null:baseline!=null
        &&slow.getAmplifier()==baseline.getAmplifier()&&slow.getDuration()==baseline.getDuration()),
        "Level-30 Fragility added unpurchased freeze/slow beyond the native spell");
   }finally{control.discard();}
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{t.discard();TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeIceCastOpensWinterHeart(TestContext c){
  if(!iron()){c.complete();return;}var p=mage(c,RPGSpecialization.CRYOMANCER);
  try{castEvent(p,"FROSTWAVE_SPELL");c.assertTrue(MageState.get(p.getUuid()).winterHeartTicks==160,"Real Ice cast did not activate Winter Heart");}
  catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeIceBlockProvidesBoundedClassGuard(TestContext c){
  if(!iron()){c.complete();return;}var p=mage(c,RPGSpecialization.CRYOMANCER);
  try{castEvent(p,"ICE_BLOCK_SPELL");float loss=MageCombatHandler.modifyIncomingDamage(p,20f,p.getDamageSources().generic());c.assertTrue(loss<=18.01f&&loss>=17.99f,"Ice Barrier modifier did not provide its controlled 10% guard: "+loss);}
  catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeInvisibilityArmsGhostAttack(TestContext c){
  if(!iron()){c.complete();return;}var p=mage(c,RPGSpecialization.ILLUSIONIST);
  try{castEvent(p,"INVISIBILITY_SPELL");c.assertTrue(MageState.get(p.getUuid()).ghostAttackTicks>0,"Native Invisibility did not arm Ghost Attack");}
  catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=80)
 public static void nativeCurseHitArmsWeaknessAndFate(TestContext c){
  if(!iron()){c.complete();return;}var p=mage(c,RPGSpecialization.CURSEWEAVER);var t=EntityType.COW.create(c.getWorld());
  t.refreshPositionAndAngles(c.getAbsolutePos(new BlockPos(6,3,6)),0,0);c.getWorld().spawnEntity(t);
  try{
   Object s=spell("WITHER_SKULL_SPELL");DamageSource source=(DamageSource)s.getClass().getMethod("getDamageSource",Entity.class,Entity.class).invoke(s,p,p);
   Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class).invoke(null,t,2f,source);
   var state=MageState.get(p.getUuid()).target(t.getUuid());
   c.assertTrue(state.weaknessTicks>0&&state.fragilityTicks>0&&state.ruinTicks>0&&state.fateMarkReady,"Native curse hit did not assemble Weakness/Fragility/Ruin/Fate");
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{t.discard();TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=160)
 public static void elementalSpecializationsDealNativeBossDamage(TestContext c){
  if(!iron()||!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
  var specs=new RPGSpecialization[]{RPGSpecialization.PYROMANCER,RPGSpecialization.CRYOMANCER,RPGSpecialization.STORMCALLER,RPGSpecialization.ACCELERATOR};
  String[] fields={"FIREBALL_SPELL","CONE_OF_COLD_SPELL","LIGHTNING_LANCE_SPELL","MAGIC_ARROW_SPELL"};
  String[] entities={"fireball.MagicFireball","cone_of_cold.ConeOfColdProjectile","lightning_lance.LightningLanceProjectile","magic_arrow.MagicArrowProjectile"};
  int[] ranks={5,10,10,10};var ps=new ArrayList<ServerPlayerEntity>();var bs=new ArrayList<MobEntity>();var chunks=new HashSet<ChunkPos>();
  for(int i=0;i<specs.length;i++){
   var p=mage(c,specs[i]);var pos=c.getAbsolutePos(new BlockPos(32768+i*64,3,16384));var ch=new ChunkPos(pos);
   if(!c.getWorld().getForcedChunks().contains(ch.toLong())){chunks.add(ch);c.getWorld().setChunkForced(ch.x,ch.z,true);}
   var b=(MobEntity)net.minecraft.registry.Registries.ENTITY_TYPE.get(new Identifier("soulsweapons:returning_knight")).create(c.getWorld());
   b.setAiDisabled(true);b.setNoGravity(true);b.refreshPositionAndAngles(pos,0,0);p.refreshPositionAndAngles(pos.add(0,0,3),180,0);c.getWorld().spawnEntity(b);
   try{b.getClass().getMethod("setSpawning",boolean.class).invoke(b,false);}catch(ReflectiveOperationException e){throw new AssertionError(e);}ps.add(p);bs.add(b);
  }
  c.runAtTick(100,()->{
   try{
    Method apply=Class.forName("io.redspace.ironsspellbooks.damage.DamageSources").getMethod("applyDamage",Entity.class,float.class,DamageSource.class);
    for(int i=0;i<specs.length;i++){
     var p=ps.get(i);var b=bs.get(i);Object s=spell(fields[i]);
     Entity shot=(Entity)Class.forName("io.redspace.ironsspellbooks.entity.spells."+entities[i]).getConstructor(net.minecraft.world.World.class,LivingEntity.class).newInstance(c.getWorld(),p);
     shot.refreshPositionAndAngles(b.getX(),b.getY()+1,b.getZ(),0,0);
     String getter=i<2?"getDamage":"getSpellPower";
     Method power=Arrays.stream(s.getClass().getMethods()).filter(m->m.getName().equals(getter)&&m.getParameterCount()==2&&m.getParameterTypes()[0]==int.class).findFirst().orElseThrow();
     float input=((Number)power.invoke(s,ranks[i],p)).floatValue();
     DamageSource source=(DamageSource)s.getClass().getMethod("getDamageSource",Entity.class,Entity.class).invoke(s,shot,p);
     b.getRandom().setSeed(1L);p.getRandom().setSeed(5123L);float before=b.getHealth();boolean accepted=(boolean)apply.invoke(null,b,input,source);float loss=before-b.getHealth();
     RPGStatsMod.LOGGER.info("RPG_MAGE_SPECIALIZATION spec={} level=50 INT=50 spell={} rank={} nativeInput={} healthLoss={} accepted={} oneDamageCallback=true",specs[i],fields[i],ranks[i],input,loss,accepted);
     c.assertTrue(accepted&&loss>0,"Native elemental boss hit rejected: "+specs[i]);shot.discard();
    }
   }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{bs.forEach(b->{b.discard();BossScaler.untrack(b);});chunks.forEach(ch->c.getWorld().setChunkForced(ch.x,ch.z,false));TestPlayers.finish(c);}c.complete();
  });
 }
}
