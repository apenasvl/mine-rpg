package com.rpgstats.preview;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.*;
import com.rpgstats.combat.CombatState;
import com.rpgstats.stats.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
/** Real integrated client/server probe, tool-only. World is prepared by GameTest CI first. */
@Mod.EventBusSubscriber(modid="rpgstats", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatClientProbe {
    private static int tick, phase, age;
    private static volatile boolean configured, verified;
    private static volatile Throwable failure;
    private static boolean animation;
    private static final InputUtil.Key R=InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_R);
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (!Boolean.getBoolean("rpgstats.combatProbe") || event.phase != TickEvent.Phase.END) return;
        if (++tick>2400) throw new IllegalStateException("Client probe timeout at phase "+phase);
        if (failure!=null) throw new IllegalStateException("Server probe failed",failure);
        var c=MinecraftClient.getInstance();
        if (phase==0 && c.getOverlay()==null && c.currentScreen!=null && tick>80) {
            phase=1;c.createIntegratedServerLoader().start(c.currentScreen,"CombatProbe");return;
        }
        if (c.player==null || c.world==null || c.getServer()==null) return;
        if (phase==1) {
            phase=2;
            c.getServer().execute(() -> {
                try {
                    var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());var w=p.getServerWorld();
                    for(int x=-4;x<=4;x++) for(int z=-5;z<=5;z++) {
                        w.setBlockState(new BlockPos(x,100,z),Blocks.STONE.getDefaultState());
                        for(int y=101;y<=104;y++) w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                    }
                    for(int x=-4;x<=4;x++) for(int y=101;y<=103;y++) w.setBlockState(new BlockPos(x,y,-2),Blocks.STONE.getDefaultState());
                    p.teleport(w,.5,101,.5,0,0);
                    p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                    PlayerStats s=new PlayerStats();s.clazz=RPGClass.ARQUEIRO;s.specialization=RPGSpecialization.SURVIVALIST;s.path=s.specialization.parent;
                    s.level=StatsManager.MAX_LEVEL;s.awakened=true;s.unlockedNodes.add(s.specialization.nodes.get(0).id());s.unlockedNodes.add("arc_ward_surv_technique");
                    s.refreshResourceMax();s.resource=s.resourceMax;s.setActiveSlot(0,"arc_ward_surv_technique");
                    CombatState.remove(p.getUuid());StatsManager.saveAndSync(p,s);configured=true;
                } catch(Throwable e){failure=e;}
            });return;
        }
        if (phase==2 && configured && ++age>50) {
            c.setScreen(null);
            c.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT);
            Object manager=Class.forName("net.combatroll.internals.RollingEntity").getMethod("getRollManager").invoke(c.player);
            if ((boolean)manager.getClass().getMethod("isRollAvailable",net.minecraft.entity.player.PlayerEntity.class).invoke(manager,c.player)) throw new IllegalStateException("Native free roll allowed");
            if (!net.minecraftforge.fml.ModList.get().isLoaded("presencefootsteps")) throw new IllegalStateException("Forge footsteps missing");
            KeyBinding.setKeyPressed(R,true);KeyBinding.onKeyPressed(R);phase=3;age=0;return;
        }
        if (phase==3) {
            if (++age==1) KeyBinding.setKeyPressed(R,false);
            // PlayerAnimator's real layer must become active after the server-authorized packet.
            for(Class<?> type=c.player.getClass();type!=null;type=type.getSuperclass()) for(var f:type.getDeclaredFields()) {
                if (f.getName().equals("base") && f.getType().getName().contains("ModifierLayer")) {
                    f.setAccessible(true);Object layer=f.get(c.player);
                    if ((boolean)layer.getClass().getMethod("isActive").invoke(layer)) animation=true;
                }
            }
            if (animation && age<20) {
                Path out=Path.of(System.getProperty("rpgstats.combatProbeDir"));Files.createDirectories(out);
                try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {image.writeTo(out.resolve("roll.png"));}
            }
            if (age==40) {
                if(!animation) throw new IllegalStateException("Native roll animation never became active");
                phase=4;c.getServer().execute(() -> {
                    try {
                        var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());var s=StatsManager.get(p);
                        if(p.getZ()>=.4 || p.getZ()<-1.71) throw new IllegalStateException("Roll failed movement or crossed wall: "+p.getPos());
                        if(CombatState.get(p.getUuid()).cooldown("arc_ward_surv_technique")<=0) throw new IllegalStateException("Roll cooldown missing");
                        // Recharge ticks can restore small amounts; a second 20-resource spend must never occur.
                        if(s.resource<s.resourceMax-25 || s.resource>s.resourceMax-10) throw new IllegalStateException("Unexpected roll resource: "+s.resource+"/"+s.resourceMax);
                        verified=true;
                    }catch(Throwable e){failure=e;}
                });
            }
        }
        if(phase==4 && verified) {
            phase=5;age=0;configured=false;
            c.getServer().execute(() -> {
                try {
                    var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());
                    var s=new PlayerStats();s.clazz=RPGClass.ARQUEIRO;s.path=RPGPath.ARC_ARCANE;s.specialization=RPGSpecialization.FLAMEBOW;
                    s.awakened=true;s.level=StatsManager.MAX_LEVEL;
                    s.path.nodes.forEach(n->s.unlockedNodes.add(n.id()));s.specialization.nodes.forEach(n->s.unlockedNodes.add(n.id()));
                    s.setActiveSlot(0,"arc_magic_technique");s.setActiveSlot(1,"arc_magic_signature");
                    s.setActiveSlot(2,"arc_magic_fire_technique");s.setActiveSlot(3,"arc_magic_fire_signature");
                    s.refreshResourceMax();s.resource=s.resourceMax;
                    CombatState.remove(p.getUuid());CombatState.get(p.getUuid()).classMode=2;
                    StatsManager.saveAndSync(p,s);configured=true;
                }catch(Throwable e){failure=e;}
            });
        }
        if(phase==5 && configured && ++age>40) {
            if(com.rpgstats.ClientStatsStore.stats.path!=RPGPath.ARC_ARCANE || com.rpgstats.ClientStatsStore.archerAffinity!=2)
                throw new IllegalStateException("Arcane HUD failed authoritative affinity sync");
            Path out=Path.of(System.getProperty("rpgstats.combatProbeDir"));
            try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {image.writeTo(out.resolve("arcane-hud-names.png"));}
            KeyBinding.setKeyPressed(InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT),true);
            phase=6;age=0;
        }
        if(phase==6 && ++age==10) {
            if(!com.rpgstats.RPGStatsClient.abilityDetailsHeld())throw new IllegalStateException("Rebindable HUD details key was not held");
            Path out=Path.of(System.getProperty("rpgstats.combatProbeDir"));
            try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {image.writeTo(out.resolve("arcane-hud-details.png"));}
            KeyBinding.setKeyPressed(R,true);KeyBinding.onKeyPressed(R);
            phase=7;age=0;
        }
        if(phase==7) {
            if(++age==1)KeyBinding.setKeyPressed(R,false);
            if(age==30) {
                if(com.rpgstats.ClientStatsStore.archerAffinity!=0 || com.rpgstats.ClientStatsStore.cooldown("arc_magic_technique")<=0)
                    throw new IllegalStateException("Arcane activation did not update HUD affinity/cooldown");
                Path out=Path.of(System.getProperty("rpgstats.combatProbeDir"));
                try(var image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {image.writeTo(out.resolve("arcane-hud-after-switch.png"));}
                KeyBinding.setKeyPressed(InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT),false);
                Files.writeString(out.resolve("result.txt"),"PASS: real Forge combined client; authorized Combat Roll, footsteps; Arcane HUD distinct names, held details key, authoritative affinity and real R activation/cooldown sync.\n");
                System.out.println("RPG_COMBAT_CLIENT_PASS");phase=8;c.scheduleStop();
            }
        }
    }
}
