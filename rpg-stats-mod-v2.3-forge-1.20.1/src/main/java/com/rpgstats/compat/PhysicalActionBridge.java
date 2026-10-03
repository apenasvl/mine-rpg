package com.rpgstats.compat;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;
public interface PhysicalActionBridge {
    enum Route { CORE, EXTERNAL, REJECTED }
    record ActionDecision(Route route, String reason) {}
    ActionDecision preflight(ServerPlayerEntity player, PlayerStats stats, String nodeId, SkillEffect effect);
    boolean execute(ServerPlayerEntity player, PlayerStats stats, String nodeId, SkillEffect effect);
    default void forget(UUID playerId) {}
}
