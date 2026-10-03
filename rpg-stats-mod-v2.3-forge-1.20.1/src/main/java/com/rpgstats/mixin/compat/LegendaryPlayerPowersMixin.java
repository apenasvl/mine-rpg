package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Discarded armor/pet keys must not execute native client-requested powers. Boss packets stay intact. */
@Pseudo
@Mixin(targets={"net.miauczel.legendary_monsters.Message.AnnihilatorHelmetAbilityMessage$Handler",
 "net.miauczel.legendary_monsters.Message.AnnihilatorChestplatePlaySoundMessage$Handler",
 "net.miauczel.legendary_monsters.Message.SkeloraptorRoarKeyMessage$Handler",
 "net.miauczel.legendary_monsters.Message.SkeloraptorTailAttackMessage$Handler"},remap=false)
public abstract class LegendaryPlayerPowersMixin {
 @Inject(method="onMessage",at=@At("HEAD"),cancellable=true,remap=false,require=1)
 private static void rpgstats$discardKeyPower(CallbackInfo ci){ci.cancel();}
}
