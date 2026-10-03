package com.rpgstats.ability;

public enum AbilityType {
    /** Sempre ativo quando o nó está desbloqueado. */
    PASSIVE,
    /** Ativado manualmente (tecla de habilidade). */
    ACTIVE,
    /** Ativo até desligar ou acabar duração. */
    TOGGLE
}
