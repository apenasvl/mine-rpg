package com.rpgstats.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rpgstats.RPGStatsMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Carrega todos os JSON sob data/&#42;/rpgstats. O snapshot é trocado apenas ao final do reload. */
public final class DataDrivenRegistry {
    private static volatile Snapshot snapshot = Snapshot.empty();

    public static void register(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener((net.minecraft.resource.SynchronousResourceReloader) manager -> snapshot = load(manager));
    }

    public static Optional<BossProfile> boss(LivingEntity entity) {
        Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
        BossProfile exact = snapshot.bosses.get(entityId);
        if (exact != null) return Optional.of(exact);
        for (Map.Entry<Identifier, BossProfile> entry : snapshot.bossTags.entrySet()) {
            if (entity.getType().isIn(TagKey.of(RegistryKeys.ENTITY_TYPE, entry.getKey()))) return Optional.of(entry.getValue());
        }
        return Optional.empty();
    }

    public static Optional<ItemProfile> weapon(ItemStack stack) { return item(stack, snapshot.weapons, snapshot.weaponTags); }
    public static Optional<ItemProfile> equipment(ItemStack stack) { return item(stack, snapshot.equipment, snapshot.equipmentTags); }
    public static Optional<EquipmentRules> equipmentRules(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        return Optional.ofNullable(snapshot.equipmentRules.get(Registries.ITEM.getId(stack.getItem())));
    }
    public static Map<Identifier, SpellProfile> spells() { return snapshot.spells; }

    private static Optional<ItemProfile> item(ItemStack stack, Map<Identifier, ItemProfile> exact,
                                               Map<Identifier, ItemProfile> tags) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        ItemProfile profile = exact.get(Registries.ITEM.getId(stack.getItem()));
        if (profile != null) return Optional.of(profile);
        for (Map.Entry<Identifier, ItemProfile> entry : tags.entrySet()) {
            if (stack.isIn(TagKey.of(RegistryKeys.ITEM, entry.getKey()))) return Optional.of(entry.getValue());
        }
        return Optional.empty();
    }

    private static Snapshot load(ResourceManager manager) {
        Mutable data = new Mutable();
        Map<Identifier, Resource> files = manager.findResources("rpgstats", id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
            try (BufferedReader reader = file.getValue().getReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                String type = text(root, "type", "").toLowerCase(Locale.ROOT);
                JsonArray entries = root.has("entries") ? root.getAsJsonArray("entries") : new JsonArray();
                for (JsonElement element : entries) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("Entry must be an object");
                    parseEntry(type, element.getAsJsonObject(), data);
                }
            } catch (Exception error) {
                RPGStatsMod.LOGGER.warn("Reload de integração recusado em {}; mantendo snapshot anterior: {}", file.getKey(), error.getMessage());
                return snapshot;
            }
        }
        RPGStatsMod.LOGGER.info("RPG Stats data: {} bosses, {} armas, {} equipamentos, {} magias",
                data.bosses.size() + data.bossTags.size(), data.weapons.size() + data.weaponTags.size(),
                data.equipment.size() + data.equipmentTags.size(), data.spells.size());
        return data.freeze();
    }

    private static void parseEntry(String type, JsonObject json, Mutable data) {
        Identifier id = identifier(text(json, "id", ""));
        Identifier tag = identifier(text(json, "tag", ""));
        if ((id == null) == (tag == null)) throw new IllegalArgumentException("Entry needs exactly one id or tag");
        if (type.equals("bosses")) {
            BossProfile p = new BossProfile(integer(json,"tier",2), integer(json,"xp",70), decimal(json,"health_bonus",.4f),
                    decimal(json,"damage_bonus",.1f), decimal(json,"health_per_player",.25f),
                    decimal(json,"damage_per_player",.05f), decimal(json,"status_resistance",.25f),
                    decimal(json,"stagger_resistance",.25f), fixed(json));
            if (id != null) putUnique(data.bosses,id,p); else putUnique(data.bossTags,tag,p);
        } else if (type.equals("equipment_rules")) {
            if (id == null) throw new IllegalArgumentException("Equipment rules require an exact item ID");
            Set<String> classes = new HashSet<>();
            if (json.has("classes")) for (JsonElement e : json.getAsJsonArray("classes"))
                classes.add(e.getAsString().toUpperCase(Locale.ROOT));
            Map<String,Integer> required = new LinkedHashMap<>();
            if (json.has("stats")) for (var e : json.getAsJsonObject("stats").entrySet())
                required.put(e.getKey(),e.getValue().getAsBigDecimal().intValueExact());
            putUnique(data.equipmentRules,id,new EquipmentRules(integer(json,"min_level",1),classes,required,
                    decimal(json,"damage_factor",1),decimal(json,"attack_speed_factor",1),bool(json,"enabled",true)));
        } else if (type.equals("spells") && id != null) {
            putUnique(data.spells,id, new SpellProfile(strings(json,"categories"), decimal(json,"resource_cost",0), decimal(json,"power_scale",1)));
        } else if (type.equals("weapons") || type.equals("equipment")) {
            ItemProfile p = new ItemProfile(strings(json,"categories"), decimal(json,"weight",0), integer(json,"str",0),
                    integer(json,"dex",0),integer(json,"int",0),integer(json,"faith",0),integer(json,"arc",0),
                    decimal(json,"stamina_cost",0),decimal(json,"attack_speed",1),decimal(json,"status_buildup",0));
            Map<Identifier,ItemProfile> exact = type.equals("weapons") ? data.weapons : data.equipment;
            Map<Identifier,ItemProfile> tags = type.equals("weapons") ? data.weaponTags : data.equipmentTags;
            if (id != null) putUnique(exact,id,p); else putUnique(tags,tag,p);
        }
    }

    private static FixedBossProfile fixed(JsonObject json) {
        if (!json.has("fixed")) return null;
        JsonObject o = json.getAsJsonObject("fixed");
        return new FixedBossProfile(integer(o,"min_level",1), integer(o,"max_level",50),
                integer(o,"reference_level",1), decimal(o,"health_factor",1), decimal(o,"damage_factor",1));
    }
    private static <T> void putUnique(Map<Identifier,T> map, Identifier id, T value) {
        if (map.putIfAbsent(id,value) != null) throw new IllegalArgumentException("Duplicate integration ID: " + id);
    }
    private static Identifier identifier(String value) {
        if (value.isBlank()) return null;
        Identifier id = Identifier.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid integration ID: " + value);
        return id;
    }
    private static boolean bool(JsonObject o,String key,boolean fallback) {
        if(!o.has(key))return fallback;
        var v=o.getAsJsonPrimitive(key);
        if(!v.isBoolean())throw new IllegalArgumentException("Expected boolean: "+key);
        return v.getAsBoolean();
    }
    private static String text(JsonObject o,String k,String d){return o.has(k)?o.get(k).getAsString():d;}
    private static int integer(JsonObject o,String k,int d){return o.has(k)?o.get(k).getAsBigDecimal().intValueExact():d;}
    private static float decimal(JsonObject o,String k,float d){
        float value=o.has(k)?o.get(k).getAsFloat():d;
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Nonfinite field: " + k);
        return value;
    }
    private static Set<String> strings(JsonObject o,String k){Set<String>s=new HashSet<>();if(o.has(k))for(JsonElement e:o.getAsJsonArray(k))s.add(e.getAsString().toLowerCase(Locale.ROOT));return s;}

    private record Snapshot(Map<Identifier,BossProfile> bosses,Map<Identifier,BossProfile> bossTags,
                            Map<Identifier,ItemProfile> weapons,Map<Identifier,ItemProfile> weaponTags,
                            Map<Identifier,ItemProfile> equipment,Map<Identifier,ItemProfile> equipmentTags,
                            Map<Identifier,SpellProfile> spells, Map<Identifier,EquipmentRules> equipmentRules) {
        static Snapshot empty(){return new Snapshot(Map.of(),Map.of(),Map.of(),Map.of(),Map.of(),Map.of(),Map.of(),Map.of());}
    }
    private static final class Mutable {
        final Map<Identifier,BossProfile> bosses=new LinkedHashMap<>(),bossTags=new LinkedHashMap<>();
        final Map<Identifier,ItemProfile> weapons=new LinkedHashMap<>(),weaponTags=new LinkedHashMap<>(),equipment=new LinkedHashMap<>(),equipmentTags=new LinkedHashMap<>();
        final Map<Identifier,SpellProfile> spells=new LinkedHashMap<>();
        final Map<Identifier,EquipmentRules> equipmentRules=new LinkedHashMap<>();
        Snapshot freeze(){return new Snapshot(Map.copyOf(bosses),Map.copyOf(bossTags),Map.copyOf(weapons),Map.copyOf(weaponTags),Map.copyOf(equipment),Map.copyOf(equipmentTags),Map.copyOf(spells),Map.copyOf(equipmentRules));}
    }
    private DataDrivenRegistry() {}
}

