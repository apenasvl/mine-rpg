package com.rpgstats.compat;

import com.rpgstats.stats.PlayerStats;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Optional authority for the Mage primary Mana pool.
 *
 * The core never imports an external magic mod. Without an implementation RPG Stats keeps using
 * PlayerStats.resource; an optional adapter may become authoritative and mirror its pool to the HUD.
 */
public interface MageManaBridge {
    boolean sync(ServerPlayerEntity player, PlayerStats stats);
    float current(ServerPlayerEntity player);
    void set(ServerPlayerEntity player, float amount);
    boolean isCasting(ServerPlayerEntity player);
    default int nativeSummonCount(ServerPlayerEntity player, PlayerStats stats) { return 0; }
    default float nativeSummonDiversityBonus(ServerPlayerEntity player, PlayerStats stats) { return 0f; }
    default boolean focusNativeSummons(ServerPlayerEntity player, PlayerStats stats,
                                       net.minecraft.entity.LivingEntity target) { return false; }
    default boolean transferNativeSummons(ServerPlayerEntity player, PlayerStats stats) { return false; }
    default float nativeIncomingMultiplier(ServerPlayerEntity player, PlayerStats stats) { return 1f; }
}

