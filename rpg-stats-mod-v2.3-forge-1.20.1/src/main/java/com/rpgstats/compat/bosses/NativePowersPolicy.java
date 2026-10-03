package com.rpgstats.compat.bosses;
import com.rpgstats.RPGStatsMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
/** Native registration/common setup must finish before replacing its runtime item powers. */
@Mod.EventBusSubscriber(modid=RPGStatsMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class NativePowersPolicy {
    private static volatile boolean ready;
    @SubscribeEvent public static void loaded(FMLLoadCompleteEvent event){event.enqueueWork(()->ready=true);}
    public static boolean replacesNativeAbilities(){return ready;}
    private NativePowersPolicy(){}
}
