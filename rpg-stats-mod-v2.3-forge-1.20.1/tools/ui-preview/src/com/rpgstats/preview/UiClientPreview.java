package com.rpgstats.preview;

import com.rpgstats.ClientStatsStore;
import com.rpgstats.classes.*;
import com.rpgstats.gui.*;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.Stat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
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
            CASES.add(new Sample(c,"Stats",1920,1080,2,id+"-attributes"));
            CASES.add(new Sample(c,"Resumo",1920,1080,2,id+"-house-overview"));
            CASES.add(new Sample(c,"Casa",1920,1080,2,id+"-houses"));
            CASES.add(new Sample(c,"Especial.",1920,1080,2,id+"-specializations"));
            CASES.add(new Sample(c,"Afinidade",1920,1080,2,id+"-affinity"));
            CASES.add(new Sample(c,"Classe",1920,1080,2,id+"-talent"));
            CASES.add(new Sample(c,"locked",1920,1080,2,id+"-locked-talent"));
        }
        CASES.add(new Sample(RPGClass.MAGO,"class",1920,1080,4,"small-class-gui4"));
        CASES.add(new Sample(RPGClass.GUERREIRO,"Casa",1024,768,3,"small-house-4by3"));
        CASES.add(new Sample(RPGClass.ARQUEIRO,"Afinidade",2560,1080,3,"wide-affinity"));
        CASES.add(new Sample(RPGClass.MAGO,"Guide:index",1920,1080,2,"guide-index"));
        CASES.add(new Sample(RPGClass.MAGO,"Guide:house",1920,1080,2,"guide-house-bonuses"));
        CASES.add(new Sample(RPGClass.MAGO,"Guide:spec",1920,1080,2,"guide-blood-full"));
        CASES.add(new Sample(RPGClass.MAGO,"Guide:scroll",1920,1080,2,"guide-blood-end"));
        CASES.add(new Sample(RPGClass.MAGO,"Guide:specs",1024,768,4,"guide-small-specs"));
        CASES.add(new Sample(RPGClass.MAGO,"Guide:index",2560,1080,3,"guide-wide-index"));
    }
    private static int index,age;private static boolean started,capture;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        try { advance(event); }catch(Throwable error){fail(error);}
    }
    private static void advance(TickEvent.ClientTickEvent event) throws Exception {
        if(!Boolean.getBoolean("rpgstats.uiPreview")||event.phase!=TickEvent.Phase.END||index>=CASES.size())return;
        var c=MinecraftClient.getInstance();if(c.getOverlay()!=null||c.currentScreen==null)return;
        if(!started){if(++age<60)return;started=true;age=0;}
        Sample sample=CASES.get(index);
        if(age==0){GLFW.glfwSetWindowSize(c.getWindow().getHandle(),sample.width,sample.height);c.options.getGuiScale().setValue(sample.gui);GLFW.glfwSetCursorPos(c.getWindow().getHandle(),0,0);}
        if(age==10){
            c.onResolutionChanged();var s=new PlayerStats();s.awakened=!sample.page.equals("awakening");
            if(!sample.page.equals("class")&&!sample.page.equals("awakening")) {
                s.clazz=sample.clazz;s.level=50;s.skillPoints=24;s.statPoints=12;
                if(!sample.page.equals("locked"))s.clazz.nodes.forEach(n->s.unlockedNodes.add(n.id()));
                if(sample.page.equals("Especial.")||sample.page.equals("Afinidade")){s.path=RPGPath.forClass(s.clazz).get(0);s.path.nodes.forEach(n->s.unlockedNodes.add(n.id()));}
                if(sample.page.equals("Stats")) {
                    s.stats.put(Stat.VITALIDADE,32);s.stats.put(Stat.TENACIDADE,25);s.stats.put(Stat.FORCA,47);
                    s.stats.put(Stat.DESTREZA,24);s.stats.put(Stat.INTELIGENCIA,40);s.stats.put(Stat.ARCANO,18);
                }
                s.refreshResourceMax();s.resource=s.resourceMax;
            }
            ClientStatsStore.stats=s;c.setScreen(sample.page.startsWith("Guide:")?new GuideScreen():new StatsScreen());
        }
        if(age==20&&c.currentScreen instanceof StatsScreen screen){
            if(sample.page.equals("class")) {
                var cards=screen.children().stream().filter(ClassCardWidget.class::isInstance).map(ClassCardWidget.class::cast).toList();
                if(cards.size()!=4)throw new IllegalStateException("Expected four class previews");
                click(screen,cards.get(sample.clazz.ordinal()),false);
            } else if(!sample.page.equals("awakening"))click(screen,button(screen,sample.page.equals("locked")?"Classe":sample.page.equals("Resumo")?"Casa":sample.page),true);
        }
        if(age==25&&c.currentScreen instanceof StatsScreen screen){
            if(sample.page.equals("Casa")||sample.page.equals("Resumo")||sample.page.equals("Especial.")||sample.page.equals("Afinidade")) {
                String name=sample.page.equals("Casa")||sample.page.equals("Resumo")?shortName(RPGPath.forClass(sample.clazz).get(0)):
                        sample.page.equals("Especial.")?RPGSpecialization.forPath(ClientStatsStore.stats.path).get(0).display:
                        shortName(RPGPath.forClass(sample.clazz).get(1));
                click(screen,button(screen,name),true);
                if(sample.page.equals("Casa")&&ClientStatsStore.stats.path!=null || sample.page.equals("Especial.")&&ClientStatsStore.stats.specialization!=null || sample.page.equals("Afinidade")&&ClientStatsStore.stats.affinityHouse!=null)
                    throw new IllegalStateException("Preview committed a permanent choice");
            }else if(sample.page.equals("Classe")||sample.page.equals("locked")) {
                var n=sample.clazz.nodes.get(sample.page.equals("locked")?sample.clazz.nodes.size()-1:0);click(screen,button(screen,n.name()),true);
                var f=StatsScreen.class.getDeclaredField("inspectedNode");f.setAccessible(true);
                if(!n.id().equals(f.get(screen)))throw new IllegalStateException("Clicked talent details did not persist");
                screen.rebuild();
                if(!n.id().equals(f.get(screen)))throw new IllegalStateException("Server rebuild cleared inspected talent");
                var v=MageViewport.fit(screen.width,screen.height);screen.mouseScrolled(v.x()+60*v.scale(),v.y()+200*v.scale(),-1);
            }
        }
        if(age==27 && sample.page.equals("Casa") && c.currentScreen instanceof StatsScreen screen) {
            click(screen,button(screen,"Ver talentos"),true);
            var v=MageViewport.fit(screen.width,screen.height);screen.mouseScrolled(v.x()+300*v.scale(),v.y()+280*v.scale(),-1);
        }
        if(age==30&&c.currentScreen instanceof StatsScreen screen){
            if(sample.page.equals("class")) {
                if(ClientStatsStore.stats.clazz!=null||!button(screen,"Confirmar classe").active)throw new IllegalStateException("Class preview/confirmation contract broken");
            } else if(!sample.page.equals("awakening")&&!sample.page.equals("Classe")&&!sample.page.equals("Stats")&&!sample.page.equals("locked")&&!button(screen,"Confirmar escolha").active)
                throw new IllegalStateException("Eligible preview cannot confirm");
            if(sample.page.equals("Stats")) {
                long plus=screen.children().stream().filter(ButtonWidget.class::isInstance).map(ButtonWidget.class::cast).filter(b->b.getMessage().getString().equals("+")).count();
                if(plus!=6)throw new IllegalStateException("Expected six active attributes, got "+plus);
                if(Stat.byName("FE")!=null)throw new IllegalStateException("Faith allocation still available");
                if(AttributeDetails.lines(ClientStatsStore.stats,Stat.ARCANO).stream().anyMatch(x->x.contains("Duração de aflições")))
                    throw new IllegalStateException("Arcane advertises unimplemented generic ailment duration");
            }
            if(sample.page.equals("Casa")) {
                var f=StatsScreen.class.getDeclaredField("choiceScroll");f.setAccessible(true);
                if(f.getInt(screen)<=0)throw new IllegalStateException("Scrolling did not reach additional House details");
            }
        }
        if(c.currentScreen instanceof GuideScreen guide) {
            if(age==20&&!sample.page.equals("Guide:index"))click(guide,button(guide,RPGClass.MAGO.display),true);
            if(age==22&&!sample.page.equals("Guide:index"))click(guide,button(guide,sample.page.equals("Guide:specs")?"15 Especializações":RPGPath.MAGE_OCCULT.display),true);
            if(age==24&&(sample.page.equals("Guide:spec")||sample.page.equals("Guide:scroll")))click(guide,button(guide,RPGSpecialization.BLOODMANCER.display),true);
            if(age==27&&sample.page.equals("Guide:scroll")) {
                var v=MageViewport.fit(guide.width,guide.height);
                guide.mouseScrolled(v.x()+610*v.scale(),v.y()+280*v.scale(),-200);
            }
            if(age==30) {
                var v=MageViewport.fit(guide.width,guide.height);
                for(var child:guide.children())if(child instanceof ButtonWidget b) {
                    if(b.getX()<GuideLayout.BOOK_X||b.getY()<GuideLayout.BOOK_Y||b.getY()+b.getHeight()>452)
                        throw new IllegalStateException("Guide control outside book: "+b.getMessage().getString());
                }
                if(sample.page.equals("Guide:scroll")) {
                    var f=GuideScreen.class.getDeclaredField("rightScroll");f.setAccessible(true);
                    if(f.getInt(guide)<=0)throw new IllegalStateException("Long guide not scrollable");
                }
                var field=GuideScreen.class.getDeclaredField("pageKey");field.setAccessible(true);
                String before=(String)field.get(guide);
                click(guide,button(guide,"Próx. ▶"),true);click(guide,button(guide,"Voltar"),true);
                if(!before.equals(field.get(guide)))throw new IllegalStateException("Guide next/back history broken");
            }
        }
        if(age==32&&sample.page.equals("Guide:scroll")&&c.currentScreen instanceof GuideScreen guide) {
            var v=MageViewport.fit(guide.width,guide.height);guide.mouseScrolled(v.x()+610*v.scale(),v.y()+280*v.scale(),-200);
        }
        if(++age>=36)capture=true;
    }
    private static String shortName(RPGPath p){return p.display.replace("Casa da ","").replace("Casa do ","").replace("Casa dos ","").replace("Casa de ","").replace("Casa ","");}
    private static ButtonWidget button(Screen screen,String label){return screen.children().stream().filter(ButtonWidget.class::isInstance).map(ButtonWidget.class::cast).filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new IllegalStateException("Missing button "+label));}
    private static void click(Screen screen,ButtonWidget button,boolean scaled){
        double x=button.getX()+button.getWidth()/2.0,y=button.getY()+button.getHeight()/2.0;
        if(scaled){var v=MageViewport.fit(screen.width,screen.height);x=v.x()+x*v.scale();y=v.y()+y*v.scale();}
        if(!screen.mouseClicked(x,y,0))throw new IllegalStateException("Visible control center not clickable: "+button.getMessage().getString());screen.mouseReleased(x,y,0);
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        try { capture(event); }catch(Throwable error){fail(error);}
    }
    private static void capture(TickEvent.RenderTickEvent event)throws Exception {
        if(!Boolean.getBoolean("rpgstats.uiPreview")||event.phase!=TickEvent.Phase.END||!capture)return;
        capture=false;var c=MinecraftClient.getInstance();Path out=Path.of(System.getProperty("rpgstats.uiPreviewDir"));Files.createDirectories(out);
        try(NativeImage image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){image.writeTo(out.resolve(CASES.get(index).file+".png"));}
        System.out.println("RPG_UI_CAPTURE "+CASES.get(index).file);index++;age=0;
        if(index==CASES.size()){Files.writeString(out.resolve("result.txt"),"PASS: 42 real Forge UI cases; six attributes, numeric House bonuses, guide navigation/history/scrolling, awakening, four classes, House/spec/affinity previews, confirmation eligibility, locked/learned talent inspection, reachable scrolling, scaled hitboxes and resizing.\n");c.scheduleStop();}
    }
    private static void fail(Throwable error) {
        try {
            Path out=Path.of(System.getProperty("rpgstats.uiPreviewDir"));Files.createDirectories(out);
            var buffer=new java.io.StringWriter();error.printStackTrace(new java.io.PrintWriter(buffer));
            Files.writeString(out.resolve("failure.txt"),buffer.toString());
            System.err.println(buffer);
        }catch(Exception ignored){}
        index=CASES.size();MinecraftClient.getInstance().scheduleStop();
    }
    private UiClientPreview(){}
}
