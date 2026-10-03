package com.rpgstats.mixin;

import com.rpgstats.IEntityDataSaver;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements IEntityDataSaver {
    @Unique private NbtCompound rpgstatsPersistentData;

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void rpgstats$write(NbtCompound nbt, CallbackInfo ci) {
        if (this.rpgstatsPersistentData != null) {
            nbt.put("rpgstats.data", this.rpgstatsPersistentData);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void rpgstats$read(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("rpgstats.data", NbtElement.COMPOUND_TYPE)) {
            this.rpgstatsPersistentData = nbt.getCompound("rpgstats.data");
        }
    }

    @Override
    public NbtCompound rpgstats$getPersistentData() {
        if (this.rpgstatsPersistentData == null) {
            this.rpgstatsPersistentData = new NbtCompound();
        }
        return this.rpgstatsPersistentData;
    }
}
