package com.rpgstats.compat.bosses;

import com.rpgstats.combat.ArcherShotTracker;
import com.rpgstats.compat.WeaponTypeResolver;
import com.rpgstats.compat.WeaponTypePolicy;
import com.rpgstats.integration.DataDrivenRegistry;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import java.util.Set;

/** Weapon affinity changes damage, never the player's class or unlocked abilities. */
public final class WeaponAffinity {
    public static float damageFactor(PlayerStats stats, ItemStack weapon) {
        String clazz=stats.clazz==null?null:stats.clazz.name();
        // Bow shape takes precedence over obsolete native sword/class metadata.
        if(ArcherShotTracker.isRangedWeapon(weapon))
            return EquipmentPolicy.classDamageFactor(clazz,Set.of("ARQUEIRO"));
        var rules=DataDrivenRegistry.equipmentRules(weapon).orElse(null);
        if(rules!=null)return EquipmentPolicy.classDamageFactor(clazz,rules.classes());
        if(WeaponTypeResolver.isDagger(weapon))return EquipmentPolicy.classDamageFactor(clazz,Set.of("ASSASSINO"));
        if(tagged(weapon,"staff"))return EquipmentPolicy.classDamageFactor(clazz,Set.of("MAGO"));
        if(WeaponTypeResolver.isTwoHanded(weapon))
            return EquipmentPolicy.classDamageFactor(clazz,Set.of("GUERREIRO"));
        if(WeaponTypeResolver.isNativeWeapon(weapon)) {
            if(WeaponTypeResolver.classify(weapon)==WeaponTypePolicy.Kind.MELEE)
                return EquipmentPolicy.classDamageFactor(clazz,Set.of("GUERREIRO"));
            // Unknown native metadata must never turn every SwordItem into Warrior affinity.
            return EquipmentPolicy.classDamageFactor(clazz,Set.of());
        }
        if(tagged(weapon,"greatsword") || weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem)
            return EquipmentPolicy.classDamageFactor(clazz,Set.of("GUERREIRO"));
        return 1f;
    }
    private static boolean tagged(ItemStack weapon,String category) {
        return weapon.isIn(TagKey.of(RegistryKeys.ITEM,new Identifier("rpgstats","weapons/"+category)));
    }
    private WeaponAffinity() {}
}
