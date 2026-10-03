import com.rpgstats.classes.*;
import com.rpgstats.stats.*;
import com.rpgstats.ability.*;
import net.minecraft.nbt.*;
public class HouseTests {
 static int checks=0;
 static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
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

