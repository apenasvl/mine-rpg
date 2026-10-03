package com.rpgstats.mixin;

import com.rpgstats.boss.EncounterManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Observes RPG Stats' own healNearby helper without changing the class-mechanics code path.
 * Only effective ally healing / newly granted absorption is credited.
 */
@Mixin(targets = "com.rpgstats.combat.ClassMechanics", remap = false)
public abstract class ClassMechanicsSupportMixin {
    private static final ThreadLocal<Map<UUID, SupportSnapshot>> RPGSTATS_SUPPORT_BEFORE =
            ThreadLocal.withInitial(HashMap::new);

    @Inject(method = "healNearby", at = @At("HEAD"))
    private static void rpgstats$captureSupportBefore(ServerPlayerEntity player, double radius, float amount,
                                                       boolean absorption, CallbackInfo ci) {
        Map<UUID, SupportSnapshot> snapshots = new HashMap<>();
        if (player == null || amount <= 0f) {
            RPGSTATS_SUPPORT_BEFORE.set(snapshots);
            return;
        }

        Box box = player.getBoundingBox().expand(radius);
        for (ServerPlayerEntity ally : player.getServerWorld().getEntitiesByClass(ServerPlayerEntity.class, box,
                p -> p.isAlive() && p != player
                        && (player.getScoreboardTeam() == null || player.isTeammate(p)))) {
            snapshots.put(ally.getUuid(), new SupportSnapshot(
                    ally.getHealth(), ally.getAbsorptionAmount(), ally.getMaxHealth()));
        }
        RPGSTATS_SUPPORT_BEFORE.set(snapshots);
    }

    @Inject(method = "healNearby", at = @At("RETURN"))
    private static void rpgstats$recordSupportAfter(ServerPlayerEntity player, double radius, float amount,
                                                     boolean absorption, CallbackInfo ci) {
        Map<UUID, SupportSnapshot> snapshots = RPGSTATS_SUPPORT_BEFORE.get();
        RPGSTATS_SUPPORT_BEFORE.remove();
        if (player == null || snapshots.isEmpty()) return;

        Box box = player.getBoundingBox().expand(radius);
        for (ServerPlayerEntity ally : player.getServerWorld().getEntitiesByClass(ServerPlayerEntity.class, box,
                p -> p.isAlive() && p != player && snapshots.containsKey(p.getUuid()))) {
            SupportSnapshot before = snapshots.get(ally.getUuid());
            float effectiveHealing = Math.max(0f, ally.getHealth() - before.health());
            float grantedProtection = Math.max(0f, ally.getAbsorptionAmount() - before.absorption());
            if (effectiveHealing <= 0f && grantedProtection <= 0f) continue;
            EncounterManager.recordSupport(player, ally, effectiveHealing, grantedProtection);
        }
    }

    private record SupportSnapshot(float health, float absorption, float maxHealth) {}
}
