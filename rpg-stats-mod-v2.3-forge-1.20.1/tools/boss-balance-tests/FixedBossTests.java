import com.rpgstats.integration.FixedBossProfile;
import com.rpgstats.compat.bosses.FixedBossPolicy;
public class FixedBossTests {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static void rejects(Runnable r){try{r.run();throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args){
  var p=new FixedBossProfile(31,40,35,3f,4f);
  var solo=FixedBossPolicy.scale(p,1);check(solo.healthFactor()==3);check(solo.damageFactor()==4);
  check(FixedBossPolicy.scale(p,8).equals(FixedBossPolicy.scale(p,12)));
  check(FixedBossPolicy.scale(p,1).equals(FixedBossPolicy.scale(p,0)));
  check(FixedBossPolicy.scale(p,2).healthFactor()>solo.healthFactor());
  double extra2=FixedBossPolicy.scale(p,2).healthFactor()-solo.healthFactor();
  double extra3=FixedBossPolicy.scale(p,3).healthFactor()-FixedBossPolicy.scale(p,2).healthFactor();check(extra3<extra2);
  rejects(()->new FixedBossProfile(31,40,25,3f,4f));rejects(()->new FixedBossProfile(0,40,35,3f,4f));
  rejects(()->new FixedBossProfile(31,51,35,3f,4f));rejects(()->new FixedBossProfile(40,31,35,3f,4f));
  for(float invalid:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
   rejects(()->new FixedBossProfile(31,40,35,invalid,4));rejects(()->new FixedBossProfile(31,40,35,3,invalid));
  }
  System.out.println(checks+" fixed boss policy checks passed");
 }
}
