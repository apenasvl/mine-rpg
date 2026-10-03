import com.rpgstats.compat.irons.*;
import com.rpgstats.combat.MageBalance;
import java.util.UUID;
/** Regression: changing INT must survive normalization and exhausted per-target ceilings. */
public final class MageIntelligenceBudgetTests {
 static float hit(String spell,float nativeDamage,float progression,float conditional){
  float normalized=IronsSpellBalance.applyRpgScaling(spell,nativeDamage,nativeDamage*progression*conditional,progression);
  return IronsSpellBalance.applyTargetBudget(spell,UUID.randomUUID(),UUID.randomUUID(),0,normalized,MageBossBalance.progressionBudgetScale(progression,1f));
 }
 public static void main(String[] args){
  float lowProgression=1f+MageBalance.MAGE_INTELLIGENCE_DAMAGE_CAP*.2f+MageBalance.MAGE_LEVEL_DAMAGE_CAP+MageBalance.MAGE_CORE_MASTERY_DAMAGE_BONUS;
  float highProgression=1f+MageBalance.MAGE_INTELLIGENCE_DAMAGE_CAP+MageBalance.MAGE_LEVEL_DAMAGE_CAP+MageBalance.MAGE_CORE_MASTERY_DAMAGE_BONUS;
  for(String spell:new String[]{"magic_arrow","fireball","eldritch_blast","ray_of_frost","arrow_volley"}){
   float low=hit(spell,28f,lowProgression,1.25f),high=hit(spell,28f,highProgression,1.25f);
   if(high<=low*1.30f)throw new AssertionError("INT erased: "+spell+" -> "+low+" / "+high);
   if(Math.abs(high/low-highProgression/lowProgression)>.001f)throw new AssertionError("Persistent progression applied more than once: "+spell);
  }
  if(MageBossBalance.progressionBudgetScale(1.5f,2.5f)!=2.5f)throw new AssertionError("Boss and persistent ceilings multiplied");
  if(IronsSpellBalance.persistentMultiplier(100)!=1f+MageBalance.MAGE_PERSISTENT_DAMAGE_CAP || IronsSpellBalance.persistentMultiplier(Float.NaN)!=1f)throw new AssertionError("Unbounded persistent multiplier");
  if(IronsSpellBalance.applyRpgScaling("shield",10,30,1.5f)!=10f)throw new AssertionError("Utility acquired offensive scaling");
  if(IronsSpellBalance.applyTargetBudget("magic_arrow",UUID.randomUUID(),UUID.randomUUID(),0,100)!=26.5f)throw new AssertionError("Legacy/PvP limit changed");
  for(String spell:new String[]{"arrow_volley","eldritch_blast","ray_of_frost"}){
   UUID caster=UUID.randomUUID(),target=UUID.randomUUID();float total=0;
   for(int i=0;i<80;i++)total+=IronsSpellBalance.applyTargetBudget(spell,caster,target,0,100,1.5f);
   if(total>IronsSpellBalance.targetWindowCap(spell)*1.5f+.001f)throw new AssertionError("Projectile/channel count escaped budget");
   if(IronsSpellBalance.applyTargetBudget(spell,caster,target,0,100,1.28f)!=0)throw new AssertionError("INT change reset shared bucket");
  }
  System.out.println("PASS: INT survives spell guards exactly once; volleys/channels, utility, PvP and boss ceilings bounded");
 }
}
