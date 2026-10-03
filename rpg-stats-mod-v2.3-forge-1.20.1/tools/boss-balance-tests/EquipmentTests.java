import com.rpgstats.integration.EquipmentRules;
import com.rpgstats.compat.bosses.EquipmentPolicy;
import java.util.*;
public class EquipmentTests {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("Equipment check "+checks);}
 static void rejects(Runnable r){try{r.run();throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args){
  var r=new EquipmentRules(35,Set.of("GUERREIRO"),Map.of("str",30),.8f,1);
  check(!EquipmentPolicy.evaluate(34,"GUERREIRO",Map.of("str",30),r).allowed());
  check(EquipmentPolicy.evaluate(35,"GUERREIRO",Map.of("str",30),r).allowed());
  check(EquipmentPolicy.evaluate(36,"GUERREIRO",Map.of("str",31),r).allowed());
  check(EquipmentPolicy.evaluate(50,"MAGO",Map.of("str",30),r).allowed());
  check(!EquipmentPolicy.evaluate(50,"GUERREIRO",Map.of("str",29),r).allowed());
  check(EquipmentPolicy.evaluate(1,null,Map.of(),null).allowed());
  rejects(()->new EquipmentRules(51,Set.of("GUERREIRO"),Map.of(),1,1));
  rejects(()->new EquipmentRules(35,Set.of("INVALID_CLASS"),Map.of(),1,1));
  rejects(()->new EquipmentRules(35,Set.of("MAGO"),Map.of("unknown",2),1,1));
  rejects(()->new EquipmentRules(35,Set.of("MAGO"),Map.of("int",-1),1,1));
  rejects(()->new EquipmentRules(35,Set.of("MAGO"),Map.of(),Float.NaN,1));
  rejects(()->new EquipmentRules(35,Set.of("MAGO"),Map.of(),1,0));
  var classes=new HashSet<String>();classes.add("GUERREIRO");var stats=new HashMap<String,Integer>();stats.put("str",30);
  var frozen=new EquipmentRules(35,classes,stats,1,1);classes.add("MAGO");stats.put("str",0);
  check(!frozen.classes().contains("MAGO"));
  check(!EquipmentPolicy.evaluate(50,"GUERREIRO",Map.of("str",0),frozen).allowed());
  check(EquipmentPolicy.classDamageFactor("GUERREIRO",Set.of("GUERREIRO"))==1f);
  check(EquipmentPolicy.classDamageFactor("MAGO",Set.of("GUERREIRO"))==.625f);
  check(8f*EquipmentPolicy.classDamageFactor("MAGO",Set.of("ARQUEIRO"))==5f);
  check(EquipmentPolicy.classDamageFactor(null,Set.of("ARQUEIRO"))==.625f);
  check(EquipmentPolicy.classDamageFactor("ASSASSINO",Set.of("GUERREIRO","ASSASSINO"))==1f);
  System.out.println(checks+" equipment policy checks passed");
 }
}
