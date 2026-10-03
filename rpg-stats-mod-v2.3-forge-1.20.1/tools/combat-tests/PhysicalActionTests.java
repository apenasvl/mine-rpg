import com.rpgstats.compat.PhysicalActionPolicy;
import com.rpgstats.compat.ArmorBonusPolicy;
public class PhysicalActionTests {
 public static void main(String[] args) {
  check(PhysicalActionPolicy.isRollNode("arc_ward_surv_technique"));
  check(PhysicalActionPolicy.isRollNode("arc_skirm_guer_technique"));
  check(!PhysicalActionPolicy.isRollNode("arc_wind_run_technique"));
  check(!PhysicalActionPolicy.isRollNode("house_arc_ward_surv_technique"));
  check(!PhysicalActionPolicy.canRoll(true,false,false,false,false,false));
  check(PhysicalActionPolicy.canRoll(true,true,false,false,false,false));
  check(!PhysicalActionPolicy.canRoll(false,true,false,false,false,false));
  check(!PhysicalActionPolicy.canRoll(true,true,true,false,false,false));
  check(!PhysicalActionPolicy.canRoll(true,true,false,true,false,false));
  check(!PhysicalActionPolicy.canRoll(true,true,false,false,true,false));
  check(!PhysicalActionPolicy.canRoll(true,true,false,false,false,true));
  check(ArmorBonusPolicy.weight(-.02f)==0);
  check(ArmorBonusPolicy.weight(0)==0);
  check(ArmorBonusPolicy.weight(.15f)==.15f);
  System.out.println("14 physical-action/armor checks passed");
 }
 static void check(boolean b) { if(!b) throw new AssertionError(); }
}
