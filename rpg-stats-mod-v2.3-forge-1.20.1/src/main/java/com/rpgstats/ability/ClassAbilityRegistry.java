package com.rpgstats.ability;

import com.rpgstats.classes.RPGPath;
import com.rpgstats.classes.RPGSpecialization;
import java.util.List;
import java.util.Locale;

/**
 * Registro das quatro classes nao-Mago apos a auditoria de identidade.
 *
 * A regra importante aqui e intencional: nodes mecanicos NAO viram buffs genericos por sufixo.
 * O estado e o comportamento desses nodes vivem em ClassMechanics e sao resolvidos pelo id real
 * do node/Caminho/Especializacao. Este registro guarda apenas stats honestos e marca as tecnicas.
 */
public final class ClassAbilityRegistry {
    public static final String CLASS_ACTIVE = "active_class_mechanic";

    private static java.util.function.Function<String,String> physicalDescription = id -> "";
    public static void setPhysicalDescription(java.util.function.Function<String,String> provider) {
        physicalDescription = provider == null ? id -> "" : provider;
    }
    private ClassAbilityRegistry() {}

    public static List<SkillEffect> get(String id) {
        if (!isExpandedClassNode(id)) return List.of();

        // Pequenos STAT nodes que continuam numericos de proposito.
        if (id.endsWith("_core_awakening")) return List.of(p("resource_flat", 3f));
        if (id.endsWith("_core_form")) return List.of(p(damageType(id), 0.02f));
        if (id.endsWith("_core_efficiency")) return List.of(p("resource_cost_reduction", 0.04f));
        if (id.endsWith("_core_power")) return List.of(p(damageType(id), 0.04f));
        if (id.endsWith("_economy")) return List.of(p("resource_cost_reduction", 0.05f));

        // Gates nao possuem efeito escondido.
        if (id.endsWith("_core_mastery") || id.endsWith("_mastery")) return List.of();

        // Tecnicas sao despachadas pelo id real em ClassMechanics. O valor e uma escala de magnitude,
        // portanto a Casa secundaria pode reduzi-lo via HouseRules sem perder a identidade da tecnica.
        if (id.endsWith("_core_utility")) {
            int duration = id.startsWith("arc_") ? 95 : 65;
            return List.of(active(1f, duration, 260, 12f));
        }
        if (id.endsWith("_technique")) {
            boolean spec = isSpecializationNode(id);
            return List.of(active(1f, spec ? (id.startsWith("arc_") ? 100 : 75) : 85, spec ? 280 : 240, spec ? 20f : 18f));
        }
        if (id.endsWith("_signature")) {
            boolean spec = isSpecializationNode(id);
            return List.of(active(1f, spec ? (id.startsWith("arc_") ? 120 : 125) : 135, spec ? 480 : 420, spec ? 30f : 28f));
        }
        if (id.endsWith("_ascension")) return List.of(active(1f, id.startsWith("arc_") ? 180 : 175, 1200, 32f));

        // Foundation/discipline/setup/reaction/synergy/initiation/engine/conversion/risk sao
        // mechanics/modifiers server-authoritative lidos diretamente por ClassMechanics.
        return List.of();
    }

    public static boolean isExpandedClassNode(String id) {
        return id != null && (id.startsWith("war_") || id.startsWith("arc_")
                || id.startsWith("ass_"));
    }

    private static boolean isSpecializationNode(String id) {
        int underscores = 0;
        for (int i = 0; i < id.length(); i++) if (id.charAt(i) == '_') underscores++;
        return underscores >= 3 && !id.contains("_core_");
    }

    private static String damageType(String id) {
        if (id.startsWith("arc_")) return "ranged_damage";
        return "melee_damage";
    }

    /** Tooltip honesto: mecanica anunciada aqui precisa existir em ClassMechanics. */
    public static String description(String id) {
        if (!isExpandedClassNode(id)) return "";

        String archer = archerDescription(id);
        if (!archer.isEmpty()) return archer + physicalDescription.apply(id);

        String assassin = assassinDescription(id);
        if (!assassin.isEmpty()) return assassin;
        if (id.endsWith("_core_mastery")) return "KEYSTONE: libera a escolha da Casa principal no nivel 10.";
        if (id.endsWith("_mastery")) return "KEYSTONE: libera uma especializacao desta Casa no nivel 25.";

        if (id.endsWith("_core_awakening")) return "+3 recurso maximo; desbloqueia o loop de recurso da classe.";
        if (id.endsWith("_core_form")) return statText(id, 2);
        if (id.endsWith("_core_efficiency")) return "STAT: -4% custo das tecnicas da classe, respeitando o cap.";
        if (id.endsWith("_core_power")) return statText(id, 4);
        if (id.endsWith("_economy")) return "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";

        String warrior = warriorSpecializationDescription(id);
        if (!warrior.isEmpty()) return warrior;

        String mechanic = mechanicName(id);
        if (id.endsWith("_core_flow")) return "MECANICA: gera recurso por " + coreVerb(id) + ", nao por um gatilho universal.";
        if (id.endsWith("_core_guard")) return "MODIFICADOR: defesa condicionada a " + coreDefense(id) + ".";
        if (id.endsWith("_core_tempo")) return "MODIFICADOR: recompensa o ritmo proprio de " + className(id) + ".";
        if (id.endsWith("_core_resolve")) return "MECANICA: resposta defensiva de emergencia propria da classe, com recarga interna.";
        if (id.endsWith("_core_combo")) return "MECANICA: completa uma sequencia valida do loop de " + className(id) + " para gerar recurso.";
        if (id.endsWith("_core_sustain")) return "MODIFICADOR: marcos de combate e eliminacoes devolvem recurso sem depender de farm de mobs.";
        if (id.endsWith("_core_utility")) return "ATIVA: tecnica-base de " + className(id) + "; modifica combate normal em vez de copiar uma ultimate.";

        if (id.endsWith("_foundation")) return "MECANICA: ativa " + mechanic + " como estado central desta Casa.";
        if (id.endsWith("_discipline")) return "MODIFICADOR: melhora a consistencia de " + mechanic + " sem ser apenas +dano.";
        if (id.endsWith("_setup")) return "MECANICA: cumprir a condicao de " + mechanic + " prepara a proxima decisao da Casa.";
        if (id.endsWith("_reaction")) return "MODIFICADOR: reagir no timing correto transforma " + mechanic + " em defesa/posicionamento.";
        if (id.endsWith("_synergy")) return "MODIFICADOR: conecta a tecnica da Casa com " + mechanic + ".";
        if (id.endsWith("_initiation")) return "MECANICA: inicia o motor de " + specializationName(id) + ".";
        if (id.endsWith("_engine")) return "MECANICA: gera a carga/condicao exclusiva de " + specializationName(id) + ".";
        if (id.endsWith("_conversion")) return "MODIFICADOR: converte o estado da especializacao em outro beneficio com trade-off.";
        if (id.endsWith("_risk")) return "MECANICA: risco/recompensa especifico de " + specializationName(id) + ", com limite contra chefes.";
        if (id.endsWith("_technique")) return "ATIVA: tecnica caracteristica ligada a " + mechanic + "; nao usa o antigo fallback generico.";
        if (id.endsWith("_signature")) return "ATIVA: consome/prepara " + mechanic + " de forma diferente da tecnica basica.";
        if (id.endsWith("_ascension")) return "ASCENSAO: altera temporariamente as regras de " + specializationName(id) + "; nao e apenas mais dano/duracao.";
        return "MECANICA de classe resolvida pelo id do node.";
    }

    /**
     * Compatibilidade para telas que descrevem um efeito ja resolvido. Nao decide a identidade
     * do node por sufixo/substrings; apenas traduz o SkillEffect executavel recebido.
     */
    public static String describe(SkillEffect effect) {
        if (effect == null) return "";
        if (effect.type() == AbilityType.ACTIVE) {
            if (CLASS_ACTIVE.equals(effect.effectId())) {
                return "Tecnica de classe: comportamento definido pela Casa/Especializacao do node.";
            }
            return AbilityRegistry.activeSummaryForEffect(effect);
        }
        float v = effect.value();
        String pct = String.format(Locale.ROOT, "%.1f%%", v * 100f);
        return switch (effect.effectId()) {
            case "resource_flat" -> "+" + clean(v) + " recurso maximo";
            case "resource_cost_reduction" -> "-" + pct + " custo de tecnicas";
            case "melee_damage" -> "+" + pct + " dano corpo a corpo";
            case "ranged_damage" -> "+" + pct + " dano a distancia";
            case "magic_power" -> "+" + pct + " poder magico";
            case "damage_reduction" -> "+" + pct + " reducao de dano";
            case "lifesteal" -> pct + " roubo de vida fisico";
            case "crit_chance" -> "+" + pct + " chance de critico fisico";
            case "attack_speed" -> "+" + pct + " velocidade de ataque";
            case "move_speed" -> "+" + pct + " velocidade de movimento";
            case "resource_on_hit" -> "+" + clean(v) + " recurso por acerto fisico";
            case "resource_on_hurt" -> "+" + clean(v) + " recurso ao receber dano";
            case "resource_on_kill" -> "+" + clean(v) + " recurso por eliminacao";
            case "heal_on_kill" -> "+" + clean(v) + " HP por eliminacao";
            case "poison_hit" -> "Veneno em acerto fisico";
            default -> effect.effectId().replace('_', ' ') + " " + clean(v);
        };
    }

    private static String clean(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001f) return Integer.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String statText(String id, int pct) {
        String type = id.startsWith("arc_") ? "dano a distancia" : "dano corpo a corpo";
        return "STAT: +" + pct + "% " + type + ".";
    }

    private static String className(String id) {
        if (id.startsWith("war_")) return "Guerreiro";
        if (id.startsWith("arc_")) return "Arqueiro";
        if (id.startsWith("ass_")) return "Assassino";
        return "Classe";
    }

    private static String coreVerb(String id) {
        if (id.startsWith("war_")) return "impacto, guarda e pressao";
        if (id.startsWith("arc_")) return "precisao, distancia e posicionamento";
        if (id.startsWith("ass_")) return "aberturas, combo e reposicionamento";
        return "mecanica de classe";
    }

    private static String coreDefense(String id) {
        if (id.startsWith("war_")) return "guarda/compromisso frontal";
        if (id.startsWith("arc_")) return "manter distancia ou reposicionar";
        if (id.startsWith("ass_")) return "criar uma rota de saida";
        return "defesa e posicionamento";
    }

    private static String mechanicName(String id) {
        if (id.startsWith("war_van_")) return "Guarda";
        if (id.startsWith("war_bers_")) return "Furia/Ferida";
        if (id.startsWith("war_weap_")) return "Abertura de Arma";
        if (id.startsWith("war_rune_")) return "Carga Runica";
        if (id.startsWith("war_cmd_")) return "Moral/Ordem";
        if (id.startsWith("arc_mark_")) return "Mira";
        if (id.startsWith("arc_ward_")) return "Presa/Instinto";
        if (id.startsWith("arc_skirm_")) return "Momentum";
        if (id.startsWith("arc_magic_")) return "Marca Elemental";
        if (id.startsWith("arc_art_")) return "Carga de Dispositivo";
        if (id.startsWith("ass_shadow_")) return "Abertura Sombria";
        if (id.startsWith("ass_venom_")) return "Doses";
        if (id.startsWith("ass_duel_")) return "Vantagem/Riposta";
        if (id.startsWith("ass_sabo_")) return "Preparacao/Dispositivo";
        if (id.startsWith("ass_myst_")) return "Eco de Alma";
        return coreVerb(id);
    }

    private static String specializationName(String id) {
        String[] p = id.toLowerCase(Locale.ROOT).split("_");
        if (p.length < 4) return mechanicName(id);
        String key = p[0] + "_" + p[1] + "_" + p[2];
        return switch (key) {
            case "war_van_bulw" -> "Bastiao"; case "war_van_lord" -> "Senhor da Guerra"; case "war_van_jugg" -> "Juggernaut";
            case "war_bers_reav" -> "Saqueador de Sangue"; case "war_bers_rage" -> "Nascido da Furia"; case "war_bers_pain" -> "Colosso da Dor";
            case "war_weap_blade" -> "Mestre das Laminas"; case "war_weap_duel" -> "Mestre do Duelo"; case "war_weap_titan" -> "Quebra-Titas";
            case "war_rune_knight" -> "Cavaleiro Runico"; case "war_rune_break" -> "Quebra-Feiticos"; case "war_rune_storm" -> "Lamina da Tempestade";
            case "war_cmd_banner" -> "Porta-Estandarte"; case "war_cmd_tact" -> "Estrategista"; case "war_cmd_guard" -> "Guarda de Ferro";
            case "arc_mark_snipe" -> "Franco-Atirador"; case "arc_mark_dead" -> "Olho Mortal"; case "arc_mark_ball" -> "Balistico";
            case "arc_ward_beast" -> "Mestre das Feras"; case "arc_ward_trap" -> "Armadilheiro"; case "arc_ward_surv" -> "Sobrevivencialista";
            case "arc_skirm_wind" -> "Corredor do Vento"; case "arc_skirm_acro" -> "Acrobata"; case "arc_skirm_guer" -> "Guerrilheiro";
            case "arc_magic_fire" -> "Arco Igneo"; case "arc_magic_frost" -> "Arco Glacial"; case "arc_magic_storm" -> "Arco da Tempestade";
            case "arc_art_cross" -> "Besteiro"; case "arc_art_bomb" -> "Bombardeiro"; case "arc_art_eng" -> "Engenheiro";
            case "ass_shadow_night" -> "Lamina Noturna"; case "ass_shadow_phant" -> "Fantasma"; case "ass_shadow_exec" -> "Executor";
            case "ass_venom_alch" -> "Alquimista"; case "ass_venom_plague" -> "Arauto da Peste"; case "ass_venom_toxic" -> "Toxicologista";
            case "ass_duel_fence" -> "Esgrimista"; case "ass_duel_dance" -> "Dancarino de Laminas"; case "ass_duel_counter" -> "Contra-Lamina";
            case "ass_sabo_demo" -> "Demolidor"; case "ass_sabo_infil" -> "Infiltrador"; case "ass_sabo_wire" -> "Mestre dos Fios";
            case "ass_myst_hex" -> "Cacador de Bruxos"; case "ass_myst_soul" -> "Lamina da Alma"; case "ass_myst_void" -> "Andarilho do Vazio";
            default -> mechanicName(id);
        };
    }

    /**
     * Resumo curto das ativas das classes expandidas. Os tempos refletem os timers realmente
     * iniciados por ClassMechanics. O scale preserva a reducao aplicada a uma Casa secundaria.
     */
    public static String activeSummary(String nodeId, float scale) {
        String real = nodeId == null ? "" : nodeId;
        float safeScale = Math.max(.45f, Math.min(1f, scale));

        if (real.endsWith("_core_utility")) {
            if (real.startsWith("war_")) return "Contra-pressao · guarda " + seconds(Math.round(65 * safeScale));
            if (real.startsWith("arc_")) return "Tiro Preparado · proximo projetil +8% · janela " + seconds(Math.round(95 * safeScale));
            if (real.startsWith("ass_")) return "Reposicionar · Abertura " + seconds(Math.round(65 * safeScale))
                    + " · saida " + seconds(Math.round(45 * safeScale));
        }

        RPGPath path = RPGPath.ownerOfNode(real);
        if (path != null && (real.endsWith("_technique") || real.endsWith("_signature"))) {
            boolean signature = real.endsWith("_signature");
            int ticks = Math.round((signature ? 135 : 85) * safeScale);
            return switch (path) {
                case ARC_MARKSMAN -> "Tiro Preparado · " + seconds(ticks);
                case ARC_WARDEN -> "Cacada + Instinto · " + seconds(ticks);
                case ARC_SKIRMISHER -> "Fluxo de Movimento · janela base " + seconds(ticks);
                case ARC_ARCANE -> signature ? "Reacao Elemental · +3,5% dano · " + seconds(ticks) : "Alterna Fogo/Gelo/Tempestade";
                case ARC_ARTIFICER -> (signature ? "Zona de Dispositivo" : "Dispositivo Pronto") + " · " + seconds(ticks);
                case ASS_SHADOW -> "Abertura Sombria · " + seconds(ticks) + " · invisibilidade curta";
                case ASS_VENOM -> signature ? "Veneno Consumivel · " + seconds(ticks) + " · sem payoff atual" : "Alterna Desgaste/Potencia/Controle";
                case ASS_DUELIST -> (signature ? "Riposta Armada" : "Parry") + " · " + seconds(ticks);
                case ASS_SABOTEUR -> (signature ? "Dispositivo + Demolicao" : "Dispositivo Armado") + " · " + seconds(ticks);
                case ASS_MYSTIC -> "Golpe da Alma · janela base " + seconds(ticks);
                default -> (signature ? "Assinatura" : "Tecnica") + " · " + seconds(ticks);
            };
        }

        RPGSpecialization spec = RPGSpecialization.ownerOfNode(real);
        if (spec != null && (real.endsWith("_technique") || real.endsWith("_signature") || real.endsWith("_ascension"))) {
            boolean signature = real.endsWith("_signature");
            boolean ascension = real.endsWith("_ascension");
            int ticks = Math.round((ascension ? 175 : signature ? 125 : 75) * safeScale);
            if (real.startsWith("arc_")) return archerSpecActiveText(real.substring(0, real.lastIndexOf('_')), signature, ascension);
            if (spec == RPGSpecialization.COUNTERBLADE)
                return "Parry · max. " + seconds(Math.min(45, ticks)) + " · Riposta apos defesa valida";
            if (spec == RPGSpecialization.PHANTOM)
                return "Fase · reducao por max. " + seconds(Math.min(45, ticks)) + " · Saida " + seconds(ticks);
            if (spec == RPGSpecialization.DEADEYE)
                return (signature ? "+1,7" : "+0,8") + " Precisao imediata";
            if (spec == RPGSpecialization.ALCHEMIST || spec == RPGSpecialization.TOXICOLOGIST)
                return "Alterna formula · janela interna " + seconds(ticks);
            if (spec == RPGSpecialization.BALLISTICIAN)
                return "Janela Balistica " + seconds(ticks) + " · acelera a proxima perfuracao";
            return (ascension ? "Ascensao" : signature ? "Dominio" : "Tecnica") + " de " + spec.display
                    + " · " + seconds(ticks);
        }
        return "Tecnica de classe";
    }

    private static String seconds(int ticks) {
        float value = Math.max(0, ticks) / 20f;
        if (Math.abs(value - Math.round(value)) < .001f) return Integer.toString(Math.round(value)) + "s";
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        return text + "s";
    }

    /** Descricoes do Arqueiro auditadas contra ClassMechanics/ArcherSpecializationHandler. */
    private static String archerDescription(String id) {
        if (id == null || !id.startsWith("arc_")) return "";
        if (id.startsWith("arc_core_")) {
            String suffix = suffix(id);
            return switch (suffix) {
                case "awakening" -> "STAT: +3 Foco maximo; habilita o loop de recurso do Arqueiro.";
                case "flow" -> "MECANICA: +25% de Foco gerado por acertos validos.";
                case "form" -> "STAT: +2% dano a distancia.";
                case "guard" -> "DEFESA: -4% dano recebido de atacantes a 7+ blocos.";
                case "efficiency" -> "STAT: -4% custo das tecnicas da classe, respeitando o cap.";
                case "tempo" -> "RITMO: usar uma tecnica arma 4s; o proximo projetil recebe +3% dano e +0,45 Foco, consumindo a janela.";
                case "resolve" -> "DETERMINACAO: abaixo de 40% de vida, recebe -8% dano por 3s; recarga interna de 20s.";
                case "combo" -> "MECANICA: a cada terceiro acerto valido, gera +0,6 Foco e reinicia a sequencia.";
                case "utility" -> "ATIVA: Tiro Preparado por 4,75s; +1,2 Foco e o proximo projetil recebe +8% dano.";
                case "power" -> "STAT: +4% dano a distancia.";
                case "sustain" -> "MODIFICADOR: +35% ao Foco recebido por eliminacoes validas.";
                case "mastery" -> "KEYSTONE: libera a escolha da Casa principal no nivel 10.";
                default -> "";
            };
        }

        String path = archerPathDescription(id);
        if (!path.isEmpty()) return path;
        return archerSpecializationDescription(id);
    }

    private static String archerPathDescription(String id) {
        String suffix = suffix(id);
        if (id.substring(0, id.lastIndexOf('_')).equals("arc_mark")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: projeteis entre 9 e 28 blocos geram 0,9 Mira; fora da faixa perdem 1,2. Mira concede ate +8% dano nessa faixa. Arco totalmente puxado: correcao suave em inimigo visivel a 8-28 blocos e ate 6 graus da mira; custa 6 Foco ao travar, recarga 2s, uma flecha por disparo, ate 8 ticks e 1,5 grau/tick. Nao guia bestas ou armas de area.";
                case "discipline" -> "MODIFICADOR: +20% na Mira gerada pelos acertos validos do Atirador.";
                case "setup" -> "PREPARACAO: com 3+ Mira, um acerto arma Tiro Preparado por 4s; o proximo projetil recebe +8% dano.";
                case "technique" -> "ATIVA: arma Tiro Preparado por 4,25s.";
                case "reaction" -> "REACAO: usar Tecnica/Assinatura concede 5% reducao de dano por 2,25s.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo projetil em ate 4s gera +0,6 Mira adicional.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: +1,8 Mira e Tiro Preparado por 6,75s.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Atirador no nivel 25.";
                default -> "";
            };
        }
        if (id.substring(0, id.lastIndexOf('_')).equals("arc_ward")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: cada projetil marca a presa por 7,5s e gera 0,7 Instinto.";
                case "discipline" -> "MODIFICADOR: +20% no Instinto gerado por acertos desta Casa.";
                case "setup" -> "PREPARACAO: acertos iniciam Cacada por 5s; enquanto Cacada estiver ativa, cada projetil gera +0,25 Instinto adicional.";
                case "technique" -> "ATIVA: inicia Cacada por 4,25s e gera +0,8 Instinto.";
                case "reaction" -> "REACAO: ao sofrer dano ou usar a tecnica, ativa Sobrevivencia (-9% dano) por 2,75s; gasta ate 2 Instinto para estender ate +1s.";
                case "synergy" -> "SINERGIA: projeteis causam +2,5% dano contra a presa enquanto a marca estiver ativa.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: inicia Cacada por 6,75s e gera +1,8 Instinto.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Guardiao Selvagem no nivel 25.";
                default -> "";
            };
        }
        if (id.substring(0, id.lastIndexOf('_')).equals("arc_skirm")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: projeteis geram 0,55 Momentum; mover-se tambem acumula Momentum e ficar parado o faz decair. Momentum concede ate +6% dano.";
                case "discipline" -> "MODIFICADOR: +20% no Momentum gerado pelos acertos; nao altera o ganho por movimento.";
                case "setup" -> "PREPARACAO: com 3+ Momentum, um acerto abre Emboscada por 3,5s (+6% dano de projetil).";
                case "technique" -> "ATIVA: consome ate 2,5 Momentum, concede Velocidade I por 4,25s e abre Emboscada por 4,25-5,13s.";
                case "reaction" -> "REACAO: usar Tecnica/Assinatura concede 5% reducao de dano por 2s.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo projetil em ate 4s devolve +0,75 Momentum.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: consome ate 5 Momentum, concede Velocidade II por 6,75s e abre Emboscada por 6,75-8,5s.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Escaramucador no nivel 25.";
                default -> "";
            };
        }
        if (id.substring(0, id.lastIndexOf('_')).equals("arc_magic")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: projeteis acumulam ate 5 marcas e aplicam a afinidade atual: Fogo queima, Gelo desacelera e Tempestade revela com 3+ marcas.";
                case "discipline" -> "MODIFICADOR: +20% na progressao de Marcas Elementais; o ganho fracionario acumula no proprio alvo.";
                case "setup" -> "PREPARACAO: com 3+ marcas, um acerto abre Reacao Elemental por 3,5s e concede +3,5% dano.";
                case "technique" -> "ATIVA: alterna a afinidade entre Fogo, Gelo e Tempestade.";
                case "reaction" -> "REACAO: usar Tecnica/Assinatura concede 5% reducao de dano por 1,75s.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo projetil em ate 4s adiciona +0,75 progresso de Marca Elemental.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: abre Reacao Elemental por 6,75s, com +3,5% dano, sem trocar a afinidade atual.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Arqueiro Arcano no nivel 25.";
                default -> "";
            };
        }
        if (id.substring(0, id.lastIndexOf('_')).equals("arc_art")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: cada projetil gera 0,65 Carga de Dispositivo; Dispositivo Pronto concede +3% dano de projetil.";
                case "discipline" -> "MODIFICADOR: +20% nas Cargas de Dispositivo geradas por acertos.";
                case "setup" -> "PREPARACAO: ao atingir 3+ Cargas, um acerto arma Dispositivo Pronto por 5s (+3% dano).";
                case "technique" -> "ATIVA: +0,8 Carga e arma Dispositivo Pronto por 4,25s.";
                case "reaction" -> "REACAO: usar Tecnica/Assinatura concede 5% reducao de dano por 1,75s.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo projetil em ate 4s gera +0,55 Carga de Dispositivo.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: +1,8 Carga e abre Zona de Dispositivo por 6,75s; a zona e consumida por efeitos compativeis, como Engenheiro.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Artifice no nivel 25.";
                default -> "";
            };
        }
        return "";
    }

    private static String archerSpecializationDescription(String id) {
        if (id == null || !id.startsWith("arc_")) return "";
        String[] p = id.toLowerCase(Locale.ROOT).split("_");
        if (p.length < 4) return "";
        String key = p[0] + "_" + p[1] + "_" + p[2];
        String suffix = suffix(id);

        if ("initiation".equals(suffix)) return switch (key) {
            case "arc_mark_snipe" -> "MECANICA: parado acumula Estabilidade; com 2+ Estabilidade e alvo a 14+ blocos, projeteis recebem +6,5% dano.";
            case "arc_mark_dead" -> "MECANICA: tiros no mesmo alvo geram 0,7 Precisao; trocar alvo gera 0,25. Precisao concede ate +6% dano.";
            case "arc_mark_ball" -> "MECANICA: projeteis acumulam stacks no alvo; com 2+ stacks recebem +3,5% dano. A cada 3 acertos, uma perfuracao pode atingir 1 alvo atras.";
            case "arc_ward_beast" -> "MECANICA: projeteis marcam a presa por 7s; tiros contra a presa marcada recebem +2,5% dano.";
            case "arc_ward_trap" -> "MECANICA: causa +4,5% dano contra alvos lentos; ativas preparam armadilhas fisicas proprias e consomem suas marcas.";
            case "arc_ward_surv" -> "MECANICA: -2,5% dano recebido; cada projetil ativa Sobrevivencia (-9% dano) por 2,25s. Abaixo de 55% de vida, +2,5% dano.";
            case "arc_skirm_wind" -> "MECANICA: projeteis geram Momentum; Momentum concede ate +5% dano adicional nesta especializacao.";
            case "arc_skirm_acro" -> "MECANICA: tiros no ar geram 0,35 Foco com recarga interna de 0,4s; tiros aereos recebem +5,5% dano.";
            case "arc_skirm_guer" -> "MECANICA: apos 4,5s sem acertar um projetil, o proximo acerto abre Emboscada por 2,75s (+5,5% dano).";
            case "arc_magic_fire" -> "MECANICA: com afinidade Fogo, projeteis incendeiam por 2s; durante Reacao Elemental, a queimadura se espalha a ate 4 alvos proximos a cada 1s.";
            case "arc_magic_frost" -> "MECANICA: com afinidade Gelo, projeteis aplicam Lentidao por 2,5s (1,1s em boss); Reacao Elemental intensifica o controle em alvos comuns.";
            case "arc_magic_storm" -> "MECANICA: com afinidade Tempestade, a cada 3 projeteis, uma corrente atinge 1 alvo extra em ate 5 blocos por 28% do dano base, maximo 4,5.";
            case "arc_art_cross" -> "MECANICA: tiros de besta preparados pelas ativas geram +0,8 Cargas de Dispositivo; requer arma de besta no disparo.";
            case "arc_art_bomb" -> "MECANICA: as ativas armam tiros bomba com ate 2 alvos secundarios; o dano suplementar preserva a explosao nativa.";
            case "arc_art_eng" -> "MECANICA: tiros contra alvos em ate 4 blocos da ancora ativa geram +0,4 Cargas; pulsos das ativas controlam ate 3 hostis.";
            default -> "";
        };

        if ("engine".equals(suffix)) return switch (key) {
            case "arc_mark_snipe" -> "MOTOR: Estabilidade acumula 25% mais rapido enquanto voce permanece parado.";
            case "arc_mark_dead" -> "MOTOR: +25% na Precisao gerada por acertos.";
            case "arc_mark_ball" -> "MOTOR: +25% na carga da perfuracao passiva; ativas perfuram alvos geometricamente alinhados.";
            case "arc_ward_beast" -> "MOTOR: Forca do Comando e Resistencia da Matilha nos pets duram 25% mais.";
            case "arc_ward_trap" -> "MOTOR: a preparacao das armadilhas fisicas dura 25% mais.";
            case "arc_ward_surv" -> "MOTOR: Sobrevivencia ativada por projeteis dura 25% mais.";
            case "arc_skirm_wind" -> "MOTOR: +25% no Momentum gerado pelos projeteis desta especializacao.";
            case "arc_skirm_acro" -> "MOTOR: tiros aereos geram 25% mais Foco, mantendo a mesma recarga interna.";
            case "arc_skirm_guer" -> "MOTOR: a Emboscada criada apos o intervalo sem acertos dura 25% mais.";
            case "arc_magic_fire" -> "MOTOR: a queimadura propagada durante Reacao Elemental dura 3s em vez de 2s.";
            case "arc_magic_frost" -> "MOTOR: o controle extra da Reacao Elemental dura 25% mais.";
            case "arc_magic_storm" -> "MOTOR: +25% na carga da corrente eletrica.";
            case "arc_art_cross" -> "MOTOR: +25% nas Cargas extras dos tiros de besta preparados pelas ativas.";
            case "arc_art_bomb" -> "MOTOR: aumenta o raio da explosao secundaria de 3,5 para 4,25 blocos, mantendo o limite de 2 alvos.";
            case "arc_art_eng" -> "MOTOR: +25% nas Cargas extras dos tiros contra alvos proximos da ancora ativa.";
            default -> "";
        };

        if ("conversion".equals(suffix))
            return "CONVERSAO: aumenta em 15% o bonus condicional de dano da especializacao; nao sao +15 pontos percentuais.";
        if ("risk".equals(suffix))
            return "RISCO: projeteis contra alvos a 12+ blocos recebem +3% dano enquanto este node estiver desbloqueado.";

        if ("technique".equals(suffix)) return archerSpecActiveText(key, false, false);
        if ("signature".equals(suffix)) return archerSpecActiveText(key, true, false);
        if ("ascension".equals(suffix)) return archerSpecActiveText(key, false, true);
        return "";
    }

    public static String archerActiveName(String id) {
        var hud = com.rpgstats.gui.ArcaneHudText.entry(id);
        if (hud != null) return hud.name();
        String[] words = archerActiveWords(id);
        return words == null ? "" : words[0];
    }

    private static String[] archerActiveWords(String id) {
        if (id == null) return null;
        int split = id.lastIndexOf('_');
        if (split < 0) return null;
        String suffix = id.substring(split + 1);
        int variant = switch (suffix) { case "technique" -> 0; case "signature" -> 1; case "ascension" -> 2; default -> -1; };
        if (variant < 0) return null;
        String[][] words = switch (id.substring(0, split)) {
            case "arc_magic" -> new String[][] {
                {"Alternar Afinidade", "Troca Fogo, Gelo e Tempestade; permanece selecionada ate a proxima troca."},
                {"Reacao Elemental", "Abre a janela elemental por 6,75s; preserva a afinidade escolhida."},
            };
            case "arc_mark_snipe" -> new String[][] {
                {"Postura de Precisao", "Fique parado por 1s; o proximo tiro a 9+ blocos em ate 5s causa +15% suplementar, max. 4. Mover cancela."},
                {"Alvo Prioritario", "Marca um hostil visivel na mira em ate 24 blocos por 6s; o proximo tiro nele causa +20% suplementar, max. 4."},
                {"Pontos Fracos", "Durante 9s, ate 2 tiros a 9+ blocos com 2+ Estabilidade causam +20% suplementar, max. 4 por tiro."},
            };
            case "arc_mark_dead" -> new String[][] {
                {"Sequencia Precisa", "Durante 5s, dois tiros consecutivos na mesma presa concedem +1 Precisao; conclui uma vez."},
                {"Execucao Calculada", "Gasta 2 a 4 Precisao; o proximo tiro em ate 6s causa dano suplementar igual ao gasto, max. 4."},
                {"Troca de Presa", "Durante 9s, ate 2 trocas de alvo entre tiros concedem +1 Precisao cada."},
            };
            case "arc_mark_ball" -> new String[][] {
                {"Linha Perfurante", "O proximo tiro em ate 5s perfura 1 hostil ate 6 blocos atras da vitima: 25% do dano, max. 4."},
                {"Corredor Balistico", "Fixa um corredor de 16 blocos na direcao atual por 6s; a cada 1s aplica Lentidao curta a ate 3 hostis na linha."},
                {"Tres Perfuracoes", "Durante 9s, ate 3 tiros perfuram 1 hostil atras da vitima: 25% do dano, max. 4 por tiro."},
            };
            case "arc_ward_beast" -> new String[][] {
                {"Designar Presa", "Marca um hostil visivel na mira por 7s; o proximo tiro em ate 5s aplica Lentidao por 2s."},
                {"Comando de Ataque", "Ordena aos pets proprios em ate 16 blocos atacar o hostil na mira; concede Forca I por 4s."},
                {"Guarda da Matilha", "Requer pet proprio em ate 16 blocos; concede Resistencia I aos pets por 6s e a voce por 4s."},
            };
            case "arc_ward_trap" -> new String[][] {
                {"Preparar Armadilhas", "Prepara ate 2 armadilhas fisicas proprias por 5s; requer armadilhas reais colocadas, abertas e validas."},
                {"Cobrar Captura", "Requer hostil marcado por armadilha propria; o proximo tiro marcado em ate 6s consome a marca e causa +4 suplementar."},
                {"Rede Territorial", "Prepara ate 2 armadilhas proprias por 9s; mova-se 2+ blocos da origem: ate 2 tiros marcados consomem a marca para +4 suplementar."},
            };
            case "arc_ward_surv" -> new String[][] {
                {"Recuo de Emergencia", "Impulso para tras e Sobrevivencia por 2,5s; Instinto pode estender a defesa em ate 1s."},
                {"Recuperar Folego", "Requer vida perdida e 1+ Instinto; gasta ate 4 Instinto para curar ate 4 de vida imediatamente."},
                {"Ultimo Refugio", "Durante 9s, ao ficar em 35% de vida ou menos, cura 3 e concede Resistencia I por 3s; ativa uma vez."},
            };
            case "arc_skirm_wind" -> new String[][] {
                {"Avanco do Vento", "Impulso para frente e +0,8 Momentum imediato."},
                {"Disparo de Arrancada", "Gasta 2 a 5 Momentum; o proximo tiro em movimento em ate 6s causa 70% do gasto como dano suplementar, max. 3,5."},
                {"Cadencia em Movimento", "Durante 9s, ate 3 tiros apos mover-se 2+ blocos concedem +1 Momentum; cada tiro redefine a origem."},
            };
            case "arc_skirm_acro" -> new String[][] {
                {"Salto Acrobatico", "Aplica um impulso vertical imediato, inclusive no ar; preserva o disparo equipado."},
                {"Pouso Protegido", "Pouse depois de estar no ar em ate 6s para obter guarda de reposicionamento por 2,25s."},
                {"Ciclo Aereo", "Durante 9s, ate 3 tiros no ar acumulam 0,5 Foco cada; o pouso recebe o Foco e guarda por 2,25s."},
            };
            case "arc_skirm_guer" -> new String[][] {
                {"Rolamento Tatico", "Impulso para tras; o proximo tiro em ate 5s arma guarda de reposicionamento por 1,5s."},
                {"Emboscada de Flanco", "Mova-se 2+ blocos da origem; o proximo tiro em ate 6s causa +20% suplementar, max. 4."},
                {"Duas Emboscadas", "Durante 9s, ate 2 tiros apos mover-se 2+ blocos causam +20% suplementar, max. 4; cada tiro redefine a origem."},
            };
            case "arc_magic_fire" -> new String[][] {
                {"Revestir com Brasas", "O proximo tiro em ate 5s marca a vitima por 6s e incendeia por 2s."},
                {"Consumir Brasas", "O proximo tiro em ate 6s contra hostil marcado consome a marca para +4 suplementar; incendeia ate 2 hostis proximos por 2s."},
                {"Ciclo de Combustao", "Durante 9s, tiros alternam marcar e consumir marcas; ate 3 consumos causam 30% suplementar, max. 4, e incendeiam ate 2 vizinhos."},
            };
            case "arc_magic_frost" -> new String[][] {
                {"Revestir com Geada", "O proximo tiro em ate 5s marca a vitima por 6s e aplica Lentidao por 2s."},
                {"Estilhacar Geada", "O proximo tiro em ate 6s contra hostil marcado consome a marca para +4 suplementar e Lentidao por 3s; controla ate 2 vizinhos por 2,5s."},
                {"Ciclo Glacial", "Durante 9s, tiros alternam marcar e consumir marcas; ate 3 consumos causam 30% suplementar, max. 4, e Lentidao curta em ate 2 vizinhos."},
            };
            case "arc_magic_storm" -> new String[][] {
                {"Carregar Condutor", "O proximo tiro em ate 5s marca um condutor por 6s."},
                {"Descarga Encadeada", "O proximo tiro em ate 6s contra hostil marcado consome a marca para +4 suplementar; corrente atinge ate 2 vizinhos por 2,4 cada."},
                {"Ciclo de Relampagos", "Durante 9s, tiros alternam marcar e consumir marcas; ate 3 consumos causam 30% suplementar, max. 4, e corrente de ate 2,4 a ate 2 vizinhos."},
            };
            case "arc_art_cross" -> new String[][] {
                {"Preparar Virola", "Requer besta equipada; o proximo tiro de besta em ate 5s causa +15% suplementar, max. 4."},
                {"Virola Perfurante", "Requer besta e 2+ Cargas; gasta ate 4. O proximo tiro de besta em ate 6s perfura 1 hostil e causa ate +2 suplementar na vitima."},
                {"Tres Virolas", "Requer besta equipada; ate 3 tiros de besta em 9s causam +15% suplementar, max. 4 por tiro."},
            };
            case "arc_art_bomb" -> new String[][] {
                {"Armar Disparo Bomba", "O proximo tiro em ate 5s atinge ate 2 hostis secundarios em 3,5 blocos: 25% do dano, max. 3 cada; municao com area nativa nao recebe outra explosao."},
                {"Area de Supressao", "Fixa area na presa visivel ou na sua posicao por 6s; a cada 1s aplica Lentidao curta a ate 3 hostis em 4 blocos."},
                {"Tres Disparos Bomba", "Durante 9s, ate 3 tiros atingem ate 2 hostis secundarios: 25% do dano, max. 3 cada; municao com area nativa nao recebe outra explosao."},
            };
            case "arc_art_eng" -> new String[][] {
                {"Fixar Dispositivo", "Requer chao; fixa ancora por 9s. O proximo tiro em ate 5s contra alvo a ate 4 blocos dela aplica Lentidao por 2,5s."},
                {"Pulso de Fraqueza", "Requer ancora ativa a ate 10 blocos e chao; aplica Lentidao e Fraqueza por 2s a ate 3 hostis em 4 blocos da ancora."},
                {"Bateria de Pulsos", "Requer chao; fixa ancora por 9s. Emite 3 pulsos, apos 1s e a cada 2s, de Lentidao e Fraqueza curtas a ate 3 hostis."},
            };
            default -> null;
        };
        return words == null || variant >= words.length ? null : words[variant];
    }

    private static String archerSpecActiveText(String key, boolean signature, boolean ascension) {
        String id = key + (ascension ? "_ascension" : signature ? "_signature" : "_technique");
        String[] words = archerActiveWords(id);
        return words == null ? "" : archerActiveName(id) + ": " + words[1];
    }

    private static String suffix(String id) {
        int at = id == null ? -1 : id.lastIndexOf('_');
        return at < 0 ? "" : id.substring(at + 1);
    }

    /** Descricoes do Assassino auditadas contra ClassMechanics/AssassinSpecializationHandler. */
    private static String assassinDescription(String id) {
        if (id == null || !id.startsWith("ass_")) return "";
        if (id.startsWith("ass_core_")) {
            String suffix = suffix(id);
            return switch (suffix) {
                case "awakening" -> "STAT: +3 Energia maxima; habilita o loop de recurso do Assassino.";
                case "flow" -> "MECANICA: +25% de Energia gerada por acertos validos.";
                case "form" -> "STAT: +2% dano corpo a corpo.";
                case "guard" -> "DEFESA: enquanto uma rota de Saida estiver ativa, recebe -4% dano adicional.";
                case "efficiency" -> "STAT: -4% custo das tecnicas da classe, respeitando o cap.";
                case "tempo" -> "RITMO: usar uma tecnica arma 4s; o proximo golpe corpo a corpo recebe +3% dano e +0,45 Energia, consumindo a janela.";
                case "resolve" -> "DETERMINACAO: abaixo de 40% de vida, recebe -8% dano por 3s; recarga interna de 20s.";
                case "combo" -> "MECANICA: a cada terceiro acerto valido, gera +0,6 Energia e reinicia a sequencia.";
                case "utility" -> "ATIVA: abre uma Abertura por 3,25s e concede Velocidade I + rota de saida por 2,25s; a Abertura vale para um golpe corpo a corpo.";
                case "power" -> "STAT: +4% dano corpo a corpo.";
                case "sustain" -> "MODIFICADOR: +35% a Energia recebida por eliminacoes validas.";
                case "mastery" -> "KEYSTONE: libera a escolha da Casa principal no nivel 10.";
                default -> "";
            };
        }

        String path = assassinPathDescription(id);
        if (!path.isEmpty()) return path;
        return assassinSpecializationDescription(id);
    }

    private static String assassinPathDescription(String id) {
        String suffix = suffix(id);
        if (id.startsWith("ass_shadow_")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: golpes pelas costas ou apos Preparacao Sombria criam Abertura; o primeiro golpe preparado recebe +8% dano e consome a janela.";
                case "discipline" -> "MODIFICADOR: Preparacao Sombria acumula 20% mais rapido fora de combate.";
                case "setup" -> "PREPARACAO: fora de combate, Preparacao Sombria enche em cerca de 2,8s e passa a armar uma Abertura curta para o primeiro golpe.";
                case "technique" -> "ATIVA: Abertura por 4,25s, rota de saida por 2,1s e Invisibilidade por ate 2,1s; o primeiro golpe corpo a corpo consome a Abertura.";
                case "reaction" -> "REACAO: Tecnica/Assinatura tambem concede Velocidade I durante a janela curta de saida.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo golpe corpo a corpo em ate 4s abre uma rota de Saida por 2,25s.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: Abertura por 6,75s, rota de saida por 3,35s e Invisibilidade por 2,25s; o primeiro golpe consome a Abertura.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Sombra no nivel 25.";
                default -> "";
            };
        }
        if (id.startsWith("ass_venom_")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: cada golpe corpo a corpo adiciona 1 Dose (max. 6) e aplica Veneno; com 3+ Doses, o dano cresce em 0,8% por Dose.";
                case "discipline" -> "MODIFICADOR: +20% na progressao de Doses; ganho fracionario acumula por alvo.";
                case "setup" -> "PREPARACAO: 3+ Doses armam Veneno Pronto por 4s; a proxima Tecnica/Assinatura consome a preparacao e fortalece o modo toxico.";
                case "technique" -> "ATIVA: alterna o modo toxico entre Desgaste, Potencia e Controle; efeitos adicionais dependem da especializacao que consulta o modo.";
                case "reaction" -> "REACAO: usar Tecnica/Assinatura abre rota de Saida por 1,5s.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo golpe em ate 4s adiciona +0,75 progresso de Dose.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: arma Veneno Consumivel por 6,75s; o proximo golpe consome ate 3 Doses do alvo (4 se Veneno Pronto foi consumido) e aplica Veneno concentrado.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Veneno no nivel 25.";
                default -> "";
            };
        }
        if (id.startsWith("ass_duel_")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: primeiro golpe gera 0,3 Vantagem; acertos dentro de 2,5s geram 0,75. Vantagem concede ate +4,5% dano.";
                case "discipline" -> "MODIFICADOR: +20% na Vantagem gerada por golpes e pela Sinergia do Duelista.";
                case "setup" -> "PREPARACAO: com 2+ Vantagem, um golpe arma Preparacao de Parry por 4s; a proxima Tecnica/Assinatura consome a preparacao, ganha +0,35 Vantagem e +0,9s de janela.";
                case "technique" -> "ATIVA: abre Parry por 4,25s e gera +0,5 Vantagem. Parry de golpe corpo a corpo reduz 32% do dano, e arma Riposta por 3,5s.";
                case "reaction" -> "REACAO: um Parry bem-sucedido concede Velocidade I por 1,5s.";
                case "synergy" -> "SINERGIA: usar Tecnica/Assinatura arma uma janela de 4s; o proximo golpe corpo a corpo gera +0,45 Vantagem e consome a janela.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: arma Riposta diretamente por 6,75s e gera +1,3 Vantagem; se Preparacao estiver pronta, dura +0,9s e gera +0,35 adicional.";
                case "mastery" -> "KEYSTONE: libera Esgrimista, Dancarino de Laminas e Contra-Lamina no nivel 25.";
                default -> "";
            };
        }
        if (id.startsWith("ass_sabo_")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: acertos geram 0,55 Preparacao. Com Dispositivo Armado, o proximo acerto recebe +2,5% dano, aplica Lentidao e consome o dispositivo.";
                case "discipline" -> "MODIFICADOR: +20% na Preparacao gerada por acertos.";
                case "setup" -> "PREPARACAO: com 3+ Preparacao, um acerto arma Dispositivo Pronto por 4,5s; a proxima Tecnica/Assinatura consome isso para +1s de janela.";
                case "technique" -> "ATIVA: +0,8 Preparacao e arma Dispositivo por 4,25s; o primeiro acerto valido aplica o efeito e consome a janela.";
                case "reaction" -> "REACAO: quando Dispositivo Armado acerta e aplica Lentidao, recebe Velocidade I por 1,5s.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo acerto em ate 4s gera +0,45 Preparacao adicional.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: +1,8 Preparacao, arma Dispositivo e abre Demolicao por 6,75s; Demolicao e aproveitada pela especializacao Demolidor.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Sabotador no nivel 25.";
                default -> "";
            };
        }
        if (id.startsWith("ass_myst_")) {
            return switch (suffix) {
                case "foundation" -> "MECANICA: golpes corpo a corpo geram 0,6 Eco de Alma (max. 6); Ecos concedem ate +4,5% dano.";
                case "discipline" -> "MODIFICADOR: +20% nos Ecos gerados pelos golpes desta Casa.";
                case "setup" -> "PREPARACAO: 3+ Ecos armam Alma Pronta por 4,5s; a proxima Tecnica/Assinatura gasta 1 Eco a menos e ganha +0,9s de janela.";
                case "technique" -> "ATIVA: consome ate 2 Ecos e arma Golpe da Alma por 4,25-5,15s; o proximo golpe corpo a corpo recebe +6% dano.";
                case "reaction" -> "REACAO: usar a tecnica abre rota de saida por 1,75s, reduzindo dano recebido em 6%.";
                case "synergy" -> "SINERGIA: apos Tecnica/Assinatura, o proximo golpe corpo a corpo em ate 4s gera +0,45 Eco adicional.";
                case "economy" -> "STAT: -5% custo das tecnicas desta Casa, respeitando o cap.";
                case "signature" -> "ATIVA: consome ate 4 Ecos e arma Golpe da Alma por 6,75-8,55s; o proximo golpe corpo a corpo recebe +6% dano.";
                case "mastery" -> "KEYSTONE: libera uma especializacao de Lamina Mistica no nivel 25.";
                default -> "";
            };
        }
        return "";
    }

    private static String assassinSpecializationDescription(String id) {
        if (id == null || !id.startsWith("ass_")) return "";
        String[] p = id.toLowerCase(Locale.ROOT).split("_");
        if (p.length < 4) return "";
        String key = p[0] + "_" + p[1] + "_" + p[2];
        String suffix = suffix(id);

        if ("initiation".equals(suffix)) return switch (key) {
            case "ass_shadow_night" -> "MECANICA: o golpe que consome uma Abertura recebe +6,5% dano; com o Motor, essa Abertura tambem prepara uma fuga curta.";
            case "ass_shadow_phant" -> "MECANICA: durante Saida de Fase, golpes corpo a corpo recebem +5% dano e abrem rota de fuga.";
            case "ass_shadow_exec" -> "MECANICA: +10% dano contra alvos abaixo de 30% de vida (18% em boss); golpes nessa faixa tambem geram Combo.";
            case "ass_venom_alch" -> "MECANICA: com 3+ Doses, +3,5% dano. No modo Desgaste, cada golpe adiciona 1 Dose extra.";
            case "ass_venom_plague" -> "MECANICA: com 4+ Doses, +4,5% dano; a Conversao libera propagacao de Veneno para alvos proximos.";
            case "ass_venom_toxic" -> "MECANICA: com 2+ Doses, +3,5% dano. Potencia aumenta o nivel do Veneno; Controle aplica Lentidao.";
            case "ass_duel_fence" -> "MECANICA: com 2+ Vantagem, +4% dano. O Motor recompensa o primeiro golpe depois de quebrar a cadencia de 2,5s.";
            case "ass_duel_dance" -> "MECANICA: golpes geram Danca; trocar de alvo gera muito mais. Danca concede ate +5% dano.";
            case "ass_duel_counter" -> "MECANICA: durante Riposta, +6% dano e cada golpe gera +1 Vantagem; o Motor converte a defesa em controle.";
            case "ass_sabo_demo" -> "MECANICA: durante Demolicao, +4% dano e 12% do dano de cada acerto e armazenado para uma detonacao limitada.";
            case "ass_sabo_infil" -> "MECANICA: durante Abertura, +5% dano; acertar nessa janela abre uma rota de fuga de 2,5s.";
            case "ass_sabo_wire" -> "MECANICA: +3,5% dano contra alvos lentos; o Motor converte a Lentidao do dispositivo em Fraqueza.";
            case "ass_myst_hex" -> "MECANICA: durante Caca Hex, golpes corpo a corpo recebem +4% dano e marcam o alvo; com o Motor, a marca mantem +2% dano apos a janela ativa.";
            case "ass_myst_soul" -> "MECANICA: Ecos concedem ate +5% dano; o Motor permite consumir Ecos em um corte magico separado.";
            case "ass_myst_void" -> "MECANICA: durante Divida do Vazio, +6,5% dano; o Motor converte o primeiro golpe apos o blink em mobilidade e controle.";
            default -> "";
        };

        if ("engine".equals(suffix)) return switch (key) {
            case "ass_shadow_night" -> "MOTOR: ao consumir Abertura, concede Velocidade por 2,1s e Invisibilidade por 0,9s, com recarga interna de 1,5s.";
            case "ass_shadow_phant" -> "MOTOR: golpes durante Saida de Fase prolongam em 25% a rota de fuga criada pelo Fantasma.";
            case "ass_shadow_exec" -> "MOTOR: +25% no Combo gerado por golpes contra alvos abaixo do limiar de execucao.";
            case "ass_venom_alch" -> "MOTOR: durante a janela de Formula, o modo Desgaste gera 25% mais progresso na Dose extra.";
            case "ass_venom_plague" -> "MOTOR: durante Peste, acertar alvo com 4+ Doses aplica Fraqueza por 2,25s (1s em boss).";
            case "ass_venom_toxic" -> "MOTOR: durante Toxicologia, os efeitos de controle/veneno da formula ativa duram 25% mais.";
            case "ass_duel_fence" -> "MOTOR: primeiro golpe apos quebrar a cadencia gera +1,0 Vantagem; durante Contra-Tempo, gera +1,5 e consome a janela.";
            case "ass_duel_dance" -> "MOTOR: +25% na Danca gerada pelos golpes.";
            case "ass_duel_counter" -> "MOTOR: uma Riposta real aplica Fraqueza por 2,1s (1s em boss) e concede Resistencia por 1,2s.";
            case "ass_sabo_demo" -> "MOTOR: com 2,5+ dano armazenado, detona 60% do valor (max. 3,5) e zera o armazenamento; recarga interna de 1,4s.";
            case "ass_sabo_infil" -> "MOTOR: golpes durante Abertura prolongam em 25% a rota de fuga criada pelo Infiltrador.";
            case "ass_sabo_wire" -> "MOTOR: ao acertar alvo lento, aplica Fraqueza por 1,9s; bosses recebem janela menor.";
            case "ass_myst_hex" -> "MOTOR: Caca Hex marca o alvo por mais tempo; depois da janela, a marca preserva +2% dano corpo a corpo enquanto durar.";
            case "ass_myst_soul" -> "MOTOR: +25% na geracao de Ecos e Golpe da Alma passa a consumir ate 2 Ecos para causar 1,5-3,0 dano magico extra.";
            case "ass_myst_void" -> "MOTOR: o golpe durante Divida do Vazio aplica Lentidao e concede Velocidade por 1,5s; bosses resistem ao controle.";
            default -> "";
        };

        if ("conversion".equals(suffix)) return switch (key) {
            case "ass_shadow_night" -> "CONVERSAO: +15% sobre o bonus condicional da especializacao e +0,8 Energia ao executar a fuga da Lamina Noturna.";
            case "ass_venom_plague" -> "CONVERSAO: +15% sobre o bonus condicional e, com 4+ Doses, golpes espalham Veneno por 2,25s em alvos proximos (bosses resistem).";
            case "ass_duel_counter" -> "CONVERSAO: +15% sobre o bonus condicional e devolve +0,65 Energia quando a Contra-Lamina converte uma Riposta.";
            case "ass_sabo_demo" -> "CONVERSAO: +15% sobre o bonus condicional; detonacao passa a liberar 75% do armazenado (max. 4,5) e splash limitado em ate 3 alvos.";
            case "ass_sabo_wire" -> "CONVERSAO: +15% sobre o bonus condicional e aumenta a Fraqueza do fio para 2,75s antes da resistencia de bosses.";
            case "ass_myst_soul" -> "CONVERSAO: +15% sobre o bonus condicional e devolve 0,30 + 0,15 por Eco consumido no corte mistico.";
            case "ass_myst_void" -> "CONVERSAO: +15% sobre o bonus condicional; o golpe do Vazio tambem aplica Fraqueza e concede Resistencia curta.";
            default -> "CONVERSAO: aumenta em 15% o bonus condicional de dano da especializacao; nao sao +15 pontos percentuais.";
        };

        if ("risk".equals(suffix)) {
            if ("ass_myst_void".equals(key))
                return "RISCO: com 3+ Combo, +3,5% dano corpo a corpo; enquanto Divida do Vazio estiver ativa, voce tambem recebe +6% dano.";
            return "RISCO: com 3+ Combo, golpes corpo a corpo recebem +3,5% dano enquanto este node estiver desbloqueado.";
        }

        if ("technique".equals(suffix)) return assassinSpecActiveText(key, false, false);
        if ("signature".equals(suffix)) return assassinSpecActiveText(key, true, false);
        if ("ascension".equals(suffix)) return assassinSpecActiveText(key, false, true);
        return "";
    }

    private static String assassinSpecActiveText(String key, boolean signature, boolean ascension) {
        String window = ascension ? "8,75s" : signature ? "6,25s" : "3,75s";
        String label = ascension ? "ASCENSAO" : signature ? "DOMINIO" : "ATIVA";
        return switch (key) {
            case "ass_shadow_night" -> label + ": abre Abertura por " + window + "; o primeiro golpe corpo a corpo consome a janela." + (ascension ? " Tambem concede Invisibilidade por 2,75s." : "");
            case "ass_shadow_phant" -> label + ": Fase reduz dano recebido em 22% por ate 2,25s; Saida de Fase permanece por " + window + " para fortalecer o proximo golpe.";
            case "ass_shadow_exec" -> label + ": abre Janela de Execucao por " + window + "; ela permite gastar Combo em um golpe preparado e recuperar Energia.";
            case "ass_venom_alch" -> label + ": alterna a formula Desgaste/Potencia/Controle por " + window + "; com o Motor, Desgaste acelera ainda mais a Dose extra.";
            case "ass_venom_plague" -> label + ": abre Peste por " + window + "; com o Motor, golpes em alvos com 4+ Doses aplicam Fraqueza.";
            case "ass_venom_toxic" -> label + ": alterna Desgaste/Potencia/Controle por " + window + "; Potencia fortalece Veneno e Controle aplica Lentidao, com duracao maior via Motor.";
            case "ass_duel_fence" -> label + ": quebra a cadencia e abre Contra-Tempo por " + window + "; o proximo golpe recebe +5% dano e o Motor gera Vantagem reforcada.";
            case "ass_duel_dance" -> label + ": +1,7 Danca e Janela de Danca por " + window + "; durante a janela, a geracao de Danca e 35% maior.";
            case "ass_duel_counter" -> label + ": abre Parry por no maximo 2,25s; um Parry corpo a corpo bem-sucedido arma Riposta por 3,5s.";
            case "ass_sabo_demo" -> label + ": arma Dispositivo e Demolicao por " + window + "; durante Demolicao, 12% do dano de cada acerto e armazenado.";
            case "ass_sabo_infil" -> label + ": abre Abertura por " + window + " e rota de saida por metade dessa duracao; o primeiro golpe consome a Abertura.";
            case "ass_sabo_wire" -> label + ": arma Dispositivo por " + window + "; o primeiro acerto aplica Lentidao e, com o Motor, tambem prepara Fraqueza.";
            case "ass_myst_hex" -> label + ": abre Caca Hex por " + window + ", concedendo +4% dano corpo a corpo durante a janela.";
            case "ass_myst_soul" -> label + ": +1,7 Eco e Golpe da Alma por " + window + "; o proximo golpe recebe +6% dano e, com o Motor, consome Ecos em dano magico.";
            case "ass_myst_void" -> label + ": blink curto de 2,4 blocos e Divida do Vazio por " + window + "; o primeiro golpe consome a Divida.";
            default -> "";
        };
    }

    /** Descricoes concretas do passe do Guerreiro; evita prometer apenas "mais dano/duracao". */
    private static String warriorSpecializationDescription(String id) {
        if (id == null || !id.startsWith("war_")) return "";
        String[] p = id.toLowerCase(Locale.ROOT).split("_");
        if (p.length < 4) return "";
        String key = p[0] + "_" + p[1] + "_" + p[2];
        String loop = switch (key) {
            case "war_van_bulw" -> "bloquear -> armazenar Guarda -> barreira curta para a formacao";
            case "war_van_lord" -> "marcar ameaca -> aliados acertam -> gerar Moral e Ordem";
            case "war_van_jugg" -> "avancar sob pressao -> acumular Inercia -> consumir em controle frontal";
            case "war_bers_reav" -> "ferir o alvo correto -> gerar Sede -> sustain limitado -> executar";
            case "war_bers_rage" -> "armazenar Furia -> Frenesi curto -> extensao por execucao -> Exaustao";
            case "war_bers_pain" -> "receber dano -> armazenar Dor -> golpe comprometido libera impacto";
            case "war_weap_blade" -> "manter sequencia no mesmo alvo -> Cadencia -> finisher e recuperacao de recarga";
            case "war_weap_duel" -> "aparar -> armar Riposta -> quebrar o ritmo/postura do alvo";
            case "war_weap_titan" -> "golpe pesado -> acumular Impacto -> abrir elite ou boss resistente";
            case "war_rune_knight" -> "gravar Cargas Runicas -> consumir no proximo golpe -> explosao lateral limitada";
            case "war_rune_break" -> "defender magia/projetil -> armazenar Selo -> interromper no golpe seguinte";
            case "war_rune_storm" -> "avancar -> carregar Tempestade -> golpe cria corrente curta em outro alvo";
            case "war_cmd_banner" -> "plantar Estandarte -> lutar dentro da zona -> manter Moral e formacao";
            case "war_cmd_tact" -> "emitir Ordem -> cumprir sua condicao -> empoderar a proxima decisao";
            case "war_cmd_guard" -> "vincular formacao -> interceptar dano limitado -> armar contra-pressao";
            default -> "";
        };
        if (loop.isEmpty()) return "";
        if (id.endsWith("_initiation") && key.equals("war_van_jugg"))
            return "MECANICA: " + loop + ". Defesa contra bosses cresce com nivel e Tenacidade, especialmente entre niveis 25 e 50. Inclui ataques de area e projeteis atribuidos ao boss, mesmo alem de 8 blocos. No nivel 50 e com 25 Tenacidade: 79% de reducao antes da armadura; todas as protecoes do RPG juntas respeitam o limite de 80%.";
        if (id.endsWith("_initiation")) return "MECANICA: " + loop + ".";
        if (id.endsWith("_engine")) return "MOTOR: habilita o gatilho server-side de " + specializationName(id) + ".";
        if (id.endsWith("_technique")) return "ATIVA: inicia a ferramenta caracteristica; exige o motor da build.";
        if (id.endsWith("_conversion")) return "CONVERSAO: transforma o estado preparado em utilidade, sustain ou area limitada.";
        if (id.endsWith("_risk")) return "RISCO: aumenta o compromisso da janela sem criar bonus permanente.";
        if (id.endsWith("_signature")) return "DOMINIO: consome a preparacao completa para o payoff de " + specializationName(id) + ".";
        if (id.endsWith("_ascension")) return "ASCENSAO: muda temporariamente uma regra do loop — nao apenas seus numeros.";
        return "";
    }

    private static SkillEffect p(String id, float value) {
        return SkillEffect.passive(id, value);
    }

    private static SkillEffect active(float scale, int duration, int cooldown, float cost) {
        return SkillEffect.active(CLASS_ACTIVE, scale, duration, cooldown, cost);
    }
}

