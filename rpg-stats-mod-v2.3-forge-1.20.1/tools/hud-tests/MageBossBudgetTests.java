import com.rpgstats.compat.irons.IronsSpellBalance;
import com.rpgstats.compat.irons.MageBossBalance;
import java.util.UUID;
/** Executable regression: the old absolute cap erased a strong native spell before boss defenses. */
public final class MageBossBudgetTests {
 public static void main(String[] args){
  if(IronsSpellBalance.bloodmancerMultiplier("ray_of_siphoning",50,50,true,false)!=1.5f)
   throw new AssertionError("Bloodmancer needs earned Blood damage mastery");
  float earlyBlood=IronsSpellBalance.bloodmancerMultiplier("blood_slash",25,25,true,false);
  if(!(earlyBlood>1f && earlyBlood<1.5f))throw new AssertionError("Blood mastery must progress with level and INT");
  if(IronsSpellBalance.bloodmancerMultiplier("ray_of_siphoning",500,500,true,false)!=1.5f)
   throw new AssertionError("Blood mastery overflow escaped its cap");
  for(String path:new String[]{"blood_step","raise_dead","shadow_slash","heal"})
   if(IronsSpellBalance.bloodmancerMultiplier(path,50,50,true,false)!=1f)
    throw new AssertionError("Blood mastery leaked into mobility/summons/weapon/utility: "+path);
  if(IronsSpellBalance.bloodmancerMultiplier("ray_of_siphoning",50,50,false,false)!=1f
   || IronsSpellBalance.bloodmancerMultiplier("ray_of_siphoning",50,50,true,true)!=1f
   || IronsSpellBalance.bloodmancerMultiplier("ray_of_siphoning",24,50,true,false)!=1f
   || IronsSpellBalance.bloodmancerMultiplier("ray_of_siphoning",50,0,true,false)!=1f)
   throw new AssertionError("Blood mastery escaped training/level/INT/PvP boundaries");
  UUID caster=UUID.randomUUID(),target=UUID.randomUUID();
  float nativeAmount=40f;
  float scale=MageBossBalance.budgetScale(50,50,true);
  float accepted=IronsSpellBalance.applyTargetBudget("magic_arrow",caster,target,0,nativeAmount*1.35f,scale);
  if(accepted<nativeAmount)throw new AssertionError("Endgame spell was cut below native damage before boss defenses: "+accepted+" < "+nativeAmount);
  if(accepted!=nativeAmount*1.35f)throw new AssertionError("Raising headroom must not multiply damage");
  if(MageBossBalance.budgetScale(50,50,false)!=1f || MageBossBalance.budgetScale(10,50,true)!=1f || MageBossBalance.budgetScale(50,0,true)!=1f)throw new AssertionError("Headroom leaked outside earned boss progression");
  if(!(MageBossBalance.budgetScale(25,25,true)<scale) || scale!=2.5f)throw new AssertionError("Boss headroom lacks bounded progression");
  float ordinary=IronsSpellBalance.applyTargetBudget("magic_arrow",caster,target,0,100f);
  if(ordinary!=26.5f)throw new AssertionError("Ordinary/PvP cap changed");
  caster=UUID.randomUUID();target=UUID.randomUUID();float total=0f;
  for(int i=0;i<80;i++)total+=IronsSpellBalance.applyTargetBudget("arrow_volley",caster,target,0,100f,scale);
  if(total>67.501f)throw new AssertionError("High-volume boss burst escaped its bounded window: "+total);
  if(IronsSpellBalance.applyTargetBudget("arrow_volley",caster,target,0,100f,1f)!=0f)throw new AssertionError("Changing budget scale reset the shared bucket");
  if(IronsSpellBalance.applyTargetBudget("arrow_volley",caster,target,40,100f,scale)<=0f)throw new AssertionError("Budget did not refill");
  float marium=MageBossBalance.mariumBudgetScale(50,50);
  if(marium!=3f || MageBossBalance.mariumBudgetScale(10,50)!=1f || MageBossBalance.mariumBudgetScale(50,0)!=1f || !(MageBossBalance.mariumBudgetScale(25,25)<marium))throw new AssertionError("Marium headroom lacks bounded progression");
  if(MageBossBalance.progressionBudgetScale(1.88f,marium)!=3f)throw new AssertionError("Marium bonus applied twice");
  caster=UUID.randomUUID();target=UUID.randomUUID();total=0f;
  for(int i=0;i<80;i++)total+=IronsSpellBalance.applyTargetBudget("arrow_volley",caster,target,0,100f,marium);
  if(total>81.001f)throw new AssertionError("Marium volley escaped shared window");
  if(IronsSpellBalance.applyTargetBudget("arrow_volley",caster,target,0,100f,2.5f)!=0f)throw new AssertionError("Changing Marium headroom reset shared bucket");
  System.out.println("PASS: Mage boss ceilings follow earned progression; native damage, ordinary limits and shared burst bucket preserved");
 }
}
