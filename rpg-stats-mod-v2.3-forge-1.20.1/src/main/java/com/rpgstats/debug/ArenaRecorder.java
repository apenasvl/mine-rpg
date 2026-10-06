package com.rpgstats.debug;

import com.google.gson.*;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.*;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.CombatState;
import com.rpgstats.compat.CompatManager;
import com.rpgstats.stats.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;
import java.util.*;

/** Opt-in real-fight observer. Never changes combat, AI, resources or progression. */
public final class ArenaRecorder {
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> BUILDS=Set.of("offensive","balanced","defensive");
    public static boolean start(ServerPlayerEntity player,LivingEntity boss,String build) {
        if(player==null || boss==null || !BUILDS.contains(build) || ACTIVE.containsKey(player.getUuid()) || ACTIVE.size()>=8
                || !player.isAlive() || !boss.isAlive() || player.getWorld()!=boss.getWorld() || !BossScaler.isCandidate(boss)
                || StatsManager.get(player).clazz==null)return false;
        ACTIVE.put(player.getUuid(),new Session(player,boss,build));
        player.sendMessage(Text.literal("[RPG Arena] Gravando luta real; /rpg debug arena stop encerra."),false);return true;
    }
    public static void confirmedDamage(LivingEntity victim,DamageSource source,float actual) {
        if(ACTIVE.isEmpty() || !Float.isFinite(actual) || actual<=0)return;
        for(var s:ACTIVE.values()) {
            if(victim==s.boss) {
                if(source.getAttacker()==s.player){s.dealt+=actual;s.hits++;}
                else {s.otherDamage+=actual;if(source.getAttacker()!=null)s.otherAttackers.add(source.getAttacker().getUuid());}
            }
            if(victim==s.player) {
                s.received+=actual;s.pendingDamage+=actual;
                if(source.getAttacker()==s.boss || BossLaunchTracker.fallBoss(s.player,source)==s.boss)s.attributed+=actual;
                String key=source.getName()+"|"+(source.getAttacker()==null?"unattributed":Registries.ENTITY_TYPE.getId(source.getAttacker().getType()));
                s.sources.merge(key,(double)actual,Double::sum);
            }
        }
    }
    public static void confirmedDeath(LivingEntity entity) {
        if(ACTIVE.isEmpty())return;
        for(var s:new ArrayList<>(ACTIVE.values())) {
            if(entity==s.boss)stop(s.player,"BOSS_DEAD");
            else if(entity==s.player)stop(s.player,"PLAYER_DEAD");
        }
    }
    public static void tick(MinecraftServer server) {
        if(ACTIVE.isEmpty())return;
        for(var s:new ArrayList<>(ACTIVE.values())) {
            if(s.player.getServer()!=server)continue;
            if(s.player.isRemoved() || s.boss.isRemoved() || s.player.getWorld()!=s.boss.getWorld() || s.player.getWorld()!=s.world)
                stop(s.player,"INTERRUPTED");
            else if(s.elapsed()>12000 || System.nanoTime()-s.startedNano>1_200_000_000_000L)stop(s.player,"TIMEOUT");
            else s.sample(false);
        }
    }
    public static void mark(ServerPlayerEntity player,String note) {
        if(player==null)return;var s=ACTIVE.get(player.getUuid());if(s==null || s.marks.size()>=128)return;
        var mark=new JsonObject();mark.addProperty("tick",s.elapsed());mark.addProperty("note",note.substring(0,Math.min(80,note.length())));s.marks.add(mark);
    }
    public static JsonObject stop(ServerPlayerEntity player,String outcome) {
        if(player==null)return null;var s=ACTIVE.remove(player.getUuid());if(s==null)return null;
        s.sample(true);var report=s.report(outcome);
        try {
            var directory=FMLPaths.GAMEDIR.get().resolve("rpgstats/arena");Files.createDirectories(directory);
            var file=directory.resolve(s.id+".json");Files.writeString(file,JSON.toJson(report)+"\n");
            RPGStatsMod.LOGGER.info("RPG_ARENA_REPORT {} outcome={} ticks={} dealt={} received={}",file,outcome,s.elapsed(),s.dealt,s.received);
            player.sendMessage(Text.literal("[RPG Arena] "+outcome+"; relatório: rpgstats/arena/"+s.id+".json"),false);
        }catch(java.io.IOException e){RPGStatsMod.LOGGER.error("Cannot save real arena report {}",s.id,e);player.sendMessage(Text.literal("[RPG Arena] Falha ao salvar relatório; consulte o log."),false);}
        return report;
    }
    public static void clear() {for(var s:new ArrayList<>(ACTIVE.values()))stop(s.player,"SERVER_STOPPED");}
    private static final class Session {
        final UUID id=UUID.randomUUID();final ServerPlayerEntity player;final LivingEntity boss;final net.minecraft.world.World world;
        final long startedTick,startedNano=System.nanoTime();final String build;
        final JsonObject initial=new JsonObject();final JsonArray samples=new JsonArray(),marks=new JsonArray();
        final Map<String,Double> sources=new TreeMap<>();final Set<UUID> otherAttackers=new HashSet<>();
        final boolean fullBoss;double dealt,received,attributed,otherDamage,pendingDamage,recovery,resourceDepleted,staminaDepleted;
        float lastHp,lastResource,lastStamina;long lastSample=-20;int hits;
        Session(ServerPlayerEntity p,LivingEntity b,String build) {
            player=p;boss=b;world=p.getWorld();startedTick=world.getTime();this.build=build;
            var stats=StatsManager.get(p);lastHp=p.getHealth();lastResource=resource(p,stats);lastStamina=stats.stamina;
            fullBoss=Math.abs(b.getHealth()-b.getMaxHealth())<.01;
            initial.addProperty("registry_id",Registries.ENTITY_TYPE.getId(b.getType()).toString());initial.addProperty("boss_uuid",b.getUuid().toString());
            initial.addProperty("class",stats.clazz.name());initial.addProperty("specialization",stats.specialization==null?"":stats.specialization.name());initial.addProperty("level",stats.level);
            initial.addProperty("declared_build",build);initial.addProperty("build_validated",false);initial.addProperty("boss_hp",b.getHealth());initial.addProperty("boss_max_hp",b.getMaxHealth());initial.addProperty("boss_armor",b.getArmor());
            initial.addProperty("boss_ai_disabled",b instanceof net.minecraft.entity.mob.MobEntity mob && mob.isAiDisabled());initial.addProperty("boss_no_gravity",b.hasNoGravity());
            initial.addProperty("player_no_gravity",p.hasNoGravity());initial.addProperty("player_invulnerable",p.isInvulnerable());initial.addProperty("world_difficulty",world.getDifficulty().name());initial.addProperty("server_runtime_class",p.getServer().getClass().getName());
            initial.addProperty("player_hp",p.getHealth());initial.addProperty("player_max_hp",p.getMaxHealth());initial.addProperty("player_armor",p.getArmor());
            var attributes=new JsonObject();stats.stats.forEach((k,v)->attributes.addProperty(k.name(),v));initial.add("stats",attributes);
            var equipment=new JsonArray();for(var stack:p.getArmorItems())equipment.add(item(stack));
            equipment.add(item(p.getMainHandStack()));equipment.add(item(p.getOffHandStack()));initial.add("equipment",equipment);
            initial.add("unlocked_nodes",JSON.toJsonTree(new TreeSet<>(stats.unlockedNodes)));
            sample(true);
        }
        long elapsed(){return Math.max(0,world.getTime()-startedTick);}
        void sample(boolean force) {
            var stats=StatsManager.get(player);float hp=player.getHealth(),r=resource(player,stats),stamina=stats.stamina;
            recovery+=Math.max(0,hp-lastHp+pendingDamage);pendingDamage=0;lastHp=hp;
            resourceDepleted+=Math.max(0,lastResource-r);staminaDepleted+=Math.max(0,lastStamina-stamina);lastResource=r;lastStamina=stamina;
            if(!force && elapsed()-lastSample<20)return;lastSample=elapsed();
            if(samples.size()>=605)return;
            var sample=new JsonObject();sample.addProperty("tick",elapsed());sample.addProperty("player_hp",hp);sample.addProperty("boss_hp",boss.getHealth());
            sample.addProperty("resource",r);sample.addProperty("stamina",stamina);sample.addProperty("x",player.getX());sample.addProperty("y",player.getY());sample.addProperty("z",player.getZ());
            sample.addProperty("main_hand",Registries.ITEM.getId(player.getMainHandStack().getItem()).toString());sample.addProperty("off_hand",Registries.ITEM.getId(player.getOffHandStack().getItem()).toString());
            sample.addProperty("boss_distance",player.distanceTo(boss));var cooldowns=new JsonObject();var state=CombatState.get(player.getUuid());
            for(var node:stats.unlockedNodes)if(state.cooldown(node)>0)cooldowns.addProperty(node,state.cooldown(node));sample.add("rpg_cooldowns_ticks",cooldowns);samples.add(sample);
        }
        JsonObject report(String outcome) {
            var result=new JsonObject();double wall=(System.nanoTime()-startedNano)/1e9;
            result.addProperty("schema",1);result.addProperty("session",id.toString());result.add("initial",initial);
            result.addProperty("outcome",outcome);result.addProperty("elapsed_ticks",elapsed());result.addProperty("wall_seconds",wall);result.addProperty("started_boss_full_health",fullBoss);
            result.add("ttk_seconds",outcome.equals("BOSS_DEAD")&&fullBoss?new JsonPrimitive(wall):JsonNull.INSTANCE);
            result.addProperty("damage_dealt",dealt);result.addProperty("damage_received",received);result.addProperty("boss_attributed_damage_received",attributed);
            result.addProperty("other_damage_to_boss",otherDamage);result.addProperty("other_attackers",otherAttackers.size());result.addProperty("confirmed_player_hits",hits);
            result.addProperty("player_dps_wall",wall>0?dealt/wall:0);result.addProperty("net_health_recovered",recovery);
            result.addProperty("sampled_resource_depletion",resourceDepleted);result.addProperty("sampled_stamina_depletion",staminaDepleted);
            result.add("incoming_sources",JSON.toJsonTree(sources));result.add("samples",samples);result.add("manual_markers",marks);
            result.addProperty("forge_version",net.minecraftforge.versions.forge.ForgeVersion.getVersion());var mods=new JsonObject();ModList.get().getMods().forEach(m->mods.addProperty(m.getModId(),m.getVersion().toString()));result.add("mods",mods);
            result.add("final_main_hand",item(player.getMainHandStack()));result.add("final_off_hand",item(player.getOffHandStack()));
            result.addProperty("limitations","Build requires manual legality review; markers are annotations; native spell cooldowns/misses/uptime/summon ownership not inferred. Recovery has no heal-source attribution; sampled depletion is net per tick, not gross resource costs. Recorder correctness fixtures are not real encounter calibration.");
            return result;
        }
    }
    private static JsonObject item(net.minecraft.item.ItemStack stack) {
        var value=new JsonObject();value.addProperty("id",Registries.ITEM.getId(stack.getItem()).toString());value.addProperty("count",stack.getCount());
        value.add("nbt",stack.getNbt()==null?JsonNull.INSTANCE:new JsonPrimitive(stack.getNbt().toString()));return value;
    }
    private static float resource(ServerPlayerEntity p,PlayerStats s){return s.clazz==RPGClass.MAGO?CompatManager.mageMana(p,s):s.resource;}
    private ArenaRecorder(){}
}
