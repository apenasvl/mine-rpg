package com.rpgstats.combat;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * Complementos de identidade das especializacoes do Arqueiro.
 *
 * ClassMechanics continua sendo o motor principal. Esta camada existe apenas para efeitos que
 * precisam de geometria/AoE no evento real de projétil e que nao devem virar multiplicador bruto:
 * perfuracao, corrente, explosao lateral, zona de controle e recuperacao de Focus.
 */
public final class ArcherSpecializationHandler {
    private static final ThreadLocal<Boolean> PROC_GUARD = ThreadLocal.withInitial(() -> false);

    /** Called only after positive confirmed damage and the launch-group claim. */
    public static void onHit(ServerPlayerEntity player, LivingEntity target, PlayerStats stats,
                             net.minecraft.entity.damage.DamageSource source, float damage) {
        if (PROC_GUARD.get() || target.getWorld().isClient || ProcDamageQueue.isApplying()) return;
        if (!(source.getSource() instanceof ProjectileEntity projectile)) return;
        if (stats.clazz != RPGClass.ARQUEIRO || stats.specialization == null
                || !stats.unlockedNodes.contains(stats.specialization.nodes.get(0).id())) return;
        CombatState state = CombatState.get(player.getUuid());
        float base = Math.max(0f, damage);
        if (base <= 0f) return;

        switch (stats.specialization) {
            case ACROBAT -> acrobat(player, stats, state);
            case BALLISTICIAN -> ballistician(player, projectile, target, stats, state, base);
            case STORMBOW -> { if (state.classMode == 2) stormbow(player, target, stats, state, base); }
            case FLAMEBOW -> { if (state.classMode == 0) flamebow(player, target, stats, state); }
            case FROSTBOW -> { if (state.classMode == 1) frostbow(target, stats, state); }
            case BOMBARDIER -> bombardier(player, target, stats, state, base);
            // Engineer effects are anchored and capped in ArcherTechniqueHandler.
            default -> { }
        }
    }

    /** Acrobata converte tiro realmente aereo em Focus, com ICD para arcos multishot. */
    private static void acrobat(ServerPlayerEntity player, PlayerStats stats, CombatState state) {
        if (player.isOnGround() || state.timer("internal_arc_acrobat_focus") > 0) return;
        state.addGauge("arc_focus", 0.35f * engineScale(stats), 10f);
        state.startTimer("internal_arc_acrobat_focus", 8);
    }

    /**
     * Balistico: a cada terceiro acerto arma uma perfuracao geometrica. Apenas um alvo atras do
     * primeiro recebe dano secundario, e esse dano nunca reaplica o pipeline do Arqueiro.
     */
    private static void ballistician(ServerPlayerEntity player, ProjectileEntity projectile, LivingEntity target,
                                     PlayerStats stats, CombatState state, float base) {
        float chargeGain = (state.timer("arc_ballistic") > 0 ? 2f : 1f) * engineScale(stats);
        float charge = state.addGauge("arc_ballistic_pierce", chargeGain, 3f);
        if (charge < 2.99f) return;
        state.setGauge("arc_ballistic_pierce", 0f, 3f);
        if (state.timer("arc_ballistic") > 0) state.setTimer("arc_ballistic", 0);

        ServerWorld world = player.getServerWorld();
        Vec3d direction = projectile.getVelocity();
        if (direction.lengthSquared() < 0.001) direction = target.getPos().subtract(player.getPos());
        if (direction.lengthSquared() < 0.001) return;
        direction = direction.normalize();

        Box area = target.getBoundingBox().expand(6.0);
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, area,
                e -> validExtraTarget(player, target, e));
        candidates.sort(Comparator.comparingDouble(target::squaredDistanceTo));

        for (LivingEntity extra : candidates) {
            Vec3d offset = extra.getPos().subtract(target.getPos());
            double forward = offset.dotProduct(direction);
            if (forward <= 0.4 || forward > 6.0) continue;
            double lateralSq = offset.subtract(direction.multiply(forward)).lengthSquared();
            if (lateralSq > 2.25) continue;
            procDamage(player, projectile, extra, Math.min(4.5f, base * 0.30f));
            break;
        }
    }

    /** Tempestade: tres acertos carregam uma corrente curta para apenas um alvo adicional. */
    private static void stormbow(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state, float base) {
        float charge = state.addGauge("arc_storm_chain", engineScale(stats), 3f);
        if (charge < 2.99f || state.timer("internal_arc_storm_chain") > 0) return;
        state.setGauge("arc_storm_chain", 0f, 3f);
        state.startTimer("internal_arc_storm_chain", 10);

        LivingEntity extra = nearestExtraTarget(player, target, 5.0);
        if (extra != null) procDamage(player, player, extra, Math.min(4.5f, base * 0.28f));
    }

    /** Arco Igneo ganha propagacao de queimadura durante a janela elemental, nao explosao de dano gratis. */
    private static void flamebow(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state) {
        if (state.timer("arc_elemental_reaction") <= 0 || state.timer("internal_arc_flame_splash") > 0) return;
        state.startTimer("internal_arc_flame_splash", 20);
        Box area = target.getBoundingBox().expand(hasEngine(stats) ? 4.25 : 3.5);
        int affected = 0;
        for (LivingEntity extra : player.getServerWorld().getEntitiesByClass(LivingEntity.class, area,
                e -> validExtraTarget(player, target, e))) {
            extra.setOnFireFor(hasEngine(stats) ? 3 : 2);
            if (++affected >= 4) break;
        }
    }

    /** Arco Glacial troca dano por controle: a janela elemental intensifica o slow, chefes resistem. */
    private static void frostbow(LivingEntity target, PlayerStats stats, CombatState state) {
        if (state.timer("arc_elemental_reaction") <= 0) return;
        boolean boss = BossScaler.getTier(target) > 0;
        int duration = Math.round((boss ? 22 : 38) * engineScale(stats));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, boss ? 0 : 1));
    }

    /**
     * Bombardeiro so explode quando sua tecnica armou `arc_bomb_shot`. O alvo principal nao toma
     * segundo hit; a explosao e exclusivamente AoE e possui ICD contra multishot.
     */
    private static void bombardier(ServerPlayerEntity player, LivingEntity target, PlayerStats stats, CombatState state, float base) {
        if (state.timer("arc_bomb_shot") <= 0 || state.timer("internal_arc_bombardier") > 0) return;
        state.startTimer("internal_arc_bombardier", 18);
        Box area = target.getBoundingBox().expand(3.5);
        int hit = 0;
        for (LivingEntity extra : player.getServerWorld().getEntitiesByClass(LivingEntity.class, area,
                e -> validExtraTarget(player, target, e))) {
            procDamage(player, player, extra, Math.min(4.0f, base * 0.25f));
            if (++hit >= 3) break;
        }
    }

    private static boolean hasEngine(PlayerStats stats) {
        if (stats == null || stats.specialization == null) return false;
        String prefix = stats.specialization.nodes.get(0).id().replace("_initiation", "");
        return stats.unlockedNodes.contains(prefix + "_engine");
    }

    private static float engineScale(PlayerStats stats) {
        return hasEngine(stats) ? 1.25f : 1f;
    }

    private static LivingEntity nearestExtraTarget(ServerPlayerEntity player, LivingEntity center, double radius) {
        Box area = center.getBoundingBox().expand(radius);
        return player.getServerWorld().getEntitiesByClass(LivingEntity.class, area,
                        e -> validExtraTarget(player, center, e)).stream()
                .min(Comparator.comparingDouble(center::squaredDistanceTo)).orElse(null);
    }

    private static boolean validExtraTarget(ServerPlayerEntity player, LivingEntity original, LivingEntity target) {
        if (!target.isAlive() || target == original || target == player || target instanceof PlayerEntity) return false;
        if (target instanceof PassiveEntity) return false;
        return !(target instanceof TameableEntity tameable) || tameable.getOwner() != player;
    }

    private static void procDamage(ServerPlayerEntity player, net.minecraft.entity.Entity source,
                                   LivingEntity target, float amount) {
        if (amount <= 0.01f) return;
        try {
            PROC_GUARD.set(true);
            ProcDamageQueue.damageExtra(player, target, amount);
        } finally {
            PROC_GUARD.set(false);
        }
    }

    private ArcherSpecializationHandler() {}
}

