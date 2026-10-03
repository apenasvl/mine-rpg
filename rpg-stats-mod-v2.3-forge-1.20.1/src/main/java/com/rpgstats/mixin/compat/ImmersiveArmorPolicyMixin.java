package com.rpgstats.mixin.compat;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Keep armor/model/weight; martial powers belong to the RPG progression. Pinned 1.7.2 API. */
@Pseudo
@Mixin(targets="immersive_armors.item.ExtendedArmorMaterial", remap=false)
public abstract class ImmersiveArmorPolicyMixin {
    @Inject(method="getWeight",at=@At("RETURN"),cancellable=true,require=1,remap=false)
    private void rpgstats$noFreeMovement(CallbackInfoReturnable<Float> ci) {
        ci.setReturnValue(com.rpgstats.compat.ArmorBonusPolicy.weight(ci.getReturnValue()));
    }
    @Inject(method="getEffects",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void rpgstats$effects(CallbackInfoReturnable<List<?>> ci) { ci.setReturnValue(List.of()); }
    @Inject(method="getEnchantments",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void rpgstats$enchantments(CallbackInfoReturnable<Map<?,?>> ci) { ci.setReturnValue(Map.of()); }
    @Inject(method={"getExtraHealth","getLuck","getEnchantment"},at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void rpgstats$noFreeStats(CallbackInfoReturnable<Integer> ci) { ci.setReturnValue(0); }
    @Inject(method={"getAttackDamage","getAttackSpeed"},at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void rpgstats$noFreeAttack(CallbackInfoReturnable<Float> ci) { ci.setReturnValue(0f); }
    @Inject(method={"hasEnchantment","isAntiSkeleton"},at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void rpgstats$noFreePassive(CallbackInfoReturnable<Boolean> ci) { ci.setReturnValue(false); }
}
