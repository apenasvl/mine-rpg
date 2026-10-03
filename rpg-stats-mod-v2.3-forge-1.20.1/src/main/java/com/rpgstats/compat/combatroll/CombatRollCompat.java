package com.rpgstats.compat.combatroll;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.classes.RPGSpecialization;
import com.rpgstats.compat.*;
import com.rpgstats.network.RpgNetwork;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
/** Server owns displacement; Combat Roll contributes visual animation only. */
public final class CombatRollCompat implements PhysicalActionBridge {
    public ActionDecision preflight(ServerPlayerEntity p, PlayerStats s, String id, SkillEffect e) {
        if (!PhysicalActionPolicy.isRollNode(id)) return new ActionDecision(Route.CORE, "");
        if (s.clazz != RPGClass.ARQUEIRO || s.specialization != RPGSpecialization.ownerOfNode(id) || !s.unlockedNodes.contains(id))
            return new ActionDecision(Route.REJECTED, "Aprenda a tecnica da sua especializacao.");
        if (!PhysicalActionPolicy.canRoll(p.isAlive() && !p.isSpectator(), p.isOnGround(), p.isTouchingWater() || p.isInLava(), p.hasVehicle(), p.isUsingItem(), p.isFallFlying() || p.getAbilities().flying))
            return new ActionDecision(Route.REJECTED, "Role no chao, fora da agua e sem usar um item.");
        return new ActionDecision(Route.EXTERNAL, "");
    }
    public boolean execute(ServerPlayerEntity p, PlayerStats s, String id, SkillEffect e) {
        if (preflight(p,s,id,e).route() != Route.EXTERNAL) return false;
        Vec3d look=p.getRotationVec(1f);
        double scale="arc_ward_surv_technique".equals(id) ? Math.max(.45f,Math.min(1f,e.value())) : 1;
        Vec3d motion=new Vec3d(look.x,0,look.z).normalize().multiply(-.65*scale);
        try {
            RpgNetwork.rollVisual(p, motion);
        } catch (RuntimeException error) {
            com.rpgstats.RPGStatsMod.LOGGER.error("Falha ao enviar animacao de rolamento",error);
            return false;
        }
        p.addVelocity(motion.x,"arc_ward_surv_technique".equals(id) ? .12 : .08,motion.z);
        p.velocityModified=true;
        return true;
    }
}
