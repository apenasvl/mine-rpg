package com.rpgstats.mixin.compat;

import com.rpgstats.boss.BossLaunchTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pinned Returning Knight impulses occur both before and after hurt; preserve each native write. */
@Pseudo
@Mixin(targets="net.soulsweaponry.entity.ai.goal.ReturningKnightGoal",remap=false)
public abstract class SoulsBossLaunchMixin {
    @Unique private static java.lang.reflect.Field rpgstats$bossField;
    @Unique private LivingEntity rpgstats$boss() {
        try {
            if(rpgstats$bossField==null) {
                rpgstats$bossField=this.getClass().getDeclaredField("boss");rpgstats$bossField.setAccessible(true);
            }
            return (LivingEntity)rpgstats$bossField.get(this);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Pinned Returning Knight boss field changed",e);}
    }
    @Redirect(method={"tick","m_8037_"},at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;addVelocity(DDD)V",remap=true),require=1,remap=false)
    private void rpgstats$addImpulse(LivingEntity victim,double x,double y,double z) {
        double before=victim.getVelocity().y;victim.addVelocity(x,y,z);
        if(victim instanceof ServerPlayerEntity p)BossLaunchTracker.recordImpulse(p,rpgstats$boss(),before,p.getVelocity().y);
    }
    @Redirect(method={"tick","m_8037_"},at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;setVelocity(DDD)V",remap=true),require=2,remap=false)
    private void rpgstats$setImpulse(Entity victim,double x,double y,double z) {
        double before=victim.getVelocity().y;victim.setVelocity(x,y,z);
        if(victim instanceof ServerPlayerEntity p)BossLaunchTracker.recordImpulse(p,rpgstats$boss(),before,p.getVelocity().y);
    }
}
