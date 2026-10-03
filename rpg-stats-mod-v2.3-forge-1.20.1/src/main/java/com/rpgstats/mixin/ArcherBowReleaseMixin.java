package com.rpgstats.mixin;
import com.rpgstats.combat.ArcherShotTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers every custom BowItem override through vanilla's real use/release lifecycle. */
@Mixin(LivingEntity.class)
public abstract class ArcherBowReleaseMixin {
    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void rpgstats$beginRelease(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player)
            ArcherShotTracker.beginLaunch(player, player.getActiveItem());
    }
    @Inject(method = "stopUsingItem", at = @At("RETURN"))
    private void rpgstats$endRelease(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player) ArcherShotTracker.endLaunch(player);
    }
}
