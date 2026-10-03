package com.rpgstats.mixin.compat;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native item powers are replaced by RPG class abilities; registration still builds native items normally. */
@Pseudo
@Mixin(targets={"net.soulsweaponry.items.ModdedSword","net.soulsweaponry.items.armor.ModdedArmor",
        "net.soulsweaponry.items.axe.ModdedAxe","net.soulsweaponry.items.bow.ModdedBow",
        "net.soulsweaponry.items.crossbow.ModdedCrossbow","net.soulsweaponry.items.gun.GunItem",
        "net.soulsweaponry.items.misc.ModdedItem"},remap=false)
public abstract class SoulsRpgAbilitiesMixin {
    @Inject(method="getAbilities",at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private void rpgstats$replaceItemPowers(CallbackInfoReturnable<List<?>> ci) {
        if(com.rpgstats.compat.bosses.NativePowersPolicy.replacesNativeAbilities())ci.setReturnValue(List.of());
    }
}
