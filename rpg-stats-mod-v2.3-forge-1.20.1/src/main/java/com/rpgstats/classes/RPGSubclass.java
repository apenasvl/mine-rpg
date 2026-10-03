package com.rpgstats.classes;

import com.rpgstats.stats.Stat;
import com.rpgstats.tree.SkillNode;

import java.util.List;
import java.util.Map;

import static com.rpgstats.stats.Stat.*;

public enum RPGSubclass {
    BERSERKER(RPGClass.GUERREIRO, "Berserker", "Dano brutal e sacrificio de defesa", List.of(
            node("ber_frenesi", "Frenesi", "+3 For/+2 Ten · velocidade de ataque", 1, null, FORCA, 14, Map.of(FORCA, 3, TENACIDADE, 2)),
            node("ber_sangue", "Sede de Sangue", "+4 For · lifesteal 10%", 1, "ber_frenesi", null, 0, Map.of(FORCA, 4)),
            node("ber_resistencia", "Dor e Combustivel", "+3 Vitalidade", 1, "ber_frenesi", null, 0, Map.of(VITALIDADE, 3)),
            node("ber_massacre", "Massacre", "+5 For · ATIVO: sede de sangue", 2, "ber_sangue", FORCA, 22, Map.of(FORCA, 5, DESTREZA, 2)),
            node("ber_avatar", "Avatar da Guerra", "+6 Forca, +4 Vitalidade", 3, "ber_massacre", FORCA, 30, Map.of(FORCA, 6, VITALIDADE, 4))
    )),
    GUARDIAO(RPGClass.GUERREIRO, "Guardiao", "Muralha viva e protetor do grupo", List.of(
            node("gua_postura", "Guarda Defensiva", "+5 Vit · ATIVO: guarda 40%", 1, null, VITALIDADE, 14, Map.of(VITALIDADE, 5)),
            node("gua_aco", "Corpo de Aco", "+4 Vitalidade, +2 Forca", 1, "gua_postura", null, 0, Map.of(VITALIDADE, 4, FORCA, 2)),
            node("gua_vigilia", "Vigilia", "+3 Destreza", 1, "gua_postura", null, 0, Map.of(DESTREZA, 3)),
            node("gua_bastiao", "Bastiao", "+7 Vitalidade", 2, "gua_aco", VITALIDADE, 24, Map.of(VITALIDADE, 7)),
            node("gua_inabalavel", "Inabalavel", "+8 Vitalidade, +3 Forca", 3, "gua_bastiao", VITALIDADE, 32, Map.of(VITALIDADE, 8, FORCA, 3))
    )),
    CAVALEIRO_RUNICO(RPGClass.GUERREIRO, "Cavaleiro Runico", "Forca marcial alimentada por runas", List.of(
            node("run_marca", "Marca Runica", "+3 Forca, +3 Inteligencia", 1, null, FORCA, 12, Map.of(FORCA, 3, INTELIGENCIA, 3)),
            node("run_lamina", "Lamina Encantada", "+4 Forca", 1, "run_marca", INTELIGENCIA, 8, Map.of(FORCA, 4)),
            node("run_protecao", "Runa de Protecao", "+4 Vitalidade", 1, "run_marca", null, 0, Map.of(VITALIDADE, 4)),
            node("run_tempestade", "Tempestade Runica", "+4 Int · ATIVO: tempestade", 2, "run_lamina", INTELIGENCIA, 15, Map.of(INTELIGENCIA, 4, DESTREZA, 3)),
            node("run_mestre", "Mestre das Runas", "+5 Forca, +5 Inteligencia", 3, "run_tempestade", INTELIGENCIA, 24, Map.of(FORCA, 5, INTELIGENCIA, 5))
    )),

    ATIRADOR(RPGClass.ARQUEIRO, "Atirador", "Precisao extrema e dano a distancia", List.of(
            node("ati_mira", "Mira Estavel", "+5 Destreza", 1, null, DESTREZA, 15, Map.of(DESTREZA, 5)),
            node("ati_foco", "Foco Total", "+4 Destreza, +2 Inteligencia", 1, "ati_mira", null, 0, Map.of(DESTREZA, 4, INTELIGENCIA, 2)),
            node("ati_passo", "Reposicionamento", "+3 Tenacidade", 1, "ati_mira", null, 0, Map.of(TENACIDADE, 3)),
            node("ati_perfurante", "Tiro Perfurante", "+7 Destreza", 2, "ati_foco", DESTREZA, 25, Map.of(DESTREZA, 7)),
            node("ati_mestre", "Olho do Mestre", "+10 Destreza", 3, "ati_perfurante", DESTREZA, 35, Map.of(DESTREZA, 10))
    )),
    CACADOR(RPGClass.ARQUEIRO, "Cacador", "Sobrevivencia, rastreio e resistencia", List.of(
            node("cac_instinto", "Instinto Selvagem", "+3 Tenacidade, +3 Destreza", 1, null, TENACIDADE, 12, Map.of(TENACIDADE, 3, DESTREZA, 3)),
            node("cac_trilha", "Mestre da Trilha", "+4 Tenacidade", 1, "cac_instinto", null, 0, Map.of(TENACIDADE, 4)),
            node("cac_presa", "Marcar Presa", "+4 Destreza", 1, "cac_instinto", null, 0, Map.of(DESTREZA, 4)),
            node("cac_feroz", "Companheiro Feroz", "+4 Forca, +4 Vitalidade", 2, "cac_trilha", VITALIDADE, 12, Map.of(FORCA, 4, VITALIDADE, 4)),
            node("cac_alfa", "Predador Alfa", "+6 Tenacidade, +6 Destreza", 3, "cac_feroz", TENACIDADE, 28, Map.of(TENACIDADE, 6, DESTREZA, 6))
    )),
    PATRULHEIRO(RPGClass.ARQUEIRO, "Patrulheiro", "Mobilidade e combate equilibrado", List.of(
            node("pat_duplo", "Passo Duplo", "+4 Tenacidade, +2 Destreza", 1, null, TENACIDADE, 14, Map.of(TENACIDADE, 4, DESTREZA, 2)),
            node("pat_emboscada", "Emboscada", "+3 Forca, +3 Destreza", 1, "pat_duplo", null, 0, Map.of(FORCA, 3, DESTREZA, 3)),
            node("pat_vigor", "Vigor da Mata", "+4 Vitalidade", 1, "pat_duplo", null, 0, Map.of(VITALIDADE, 4)),
            node("pat_chuva", "Chuva de Flechas", "+5 Des · ATIVO: chuva de flechas", 2, "pat_emboscada", DESTREZA, 22, Map.of(DESTREZA, 5, TENACIDADE, 4)),
            node("pat_lenda", "Lenda da Fronteira", "+6 Tenacidade, +6 Vitalidade", 3, "pat_chuva", TENACIDADE, 30, Map.of(TENACIDADE, 6, VITALIDADE, 6))
    )),

    NINJA(RPGClass.ASSASSINO, "Ninja", "Velocidade, tecnica e golpes rapidos", List.of(
            node("nin_selo", "Selo da Velocidade", "+5 Tenacidade", 1, null, TENACIDADE, 15, Map.of(TENACIDADE, 5)),
            node("nin_shuriken", "Mestre de Shuriken", "+4 Destreza", 1, "nin_selo", null, 0, Map.of(DESTREZA, 4)),
            node("nin_fumaca", "Passo de Fumaca", "+4 Ten · ATIVO: fumaca", 1, "nin_selo", null, 0, Map.of(TENACIDADE, 4, INTELIGENCIA, 2)),
            node("nin_combo", "Combo Relampago", "+5 Tenacidade, +4 Destreza", 2, "nin_shuriken", TENACIDADE, 24, Map.of(TENACIDADE, 5, DESTREZA, 4)),
            node("nin_mestre", "Mestre das Sombras", "+8 Tenacidade, +5 Destreza", 3, "nin_combo", TENACIDADE, 34, Map.of(TENACIDADE, 8, DESTREZA, 5))
    )),
    ALQUIMISTA(RPGClass.ASSASSINO, "Alquimista", "Venenos, preparo e conhecimento", List.of(
            node("alq_formula", "Formula Toxica", "+4 Inteligencia, +3 Destreza", 1, null, DESTREZA, 13, Map.of(INTELIGENCIA, 4, DESTREZA, 3)),
            node("alq_resistencia", "Sangue Antidoto", "+4 Vitalidade", 1, "alq_formula", null, 0, Map.of(VITALIDADE, 4)),
            node("alq_corrosao", "Corrosao", "+4 Inteligencia, +2 Forca", 1, "alq_formula", null, 0, Map.of(INTELIGENCIA, 4, FORCA, 2)),
            node("alq_nuvem", "Nuvem Venenosa", "+5 Int · ATIVO: nuvem toxica", 2, "alq_corrosao", INTELIGENCIA, 20, Map.of(INTELIGENCIA, 5, DESTREZA, 4)),
            node("alq_peste", "Arauto da Peste", "+8 Inteligencia, +5 Destreza", 3, "alq_nuvem", INTELIGENCIA, 30, Map.of(INTELIGENCIA, 8, DESTREZA, 5))
    )),
    ESPECTRO(RPGClass.ASSASSINO, "Espectro", "Evasao e ataques vindos das sombras", List.of(
            node("esp_vulto", "Vulto", "+4 Tenacidade, +3 Forca", 1, null, TENACIDADE, 14, Map.of(TENACIDADE, 4, FORCA, 3)),
            node("esp_intangivel", "Intangivel", "+4 Ten · ATIVO: dash", 1, "esp_vulto", null, 0, Map.of(TENACIDADE, 4, VITALIDADE, 3)),
            node("esp_corte", "Corte Fantasma", "+5 Forca", 1, "esp_vulto", null, 0, Map.of(FORCA, 5)),
            node("esp_assombro", "Assombro", "+5 Forca, +5 Tenacidade", 2, "esp_corte", FORCA, 22, Map.of(FORCA, 5, TENACIDADE, 5)),
            node("esp_morte", "Forma da Morte", "+7 Forca, +7 Tenacidade", 3, "esp_assombro", TENACIDADE, 32, Map.of(FORCA, 7, TENACIDADE, 7))
    ));


    public final RPGClass parent;
    public final String display;
    public final String desc;
    public final List<SkillNode> nodes;

    RPGSubclass(RPGClass parent, String display, String desc, List<SkillNode> nodes) {
        this.parent = parent;
        this.display = display;
        this.desc = desc;
        this.nodes = nodes;
    }

    public SkillNode findNode(String id) {
        for (SkillNode node : nodes) {
            if (node.id().equals(id)) return node;
        }
        return null;
    }

    public static List<RPGSubclass> forClass(RPGClass parent) {
        return java.util.Arrays.stream(values())
                .filter(subclass -> subclass.parent == parent)
                .toList();
    }

    private static SkillNode node(String id, String name, String description, int cost,
                                  String prereq, Stat reqStat, int reqVal, Map<Stat, Integer> bonus) {
        return new SkillNode(id, name, description, cost, prereq, reqStat, reqVal, bonus);
    }
}
