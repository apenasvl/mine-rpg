import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import com.rpgstats.ability.*;
import net.minecraft.nbt.*;
public class HouseTests {
 static int checks=0;
 static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  ok(Stat.byName("FE")==null,"retired Faith must reject allocation requests");
  for(RPGClass clazz:RPGClass.values()) {
   PlayerStats legacy=new PlayerStats();legacy.clazz=clazz;legacy.level=50;legacy.statPoints=7;
   legacy.stats.put(Stat.FE,19);legacy.stats.put(Stat.ARCANO,23);
   legacy.unlockedNodes.add(clazz.nodes.get(0).id());legacy.setActiveSlot(0,clazz.nodes.get(0).id());
   NbtCompound oldFaith=legacy.toNbt();oldFaith.putInt("dataVersion",10);
   oldFaith.getCompound("stats").putInt("FE",19);
   PlayerStats noFaith=PlayerStats.fromNbt(oldFaith);
   ok(noFaith.statPoints==26,"Faith PA refund for "+clazz);
   ok(noFaith.stats.get(Stat.FE)==0&&noFaith.stats.get(Stat.ARCANO)==23,"only Faith removed");
   ok(noFaith.level==50&&noFaith.clazz==clazz&&noFaith.unlockedNodes.equals(legacy.unlockedNodes),"build preserved");
   ok(!noFaith.toNbt().getCompound("stats").contains("FE"),"retired stat not persisted");
   ok(PlayerStats.fromNbt(noFaith.toNbt()).statPoints==26,"Faith refund is idempotent");
  }
  var holy=new com.rpgstats.integration.ItemProfile(java.util.Set.of("holy"),7,8,0,6,14,0,14,1,10);
  ok(holy.faith()==0&&holy.intelligence()==14,"legacy holy equipment uses Intelligence");
  var oldRules=new com.rpgstats.integration.EquipmentRules(10,java.util.Set.of("MAGO"),java.util.Map.of("int",8,"faith",14),1,1,true);
  ok(!oldRules.stats().containsKey("faith")&&oldRules.stats().get("int")==14,"legacy equipment gate migrated");
  try {
   Class<?> summary=Class.forName("com.rpgstats.gui.HouseBonusSummary");
   var sum=summary.getMethod("attributes",java.util.List.class,java.util.Set.class);
   var first=new com.rpgstats.tree.SkillNode("test_a","A","",1,null,null,0,java.util.Map.of(Stat.FORCA,2));
   var second=new com.rpgstats.tree.SkillNode("test_b","B","",1,null,null,0,java.util.Map.of(Stat.FORCA,3,Stat.ARCANO,1));
   var learned=(java.util.Map<?,?>)sum.invoke(null,java.util.List.of(first,second),java.util.Set.of("test_a"));
   ok(learned.get(Stat.FORCA).equals(2)&&!learned.containsKey(Stat.ARCANO),"House summary counts only learned nodes");
  } catch(ClassNotFoundException ex) {throw new AssertionError("House numeric bonus summary missing",ex);}
    catch(ReflectiveOperationException ex) {throw new AssertionError(ex);}
  var counterSummary=com.rpgstats.gui.HouseBonusSummary.lines(java.util.List.of(RPGPath.MAGE_ARCANA.findNode("mag_arc_counterspell")),java.util.Set.of());
  ok(counterSummary.stream().anyMatch(x->x.contains("após sucesso")),"Counterspell success condition retained");
  var drainSummary=String.join(" ",com.rpgstats.gui.HouseBonusSummary.lines(java.util.List.of(RPGPath.MAGE_OCCULT.findNode("mag_occ_drain")),java.util.Set.of()));
  ok(drainSummary.contains("3%")&&drainSummary.contains("bosses"),"Drain percentage and boss caveat retained");
  for(RPGPath main:RPGPath.values())for(RPGPath other:RPGPath.values()){
   PlayerStats s=new PlayerStats();s.clazz=main.parent;s.path=main;s.level=30;s.skillPoints=20;
   boolean valid=other!=main&&other.parent==main.parent;
   ok(HouseRules.chooseError(s,other).isEmpty()==valid,"house choice");
   if(!valid)continue;
   s.affinityHouse=other;
   ok(!HouseRules.chooseError(s,other).isEmpty(),"one house");
   var nodes=HouseRules.nodes(other);
   ok(nodes.size()==4,"curriculum size");
   for(var n:nodes){
    ok(n!=null,"missing node");
    ok(HouseRules.purchaseError(s,n.id()).isEmpty(),"purchase "+n.id());
    String real=HouseRules.real(n.id());
    boolean statefulClassMechanic=ClassAbilityRegistry.isExpandedClassNode(real);
    ok(!AbilityRegistry.get(n.id()).isEmpty() || statefulClassMechanic || n.id().equals("borrowed_mag_conj_pact"),"missing effects/mechanic "+n.id());
    s.skillPoints-=n.cost();s.unlockedNodes.add(n.id());
    ok(!HouseRules.purchaseError(s,n.id()).isEmpty(),"duplicate");
    for(var e:AbilityRegistry.get(n.id()))ok(Float.isFinite(e.value()),"finite");
   }
   ok(s.skillPoints==12,"budget");
   ok(HouseRules.count(s)==4,"count");
   var restored=PlayerStats.fromNbt(s.toNbt());
   ok(restored.affinityHouse==other&&HouseRules.count(restored)==4,"roundtrip");
  }
  for (RPGSpecialization spec : RPGSpecialization.values()) {
   if (spec.parent.parent != RPGClass.ARQUEIRO) continue;
   for (var node : spec.nodes) {
    String name = ClassAbilityRegistry.archerActiveName(node.id());
    if (name.isEmpty()) continue;
    ok(node.description().contains(name), "Specialization tooltip routed to House: " + node.id() + " -> " + node.description());
   }
  }
  PlayerStats s=new PlayerStats();s.clazz=RPGClass.MAGO;s.path=RPGPath.MAGE_ELEMENTAL;s.level=29;
  ok(!HouseRules.chooseError(s,RPGPath.MAGE_OCCULT).isEmpty(),"level gate");
  s.level=30;s.affinityHouse=RPGPath.MAGE_OCCULT;s.skillPoints=8;
  ok(!HouseRules.purchaseError(s,HouseRules.nodes(s.affinityHouse).get(1).id()).isEmpty(),"prerequisite");
  for(var n:HouseRules.nodes(s.affinityHouse))s.unlockedNodes.add(n.id());
  ok(Math.abs(HouseRules.vulnerability(s)-.08f)<.0001,"vulnerability");
  ok(Math.abs(HouseRules.scaled(SkillEffect.passive("spell_lifesteal",.1f)).value()-.055f)<.0001,"lifesteal secondary scale");
  ok(Math.abs(HouseRules.scaled(SkillEffect.passive("damage_reduction",.1f)).value()-.060f)<.0001,"defense secondary scale");
  ok(Math.abs(HouseRules.scaled(SkillEffect.passive("crit_chance",.1f)).value()-.070f)<.0001,"crit secondary scale");
  NbtCompound old=s.toNbt();old.putInt("dataVersion",8);old.putString("secondaryClass","GUERREIRO");
  NbtList list=new NbtList();list.add(NbtString.of("sec_war_core_awakening"));old.put("nodes",list);
  PlayerStats migrated=PlayerStats.fromNbt(old);
  ok(migrated.skillPoints==s.skillPoints+RPGClass.GUERREIRO.findNode("war_core_awakening").cost()+1,"exact legacy refund");
  ok(migrated.unlockedNodes.stream().noneMatch(x->x.startsWith("sec_")),"legacy removed");
  ok(PlayerStats.fromNbt(migrated.toNbt()).skillPoints==migrated.skillPoints,"no repeated refund");
  System.out.println("PASS: "+checks+" domain checks (no Minecraft runtime).");
 }
}
