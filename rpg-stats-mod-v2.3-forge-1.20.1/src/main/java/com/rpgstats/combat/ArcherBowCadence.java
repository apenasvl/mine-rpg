package com.rpgstats.combat;
import com.rpgstats.RPGStatsMod;
import com.rpgstats.stats.StatsApplier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Native draw countdown, driven by a synchronized role attribute on client and server. */
@Mod.EventBusSubscriber(modid=RPGStatsMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ArcherBowCadence {
 private static final String FRACTION="rpgstats_draw_fraction";
 @SubscribeEvent public static void draw(LivingEntityUseItemEvent.Tick event){
  if(event.isCanceled() || !(event.getEntity() instanceof PlayerEntity player) || !(event.getItem().getItem() instanceof BowItem) || event.getDuration()<=1)return;
  var attribute=player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
  var modifier=attribute==null?null:attribute.getModifier(StatsApplier.MOD_ARCHER_CADENCE);
  if(modifier==null){player.getPersistentData().remove(FRACTION);return;}
  float bonus=(float)Math.max(0,Math.min(.35,modifier.getValue()));
  float carried=player.getItemUseTime()<=0?0:player.getPersistentData().getFloat(FRACTION);
  float sum=carried+bonus;int extra=(int)sum;
  player.getPersistentData().putFloat(FRACTION,sum-extra);
  if(extra>0)event.setDuration(Math.max(1,event.getDuration()-extra));
 }
 private ArcherBowCadence(){}
}
