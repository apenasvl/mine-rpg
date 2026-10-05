package com.rpgstats.mixin.compat;
import net.minecraft.entity.damage.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Keep player arrows as arrows; apply the native enchantment before the RPG damage budget. */
@Pseudo @Mixin(targets="betterweaponry.procedures.EnchantmentsTriggerProcedure",remap=false)
public abstract class BetterWeaponryProjectileMixin {
 @Redirect(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;D)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/DamageSource;m_276093_(Lnet/minecraft/resources/ResourceKey;)Z",remap=false),require=1,remap=false)
 private static boolean rpgstats$keepPlayerProjectileType(DamageSource source,RegistryKey<DamageType> type) {
  return !(source.getAttacker() instanceof ServerPlayerEntity) && source.isOf(type);
 }
}
