package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.combat.WarriorSustain;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
@GameTestHolder(RPGStatsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WarriorSustainGameTests {
 @GameTest(templateName="empty",tickLimit=40)
 public static void rapidOffhandAndPrimaryHealingShareBudgetButPotionHealingIsSeparate(TestContext c){
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.clazz=RPGClass.GUERREIRO;StatsManager.save(p,s);
  p.setHealth(4);float start=p.getHealth();
  for(int i=0;i<100;i++)WarriorSustain.heal(p,.2f,new ItemStack(i%2==0?Items.GOLDEN_SWORD:Items.IRON_SWORD));
  c.assertTrue(Math.abs(p.getHealth()-start-1.5f)<.001,"Rapid hits exceeded the shared rolling sustain budget");
  p.heal(2);c.assertTrue(Math.abs(p.getHealth()-start-3.5f)<.001,"Ordinary healing was capped as weapon sustain");
  TestPlayers.finish(c);c.complete();
 }
 @GameTest(templateName="empty",tickLimit=40)
 public static void heavyAndOffClassWeaponsReduceActualHeal(TestContext c){
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.clazz=RPGClass.GUERREIRO;StatsManager.save(p,s);p.setHealth(4);
  c.assertTrue(Math.abs(WarriorSustain.heal(p,1,new ItemStack(Items.GOLDEN_SWORD))-.5f)<.001,"Off-class dagger healing not reduced");
  c.assertTrue(Math.abs(WarriorSustain.heal(p,1,new ItemStack(Items.NETHERITE_SWORD))-.7f)<.001,"Two-handed healing not reduced");
  TestPlayers.finish(c);c.complete();
 }
 @GameTest(templateName="empty",tickLimit=40)
 public static void lowDamageAndRapidRepeatedHitsCannotSpamThirst(TestContext c){
  var p=TestPlayers.create(c);p.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
  p.setStackInHand(net.minecraft.util.Hand.OFF_HAND,new ItemStack(Items.GOLDEN_SWORD));
  var s=new PlayerStats();s.awakened=true;s.clazz=RPGClass.GUERREIRO;s.path=com.rpgstats.classes.RPGPath.WAR_BERSERKER;s.specialization=com.rpgstats.classes.RPGSpecialization.BLOOD_REAVER;
  s.unlockedNodes.add("war_bers_reav_initiation");s.unlockedNodes.add("war_bers_reav_engine");StatsManager.save(p,s);
  var target=net.minecraft.entity.EntityType.ZOMBIE.create(p.getWorld());
  var state=com.rpgstats.combat.CombatState.get(p.getUuid());
  var source=p.getDamageSources().playerAttack(p);
  for(int i=0;i<100;i++)com.rpgstats.combat.ClassMechanics.onHit(p,target,s,.1f,source,false,false);
  c.assertTrue(Math.abs(state.gauge("war_thirst")-.002734375f)<.00001,"Tiny repeated hits generated excessive Sede or ignored rapid offhand");
  TestPlayers.finish(c);c.complete();
 }
}
