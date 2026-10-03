package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Pinned binary: reject publish BEFORE animation, invulnerability, exhaustion and callbacks. */
@Pseudo
@Mixin(targets="net.combatroll.network.ServerNetwork", remap=false)
public abstract class CombatRollGateMixin {
    @Inject(method="lambda$initializeHandlers$5", at=@At("HEAD"), cancellable=true, require=1, remap=false)
    private static void rpgstats$rejectFreeRoll(CallbackInfo ci) { ci.cancel(); }
}
