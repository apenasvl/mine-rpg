package com.rpgstats.combat;

/**
 * Fonte central dos limites globais de balanceamento do Mago.
 *
 * Os valores foram escolhidos para manter a classe acima do vanilla no endgame,
 * mas sem transformar uma única habilidade em uma solução universal. Ajustes de
 * playtest devem começar aqui antes de espalhar novos "magic numbers" pelo código.
 */
public final class MageBalance {
    public static final float BASE_MANA = 100f;
    public static final float MANA_PER_INTELLIGENCE = 2.0f;

    /**
     * Teto do pool produzido SOMENTE pelo RPG Stats quando ele precisa operar sem Iron's.
     * Quando Iron's está presente, equipamento/atributos externos não podem ser cortados por este valor;
     * apenas o bônus próprio do RPG é limitado a MAX_RPG_MANA_BONUS_WITH_IRONS.
     */
    public static final float MAX_MANA = 280f;
    public static final float MAX_RPG_MANA_BONUS_WITH_IRONS = MAX_MANA - BASE_MANA;
    public static final float BASE_MANA_REGEN_PER_SECOND = 3.0f;

    public static final float MAX_CONCENTRATION = 100f;
    public static final float MAX_CORRUPTION = 100f;

    /**
     * Progressão persistente de dano do Mago. Ela cresce por atributo + nível RPG + domínio do core,
     * mas continua separada dos bônus condicionais de Casa/especialização e do crítico mágico.
     * O conjunto chega a ~+88% em uma build geral lvl 50 e ~+93% na Oculta focada em Arcano.
     */
    public static final float MAGE_INTELLIGENCE_DAMAGE_CAP = 0.60f;
    public static final float MAGE_LEVEL_DAMAGE_CAP = 0.22f;
    public static final float MAGE_CORE_MASTERY_DAMAGE_BONUS = 0.06f;
    public static final float MAGE_OCCULT_ARCANE_DAMAGE_CAP = 0.05f;
    public static final float MAGE_PERSISTENT_DAMAGE_CAP = 0.93f;

    /** Bônus permanente da árvore; buffs temporários podem ultrapassar até TEMPORARY_CDR_CAP. */
    public static final float PERMANENT_CDR_CAP = 0.25f;
    public static final float TEMPORARY_CDR_CAP = 0.50f;
    public static final float PERMANENT_SPELL_CRIT_CAP = 0.20f;
    public static final float SITUATIONAL_SPELL_CRIT_CAP = 0.30f;
    public static final float MAGIC_POWER_CAP = 0.60f;
    public static final float SPELL_LIFESTEAL_CAP = 0.10f;
    public static final float MAX_MANA_COST_REDUCTION = 0.45f;

    /** Evita que perda de Concentração por DoT transforme a barra em algo impossível de manter. */
    public static final int CONCENTRATION_HURT_GATE_TICKS = 10;
    public static final float CONCENTRATION_HURT_MIN = 2f;
    public static final float CONCENTRATION_HURT_PER_DAMAGE = 0.75f;
    public static final float CONCENTRATION_HURT_MAX = 12f;

    /** Fluxo Compartilhado não pode ser usado para gerar Mana infinita com várias invocações. */
    public static final float SUMMON_MANA_PER_HIT = 0.40f;
    public static final float SUMMON_MANA_PER_SECOND_CAP = 2.0f;

    private MageBalance() {}
}
