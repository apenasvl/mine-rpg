package com.rpgstats.integration;

import com.rpgstats.balance.GlobalCaps;
import com.rpgstats.classes.HouseRules;
import com.rpgstats.classes.RPGPath;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.Stat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;

/** Fachada pública: adaptadores consultam dados e scaling sem contornar o core. */
public final class IntegrationServices {
    public static final IntegrationServices INSTANCE = new IntegrationServices();

    public Optional<BossProfile> boss(LivingEntity entity) { return DataDrivenRegistry.boss(entity); }
    public int bossTier(LivingEntity entity) { return boss(entity).map(BossProfile::tier).orElse(0); }
    public float statusResistance(LivingEntity entity) { return boss(entity).map(BossProfile::statusResistance).orElse(0f); }
    public Optional<ItemProfile> weapon(ItemStack stack) { return DataDrivenRegistry.weapon(stack); }
    public Optional<ItemProfile> equipment(ItemStack stack) { return DataDrivenRegistry.equipment(stack); }
    public Optional<SpellProfile> spell(Identifier id) { return Optional.ofNullable(DataDrivenRegistry.spells().get(id)); }
    public RPGPath primaryHouse(PlayerStats stats) { return stats == null ? null : stats.path; }
    public RPGPath secondaryHouse(PlayerStats stats) { return stats == null ? null : stats.affinityHouse; }

    public boolean meetsRequirements(PlayerStats stats, ItemProfile item) {
        if (stats == null || item == null) return true;
        return stat(stats, Stat.FORCA) >= item.strength() && stat(stats, Stat.DESTREZA) >= item.dexterity()
                && stat(stats, Stat.INTELIGENCIA) >= item.intelligence() && stat(stats, Stat.FE) >= item.faith()
                && stat(stats, Stat.ARCANO) >= item.arcane();
    }

    /** Scaling aditivo normalizado; o cap global impede multiplicação entre mods. */
    public float weaponMultiplier(PlayerStats stats, ItemProfile item) {
        if (stats == null || item == null || !meetsRequirements(stats, item)) return 0.70f;
        float weighted = stat(stats,Stat.FORCA)*item.strength() + stat(stats,Stat.DESTREZA)*item.dexterity()
                + stat(stats,Stat.INTELIGENCIA)*item.intelligence() + stat(stats,Stat.FE)*item.faith()
                + stat(stats,Stat.ARCANO)*item.arcane();
        float weights = item.strength()+item.dexterity()+item.intelligence()+item.faith()+item.arcane();
        float bonus = weights <= 0 ? 0f : Math.min(.60f, weighted / weights / 100f);
        return GlobalCaps.damageMultiplier(1f + bonus);
    }

    /** Casa principal recebe força completa; Casa secundária usa o mesmo redutor do core. */
    public float spellMultiplier(PlayerStats stats, Identifier spellId) {
        SpellProfile spell = DataDrivenRegistry.spells().get(spellId);
        if (spell == null || stats == null) return 1f;
        float bonus = 0f;
        if (matchesHouse(stats.path, spell)) bonus += 0.25f;
        if (matchesHouse(stats.affinityHouse, spell)) bonus += 0.25f * HouseRules.passiveScale("magic_power");
        return GlobalCaps.damageMultiplier((1f + bonus) * spell.powerScale());
    }

    public float resourceCost(PlayerStats stats, Identifier spellId) {
        SpellProfile spell = DataDrivenRegistry.spells().get(spellId);
        if (spell == null) return 0f;
        return Math.max(0f, spell.baseResourceCost() * (1f + (stats == null ? 0f : HouseRules.surcharge(stats))));
    }

    private static boolean matchesHouse(RPGPath house, SpellProfile spell) {
        if (house == null) return false;
        return switch (house) {
            case MAGE_ELEMENTAL -> spell.has("elemental") || spell.has("fire") || spell.has("lightning") || spell.has("ice");
            case MAGE_ARCANA -> spell.has("arcane");
            case MAGE_CONJURATION -> spell.has("conjuration") || spell.has("summon");
            case MAGE_OCCULT -> spell.has("occult") || spell.has("curse") || spell.has("blood");
            case MAGE_TEMPORAL -> spell.has("temporal");
            default -> false;
        };
    }

    private static int stat(PlayerStats stats, Stat stat) { return stats.stats.getOrDefault(stat, 0); }

    private IntegrationServices() {}
}
