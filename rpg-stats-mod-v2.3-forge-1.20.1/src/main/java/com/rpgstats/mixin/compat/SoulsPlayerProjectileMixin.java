package com.rpgstats.mixin.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native projectile-blocking bosses accept player bow/trident shots; AI and phase guards stay native. */
@Pseudo
@Mixin(targets="net.soulsweaponry.entity.mobs.BossEntity",remap=false)
public abstract class SoulsPlayerProjectileMixin {
 @Inject(target=@Desc(value="isProjectileWhitelisted",args=Entity.class,ret=boolean.class),
         at=@At("HEAD"),cancellable=true,require=1,remap=false)
 private void rpgstats$allowPlayerShot(Entity entity,CallbackInfoReturnable<Boolean> ci){
  if(entity instanceof PersistentProjectileEntity shot && shot.getOwner() instanceof ServerPlayerEntity)
   ci.setReturnValue(true);
 }
}
