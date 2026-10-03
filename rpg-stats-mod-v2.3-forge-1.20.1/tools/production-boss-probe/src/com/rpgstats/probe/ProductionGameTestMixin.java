package com.rpgstats.probe.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Isolated CI probe: lets Forge register its tests in an installed production namespace. */
@Mixin(value=net.minecraftforge.gametest.ForgeGameTestHooks.class,remap=false)
public abstract class ProductionGameTestMixin {
    @Inject(method={"isGametestEnabled","isGametestServer"},at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private static void enableOnlyInProbe(CallbackInfoReturnable<Boolean> ci) {
        if(Boolean.getBoolean("rpgstats.productionBossTests") && Boolean.getBoolean("forge.gameTestServer"))ci.setReturnValue(true);
    }
}
