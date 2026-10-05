package com.rpgstats.forge;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.boss.EncounterManager;
import com.rpgstats.combat.CombatHandler;
import com.rpgstats.combat.CombatState;
import com.rpgstats.combat.MageCombatHandler;
import com.rpgstats.combat.ProcDamageQueue;
import com.rpgstats.integration.DataDrivenRegistry;
import com.rpgstats.network.RpgNetwork;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsApplier;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import com.rpgstats.compat.bosses.BossEquipmentService;
import net.minecraftforge.fml.common.Mod;

/** Forge event bus. All game-state writes happen on the logical server. */
@Mod.EventBusSubscriber(modid=RPGStatsMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEvents {
    /** Old physical Codex items use the same corrected screen as /rpg guia. */
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void openPhysicalGuide(PlayerInteractEvent.RightClickItem event) {
        if(!com.rpgstats.guide.RpgGuideBook.isGuide(event.getItemStack()))return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.util.ActionResult.SUCCESS);
        if(event.getEntity() instanceof ServerPlayerEntity player) {
            RPGStatsMod.syncStats(player);RpgNetwork.openGuide(player);
        }
    }
    @SubscribeEvent public static void armorAffinityTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        var stack=event.getItemStack();if(!(stack.getItem() instanceof net.minecraft.item.ArmorItem))return;
        for(var row:new String[][]{{"guerreiro","Guerreiro: +0,5 tenacidade de armadura e +3% resistência a recuo"},{"arqueiro","Arqueiro: +2% movimento"},{"assassino","Assassino: +1% movimento e +2% velocidade de ataque"},{"mago","Mago: +0,25 tenacidade de armadura"}}) {
            var tag=net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.ITEM,new net.minecraft.util.Identifier("rpgstats","equipment/class_"+row[0]));
            if(stack.isIn(tag))event.getToolTip().add(net.minecraft.text.Text.literal(row[1]+" por peça no nv. 50").formatted(net.minecraft.util.Formatting.AQUA));
        }
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) { RPGStatsMod.registerCommands(event); }
    @SubscribeEvent public static void resources(AddReloadListenerEvent event) { DataDrivenRegistry.register(event); }
    @SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        com.rpgstats.compat.bosses.MariumMageProjectiles.install();
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void equipmentUse(PlayerInteractEvent event) {
        // The Somber Key is a native SwordItem, but its dungeon lock interaction is utility.
        if(event instanceof PlayerInteractEvent.RightClickBlock && net.minecraft.registry.Registries.ITEM.getId(event.getItemStack().getItem()).toString().equals("legendary_monsters:somber_key"))return;
        if (event.isCancelable() && event.getEntity() instanceof ServerPlayerEntity player
                && BossEquipmentService.rejectWithMessage(player,event.getItemStack())) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void equipmentAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player
                && BossEquipmentService.rejectWithMessage(player,player.getMainHandStack())) event.setCanceled(true);
    }

    /** Keep native pull time/velocity, but replace its very large additive arrow damage. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void nativeRangedAttributes(net.minecraftforge.event.ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();
        if(!net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getNamespace().equals("soulsweapons")
                || !com.rpgstats.combat.ArcherShotTracker.isRangedWeapon(stack))return;
        var id=new net.minecraft.util.Identifier("ranged_weapon","damage");
        if(net.minecraft.registry.Registries.ATTRIBUTE.containsId(id))
            event.removeAttribute(net.minecraft.registry.Registries.ATTRIBUTE.get(id));
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void nativeWeaponAttributes(net.minecraftforge.event.ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();
        String namespace=net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getNamespace();
        if((!namespace.equals("soulsweapons") && !namespace.equals("legendary_monsters"))
                || !(stack.getItem() instanceof net.minecraft.item.SwordItem))return;
        event.removeAttribute(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        event.removeAttribute(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_SPEED);
        if(event.getSlotType()!=net.minecraft.entity.EquipmentSlot.MAINHAND)return;
        var rules=DataDrivenRegistry.equipmentRules(stack).orElse(null);
        float factor=rules==null?1:rules.damageFactor();
        event.addModifier(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new net.minecraft.entity.attribute.EntityAttributeModifier(java.util.UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf"),"RPG native weapon budget",7*factor,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADDITION));
        event.addModifier(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_SPEED,
                new net.minecraft.entity.attribute.EntityAttributeModifier(java.util.UUID.fromString("fa233e1c-4180-4865-b01b-bcce9785aca3"),"RPG native weapon speed",-2.4,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void nativeArmorAttributes(net.minecraftforge.event.ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();String namespace=net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getNamespace();
        if(!java.util.Set.of("soulsweapons","legendary_monsters","better_weaponry").contains(namespace)
                || !(stack.getItem() instanceof net.minecraft.item.ArmorItem armor))return;
        var slot=armor.getSlotType();if(event.getSlotType()!=slot)return;
        double max=switch(slot){case CHEST->8;case LEGS->6;case HEAD->3;default->3;};
        var defense=net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR;
        var toughness=net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS;
        double value=event.getModifiers().get(defense).stream().mapToDouble(m->m.getValue()).sum();
        double tough=event.getModifiers().get(toughness).stream().mapToDouble(m->m.getValue()).sum();
        event.removeAttribute(defense);event.removeAttribute(toughness);
        event.removeAttribute(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        event.removeAttribute(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_SPEED);
        java.util.UUID id=java.util.UUID.nameUUIDFromBytes(("rpgstats.nativeArmor."+slot).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        event.addModifier(defense,new net.minecraft.entity.attribute.EntityAttributeModifier(id,"RPG armor budget",Math.min(max,Math.max(0,value)),net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADDITION));
        event.addModifier(toughness,new net.minecraft.entity.attribute.EntityAttributeModifier(id,"RPG toughness budget",Math.min(3,Math.max(0,tough)),net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity p) {
            forget(p); StatsApplier.apply(p); RPGStatsMod.syncStats(p);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity p) {
            EncounterManager.removePlayer(p.getUuid());
            forget(p);
        }
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayerEntity p) {
            // Our NBT mixin does not depend on Forge capabilities or their revive lifecycle.
            StatsManager.save(p, StatsManager.get(event.getOriginal()));
        }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity p) {
            forget(p);
            PlayerStats stats=StatsManager.get(p);
            stats.refreshResourceMax(); stats.stamina=stats.staminaMax;
            stats.concentration=0; stats.corruption=0; stats.temporalFragments=0;
            StatsManager.finish(p,stats);
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity p) {
            forget(p); StatsApplier.apply(p); RPGStatsMod.syncStats(p);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server=event.getServer();
        for (ServerPlayerEntity p:server.getPlayerManager().getPlayerList()) {
            CombatHandler.tickPlayer(p); StatsManager.tickRecovery(p);
            com.rpgstats.compat.ClassArmorBonuses.apply(p);
        }
        ProcDamageQueue.tick(server);
        BossScaler.tick(server);
        com.rpgstats.combat.WarriorSustain.tick(server);
        com.rpgstats.compat.BetterWeaponrySustain.tick(server);
        com.rpgstats.combat.ArcherAimAssist.tick(server);
    }

    /** Health loss confirmed after mitigation; called before lethal XP payment or on damage return. */
    public static void confirmedDamage(LivingEntity victim,DamageSource source,float actual) {
        if(victim.getWorld().isClient || !Float.isFinite(actual) || actual<=0)return;
        if(source.getAttacker() instanceof ServerPlayerEntity player && BossScaler.isCandidate(victim))
            EncounterManager.recordDamageDealt(player,victim,actual);
        else if(victim instanceof ServerPlayerEntity player && source.getAttacker() instanceof LivingEntity boss && BossScaler.isCandidate(boss))
            EncounterManager.recordDamageTaken(player,boss,actual);
    }

    @SubscribeEvent public static void load(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClient && event.getEntity() instanceof LivingEntity e) BossScaler.track(e);
    }
    @SubscribeEvent public static void unload(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClient && event.getEntity() instanceof LivingEntity e) {
            EncounterManager.removeBoss(e);
            BossScaler.untrack(e);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        for (ServerPlayerEntity p:event.getServer().getPlayerManager().getPlayerList()) forget(p);
        ProcDamageQueue.clear(); com.rpgstats.combat.ArcherShotTracker.clear();
        com.rpgstats.combat.WarriorSustain.clear();
        com.rpgstats.compat.BetterWeaponrySustain.clear();
        com.rpgstats.combat.ArcherAimAssist.clear(); com.rpgstats.integration.ArcherTrapCompat.clear(); com.rpgstats.combat.ArcherTechniqueHandler.clear();
        EncounterManager.clear(); BossScaler.clear(); RpgNetwork.clear();
    }

    /** Invoked after confirmed vanilla death, not from Forge's cancellable LivingDeathEvent. */
    public static void confirmedDeath(LivingEntity entity, DamageSource source) {
        if (entity.getWorld().isClient) return;
        if (entity instanceof ServerPlayerEntity dead) StatsManager.onPlayerDeath(dead);
        boolean boss=BossScaler.isCandidate(entity);
        ServerPlayerEntity killer=source.getAttacker() instanceof ServerPlayerEntity p?p:null;
        if(boss)EncounterManager.rewardCompletion(entity,killer);
        if (killer!=null) {
            if(!boss)StatsManager.addXpFromKill(killer,entity);
            CombatHandler.onPlayerKill(killer,entity);
        }
        if (BossScaler.isCandidate(entity)) EncounterManager.removeBoss(entity);
    }
    private static void forget(ServerPlayerEntity p) {
        com.rpgstats.combat.ArcherTechniqueHandler.remove(p.getUuid()); com.rpgstats.integration.ArcherTrapCompat.remove(p.getUuid()); com.rpgstats.combat.ArcherShotTracker.remove(p.getUuid()); CombatState.remove(p.getUuid()); MageCombatHandler.remove(p.getUuid()); RpgNetwork.forget(p.getUuid());
    }
    private ForgeEvents() {}
}
