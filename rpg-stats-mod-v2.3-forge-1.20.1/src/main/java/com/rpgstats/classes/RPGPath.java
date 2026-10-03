package com.rpgstats.classes;

import com.rpgstats.tree.SkillNode;
import java.util.Arrays;
import java.util.List;

/**
 * Grande caminho interno de uma classe. Em v1.5 o Mago e a primeira classe
 * totalmente convertida para este modelo; as demais serao migradas depois.
 */
public enum RPGPath {
    MAGE_ELEMENTAL(RPGClass.MAGO, "Casa Elemental", "Reações de fogo, gelo e raio · DPS/controle", MageTrees.pathElemental()),
    MAGE_ARCANA(RPGClass.MAGO, "Casa Arcana", "Combos, selos, manipulação de magia · técnico", MageTrees.pathArcana()),
    MAGE_CONJURATION(RPGClass.MAGO, "Casa da Conjuração", "Invocações, vínculos e construtos · controle/suporte", MageTrees.pathConjuration()),
    MAGE_OCCULT(RPGClass.MAGO, "Casa Oculta", "Corrupção, sangue e maldições · risco/recompensa", MageTrees.pathOccult()),
    MAGE_TEMPORAL(RPGClass.MAGO, "Casa Temporal", "Cooldown, stasis e reversão · controle/utility", MageTrees.pathTemporal()),

    WAR_VANGUARD(RPGClass.GUERREIRO, "Vanguarda", "Guarda, provocação e controle da linha de frente", ClassTrees.path("war_van", "Vanguarda", "Guarda acumulada reduz impactos frontais")),
    WAR_BERSERKER(RPGClass.GUERREIRO, "Fúria Carmesim", "Risco, ferimentos e dano sustentado", ClassTrees.path("war_bers", "Fúria Carmesim", "Vida perdida alimenta Fúria sem permitir suicídio")),
    WAR_WEAPONMASTER(RPGClass.GUERREIRO, "Mestre de Armas", "Troca de armas, guarda e técnica", ClassTrees.path("war_weap", "Mestre de Armas", "Alternar categorias de arma cria Aberturas")),
    WAR_RUNIC(RPGClass.GUERREIRO, "Vínculo Rúnico", "Combate híbrido, runas e antimagia", ClassTrees.path("war_rune", "Vínculo Rúnico", "Golpes armazenam Cargas Rúnicas")),
    WAR_COMMANDER(RPGClass.GUERREIRO, "Comandante", "Bandeiras, formação e suporte marcial", ClassTrees.path("war_cmd", "Comandante", "Ações coordenadas geram Moral")),

    ARC_MARKSMAN(RPGClass.ARQUEIRO, "Atirador", "Precisão, distância e pontos fracos", ClassTrees.path("arc_mark", "Atirador", "Projéteis entre 9 e 28 blocos acumulam Mira")),
    ARC_WARDEN(RPGClass.ARQUEIRO, "Guardião Selvagem", "Companheiros, armadilhas e sobrevivência", ClassTrees.path("arc_ward", "Guardião Selvagem", "Acertos à distância marcam a presa e geram Instinto")),
    ARC_SKIRMISHER(RPGClass.ARQUEIRO, "Escaramuçador", "Mobilidade, ritmo e disparos em movimento", ClassTrees.path("arc_skirm", "Escaramuçador", "Mover-se e acertar projéteis acumulam Momentum")),
    ARC_ARCANE(RPGClass.ARQUEIRO, "Arqueiro Arcano", "Flechas elementais e magia à distância", ClassTrees.path("arc_magic", "Arqueiro Arcano", "A técnica alterna Fogo/Gelo/Tempestade; disparos aplicam a afinidade atual")),
    ARC_ARTIFICER(RPGClass.ARQUEIRO, "Artífice", "Bestas, munições e engenhocas", ClassTrees.path("arc_art", "Artífice", "Disparos e técnicas acumulam Cargas de Dispositivo")),

    ASS_SHADOW(RPGClass.ASSASSINO, "Sombra", "Furtividade, abertura e reposicionamento", ClassTrees.path("ass_shadow", "Sombra", "Atacar pelas costas ou após preparação abre uma Abertura para um único golpe")),
    ASS_VENOM(RPGClass.ASSASSINO, "Veneno", "Toxinas, doses e desgaste", ClassTrees.path("ass_venom", "Veneno", "Golpes corpo a corpo acumulam Doses e aplicam Veneno")),
    ASS_DUELIST(RPGClass.ASSASSINO, "Duelista", "Parry, contra-ataque e dança de lâminas", ClassTrees.path("ass_duel", "Duelista", "Golpes em até 2,5s mantêm a cadência; parries bem-sucedidos armam Riposta e geram Vantagem")),
    ASS_SABOTEUR(RPGClass.ASSASSINO, "Sabotador", "Armadilhas, bombas e preparação", ClassTrees.path("ass_sabo", "Sabotador", "Acertos geram Preparação; Dispositivo Armado controla o próximo alvo atingido")),
    ASS_MYSTIC(RPGClass.ASSASSINO, "Lâmina Mística", "Alma, vazio e maldições corpo a corpo", ClassTrees.path("ass_myst", "Lâmina Mística", "Golpes corpo a corpo acumulam Ecos de Alma"));

    public final RPGClass parent;
    public final String display;
    public final String desc;
    public final List<SkillNode> nodes;

    RPGPath(RPGClass parent, String display, String desc, List<SkillNode> nodes) {
        this.parent = parent;
        this.display = display;
        this.desc = desc;
        this.nodes = List.copyOf(nodes);
    }

    public SkillNode findNode(String id) {
        if (id == null) return null;
        for (SkillNode node : nodes) if (node.id().equals(id)) return node;
        return null;
    }

    public static List<RPGPath> forClass(RPGClass clazz) {
        return Arrays.stream(values()).filter(path -> path.parent == clazz).toList();
    }

    public static RPGPath ownerOfNode(String nodeId) {
        for (RPGPath path : values()) if (path.findNode(nodeId) != null) return path;
        return null;
    }
}
