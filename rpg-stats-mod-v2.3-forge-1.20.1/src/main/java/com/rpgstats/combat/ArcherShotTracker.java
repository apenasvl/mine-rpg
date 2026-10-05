package com.rpgstats.combat;

import com.rpgstats.RPGStatsMod;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Snapshots the actual launch weapon; all projectiles launched in one server tick share a shot.
 * Mods keep their own entities, ammo and native effects. Only RPG rewards are deduplicated.
 */
@Mod.EventBusSubscriber(modid = RPGStatsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArcherShotTracker {
    private static final String KEY = "rpgstats_bow_shot";
    private static final TagKey<net.minecraft.item.Item> BOWS = TagKey.of(RegistryKeys.ITEM, new Identifier("rpgstats", "weapons/bow"));
    private static final TagKey<net.minecraft.item.Item> CROSSBOWS = TagKey.of(RegistryKeys.ITEM, new Identifier("rpgstats", "weapons/crossbow"));
    private static final Map<UUID, java.util.ArrayDeque<ItemStack>> LAUNCHES = new HashMap<>();
    private static final Map<UUID, Rewards> REWARDS = new HashMap<>();
    private record Rewards(CombatState state, Map<Long, Long> shots) {}

    public static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem || isCrossbow(stack) || stack.isIn(BOWS);
    }
    public static boolean isCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem || stack.isIn(CROSSBOWS);
    }

    /** Scoped around the real release/shoot call, including custom BowItem overrides. */
    public static void beginLaunch(ServerPlayerEntity player, ItemStack weapon) {
        LAUNCHES.computeIfAbsent(player.getUuid(), ignored -> new java.util.ArrayDeque<>()).push(weapon.copy());
    }
    public static void endLaunch(ServerPlayerEntity player) {
        var scope = LAUNCHES.get(player.getUuid());
        if (scope == null) return;
        if (!scope.isEmpty()) scope.pop();
        if (scope.isEmpty()) LAUNCHES.remove(player.getUuid());
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void launched(EntityJoinLevelEvent event) {
        if (event.getLevel().isClient || !(event.getEntity() instanceof ProjectileEntity projectile)
                || !(projectile.getOwner() instanceof ServerPlayerEntity player)) return;
        // Loaded entities already carry the original weapon and shot, even after a relog.
        if (projectile.getPersistentData().contains(KEY)) return;
        // Native ItemSupplier is stripped on dedicated servers by @OnlyIn(CLIENT).
        // Original 1.1.3 registers <material>_shuriken_entity for <material>_shuriken.
        var nativeId=net.minecraft.registry.Registries.ENTITY_TYPE.getId(projectile.getType());
        if(nativeId.getNamespace().equals("better_weaponry") && nativeId.getPath().endsWith("_shuriken_entity")) {
            var itemId=new Identifier("better_weaponry",nativeId.getPath().substring(0,nativeId.getPath().length()-"_entity".length()));
            if(!net.minecraft.registry.Registries.ITEM.containsId(itemId))return;
            ItemStack weapon=new ItemStack(net.minecraft.registry.Registries.ITEM.get(itemId));
            if(player.getActiveItem().isOf(weapon.getItem()))weapon=player.getActiveItem().copy();
            else if(player.getMainHandStack().isOf(weapon.getItem()))weapon=player.getMainHandStack().copy();
            else if(player.getOffHandStack().isOf(weapon.getItem()))weapon=player.getOffHandStack().copy();
            NbtCompound shot=new NbtCompound();shot.putLong("tick",event.getLevel().getTime());shot.putUuid("owner",player.getUuid());
            shot.put("weapon",weapon.writeNbt(new NbtCompound()));shot.putBoolean("thrown",true);projectile.getPersistentData().put(KEY,shot);return;
        }
        var scope = LAUNCHES.get(player.getUuid());
        if (scope == null || scope.isEmpty()) return;
        ItemStack weapon = scope.peek();
        if (!isRangedWeapon(weapon)) return;
        NbtCompound shot = new NbtCompound();
        shot.putLong("tick", event.getLevel().getTime());
        shot.putUuid("owner", player.getUuid());
        shot.put("weapon", weapon.writeNbt(new NbtCompound()));
        projectile.getPersistentData().put(KEY, shot);
        if (projectile instanceof net.minecraft.entity.projectile.PersistentProjectileEntity arrow)
            ArcherAimAssist.onLaunch(player, arrow, weapon);
    }

    public static boolean isBowShot(DamageSource source) {
        return source.getSource() instanceof ProjectileEntity projectile
                && projectile.getPersistentData().contains(KEY)
                && !projectile.getPersistentData().getCompound(KEY).getBoolean("thrown")
                && !source.isIn(DamageTypeTags.IS_EXPLOSION) && !source.isIn(DamageTypeTags.IS_FIRE);
    }

    /** Datapacks can classify intrinsic AoE bows and ammunition without a mandatory mod API. */
    public static boolean hasNativeAreaEffect(DamageSource source) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return false;
        ItemStack weapon = launchWeapon(source, player);
        return weapon.isIn(TagKey.of(RegistryKeys.ITEM, new Identifier("rpgstats", "weapons/native_area")))
                || source.getSource() != null && source.getSource().getType().isIn(
                    TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier("rpgstats", "native_area_projectiles")));
    }

    public static boolean isNativeSecondary(DamageSource source) {
        return source.getSource() instanceof ProjectileEntity projectile
                && projectile.getPersistentData().contains(KEY)
                && (source.isIn(DamageTypeTags.IS_EXPLOSION) || source.isIn(DamageTypeTags.IS_FIRE));
    }

    /** Optional native enchantments require a verified launch, never the current held weapon. */
    public static java.util.Optional<ItemStack> storedLaunchWeapon(DamageSource source,ServerPlayerEntity player) {
        if(!(source.getSource() instanceof ProjectileEntity projectile) || !projectile.getPersistentData().contains(KEY))return java.util.Optional.empty();
        var shot=projectile.getPersistentData().getCompound(KEY);
        if(!shot.containsUuid("owner") || !player.getUuid().equals(shot.getUuid("owner")))return java.util.Optional.empty();
        var weapon=ItemStack.fromNbt(shot.getCompound("weapon"));return weapon.isEmpty()?java.util.Optional.empty():java.util.Optional.of(weapon);
    }

    public static ItemStack launchWeapon(DamageSource source, ServerPlayerEntity player) {
        if (source.getSource() instanceof ProjectileEntity projectile && projectile.getPersistentData().contains(KEY))
            return ItemStack.fromNbt(projectile.getPersistentData().getCompound(KEY).getCompound("weapon"));
        return player.getMainHandStack();
    }

    /** Only confirmed positive damage may claim a shot. Cancelled/immune hits claim nothing. */
    public static boolean claim(ServerPlayerEntity player, DamageSource source) {
        if (!isBowShot(source)) return true;
        if (source.getSource().getWorld() != player.getWorld()) return false;
        NbtCompound shot = source.getSource().getPersistentData().getCompound(KEY);
        if (!shot.containsUuid("owner") || !player.getUuid().equals(shot.getUuid("owner"))) return false;
        CombatState state = CombatState.get(player.getUuid());
        Rewards rewards = REWARDS.get(player.getUuid());
        if (rewards == null || rewards.state != state) {
            rewards = new Rewards(state, new HashMap<>());
            REWARDS.put(player.getUuid(), rewards);
        }
        long now = player.getWorld().getTime();
        if (now - shot.getLong("tick") > 1200) return false;
        rewards.shots.entrySet().removeIf(e -> e.getValue() < now);
        return rewards.shots.putIfAbsent(shot.getLong("tick"), now + 1200) == null;
    }

    public static void remove(UUID owner) { REWARDS.remove(owner); LAUNCHES.remove(owner); }
    public static void clear() { REWARDS.clear(); LAUNCHES.clear(); }
    private ArcherShotTracker() {}
}
