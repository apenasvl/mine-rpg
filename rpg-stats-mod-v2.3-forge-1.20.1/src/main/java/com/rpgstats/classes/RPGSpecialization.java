package com.rpgstats.classes;

import com.rpgstats.tree.SkillNode;
import java.util.Arrays;
import java.util.List;

/** Especialização profunda escolhida dentro de uma Casa/Caminho. */
public enum RPGSpecialization {
    PYROMANCER(RPGPath.MAGE_ELEMENTAL, "Piromante", "Calor, combustão e pressão em área", MageTrees.specPyromancer()),
    CRYOMANCER(RPGPath.MAGE_ELEMENTAL, "Criomante", "Frio, barreiras e controle", MageTrees.specCryomancer()),
    STORMCALLER(RPGPath.MAGE_ELEMENTAL, "Tempestário", "Carga, correntes e mobilidade elétrica", MageTrees.specStormcaller()),

    RUNIST(RPGPath.MAGE_ARCANA, "Runista", "Runas preparadas, redes e zonas", MageTrees.specRunist()),
    ILLUSIONIST(RPGPath.MAGE_ARCANA, "Ilusionista", "Engano, evasão e ecos", MageTrees.specIllusionist()),
    TELEMANCER(RPGPath.MAGE_ARCANA, "Telemante", "Blink, espaço e singularidades", MageTrees.specTelemancer()),

    CONJURER(RPGPath.MAGE_CONJURATION, "Conjurador", "Familiares e invocações pesadas", MageTrees.specConjurer()),
    ANIMIST(RPGPath.MAGE_CONJURATION, "Animista", "Espíritos, auras e suporte", MageTrees.specAnimist()),
    ASTRAL_FORGER(RPGPath.MAGE_CONJURATION, "Forjador Astral", "Construtos e arsenal mágico", MageTrees.specAstralForger()),

    BLOODMANCER(RPGPath.MAGE_OCCULT, "Sanguimante", "Vida como recurso e sustain ofensivo", MageTrees.specBloodmancer()),
    CURSEWEAVER(RPGPath.MAGE_OCCULT, "Maledicente", "Maldições, ruína e execução", MageTrees.specCurseweaver()),
    HEXBLADE(RPGPath.MAGE_OCCULT, "Hexblade", "Magia corpo a corpo e imbuimentos", MageTrees.specHexblade()),

    ACCELERATOR(RPGPath.MAGE_TEMPORAL, "Acelerador", "Ritmo, mobilidade e recuperação", MageTrees.specAccelerator()),
    STAGNATOR(RPGPath.MAGE_TEMPORAL, "Estagnador", "Slow, stasis e controle de área", MageTrees.specStagnator()),
    REVERSER(RPGPath.MAGE_TEMPORAL, "Reversor", "Marcas temporais, rewind e segunda chance", MageTrees.specReverser()),

    BULWARK(RPGPath.WAR_VANGUARD, "Bastião", "Absorve pressão e protege aliados próximos", ClassTrees.spec("war_van_bulw", "Bastião", "Converte Guarda em barreira de equipe.")),
    WARLORD(RPGPath.WAR_VANGUARD, "Senhor da Guerra", "Controle de ameaça e liderança frontal", ClassTrees.spec("war_van_lord", "Senhor da Guerra", "Provoca inimigos e abre janelas para aliados.")),
    JUGGERNAUT(RPGPath.WAR_VANGUARD, "Juggernaut", "Avanço imparável e resistência a controle", ClassTrees.spec("war_van_jugg", "Juggernaut", "Ganha potência enquanto avança sob pressão.")),
    BLOOD_REAVER(RPGPath.WAR_BERSERKER, "Saqueador de Sangue", "Sustain agressivo sem cura infinita", ClassTrees.spec("war_bers_reav", "Saqueador de Sangue", "Ferimentos alimentam roubo de vida limitado.")),
    RAGEBORN(RPGPath.WAR_BERSERKER, "Nascido da Fúria", "Picos curtos de dano e velocidade", ClassTrees.spec("war_bers_rage", "Nascido da Fúria", "Consome toda Fúria para uma janela explosiva.")),
    PAIN_COLOSSUS(RPGPath.WAR_BERSERKER, "Colosso da Dor", "Dano recebido vira pressão controlada", ClassTrees.spec("war_bers_pain", "Colosso da Dor", "Armazena parte do dano e devolve sem refletir infinitamente.")),
    BLADEMASTER(RPGPath.WAR_WEAPONMASTER, "Mestre das Lâminas", "Combos rápidos com espada", ClassTrees.spec("war_weap_blade", "Mestre das Lâminas", "Sequências precisas reduzem a próxima recarga.")),
    DUEL_MASTER(RPGPath.WAR_WEAPONMASTER, "Mestre do Duelo", "Parry, guarda e contra-ataque", ClassTrees.spec("war_weap_duel", "Mestre do Duelo", "Defesas no tempo certo criam contra-ataques.")),
    TITAN_MAULER(RPGPath.WAR_WEAPONMASTER, "Quebra-Titãs", "Armas pesadas, interrupção e impacto", ClassTrees.spec("war_weap_titan", "Quebra-Titãs", "Ataques lentos acumulam impacto contra elites.")),
    RUNE_KNIGHT(RPGPath.WAR_RUNIC, "Cavaleiro Rúnico", "Imbuimentos e explosões rúnicas", ClassTrees.spec("war_rune_knight", "Cavaleiro Rúnico", "Consome cargas em dano híbrido moderado.")),
    SPELLBREAKER(RPGPath.WAR_RUNIC, "Quebra-Feitiços", "Antimagia, barreira e interrupção", ClassTrees.spec("war_rune_break", "Quebra-Feitiços", "Guarda runas para reduzir magia e projéteis.")),
    STORMBLADE(RPGPath.WAR_RUNIC, "Lâmina da Tempestade", "Mobilidade e corrente elétrica", ClassTrees.spec("war_rune_storm", "Lâmina da Tempestade", "Alterna golpes e avanços elétricos.")),
    BANNER_LORD(RPGPath.WAR_COMMANDER, "Porta-Estandarte", "Auras posicionais para o grupo", ClassTrees.spec("war_cmd_banner", "Porta-Estandarte", "Planta uma zona que fortalece formação.")),
    TACTICIAN(RPGPath.WAR_COMMANDER, "Estrategista", "Ordens, marcação e controle de ritmo", ClassTrees.spec("war_cmd_tact", "Estrategista", "Alterna ordens ofensivas e defensivas.")),
    IRON_GUARD(RPGPath.WAR_COMMANDER, "Guarda de Ferro", "Interceptação e escolta", ClassTrees.spec("war_cmd_guard", "Guarda de Ferro", "Divide parte do dano aliado com limite.")),

    SNIPER(RPGPath.ARC_MARKSMAN, "Franco-Atirador", "Dano preparado a longa distância", ClassTrees.spec("arc_mark_snipe", "Franco-Atirador", "Ficar parado acumula Estabilidade; a longa distância ela aumenta o dano.")),
    DEADEYE(RPGPath.ARC_MARKSMAN, "Olho Mortal", "Precisão sustentada no mesmo alvo", ClassTrees.spec("arc_mark_dead", "Olho Mortal", "Acertos consecutivos no mesmo alvo acumulam Precisão e dano até um teto.")),
    BALLISTICIAN(RPGPath.ARC_MARKSMAN, "Balístico", "Perfuração e alinhamento de alvos", ClassTrees.spec("arc_mark_ball", "Balístico", "A cada três acertos, uma perfuração pode atingir um único alvo atrás do primeiro.")),
    BEASTMASTER(RPGPath.ARC_WARDEN, "Mestre das Feras", "Companheiro e ataques coordenados", ClassTrees.spec("arc_ward_beast", "Mestre das Feras", "Escolhe a presa, ordena ataques dos próprios pets e protege a matilha com resistência temporária.")),
    TRAPPER(RPGPath.ARC_WARDEN, "Armadilheiro", "Armadilhas físicas e cobrança de capturas", ClassTrees.spec("arc_ward_trap", "Armadilheiro", "Prepara armadilhas físicas próprias; capturas marcam presas hostis para tiros de cobrança e reposicionamento territorial.")),
    SURVIVALIST(RPGPath.ARC_WARDEN, "Sobrevivencialista", "Resistência e adaptação sob pressão", ClassTrees.spec("arc_ward_surv", "Sobrevivencialista", "Recua sob pressão, converte Instinto em cura e prepara uma recuperação de emergência limitada.")),
    WINDRUNNER(RPGPath.ARC_SKIRMISHER, "Corredor do Vento", "Velocidade e disparo em movimento", ClassTrees.spec("arc_skirm_wind", "Corredor do Vento", "Projéteis e movimento acumulam Momentum, que fortalece os tiros.")),
    ACROBAT(RPGPath.ARC_SKIRMISHER, "Acrobata", "Saltos, ângulos e tiro aéreo", ClassTrees.spec("arc_skirm_acro", "Acrobata", "Tiros no ar recuperam Foco com recarga interna e recebem bônus de dano.")),
    GUERRILLA(RPGPath.ARC_SKIRMISHER, "Guerrilheiro", "Ataque, recuo e emboscada", ClassTrees.spec("arc_skirm_guer", "Guerrilheiro", "Após 4,5s sem acertar um projétil, o próximo acerto abre uma curta Emboscada.")),
    FLAMEBOW(RPGPath.ARC_ARCANE, "Arco Ígneo", "Queimadura e propagação elemental", ClassTrees.spec("arc_magic_fire", "Arco Ígneo", "Marca uma presa com o arco equipado; outro tiro consome a marca em dano suplementar e propagação de fogo limitada.")),
    FROSTBOW(RPGPath.ARC_ARCANE, "Arco Glacial", "Lentidão e controle de rotas", ClassTrees.spec("arc_magic_frost", "Arco Glacial", "Marca e desacelera a presa; um tiro posterior consome a marca em dano suplementar e controle de rotas limitado.")),
    STORMBOW(RPGPath.ARC_ARCANE, "Arco da Tempestade", "Correntes elétricas entre alvos", ClassTrees.spec("arc_magic_storm", "Arco da Tempestade", "Marca a presa com um tiro; outro tiro consome a marca em corrente limitada para hostis próximos.")),
    CROSSBOW_EXPERT(RPGPath.ARC_ARTIFICER, "Besteiro", "Janela preparada e Cargas de Dispositivo", ClassTrees.spec("arc_art_cross", "Besteiro", "Exige besta real; prepara tiros, gasta Cargas em perfuração e recompensa a salva uma única vez.")),
    BOMBARDIER(RPGPath.ARC_ARTIFICER, "Bombardeiro", "Munição explosiva de área limitada", ClassTrees.spec("arc_art_bomb", "Bombardeiro", "Prepara disparos para até dois hostis secundários e uma zona de supressão; respeita munição com área própria.")),
    ENGINEER(RPGPath.ARC_ARTIFICER, "Engenheiro", "Zona de controle e dispositivos", ClassTrees.spec("arc_art_eng", "Engenheiro", "Fixa uma âncora no chão; controla alvos próximos dela com tiros preparados e pulsos limitados.")),

    NIGHTBLADE(RPGPath.ASS_SHADOW, "Lâmina Noturna", "Furtividade, primeiro golpe e retirada", ClassTrees.spec("ass_shadow_night", "Lâmina Noturna", "Consumir uma Abertura fortalece o primeiro golpe e, com o Motor, prepara uma fuga curta.")),
    PHANTOM(RPGPath.ASS_SHADOW, "Fantasma", "Redução curta e ataque na saída de fase", ClassTrees.spec("ass_shadow_phant", "Fantasma", "Fase reduz dano recebido; durante a Saída de Fase, golpes ganham dano e abrem fuga.")),
    EXECUTIONER(RPGPath.ASS_SHADOW, "Executor", "Finalização de alvos enfraquecidos", ClassTrees.spec("ass_shadow_exec", "Executor", "Ganha dano abaixo de 30% de vida, ou 18% em bosses; golpes nessa faixa também geram Combo.")),
    ALCHEMIST(RPGPath.ASS_VENOM, "Alquimista", "Fórmulas e acúmulo acelerado de Doses", ClassTrees.spec("ass_venom_alch", "Alquimista", "Alterna fórmulas; no modo Desgaste, cada golpe adiciona uma Dose extra.")),
    PLAGUEBRINGER(RPGPath.ASS_VENOM, "Arauto da Peste", "Propagação de Veneno em alvos carregados", ClassTrees.spec("ass_venom_plague", "Arauto da Peste", "Com 4+ Doses ganha dano; a Conversão espalha Veneno para alvos próximos.")),
    TOXICOLOGIST(RPGPath.ASS_VENOM, "Toxicologista", "Modos de veneno e controle", ClassTrees.spec("ass_venom_toxic", "Toxicologista", "Alterna modos: Potência aumenta o Veneno e Controle aplica Lentidão.")),
    FENCER(RPGPath.ASS_DUELIST, "Esgrimista", "Precisão e contra-tempo", ClassTrees.spec("ass_duel_fence", "Esgrimista", "Quebrar a cadência de 2,5s fortalece o primeiro golpe e gera Vantagem; a técnica força essa quebra.")),
    BLADE_DANCER(RPGPath.ASS_DUELIST, "Dançarino de Lâminas", "Combo e troca de alvos", ClassTrees.spec("ass_duel_dance", "Dançarino de Lâminas", "Golpes geram Dança; trocar de alvo gera muito mais e a técnica amplia essa janela.")),
    COUNTERBLADE(RPGPath.ASS_DUELIST, "Contra-Lâmina", "Parry, Riposta e controle", ClassTrees.spec("ass_duel_counter", "Contra-Lâmina", "Parry corpo a corpo arma Riposta; o Motor converte a resposta em Fraqueza e Resistência curta.")),
    DEMOLITIONIST(RPGPath.ASS_SABOTEUR, "Demolidor", "Dano armazenado e detonação limitada", ClassTrees.spec("ass_sabo_demo", "Demolidor", "Durante Demolição armazena parte do dano; o Motor detona com limite e a Conversão adiciona splash.")),
    INFILTRATOR(RPGPath.ASS_SABOTEUR, "Infiltrador", "Entrada preparada e retirada", ClassTrees.spec("ass_sabo_infil", "Infiltrador", "A técnica abre uma Abertura; acertar durante ela fortalece o golpe e cria uma rota de fuga.")),
    WIREMASTER(RPGPath.ASS_SABOTEUR, "Mestre dos Fios", "Lentidão e Fraqueza por dispositivo", ClassTrees.spec("ass_sabo_wire", "Mestre dos Fios", "Dispositivo Armado aplica Lentidão; com o Motor, alvos lentos também recebem Fraqueza.")),
    HEXKILLER(RPGPath.ASS_MYSTIC, "Caçador de Bruxos", "Janela ofensiva de Caça Hex", ClassTrees.spec("ass_myst_hex", "Caçador de Bruxos", "Caça Hex concede dano corpo a corpo e marca os alvos atingidos; não detecta conjuradores nem rouba recurso.")),
    SOULKNIFE(RPGPath.ASS_MYSTIC, "Lâmina da Alma", "Dano híbrido e consumo de Ecos", ClassTrees.spec("ass_myst_soul", "Lâmina da Alma", "Ecos fortalecem golpes; o Motor consome até dois Ecos em um corte mágico separado.")),
    VOIDWALKER(RPGPath.ASS_MYSTIC, "Andarilho do Vazio", "Blink, controle e risco", ClassTrees.spec("ass_myst_void", "Andarilho do Vazio", "Blink abre Dívida do Vazio; o próximo golpe converte a dívida em controle e mobilidade."));

    public final RPGPath parent;
    public final String display;
    public final String desc;
    public final List<SkillNode> nodes;

    RPGSpecialization(RPGPath parent, String display, String desc, List<SkillNode> nodes) {
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

    public static List<RPGSpecialization> forPath(RPGPath path) {
        return Arrays.stream(values()).filter(spec -> spec.parent == path).toList();
    }

    public static RPGSpecialization ownerOfNode(String nodeId) {
        for (RPGSpecialization spec : values()) if (spec.findNode(nodeId) != null) return spec;
        return null;
    }
}
