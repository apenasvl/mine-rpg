package com.rpgstats.boss;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A landing requires both accepted health loss and a measured boss impulse in the same world tick. */
public final class BossLaunchTracker {
    private record Event(UUID boss,ServerWorld world,long tick){}
    private static final Map<UUID,Event> HITS=new HashMap<>(), IMPULSES=new HashMap<>(), LAUNCHES=new HashMap<>();
    private static final long MAX_FLIGHT_TICKS=200;
    private static Event event(ServerPlayerEntity p,LivingEntity boss) {
        if(boss instanceof net.minecraft.entity.player.PlayerEntity || p.getWorld()!=boss.getWorld() || BossScaler.getTier(boss)<=0 || safe(p))return null;
        return new Event(boss.getUuid(),p.getServerWorld(),p.getServerWorld().getTime());
    }
    public static void record(ServerPlayerEntity p,LivingEntity boss) {
        Event e=event(p,boss);if(e==null)return;
        HITS.put(p.getUuid(),e);match(p,e,IMPULSES.get(p.getUuid()));
    }
    /** Called only at boss-owned native velocity writes or knockback inside that boss's damage call. */
    public static void recordImpulse(ServerPlayerEntity p,LivingEntity boss,double beforeY,double afterY) {
        if(!Double.isFinite(beforeY)||!Double.isFinite(afterY)||afterY-beforeY<.25||afterY<.25)return;
        // A newer upward force changes the cause of this flight, even if its hit is rejected.
        LAUNCHES.remove(p.getUuid());
        Event e=event(p,boss);if(e==null){remove(p.getUuid());return;}
        IMPULSES.put(p.getUuid(),e);match(p,e,HITS.get(p.getUuid()));
    }
    private static void match(ServerPlayerEntity p,Event a,Event b) {
        if(a.equals(b))LAUNCHES.put(p.getUuid(),a);
    }
    private static boolean safe(ServerPlayerEntity p) {
        return !p.isAlive() || p.getAbilities().flying || p.isFallFlying() || p.isTouchingWater()
                || p.isInLava() || p.isClimbing() || p.hasVehicle();
    }
    private static Event active(ServerPlayerEntity p) {
        Event e=LAUNCHES.get(p.getUuid());
        if(e!=null && (p.getServerWorld()!=e.world || safe(p) || p.getServerWorld().getTime()-e.tick>MAX_FLIGHT_TICKS)) {
            remove(p.getUuid());return null;
        }
        return e;
    }
    public static boolean isBossFall(ServerPlayerEntity p,DamageSource source) {
        return source.isOf(DamageTypes.FALL) && source.getAttacker()==null && active(p)!=null;
    }
    public static LivingEntity fallBoss(ServerPlayerEntity p,DamageSource source) {
        if(!isBossFall(p,source))return null;
        var e=active(p);var entity=e.world.getEntity(e.boss);
        return entity instanceof LivingEntity boss?boss:null;
    }
    public static void finishFall(ServerPlayerEntity p,DamageSource source) {
        if(source.isOf(DamageTypes.FALL))remove(p.getUuid());
    }
    public static void tick(MinecraftServer server) {
        LAUNCHES.entrySet().removeIf(entry->{
            var p=server.getPlayerManager().getPlayer(entry.getKey());var e=entry.getValue();
            boolean remove=p==null || p.getServerWorld()!=e.world || safe(p) || e.world.getTime()-e.tick>MAX_FLIGHT_TICKS
                    || (e.world.getTime()>e.tick+2 && p.isOnGround());
            if(remove){HITS.remove(entry.getKey());IMPULSES.remove(entry.getKey());}
            return remove;
        });
        // Pairing is strictly same-tick; no history or unbounded accumulation.
        HITS.entrySet().removeIf(e->stalePair(server,e));
        IMPULSES.entrySet().removeIf(e->stalePair(server,e));
    }
    private static boolean stalePair(MinecraftServer server,Map.Entry<UUID,Event> entry) {
        var p=server.getPlayerManager().getPlayer(entry.getKey());var e=entry.getValue();
        return p==null || p.getServerWorld()!=e.world || safe(p) || e.world.getTime()!=e.tick;
    }
    public static void remove(UUID player){LAUNCHES.remove(player);HITS.remove(player);IMPULSES.remove(player);}
    public static void clear(){LAUNCHES.clear();HITS.clear();IMPULSES.clear();}
    private BossLaunchTracker(){}
}
