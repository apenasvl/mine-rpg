package com.rpgstats.classes;

import com.rpgstats.tree.SkillNode;
import java.util.List;
import java.util.Map;

/**
 * Fábrica das árvores expandidas das classes não-mago.
 * Os nomes e identidades são próprios; a geometria comum mantém custos e marcos comparáveis.
 */
public final class ClassTrees {
    private ClassTrees() {}

    private static SkillNode n(String id, String name, String desc, int cost, String prereq,
                               int level, int tier, boolean cross) {
        String archerName = com.rpgstats.ability.ClassAbilityRegistry.archerActiveName(id);
        if (!archerName.isEmpty()) name = archerName;
        return new SkillNode(id, name, com.rpgstats.ability.ClassAbilityRegistry.description(id), cost, prereq, null, 0, Map.of(), level, tier, cross);
    }

    public static List<SkillNode> core(String prefix, String resource, String identity) {
        return List.of(
                n(prefix + "_core_awakening", "Despertar de " + identity, "Desbloqueia " + resource + " e a identidade central da classe.", 1, null, 1, 1, false),
                n(prefix + "_core_flow", "Fluxo de " + resource, "Melhora a geração de " + resource + " em combate.", 1, prefix + "_core_awakening", 2, 2, false),
                n(prefix + "_core_form", "Forma Treinada", "Pequeno bônus ofensivo específico da classe.", 1, prefix + "_core_awakening", 2, 2, false),
                n(prefix + "_core_guard", "Defesa Instintiva", "Redução defensiva pequena e condicional.", 1, prefix + "_core_awakening", 3, 2, false),
                n(prefix + "_core_efficiency", "Eficiência", "Habilidades custam 4% menos recurso.", 1, prefix + "_core_flow", 4, 3, false),
                n(prefix + "_core_tempo", "Ritmo de Combate", "Alternar ataques e habilidades fortalece a rotação.", 1, prefix + "_core_form", 5, 3, false),
                n(prefix + "_core_resolve", "Determinação", "Ao cair abaixo de 40% de vida, recebe defesa curta. Recarga interna.", 1, prefix + "_core_guard", 6, 4, false),
                n(prefix + "_core_combo", "Sequência Perfeita", "A terceira ação diferente gera recurso adicional.", 1, prefix + "_core_tempo", 7, 4, false),
                n(prefix + "_core_utility", "Técnica Versátil", "ATIVA · habilidade utilitária própria da classe.", 2, prefix + "_core_efficiency", 8, 5, false),
                n(prefix + "_core_power", "Potencial Controlado", "+4% no tipo principal de dano.", 1, prefix + "_core_combo", 8, 5, false),
                n(prefix + "_core_sustain", "Fôlego", "Recupera uma pequena quantidade de recurso após uma eliminação válida.", 1, prefix + "_core_resolve", 9, 5, false),
                n(prefix + "_core_mastery", "Maestria de " + identity, "Desbloqueia a escolha de um Caminho no nível 10.", 1, prefix + "_core_power", 10, 6, false)
        );
    }

    public static List<SkillNode> path(String prefix, String display, String mechanic) {
        return List.of(
                n(prefix + "_foundation", "Fundamento: " + display, mechanic + " passa a fazer parte da sua rotação.", 1, null, 10, 1, true),
                n(prefix + "_discipline", "Disciplina", "Melhora a consistência sem elevar muito o dano bruto.", 1, prefix + "_foundation", 10, 2, true),
                n(prefix + "_setup", "Preparação", "Cumprir a condição do caminho gera uma pequena vantagem tática.", 1, prefix + "_discipline", 12, 3, true),
                n(prefix + "_technique", "Técnica de " + display, "ATIVA · ferramenta principal do caminho, com custo e recarga moderados.", 2, prefix + "_setup", 14, 4, false),
                n(prefix + "_reaction", "Reação Treinada", "Usar a técnica no momento certo melhora defesa ou posicionamento.", 1, prefix + "_discipline", 14, 4, false),
                n(prefix + "_synergy", "Sinergia", "Alternar a técnica com ataques fortalece o próximo efeito.", 1, prefix + "_reaction", 16, 5, false),
                n(prefix + "_economy", "Economia de Recurso", "Reduz em 5% o custo das habilidades deste caminho.", 1, prefix + "_setup", 16, 5, false),
                n(prefix + "_signature", "Assinatura: " + display, "ATIVA · versão avançada da mecânica central.", 2, prefix + "_synergy", 19, 6, false),
                n(prefix + "_mastery", "Mestre de " + display, "Consolida a mecânica e libera três especializações.", 2, prefix + "_signature", 22, 7, false)
        );
    }

    public static List<SkillNode> spec(String prefix, String display, String fantasy) {
        return List.of(
                n(prefix + "_initiation", "Iniciação: " + display, fantasy, 2, null, 25, 1, false),
                n(prefix + "_engine", "Motor da Build", "Cria a condição recorrente que alimenta esta especialização.", 1, prefix + "_initiation", 30, 2, false),
                n(prefix + "_technique", "Técnica: " + display, "ATIVA · ferramenta característica com poder moderado.", 2, prefix + "_engine", 35, 3, false),
                n(prefix + "_conversion", "Conversão Tática", "Transforma recurso, defesa ou posicionamento em pressão ofensiva.", 1, prefix + "_technique", 40, 4, false),
                n(prefix + "_risk", "Risco Calculado", "Escolha de risco/recompensa com limite contra chefes.", 1, prefix + "_conversion", 45, 5, false),
                n(prefix + "_signature", "Domínio: " + display, "ATIVA · finalizador que exige preparação.", 2, prefix + "_risk", 48, 6, false),
                n(prefix + "_ascension", "ASCENSÃO · " + display, "No nível 50, amplifica a identidade por poucos segundos; não causa morte instantânea.", 3, prefix + "_signature", 50, 7, false)
        );
    }
}
