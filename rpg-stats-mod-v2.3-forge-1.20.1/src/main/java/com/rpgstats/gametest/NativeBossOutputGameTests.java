package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.integration.IntegrationServices;
import com.rpgstats.stats.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
/** Actual player output through each native boss's damage override, armor and invulnerability. */
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeBossOutputGameTests {
 @GameTest(templateName="empty",tickLimit=1350)
 public static void observeNativeBossReferenceOutputWindows(TestContext c) {
  java.util.List<MobEntity> bosses=new java.util.ArrayList<>();
  java.util.List<ServerPlayerEntity> players=new java.util.ArrayList<>();
  java.util.List<Float> initial=new java.util.ArrayList<>();
  java.util.Set<ChunkPos> forced=new java.util.HashSet<>();
  for(var id:Registries.ENTITY_TYPE.getIds())if(java.util.Set.of("soulsweapons","legendary_monsters","bosses_of_mass_destruction").contains(id.getNamespace())) {
   var entity=Registries.ENTITY_TYPE.get(id).create(c.getWorld());
   if(!(entity instanceof MobEntity boss)){if(entity!=null)entity.discard();continue;}
   var profile=IntegrationServices.INSTANCE.boss(boss).orElse(null);
   if(profile==null || profile.fixed()==null){boss.discard();continue;}
   var p=TestPlayers.create(c);StatsManager.awaken(p);StatsManager.selectClass(p,RPGClass.GUERREIRO.name());
   int xp=0;for(int level=1;level<profile.fixed().referenceLevel();level++)xp+=PlayerStats.xpToNext(level);StatsManager.addXp(p,xp);
   while(StatsManager.get(p).stats.get(Stat.DESTREZA)<7)StatsManager.allocate(p,"DESTREZA");
   while(StatsManager.get(p).statPoints>0 && StatsManager.get(p).stats.get(Stat.FORCA)<50)StatsManager.allocate(p,"FORCA");
   while(StatsManager.get(p).statPoints>0 && StatsManager.get(p).stats.get(Stat.TENACIDADE)<50)StatsManager.allocate(p,"TENACIDADE");
   for(var node:RPGClass.GUERREIRO.nodes)StatsManager.unlockNode(p,node.id());
   p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.NETHERITE_SWORD));p.setNoGravity(true);p.playerTick();p.tick();
   var stats=StatsManager.get(p);stats.stamina=stats.staminaMax;StatsManager.save(p,stats);
   var pos=c.getAbsolutePos(new BlockPos(128+players.size()*64,2,256));var chunk=new ChunkPos(pos);
   if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);c.getWorld().setChunkForced(chunk.x,chunk.z,true);}
   for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
   p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ()+1,180,0);boss.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);boss.setAiDisabled(true);boss.setNoGravity(true);
   c.getWorld().spawnEntity(boss);boss.setHealth(boss.getMaxHealth());
   try{boss.getClass().getMethod("setSpawning",boolean.class).invoke(boss,false);}catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException error){throw new AssertionError(error);}
   bosses.add(boss);players.add(p);initial.add(boss.getHealth());
  }
  if(bosses.isEmpty()){c.complete();return;}
  for(int tick=1;tick<=1300;tick++)c.runAtTick(tick,()->players.forEach(ServerPlayerEntity::playerTick));
  for(int tick=100;tick<1300;tick+=23)c.runAtTick(tick,()-> {
   for(int i=0;i<players.size();i++)if(bosses.get(i).isAlive()) {
    var p=players.get(i);p.setVelocity(Vec3d.ZERO);p.setSprinting(false);p.getRandom().setSeed(5123L+p.age);
    p.attack(bosses.get(i));
   }
  });
  for(int seconds:new int[]{5,30,60})c.runAtTick(100+seconds*20,()-> {
   for(int i=0;i<bosses.size();i++) {
    var boss=bosses.get(i);float dealt=Math.max(0,initial.get(i)-boss.getHealth());
    RPGStatsMod.LOGGER.info("RPG_BOSS_OUTPUT id={} reference={} seconds={} initialHP={} effectiveDamage={} remainingHP={} stationaryTTK={}",Registries.ENTITY_TYPE.getId(boss.getType()),StatsManager.get(players.get(i)).level,seconds,initial.get(i),dealt,boss.getHealth(),dealt>0?initial.get(i)/(dealt/seconds):-1);
   }
   if(seconds==60){for(var boss:bosses){boss.discard();BossScaler.untrack(boss);}for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);c.complete();}
  });
 }
 private NativeBossOutputGameTests(){}
}
