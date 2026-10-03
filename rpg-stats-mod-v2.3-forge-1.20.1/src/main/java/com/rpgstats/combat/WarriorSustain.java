package com.rpgstats.combat;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.compat.WeaponTypeResolver;
import com.rpgstats.compat.bosses.WeaponAffinity;
import com.rpgstats.stats.StatsManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Limits weapon sustain only; vanilla regeneration, potions and support healing stay separate. */
@Mod.EventBusSubscriber(modid=RPGStatsMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class WarriorSustain {
    private record Request(UUID owner,ItemStack weapon) {}
    private static final ThreadLocal<Request> REQUEST=new ThreadLocal<>();
    private static final Map<UUID,WarriorHealingPolicy.Budget> BUDGETS=new HashMap<>();
    public static ItemStack meleeWeapon(ServerPlayerEntity player) {
        ItemStack main=player.getMainHandStack(),off=player.getOffHandStack();
        // A rapid offhand shares the pair's sustain penalty even when an optional
        // dual-wield implementation does not expose its attacking hand to Forge.
        return WeaponTypeResolver.isDagger(off) && weaponScale(player,off)<weaponScale(player,main) ? off : main;
    }
    public static float weaponScale(ServerPlayerEntity player,ItemStack weapon) {
        return WarriorHealingPolicy.weaponScale(WeaponAffinity.damageFactor(StatsManager.get(player),weapon)<1,
                WeaponTypeResolver.isTwoHanded(weapon));
    }
    public static float heal(ServerPlayerEntity player,float requested,ItemStack weapon) {
        Request previous=REQUEST.get();REQUEST.set(new Request(player.getUuid(),weapon.copy()));
        float before=player.getHealth();
        try {player.heal(requested);}finally{if(previous==null)REQUEST.remove();else REQUEST.set(previous);}
        return Math.max(0,player.getHealth()-before);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void healing(LivingHealEvent event) {
        if(!(event.getEntity() instanceof ServerPlayerEntity player)||StatsManager.get(player).clazz!=RPGClass.GUERREIRO)return;
        Request request=REQUEST.get();ItemStack weapon;
        if(request!=null&&request.owner().equals(player.getUuid()))weapon=request.weapon();
        else {
            // Native Simply Swords directly invokes heal from item/effect code. Its sustain
            // must share the same budget, without imposing a cap on vanilla potion healing.
            boolean nativeCall=StackWalker.getInstance().walk(frames->frames.anyMatch(f->f.getClassName().startsWith("net.sweenus.simplyswords.")));
            if(!nativeCall)return;
            ItemStack main=player.getMainHandStack(),off=player.getOffHandStack();
            boolean mainNative=Registries.ITEM.getId(main.getItem()).getNamespace().equals("simplyswords");
            boolean offNative=Registries.ITEM.getId(off.getItem()).getNamespace().equals("simplyswords");
            if(!mainNative&&!offNative)return;
            weapon=mainNative?main:off;
            if(offNative&&weaponScale(player,off)<weaponScale(player,weapon))weapon=off;
        }
        float accepted=BUDGETS.computeIfAbsent(player.getUuid(),id->new WarriorHealingPolicy.Budget())
                .take(event.getAmount(),player.getMaxHealth()-player.getHealth(),weaponScale(player,weapon),player.getServer().getTicks());
        event.setAmount(accepted);if(accepted<=0)event.setCanceled(true);
    }
    public static void tick(MinecraftServer server){BUDGETS.entrySet().removeIf(e->e.getValue().expired(server.getTicks()));}
    public static void clear(){BUDGETS.clear();REQUEST.remove();}
    private WarriorSustain(){}
}
