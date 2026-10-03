package com.rpgstats.network;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.combat.CombatHandler;
import com.rpgstats.stats.StatsManager;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Forge channel. Directions and packet sizes are checked before touching game state. */
public final class RpgNetwork {
    private static final String PROTOCOL = "forge-2.5-2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new Identifier("rpgstats", "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final Map<UUID, Long> WINDOWS = new HashMap<>();
    private static final Map<UUID, Integer> COUNTS = new HashMap<>();

    public record Action(Identifier action, byte[] payload) {}
    public record Sync(NbtCompound data) {}
    public record Open() {}
    public record OpenGuide() {}
    public record RollVisual(UUID player, Identifier dimension, net.minecraft.util.math.Vec3d motion) {}

    public static void initialize() {
        CHANNEL.registerMessage(4, RollVisual.class, (p,b) -> {
            b.writeUuid(p.player()); b.writeIdentifier(p.dimension());
            b.writeDouble(p.motion().x); b.writeDouble(p.motion().y); b.writeDouble(p.motion().z);
        }, b -> new RollVisual(b.readUuid(), b.readIdentifier(), new net.minecraft.util.math.Vec3d(b.readDouble(),b.readDouble(),b.readDouble())),
        (p,c) -> { var ctx=c.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.rpgstats.compat.combatroll.CombatRollVisuals.receive(p))); ctx.setPacketHandled(true); },
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(0, Action.class, (p,b) -> {
            b.writeIdentifier(p.action()); b.writeByteArray(p.payload());
        }, b -> new Action(b.readIdentifier(), b.readByteArray(512)), RpgNetwork::handleAction,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Sync.class, (p,b) -> b.writeNbt(p.data()),
                b -> new Sync(b.readNbt()), RpgNetwork::handleSync, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, Open.class, (p,b) -> {}, b -> new Open(),
                RpgNetwork::handleOpen, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, OpenGuide.class, (p,b) -> {}, b -> new OpenGuide(),
                RpgNetwork::handleOpenGuide, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Takes ownership of the small, temporary buffer created by our client screens. */
    public static void sendToServer(Identifier action, PacketByteBuf buffer) {
        try {
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            if (bytes.length <= 512) CHANNEL.sendToServer(new Action(action, bytes));
        } finally { buffer.release(); }
    }

    public static void rollVisual(ServerPlayerEntity player, net.minecraft.util.math.Vec3d motion) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new RollVisual(player.getUuid(), player.getWorld().getRegistryKey().getValue(), motion));
    }
    public static void sync(ServerPlayerEntity player, NbtCompound data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Sync(data.copy()));
    }
    public static void open(ServerPlayerEntity player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Open());
    }
    public static void openGuide(ServerPlayerEntity player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenGuide());
    }
    public static void forget(UUID id) { WINDOWS.remove(id); COUNTS.remove(id); }
    public static void clear() { WINDOWS.clear(); COUNTS.clear(); }

    private static boolean allow(ServerPlayerEntity player) {
        long tick = player.getServerWorld().getTime();
        UUID id = player.getUuid();
        long previous = WINDOWS.getOrDefault(id, Long.MIN_VALUE);
        if (previous == Long.MIN_VALUE || tick < previous || tick - previous >= 20) {
            WINDOWS.put(id, tick); COUNTS.put(id, 0);
        }
        int count = COUNTS.getOrDefault(id, 0);
        if (count >= 40) return false;
        COUNTS.put(id, count + 1);
        return true;
    }

    private static void handleAction(Action message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = ctx.getSender();
            if (player == null || !player.isAlive() || player.isSpectator()
                    || !message.action().getNamespace().equals(RPGStatsMod.MOD_ID) || !allow(player)) return;
            PacketByteBuf b = new PacketByteBuf(Unpooled.wrappedBuffer(message.payload()));
            try {
                // Decode the whole request BEFORE running a mutation. Reject trailing/malformed data.
                Runnable action = switch (message.action().getPath()) {
                    case "allocate" -> { String id=b.readString(32); yield () -> StatsManager.allocate(player,id); }
                    case "select_class" -> { String id=b.readString(32); yield () -> StatsManager.selectClass(player,id); }
                    case "select_subclass" -> { String id=b.readString(48); yield () -> StatsManager.selectSubclass(player,id); }
                    case "select_path" -> { String id=b.readString(64); yield () -> StatsManager.selectPath(player,id); }
                    case "select_specialization" -> { String id=b.readString(64); yield () -> StatsManager.selectSpecialization(player,id); }
                    case "select_affinity" -> { String id=b.readString(64); yield () -> StatsManager.selectAffinity(player,id); }
                    case "awaken" -> { b.readString(0); yield () -> StatsManager.awaken(player); }
                    case "unlock_node" -> { String id=b.readString(96); yield () -> StatsManager.unlockNode(player,id); }
                    case "select_active" -> {
                        int slot=b.readVarInt(); String id=b.readString(96);
                        yield () -> StatsManager.selectActiveAbility(player,slot,id);
                    }
                    case "activate_ability" -> { int slot=b.readVarInt(); yield () -> CombatHandler.activateAbility(player,slot); }
                    default -> null;
                };
                if (action != null && b.readableBytes() == 0) action.run();
            } catch (IllegalArgumentException | IndexOutOfBoundsException | io.netty.handler.codec.DecoderException ignored) {
                // Client input is untrusted. No partial mutation before successful decode.
            } finally { b.release(); }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleSync(Sync packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx=context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.rpgstats.RPGStatsClient.receiveSync(packet.data())));
        ctx.setPacketHandled(true);
    }
    private static void handleOpen(Open packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx=context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.rpgstats.RPGStatsClient.openScreen()));
        ctx.setPacketHandled(true);
    }
    private static void handleOpenGuide(OpenGuide packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx=context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.rpgstats.RPGStatsClient.openGuideScreen()));
        ctx.setPacketHandled(true);
    }
    private RpgNetwork() {}
}

