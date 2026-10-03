package com.rpgstats.classes;

import com.rpgstats.tree.SkillNode;
import java.util.List;
import java.util.Map;

/** Dados balanceados da árvore expandida do Mago (v1.5). */
public final class MageTrees {
    private MageTrees() {}

    private static SkillNode n(String id, String name, String desc, int cost, String prereq, int level, int tier, boolean cross) {
        return new SkillNode(id, name, desc, cost, prereq, null, 0, Map.of(), level, tier, cross);
    }

    public static List<SkillNode> core() {
        return List.of(
                n("mag_core_awakening", "Despertar Arcano", "Desbloqueia a base do Mago, +20 Mana máxima.", 1, null, 1, 1, false),
                n("mag_core_channeling", "Canalização", "Regenera +0,4 Mana por segundo.", 1, "mag_core_awakening", 2, 2, false),
                n("mag_core_reserve", "Reserva Mental", "+10% Mana máxima.", 1, "mag_core_awakening", 2, 2, false),
                n("mag_core_precision", "Precisão Mística", "+3% chance de crítico mágico.", 1, "mag_core_awakening", 2, 2, false),
                n("mag_core_efficiency", "Conjuração Eficiente", "Reduz custos de Mana em 4%.", 1, "mag_core_channeling", 4, 3, false),
                n("mag_core_stable_mind", "Mente Estável", "Perde 25% menos Mana ao sofrer dano.", 1, "mag_core_reserve", 4, 3, false),
                n("mag_core_flow", "Fluxo Arcano", "Após 3 habilidades mágicas diferentes, a próxima custa 12% menos Mana.", 1, "mag_core_efficiency", 6, 4, false),
                n("mag_core_weaving", "Tecelagem Mágica", "Após mobilidade, a próxima magia ofensiva em 4s causa +6% dano.", 1, "mag_core_precision", 6, 4, false),
                n("mag_core_guard", "Guarda Arcana", "Gastar 35 Mana em 3s concede 4 HP de absorção. CD interno 15s.", 2, "mag_core_stable_mind", 8, 5, false),
                n("mag_core_knowledge", "Conhecimento Arcano", "+5% poder mágico.", 1, "mag_core_flow", 8, 5, false),
                n("mag_core_echo", "Eco Arcano", "8% de chance de repetir um projétil mágico com 35% do dano. CD interno 8s.", 2, "mag_core_weaving", 8, 5, false),
                n("mag_core_mastery", "Maestria Arcana", "Desbloqueia a escolha de uma Casa Arcana no nível 10.", 1, "mag_core_knowledge", 10, 6, false)
        );
    }

    public static List<SkillNode> pathElemental() {
        return List.of(
                n("mag_ele_affinity", "Afinidade Elemental", "+4% dano elemental.", 1, null, 10, 1, true),
                n("mag_ele_resonance", "Ressonância Elemental", "Fogo, gelo e raio passam a aplicar marcas elementais.", 1, "mag_ele_affinity", 10, 2, true),
                n("mag_ele_catalyst", "Catalisador", "Reações elementais causam +10% dano.", 1, "mag_ele_resonance", 12, 3, true),
                n("mag_ele_burst", "Explosão Elemental", "MODIFICADOR · Reações de spells Fire/Ice/Lightning recebem +10% potência de detonação.", 2, "mag_ele_catalyst", 14, 4, false),
                n("mag_ele_opposites", "Elementos Opostos", "Aplicar elemento diferente em alvo marcado causa Choque Elemental. CD por alvo 2s.", 1, "mag_ele_resonance", 14, 4, false),
                n("mag_ele_instability", "Instabilidade", "Consumir duas marcas gera pequena explosão em área.", 1, "mag_ele_opposites", 16, 5, false),
                n("mag_ele_conservation", "Conservação Elemental", "Magias elementais custam 5% menos Mana.", 1, "mag_ele_catalyst", 16, 5, false),
                n("mag_ele_perfect_reaction", "Reação Perfeita", "Reações geram +6 Mana, com limite por conjuração.", 1, "mag_ele_instability", 19, 6, false),
                n("mag_ele_mastery", "Mestre Elemental", "+5% dano elemental e acesso às especializações elementais.", 2, "mag_ele_perfect_reaction", 22, 7, false)
        );
    }

    public static List<SkillNode> pathArcana() {
        return List.of(
                n("mag_arc_sequence", "Sequência Arcana", "Habilidades passam a registrar categorias para combos arcanos.", 1, null, 10, 1, true),
                n("mag_arc_triad", "Tríade", "Usar 3 categorias diferentes gera um Selo Arcano.", 1, "mag_arc_sequence", 10, 2, true),
                n("mag_arc_precision", "Precisão Arcana", "+4% crítico mágico.", 1, "mag_arc_triad", 12, 3, true),
                n("mag_arc_pulse", "Pulso Arcano", "MODIFICADOR · Spells Evocation de área recebem +8% controle de pulso/empurrão.", 2, "mag_arc_precision", 14, 4, false),
                n("mag_arc_preparation", "Preparação", "Ficar 3s sem lançar magia reduz em 10% o custo da próxima.", 1, "mag_arc_triad", 14, 4, false),
                n("mag_arc_finisher", "Finalizador", "Consome um Selo Arcano para dar +12% na próxima magia.", 1, "mag_arc_preparation", 16, 5, false),
                n("mag_arc_counterspell", "Contrafeitiço", "MODIFICADOR · Counterspell do Iron's concede proteção curta e limitada após sucesso.", 2, "mag_arc_precision", 16, 5, false),
                n("mag_arc_perfect_cycle", "Ciclo Perfeito", "Completar Tríade restaura 4 Mana.", 1, "mag_arc_finisher", 19, 6, false),
                n("mag_arc_high_magic", "Alta Magia", "+5% poder mágico e acesso às especializações arcanas.", 2, "mag_arc_perfect_cycle", 22, 7, false)
        );
    }

    public static List<SkillNode> pathConjuration() {
        return List.of(
                n("mag_conj_pact", "Pacto de Vínculo", "Desbloqueia Pontos de Vínculo para invocações e construtos.", 1, null, 10, 1, true),
                n("mag_conj_vitality", "Vitalidade Conjurada", "Invocações virtuais ganham +10% duração/estabilidade.", 1, "mag_conj_pact", 10, 2, true),
                n("mag_conj_power", "Poder Conjurado", "Invocações recebem +8% dano.", 1, "mag_conj_vitality", 12, 3, true),
                n("mag_conj_focus_order", "Ordem: Foco", "ATIVA · 8s. Faz invocações focarem o alvo mirado.", 2, "mag_conj_power", 14, 4, false),
                n("mag_conj_shared_flow", "Fluxo Compartilhado", "Dano de invocações restaura pequenas quantidades de Mana, com cap por segundo.", 1, "mag_conj_power", 14, 4, false),
                n("mag_conj_defensive", "Formação Defensiva", "Com invocação próxima, até +5% redução de dano.", 1, "mag_conj_vitality", 16, 5, false),
                n("mag_conj_extended_bond", "Elo Ampliado", "+2 Pontos de Vínculo.", 2, "mag_conj_focus_order", 16, 5, false),
                n("mag_conj_recycling", "Reciclagem Arcana", "Quando uma invocação expira/morre, recupera 15% do custo, com CD interno.", 1, "mag_conj_extended_bond", 19, 6, false),
                n("mag_conj_mastery", "Mestre Conjurador", "+1 Vínculo e acesso às especializações de conjuração.", 2, "mag_conj_recycling", 22, 7, false)
        );
    }

    public static List<SkillNode> pathOccult() {
        return List.of(
                n("mag_occ_forbidden", "Conhecimento Proibido", "Desbloqueia Corrupção Arcana: mais poder em troca de risco.", 1, null, 10, 1, true),
                n("mag_occ_temptation", "Tentação", "Com 25+ Corrupção: +4% dano mágico.", 1, "mag_occ_forbidden", 10, 2, true),
                n("mag_occ_instability", "Poder Instável", "Com 50+ Corrupção: bônus total sobe para +8%.", 1, "mag_occ_temptation", 12, 3, true),
                n("mag_occ_forbidden_power", "Poder Proibido", "Com 75+ Corrupção: +12% dano mágico, mas +8% dano recebido.", 1, "mag_occ_instability", 14, 4, false),
                n("mag_occ_drain", "Dreno Arcano", "+3% roubo de vida mágico; eficiência menor em bosses.", 1, "mag_occ_instability", 14, 4, false),
                n("mag_occ_efficiency", "Eficiência Proibida", "Com 50+ Corrupção: -6% custo de Mana.", 1, "mag_occ_temptation", 16, 5, false),
                n("mag_occ_backlash", "Reação Adversa", "Receber um golpe forte dissipa parte da Corrupção.", 1, "mag_occ_forbidden_power", 16, 5, false),
                n("mag_occ_purification", "Purificação", "ATIVA · 20s. Remove 40 Corrupção.", 2, "mag_occ_backlash", 19, 6, false),
                n("mag_occ_ritual", "Ritual Oculto", "+4% poder mágico e acesso às especializações ocultas.", 2, "mag_occ_purification", 22, 7, false)
        );
    }

    public static List<SkillNode> pathTemporal() {
        return List.of(
                n("mag_time_sensitivity", "Sensibilidade Temporal", "Desbloqueia Fragmentos Temporais.", 1, null, 10, 1, true),
                n("mag_time_rhythm", "Ritmo Temporal", "3 magias em 5s geram 1 Fragmento, com CD interno.", 1, "mag_time_sensitivity", 10, 2, true),
                n("mag_time_efficiency", "Eficiência Temporal", "+5% recuperação de cooldown.", 1, "mag_time_rhythm", 12, 3, true),
                n("mag_time_shift", "Deslocamento Temporal", "MODIFICADOR · Spells de mobilidade ativam preparação temporal e +6% na próxima magia.", 2, "mag_time_efficiency", 14, 4, false),
                n("mag_time_stored_moment", "Momento Guardado", "Gasta 1 Fragmento para reduzir em 20% o primeiro dano recebido. CD 18s.", 1, "mag_time_rhythm", 14, 4, false),
                n("mag_time_recovery", "Recuperação", "Gastar Fragmento reduz levemente um cooldown ativo.", 1, "mag_time_efficiency", 16, 5, false),
                n("mag_time_expanded_memory", "Memória Expandida", "+2 Fragmentos máximos.", 2, "mag_time_recovery", 16, 5, false),
                n("mag_time_continuity", "Continuidade", "Fragmentos demoram mais para desaparecer fora de combate.", 1, "mag_time_expanded_memory", 19, 6, false),
                n("mag_time_mastery", "Mestre do Tempo", "+5% recuperação de cooldown e acesso às especializações temporais.", 2, "mag_time_continuity", 22, 7, false)
        );
    }

    public static List<SkillNode> specPyromancer() {
        return List.of(
                n("mag_pyr_ember_lance", "Domínio das Brasas", "MODIFICADOR · Fire spells do Iron's aplicam +5 Calor.", 2, null, 25, 1, false),
                n("mag_pyr_fuel", "Combustível", "Alvos com 50+ Calor recebem +6% dano de fogo.", 1, "mag_pyr_ember_lance", 30, 2, false),
                n("mag_pyr_flame_wall", "Parede Incandescente", "MODIFICADOR · Wall of Fire e Fire spells de área aplicam +8 Calor.", 2, "mag_pyr_fuel", 35, 3, false),
                n("mag_pyr_combustion", "Combustão", "100 Calor detona por 6 HP em área; bosses têm CD interno.", 1, "mag_pyr_flame_wall", 40, 4, false),
                n("mag_pyr_ash_step", "Passo de Cinzas", "MODIFICADOR · Burning Dash arma +6% para a próxima Fire spell.", 2, "mag_pyr_combustion", 45, 5, false),
                n("mag_pyr_controlled_inferno", "Inferno Controlado", "Com 60+ Calor: +8% crítico de fogo contra o alvo.", 1, "mag_pyr_ash_step", 48, 6, false),
                n("mag_pyr_asc_phoenix", "ASCENSÃO · Coração da Fênix", "No nível 50, dano fatal pode reviver com 30% HP e 25% Mana. CD 180s.", 3, "mag_pyr_controlled_inferno", 50, 7, false)
        );
    }

    public static List<SkillNode> specCryomancer() {
        return List.of(
                n("mag_cryo_ice_shard", "Domínio Glacial", "MODIFICADOR · Ice spells do Iron's aplicam +5 Frio.", 2, null, 25, 1, false),
                n("mag_cryo_fragility", "Fragilidade Térmica", "100 Frio consome a marca e arma +8% bruto na próxima magia em 3s, inclusive contra bosses. Funciona desde o nível 30.", 1, "mag_cryo_ice_shard", 30, 2, false),
                n("mag_cryo_ice_barrier", "Barreira Glacial", "MODIFICADOR · Ice Block/Shield ativam 10% redução de dano por 6s.", 2, "mag_cryo_fragility", 35, 3, false),
                n("mag_cryo_frost_nova", "Onda Congelante", "MODIFICADOR · Frostwave/Cone of Cold aplicam +8 Frio.", 2, "mag_cryo_ice_barrier", 40, 4, false),
                n("mag_cryo_deep_freeze", "Congelamento Profundo", "100 Frio congela mobs por 1,25s; boss recebe slow por 3s e Fragilidade, se comprada.", 1, "mag_cryo_frost_nova", 45, 5, false),
                n("mag_cryo_shatter", "Estilhaçar", "Inimigo congelado que morre explode por 3 HP em área. CD por proc 1s.", 1, "mag_cryo_deep_freeze", 48, 6, false),
                n("mag_cryo_asc_winterheart", "ASCENSÃO · Coração Invernal", "Uma magia de gelo ativa 8s: +25% Frio, +8% dano bruto e -8% custo. CD 40s.", 3, "mag_cryo_shatter", 50, 7, false)
        );
    }

    public static List<SkillNode> specStormcaller() {
        return List.of(
                n("mag_storm_arc_bolt", "Condutividade", "MODIFICADOR · Lightning spells do Iron's aplicam Carga Estática.", 2, null, 25, 1, false),
                n("mag_storm_static_charge", "Carga Estática", "Raio aplica Carga, até 4 stacks por alvo.", 1, "mag_storm_arc_bolt", 30, 2, false),
                n("mag_storm_lightning_step", "Passo Relâmpago", "MODIFICADOR · Charge: +15% movimento por 3s; próxima magia elétrica em 4s ganha +5% bruto.", 2, "mag_storm_static_charge", 35, 3, false),
                n("mag_storm_static_field", "Campo Estático", "MODIFICADOR · Lightning spells de área mantêm Carga/Sobrecarga.", 2, "mag_storm_lightning_step", 40, 4, false),
                n("mag_storm_overload", "Sobrecarga", "4 Cargas: próxima magia elétrica consome as stacks e causa +5 HP (+3 em boss).", 1, "mag_storm_static_field", 45, 5, false),
                n("mag_storm_conductor", "Condutor da Tempestade", "Chain Lightning/Ball Lightning/Thunderstorm recebem +6% dano bruto dentro dos limites.", 1, "mag_storm_overload", 48, 6, false),
                n("mag_storm_asc_avatar", "ASCENSÃO · Avatar da Tempestade", "ATIVA · 100 Mana · 40s. Por 8s: +15% velocidade de conjuração e +10% movimento.", 3, "mag_storm_conductor", 50, 7, false)
        );
    }

    public static List<SkillNode> specRunist() {
        return List.of(
                n("mag_rune_inscribe", "Inscrever Runa", "ATIVA · 18 Mana. Coloca uma runa no chão; máximo inicial 3.", 2, null, 25, 1, false),
                n("mag_rune_explosive", "Runa Explosiva", "Melhora a runa arcana básica (3 HP) para uma runa explosiva de 6 HP quando um inimigo entra na área.", 1, "mag_rune_inscribe", 30, 2, false),
                n("mag_rune_protection", "Runa de Proteção", "Alterna a runa para proteção, concedendo 4 HP de absorção a aliados próximos.", 1, "mag_rune_inscribe", 35, 3, false),
                n("mag_rune_amplifier", "Runa Amplificadora", "Runas próximas recebem +15% eficiência.", 1, "mag_rune_protection", 40, 4, false),
                n("mag_rune_network", "Rede Rúnica", "Runas próximas podem disparar em cadeia sem recursão infinita.", 2, "mag_rune_explosive", 45, 5, false),
                n("mag_rune_geometry", "Geometria Superior", "Limite de runas aumenta para 5.", 1, "mag_rune_network", 48, 6, false),
                n("mag_rune_asc_grand_circle", "ASCENSÃO · Grande Círculo", "ATIVA · 60 Mana · 60s. Área por 10s: runas dentro recebem +25% potência.", 3, "mag_rune_geometry", 50, 7, false)
        );
    }

    public static List<SkillNode> specIllusionist() {
        return List.of(
                n("mag_illu_mirror_image", "Imagem Espelhada", "MODIFICADOR · Scapegoat/Invisibility concedem cargas de ilusão.", 2, null, 25, 1, false),
                n("mag_illu_illusory_step", "Passo Ilusório", "MODIFICADOR · Teleport prepara Ataque Fantasma por 4s, quando esse talento estiver comprado.", 2, "mag_illu_mirror_image", 30, 2, false),
                n("mag_illu_confusion", "Confusão", "MODIFICADOR · Slow/Root no alvo escolhido reduzem seu dano contra você em 10% por 3s.", 2, "mag_illu_illusory_step", 35, 3, false),
                n("mag_illu_ghost_attack", "Ataque Fantasma", "Invisibility/Teleport: primeira magia ofensiva em 4s ganha +20% dano bruto, sujeito aos limites.", 1, "mag_illu_confusion", 40, 4, false),
                n("mag_illu_perfect_decoy", "Isca Perfeita", "Primeiro ataque direto periódico pode consumir uma carga de ilusão em vez de causar dano.", 1, "mag_illu_ghost_attack", 45, 5, false),
                n("mag_illu_arcane_reflection", "Reflexo Arcano", "Com ilusão ativa, 12% de chance de ecoar 20% do dano da magia. CD interno.", 1, "mag_illu_perfect_decoy", 48, 6, false),
                n("mag_illu_asc_mirror_hall", "ASCENSÃO · Salão dos Espelhos", "ATIVA · 55 Mana · 60s. 5 cargas; a cada 3ª magia, uma cópia causa 25% do dano.", 3, "mag_illu_arcane_reflection", 50, 7, false)
        );
    }

    public static List<SkillNode> specTelemancer() {
        return List.of(
                n("mag_tele_blink", "Domínio do Teleporte", "MODIFICADOR · Teleport do Iron's ativa Compressão espacial.", 2, null, 25, 1, false),
                n("mag_tele_repulsion", "Repulsão", "MODIFICADOR · Telekinesis/Throw/Gust recebem controle espacial limitado.", 2, "mag_tele_blink", 30, 2, false),
                n("mag_tele_anchor", "Âncora Espacial", "ATIVA · 22 Mana · 12s. Marca a posição; reusar dentro de 8s retorna ao ponto.", 2, "mag_tele_repulsion", 35, 3, false),
                n("mag_tele_double_portal", "Mestre dos Portais", "MODIFICADOR · Portal do Iron's ativa preparação e Compressão.", 2, "mag_tele_anchor", 40, 4, false),
                n("mag_tele_compression", "Compressão", "Após usar teleporte, a próxima magia em 4s causa +12%.", 1, "mag_tele_double_portal", 45, 5, false),
                n("mag_tele_singularity", "Singularidade", "MODIFICADOR · Black Hole/Gravity Fissure recebem +10% dano bruto dentro dos limites.", 2, "mag_tele_compression", 48, 6, false),
                n("mag_tele_asc_collapse", "ASCENSÃO · Colapso Espacial", "MODIFICADOR · Black Hole/Gravity Fissure recebem +12% dano bruto, somando até 18% com Singularidade.", 3, "mag_tele_singularity", 50, 7, false)
        );
    }

    public static List<SkillNode> specConjurer() {
        return List.of(
                n("mag_summ_familiar", "Pacto do Familiar", "MODIFICADOR · Summons leves do Iron's passam a consumir Vínculo.", 2, null, 25, 1, false),
                n("mag_summ_guardian", "Pacto do Guardião", "MODIFICADOR · Summons resistentes recebem defesa de Vínculo.", 2, "mag_summ_familiar", 30, 2, false),
                n("mag_summ_assault_order", "Ordem: Assalto", "ATIVA · 12s. Invocações recebem +12% dano por 5s.", 1, "mag_summ_guardian", 35, 3, false),
                n("mag_summ_pack_tactics", "Táticas de Matilha", "Cada tipo adicional de invocação real concede +6% dano, até 15%.", 1, "mag_summ_assault_order", 40, 4, false),
                n("mag_summ_transfer", "Transferência Arcana", "ATIVA · 15 Mana · 12s. Cura invocações reais próximas em 20% da vida máxima.", 2, "mag_summ_pack_tactics", 45, 5, false),
                n("mag_summ_great_conjuration", "Grande Conjuração", "MODIFICADOR · Invocações reais recebem +15% dano dentro do limite de conjuração.", 2, "mag_summ_transfer", 48, 6, false),
                n("mag_summ_asc_ephemeral_army", "ASCENSÃO · Exército Efêmero", "ATIVA · 70 Mana · 60s. +3 Vínculo temporário e +15% dano dos summons por 10s.", 3, "mag_summ_great_conjuration", 50, 7, false)
        );
    }

    public static List<SkillNode> specAnimist() {
        return List.of(
                n("mag_anim_life_spirit", "Espírito da Vida", "POSTURA · Nature/Holy spells fortalecem a postura de cura limitada.", 2, null, 25, 1, false),
                n("mag_anim_earth_spirit", "Espírito da Terra", "POSTURA · Oakskin/Fortify fortalecem a postura defensiva.", 2, "mag_anim_life_spirit", 30, 2, false),
                n("mag_anim_hunt_spirit", "Espírito da Caça", "POSTURA · Nature/controle marcam alvos para a postura ofensiva.", 2, "mag_anim_earth_spirit", 35, 3, false),
                n("mag_anim_spirit_totem", "Totem Espiritual", "ATIVA · 25 Mana · 15s. Cria área de suporte por 8s.", 2, "mag_anim_hunt_spirit", 40, 4, false),
                n("mag_anim_spirit_swap", "Troca Espiritual", "ATIVA · 4s. Alterna rapidamente o espírito predominante sem custo alto.", 1, "mag_anim_spirit_totem", 45, 5, false),
                n("mag_anim_harmony", "Harmonia Ancestral", "Com dois efeitos espirituais ativos: +8% regen de Mana e +5% poder mágico.", 1, "mag_anim_spirit_swap", 48, 6, false),
                n("mag_anim_asc_council", "ASCENSÃO · Conselho Ancestral", "ATIVA · 70 Mana · 75s. Ativa vida, terra e caça juntos por 12s.", 3, "mag_anim_harmony", 50, 7, false)
        );
    }

    public static List<SkillNode> specAstralForger() {
        return List.of(
                n("mag_astral_blade", "Lâmina Astral", "MODIFICADOR · Echoing Strikes recebe +6% dano bruto dentro dos limites.", 2, null, 25, 1, false),
                n("mag_astral_shield", "Escudo Astral", "MODIFICADOR · Shield/Fang Ward ativam 6% redução de dano por 6s; formação defensiva aumenta para 10%.", 2, "mag_astral_blade", 30, 2, false),
                n("mag_astral_turret", "Artilharia Astral", "MODIFICADOR · Magic Missile/Arrow recebem +6% dano bruto dentro dos limites.", 2, "mag_astral_shield", 35, 3, false),
                n("mag_astral_formation", "Formação", "ATIVA · 4s. Alterna formação: defensiva melhora Escudo Astral; escolta dá +8% movimento com escudo/invocação.", 1, "mag_astral_turret", 40, 4, false),
                n("mag_astral_resonant", "Arsenal Ressonante", "Cada invocação real ou Escudo Astral ativo concede +4% poder mágico bruto, até 12%.", 1, "mag_astral_formation", 45, 5, false),
                n("mag_astral_core", "Núcleo Aprimorado", "Escudo Astral dura 15% mais (6,9s). +1 limite de Vínculo.", 1, "mag_astral_resonant", 48, 6, false),
                n("mag_astral_asc_arsenal", "ASCENSÃO · Arsenal Astral", "MODIFICADOR · Echoing Strikes/Magic Missile/Arrow recebem +12% bruto; bônus do arsenal limitado a 18%.", 3, "mag_astral_core", 50, 7, false)
        );
    }

    public static List<SkillNode> specBloodmancer() {
        return List.of(
                n("mag_blood_lance", "Hemomancia", "MODIFICADOR · Magias ofensivas de sangue ganham até +50% dano por nível/INT contra criaturas e aplicam 1 stack de Hemorragia. Cura nativa não acompanha o bônus.", 2, null, 25, 1, false),
                n("mag_blood_vital_conversion", "Conversão Vital", "Mana faltante pode usar vida, nunca reduzindo abaixo de 3 HP.", 1, "mag_blood_lance", 30, 2, false),
                n("mag_blood_hemorrhage", "Hemorragia", "Magias de sangue acumulam até 3 stacks: 1 HP por stack a cada 2s por 6s. Novos acertos renovam a duração sem adiar o pulso.", 1, "mag_blood_vital_conversion", 35, 3, false),
                n("mag_blood_transfusion", "Transfusão", "MODIFICADOR · Ray of Siphoning/Devour recebem +2% dreno dentro do cap.", 2, "mag_blood_hemorrhage", 40, 4, false),
                n("mag_blood_crimson_desperation", "Desespero Carmesim", "Abaixo de 40% HP: +10% poder mágico, -10% regen de Mana.", 1, "mag_blood_transfusion", 45, 5, false),
                n("mag_blood_blood_pact", "Pacto Sanguíneo", "Roubo de vida mágico pode chegar a 6%, com cap por hit.", 1, "mag_blood_crimson_desperation", 48, 6, false),
                n("mag_blood_asc_eclipse", "ASCENSÃO · Eclipse Carmesim", "ATIVA · 60s. 10s de +10% magia e 8% lifesteal; HP pode cobrir Mana sem causar suicídio.", 3, "mag_blood_blood_pact", 50, 7, false)
        );
    }

    public static List<SkillNode> specCurseweaver() {
        return List.of(
                n("mag_curse_weakness", "Maldição da Fraqueza", "MODIFICADOR · Acertos Blood/Eldritch reduzem em 10% o dano do alvo contra você por 6s.", 2, null, 25, 1, false),
                n("mag_curse_fragility", "Maldição da Fragilidade", "MODIFICADOR · Acertos Blood/Eldritch armam +8% bruto na próxima magia em 6s.", 2, "mag_curse_weakness", 30, 2, false),
                n("mag_curse_ruin", "Ruína", "MODIFICADOR · Blight/Wither Skull/Heartstop recebem sinergia de duração.", 2, "mag_curse_fragility", 35, 3, false),
                n("mag_curse_spread", "Propagação", "ATIVA · 24 Mana · 14s. Espalha uma maldição para até 3 inimigos com duração reduzida.", 2, "mag_curse_ruin", 40, 4, false),
                n("mag_curse_fate_mark", "Marca do Destino", "3 maldições diferentes fazem a próxima magia direta causar +25%.", 1, "mag_curse_spread", 45, 5, false),
                n("mag_curse_master", "Mestre das Maldições", "+20% duração das maldições.", 1, "mag_curse_fate_mark", 48, 6, false),
                n("mag_curse_asc_great_curse", "ASCENSÃO · Grande Maldição", "ATIVA · 60 Mana · 70s. Área aplica versões reduzidas de Fraqueza, Ruína e Fragilidade.", 3, "mag_curse_master", 50, 7, false)
        );
    }

    public static List<SkillNode> specHexblade() {
        return List.of(
                n("mag_hex_imbue", "Imbuimento Arcano", "Golpes melee podem gastar Mana para causar +1,5 dano mágico.", 1, null, 25, 1, false),
                n("mag_hex_blink_strike", "Blink Strike", "MODIFICADOR · Blood Step/Shadow Slash armam +4 dano mágico no próximo melee.", 2, "mag_hex_imbue", 30, 2, false),
                n("mag_hex_mystic_parry", "Parry Místico", "ATIVA · 20 Mana · 10s. Janela ~0,6s; reduz ataque em ~70% e gera Mana.", 2, "mag_hex_blink_strike", 35, 3, false),
                n("mag_hex_elemental_imbue", "Imbuimento Elemental", "ATIVA · 4s. Alterna Fogo/Gelo/Raio para os golpes imbuídos.", 2, "mag_hex_mystic_parry", 40, 4, false),
                n("mag_hex_spellblade_rhythm", "Ritmo Spellblade", "Alternar melee → magia → melee gera +3% dano por stack, máximo 3.", 1, "mag_hex_elemental_imbue", 45, 5, false),
                n("mag_hex_dimensional_cut", "Corte Dimensional", "MODIFICADOR · Shadow Slash fortalece o ritmo Spellblade em +10% controlado.", 2, "mag_hex_spellblade_rhythm", 48, 6, false),
                n("mag_hex_asc_arcane_form", "ASCENSÃO · Forma Arcana", "ATIVA · 70 Mana · 60s. 10s: +15% attack speed, +2 magia no melee, -15% Mana e +10% defesa.", 3, "mag_hex_dimensional_cut", 50, 7, false)
        );
    }

    public static List<SkillNode> specAccelerator() {
        return List.of(
                n("mag_acc_acceleration", "Aceleração", "MODIFICADOR · Haste/Charge geram Momentum e +10% ritmo dentro do cap.", 2, null, 25, 1, false),
                n("mag_acc_instant_cast", "Conjuração Instantânea", "ATIVA · 25 Mana · 15s. Próxima habilidade mágica não-ultimate recebe redução de cooldown após uso.", 2, "mag_acc_acceleration", 30, 2, false),
                n("mag_acc_temporal_step", "Passo Temporal", "MODIFICADOR · Teleport/steps reais alimentam Fragmentos e Momentum.", 2, "mag_acc_instant_cast", 35, 3, false),
                n("mag_acc_overclock", "Overclock", "ATIVA · 1 Fragmento · 8s. Reduz em 20% o cooldown da habilidade com maior recarga.", 1, "mag_acc_temporal_step", 40, 4, false),
                n("mag_acc_momentum", "Momentum", "Magias consecutivas geram até 3 stacks de +3% recuperação de cooldown.", 1, "mag_acc_overclock", 45, 5, false),
                n("mag_acc_fast_recovery", "Recuperação Acelerada", "Com 3+ Fragmentos: +20% regen de Mana.", 1, "mag_acc_momentum", 48, 6, false),
                n("mag_acc_asc_distorted_time", "ASCENSÃO · Tempo Distorcido", "ATIVA · 70 Mana · 70s. 8s de +20% ritmo, +15% movimento e +25% recuperação de cooldown.", 3, "mag_acc_fast_recovery", 50, 7, false)
        );
    }

    public static List<SkillNode> specStagnator() {
        return List.of(
                n("mag_stag_temporal_slow", "Lentidão Temporal", "MODIFICADOR · Slow no alvo escolhido reduz seu dano contra você em 10% por 6s.", 2, null, 25, 1, false),
                n("mag_stag_stasis_bubble", "Bolha de Stasis", "ATIVA · 30 Mana · 16s. Área por 5s: mobs -35%, projéteis -50%; boss -15%.", 2, "mag_stag_temporal_slow", 30, 2, false),
                n("mag_stag_temporal_anchor", "Âncora Temporal", "MODIFICADOR · Root no alvo escolhido reduz seu dano contra você em 10% por 6s.", 2, "mag_stag_stasis_bubble", 35, 3, false),
                n("mag_stag_time_lock", "Time Lock", "Slow/Root ativam 0,75s de imobilização em mobs; boss recebe slow moderado por 2s. CD 3s.", 1, "mag_stag_temporal_anchor", 40, 4, false),
                n("mag_stag_slow_horizon", "Horizonte Lento", "Projéteis hostis próximos perdem velocidade periodicamente.", 1, "mag_stag_time_lock", 45, 5, false),
                n("mag_stag_entropy", "Entropia", "Stasis: +6% dano mágico. Slow/Root também armam +6% bruto na próxima magia em 6s.", 1, "mag_stag_slow_horizon", 48, 6, false),
                n("mag_stag_asc_time_stop", "ASCENSÃO · Parada Temporal", "ATIVA · 80 Mana · 90s. Área por 4s: mobs ~80% slow; boss 25%; projéteis quase param.", 3, "mag_stag_entropy", 50, 7, false)
        );
    }

    public static List<SkillNode> specReverser() {
        return List.of(
                n("mag_rev_temporal_mark", "Marca Temporal", "ATIVA · 20 Mana · 15s. Salva posição e HP por 5s.", 2, null, 25, 1, false),
                n("mag_rev_rewind", "Retrocesso", "ATIVA · 12s. Volta à marca e recupera no máximo 4 HP perdidos.", 2, "mag_rev_temporal_mark", 30, 2, false),
                n("mag_rev_temporal_echo", "Eco Temporal", "ATIVA · 25 Mana · 18s. Próxima magia ofensiva não-ultimate repete com 40% do dano.", 2, "mag_rev_rewind", 35, 3, false),
                n("mag_rev_second_chance", "Segunda Chance", "Dano fatal deixa você em 1 HP. CD interno 120s.", 2, "mag_rev_temporal_echo", 40, 4, false),
                n("mag_rev_temporal_debt", "Dívida Temporal", "Parte da vida recuperada por Retrocesso volta como dano gradual, impedindo heal infinito.", 1, "mag_rev_second_chance", 45, 5, false),
                n("mag_rev_causal_loop", "Loop Causal", "Após Retrocesso, a próxima magia custa 30% menos Mana.", 1, "mag_rev_temporal_debt", 48, 6, false),
                n("mag_rev_asc_rewrite", "ASCENSÃO · Reescrever Destino", "ATIVA · 90 Mana · 120s. Grava 6s; retorna posição e até 60% de HP/Mana perdidos, com exaustão.", 3, "mag_rev_causal_loop", 50, 7, false)
        );
    }
}
