package com.rpgstats.mixin;

import com.rpgstats.forge.ForgeEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ServerPlayer overrides damage/onDeath, including rejection before LivingEntity is called. */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerDeathMixin {
    @Unique private boolean rpgstats$deathRewarded;
    @Inject(method="damage",at=@At("HEAD"))
    private void rpgstats$beginPlayerDamage(DamageSource source,float amount,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        com.rpgstats.boss.BossLaunchTracker.beginDamage((ServerPlayerEntity)(Object)this);
    }
    @Inject(method="damage",at=@At("RETURN"))
    private void rpgstats$rejectedPlayerDamage(DamageSource source,float amount,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue()) {
            var p=(ServerPlayerEntity)(Object)this;
            com.rpgstats.boss.BossLaunchTracker.finishDamage(p,0);
            com.rpgstats.boss.BossLaunchTracker.finishFall(p,source);
        }
    }
    @Inject(method="onDeath",at=@At("TAIL"))
    private void rpgstats$afterDeath(DamageSource source, CallbackInfo ci) {
        if (!rpgstats$deathRewarded) {
            rpgstats$deathRewarded=true;
            ForgeEvents.confirmedDeath((ServerPlayerEntity)(Object)this,source);
        }
    }
}
