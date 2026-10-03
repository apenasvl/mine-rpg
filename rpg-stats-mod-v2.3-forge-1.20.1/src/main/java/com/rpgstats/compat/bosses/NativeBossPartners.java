package com.rpgstats.compat.bosses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
/** Only Marium's own reciprocal UUID link establishes a pair. No proximity inference. */
public final class NativeBossPartners {
 public static LivingEntity livingPartner(LivingEntity boss) {
  String id=Registries.ENTITY_TYPE.getId(boss.getType()).toString();
  if(!id.equals("soulsweapons:day_stalker") && !id.equals("soulsweapons:night_prowler"))return null;
  if(!(boss.getWorld() instanceof ServerWorld world))return null;
  try {
   var uuid=(java.util.UUID)boss.getClass().getMethod("getPartnerUuid").invoke(boss);
   if(uuid==null || !(world.getEntity(uuid) instanceof LivingEntity partner) || !partner.isAlive())return null;
   String other=Registries.ENTITY_TYPE.getId(partner.getType()).toString();
   if(!other.equals(id.equals("soulsweapons:day_stalker")?"soulsweapons:night_prowler":"soulsweapons:day_stalker"))return null;
   return boss.getUuid().equals(partner.getClass().getMethod("getPartnerUuid").invoke(partner))?partner:null;
  }catch(ReflectiveOperationException error){throw new IllegalStateException("Pinned native partner API changed",error);}
 }
 private NativeBossPartners(){}
}
