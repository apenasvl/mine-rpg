package com.rpgstats.gui;
import com.rpgstats.ability.*;
/** Numeric effects share one vocabulary; node descriptions retain conditions and limits. */
public final class EffectText {
    public static String describe(String nodeId, SkillEffect effect) {
        if (effect.type() == AbilityType.ACTIVE) {
            String summary = AbilityRegistry.activeSummary(nodeId);
            if (!summary.isBlank()) return summary;
        }
        float v = effect.value();
        return switch (effect.effectId()) {
            case "melee_damage" -> percent(v) + " dano corpo a corpo";
            case "ranged_damage" -> percent(v) + " dano a distancia";
            case "magic_power" -> percent(v) + " poder magico";
            case "lifesteal", "spell_lifesteal" -> percent(v) + " roubo de vida";
            case "damage_reduction" -> percent(v) + " reducao de dano";
            case "crit_chance", "spell_crit" -> percent(v) + " chance de critico";
            case "poison_hit" -> "Veneno em ataques (potencia " + clean(v) + ")";
            case "thorns" -> percent(v) + " espinhos";
            case "attack_speed" -> percent(v) + " velocidade de ataque";
            case "move_speed" -> percent(v) + " velocidade de movimento";
            case "resource_on_hit" -> "+" + clean(v) + " recurso ao acertar";
            case "resource_on_hurt" -> "+" + clean(v) + " recurso ao receber dano";
            case "heal_on_kill" -> "+" + clean(v) + " HP ao eliminar inimigo";
            case "mana_flat" -> "+" + clean(v) + " Mana maxima";
            case "mana_regen_flat" -> "+" + clean(v) + " Mana/s";
            case "mana_max_pct" -> percent(v) + " Mana maxima";
            case "mana_cost_reduction" -> percent(v) + " eficiencia de Mana";
            case "cooldown_recovery" -> percent(v) + " recuperacao de cooldown";
            case "elemental_damage" -> percent(v) + " dano elemental";
            case "reaction_damage" -> percent(v) + " dano de reacoes";
            case "summon_damage" -> percent(v) + " dano de invocacoes";
            case "summon_duration" -> percent(v) + " duracao/estabilidade de invocacoes";
            case "summon_defense" -> percent(v) + " defesa com invocacao proxima";
            case "summon_refund" -> percent(v) + " do custo devolvido ao expirar (gate 1s)";
            case "summon_diversity_damage" -> "+" + percent(v) + " dano por variedade de invocacoes (cap da skill)";
            case "astral_resonance" -> "+" + percent(v) + " poder por constructo diferente (cap da skill)";
            case "bond_flat" -> "+" + clean(v) + " Pontos de Vinculo";
            case "concentration_retention" -> percent(v) + " menos perda de Concentracao ao sofrer dano";
            case "concentration_reaction" -> "+" + clean(v) + " Concentracao por reacao (com cap por cast)";
            case "elemental_cost_reduction" -> percent(v) + " menor custo de Mana elemental";
            case "fire_damage_marked" -> percent(v) + " dano de fogo contra alvos aquecidos";
            case "fire_crit_hot" -> percent(v) + " critico de fogo contra alvos muito aquecidos";
            case "frozen_fragility" -> percent(v) + " dano no proximo golpe contra alvo congelado";
            case "reaction_aoe" -> "+" + clean(v) + " HP na explosao de reacao em area";
            case "shatter_damage" -> "+" + clean(v) + " HP na explosao ao estilhacar";
            case "chain_concentration" -> "+" + clean(v) + " Concentracao por salto de raio (com cap por cast)";
            case "rune_limit" -> "+" + clean(v) + " limite de runas";
            case "illusion_echo" -> percent(v) + " chance controlada de eco ilusorio";
            case "mage_echo" -> percent(v) + " chance de Eco Arcano (35% do dano; CD interno)";
            case "teleport_spell_bonus" -> percent(v) + " dano da proxima magia apos mobilidade espacial";
            case "curse_duration" -> percent(v) + " duracao de maldicoes";
            case "mana_regen_pct" -> percent(v) + " regeneracao de Mana quando a condicao da skill estiver ativa";
            case "stasis_magic_amp" -> percent(v) + " dano magico contra alvos em Stasis";
            case "temporal_second_chance" -> "Segunda Chance temporal com cooldown interno";
            case "hex_melee_magic" -> "+" + clean(v) + " dano magico em ataques imbuídos";
            case "spellblade_rhythm" -> percent(v) + " dano por stack do ritmo melee-spell-melee";
            case "temporal_fragment_max" -> "+" + clean(v) + " Fragmentos Temporais maximos";
            case "irons_counterspell_guard" -> percent(v)+" proteção curta após Counterspell bem-sucedido";
            default -> {
                String resolved=com.rpgstats.ability.ClassAbilityRegistry.describe(effect);
                yield resolved.startsWith(effect.effectId().replace('_',' '))?"":resolved;
            }
        };
    }

    private static String percent(float v){return (v>=0?"+":"")+Math.round(v*100f)+"%";}
    private static String clean(float v){return Math.abs(v-Math.round(v))<.001f?Integer.toString(Math.round(v)):String.format(java.util.Locale.ROOT,"%.1f",v);}
    private EffectText(){}
}
