package com.rpgstats.compat;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.integration.DataDrivenRegistry;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.*;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
/** Small own-class gear bonuses; original armor and Iron spell attributes are never removed. */
public final class ClassArmorBonuses {
 private static final UUID MOVE=UUID.fromString("d8462f11-f5b4-43e4-a3dc-3a848bc50c01"),ATTACK=UUID.fromString("d8462f11-f5b4-43e4-a3dc-3a848bc50c02"),TOUGH=UUID.fromString("d8462f11-f5b4-43e4-a3dc-3a848bc50c03"),KNOCK=UUID.fromString("d8462f11-f5b4-43e4-a3dc-3a848bc50c04");
 public static void apply(ServerPlayerEntity p) {
  var stats=StatsManager.get(p);var clazz=stats.clazz;int pieces=0;
  if(clazz!=null)for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
   var stack=p.getEquippedStack(slot);
   if(stack.getItem() instanceof net.minecraft.item.ArmorItem && DataDrivenRegistry.equipment(stack).map(profile->profile.categories().contains("class_"+clazz.name().toLowerCase(Locale.ROOT))).orElse(false))pieces++;
  }
  double earned=pieces*com.rpgstats.balance.ClassBalance.levelProgress(stats.level);
  double move=clazz==RPGClass.ARQUEIRO?.02*earned:clazz==RPGClass.ASSASSINO?.01*earned:0;
  double attack=clazz==RPGClass.ASSASSINO?.02*earned:0;
  double tough=clazz==RPGClass.GUERREIRO?.5*earned:clazz==RPGClass.MAGO?.25*earned:0;
  var instance=p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
  if(instance!=null) {
   double additive=instance.getBaseValue(),baseMultiplier=0,totalMultiplier=1;
   for(var m:instance.getModifiers())if(!m.getId().equals(TOUGH))switch(m.getOperation()) {
    case ADDITION -> additive+=m.getValue();
    case MULTIPLY_BASE -> baseMultiplier+=m.getValue();
    case MULTIPLY_TOTAL -> totalMultiplier*=1+m.getValue();
   }
   double factor=(1+baseMultiplier)*totalMultiplier;
   tough=factor>0?Math.min(tough,Math.max(0,(12-additive*factor)/factor)):0;
  }
  modifier(p,EntityAttributes.GENERIC_MOVEMENT_SPEED,MOVE,move,EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
  modifier(p,EntityAttributes.GENERIC_ATTACK_SPEED,ATTACK,attack,EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
  modifier(p,EntityAttributes.GENERIC_ARMOR_TOUGHNESS,TOUGH,tough,EntityAttributeModifier.Operation.ADDITION);
  modifier(p,EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,KNOCK,clazz==RPGClass.GUERREIRO?.03*earned:0,EntityAttributeModifier.Operation.ADDITION);
 }
 private static void modifier(ServerPlayerEntity p,EntityAttribute attribute,UUID id,double value,EntityAttributeModifier.Operation operation) {
  var a=p.getAttributeInstance(attribute);if(a==null)return;var old=a.getModifier(id);
  if(old!=null && Math.abs(old.getValue()-value)<.0000001)return;
  if(old!=null)a.removeModifier(id);
  if(value!=0)a.addPersistentModifier(new EntityAttributeModifier(id,"RPG class armor",value,operation));
 }
 private ClassArmorBonuses(){}
}
