package com.rpgstats.mixin.compat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Pinned original Forge bytecode writes health before the hit is accepted. */
@Pseudo @Mixin(targets="betterweaponry.procedures.EnchantmentsTriggerProcedure",remap=false)
public abstract class BetterWeaponryVampirismMixin {
 @Redirect(method="execute",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_21153_(F)V",remap=false),require=1,remap=false)
 private static void rpgstats$deferPlayerVampirism(LivingEntity entity,float health) {
  if(!(entity instanceof ServerPlayerEntity))entity.setHealth(health);
 }
}
