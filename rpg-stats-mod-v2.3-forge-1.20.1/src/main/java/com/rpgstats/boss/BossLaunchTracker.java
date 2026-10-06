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
    private static final Map<UUID,Event> HITS=new HashMap<>(), IMPULSES=new HashMap<>(), LAUNCHES=new HashMap<>(), NATIVE_HITS=new HashMap<>();
    private static final long MAX_FLIGHT_TICKS=200;
    private static Event event(ServerPlayerEntity p,LivingEntity boss) {
        if(boss instanceof net.minecraft.entity.player.PlayerEntity || p.getWorld()!=boss.getWorld() || BossScaler.getTier(boss)<=0 || safe(p))return null;
        return new Event(boss.getUuid(),p.getServerWorld(),p.getServerWorld().getTime());
    }
    /** Every damage invocation starts a new action; an older accepted hit cannot fund a rejected one. */
    public static void beginDamage(ServerPlayerEntity p){HITS.remove(p.getUuid());NATIVE_HITS.remove(p.getUuid());}
    public static void finishDamage(ServerPlayerEntity p,float actual) {
        if(!(actual>0)){HITS.remove(p.getUuid());IMPULSES.remove(p.getUuid());NATIVE_HITS.remove(p.getUuid());}
    }
    public static void record(ServerPlayerEntity p,LivingEntity boss) {
        Event e=event(p,boss);if(e==null)return;
        HITS.put(p.getUuid(),e);NATIVE_HITS.put(p.getUuid(),e);match(p,e,IMPULSES.get(p.getUuid()));
    }
    /** Pre-hurt writes must await their own damage; never pair with an older hit of the same boss. */
    public static void recordImpulseBeforeDamage(ServerPlayerEntity p,LivingEntity boss,double beforeY,double afterY) {
        if(!Double.isFinite(beforeY)||!Double.isFinite(afterY)||afterY-beforeY<.25||afterY<.25)return;
        HITS.remove(p.getUuid());recordImpulse(p,boss,beforeY,afterY);
    }
    /** Post-hurt native writes pair only with the most recent accepted damage invocation. */
    public static void recordImpulse(ServerPlayerEntity p,LivingEntity boss,double beforeY,double afterY) {
        if(!Double.isFinite(beforeY)||!Double.isFinite(afterY)||afterY-beforeY<.25||afterY<.25)return;
        // A newer upward force changes the cause of this flight, even if its hit is rejected.
        LAUNCHES.remove(p.getUuid());
        Event e=event(p,boss);if(e==null){remove(p.getUuid());return;}
        IMPULSES.put(p.getUuid(),e);match(p,e,HITS.get(p.getUuid()));
    }
    /** Only an audited native post-hurt call site may continue the same accepted action.
     * Vanilla knockback may already have consumed its ordinary pair. A new damage call
     * (including a ServerPlayer early rejection) invalidates this continuation token. */
    public static void recordNativePostDamageImpulse(ServerPlayerEntity p,LivingEntity boss,double beforeY,double afterY) {
        Event e=event(p,boss);
        if(e!=null && e.equals(NATIVE_HITS.get(p.getUuid())))HITS.put(p.getUuid(),e);
        recordImpulse(p,boss,beforeY,afterY);
    }
    private static void match(ServerPlayerEntity p,Event a,Event b) {
        if(a.equals(b)) {
            LAUNCHES.put(p.getUuid(),a);HITS.remove(p.getUuid());IMPULSES.remove(p.getUuid());
        }
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
            if(remove){HITS.remove(entry.getKey());IMPULSES.remove(entry.getKey());NATIVE_HITS.remove(entry.getKey());}
            return remove;
        });
        // Pairing is strictly same-tick; no history or unbounded accumulation.
        HITS.entrySet().removeIf(e->stalePair(server,e));
        IMPULSES.entrySet().removeIf(e->stalePair(server,e));
        NATIVE_HITS.entrySet().removeIf(e->stalePair(server,e));
    }
    private static boolean stalePair(MinecraftServer server,Map.Entry<UUID,Event> entry) {
        var p=server.getPlayerManager().getPlayer(entry.getKey());var e=entry.getValue();
        return p==null || p.getServerWorld()!=e.world || safe(p) || e.world.getTime()!=e.tick;
    }
    public static void remove(UUID player){LAUNCHES.remove(player);HITS.remove(player);IMPULSES.remove(player);NATIVE_HITS.remove(player);}
    public static void clear(){LAUNCHES.clear();HITS.clear();IMPULSES.clear();NATIVE_HITS.clear();}
    private BossLaunchTracker(){}
}
