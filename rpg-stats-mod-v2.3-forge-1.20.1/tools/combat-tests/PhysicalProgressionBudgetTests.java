import com.rpgstats.balance.ClassBalance;
import com.rpgstats.balance.GlobalCaps;
public final class PhysicalProgressionBudgetTests {
 public static void main(String[] args){
  float archer=1f+Math.min(ClassBalance.ARCHER_PERSISTENT_DAMAGE_CAP,ClassBalance.ARCHER_DEX_DAMAGE_CAP+ClassBalance.ARCHER_LEVEL_DAMAGE_CAP+ClassBalance.ARCHER_CORE_MASTERY_DAMAGE_BONUS);
  if(archer<1.65f)throw new AssertionError("Endgame Archer barely exceeds native damage: "+archer);
  if(ClassBalance.warriorMeleeMultiplier(50,25,false)<=ClassBalance.warriorMeleeMultiplier(25,25,false)*1.10f)throw new AssertionError("Warrior level growth missing");
  if(ClassBalance.assassinMeleeMultiplier(50,25,false)<=ClassBalance.assassinMeleeMultiplier(25,25,false)*1.10f)throw new AssertionError("Assassin level growth missing");
  if(ClassBalance.assassinMeleeMultiplier(50,50,false)<=ClassBalance.assassinMeleeMultiplier(50,0,false))throw new AssertionError("Assassin Dexterity does not improve damage");
  if(ClassBalance.warriorMeleeMultiplier(50,50,true)>1.751f || ClassBalance.assassinMeleeMultiplier(50,50,true)>2.101f)throw new AssertionError("Persistent melee power exceeded budget");
  if(ClassBalance.warriorMeleeMultiplier(500,500,true)>1.751f || ClassBalance.assassinMeleeMultiplier(500,500,true)>2.101f)throw new AssertionError("Stats above level/stat limits escape caps");
  if(ClassBalance.assassinMeleeMultiplier(50,50,true)<2.0f)throw new AssertionError("Assassin endgame burst progression still below role budget");
  if(ClassBalance.warriorMeleeMultiplier(1,0,false)!=1f || ClassBalance.assassinMeleeMultiplier(1,0,false)!=1f)throw new AssertionError("Early base damage changed");
  if(GlobalCaps.damageMultiplier(100f)!=2.25f || GlobalCaps.physicalDamageMultiplier(100f,1)!=2.25f || GlobalCaps.physicalDamageMultiplier(100f,50)!=3.5f)throw new AssertionError("Magic or early caps changed / physical conditional headroom missing");
  if(GlobalCaps.physicalDamageMultiplier(2.8f,50)!=2.8f)throw new AssertionError("Cap should not amplify damage itself");
  if(ClassBalance.warriorBossReduction(50,50)>.3501f || ClassBalance.warriorBossReduction(25,50)>=ClassBalance.warriorBossReduction(50,50)*.3f)throw new AssertionError("Boss defense erases early progression or exceeds its budget");
  if(ClassBalance.warriorBossDamage(100f,10f,.35f)!=40f)throw new AssertionError("Stacked Warrior guards bypass total RPG cap");
  if(ClassBalance.warriorBossDamage(100f,100f,.35f)<64.9f)throw new AssertionError("Role defense alone bypasses cap");
  if(ClassBalance.mobileMoveBonus(50,50,false)<.21f || ClassBalance.mobileMoveBonus(50,50,true)<.27f || ClassBalance.mobileMoveBonus(500,500,true)>.2801f)throw new AssertionError("Mobile roles missing or uncapped");
  if(ClassBalance.archerCadenceBonus(50,50)<.34f || ClassBalance.archerCadenceBonus(500,500)>.3501f || ClassBalance.archerCadenceBonus(1,50)!=0f)throw new AssertionError("Draw speed lacks earned progression");
  if(ClassBalance.mobileStaminaScale(50)<.84f || ClassBalance.mobileRegenDelay(50)!=12 || ClassBalance.mobileRegenDelay(1)!=24)throw new AssertionError("Mobility makes stamina unbounded / early recovery changes");
  System.out.println("PASS: physical damage grows with level and primary attributes; early/magic limits and bounded conditional headroom preserved");
 }
}
