package com.rpgstats.gametest;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Cleanup is explicit: succeedWhen/addInstantFinalTask would finish tests before delayed assertions. */
public final class TestPlayers {
    private static final AtomicInteger TEST_PLAYER_IDS = new AtomicInteger();
    private static final java.util.Map<TestContext, java.util.List<Entry>> PLAYERS = new java.util.HashMap<>();
    private record Entry(ServerPlayerEntity player, EmbeddedChannel channel) {}
    public static ServerPlayerEntity create(TestContext context) {
        var world = context.getWorld();
        var server = world.getServer();
        String name = "rpgtest" + TEST_PLAYER_IDS.incrementAndGet();
        ServerPlayerEntity player = new ServerPlayerEntity(
                server, world, new GameProfile(UUID.randomUUID(), name));

        // Forge's network hooks expect a real Netty pipeline during PlayerManager login.
        // Vanilla GameTest's mock connection has no backing channel, so create an in-memory one.
        ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND) {
            @Override
            public boolean isLocal() {
                return true;
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getPlayerManager().onPlayerConnect(connection, player);

        BlockPos pos = context.getAbsolutePos(new BlockPos(1, 1, 1));
        player.refreshPositionAndAngles(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        player.changeGameMode(GameMode.SURVIVAL);
        context.assertTrue(player.interactionManager.getGameMode() == GameMode.SURVIVAL,
                "Connected GameTest player is not in survival mode");

        PLAYERS.computeIfAbsent(context, ignored -> new java.util.ArrayList<>()).add(new Entry(player, channel));
        return player;
    }

    public static void finish(TestContext context) {
        java.util.List<Entry> entries = PLAYERS.remove(context);
        if (entries == null) return;
        for (Entry entry : entries) {
            var player = entry.player;
            if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) != null)
                player.getServer().getPlayerManager().remove(player);
            entry.channel.finishAndReleaseAll();
        }
    }
}
