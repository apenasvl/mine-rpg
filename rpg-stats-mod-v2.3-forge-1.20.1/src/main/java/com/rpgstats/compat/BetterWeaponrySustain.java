package com.rpgstats.compat;
import com.rpgstats.combat.*;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.stats.StatsManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
/** Native Vampirism is replaced only for the audited 1.1.3 binary. */
public final class BetterWeaponrySustain {
 private static final Identifier VAMPIRISM=new Identifier("better_weaponry","vampirism");
 private static final Map<UUID,WarriorHealingPolicy.Budget> BUDGETS=new HashMap<>();
 private static boolean auditedVersion() {
  return net.minecraftforge.fml.ModList.get().getMods().stream().anyMatch(m->m.getModId().equals("better_weaponry")&&m.getVersion().toString().equals("1.1.3"));
 }
 public static float projectileDamage(ServerPlayerEntity p,float amount,DamageSource source) {
  if(!auditedVersion() || !source.isOf(net.minecraft.entity.damage.DamageTypes.ARROW) || amount<=0)return amount;
  var id=new Identifier("better_weaponry","damage");if(!Registries.ENCHANTMENT.containsId(id))return amount;
  var original=ArcherShotTracker.storedLaunchWeapon(source,p);if(original.isEmpty())return amount;
  int level=EnchantmentHelper.getLevel(Registries.ENCHANTMENT.get(id),original.get());
  return amount+2*Math.min(5,Math.max(0,level));
 }
 public static void confirmedHit(ServerPlayerEntity p,float damage,DamageSource source) {
  if(!auditedVersion() || source.getSource()!=p || damage<=0 || source.isOf(net.minecraft.entity.damage.DamageTypes.THORNS))return;
  if(!Registries.ENCHANTMENT.containsId(VAMPIRISM))return;
  ItemStack stack=p.getMainHandStack();int level=EnchantmentHelper.getLevel(Registries.ENCHANTMENT.get(VAMPIRISM),stack);
  if(level<=0)return;
  float requested=damage*Math.min(3,level)/24f;
  if(StatsManager.get(p).clazz==RPGClass.GUERREIRO)WarriorSustain.heal(p,requested,WarriorSustain.meleeWeapon(p));
  else {
   float scale=WarriorSustain.weaponScale(p,stack);
   float accepted=BUDGETS.computeIfAbsent(p.getUuid(),id->new WarriorHealingPolicy.Budget()).take(requested,p.getMaxHealth()-p.getHealth(),scale,p.getServer().getTicks());
   if(accepted>0)p.heal(accepted);
  }
 }
 public static void tick(net.minecraft.server.MinecraftServer server){BUDGETS.entrySet().removeIf(e->e.getValue().expired(server.getTicks()));}
 public static void clear(){BUDGETS.clear();}
 private BetterWeaponrySustain(){}
}
