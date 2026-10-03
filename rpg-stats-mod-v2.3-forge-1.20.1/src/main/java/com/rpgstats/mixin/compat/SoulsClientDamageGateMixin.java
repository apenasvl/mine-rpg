package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Reject native client-authorized effects before any cost or world mutation. */
@Pseudo
@Mixin(targets="net.soulsweaponry.networking.packets.C2S.DamagingBoxC2S", remap=false)
public abstract class SoulsClientDamageGateMixin {
    @Inject(method="handlePacket", at=@At("HEAD"), cancellable=true, require=1, remap=false)
    private void rpgstats$rejectUntrustedEffect(CallbackInfo ci) { ci.cancel(); }
}
