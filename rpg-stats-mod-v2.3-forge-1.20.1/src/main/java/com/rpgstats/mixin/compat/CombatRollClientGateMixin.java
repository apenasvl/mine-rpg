package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="net.combatroll.internals.RollManager", remap=false)
public abstract class CombatRollClientGateMixin {
    @Inject(method="isRollAvailable", at=@At("HEAD"), cancellable=true, require=1, remap=false)
    private void rpgstats$disableFreeKey(CallbackInfoReturnable<Boolean> ci) { ci.setReturnValue(false); }
}
