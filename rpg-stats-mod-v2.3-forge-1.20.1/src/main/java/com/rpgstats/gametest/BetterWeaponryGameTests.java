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
 @GameTest(templateName="empty",tickLimit=40)
 public static void armorProfilesDistinguishTheClassRoles(TestContext c) {
  for(var row:new String[][]{{"soulsweapons:soul_ingot_chestplate","GUERREIRO"},{"soulsweapons:soul_robes_chestplate","ARQUEIRO"},{"soulsweapons:forlorn_chestplate","ASSASSINO"},{"irons_spellbooks:cryomancer_chestplate","MAGO"}}) {
   var id=new Identifier(row[0]);if(!Registries.ITEM.containsId(id))continue;
   var profile=DataDrivenRegistry.equipment(new ItemStack(Registries.ITEM.get(id))).orElse(null);
   c.assertTrue(profile!=null && profile.categories().contains("class_"+row[1].toLowerCase(java.util.Locale.ROOT)),"Armor class profile missing "+id);
  }c.complete();
 }
 @GameTest(templateName="empty",tickLimit=40)
 public static void confirmedNativeVampirismSharesWarriorBudget(TestContext c) {
  if(!ModList.get().isLoaded("better_weaponry")){c.complete();return;}
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.level=50;s.clazz=RPGClass.GUERREIRO;StatsManager.finish(p,s);
  var weapon=new ItemStack(Registries.ITEM.get(new Identifier("better_weaponry","diamond_dagger")));weapon.addEnchantment(Registries.ENCHANTMENT.get(new Identifier("better_weaponry","vampirism")),3);p.setStackInHand(Hand.MAIN_HAND,weapon);p.setHealth(4);
  var target=EntityType.ZOMBIE.create(c.getWorld());target.setAiDisabled(true);target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100000);target.setHealth(100000);c.getWorld().spawnEntity(target);
  try {
   float before=target.getHealth();target.damage(p.getDamageSources().playerAttack(p),100);
   c.assertTrue(target.getHealth()<before,"Vampirism fixture hit was not confirmed");
   c.assertTrue(Math.abs(p.getHealth()-5.5f)<.001,"Confirmed Vampirism escaped the rolling healing cap");
   com.rpgstats.combat.WarriorSustain.heal(p,2,new ItemStack(Items.IRON_SWORD));
   c.assertTrue(Math.abs(p.getHealth()-5.5f)<.001,"Native and RPG Vampirism used separate budgets");
   p.heal(2);c.assertTrue(Math.abs(p.getHealth()-7.5f)<.001,"Ordinary healing was capped by weapon sustain");
  }finally{target.discard();TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=60)
 public static void armorRoleBonusesApplyOnceAndLeaveWhenUnequipped(TestContext c) {
  if(!ModList.get().isLoaded("soulsweapons")){c.complete();return;}
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.level=50;s.clazz=RPGClass.ARQUEIRO;StatsManager.finish(p,s);
  var move=net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED;double bare=p.getAttributeValue(move);
  try {
   var slots=new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD,net.minecraft.entity.EquipmentSlot.CHEST,net.minecraft.entity.EquipmentSlot.LEGS,net.minecraft.entity.EquipmentSlot.FEET};
   var suffixes=new String[]{"helmet","chestplate","leggings","boots"};
   for(int j=0;j<4;j++)p.equipStack(slots[j],new ItemStack(Registries.ITEM.get(new Identifier("soulsweapons","soul_robes_"+suffixes[j]))));
   p.playerTick();p.tick();StatsApplier.apply(p);double equipped=p.getAttributeValue(move);
   c.assertTrue(Math.abs(equipped/bare-1.08)<.001,"Archer light armor did not grant its bounded mobility bonus");
   for(int j=0;j<3;j++)StatsApplier.apply(p);c.assertTrue(Math.abs(p.getAttributeValue(move)-equipped)<.00001,"Armor role bonus stacked on refresh");
   s=StatsManager.get(p);s.clazz=RPGClass.GUERREIRO;StatsManager.finish(p,s);c.assertTrue(p.getAttributeValue(move)<equipped,"Armor bonus persisted after changing class");
   s.clazz=RPGClass.ARQUEIRO;StatsManager.finish(p,s);for(var slot:slots)p.equipStack(slot,ItemStack.EMPTY);p.playerTick();p.tick();StatsApplier.apply(p);
   c.assertTrue(Math.abs(p.getAttributeValue(move)-bare)<.00001,"Armor role bonus remained after unequipping");
  }finally{TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=40)
 public static void nativeShurikenKeepsItsAffinityAfterWeaponSwap(TestContext c) {
  if(!ModList.get().isLoaded("better_weaponry")){c.complete();return;}
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.level=50;s.clazz=RPGClass.GUERREIRO;StatsManager.finish(p,s);
  var weapon=new ItemStack(Registries.ITEM.get(new Identifier("better_weaponry","diamond_shuriken")));p.setStackInHand(Hand.MAIN_HAND,weapon);
  net.minecraft.entity.Entity raw=null;
  try {
   var cls=Class.forName("betterweaponry.entity.DiamondShurikenEntityEntity");var shoot=java.util.Arrays.stream(cls.getMethods()).filter(m->m.getName().equals("shoot")&&m.getParameterCount()==3).findFirst().orElseThrow();
   raw=(net.minecraft.entity.Entity)shoot.invoke(null,c.getWorld(),p,p.getRandom());
   var shot=(net.minecraft.entity.projectile.PersistentProjectileEntity)raw;var source=p.getDamageSources().arrow(shot,p);
   p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
   c.assertTrue(ArcherWeapon(source,p).isOf(weapon.getItem()),"Native thrown weapon identity was lost after swapping");
   c.assertTrue(Math.abs(WeaponAffinity.damageFactor(StatsManager.get(p),ArcherWeapon(source,p))-.625f)<.0001,"Weapon swap removed shuriken off-class penalty");
   c.assertTrue(!com.rpgstats.combat.ArcherShotTracker.isBowShot(source),"Native shuriken incorrectly acquired bow abilities");
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{if(raw!=null)raw.discard();TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=40)
 public static void nativeDamageEnchantmentPreservesArrowAndCannotTriggerMeleeHealing(TestContext c) {
  if(!ModList.get().isLoaded("better_weaponry")){c.complete();return;}
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.level=50;s.clazz=RPGClass.GUERREIRO;StatsManager.finish(p,s);
  var weapon=new ItemStack(Items.BOW);weapon.addEnchantment(Registries.ENCHANTMENT.get(new Identifier("better_weaponry","damage")),1);weapon.addEnchantment(Registries.ENCHANTMENT.get(new Identifier("better_weaponry","vampirism")),3);p.setStackInHand(Hand.MAIN_HAND,weapon);p.setHealth(4);
  var target=EntityType.ZOMBIE.create(c.getWorld());target.setAiDisabled(true);target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100000);target.setHealth(100000);c.getWorld().spawnEntity(target);
  var shot=new net.minecraft.entity.projectile.ArrowEntity(c.getWorld(),p);
  try {
   com.rpgstats.combat.ArcherShotTracker.beginLaunch(p,weapon);try{c.getWorld().spawnEntity(shot);}finally{com.rpgstats.combat.ArcherShotTracker.endLaunch(p);}
   var source=p.getDamageSources().arrow(shot,p);
   var swapped=new ItemStack(Items.IRON_SWORD);swapped.addEnchantment(Registries.ENCHANTMENT.get(new Identifier("better_weaponry","damage")),5);swapped.addEnchantment(Registries.ENCHANTMENT.get(new Identifier("better_weaponry","vampirism")),3);p.setStackInHand(Hand.MAIN_HAND,swapped);
   c.assertTrue(com.rpgstats.compat.BetterWeaponrySustain.projectileDamage(p,10,source)==12,"Damage enchantment did not use the original launch stack");
   target.damage(source,100);
   var recent=target.getRecentDamageSource();c.assertTrue(recent!=null && recent.isOf(net.minecraft.entity.damage.DamageTypes.ARROW),"Native Damage enchantment converted a projectile to melee");
   c.assertTrue(p.getHealth()==4,"Arrow incorrectly triggered native melee Vampirism");
  }finally{shot.discard();target.discard();TestPlayers.finish(c);}c.complete();
 }
 @GameTest(templateName="empty",tickLimit=40)
 public static void armorToughnessBudgetHandlesNativeMultipliers(TestContext c) {
  if(!ModList.get().isLoaded("irons_spellbooks")){c.complete();return;}
  var p=TestPlayers.create(c);var s=new PlayerStats();s.awakened=true;s.level=50;s.clazz=RPGClass.MAGO;StatsManager.finish(p,s);
  var slots=new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD,net.minecraft.entity.EquipmentSlot.CHEST,net.minecraft.entity.EquipmentSlot.LEGS,net.minecraft.entity.EquipmentSlot.FEET};var names=new String[]{"helmet","chestplate","leggings","boots"};
  try {
   for(int i=0;i<4;i++)p.equipStack(slots[i],new ItemStack(Registries.ITEM.get(new Identifier("irons_spellbooks","wizard_"+names[i]))));p.playerTick();p.tick();
   var tough=p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS);tough.setBaseValue(10.5);
   tough.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(java.util.UUID.fromString("9c439aba-6827-4e7c-bca5-dad6e173f44f"),"Native multiplicative fixture",.1,net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
   for(int i=0;i<5;i++){StatsApplier.apply(p);c.assertTrue(Math.abs(tough.getValue()-12)<.00001,"Armor role bonus exceeded or jittered at multiplicative toughness cap");}
   for(var slot:slots)p.equipStack(slot,ItemStack.EMPTY);p.playerTick();p.tick();StatsApplier.apply(p);c.assertTrue(Math.abs(tough.getValue()-11.55)<.00001,"Removing RPG armor changed native multiplicative toughness");
  }finally{TestPlayers.finish(c);}c.complete();
 }
 private static ItemStack ArcherWeapon(net.minecraft.entity.damage.DamageSource source,net.minecraft.server.network.ServerPlayerEntity p){return com.rpgstats.combat.ArcherShotTracker.launchWeapon(source,p);}
 private BetterWeaponryGameTests(){}
}
