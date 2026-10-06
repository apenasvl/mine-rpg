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

/** One landing from a confirmed upward boss hit; transient, never inferred from nearby mobs. */
public final class BossLaunchTracker {
    private record Launch(UUID boss,ServerWorld world,long started){}
    private static final Map<UUID,Launch> LAUNCHES=new HashMap<>();
    private static final long MAX_FLIGHT_TICKS=200;

    public static void record(ServerPlayerEntity player,LivingEntity boss) {
        if(player.getWorld()!=boss.getWorld() || BossScaler.getTier(boss)<=0
                || !player.isAlive() || player.getAbilities().flying || player.getVelocity().y<.25)return;
        LAUNCHES.put(player.getUuid(),new Launch(boss.getUuid(),player.getServerWorld(),player.getServer().getTicks()));
    }
    private static Launch active(ServerPlayerEntity player) {
        Launch launch=LAUNCHES.get(player.getUuid());
        if(launch!=null && (player.getServerWorld()!=launch.world || !player.isAlive()
                || player.getAbilities().flying || player.getServer().getTicks()-launch.started>MAX_FLIGHT_TICKS)) {
            LAUNCHES.remove(player.getUuid());return null;
        }
        return launch;
    }
    public static boolean isBossFall(ServerPlayerEntity player,DamageSource source) {
        return source.isOf(DamageTypes.FALL) && source.getAttacker()==null && active(player)!=null;
    }
    public static LivingEntity fallBoss(ServerPlayerEntity player,DamageSource source) {
        if(!isBossFall(player,source))return null;
        var launch=active(player);var entity=launch.world.getEntity(launch.boss);
        return entity instanceof LivingEntity boss?boss:null;
    }
    /** Consume after confirmed contribution accounting, even when vanilla rejected the landing damage. */
    public static void finishFall(ServerPlayerEntity player,DamageSource source) {
        if(source.isOf(DamageTypes.FALL))remove(player.getUuid());
    }
    public static void tick(MinecraftServer server) {
        long now=server.getTicks();
        LAUNCHES.entrySet().removeIf(entry->{
            var player=server.getPlayerManager().getPlayer(entry.getKey());var launch=entry.getValue();
            return player==null || !player.isAlive() || player.getServerWorld()!=launch.world
                    || player.getAbilities().flying || now-launch.started>MAX_FLIGHT_TICKS
                    || (now>launch.started+2 && player.isOnGround());
        });
    }
    public static void remove(UUID player){LAUNCHES.remove(player);}
    public static void clear(){LAUNCHES.clear();}
    private BossLaunchTracker(){}
}
