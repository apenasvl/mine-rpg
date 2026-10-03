package com.rpgstats;

import com.rpgstats.gui.ResourceHud;
import com.rpgstats.gui.GuideScreen;
import com.rpgstats.gui.StatsScreen;
import com.rpgstats.network.RpgNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Physical-client-only subscriptions; dedicated servers never register this class. */
@Mod.EventBusSubscriber(modid=RPGStatsMod.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class RPGStatsClient {
    private static final KeyBinding DETAILS=bind("key.rpgstats.ability_details",GLFW.GLFW_KEY_LEFT_ALT);
    public static boolean abilityDetailsHeld() { return DETAILS.isPressed(); }
    public static String abilityDetailsKey() { return DETAILS.getBoundKeyLocalizedText().getString(); }
    private static final KeyBinding OPEN=bind("key.rpgstats.open",GLFW.GLFW_KEY_K);
    private static final KeyBinding[] ABILITIES={
        bind("key.rpgstats.ability1",GLFW.GLFW_KEY_R),
        bind("key.rpgstats.ability2",GLFW.GLFW_KEY_Z),
        bind("key.rpgstats.ability3",GLFW.GLFW_KEY_X),
        bind("key.rpgstats.ability4",GLFW.GLFW_KEY_C)
    };
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
        event.register(DETAILS);
        for (KeyBinding key:ABILITIES) event.register(key);
    }
    @SubscribeEvent public static void hud(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("resources",(gui,context,partialTick,width,height) ->
                ResourceHud.render(context,partialTick));
    }
    private static KeyBinding bind(String key,int code) {
        return new KeyBinding(key,InputUtil.Type.KEYSYM,code,"category.rpgstats");
    }
    public static void receiveSync(NbtCompound data) { ClientStatsStore.update(data); }
    public static void openScreen() {
        MinecraftClient client=MinecraftClient.getInstance();
        if(client.player!=null) client.setScreen(new StatsScreen());
    }
    public static void openGuideScreen() {
        MinecraftClient client=MinecraftClient.getInstance();
        if(client.player!=null) client.setScreen(new GuideScreen());
    }
    @Mod.EventBusSubscriber(modid=RPGStatsMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
    public static final class GameEvents {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if(event.phase!=TickEvent.Phase.END)return;
            MinecraftClient client=MinecraftClient.getInstance();
            if(client.player==null || client.getNetworkHandler()==null)return;
            ClientStatsStore.tick();
            if(client.currentScreen!=null) {
                while(OPEN.wasPressed()) {}
                for(KeyBinding key:ABILITIES)while(key.wasPressed()) {}
                return;
            }
            while(OPEN.wasPressed())openScreen();
            for(int slot=0;slot<ABILITIES.length;slot++)while(ABILITIES[slot].wasPressed()) {
                PacketByteBuf buffer=new PacketByteBuf(Unpooled.buffer());
                buffer.writeVarInt(slot); RpgNetwork.sendToServer(RPGStatsMod.ACTIVATE,buffer);
            }
        }
        @SubscribeEvent public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientStatsStore.clear();
        }
    }
    private RPGStatsClient() {}
}

