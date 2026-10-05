package com.rpgstats.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rpgstats.RPGStatsMod;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reads native server data without loading optional Simply Swords or Better Combat classes. */
@Mod.EventBusSubscriber(modid = RPGStatsMod.MOD_ID)
public final class WeaponTypeResolver {
    private static volatile Map<Identifier, WeaponTypePolicy.Kind> snapshot = Map.of();
    private record Metadata(Identifier parent, String category, Boolean twoHanded) {}
    private record Resolved(String category, Boolean twoHanded) {}

    @SubscribeEvent
    public static void register(AddReloadListenerEvent event) {
        event.addListener((SynchronousResourceReloader) WeaponTypeResolver::reload);
    }

    public static boolean isDagger(ItemStack stack) {
        return tagged(stack, "dagger") || classify(stack) == WeaponTypePolicy.Kind.RAPID;
    }

    public static boolean isTwoHanded(ItemStack stack) {
        return tagged(stack, "greatsword") || tagged(stack, "two_handed")
                || classify(stack) == WeaponTypePolicy.Kind.TWO_HANDED;
    }

    public static WeaponTypePolicy.Kind classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WeaponTypePolicy.Kind.UNKNOWN;
        return snapshot.getOrDefault(Registries.ITEM.getId(stack.getItem()), WeaponTypePolicy.Kind.UNKNOWN);
    }

    public static boolean isNativeWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && Set.of("simplyswords","better_weaponry").contains(Registries.ITEM.getId(stack.getItem()).getNamespace());
    }

    /** Atomic replacement also removes stale classifications after a datapack reload. */
    public static void reload(ResourceManager manager) {
        Map<Identifier, Metadata> metadata = new HashMap<>();
        manager.findResources("weapon_attributes", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            String path = id.getPath().substring("weapon_attributes/".length(), id.getPath().length() - 5);
            Identifier key = new Identifier(id.getNamespace(), path);
            try (var reader = resource.getReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject attributes = json.has("attributes") ? json.getAsJsonObject("attributes") : new JsonObject();
                Identifier parent = json.has("parent") ? Identifier.tryParse(json.get("parent").getAsString()) : null;
                String category = attributes.has("category") ? attributes.get("category").getAsString() : null;
                Boolean twoHanded = attributes.has("two_handed") ? attributes.get("two_handed").getAsBoolean() : null;
                metadata.put(key, new Metadata(parent, category, twoHanded));
            } catch (Exception ex) {
                RPGStatsMod.LOGGER.warn("Invalid native weapon metadata {}: {}", id, ex.getMessage());
            }
        });
        Map<Identifier, WeaponTypePolicy.Kind> result = new HashMap<>();
        metadata.forEach((id, ignored) -> {
            if (!Set.of("simplyswords","better_weaponry").contains(id.getNamespace())) return;
            Resolved resolved = resolve(id, metadata, new HashSet<>());
            WeaponTypePolicy.Kind kind = WeaponTypePolicy.classify(
                    resolved.category() == null ? "" : resolved.category(), Boolean.TRUE.equals(resolved.twoHanded()));
            // Native compatibility resources use folders but register the basename as the item ID.
            String path = id.getPath();
            Identifier itemId = new Identifier(id.getNamespace(), path.substring(path.lastIndexOf('/') + 1));
            result.put(itemId, kind);
            if (kind == WeaponTypePolicy.Kind.UNKNOWN)
                RPGStatsMod.LOGGER.warn("Unclassified native weapon {}: conservative off-class affinity applies", itemId);
        });
        snapshot = Map.copyOf(result);
    }

    private static Resolved resolve(Identifier id, Map<Identifier, Metadata> metadata, Set<Identifier> visiting) {
        if (!visiting.add(id)) return new Resolved(null, null);
        Metadata value = metadata.get(id);
        if (value == null) {
            Boolean fallback = WeaponTypePolicy.defaultTwoHanded(id.toString());
            return new Resolved(fallback == null ? null : id.getPath(), fallback);
        }
        Resolved parent = value.parent() == null ? new Resolved(null, null) : resolve(value.parent(), metadata, visiting);
        return new Resolved(value.category() == null ? parent.category() : value.category(),
                value.twoHanded() == null ? parent.twoHanded() : value.twoHanded());
    }

    private static boolean tagged(ItemStack stack, String category) {
        return stack != null && !stack.isEmpty()
                && stack.isIn(TagKey.of(RegistryKeys.ITEM, new Identifier("rpgstats", "weapons/" + category)));
    }

    private WeaponTypeResolver() {}
}
