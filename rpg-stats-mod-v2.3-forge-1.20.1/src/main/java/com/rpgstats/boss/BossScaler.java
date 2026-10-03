package com.rpgstats.boss;

import com.rpgstats.integration.BossProfile;
import com.rpgstats.integration.FixedBossProfile;
import com.rpgstats.compat.bosses.FixedBossPolicy;
import com.rpgstats.integration.IntegrationServices;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registro leve de bosses/encontros especiais.
 *
 * Phase 2d: scaling dinamico por nivel RPG medio e quantidade de participantes reais.
 * Contribution Score, Threat Score e profiles especificos de boss continuam fora desta fase.
 */
public final class BossScaler {
    /** Modifiers antigos, mantidos apenas para limpar saves de versoes anteriores. */
    private static final UUID HP_MOD = UUID.fromString("a1b2c3d4-1111-2222-3333-444455556666");
    private static final UUID DMG_MOD = UUID.fromString("a1b2c3d4-1111-2222-3333-444455556667");
    /** Modifiers separados para o scaling de nivel e de party. */
    private static final UUID LEVEL_HP_MOD = UUID.fromString("a1b2c3d4-1111-2222-3333-444455556668");
    private static final UUID PARTY_HP_MOD = UUID.fromString("a1b2c3d4-1111-2222-3333-444455556669");

    private static final UUID FIXED_HP_MOD = UUID.fromString("a1b2c3d4-1111-2222-3333-444455556670");

    private static final double LEVEL_SCALING_START = 10.0;
    private static final double LEVEL_SCALING_RANGE = 40.0;
    private static final double MAX_LEVEL_HEALTH_BONUS = 0.75;
    private static final double MAX_LEVEL_DAMAGE_BONUS = 0.30;

    /**
     * Party scaling usa diminishing returns. Somente participantes reais do EncounterState contam.
     * Acima de 8 participantes nao aumenta mais, evitando bosses absurdos em servidores grandes.
     */
    private static final int PARTY_SCALING_CAP = 8;
    private static final double FIRST_EXTRA_PLAYER_HEALTH_BONUS = 0.25;
    private static final double PARTY_HEALTH_DECAY = 0.72;
    private static final double MAX_PARTY_HEALTH_BONUS = 0.85;
    private static final double FIRST_EXTRA_PLAYER_DAMAGE_BONUS = 0.07;
    private static final double PARTY_DAMAGE_DECAY = 0.70;
    private static final double MAX_PARTY_DAMAGE_BONUS = 0.22;
    private static final double MAX_TOTAL_HEALTH_BONUS = 1.60;
    private static final float MAX_TOTAL_DAMAGE_MULTIPLIER = 1.50f;

    private static final Map<UUID, LivingEntity> TRACKED = new HashMap<>();
    private static final Map<UUID, Integer> TIERS = new HashMap<>();
    private static final Map<UUID, Float> DAMAGE_MULTIPLIERS = new HashMap<>();
    private static int tickCounter;

    public static void clear() {
        for (LivingEntity entity : new ArrayList<>(TRACKED.values())) clearEncounterScaling(entity);
        TRACKED.clear();
        TIERS.clear();
        DAMAGE_MULTIPLIERS.clear();
        tickCounter = 0;
    }

    public static void track(LivingEntity entity) {
        if (!isCandidate(entity)) return;
        tryScale(entity);
        if (TIERS.containsKey(entity.getUuid())) TRACKED.put(entity.getUuid(), entity);
    }

    public static void untrack(LivingEntity entity) {
        clearEncounterScaling(entity);
        TRACKED.remove(entity.getUuid());
        TIERS.remove(entity.getUuid());
        DAMAGE_MULTIPLIERS.remove(entity.getUuid());
    }

    /**
     * A cada 5 segundos, apenas valida bosses ja conhecidos e participantes ja registrados.
     * Nao varre players nem procura mobs novos. O atributo so muda quando o scaling calculado muda.
     */
    public static void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 100 != 0) return;
        for (LivingEntity entity : new ArrayList<>(TRACKED.values())) {
            if (entity.isRemoved() || !(entity.getWorld() instanceof ServerWorld)) {
                EncounterManager.removeBoss(entity);
                untrack(entity);
                continue;
            }
            EncounterManager.pruneInactive(entity);
        }
    }

    /**
     * Ao carregar o boss, limpa qualquer scaling persistido de builds anteriores e apenas classifica o encontro.
     * O scaling novo comeca quando houver participante real.
     */
    public static void tryScale(LivingEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld) || !isCandidate(entity)) return;

        normalizeLegacyScaling(entity);
        BossProfile profile = IntegrationServices.INSTANCE.boss(entity).orElse(null);
        int tier = profile == null ? classifyKnownVanilla(entity) : profile.tier();
        if (tier <= 0) {
            TIERS.remove(entity.getUuid());
            TRACKED.remove(entity.getUuid());
            return;
        }
        TIERS.put(entity.getUuid(), tier);
        if (profile != null && profile.fixed() != null) applyFixedScale(entity, profile.fixed(), 1);
        else removeFixedScale(entity);
    }

    /**
     * Combina nivel RPG medio + quantidade de participantes reais.
     * Nivel 1-10 permanece vanilla em solo. Party scaling tem diminishing returns e cap de 8 participantes.
     */
    public static void updateEncounterScaling(LivingEntity entity, EncounterState state) {
        if (entity == null || state == null || state.isEmpty() || !isCandidate(entity)) {
            clearEncounterScaling(entity);
            return;
        }

        BossProfile profile = IntegrationServices.INSTANCE.boss(entity).orElse(null);
        if (profile != null && profile.fixed() != null) {
            applyFixedScale(entity, profile.fixed(), state.participantCount());
            return;
        }
        removeFixedScale(entity);
        double averageLevel = state.averageLevel();
        int participantCount = state.participantCount();

        double levelHealthBonus = healthBonusForAverageLevel(averageLevel);
        double partyHealthBonus = partyHealthBonusForCount(participantCount);
        double targetHealthBonus = Math.min(MAX_TOTAL_HEALTH_BONUS, levelHealthBonus + partyHealthBonus);

        float levelDamageBonus = damageMultiplierForAverageLevel(averageLevel) - 1f;
        float partyDamageBonus = (float) partyDamageBonusForCount(participantCount);
        float targetDamageMultiplier = Math.min(MAX_TOTAL_DAMAGE_MULTIPLIER,
                1f + levelDamageBonus + partyDamageBonus);

        EntityAttributeInstance hp = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        EntityAttributeModifier currentLevelHp = hp == null ? null : hp.getModifier(LEVEL_HP_MOD);
        EntityAttributeModifier currentPartyHp = hp == null ? null : hp.getModifier(PARTY_HP_MOD);
        double currentHealthBonus = (currentLevelHp == null ? 0.0 : currentLevelHp.getValue())
                + (currentPartyHp == null ? 0.0 : currentPartyHp.getValue());
        float currentDamageMultiplier = DAMAGE_MULTIPLIERS.getOrDefault(entity.getUuid(), 1f);

        if (Math.abs(currentHealthBonus - targetHealthBonus) < 0.0001
                && Math.abs(currentDamageMultiplier - targetDamageMultiplier) < 0.0001f) return;

        float oldMax = Math.max(1f, entity.getMaxHealth());
        float ratio = Math.max(0f, Math.min(1f, entity.getHealth() / oldMax));

        removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, LEVEL_HP_MOD);
        removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, PARTY_HP_MOD);

        double cappedLevelHealth = Math.min(levelHealthBonus, targetHealthBonus);
        double cappedPartyHealth = Math.max(0.0, targetHealthBonus - cappedLevelHealth);
        addMultiplier(entity, EntityAttributes.GENERIC_MAX_HEALTH, LEVEL_HP_MOD,
                "RPGStats Encounter Level Scale", cappedLevelHealth);
        addMultiplier(entity, EntityAttributes.GENERIC_MAX_HEALTH, PARTY_HP_MOD,
                "RPGStats Encounter Party Scale", cappedPartyHealth);

        if (targetDamageMultiplier > 1.0001f) DAMAGE_MULTIPLIERS.put(entity.getUuid(), targetDamageMultiplier);
        else DAMAGE_MULTIPLIERS.remove(entity.getUuid());

        // Entrar/sair da luta nunca cura nem fere artificialmente: preserva exatamente a porcentagem atual.
        float newMax = Math.max(1f, entity.getMaxHealth());
        entity.setHealth(Math.max(0f, Math.min(newMax, newMax * ratio)));
    }

    /** Remove somente o scaling de encounter atual e preserva a porcentagem de vida. */
    public static void clearEncounterScaling(LivingEntity entity) {
        if (entity == null) return;
        BossProfile profile = IntegrationServices.INSTANCE.boss(entity).orElse(null);
        if (profile != null && profile.fixed() != null) {
            applyFixedScale(entity, profile.fixed(), 1);
            return;
        }
        removeFixedScale(entity);
        EntityAttributeInstance hp = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        boolean hasLevelHpScale = hp != null && hp.getModifier(LEVEL_HP_MOD) != null;
        boolean hasPartyHpScale = hp != null && hp.getModifier(PARTY_HP_MOD) != null;
        boolean hasDamageScale = DAMAGE_MULTIPLIERS.containsKey(entity.getUuid());
        if (!hasLevelHpScale && !hasPartyHpScale && !hasDamageScale) return;

        float oldMax = Math.max(1f, entity.getMaxHealth());
        float ratio = Math.max(0f, Math.min(1f, entity.getHealth() / oldMax));
        removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, LEVEL_HP_MOD);
        removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, PARTY_HP_MOD);
        DAMAGE_MULTIPLIERS.remove(entity.getUuid());
        float newMax = Math.max(1f, entity.getMaxHealth());
        entity.setHealth(Math.max(0f, Math.min(newMax, newMax * ratio)));
    }

    private static void applyFixedScale(LivingEntity entity, FixedBossProfile profile, int participants) {
        FixedBossPolicy.Scale scale = FixedBossPolicy.scale(profile, participants);
        EntityAttributeInstance hp = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (hp == null) return;
        EntityAttributeModifier current = hp.getModifier(FIXED_HP_MOD);
        double bonus = scale.healthFactor() - 1;
        boolean changed = current == null || Math.abs(current.getValue() - bonus) > .000001
                || hp.getModifier(LEVEL_HP_MOD) != null || hp.getModifier(PARTY_HP_MOD) != null;
        if (changed) {
            float ratio = Math.max(0, Math.min(1, entity.getHealth() / Math.max(1, entity.getMaxHealth())));
            hp.removeModifier(FIXED_HP_MOD);
            hp.removeModifier(LEVEL_HP_MOD);
            hp.removeModifier(PARTY_HP_MOD);
            // MULTIPLY_TOTAL follows native base/phase attribute changes without compounding reloads.
            hp.addPersistentModifier(new EntityAttributeModifier(FIXED_HP_MOD, "RPGStats Fixed Encounter",
                    bonus, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            entity.setHealth(Math.max(0, Math.min(entity.getMaxHealth(), entity.getMaxHealth() * ratio)));
        }
        DAMAGE_MULTIPLIERS.put(entity.getUuid(), scale.damageFactor());
    }

    private static void removeFixedScale(LivingEntity entity) {
        EntityAttributeInstance hp = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (hp == null || hp.getModifier(FIXED_HP_MOD) == null) return;
        float ratio = Math.max(0, Math.min(1, entity.getHealth() / Math.max(1, entity.getMaxHealth())));
        hp.removeModifier(FIXED_HP_MOD);
        entity.setHealth(Math.max(0, Math.min(entity.getMaxHealth(), entity.getMaxHealth() * ratio)));
    }

    static double healthBonusForAverageLevel(double averageLevel) {
        return MAX_LEVEL_HEALTH_BONUS * levelProgress(averageLevel);
    }

    static float damageMultiplierForAverageLevel(double averageLevel) {
        return 1f + (float) (MAX_LEVEL_DAMAGE_BONUS * levelProgress(averageLevel));
    }

    static double partyHealthBonusForCount(int participantCount) {
        return diminishingPartyBonus(participantCount, FIRST_EXTRA_PLAYER_HEALTH_BONUS,
                PARTY_HEALTH_DECAY, MAX_PARTY_HEALTH_BONUS);
    }

    static double partyDamageBonusForCount(int participantCount) {
        return diminishingPartyBonus(participantCount, FIRST_EXTRA_PLAYER_DAMAGE_BONUS,
                PARTY_DAMAGE_DECAY, MAX_PARTY_DAMAGE_BONUS);
    }

    private static double diminishingPartyBonus(int participantCount, double firstIncrement,
                                                double decay, double cap) {
        int cappedCount = Math.max(1, Math.min(PARTY_SCALING_CAP, participantCount));
        int extras = cappedCount - 1;
        double total = 0.0;
        double increment = firstIncrement;
        for (int i = 0; i < extras; i++) {
            total += increment;
            increment *= decay;
        }
        return Math.min(cap, total);
    }

    private static double levelProgress(double averageLevel) {
        double progress = (averageLevel - LEVEL_SCALING_START) / LEVEL_SCALING_RANGE;
        return Math.max(0.0, Math.min(1.0, progress));
    }

    /** Dano de boss contra jogadores apos scaling de nivel + party. 1.0 = vanilla. */
    public static float getDamageMultiplier(LivingEntity entity) {
        Float current = DAMAGE_MULTIPLIERS.get(entity.getUuid());
        if (current != null) return current;
        return IntegrationServices.INSTANCE.boss(entity).map(BossProfile::fixed)
                .map(FixedBossProfile::damageFactor).orElse(1f);
    }

    /** Compatibilidade com chamadas antigas: RPG Stats nao possui mais fases artificiais. */
    public static int getPhase(LivingEntity entity) {
        return 1;
    }

    public static int getTier(LivingEntity entity) {
        Integer tracked = TIERS.get(entity.getUuid());
        if (tracked != null) return tracked;
        BossProfile profile = IntegrationServices.INSTANCE.boss(entity).orElse(null);
        return profile == null ? classifyKnownVanilla(entity) : profile.tier();
    }

    /**
     * Somente bosses declarados por JSON/tag e os encontros vanilla conhecidos entram no sistema.
     * Deteccao generica/Threat Score vira separadamente para nao transformar mobs normais em bosses por acidente.
     */
    public static boolean isCandidate(LivingEntity entity) {
        if (entity == null) return false;
        if (IntegrationServices.INSTANCE.boss(entity).isPresent()) return true;
        return classifyKnownVanilla(entity) > 0;
    }

    private static int classifyKnownVanilla(LivingEntity entity) {
        if (entity instanceof EnderDragonEntity || entity instanceof WitherEntity) return 5;
        return entityId(entity).equals("minecraft:warden") ? 5 : 0;
    }

    public static int getXpReward(LivingEntity entity) {
        BossProfile profile = IntegrationServices.INSTANCE.boss(entity).orElse(null);
        if (profile != null) return profile.xp();
        return switch (getTier(entity)) {
            case 1 -> 120;
            case 2 -> 300;
            case 3 -> 700;
            case 4 -> 1400;
            case 5 -> 2500;
            default -> 0;
        };
    }

    public static float getStatusResistance(LivingEntity entity) {
        return IntegrationServices.INSTANCE.boss(entity).map(BossProfile::statusResistance).orElse(0f);
    }

    public static float getStaggerResistance(LivingEntity entity) {
        return IntegrationServices.INSTANCE.boss(entity).map(BossProfile::staggerResistance).orElse(0f);
    }

    private static String entityId(LivingEntity entity) {
        return Registries.ENTITY_TYPE.getId(entity.getType()).toString().toLowerCase();
    }

    /**
     * Remove HP/dano artificial de builds antigas e qualquer modifier de encounter persistido por shutdown anterior.
     * O encounter novo sempre recomeca limpo e server-authoritative.
     */
    private static void normalizeLegacyScaling(LivingEntity entity) {
        float oldMax = Math.max(1f, entity.getMaxHealth());
        float ratio = Math.max(0f, Math.min(1f, entity.getHealth() / oldMax));
        boolean changed = removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, HP_MOD);
        changed |= removeModifier(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, DMG_MOD);
        changed |= removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, LEVEL_HP_MOD);
        changed |= removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, PARTY_HP_MOD);
        DAMAGE_MULTIPLIERS.remove(entity.getUuid());
        if (changed) {
            float newMax = Math.max(1f, entity.getMaxHealth());
            entity.setHealth(Math.max(0f, Math.min(newMax, newMax * ratio)));
        }
    }

    private static boolean removeModifier(LivingEntity entity, EntityAttribute attribute, UUID id) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance == null || instance.getModifier(id) == null) return false;
        instance.removeModifier(id);
        return true;
    }

    private static void addMultiplier(LivingEntity entity, EntityAttribute attribute, UUID id, String name, double amount) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance == null || amount <= 0) return;
        instance.addPersistentModifier(new EntityAttributeModifier(
                id, name, amount, EntityAttributeModifier.Operation.MULTIPLY_BASE));
    }

    private BossScaler() {}
}

