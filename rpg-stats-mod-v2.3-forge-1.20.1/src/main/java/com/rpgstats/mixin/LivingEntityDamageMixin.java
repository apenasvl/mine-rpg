package com.rpgstats.mixin;

import com.rpgstats.combat.CombatHandler;
import com.rpgstats.combat.ProcDamageQueue;
import com.rpgstats.compat.CompatManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Modifica e reage a dano envolvendo jogadores, com protecao contra proc recursivo de thorns. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Unique private boolean rpgstats$deathRewarded;
    @Unique private final java.util.ArrayDeque<DamageSource> rpgstats$damageSources=new java.util.ArrayDeque<>();
    @Unique private final java.util.ArrayDeque<Double> rpgstats$knockbackY=new java.util.ArrayDeque<>();
    @Inject(method="takeKnockback",at=@At("HEAD"))
    private void rpgstats$beforeKnockback(double strength,double x,double z,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        rpgstats$knockbackY.push(((LivingEntity)(Object)this).getVelocity().y);
    }
    @Inject(method="takeKnockback",at=@At("RETURN"))
    private void rpgstats$bossKnockback(double strength,double x,double z,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        double before=rpgstats$knockbackY.pop();
        if(self instanceof ServerPlayerEntity p && !rpgstats$damageSources.isEmpty()
                && rpgstats$damageSources.peek().getAttacker() instanceof LivingEntity boss)
            com.rpgstats.boss.BossLaunchTracker.recordImpulse(p,boss,before,p.getVelocity().y);
    }

    @Inject(method="onDeath", at=@At("TAIL"))
    private void rpgstats$confirmedDeath(DamageSource source,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if (!(self instanceof ServerPlayerEntity) && !self.getWorld().isClient && !rpgstats$deathRewarded) {
            rpgstats$deathRewarded=true;
            com.rpgstats.forge.ForgeEvents.confirmedDeath(self,source);
        }
    }
    @Unique private final java.util.ArrayDeque<Float> rpgstats$healthBefore=new java.util.ArrayDeque<>();
    @Unique private final java.util.ArrayDeque<Float> rpgstats$trainingDamage = new java.util.ArrayDeque<>();
    @Unique private final java.util.ArrayDeque<Boolean> rpgstats$contributionRecorded=new java.util.ArrayDeque<>();
    @Inject(method="onDeath",at=@At("HEAD"))
    private void rpgstats$lethalContribution(DamageSource source,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(self.getHealth()>0 || rpgstats$healthBefore.isEmpty() || rpgstats$contributionRecorded.isEmpty() || rpgstats$contributionRecorded.peek())return;
        float actual=Math.max(0,rpgstats$healthBefore.peek()-self.getHealth());
        rpgstats$contributionRecorded.pop();rpgstats$contributionRecorded.push(true);
        com.rpgstats.forge.ForgeEvents.confirmedDamage(self,source,actual);
    }
    @Inject(method="damage",at=@At("HEAD"))
    private void rpgstats$beforeDamage(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self=(LivingEntity)(Object)this;
        rpgstats$damageSources.push(source);rpgstats$healthBefore.push(self.getHealth());rpgstats$contributionRecorded.push(false);rpgstats$trainingDamage.push(0f);
    }


    /** Target Dummy deliberately keeps its health. Capture the accepted, mitigated health write,
     * then resolve combat callbacks on damage RETURN. Cancelled/absorbed hits capture nothing.
     * The simulated amount never enters boss contribution or completion XP accounting. */
    @ModifyArg(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setHealth(F)V"), index = 0)
    private float rpgstats$captureTrainingDamage(float nextHealth) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.getWorld().isClient && !rpgstats$trainingDamage.isEmpty()
                && com.rpgstats.compat.TargetDummyCompat.isTrainingTarget(self)) {
            float accepted = Math.max(0f, self.getHealth() - nextHealth);
            float previous = rpgstats$trainingDamage.pop();
            rpgstats$trainingDamage.push(Math.max(previous, accepted));
        }
        return nextHealth;
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float rpgstats$modifyDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ProcDamageQueue.isApplying()) return amount;

        if (self instanceof ServerPlayerEntity player) {
            amount = CombatHandler.modifyIncomingDamage(player, amount, source);
        }

        // Iron's has custom school DamageTypes. Its optional adapter owns the outgoing spell path,
        // otherwise this generic hook would misclassify many spells as physical/projectile attacks.
        if (!CompatManager.isIronsSpellDamage(source) && !source.isOf(DamageTypes.THORNS)
                && source.getAttacker() instanceof ServerPlayerEntity attacker && self != attacker) {
            amount = CombatHandler.modifyOutgoingDamage(attacker, self, amount, source);
        }
        return amount;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void rpgstats$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if(!rpgstats$damageSources.isEmpty())rpgstats$damageSources.pop();
        float before=rpgstats$healthBefore.isEmpty()?self.getHealth():rpgstats$healthBefore.pop();
        boolean recorded=!rpgstats$contributionRecorded.isEmpty() && rpgstats$contributionRecorded.pop();
        float training = rpgstats$trainingDamage.isEmpty() ? 0f : rpgstats$trainingDamage.pop();
        if (!cir.getReturnValue()) {
            if(self instanceof ServerPlayerEntity player)com.rpgstats.boss.BossLaunchTracker.finishFall(player,source);
            return;
        }
        float actual=Math.max(0,before-self.getHealth());
        if(actual > 0 && !recorded)com.rpgstats.forge.ForgeEvents.confirmedDamage(self,source,actual);
        if(self instanceof ServerPlayerEntity player)com.rpgstats.boss.BossLaunchTracker.finishFall(player,source);
        float resolved = actual > 0 ? actual : training;
        if(resolved<=0)return;
        if (ProcDamageQueue.isApplying()) return;

        if (self instanceof ServerPlayerEntity player) {
            CombatHandler.onPlayerHurt(player, resolved, source);
        }
        if (!CompatManager.isIronsSpellDamage(source) && !source.isOf(DamageTypes.THORNS)
                && source.getAttacker() instanceof ServerPlayerEntity attacker && self != attacker) {
            CombatHandler.onPlayerHitEntity(attacker, self, resolved, source);
        }
    }
}
