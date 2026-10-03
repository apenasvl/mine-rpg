package com.rpgstats.gametest;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.combat.CombatHandler;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Executes the pinned native attack branch, never a reconstructed damage formula. */
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossPressureGameTests {
    private static ServerPlayerEntity fixture(TestContext c,RPGClass clazz,int level) {
        var p=TestPlayers.create(c);
        StatsManager.awaken(p);StatsManager.selectClass(p,clazz.name());
        int xp=0;for(int i=1;i<level;i++)xp+=PlayerStats.xpToNext(i);
        StatsManager.addXp(p,xp);
        while(StatsManager.get(p).statPoints>0 && StatsManager.get(p).stats.get(com.rpgstats.stats.Stat.VITALIDADE)<50)
            StatsManager.allocate(p,"VITALIDADE");
        while(StatsManager.get(p).statPoints>0 && StatsManager.get(p).stats.get(com.rpgstats.stats.Stat.TENACIDADE)<50)
            StatsManager.allocate(p,"TENACIDADE");
        for(var node:clazz.nodes) StatsManager.unlockNode(p,node.id());
        if(clazz==RPGClass.GUERREIRO) {
            StatsManager.selectPath(p,RPGPath.WAR_VANGUARD.name());
            for(var node:RPGPath.WAR_VANGUARD.nodes)StatsManager.unlockNode(p,node.id());
        }
        c.assertTrue(StatsManager.get(p).level==level,"Fixture level is not legal");
        var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        var items=new net.minecraft.item.Item[]{Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};
        for(int i=0;i<4;i++){var stack=new ItemStack(items[i]);stack.addEnchantment(Enchantments.PROTECTION,4);p.equipStack(slots[i],stack);}
        p.playerTick();p.tick();p.setHealth(p.getMaxHealth());p.getHungerManager().setFoodLevel(18);p.getHungerManager().setSaturationLevel(0);
        var s=StatsManager.get(p);s.resource=s.resourceMax;s.stamina=s.staminaMax;StatsManager.save(p,s);
        return p;
    }
    private static void field(Object o,String name,Object value) throws ReflectiveOperationException {
        for(Class<?> t=o.getClass();t!=null;t=t.getSuperclass())try {
            var f=t.getDeclaredField(name);f.setAccessible(true);f.set(o,value);return;
        }catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    static void commonHit(MobEntity boss,ServerPlayerEntity player,boolean finalBoss) throws ReflectiveOperationException {
        boss.setTarget(player);
        boss.refreshPositionAndAngles(player.getX(),player.getY(),player.getZ()+1,180,0);boss.bodyYaw=180;
        if(!finalBoss) {
            boss.getClass().getMethod("setAttackState",int.class).invoke(boss,13);
            field(boss,"attackTicks",18);
            var attack=boss.getClass().getDeclaredMethod("updateWithAttack");attack.setAccessible(true);attack.invoke(boss);
        }else {
            boss.getClass().getMethod("setSpawning",boolean.class).invoke(boss,false);
            boss.getClass().getMethod("setMaceOfSpades",boolean.class).invoke(boss,true);
            var type=Class.forName("net.soulsweaponry.entity.ai.goal.ReturningKnightGoal");
            var goal=(net.minecraft.entity.ai.goal.Goal)type.getConstructor(boss.getClass()).newInstance(boss);
            field(goal,"attackCooldown",100);field(goal,"specialCooldown",100);field(goal,"summonCooldown",100);
            field(goal,"attackStatus",6);field(goal,"targetPos",player.getBlockPos());field(goal,"cordsRegistered",true);
            goal.tick();
        }
    }
    @GameTest(templateName="empty",tickLimit=240)
    public static void nativeCommonHitsPressureLegalLevel25(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons") || !ModList.get().isLoaded("legendary_monsters")){c.complete();return;}
        c.assertTrue(c.getWorld().getDifficulty()==net.minecraft.world.Difficulty.NORMAL,"Pressure requires normal difficulty");
        int[] confirmed=new int[8];
        java.util.Set<net.minecraft.util.math.ChunkPos> forced=new java.util.HashSet<>();
        java.util.List<MobEntity> bosses=new java.util.ArrayList<>();
        java.util.List<ServerPlayerEntity> players=new java.util.ArrayList<>();
        java.util.List<Float> initial=new java.util.ArrayList<>();
        java.util.List<Boolean> finalFlags=new java.util.ArrayList<>();
        for(boolean finalBoss:new boolean[]{false,true}) for(RPGClass clazz:RPGClass.values()) {
            var player=fixture(c,clazz,25);int i=players.size();
            var pos=c.getAbsolutePos(new BlockPos(128+i*32,2,0));
            if(!c.getWorld().getForcedChunks().contains(net.minecraft.util.math.ChunkPos.toLong(pos.getX()>>4,pos.getZ()>>4))) {
                forced.add(new net.minecraft.util.math.ChunkPos(pos));c.getWorld().setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
            }c.getWorld().getChunk(pos);
            for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++)
                c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
            player.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);
            var id=new Identifier(finalBoss?"soulsweapons:returning_knight":"legendary_monsters:overgrown_colossus");
            var boss=(MobEntity)Registries.ENTITY_TYPE.get(id).create(c.getWorld());
            c.assertTrue(boss!=null,"Missing native pressure fixture");boss.setAiDisabled(true);
            boss.refreshPositionAndAngles(player.getX(),player.getY(),player.getZ()+1,180,0);
            c.assertTrue(c.getWorld().spawnEntity(boss),"Native fixture did not enter the world");bosses.add(boss);players.add(player);initial.add(player.getMaxHealth());finalFlags.add(finalBoss);
        }
        c.runAtTick(95,()-> {
            for(var p:players) {
                p.setHealth(p.getMaxHealth());
                if(StatsManager.get(p).clazz==RPGClass.GUERREIRO) {
                    StatsManager.selectActiveAbility(p,0,"war_core_utility");CombatHandler.activateAbility(p,0);
                    StatsManager.selectActiveAbility(p,1,"war_van_signature");CombatHandler.activateAbility(p,1);
                }
            }
        });
        for(int hit=0;hit<5;hit++) {
            final int number=hit;
            c.runAtTick(100+hit*23,()-> {
                try {
                    for(int i=0;i<players.size();i++) {
                        var p=players.get(i);if(!p.isAlive() || confirmed[i]>=3)continue;
                        float before=p.getHealth();
                        var b=bosses.get(i);
                        RPGStatsMod.LOGGER.info("RPG_PRESSURE_CONTEXT bossPos={} playerPos={} nearby={} invulnerable={} armor={} toughness={}",
                                b.getPos(),p.getPos(),c.getWorld().getEntitiesByClass(net.minecraft.entity.LivingEntity.class,b.getBoundingBox().expand(6),e->e==p).size(),
                                p.isInvulnerableTo(b.getDamageSources().mobAttack(b)),p.getArmor(),p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
                        commonHit(b,p,finalFlags.get(i));
                        float loss=before-p.getHealth();if(loss>0)confirmed[i]++;
                        RPGStatsMod.LOGGER.info("RPG_PRESSURE boss={} class={} level=25 hit={} maxHP={} damage={} remaining={}",
                                Registries.ENTITY_TYPE.getId(bosses.get(i).getType()),StatsManager.get(p).clazz,confirmed[i],initial.get(i),loss,p.getHealth());
                        if(loss<=0)RPGStatsMod.LOGGER.info("RPG_PRESSURE_UNCONFIRMED boss={} class={}",Registries.ENTITY_TYPE.getId(b.getType()),StatsManager.get(p).clazz);
                    }
                }catch(ReflectiveOperationException e){for(var b:bosses)b.discard();for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);c.assertTrue(false,"Native attack invocation failed: "+e);}
            });
        }
        c.runAtTick(200,()-> {
            boolean good=true;
            for(int i=0;i<players.size();i++) {
                float loss=initial.get(i)-players.get(i).getHealth();
                good&=finalFlags.get(i)?!players.get(i).isAlive():(loss>=initial.get(i)*.75f && (confirmed[i]==3 || !players.get(i).isAlive()));
            }
            for(var b:bosses)b.discard();for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);
            c.assertTrue(good,"Native common hits are too weak against a legal level25 build");c.complete();
        });
    }
    @GameTest(templateName="empty",tickLimit=80)
    public static void higherLevelHasRealDefensiveProgression(TestContext c) {
        var low=fixture(c,RPGClass.GUERREIRO,25);var high=fixture(c,RPGClass.GUERREIRO,45);
        try {
            c.assertTrue(high.getMaxHealth()>low.getMaxHealth()+10,"Levels25 and45 still have identical maximum defensive health");
        }finally{TestPlayers.finish(c);}
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=180)
    public static void commonNativeHitIsSurvivableAtReferenceLevel(TestContext c) {
        if(!ModList.get().isLoaded("soulsweapons") || !ModList.get().isLoaded("legendary_monsters")){c.complete();return;}
        java.util.Set<net.minecraft.util.math.ChunkPos> forced=new java.util.HashSet<>();
        java.util.List<MobEntity> bosses=new java.util.ArrayList<>();
        java.util.List<ServerPlayerEntity> players=new java.util.ArrayList<>();
        java.util.List<Boolean> finals=new java.util.ArrayList<>();
        for(boolean endgame:new boolean[]{false,true})for(RPGClass clazz:RPGClass.values()) {
            var p=fixture(c,clazz,endgame?45:35);int i=players.size();
            var pos=c.getAbsolutePos(new BlockPos(128+i*32,2,64));
            if(!c.getWorld().getForcedChunks().contains(net.minecraft.util.math.ChunkPos.toLong(pos.getX()>>4,pos.getZ()>>4))) {
                forced.add(new net.minecraft.util.math.ChunkPos(pos));c.getWorld().setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
            }
            for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++)
                c.getWorld().setBlockState(pos.add(x,-1,z),net.minecraft.block.Blocks.STONE.getDefaultState());
            p.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);
            var boss=(MobEntity)Registries.ENTITY_TYPE.get(new Identifier(endgame?"soulsweapons:returning_knight":"legendary_monsters:overgrown_colossus")).create(c.getWorld());
            boss.setAiDisabled(true);boss.refreshPositionAndAngles(p.getX(),p.getY(),p.getZ()+1,180,0);
            c.assertTrue(c.getWorld().spawnEntity(boss),"Reference boss failed to spawn");
            players.add(p);bosses.add(boss);finals.add(endgame);
        }
        c.runAtTick(100,()-> {
            boolean valid=true;
            try {
                for(int i=0;i<players.size();i++) {
                    var p=players.get(i);p.setHealth(p.getMaxHealth());float before=p.getHealth();
                    commonHit(bosses.get(i),p,finals.get(i));float loss=before-p.getHealth();
                    RPGStatsMod.LOGGER.info("RPG_REFERENCE boss={} class={} level={} maxHP={} damage={} remaining={}",
                            Registries.ENTITY_TYPE.getId(bosses.get(i).getType()),StatsManager.get(p).clazz,StatsManager.get(p).level,before,loss,p.getHealth());
                    valid&=loss>0 && p.isAlive();
                }
            }catch(ReflectiveOperationException error){valid=false;}
            finally {for(var boss:bosses)boss.discard();for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);TestPlayers.finish(c);}
            c.assertTrue(valid,"A native common attack is unconfirmed or lethal at its reference level");c.complete();
        });
    }
    private BossPressureGameTests(){}
}
