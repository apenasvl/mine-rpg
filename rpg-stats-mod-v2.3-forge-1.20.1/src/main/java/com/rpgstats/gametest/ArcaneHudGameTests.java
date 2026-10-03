package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.ability.AbilityRegistry;
import com.rpgstats.gui.ArcaneHudText;
import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneHudGameTests {
 @GameTest(templateName="empty",tickLimit=30)
 public static void usableArcaneSkillsHaveDistinctTreeAndHudIdentities(TestContext c){
  var s=new PlayerStats();s.clazz=RPGClass.ARQUEIRO;s.path=RPGPath.ARC_ARCANE;
  var ids=new java.util.ArrayList<String>();ids.add("arc_core_utility");ids.add("arc_magic_technique");ids.add("arc_magic_signature");
  var names=new java.util.HashSet<String>();var codes=new java.util.HashSet<String>();
  for(var spec:new RPGSpecialization[]{RPGSpecialization.FLAMEBOW,RPGSpecialization.FROSTBOW,RPGSpecialization.STORMBOW}){
   s.specialization=spec;
   var relevant=new java.util.ArrayList<String>();if(spec==RPGSpecialization.FLAMEBOW)relevant.addAll(ids);
   spec.nodes.stream().filter(n->AbilityRegistry.hasActive(n.id())).forEach(n->relevant.add(n.id()));
   for(String id:relevant){
    var node=StatsManager.findNode(s,id);var hud=ArcaneHudText.entry(id);
    c.assertTrue(node!=null&&hud!=null,"Usable skill lacks a readable HUD identity: "+id);
    c.assertTrue(node.name().equals(hud.name()),"Tree and HUD disagree about skill name: "+id);
    c.assertTrue(names.add(node.name())&&codes.add(hud.code()),"Repeated active identity: "+id);
    c.assertTrue(!AbilityRegistry.activeSummary(id).isBlank(),"Usable skill lacks its action summary: "+id);
   }
  }
  c.assertTrue(names.size()==12,"Wrong Arcane usable skill coverage: "+names.size());c.complete();
 }
}
