package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.miauczel.legendary_monsters.item.custom.customArmor.armorItem.AnnihilatorArmorItem",remap=false)
public abstract class LegendaryArmorTickMixin {
 @Inject(method="onArmorTick",at=@At("HEAD"),cancellable=true,remap=false,require=1)
 private void rpgstats$discardArmorPower(CallbackInfo ci){ci.cancel();}
}
