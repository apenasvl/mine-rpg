package com.rpgstats.mixin;
import com.rpgstats.integration.DataDrivenRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ArmorItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Disabled gear stays in inventory, with no native passive ticking on its player. */
@Mixin(ItemStack.class)
public abstract class NativeItemInventoryMixin {
    @Inject(method="inventoryTick",at=@At("HEAD"),cancellable=true)
    private void rpgstats$nativeInventory(World world,Entity entity,int slot,boolean selected,CallbackInfo ci) {
        if(!(entity instanceof ServerPlayerEntity))return;
        ItemStack stack=(ItemStack)(Object)this;
        boolean disabled=DataDrivenRegistry.equipmentRules(stack).map(r->!r.enabled()).orElse(false);
        boolean armor=stack.getItem() instanceof ArmorItem && Registries.ITEM.getId(stack.getItem()).getNamespace().equals("legendary_monsters");
        if(disabled || armor)ci.cancel();
    }
}
