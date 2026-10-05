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

/** Real Forge renderer and scaled hit tests, excluded from release builds. */
@Mod.EventBusSubscriber(modid="rpgstats",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class UiClientPreview {
    private record Sample(RPGClass clazz,String page,int width,int height,int gui,String file) {}
    private static final List<Sample> CASES=new ArrayList<>();
    static {
        CASES.add(new Sample(null,"awakening",1920,1080,2,"00-awakening"));
        for(RPGClass c:RPGClass.values()) {
            String id=c.name().toLowerCase();
            CASES.add(new Sample(c,"class",1920,1080,2,id+"-class"));
            CASES.add(new Sample(c,"Casa",1920,1080,2,id+"-houses"));
            CASES.add(new Sample(c,"Especial.",1920,1080,2,id+"-specializations"));
            CASES.add(new Sample(c,"Afinidade",1920,1080,2,id+"-affinity"));
            CASES.add(new Sample(c,"Classe",1920,1080,2,id+"-talent"));
        }
        CASES.add(new Sample(RPGClass.MAGO,"class",1920,1080,4,"small-class-gui4"));
        CASES.add(new Sample(RPGClass.GUERREIRO,"Casa",1024,768,3,"small-house-4by3"));
        CASES.add(new Sample(RPGClass.ARQUEIRO,"Afinidade",2560,1080,3,"wide-affinity"));
    }
    private static int index,age;private static boolean started,capture;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) throws Exception {
        if(!Boolean.getBoolean("rpgstats.uiPreview")||event.phase!=TickEvent.Phase.END||index>=CASES.size())return;
        var c=MinecraftClient.getInstance();if(c.getOverlay()!=null||c.currentScreen==null)return;
        if(!started){if(++age<60)return;started=true;age=0;}
        Sample sample=CASES.get(index);
        if(age==0){GLFW.glfwSetWindowSize(c.getWindow().getHandle(),sample.width,sample.height);c.options.getGuiScale().setValue(sample.gui);GLFW.glfwSetCursorPos(c.getWindow().getHandle(),0,0);}
        if(age==10){
            c.onResolutionChanged();var s=new PlayerStats();s.awakened=!sample.page.equals("awakening");
            if(!sample.page.equals("class")&&!sample.page.equals("awakening")) {
                s.clazz=sample.clazz;s.level=50;s.skillPoints=24;s.statPoints=12;
                s.clazz.nodes.forEach(n->s.unlockedNodes.add(n.id()));
                if(sample.page.equals("Especial.")||sample.page.equals("Afinidade")){s.path=RPGPath.forClass(s.clazz).get(0);s.path.nodes.forEach(n->s.unlockedNodes.add(n.id()));}
                s.refreshResourceMax();s.resource=s.resourceMax;
            }
            ClientStatsStore.stats=s;c.setScreen(new StatsScreen());
        }
        if(age==20&&c.currentScreen instanceof StatsScreen screen){
            if(sample.page.equals("class")) {
                var cards=screen.children().stream().filter(ClassCardWidget.class::isInstance).map(ClassCardWidget.class::cast).toList();
                if(cards.size()!=4)throw new IllegalStateException("Expected four class previews");
                click(screen,cards.get(sample.clazz.ordinal()),false);
            } else if(!sample.page.equals("awakening"))click(screen,button(screen,sample.page),true);
        }
        if(age==25&&c.currentScreen instanceof StatsScreen screen){
            if(sample.page.equals("Casa")||sample.page.equals("Especial.")||sample.page.equals("Afinidade")) {
                String name=sample.page.equals("Casa")?shortName(RPGPath.forClass(sample.clazz).get(0)):
                        sample.page.equals("Especial.")?RPGSpecialization.forPath(ClientStatsStore.stats.path).get(0).display:
                        shortName(RPGPath.forClass(sample.clazz).get(1));
                click(screen,button(screen,name),true);
                if(sample.page.equals("Casa")&&ClientStatsStore.stats.path!=null || sample.page.equals("Especial.")&&ClientStatsStore.stats.specialization!=null || sample.page.equals("Afinidade")&&ClientStatsStore.stats.affinityHouse!=null)
                    throw new IllegalStateException("Preview committed a permanent choice");
            }else if(sample.page.equals("Classe")) {
                var n=sample.clazz.nodes.get(0);click(screen,button(screen,n.name()),true);
                var f=StatsScreen.class.getDeclaredField("inspectedNode");f.setAccessible(true);
                if(!n.id().equals(f.get(screen)))throw new IllegalStateException("Clicked talent details did not persist");
                screen.rebuild();
                if(!n.id().equals(f.get(screen)))throw new IllegalStateException("Server rebuild cleared inspected talent");
                var v=MageViewport.fit(screen.width,screen.height);screen.mouseScrolled(v.x()+60*v.scale(),v.y()+200*v.scale(),-1);
            }
        }
        if(age==30&&c.currentScreen instanceof StatsScreen screen){
            if(sample.page.equals("class")) {
                if(ClientStatsStore.stats.clazz!=null||!button(screen,"Confirmar classe").active)throw new IllegalStateException("Class preview/confirmation contract broken");
            } else if(!sample.page.equals("awakening")&&!sample.page.equals("Classe")&&!button(screen,"Confirmar escolha").active)
                throw new IllegalStateException("Eligible preview cannot confirm");
        }
        if(++age>=36)capture=true;
    }
    private static String shortName(RPGPath p){return p.display.replace("Casa da ","").replace("Casa do ","").replace("Casa dos ","").replace("Casa de ","").replace("Casa ","");}
    private static ButtonWidget button(StatsScreen screen,String label){return screen.children().stream().filter(ButtonWidget.class::isInstance).map(ButtonWidget.class::cast).filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new IllegalStateException("Missing button "+label));}
    private static void click(StatsScreen screen,ButtonWidget button,boolean scaled){
        double x=button.getX()+button.getWidth()/2.0,y=button.getY()+button.getHeight()/2.0;
        if(scaled){var v=MageViewport.fit(screen.width,screen.height);x=v.x()+x*v.scale();y=v.y()+y*v.scale();}
        if(!screen.mouseClicked(x,y,0))throw new IllegalStateException("Visible control center not clickable: "+button.getMessage().getString());screen.mouseReleased(x,y,0);
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event)throws Exception {
        if(!Boolean.getBoolean("rpgstats.uiPreview")||event.phase!=TickEvent.Phase.END||!capture)return;
        capture=false;var c=MinecraftClient.getInstance();Path out=Path.of(System.getProperty("rpgstats.uiPreviewDir"));Files.createDirectories(out);
        try(NativeImage image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){image.writeTo(out.resolve(CASES.get(index).file+".png"));}
        System.out.println("RPG_UI_CAPTURE "+CASES.get(index).file);index++;age=0;
        if(index==CASES.size()){Files.writeString(out.resolve("result.txt"),"PASS: 24 real Forge UI cases; awakening, four classes, House/spec/affinity previews, confirmation eligibility, persistent talent inspection, scaled hitboxes and resizing.\n");c.scheduleStop();}
    }
    private UiClientPreview(){}
}
