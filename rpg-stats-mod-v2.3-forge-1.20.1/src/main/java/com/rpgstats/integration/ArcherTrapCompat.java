package com.rpgstats.integration;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.combat.CombatState;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Optional, read-only adapter for Simply Bear Traps 1.0.0 (CF file 8595248).
 * Actual owner/capture fields were inspected in that binary; no guessed capture or block contact.
 * Reflection keeps external classes out of the mandatory classpath. Unknown versions fail closed.
 */
public final class ArcherTrapCompat {
    private static final String TYPE = "com.beartrap.beartrapmod.entity.BearTrapEntity";
    private record Access(Method owner, Method open, Field captured) {}
    private record Capture(UUID owner, boolean open, LivingEntity victim) {}
    private static final ClassValue<Optional<Access>> ACCESS = new ClassValue<>() {
        protected Optional<Access> computeValue(Class<?> type) {
            try {
                Field captured = type.getDeclaredField("trappedEntity");
                captured.setAccessible(true);
                return Optional.of(new Access(type.getMethod("getOwnerUuid"), type.getMethod("isOpen"), captured));
            } catch (ReflectiveOperationException | RuntimeException e) {
                RPGStatsMod.LOGGER.error("Unsupported Simply Bear Traps capture API: {}", type.getName());
                return Optional.empty();
            }
        }
    };
    private static final class Trap {
        final UUID id;
        boolean spent;
        Trap(Entity e) { id = e.getUuid(); }
    }
    private static final class Session {
        CombatState state;
        Object dimension;
        long until;
        boolean territory;
        List<Trap> traps = List.of();
        final Map<UUID, Long> marks = new HashMap<>();
        final Map<UUID, Long> rewarded = new HashMap<>();
    }
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static Capture read(Entity trap) {
        if (!TYPE.equals(trap.getClass().getName())) return null;
        Optional<Access> access = ACCESS.get(trap.getClass());
        if (access.isEmpty()) return null;
        try {
            Access a = access.get();
            return new Capture((UUID)a.owner.invoke(trap), (Boolean)a.open.invoke(trap), (LivingEntity)a.captured.get(trap));
        } catch (ReflectiveOperationException | RuntimeException e) { return null; }
    }
    private static List<Entity> available(ServerPlayerEntity player) {
        List<Entity> traps = new ArrayList<>(player.getServerWorld().getOtherEntities(player, player.getBoundingBox().expand(8), e -> {
            Capture c = read(e);
            return e.isAlive() && player.squaredDistanceTo(e) <= 64 && c != null && c.open && c.victim == null && player.getUuid().equals(c.owner);
        }));
        traps.sort(Comparator.comparingDouble(player::squaredDistanceTo));
        return traps;
    }
    private static Session session(ServerPlayerEntity p) {
        Session s = SESSIONS.get(p.getUuid());
        if (s == null || s.state != CombatState.get(p.getUuid()) || !Objects.equals(s.dimension, p.getWorld().getRegistryKey())) {
            s = new Session(); s.state = CombatState.get(p.getUuid()); s.dimension = p.getWorld().getRegistryKey();
            SESSIONS.put(p.getUuid(), s);
        }
        return s;
    }
    public static boolean canPrepare(ServerPlayerEntity player) { return !available(player).isEmpty(); }
    public static void prepare(ServerPlayerEntity player, int ticks, boolean territory) {
        Session s = session(player);
        s.traps = available(player).stream().limit(2).map(Trap::new).toList();
        s.until = player.getWorld().getTime() + ticks;
        s.territory = territory;
    }
    public static void tick(ServerPlayerEntity player, PlayerStats stats) {
        if (stats.clazz != RPGClass.ARQUEIRO || stats.specialization != RPGSpecialization.TRAPPER || !player.isAlive()) { remove(player.getUuid()); return; }
        Session s = SESSIONS.get(player.getUuid());
        if (s == null) return;
        if (s.state != CombatState.get(player.getUuid()) || !Objects.equals(s.dimension, player.getWorld().getRegistryKey())) { remove(player.getUuid()); return; }
        long now = player.getWorld().getTime();
        s.marks.entrySet().removeIf(e -> e.getValue() <= now);
        s.rewarded.entrySet().removeIf(e -> e.getValue() <= now);
        if (now >= s.until) { s.traps = List.of(); return; }
        for (Trap prepared : s.traps) {
            if (prepared.spent) continue;
            Entity trap = player.getServerWorld().getEntity(prepared.id);
            if (trap == null || !trap.isAlive() || player.squaredDistanceTo(trap) > 1024) continue;
            Capture capture = read(trap);
            if (capture == null || !player.getUuid().equals(capture.owner) || capture.victim == null) continue;
            prepared.spent = true;
            LivingEntity victim = capture.victim;
            // Physical PvP/animal capture remains native; RPG prey rewards are hostile-only.
            if (!(victim instanceof HostileEntity) || !victim.isAlive() || player.isTeammate(victim) || s.rewarded.containsKey(victim.getUuid())) continue;
            s.rewarded.put(victim.getUuid(), now + 200);
            s.marks.put(victim.getUuid(), now + 120);
            s.state.classTarget(victim.getUuid()).markTicks = 120;
            if (s.territory) s.state.startTimer("arc_reposition_guard", 40);
        }
    }
    public static boolean isMarked(ServerPlayerEntity player, LivingEntity target) {
        Session s = SESSIONS.get(player.getUuid());
        return s != null && s.state == CombatState.get(player.getUuid()) && Objects.equals(s.dimension, player.getWorld().getRegistryKey())
                && s.marks.getOrDefault(target.getUuid(), 0L) > player.getWorld().getTime();
    }
    /** Arming a future shot does not target through walls; its later positive hit is still required. */
    public static boolean hasMarkedPrey(ServerPlayerEntity player, double range) {
        Session s = SESSIONS.get(player.getUuid());
        if (s == null) return false;
        for (UUID id : s.marks.keySet()) {
            Entity raw = player.getServerWorld().getEntity(id);
            if (raw instanceof HostileEntity prey && prey.isAlive() && !player.isTeammate(prey)
                    && player.squaredDistanceTo(prey) <= range * range && isMarked(player, prey)) return true;
        }
        return false;
    }
    public static boolean consumeMark(ServerPlayerEntity player, LivingEntity target) {
        if (!isMarked(player, target)) return false;
        Session s = SESSIONS.get(player.getUuid());
        s.marks.remove(target.getUuid());
        s.state.classTarget(target.getUuid()).markTicks = 0;
        return true;
    }
    public static void remove(UUID owner) { SESSIONS.remove(owner); }
    public static void clear() { SESSIONS.clear(); }
    private ArcherTrapCompat() {}
}
