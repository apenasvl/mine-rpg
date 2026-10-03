import com.rpgstats.combat.WarriorHealingPolicy;
public final class WarriorHealingTests {
 static int checks;
 static void near(float actual,float expected){checks++;if(Math.abs(actual-expected)>.00001f)throw new AssertionError(actual+" != "+expected);}
 public static void main(String[] args){
  near(WarriorHealingPolicy.weaponScale(false,false),1f);
  near(WarriorHealingPolicy.weaponScale(true,false),.5f);
  near(WarriorHealingPolicy.weaponScale(false,true),.7f);
  near(WarriorHealingPolicy.weaponScale(true,true),.5f);
  var pool=new WarriorHealingPolicy.Budget();float healed=0;
  for(int i=0;i<100;i++)healed+=pool.take(.2f,20,1f,0);
  near(healed,1.5f);near(pool.take(2,20,1,19),0);
  near(pool.take(2,20,1,20),1.5f);
  pool=new WarriorHealingPolicy.Budget();near(pool.take(10,0,1,0),0);
  near(pool.take(10,.25f,.5f,0),.25f);
  near(pool.take(10,20,.7f,0),1.25f);
  near(pool.take(Float.NaN,20,1,21),0);
  near(pool.take(-1,20,1,21),0);
  near(pool.take(8*.025f,20,.5f,21),.1f);
  near(WarriorHealingPolicy.thirstGain(.1f,1f),.004375f);
  near(WarriorHealingPolicy.thirstGain(8,1),.35f);
  near(WarriorHealingPolicy.thirstGain(1000,1),.35f);
  System.out.println(checks+" warrior sustain checks passed");
 }
}
