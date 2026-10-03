package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Class techniques replace native client-triggered abilities, before costs and effects. */
@Pseudo
@Mixin(targets={"net.soulsweaponry.networking.packets.C2S.AttackClickC2S","net.soulsweaponry.networking.packets.C2S.CollectSummonsC2S","net.soulsweaponry.networking.packets.C2S.KeybindAbilityC2S","net.soulsweaponry.networking.packets.C2S.KillNearbyEntitiesC2S","net.soulsweaponry.networking.packets.C2S.MoonlightC2S","net.soulsweaponry.networking.packets.C2S.ParryC2S","net.soulsweaponry.networking.packets.C2S.ReturnFreyrSwordC2S","net.soulsweaponry.networking.packets.C2S.ReturnThrownWeaponC2S","net.soulsweaponry.networking.packets.C2S.StationaryFreyrSwordC2S","net.soulsweaponry.networking.packets.C2S.SwitchTrickWeaponC2S"},remap=false)
public abstract class SoulsClientActionsGateMixin {
    @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void rpgstats$rejectNativeActions(CallbackInfo ci){ci.cancel();}
}
