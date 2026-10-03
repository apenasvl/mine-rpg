package com.rpgstats.balance;

/**
 * Orçamentos de progressão persistente das classes.
 *
 * Estes valores não substituem as mecânicas condicionais de Casa/especialização. Eles definem
 * quanto poder bruto uma classe ganha apenas por nível/atributo/core antes das janelas de combate.
 * O Guerreiro continua sendo a referência de maior pancada física; classes de alcance/suporte
 * recebem orçamento menor em troca de segurança, controle ou utilidade.
 */
public final class ClassBalance {
    /** Arqueiro: Destreza é o eixo principal, mas sem transformar alcance em dano grátis. */
    public static final float ARCHER_DEX_DAMAGE_CAP = 0.35f;
    /** Progressão 1 -> 50 para que subir a classe importe mesmo sem trocar o arco. */
    public static final float ARCHER_LEVEL_DAMAGE_CAP = 0.35f;
    /** Recompensa pequena por completar o core antes das Casas. */
    public static final float ARCHER_CORE_MASTERY_DAMAGE_BONUS = 0.10f;
    /** 35% DEX + 35% nível + 10% domínio; alcance conserva custo/recarga nativos. */
    public static final float ARCHER_PERSISTENT_DAMAGE_CAP = 0.80f;

    /** Strength already adds flat attack damage; Warrior level growth is the missing axis. */
    public static float warriorMeleeMultiplier(int level, int strength, boolean mastery) {
        return 1f + .45f * levelProgress(level) + .20f * attributeProgress(strength) + (mastery ? .10f : 0f);
    }

    /** Dexterity must improve Assassin hits as well as attack speed. */
    public static float assassinMeleeMultiplier(int level, int dexterity, boolean mastery) {
        return 1f + .45f * levelProgress(level) + .55f * attributeProgress(dexterity) + (mastery ? .10f : 0f);
    }

    /** Conditional House/spec/critical bonuses need room above earned persistent damage. */
    public static float physicalDamageCeiling(int level) {
        return 2.25f + 1.25f * levelProgress(level);
    }

    /** Innate mobile roles: synchronized attributes, with bounded level/DEX growth. */
    public static float mobileMoveBonus(int level,int dexterity,boolean assassin) {
        return (assassin ? .28f : .22f)*levelProgress(level)*(.60f+.40f*attributeProgress(dexterity));
    }
    public static float archerCadenceBonus(int level,int dexterity) {
        return .35f*levelProgress(level)*(.65f+.35f*attributeProgress(dexterity));
    }
    public static float assassinSpeedBonus(int level,int dexterity) {
        return .12f*levelProgress(level)*(.65f+.35f*attributeProgress(dexterity));
    }
    public static float mobileStaminaScale(int level) { return 1f-.15f*levelProgress(level); }
    public static int mobileRegenDelay(int level) { return Math.round(24f-12f*levelProgress(level)); }
    /** Close boss combat; early levels still need equipment, defense and correct timing. */
    public static float warriorBossReduction(int level,int tenacity) {
        float progress=levelProgress(level);
        return progress*progress*(.28f+.07f*attributeProgress(tenacity));
    }
    /** All RPG guard layers together may mitigate at most 60%, before native armor. */
    public static float warriorBossDamage(float beforeClass,float afterGuards,float roleReduction) {
        return Math.max(beforeClass*.40f,afterGuards*(1f-Math.max(0f,Math.min(.35f,roleReduction))));
    }

    private static float attributeProgress(int points) {
        return Math.max(0f, Math.min(1f, points / 50f));
    }

    public static float levelProgress(int level) {
        return Math.max(0f, Math.min(1f, (level - 1f) / 49f));
    }

    private ClassBalance() {}
}
