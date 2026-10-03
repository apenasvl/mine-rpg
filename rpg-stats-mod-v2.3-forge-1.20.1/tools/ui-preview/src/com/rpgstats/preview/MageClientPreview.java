package com.rpgstats.preview;

import com.rpgstats.ClientStatsStore;
import com.rpgstats.classes.*;
import com.rpgstats.gui.*;
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
import java.nio.file.*;
import java.util.*;

/** Real-client visual/hit-test coverage for every mage House and specialization. */
@Mod.EventBusSubscriber(modid="rpgstats", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class MageClientPreview {
    private record Sample(RPGPath house,RPGSpecialization spec,int width,int height,int gui,String file) {}
    private static final List<Sample> SAMPLES=new ArrayList<>();
    static {
        for(RPGPath house:RPGPath.forClass(RPGClass.MAGO)) {
            SAMPLES.add(new Sample(house,null,1920,1080,3,house.name().toLowerCase()));
            for(RPGSpecialization spec:RPGSpecialization.values()) if(spec.parent==house)
                SAMPLES.add(new Sample(house,spec,1920,1080,3,spec.name().toLowerCase()));
        }
        SAMPLES.add(new Sample(RPGPath.MAGE_TEMPORAL,RPGSpecialization.REVERSER,1280,720,3,"small-gui3"));
        SAMPLES.add(new Sample(RPGPath.MAGE_ELEMENTAL,RPGSpecialization.CRYOMANCER,1024,768,3,"4by3"));
        SAMPLES.add(new Sample(RPGPath.MAGE_OCCULT,RPGSpecialization.HEXBLADE,2560,1080,4,"wide-gui4"));
    }
    private static int index,age;
    private static boolean started,capture;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("rpgstats.magePreview") || event.phase!=TickEvent.Phase.END || index>=SAMPLES.size()) return;
        var client=MinecraftClient.getInstance();
        if(client.getOverlay()!=null || client.currentScreen==null) return;
        if(!started) { if(++age<60) return; started=true; age=0; }
        Sample sample=SAMPLES.get(index);
        if(age==0) {
            GLFW.glfwSetWindowSize(client.getWindow().getHandle(),sample.width,sample.height);
            client.options.getGuiScale().setValue(sample.gui);
            GLFW.glfwSetCursorPos(client.getWindow().getHandle(),0,0);
        }
        if(age==10) {
            client.onResolutionChanged();
            var stats=new PlayerStats();
            stats.awakened=true; stats.clazz=RPGClass.MAGO; stats.path=sample.house; stats.specialization=sample.spec;
            stats.level=50; stats.skillPoints=24; stats.statPoints=12; stats.resource=83; stats.resourceMax=123;
            ClientStatsStore.stats=stats;
            client.setScreen(new StatsScreen());
        }
        if(age==20 && client.currentScreen instanceof StatsScreen screen) {
            String label=sample.spec==null?"Casa":"Especial.";
            var tab=screen.children().stream().filter(ButtonWidget.class::isInstance).map(ButtonWidget.class::cast)
                    .filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow();
            var viewport=MageViewport.fit(screen.width,screen.height);
            double x=viewport.x()+(tab.getX()+tab.getWidth()/2.0)*viewport.scale();
            double y=viewport.y()+(tab.getY()+tab.getHeight()/2.0)*viewport.scale();
            if(!screen.mouseClicked(x,y,0)) throw new IllegalStateException("Scaled tab center not clickable");
            screen.mouseReleased(x,y,0);
        }
        if(age==30 && client.currentScreen instanceof StatsScreen screen) {
            var expected=sample.spec==null?sample.house.nodes:sample.spec.nodes;
            for(var node:expected) if(screen.children().stream().filter(ButtonWidget.class::isInstance).map(ButtonWidget.class::cast)
                    .noneMatch(b->b.getMessage().getString().equals(node.name()))) throw new IllegalStateException("Missing real tree node "+node.id());
            if(ClientStatsStore.stats.path!=sample.house || ClientStatsStore.stats.specialization!=sample.spec)
                throw new IllegalStateException("Viewing UI modified character build");
        }
        if(++age>=45) capture=true;
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if(!Boolean.getBoolean("rpgstats.magePreview") || event.phase!=TickEvent.Phase.END || !capture) return;
        capture=false;
        var client=MinecraftClient.getInstance();
        Path directory=Path.of(System.getProperty("rpgstats.magePreviewDir"));
        try {
            Files.createDirectories(directory);
            try(NativeImage image=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) { image.writeTo(directory.resolve(SAMPLES.get(index).file+".png")); }
            System.out.println("RPG_MAGE_CAPTURE "+SAMPLES.get(index).file);
        } catch(Exception error) { throw new IllegalStateException(error); }
        index++; age=0;
        if(index==SAMPLES.size()) client.scheduleStop();
    }
}
