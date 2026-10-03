package com.rpgstats.compat.bosses;

import com.rpgstats.RPGStatsMod;
import net.minecraft.registry.Registries;
import net.minecraftforge.fml.ModList;

/** Extend native projectile allowlists in memory; native phases/reflection/ordinary arrows stay intact. */
public final class MariumMageProjectiles {
 public static void install(){
  if(!ModList.get().isLoaded("soulsweapons") || !ModList.get().isLoaded("irons_spellbooks"))return;
  String[] spells=Registries.ENTITY_TYPE.getIds().stream()
      .filter(id->id.getNamespace().equals("irons_spellbooks")).map(Object::toString).toArray(String[]::new);
  try{
   var config=Class.forName("net.soulsweaponry.config.BossConfig");
   for(String name:new String[]{"returning_knight_projectile_immunity_whitelist","fallen_icon_projectile_immunity_whitelist","night_prowler_projectile_immunity_whitelist"}){
    var field=config.getField(name);
    var ids=new java.util.LinkedHashSet<String>(java.util.Arrays.asList((String[])field.get(null)));
    ids.addAll(java.util.Arrays.asList(spells));field.set(null,ids.toArray(String[]::new));
   }
   RPGStatsMod.LOGGER.info("RPG Marium mage compatibility: {} Iron entity IDs added to native projectile allowlists",spells.length);
  }catch(ReflectiveOperationException | LinkageError error){
   RPGStatsMod.LOGGER.error("Failed to extend native Marium mage projectile allowlists",error);
  }
 }
 private MariumMageProjectiles(){}
}
