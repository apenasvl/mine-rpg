package com.rpgstats.combat;

import com.rpgstats.balance.ClassBalance;
import com.rpgstats.classes.RPGClass;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.Stat;
import com.rpgstats.stats.StatsManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Stamina e exaustao universal. Esquiva universal foi removida na v2.4. */
public final class SoulslikeCombat {
    public static final float MELEE_COST = 9f;
    public static final float RANGED_COST = 6f;
    private static final int REGEN_DELAY_TICKS = 24;

    /** Compatibility point for the incoming-damage pipeline; no universal dodge/i-frames remain. */
    public static float modifyIncomingDamage(ServerPlayerEntity player, float amount, DamageSource source) {
        return amount;
    }

    /** Cobra stamina uma vez por ataque. Golpear exausto continua possivel, mas com dano reduzido. */
    public static float physicalAttackMultiplier(ServerPlayerEntity player, PlayerStats stats,
                                                 boolean projectile, boolean magic) {
        if (magic || stats.clazz == null || player.isCreative()) return 1f;
        CombatState state = CombatState.get(player.getUuid());
        if (state.lastAttackStaminaAge == player.age) return state.lastAttackStaminaMultiplier;

        float cost = projectile ? RANGED_COST : MELEE_COST;
        boolean mobileAttack=(stats.clazz==RPGClass.ARQUEIRO && projectile)
                || (stats.clazz==RPGClass.ASSASSINO && !projectile);
        if(mobileAttack)cost*=ClassBalance.mobileStaminaScale(stats.level);
        state.lastAttackStaminaAge = player.age;
        state.staminaRegenDelayTicks = mobileAttack ? ClassBalance.mobileRegenDelay(stats.level) : REGEN_DELAY_TICKS;
        if (stats.stamina + 0.001f < cost) {
            state.lastAttackStaminaMultiplier = 0.65f;
            if (state.exhaustionMessageTicks <= 0) {
                player.sendMessage(Text.literal("§cExausto: dano fisico reduzido."), true);
                state.exhaustionMessageTicks = 20;
            }
            return state.lastAttackStaminaMultiplier;
        }

        stats.stamina = Math.max(0f, stats.stamina - cost);
        state.lastAttackStaminaMultiplier = 1f;
        StatsManager.saveAndSync(player, stats);
        return 1f;
    }

    /** Atualiza a stamina quatro vezes por segundo para evitar trafego por tick. */
    public static boolean tickStamina(ServerPlayerEntity player, PlayerStats stats) {
        if (stats.clazz == null || player.age % 5 != 0) return false;
        CombatState state = CombatState.get(player.getUuid());
        float before = stats.stamina;
        boolean moving = player.getVelocity().horizontalLengthSquared() > 0.0025;

        if (!player.isCreative() && player.isSprinting() && moving) {
            boolean mobile=stats.clazz==RPGClass.ARQUEIRO || stats.clazz==RPGClass.ASSASSINO;
            stats.stamina = Math.max(0f, stats.stamina - 1.5f*(mobile ? ClassBalance.mobileStaminaScale(stats.level) : 1f));
            state.staminaRegenDelayTicks = Math.max(state.staminaRegenDelayTicks, mobile ? ClassBalance.mobileRegenDelay(stats.level) : REGEN_DELAY_TICKS);
            if (stats.stamina <= 0f) player.setSprinting(false);
        } else if (state.staminaRegenDelayTicks <= 0 && stats.stamina < stats.staminaMax) {
            int tenacity = stats.totalStats().getOrDefault(Stat.TENACIDADE, 0);
            stats.stamina = Math.min(stats.staminaMax,
                    stats.stamina + PlayerStats.staminaRegenPerSecond(tenacity) / 4f);
        }
        return Math.abs(before - stats.stamina) > 0.001f;
    }

    /** Persistent melee progression, before conditional House/spec bonuses and final caps. */
    public static float meleeAttributeMultiplier(PlayerStats stats) {
        var totals = stats.totalStats();
        if (stats.clazz == RPGClass.GUERREIRO)
            return ClassBalance.warriorMeleeMultiplier(stats.level, totals.getOrDefault(Stat.FORCA, 0), stats.hasNode("war_core_mastery"));
        if (stats.clazz == RPGClass.ASSASSINO)
            return ClassBalance.assassinMeleeMultiplier(stats.level, totals.getOrDefault(Stat.DESTREZA, 0), stats.hasNode("ass_core_mastery"));
        return 1f;
    }

    /** Dexterity, level and core mastery grow bow damage; House/spec windows remain conditional. */
    public static float rangedAttributeMultiplier(PlayerStats stats) {
        int dexterity = stats.totalStats().getOrDefault(Stat.DESTREZA, 0);
        if (stats.clazz == RPGClass.ARQUEIRO) {
            float bonus = Math.min(ClassBalance.ARCHER_DEX_DAMAGE_CAP,
                    dexterity * (ClassBalance.ARCHER_DEX_DAMAGE_CAP / 50f));
            bonus += ClassBalance.levelProgress(stats.level) * ClassBalance.ARCHER_LEVEL_DAMAGE_CAP;
            if (stats.hasNode("arc_core_mastery")) bonus += ClassBalance.ARCHER_CORE_MASTERY_DAMAGE_BONUS;
            return 1f + Math.min(ClassBalance.ARCHER_PERSISTENT_DAMAGE_CAP, Math.max(0f, bonus));
        }
        return 1f + Math.min(0.125f, dexterity * 0.0025f);
    }

    /**
     * Escala mágica base. Para o Mago, Inteligência precisa ser um eixo real de progressão,
     * então o multiplicador também incorpora nível RPG e Domínio do core. Os bônus condicionais
     * de Casa/especialização continuam em MageCombatHandler/IronsSpellsCompat e o cap final global
     * ainda impede multiplicadores absurdos.
     */
    public static float magicAttributeMultiplier(PlayerStats stats) {
        int intelligence = stats.totalStats().getOrDefault(Stat.INTELIGENCIA, 0);
        int faith = stats.totalStats().getOrDefault(Stat.FE, 0);
        int arcane = stats.totalStats().getOrDefault(Stat.ARCANO, 0);

        if (stats.clazz == RPGClass.MAGO) {
            float bonus = Math.min(MageBalance.MAGE_INTELLIGENCE_DAMAGE_CAP,
                    intelligence * (MageBalance.MAGE_INTELLIGENCE_DAMAGE_CAP / 50f));
            float levelProgress = Math.max(0f, Math.min(1f, (stats.level - 1f) / 49f));
            bonus += levelProgress * MageBalance.MAGE_LEVEL_DAMAGE_CAP;
            if (stats.hasNode("mag_core_mastery")) bonus += MageBalance.MAGE_CORE_MASTERY_DAMAGE_BONUS;
            if (stats.path == com.rpgstats.classes.RPGPath.MAGE_OCCULT) {
                bonus += Math.min(MageBalance.MAGE_OCCULT_ARCANE_DAMAGE_CAP,
                        arcane * (MageBalance.MAGE_OCCULT_ARCANE_DAMAGE_CAP / 50f));
            }
            return 1f + Math.min(MageBalance.MAGE_PERSISTENT_DAMAGE_CAP, Math.max(0f, bonus));
        }

        float bonus = Math.min(0.125f, intelligence * 0.0025f);
        if (stats.clazz == RPGClass.ASSASSINO) {
            bonus += Math.min(0.075f, arcane * 0.0015f);
        }
        return 1f + Math.min(0.22f, bonus);
    }

    public static float healingMultiplier(PlayerStats stats) {
        int faith = stats.totalStats().getOrDefault(Stat.FE, 0);
        return 1f + Math.min(0.20f, faith * 0.004f);
    }

    public static float afflictionDurationMultiplier(PlayerStats stats) {
        int arcane = stats.totalStats().getOrDefault(Stat.ARCANO, 0);
        return 1f + Math.min(0.60f, arcane * 0.012f);
    }

    private SoulslikeCombat() {}
}
