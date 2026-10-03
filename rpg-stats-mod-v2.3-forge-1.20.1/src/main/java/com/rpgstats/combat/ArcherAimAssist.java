package com.rpgstats.combat;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.compat.TargetDummyCompat;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Passive Marksman correction of one existing fully drawn bow projectile; never creates ammo or damage. */
public final class ArcherAimAssist {
    private static final TagKey<net.minecraft.item.Item> AREA_WEAPONS=TagKey.of(RegistryKeys.ITEM,new Identifier("rpgstats","weapons/native_area"));
    private static final TagKey<net.minecraft.entity.EntityType<?>> AREA_PROJECTILES=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("rpgstats","native_area_projectiles"));
    private static final Map<UUID,Guide> GUIDES=new HashMap<>();
    private static final Map<UUID,Long> COOLDOWNS=new HashMap<>();
    private static final class Guide {
        final UUID owner;
        final PersistentProjectileEntity arrow;
        final LivingEntity target;
        final ServerWorld world;
        final Vec3d original;
        final long launched;
        long lastTick;
        int corrections;
        Guide(ServerPlayerEntity p,PersistentProjectileEntity a,LivingEntity t,long now) {
            owner=p.getUuid(); arrow=a; target=t; world=p.getServerWorld(); original=a.getVelocity().normalize();
            launched=now; lastTick=now-1;
        }
    }
    private static boolean enabled(PlayerStats stats) {
        return stats.awakened && stats.clazz==RPGClass.ARQUEIRO && stats.path==RPGPath.ARC_MARKSMAN
                && stats.unlockedNodes.contains("arc_mark_foundation");
    }
    private static boolean hostile(ServerPlayerEntity p,LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && !p.isTeammate(target)
                && (target instanceof HostileEntity || TargetDummyCompat.isTrainingTarget(target));
    }
    private static Vec3d aimPoint(LivingEntity target) { return target.getBoundingBox().getCenter(); }
    private static boolean clearLine(ServerWorld world,Vec3d from,Vec3d to,net.minecraft.entity.Entity source) {
        return world.raycast(new RaycastContext(from,to,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,source)).getType()==HitResult.Type.MISS;
    }
    private static double[] components(Vec3d v) { return new double[]{v.x,v.y,v.z}; }
    private static boolean cone(Vec3d a,Vec3d b) { return ArcherAimPolicy.inCone(components(a),components(b)); }

    /** Call only after ArcherShotTracker has recorded the actual launch weapon. Crossbows are deliberately excluded. */
    public static void onLaunch(ServerPlayerEntity player,PersistentProjectileEntity arrow,ItemStack weapon) {
        if (arrow.getOwner()!=player || arrow.getWorld()!=player.getWorld() || !arrow.isCritical()
                || !ArcherShotTracker.isRangedWeapon(weapon) || ArcherShotTracker.isCrossbow(weapon)
                || weapon.isIn(AREA_WEAPONS) || arrow.getType().isIn(AREA_PROJECTILES)
                || arrow.getVelocity().lengthSquared()<1e-9 || arrow.isRemoved() || !player.isAlive()) return;
        PlayerStats stats=StatsManager.get(player);
        long now=player.getServer().getTicks();
        if (!enabled(stats) || stats.resource<6 || COOLDOWNS.getOrDefault(player.getUuid(),Long.MIN_VALUE)>now) return;
        Vec3d eye=player.getEyePos(), look=Vec3d.fromPolar(player.getPitch(),player.getYaw()), original=arrow.getVelocity();
        LivingEntity lock=null; double best=Double.NEGATIVE_INFINITY;
        for (LivingEntity target:player.getServerWorld().getEntitiesByClass(LivingEntity.class,player.getBoundingBox().expand(28),e->hostile(player,e))) {
            Vec3d point=aimPoint(target), eyeDelta=point.subtract(eye), arrowDelta=point.subtract(arrow.getPos());
            double distance=eyeDelta.length();
            if (distance<8 || distance>28 || !cone(look,eyeDelta) || !cone(original,arrowDelta)
                    || !player.canSee(target) || !clearLine(player.getServerWorld(),arrow.getPos(),point,arrow)) continue;
            double alignment=eyeDelta.normalize().dotProduct(look);
            if (alignment>best) { best=alignment; lock=target; }
        }
        if (lock==null) return;
        GUIDES.put(arrow.getUuid(),new Guide(player,arrow,lock,now));
        COOLDOWNS.put(player.getUuid(),now+40);
        stats.resource-=6; StatsManager.save(player,stats);
    }
    public static void tick(MinecraftServer server) {
        long now=server.getTicks();
        COOLDOWNS.entrySet().removeIf(e->e.getValue()<=now);
        Iterator<Guide> entries=GUIDES.values().iterator();
        while(entries.hasNext()) {
            Guide g=entries.next(); ServerPlayerEntity p=server.getPlayerManager().getPlayer(g.owner);
            if (p==null || !p.isAlive() || p.getServerWorld()!=g.world || !enabled(StatsManager.get(p))
                    || g.arrow.isRemoved() || !g.arrow.isAlive() || g.arrow.getOwner()!=p
                    || g.arrow.getWorld()!=g.world || g.target.getWorld()!=g.world || !hostile(p,g.target)
                    || now-g.launched>=8 || g.corrections>=8 || g.arrow.getVelocity().lengthSquared()<1e-9) { entries.remove(); continue; }
            if(g.lastTick==now) continue;
            Vec3d point=aimPoint(g.target), desired=point.subtract(g.arrow.getPos());
            if (!p.canSee(g.target) || !clearLine(g.world,g.arrow.getPos(),point,g.arrow)) { entries.remove(); continue; }
            double[] next=ArcherAimPolicy.turn(components(g.arrow.getVelocity()),components(desired));
            Vec3d corrected=new Vec3d(next[0],next[1],next[2]);
            // Gravity remains vanilla. Stop correcting when its evolving trajectory leaves the original cone.
            if (!cone(g.original,corrected)) { entries.remove(); continue; }
            g.arrow.setVelocity(corrected); g.arrow.velocityModified=true;
            g.corrections++; g.lastTick=now;
        }
    }
    public static void clear() { GUIDES.clear(); COOLDOWNS.clear(); }
    private ArcherAimAssist() {}
}
