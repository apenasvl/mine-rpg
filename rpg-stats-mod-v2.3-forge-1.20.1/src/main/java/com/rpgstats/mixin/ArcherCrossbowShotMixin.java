package com.rpgstats.mixin;
import com.rpgstats.combat.ArcherShotTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Charged offhand crossbows do not enter the drawn-bow release lifecycle. */
@Mixin(CrossbowItem.class)
public abstract class ArcherCrossbowShotMixin {
    @Inject(method = "shootAll", at = @At("HEAD"))
    private static void rpgstats$beginVolley(World world, LivingEntity shooter, Hand hand, ItemStack stack, float speed, float divergence, CallbackInfo ci) {
        if (shooter instanceof ServerPlayerEntity player) ArcherShotTracker.beginLaunch(player, stack);
    }
    @Inject(method = "shootAll", at = @At("RETURN"))
    private static void rpgstats$endVolley(World world, LivingEntity shooter, Hand hand, ItemStack stack, float speed, float divergence, CallbackInfo ci) {
        if (shooter instanceof ServerPlayerEntity player) ArcherShotTracker.endLaunch(player);
    }
}
