package com.rpgstats.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Delays same-target proc damage until vanilla's hurt-resistance window can accept a full new hit.
 *
 * A nested target.damage(...) issued while the original LivingEntity.damage call is resolving is
 * commonly discarded by vanilla if the proc is smaller than the first hit. Extra/AoE targets do not
 * have that problem and can still be damaged immediately by their owning handlers.
 */
public final class ProcDamageQueue {
    private static final int SAME_TARGET_DELAY_TICKS = 11;
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);
    private static final List<Pending> PENDING = new ArrayList<>();

    private static final class Pending {
        final UUID attacker;
        final UUID target;
        final RegistryKey<World> dimension;
        final float amount;
        int ticks;

        Pending(ServerPlayerEntity attacker, LivingEntity target, float amount) {
            this.attacker = attacker.getUuid();
            this.target = target.getUuid();
            this.dimension = target.getWorld().getRegistryKey();
            this.amount = amount;
            this.ticks = SAME_TARGET_DELAY_TICKS;
        }
    }

    public static void queueSameTarget(ServerPlayerEntity attacker, LivingEntity target, float amount) {
        if (amount <= 0.01f || !target.isAlive() || target == attacker) return;
        PENDING.add(new Pending(attacker, target, amount));
    }

    public static void tick(MinecraftServer server) {
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pending = it.next();
            if (--pending.ticks > 0) continue;
            it.remove();

            ServerPlayerEntity attacker = server.getPlayerManager().getPlayer(pending.attacker);
            ServerWorld world = server.getWorld(pending.dimension);
            if (attacker == null || world == null || attacker.getServerWorld() != world) continue;
            Entity rawTarget = world.getEntity(pending.target);
            if (!(rawTarget instanceof LivingEntity target) || !target.isAlive()) continue;

            boolean previous = APPLYING.get();
            APPLYING.set(true);
            try {
                target.damage(attacker.getDamageSources().indirectMagic(attacker, attacker), pending.amount);
            } finally {
                APPLYING.set(previous);
            }
        }
    }

    /** Extra victims use the same central guard as deferred hits: no RPG scaling or resource loop. */
    public static void damageExtra(ServerPlayerEntity attacker, LivingEntity target, float amount) {
        if (amount <= .01f || !target.isAlive() || target == attacker) return;
        boolean previous = APPLYING.get();
        APPLYING.set(true);
        try {
            target.damage(attacker.getDamageSources().indirectMagic(attacker, attacker), amount);
        } finally { APPLYING.set(previous); }
    }

    public static boolean isApplying() {
        return APPLYING.get();
    }

    public static void clear() {
        PENDING.clear();
        APPLYING.remove();
    }

    private ProcDamageQueue() {}
}
