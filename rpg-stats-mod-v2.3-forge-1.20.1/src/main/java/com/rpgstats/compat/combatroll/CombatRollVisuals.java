package com.rpgstats.compat.combatroll;
import com.rpgstats.network.RpgNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
/** Loaded only on the client. No native C2S publish, hunger, i-frames or second displacement. */
public final class CombatRollVisuals {
    private static boolean failed;
    public static void receive(RpgNetwork.RollVisual packet) {
        var client=MinecraftClient.getInstance();
        if (failed || client.world == null || !client.world.getRegistryKey().getValue().equals(packet.dimension())
                || !net.minecraftforge.fml.ModList.get().isLoaded("combatroll")) return;
        var player=client.world.getPlayerByUuid(packet.player());
        if (player == null) return;
        try {
            Class<?> particles=Class.forName("net.combatroll.client.RollEffect$Particles");
            Class<?> visuals=Class.forName("net.combatroll.client.RollEffect$Visuals");
            Object value=visuals.getConstructor(String.class,particles).newInstance("combatroll:roll",particles.getField("PUFF").get(null));
            Class.forName("net.combatroll.client.RollEffect").getMethod("playVisuals",visuals,PlayerEntity.class,Vec3d.class).invoke(null,value,player,packet.motion());
        } catch (ReflectiveOperationException | LinkageError error) {
            failed=true;
            com.rpgstats.RPGStatsMod.LOGGER.error("Animacao Combat Roll indisponivel; movimento RPG preservado",error);
        }
    }
    private CombatRollVisuals() {}
}
