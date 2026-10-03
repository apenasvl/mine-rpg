package com.rpgstats.mixin.compat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.combatroll.client.gui.HudRenderHelper", remap=false)
public abstract class CombatRollHudMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private static void rpgstats$useRpgCooldown(CallbackInfo ci) { ci.cancel(); }
}
