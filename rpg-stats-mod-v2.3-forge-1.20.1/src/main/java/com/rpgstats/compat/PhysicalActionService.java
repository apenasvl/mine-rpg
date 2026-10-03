package com.rpgstats.compat;
import com.rpgstats.ability.SkillEffect;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;
public final class PhysicalActionService {
    private static PhysicalActionBridge bridge;
    public static void register(PhysicalActionBridge value) { bridge = value; }
    public static PhysicalActionBridge.ActionDecision preflight(ServerPlayerEntity p, PlayerStats s, String id, SkillEffect e) {
        return bridge == null ? new PhysicalActionBridge.ActionDecision(PhysicalActionBridge.Route.CORE, "") : bridge.preflight(p,s,id,e);
    }
    /** Called only inside CombatHandler after cost/cooldown validation. No C2S permission/token exists. */
    public static boolean executeAuthorized(ServerPlayerEntity p, PlayerStats s, String id, SkillEffect e) {
        return bridge != null && bridge.execute(p,s,id,e);
    }
    public static void forget(UUID id) { if (bridge != null) bridge.forget(id); }
    private PhysicalActionService() {}
}
