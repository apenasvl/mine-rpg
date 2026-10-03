package com.rpgstats.mixin;

import com.rpgstats.forge.ForgeEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ServerPlayer overrides onDeath; hook its successful tail separately from LivingEntity. */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerDeathMixin {
    @Unique private boolean rpgstats$deathRewarded;
    @Inject(method="onDeath",at=@At("TAIL"))
    private void rpgstats$afterDeath(DamageSource source, CallbackInfo ci) {
        if (!rpgstats$deathRewarded) {
            rpgstats$deathRewarded=true;
            ForgeEvents.confirmedDeath((ServerPlayerEntity)(Object)this,source);
        }
    }
}
