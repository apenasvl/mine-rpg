package com.rpgstats.gametest;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.compat.WeaponTypeResolver;
import com.rpgstats.compat.bosses.WeaponAffinity;
import com.rpgstats.integration.DataDrivenRegistry;
import com.rpgstats.stats.*;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
@GameTestHolder(RPGStatsMod.MOD_ID) @PrefixGameTestTemplate(false)
public final class BetterWeaponryGameTests {
 @GameTest(templateName="empty",tickLimit=40)
 public static void betterWeaponryWeaponsHaveProgressionAndCorrectAffinity(TestContext c) {
  if(!ModList.get().isLoaded("better_weaponry")){c.complete();return;}
  WeaponTypeResolver.reload(c.getWorld().getServer().getResourceManager());
  for(String name:new String[]{"diamond_dagger","diamond_sai","diamond_rapier","diamond_shuriken","diamond_claymore","diamond_broadsword","diamond_battleaxe"}) {
   var id=new Identifier("better_weaponry",name);c.assertTrue(Registries.ITEM.containsId(id),"Missing native Better Weaponry item "+id);
   var stack=new ItemStack(Registries.ITEM.get(id));var rules=DataDrivenRegistry.equipmentRules(stack).orElse(null);
   c.assertTrue(rules!=null && rules.minLevel()==20,"Better Weaponry diamond progression missing: "+name);
   boolean rapid=name.endsWith("dagger")||name.endsWith("sai")||name.endsWith("rapier")||name.endsWith("shuriken");
   for(var clazz:RPGClass.values()) {var s=new PlayerStats();s.clazz=clazz;
    c.assertTrue(Math.abs(WeaponAffinity.damageFactor(s,stack)-((clazz==(rapid?RPGClass.ASSASSINO:RPGClass.GUERREIRO))?1f:.625f))<.0001,"Wrong native weapon affinity "+name+" "+clazz);
   }
   if(!name.endsWith("shuriken"))c.assertTrue(rapid?WeaponTypeResolver.isDagger(stack):WeaponTypeResolver.isTwoHanded(stack),"Native weapon sustain type missing "+name);
  }c.complete();
 }
 @GameTest(templateName="empty",tickLimit=40)
 public static void nativeVampirismCannotHealBeforeDamageIsConfirmed(TestContext c) {
  if(!ModList.get().isLoaded("better_weaponry")){c.complete();return;}
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.clazz=RPGClass.GUERREIRO;s.level=50;StatsManager.finish(p,s);
  var target=EntityType.ZOMBIE.create(c.getWorld());target.setAiDisabled(true);target.setInvulnerable(true);c.getWorld().spawnEntity(target);
  var weapon=new ItemStack(Registries.ITEM.get(new Identifier("better_weaponry","diamond_dagger")));
  weapon.addEnchantment(Registries.ENCHANTMENT.get(new Identifier("better_weaponry","vampirism")),3);p.setStackInHand(Hand.MAIN_HAND,weapon);p.setHealth(4);
  try {
   var cls=Class.forName("betterweaponry.procedures.EnchantmentsTriggerProcedure");
   var method=java.util.Arrays.stream(cls.getMethods()).filter(m->m.getName().equals("execute")&&m.getParameterCount()==6).findFirst().orElseThrow();
   method.invoke(null,c.getWorld(),p.getDamageSources().playerAttack(p),target,p,p,80d);
   c.assertTrue(p.getHealth()==4,"Native Vampirism healed without confirmed damage");
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{target.discard();TestPlayers.finish(c);}c.complete();
 }
 private BetterWeaponryGameTests(){}
}
