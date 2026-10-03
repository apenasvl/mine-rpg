package com.rpgstats.preview;

import com.rpgstats.ClientStatsStore;
import com.rpgstats.gui.ClassCardWidget;
import com.rpgstats.gui.StatsScreen;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tool-only source, added exclusively by ui-preview.init.gradle. No server/world needed. */
@Mod.EventBusSubscriber(modid = "rpgstats", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UiClientPreview {
    private record Case(int width, int height, int gui, int selected, String file) {}
    private static final Case[] CASES = {
            new Case(1920, 1080, 2, -1, "01-1080p-gui2"),
            new Case(1920, 1080, 4, 1, "02-1080p-gui4-mage"),
            new Case(1280, 720, 3, 0, "03-720p-gui3-warrior"),
            new Case(1024, 768, 3, 3, "04-4by3-assassin"),
            new Case(2560, 1080, 3, 2, "05-wide-archer-houses")
    };
    private static int index;
    private static int age;
    private static boolean started;
    private static boolean capture;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("rpgstats.uiPreview") || event.phase != TickEvent.Phase.END || index >= CASES.length) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getOverlay() != null || client.currentScreen == null) return;
        if (!started) {
            if (++age < 60) return;
            started = true;
            age = 0;
        }
        Case sample = CASES[index];
        if (age == 0) {
            GLFW.glfwSetWindowSize(client.getWindow().getHandle(), sample.width, sample.height);
            GLFW.glfwSetCursorPos(client.getWindow().getHandle(), 0, 0);
            client.options.getGuiScale().setValue(sample.gui);
        }
        if (age == 10) {
            client.onResolutionChanged();
            ClientStatsStore.stats = new PlayerStats();
            ClientStatsStore.stats.awakened = true;
            client.setScreen(new StatsScreen());
        }
        if (age == 20 && client.currentScreen instanceof StatsScreen screen) {
            var cards = screen.children().stream().filter(ClassCardWidget.class::isInstance)
                    .map(ClassCardWidget.class::cast).toList();
            if (cards.size() != 4) throw new IllegalStateException("Expected exactly four class cards");
            if (sample.selected >= 0) {
                var card = cards.get(sample.selected);
                // Exercise the actual screen hit test, not direct invocation of the card callback.
                double x = card.getX() + card.getWidth() / 2.0;
                double y = card.getY() + card.getHeight() / 2.0;
                if (!screen.mouseClicked(x, y, 0)) throw new IllegalStateException("Visible card center is not clickable");
                screen.mouseReleased(x, y, 0);
            }
            if (ClientStatsStore.stats.clazz != null) throw new IllegalStateException("Preview committed a class without confirmation");
        }
        if (age == 30 && client.currentScreen instanceof StatsScreen screen) {
            var confirm = screen.children().stream().filter(ButtonWidget.class::isInstance)
                    .map(ButtonWidget.class::cast).filter(b -> b.getMessage().getString().equals("Confirmar classe"))
                    .findFirst().orElseThrow();
            if (confirm.active != (sample.selected >= 0)) throw new IllegalStateException("Incorrect confirmation enabled state");
            if (index == 4) {
                var card = screen.children().stream().filter(ClassCardWidget.class::isInstance)
                        .map(ClassCardWidget.class::cast).skip(2).findFirst().orElseThrow();
                double x = card.getX() + card.getWidth() * 382.0 / 703;
                double y = card.getY() + card.getHeight() * 207.0 / 320;
                if (card.houseTooltip(x, y).isEmpty()) throw new IllegalStateException("House diamond has no live tooltip");
                double scale = client.getWindow().getScaleFactor();
                GLFW.glfwSetCursorPos(client.getWindow().getHandle(), x * scale, y * scale);
            }
        }
        if (++age >= 45) capture = true;
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!Boolean.getBoolean("rpgstats.uiPreview") || event.phase != TickEvent.Phase.END || !capture) return;
        capture = false;
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Path directory = Path.of(System.getProperty("rpgstats.uiPreviewDir"));
            Files.createDirectories(directory);
            System.out.println("RPG_UI_OUTPUT " + directory.toAbsolutePath());
            try (NativeImage image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                image.writeTo(directory.resolve(CASES[index].file + ".png"));
            }
            System.out.println("RPG_UI_CAPTURE " + CASES[index].file + " " + client.getWindow().getWidth()
                    + "x" + client.getWindow().getHeight() + " gui=" + client.getWindow().getScaleFactor());
        } catch (Exception error) {
            throw new IllegalStateException("Failed to capture class-selection UI", error);
        }
        index++;
        age = 0;
        if (index == CASES.length) client.scheduleStop();
    }

    private UiClientPreview() {}
}
