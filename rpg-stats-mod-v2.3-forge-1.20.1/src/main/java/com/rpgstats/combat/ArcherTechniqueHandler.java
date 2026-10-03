package com.rpgstats.combat;

import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.integration.ArcherTrapCompat;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Bounded, confirmed-shot techniques. No replacement projectiles or weapon mutation. */
public final class ArcherTechniqueHandler {
    private static final ThreadLocal<Float> SUPPLEMENT = ThreadLocal.withInitial(() -> 0f);
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final class Session {
        CombatState combat;
        RPGSpecialization spec;
        Object dimension;
        Vec3d origin, last;
        UUID priority, previous;
        int technique, signature, ascension, shots, precisionHits, pulses, airborneHits;
        long pulseAt, anchorUntil;
        float payload;
        boolean emergency, wasAirborne;
        final Map<UUID, Long> marks = new HashMap<>();
    }
    private static Session session(ServerPlayerEntity p, PlayerStats stats) {
        CombatState combat = CombatState.get(p.getUuid());
        Session s = SESSIONS.get(p.getUuid());
        if (s == null || s.combat != combat || s.spec != stats.specialization
                || !Objects.equals(s.dimension, p.getWorld().getRegistryKey())) {
            s = new Session(); s.combat = combat; s.spec = stats.specialization;
            s.dimension = p.getWorld().getRegistryKey(); s.last = p.getPos();
            SESSIONS.put(p.getUuid(), s);
        }
        return s;
    }
    private static int kind(String id) { return id.endsWith("_ascension") ? 2 : id.endsWith("_signature") ? 1 : 0; }
    private static boolean crossbow(ServerPlayerEntity p) {
        return ArcherShotTracker.isCrossbow(p.getMainHandStack()) || ArcherShotTracker.isCrossbow(p.getOffHandStack());
    }
    private static boolean hostile(ServerPlayerEntity p, LivingEntity e) {
        return (e instanceof HostileEntity || com.rpgstats.compat.TargetDummyCompat.isTrainingTarget(e)) && e.isAlive() && !p.isTeammate(e)
                && p.squaredDistanceTo(e) <= 24 * 24 && p.canSee(e);
    }
    private static List<LivingEntity> nearby(ServerPlayerEntity p, Vec3d center, double radius) {
        List<LivingEntity> list = p.getServerWorld().getEntitiesByClass(LivingEntity.class,
                new net.minecraft.util.math.Box(center, center).expand(radius), e -> hostile(p, e) && e.getPos().squaredDistanceTo(center) <= radius * radius);
        list.sort(Comparator.comparingDouble(e -> e.getPos().squaredDistanceTo(center)));
        return list;
    }
    private static LivingEntity aimed(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1f);
        return nearby(p, p.getPos(), 24).stream().filter(e -> e.getPos().subtract(p.getPos()).normalize().dotProduct(look) > .35)
                .max(Comparator.comparingDouble(e -> e.getPos().subtract(p.getPos()).normalize().dotProduct(look))).orElse(null);
    }
    private static boolean engine(PlayerStats stats) {
        return stats.specialization != null && stats.specialization.nodes.stream().anyMatch(n -> n.id().endsWith("_engine") && stats.unlockedNodes.contains(n.id()));
    }
    private static List<TameableEntity> pets(ServerPlayerEntity p) {
        return p.getServerWorld().getEntitiesByClass(TameableEntity.class, p.getBoundingBox().expand(16),
                e -> e.isAlive() && p.getUuid().equals(e.getOwnerUuid()));
    }
    public static boolean canActivate(ServerPlayerEntity p, PlayerStats stats, String id) {
        if (stats.clazz != RPGClass.ARQUEIRO || stats.specialization == null) return true;
        RPGSpecialization spec = RPGSpecialization.ownerOfNode(id);
        if (spec != stats.specialization) return true;
        int k = kind(id); Session s = session(p, stats); CombatState c = s.combat;
        return switch (spec) {
            case SNIPER -> k != 1 || aimed(p) != null;
            case DEADEYE -> k != 1 || c.gauge("arc_precision") >= 2;
            case BEASTMASTER -> k == 0 ? aimed(p) != null : !pets(p).isEmpty() && (k != 1 || aimed(p) != null);
            case TRAPPER -> k == 1 ? ArcherTrapCompat.hasMarkedPrey(p, 24) : ArcherTrapCompat.canPrepare(p);
            case SURVIVALIST -> k != 1 || (c.gauge("arc_instinct") >= 1 && p.getHealth() < p.getMaxHealth());
            case WINDRUNNER -> k != 1 || c.gauge("arc_momentum") >= 2;
            case CROSSBOW_EXPERT -> crossbow(p) && (k != 1 || c.gauge("arc_device") >= 2);
            case ENGINEER -> p.isOnGround() && (k != 1 || s.origin != null && s.anchorUntil > p.getWorld().getTime() && p.getPos().squaredDistanceTo(s.origin) <= 100);
            case FLAMEBOW, FROSTBOW, STORMBOW -> k != 1 || nearby(p, p.getPos(), 24).stream().anyMatch(e -> marked(s, p, e));
            default -> true;
        };
    }
    public static void activate(ServerPlayerEntity p, PlayerStats stats, RPGSpecialization spec, String id, float scale) {
        activate(p, stats, spec, id, scale, false);
    }
    public static void activate(ServerPlayerEntity p, PlayerStats stats, RPGSpecialization spec, String id, float scale, boolean externalMovement) {
        Session s = session(p, stats); CombatState c = s.combat; int k = kind(id);
        float strength = Math.max(.45f, Math.min(1f, scale));
        if (k == 0) s.technique = 100;
        else if (k == 1) s.signature = 120;
        else { s.ascension = 180; s.shots = 3; }
        switch (spec) {
            case SNIPER -> {
                if (k == 0) { s.origin = p.getPos(); s.precisionHits = 0; }
                else if (k == 1) { LivingEntity target = aimed(p); if (target != null) { s.priority = target.getUuid(); c.classTarget(s.priority).markTicks = 120; } }
                else s.shots = 2;
            }
            case DEADEYE -> {
                if (k == 0) { s.previous = null; s.precisionHits = 0; }
                else if (k == 1) s.payload = spend(c, "arc_precision", 4) * strength;
                else { s.priority = s.previous; s.shots = 2; }
            }
            case BALLISTICIAN -> { if (k == 1) { s.origin = p.getPos(); s.last = p.getRotationVec(1f); } }
            case BEASTMASTER -> {
                LivingEntity target = aimed(p);
                if (k == 0 && target != null) { s.priority = target.getUuid(); c.classTarget(s.priority).markTicks = 140; }
                else for (TameableEntity pet : pets(p)) {
                    if (k == 1) { pet.setTarget(target); pet.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, engine(stats) ? 100 : 80, 0)); }
                    else pet.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, engine(stats) ? 150 : 120, 0));
                }
                if (k == 2) p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 0));
            }
            case TRAPPER -> { if (k != 1) { s.origin = p.getPos(); ArcherTrapCompat.prepare(p, Math.round((k == 2 ? 180 : 100) * (engine(stats) ? 1.25f : 1f)), k == 2); s.shots = 2; } }
            case SURVIVALIST -> {
                if (k == 0) { if (!externalMovement) dash(p, -.65 * strength, .12); c.startTimer("arc_survival", 50); }
                else if (k == 1) p.heal(Math.min(4, spend(c, "arc_instinct", 4)) * strength);
                else { s.emergency = true; s.shots = 1; }
            }
            case WINDRUNNER -> {
                if (k == 0) { dash(p, .8 * strength, .1); c.addGauge("arc_momentum", .8f, 10); }
                else if (k == 1) { s.payload = spend(c, "arc_momentum", 5) * strength; dash(p, .5, .08); }
                else { s.origin = p.getPos(); }
            }
            case ACROBAT -> {
                if (k == 0) { p.addVelocity(0, .48 * strength, 0); p.velocityModified = true; }
                else if (k == 1) s.wasAirborne = !p.isOnGround();
                else { s.airborneHits = 0; s.wasAirborne = false; }
            }
            case GUERRILLA -> { s.origin = p.getPos(); if (k == 0 && !externalMovement) dash(p, -.65, .08); if (k == 2) s.shots = 2; }
            case CROSSBOW_EXPERT -> { if (k == 1) s.payload = spend(c, "arc_device", 4) * strength; }
            case BOMBARDIER -> { if (k == 1) { LivingEntity target = aimed(p); s.origin = target == null ? p.getPos() : target.getPos(); } }
            case ENGINEER -> {
                if (k == 0 || k == 2) { s.origin = p.getPos(); s.anchorUntil = p.getWorld().getTime() + 180; s.pulses = k == 2 ? 3 : 0; s.pulseAt = p.getWorld().getTime() + 20; }
                else pulse(p, s.origin, true);
            }
            default -> { }
        }
    }
    private static float spend(CombatState c, String key, float maximum) {
        float n = Math.min(maximum, c.gauge(key)); c.setGauge(key, c.gauge(key) - n, 10); return n;
    }
    private static void dash(ServerPlayerEntity p, double power, double lift) {
        Vec3d direction = p.getRotationVec(1f); Vec3d horizontal = new Vec3d(direction.x, 0, direction.z).normalize();
        p.addVelocity(horizontal.x * power, lift, horizontal.z * power); p.velocityModified = true;
    }
    private static boolean marked(Session s, ServerPlayerEntity p, LivingEntity target) { return s.marks.getOrDefault(target.getUuid(), 0L) > p.getWorld().getTime(); }
    private static void mark(Session s, ServerPlayerEntity p, LivingEntity target) {
        if (s.marks.size() >= 16 && !s.marks.containsKey(target.getUuid())) s.marks.remove(s.marks.keySet().iterator().next());
        s.marks.put(target.getUuid(), p.getWorld().getTime() + 120);
    }
    private static void extra(ServerPlayerEntity p, LivingEntity target, float damage) { SUPPLEMENT.set(Math.min(4, SUPPLEMENT.get() + Math.max(0, damage))); }
    private static int controlTicks(LivingEntity e, int ticks) { return com.rpgstats.boss.BossScaler.getTier(e) > 0 ? Math.min(22, ticks) : ticks; }
    private static void slow(LivingEntity e, int ticks) { e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, controlTicks(e, ticks), 0)); }
    private static void pulse(ServerPlayerEntity p, Vec3d anchor, boolean weakness) {
        if (anchor == null) return;
        int count = 0; for (LivingEntity e : nearby(p, anchor, 4)) {
            slow(e, 40); if (weakness) e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, controlTicks(e, 40), 0));
            if (++count >= 3) break;
        }
    }
    private static void elemental(ServerPlayerEntity p, LivingEntity target, RPGSpecialization spec, float amount) {
        extra(p, target, amount);
        int count = 0;
        for (LivingEntity e : nearby(p, target.getPos(), 4)) {
            if (e == target) continue;
            if (spec == RPGSpecialization.FLAMEBOW) e.setOnFireFor(2);
            else if (spec == RPGSpecialization.FROSTBOW) slow(e, 50);
            else ProcDamageQueue.damageExtra(p, e, Math.min(3, amount * .6f));
            if (++count >= 2) break;
        }
        if (spec == RPGSpecialization.FROSTBOW) slow(target, 60);
    }
    private static void pierce(ServerPlayerEntity p, LivingEntity target, float damage) {
        Vec3d direction = target.getPos().subtract(p.getPos()).normalize();
        for (LivingEntity e : nearby(p, target.getPos(), 6)) {
            if (e == target) continue;
            Vec3d offset = e.getPos().subtract(target.getPos()); double ahead = offset.dotProduct(direction);
            if (ahead > .4 && offset.subtract(direction.multiply(ahead)).lengthSquared() < 2.25) {
                ProcDamageQueue.damageExtra(p, e, Math.min(4, damage * .25f)); break;
            }
        }
    }
    public static void onHit(ServerPlayerEntity p, LivingEntity target, PlayerStats stats, float damage, DamageSource source) {
        if (stats.clazz != RPGClass.ARQUEIRO || stats.specialization == null || !(source.getSource() instanceof ProjectileEntity) || damage <= 0) return;
        Session s = session(p, stats); CombatState c = s.combat; SUPPLEMENT.set(0f);
        boolean tech = s.technique > 0, sig = s.signature > 0, asc = s.ascension > 0 && s.shots > 0;
        boolean moved = s.origin != null && p.getPos().squaredDistanceTo(s.origin) >= 4;
        switch (stats.specialization) {
            case SNIPER -> {
                if (tech && s.technique <= 80 && s.origin != null && p.getPos().squaredDistanceTo(s.origin) < .09 && p.squaredDistanceTo(target) >= 81) { extra(p, target, damage * .15f); s.technique = 0; }
                if (sig && target.getUuid().equals(s.priority)) { extra(p, target, damage * .2f); s.signature = 0; }
                if (asc && c.gauge("arc_stationary") >= 2 && p.squaredDistanceTo(target) >= 81) { extra(p, target, damage * .2f); s.shots--; }
            }
            case DEADEYE -> {
                if (tech) { s.precisionHits = target.getUuid().equals(s.previous) ? s.precisionHits + 1 : 1; if (s.precisionHits >= 2) { c.addGauge("arc_precision", 1, 6); s.technique = 0; } }
                if (sig) { extra(p, target, s.payload); s.signature = 0; }
                if (asc && s.priority != null && !s.priority.equals(target.getUuid())) { c.addGauge("arc_precision", 1, 6); s.priority = target.getUuid(); s.shots--; }
                s.previous = target.getUuid(); if (asc && s.priority == null) s.priority = target.getUuid();
            }
            case BALLISTICIAN -> { if (tech || asc) { pierce(p, target, damage); if (tech) s.technique = 0; else s.shots--; } }
            case BEASTMASTER -> { if (tech && target.getUuid().equals(s.priority)) { slow(target, 40); s.technique = 0; } }
            case TRAPPER -> {
                if ((sig || asc && moved) && ArcherTrapCompat.isMarked(p, target) && ArcherTrapCompat.consumeMark(p, target)) {
                    extra(p, target, 4); if (sig) s.signature = 0; else s.shots--;
                }
            }
            case WINDRUNNER -> {
                if (sig && p.getVelocity().horizontalLengthSquared() > .01) { extra(p, target, s.payload * .7f); s.signature = 0; }
                if (asc && moved) { c.addGauge("arc_momentum", 1, 10); s.origin = p.getPos(); s.shots--; }
            }
            case ACROBAT -> { if (asc && !p.isOnGround()) { s.airborneHits++; s.shots--; } }
            case GUERRILLA -> {
                if ((sig || asc) && moved) { extra(p, target, damage * .2f); if (sig) s.signature = 0; else { s.shots--; s.origin = p.getPos(); } }
                if (tech) { c.startTimer("arc_reposition_guard", 30); s.technique = 0; }
            }
            case FLAMEBOW, FROSTBOW, STORMBOW -> {
                if (sig && marked(s, p, target)) { s.marks.remove(target.getUuid()); elemental(p, target, stats.specialization, 4); s.signature = 0; }
                else if (asc && marked(s, p, target)) { s.marks.remove(target.getUuid()); elemental(p, target, stats.specialization, Math.min(4, damage * .3f)); s.shots--; }
                else if (tech || asc) { mark(s, p, target); if (stats.specialization == RPGSpecialization.FLAMEBOW) target.setOnFireFor(2); if (stats.specialization == RPGSpecialization.FROSTBOW) slow(target, 40); if (tech) s.technique = 0; }
            }
            case CROSSBOW_EXPERT -> {
                if (!ArcherShotTracker.isCrossbow(ArcherShotTracker.launchWeapon(source, p))) break;
                if (sig) { pierce(p, target, damage); extra(p, target, Math.min(3, s.payload * .5f)); s.signature = 0; }
                if (tech || asc) { c.addGauge("arc_device", engine(stats) ? 1f : .8f, 5); extra(p, target, damage * .15f); if (tech) s.technique = 0; else s.shots--; }
            }
            case BOMBARDIER -> {
                if (tech || asc) { int count = 0; if (!ArcherShotTracker.hasNativeAreaEffect(source)) for (LivingEntity e : nearby(p, target.getPos(), engine(stats) ? 4.25 : 3.5)) { if (e == target) continue; ProcDamageQueue.damageExtra(p, e, Math.min(3, damage * .25f)); if (++count >= 2) break; } if (tech) s.technique = 0; else s.shots--; }
            }
            case ENGINEER -> { if (s.origin != null && s.anchorUntil > p.getWorld().getTime() && target.getPos().squaredDistanceTo(s.origin) <= 16) { c.addGauge("arc_device", engine(stats) ? .5f : .4f, 5); if (tech) { slow(target, 50); s.technique = 0; } } }
            default -> { }
        }
        float supplement = SUPPLEMENT.get(); SUPPLEMENT.remove();
        if (supplement > 0) ProcDamageQueue.queueSameTarget(p, target, supplement);
    }
    public static void tick(ServerPlayerEntity p, PlayerStats stats) {
        if (stats.clazz != RPGClass.ARQUEIRO || stats.specialization == null || !p.isAlive()) { SESSIONS.remove(p.getUuid()); return; }
        Session s = session(p, stats); long now = p.getWorld().getTime();
        if (stats.specialization == RPGSpecialization.SNIPER && s.technique > 0 && s.origin != null && p.getPos().squaredDistanceTo(s.origin) >= .09) s.technique = 0;
        if (s.technique > 0) s.technique--; if (s.signature > 0) s.signature--; if (s.ascension > 0) s.ascension--;
        s.marks.entrySet().removeIf(e -> e.getValue() <= now);
        if (stats.specialization == RPGSpecialization.SURVIVALIST && s.ascension > 0 && s.emergency && p.getHealth() <= p.getMaxHealth() * .35f) {
            s.emergency = false; p.heal(3); p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0));
        }
        if (stats.specialization == RPGSpecialization.ACROBAT) {
            if (!p.isOnGround()) s.wasAirborne = true;
            else if (s.wasAirborne) {
                if (s.signature > 0 || (s.ascension > 0 && s.airborneHits > 0)) { s.combat.startTimer("arc_reposition_guard", 45); if (s.airborneHits > 0) s.combat.addGauge("arc_focus", Math.min(2, s.airborneHits * .5f), 10); }
                s.signature = 0; s.airborneHits = 0; s.wasAirborne = false;
            }
        }
        if (stats.specialization == RPGSpecialization.BALLISTICIAN && s.signature > 0 && s.origin != null && now % 20 == 0) {
            int count = 0; for (LivingEntity e : nearby(p, s.origin, 16)) { Vec3d offset = e.getPos().subtract(s.origin); double forward = offset.dotProduct(s.last); if (forward > 0 && offset.subtract(s.last.multiply(forward)).lengthSquared() < 4) { slow(e, 25); if (++count >= 3) break; } }
        }
        if (stats.specialization == RPGSpecialization.BOMBARDIER && s.signature > 0 && now % 20 == 0) pulse(p, s.origin, false);
        if (stats.specialization == RPGSpecialization.ENGINEER && s.ascension > 0 && s.pulses > 0 && now >= s.pulseAt) { pulse(p, s.origin, true); s.pulses--; s.pulseAt = now + 40; }
        // Remove disconnected users opportunistically; sessions are also reset on CombatState replacement.
        SESSIONS.keySet().removeIf(id -> p.getServer().getPlayerManager().getPlayer(id) == null);
    }
    public static void remove(UUID owner) { SESSIONS.remove(owner); }
    public static void clear() { SESSIONS.clear(); SUPPLEMENT.remove(); }
    private ArcherTechniqueHandler() {}
}

