package com.rpgstats.compat.irons;

/** Boss-only headroom: permits earned native spell power without multiplying the hit itself. */
public final class MageBossBalance {
 public static float budgetScale(int level,int intelligence,boolean fixedBoss){
  if(!fixedBoss)return 1f;
  float levelProgress=Math.max(0f,Math.min(1f,(level-10)/40f));
  float intelligenceProgress=Math.max(0f,Math.min(1f,intelligence/40f));
  return 1f+1.5f*levelProgress*intelligenceProgress;
 }
 /** Marium's native defenses need additional earned headroom; other bosses retain their curve. */
 public static float mariumBudgetScale(int level,int intelligence){
  float levelProgress=Math.max(0f,Math.min(1f,(level-10)/40f));
  float intelligenceProgress=Math.max(0f,Math.min(1f,intelligence/40f));
  return 1f+2f*levelProgress*intelligenceProgress;
 }
 /** Ordinary targets retain earned growth too; boss headroom is not multiplied a second time. */
 public static float progressionBudgetScale(float persistentMultiplier,float bossHeadroom){
  float boss=Float.isFinite(bossHeadroom)?Math.max(1f,Math.min(3f,bossHeadroom)):1f;
  return Math.max(IronsSpellBalance.persistentMultiplier(persistentMultiplier),boss);
 }
 private MageBossBalance(){}
}
