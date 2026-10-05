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
  if(ClassBalance.warriorBossReduction(50,50)>.7501f || ClassBalance.warriorBossReduction(500,500)>.7501f || ClassBalance.warriorBossReduction(50,25)<.7399f)throw new AssertionError("Late Warrior boss defense missing or exceeds its budget");
  for(int level=1;level<=25;level++)for(int tenacity:new int[]{0,25,50}) {
   float p=ClassBalance.levelProgress(level),early=p*p*(.28f+.07f*tenacity/50f);
   float role=ClassBalance.warriorBossReduction(level,tenacity);
   if(Math.abs(role-early)>.0001f)throw new AssertionError("Early boss defense changed");
   for(float guards:new float[]{10f,50f,100f})
    if(Math.abs(ClassBalance.warriorBossDamage(100f,guards,role,level)-ClassBalance.warriorBossDamage(100f,guards,role))>.0001f)throw new AssertionError("Early combined cap changed");
  }
  if(ClassBalance.warriorBossDamage(100f,10f,1f,50)!=25f || ClassBalance.warriorBossDamage(100f,10f,1f,500)!=25f)throw new AssertionError("Stacked guards exceed late Warrior 75% cap");
  if(Math.abs(ClassBalance.juggernautBossReduction(50,25)-.79f)>.0001f || ClassBalance.juggernautBossDamage(100f,10f,1f,50)<19.999f)throw new AssertionError("General Warrior rebalance changed Juggernaut");
  if(ClassBalance.warriorBossDamage(100f,10f,.35f)!=40f)throw new AssertionError("Stacked Warrior guards bypass total RPG cap");
  if(ClassBalance.warriorBossDamage(100f,100f,.35f)<64.9f)throw new AssertionError("Role defense alone bypasses cap");
  if(ClassBalance.mobileMoveBonus(50,50,false)<.21f || ClassBalance.mobileMoveBonus(50,50,true)<.27f || ClassBalance.mobileMoveBonus(500,500,true)>.2801f)throw new AssertionError("Mobile roles missing or uncapped");
  if(ClassBalance.archerCadenceBonus(50,50)<.34f || ClassBalance.archerCadenceBonus(500,500)>.3501f || ClassBalance.archerCadenceBonus(1,50)!=0f)throw new AssertionError("Draw speed lacks earned progression");
  if(ClassBalance.mobileStaminaScale(50)<.84f || ClassBalance.mobileRegenDelay(50)!=12 || ClassBalance.mobileRegenDelay(1)!=24)throw new AssertionError("Mobility makes stamina unbounded / early recovery changes");
  for(int level:new int[]{-5,1,10,25})for(int tenacity:new int[]{0,25,50,500})for(float guards:new float[]{1f,35f,100f})
   if(ClassBalance.mobileBossDamage(100f,guards,level,tenacity)!=guards)throw new AssertionError("Mobile boss defense altered early guards");
  if(Math.abs(ClassBalance.mobileBossDamage(100f,100f,50,25)-27f)>.001f)throw new AssertionError("Mobile reference build cannot survive one native heavy hit");
  if(Math.abs(ClassBalance.mobileBossDamage(100f,1f,50,25)-26f)>.001f || Math.abs(ClassBalance.mobileBossDamage(100f,1f,500,500)-26f)>.001f)throw new AssertionError("Stacked mobile boss guards bypass 74% cap");
  if(ClassBalance.mobileBossDamage(100f,100f,50,25)>=ClassBalance.mobileBossDamage(100f,100f,50,0))throw new AssertionError("Mobile boss defense ignores Tenacity");
  float mobile25=ClassBalance.mobileBossDamage(100f,100f,25,25),mobile40=ClassBalance.mobileBossDamage(100f,100f,40,25),mobile50=ClassBalance.mobileBossDamage(100f,100f,50,25);
  if(!(mobile25>mobile40 && mobile40>mobile50))throw new AssertionError("Mobile boss defense has no earned late progression");
  for(int level:new int[]{1,25,30,40,50,500})for(int tenacity:new int[]{-50,0,25,50,500})
   if(ClassBalance.mobileBossReduction(level,tenacity)>ClassBalance.warriorBossReduction(level,tenacity))throw new AssertionError("Mobile role became tougher than Warrior");
  System.out.println("PASS: physical damage grows with level and primary attributes; early/magic limits and bounded conditional headroom preserved");
 }
}
