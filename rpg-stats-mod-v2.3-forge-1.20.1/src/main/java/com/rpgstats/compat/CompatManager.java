package com.rpgstats.compat;

import com.rpgstats.RPGStatsMod;
import com.rpgstats.integration.IntegrationServices;
import com.rpgstats.stats.PlayerStats;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Centraliza detecção de mods opcionais e evita referências externas no core. */
public final class CompatManager {
    private static final String IRONS_DAMAGE_SOURCE = "io.redspace.ironsspellbooks.damage.SpellDamageSource";
    private static final List<CompatModule> MODULES = List.of(
            descriptor("irons_spells", Set.of("irons_spellbooks"), "integração ativa: Mago, escolas, dano, Mana e recarga"),
            descriptor("combat_roll", Set.of("combatroll"), "rolamento nas tecnicas Survivalist/Guerrilla; custo e recarga RPG"),
            descriptor("immersive_armors", Set.of("immersive_armors"), "protecao e visuais; poderes substituidos pelas classes RPG"),
            descriptor("better_combat", Set.of("bettercombat"), "detectado; adaptador de combate pendente")
    );
    private static List<CompatModule> active = List.of();
    private static MageManaBridge mageManaBridge;

    public static void initialize() {
        ModList loader = ModList.get();
        if (loader.isLoaded("combatroll")) {
            PhysicalActionService.register(new com.rpgstats.compat.combatroll.CombatRollCompat());
            com.rpgstats.ability.ClassAbilityRegistry.setPhysicalDescription(id -> PhysicalActionPolicy.isRollNode(id)
                ? " Rolamento com Combat Roll: requer solo, fora da agua e sem usar item; custo e recarga desta tecnica. Use o slot RPG (R/Z/X/C)." : "");
        }
        List<CompatModule> found = new ArrayList<>();
        CompatContext context = new CompatContext(IntegrationServices.INSTANCE);
        for (CompatModule module : MODULES) {
            if (module.requiredMods().stream().allMatch(loader::isLoaded)) {
                module.register(context);
                if (module.id().equals("irons_spells") && !registerAdapter(
                        "com.rpgstats.compat.irons.IronsSpellsCompat")) {
                    continue;
                }
                found.add(module);
            }
        }
        active = Collections.unmodifiableList(found);
    }

    private static boolean registerAdapter(String className) {
        try {
            Class<?> type = Class.forName(className);
            type.getMethod("register").invoke(null);
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            RPGStatsMod.LOGGER.error("Falha ao ativar adaptador opcional {}", className, error);
            return false;
        }
    }

    public static void registerMageManaBridge(MageManaBridge bridge) {
        if (bridge == null) throw new IllegalArgumentException("bridge");
        mageManaBridge = bridge;
    }

    public static boolean usesExternalMageMana() { return mageManaBridge != null; }

    public static boolean syncMageMana(ServerPlayerEntity player, PlayerStats stats) {
        return mageManaBridge != null && mageManaBridge.sync(player, stats);
    }

    public static float mageMana(ServerPlayerEntity player, PlayerStats stats) {
        return mageManaBridge == null ? stats.resource : mageManaBridge.current(player);
    }

    public static void setMageMana(ServerPlayerEntity player, PlayerStats stats, float amount) {
        float clamped = Math.max(0f, Math.min(stats.resourceMax, amount));
        stats.resource = clamped;
        if (mageManaBridge != null) mageManaBridge.set(player, clamped);
    }

    public static void addMageMana(ServerPlayerEntity player, PlayerStats stats, float amount) {
        setMageMana(player, stats, mageMana(player, stats) + amount);
    }

    /** RPG techniques never interrupt or reuse Iron's internal cast data. */
    public static boolean canUseRpgTechnique(ServerPlayerEntity player) {
        return mageManaBridge == null || !mageManaBridge.isCasting(player);
    }

    public static int nativeSummonCount(ServerPlayerEntity player, PlayerStats stats) {
        return mageManaBridge == null ? 0 : mageManaBridge.nativeSummonCount(player, stats);
    }
    public static float nativeSummonDiversityBonus(ServerPlayerEntity player, PlayerStats stats) {
        return mageManaBridge == null ? 0f : mageManaBridge.nativeSummonDiversityBonus(player, stats);
    }
    public static boolean focusNativeSummons(ServerPlayerEntity player, PlayerStats stats,
                                            net.minecraft.entity.LivingEntity target) {
        return mageManaBridge != null && mageManaBridge.focusNativeSummons(player, stats, target);
    }
    public static boolean transferNativeSummons(ServerPlayerEntity player, PlayerStats stats) {
        return mageManaBridge != null && mageManaBridge.transferNativeSummons(player, stats);
    }
    public static float nativeIncomingMultiplier(ServerPlayerEntity player, PlayerStats stats) {
        return mageManaBridge == null ? 1f : mageManaBridge.nativeIncomingMultiplier(player, stats);
    }

    public static List<CompatModule> activeModules() { return active; }
    public static boolean isActive(String id) { return active.stream().anyMatch(module -> module.id().equals(id)); }

    /** Safe in the standalone core: compares class names without linking Iron's classes. */
    public static boolean isIronsSpellDamage(DamageSource source) {
        if (source == null) return false;
        for (Class<?> type = source.getClass(); type != null; type = type.getSuperclass()) {
            if (IRONS_DAMAGE_SOURCE.equals(type.getName())) return true;
        }
        return false;
    }

    private static CompatModule descriptor(String id, Set<String> mods, String summary) {
        return new CompatModule() {
            public String id() { return id; }
            public Set<String> requiredMods() { return mods; }
            public String featureSummary() { return summary; }
        };
    }
    private CompatManager() {}
}


