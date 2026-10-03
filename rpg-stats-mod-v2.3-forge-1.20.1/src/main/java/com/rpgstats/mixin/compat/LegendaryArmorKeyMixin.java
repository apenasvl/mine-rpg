package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.miauczel.legendary_monsters.Message.MessageArmorKey",remap=false)
public abstract class LegendaryArmorKeyMixin {
 @Inject(method="handle",at=@At("HEAD"),cancellable=true,remap=false,require=1)
 private static void rpgstats$discardKeyPower(CallbackInfo ci){ci.cancel();}
}
