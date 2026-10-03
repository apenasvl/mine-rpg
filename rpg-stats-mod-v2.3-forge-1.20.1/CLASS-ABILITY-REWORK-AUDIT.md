# Auditoria de rework das classes

## Objetivo

Esta auditoria congela o design antes de qualquer alteração grande de gameplay. O problema confirmado não é falta de nodes: é que Guerreiro, Arqueiro e Assassino usam uma geometria e um resolvedor de efeitos quase totalmente genéricos, fazendo árvores grandes entregarem poucas mecânicas reais.

O Mago segue a arquitetura já definida em `MAGE-IRONS-AUDIT.md`: Iron's Spells fornece os feitiços lançáveis; RPG Stats fornece classe, Casas, especializações, recursos secundários, passivas e modificadores.

## Causa raiz confirmada no código

`ClassTrees.core`, `ClassTrees.path` e `ClassTrees.spec` geram a mesma sequência de nodes para todas as classes/caminhos/especializações. `ClassAbilityRegistry` resolve grande parte desses nodes apenas pelo sufixo (`_setup`, `_engine`, `_technique`, `_signature`, `_risk`, `_ascension` etc.) e escolhe uma pequena lista de ativas genéricas por substring do ID.

Consequência: duas especializações com fantasias completamente diferentes frequentemente recebem o mesmo efeito executável, mudando só valor, duração, custo ou cooldown.

### Decisão global

- `KEEP`: estrutura/progressão que já cumpre papel claro sem fingir ser mecânica nova.
- `MODIFY`: bônus numérico aceitável, mas precisa ficar ligado à identidade correta.
- `REWORK`: node existe e mantém posição/custo quando possível, porém ganha regra mecânica própria.
- `REMOVE`: node redundante sem função própria; seu espaço deve virar modifier/mechanic, não simplesmente desaparecer deixando a árvore menor.

Nenhuma linha principal deve ter mais de 1–2 upgrades consecutivos exclusivamente numéricos.

## Auditoria do template genérico

| Sufixo atual | Função atual | Decisão | Problema | Nova regra |
|---|---|---|---|---|
| `_core_awakening` | +3 recurso máximo | MODIFY | Mesmo efeito para as classes físicas | Desbloqueia o recurso/loop real da classe; bônus flat pode permanecer secundário |
| `_core_flow` | recurso por acerto | REWORK | Mesmo gatilho para todos | Guerreiro gera por pressão/postura; Arqueiro por precisão/distância; Assassino por abertura/combo |
| `_core_form` | +dano principal | MODIFY | Stat puro | Pode ficar como stat node pequeno |
| `_core_guard` | +redução de dano | REWORK | Defesa idêntica para todos | Defesa deve depender do verbo da classe: guarda, distância ou evasão/abertura |
| `_core_efficiency` | -custo | KEEP/MODIFY | Numérico aceitável | Manter pequeno e com cap |
| `_core_tempo` | próximo hit ganha dano | REWORK | Mesmo buff de rotação em todas | Cada classe recebe uma regra de ritmo própria |
| `_core_resolve` | defesa em HP baixo | MODIFY | Pode existir como safety net | Valores e condição diferentes por classe; não virar identidade central |
| `_core_combo` | 3 ações = recurso | REWORK | Combo artificial igual para todos | Combo precisa ser definido por ações próprias da classe |
| `_core_utility` | uma das poucas ativas genéricas | REWORK | Taunt/Arrow Rain/Smoke/Heal usados como molde universal | Técnica utilitária própria, não versão menor da signature |
| `_core_power` | +4% dano | KEEP | Stat node legítimo | Manter pequeno |
| `_core_sustain` | recurso por kill | MODIFY | Kill trigger genérico | Pode existir, mas ligado ao loop da classe e sem favorecer farm trivial |
| `_foundation` | texto de fantasia | REWORK | Quase sem mecânica executável | Deve criar a mecânica central da Casa/Caminho |
| `_discipline` | passiva genérica | REWORK | Não especializa o loop | Modifier do foundation |
| `_setup` | prepared_cast genérico | REWORK | Mesmo desconto após hit | Condição de preparação exclusiva do caminho |
| `_technique` | ativa genérica | REWORK | Reutiliza poucos `active_*` | Técnica característica do caminho |
| `_reaction` | proteção após habilidade | REWORK | Mesma reação em todos | Reação ligada à mecânica do caminho |
| `_synergy` | rotation_damage | REWORK | Mesmo próximo-hit buff | Sinergia específica entre ataques/recursos/posicionamento |
| `_economy` | -5% custo | MODIFY | Aceitável como stat de suporte | Aplicar apenas ao recurso/caminho correto |
| `_signature` | ativa mais forte do mesmo tipo | REWORK | Frequentemente só técnica com valor maior | Deve mudar comportamento ou exigir setup diferente |
| `_mastery` | libera especialização | KEEP | Função de progressão clara | Sem bônus escondido obrigatório |
| `_initiation` | passivas por substring | REWORK | Fantasia no texto, efeito genérico | Primeiro node deve ligar o motor real da especialização |
| `_engine` | prepared_cast | REWORK | Mesmo motor para todas | Estado/stacks/condição exclusiva |
| spec `_technique` | ativa genérica | REWORK | Pouca variedade real | Ferramenta característica da spec |
| `_conversion` | combo_resource | REWORK | Conversão não é realmente conversão | Transformar um recurso/estado específico em outro benefício |
| `_risk` | +dano e +dano recebido em HP baixo | REWORK | Mesmo risco/recompensa em dezenas de specs | Risco exclusivo da fantasia |
| spec `_signature` | mesma ativa mais forte | REWORK | Progressão só vertical | Finalizador preparado pela engine |
| `_ascension` | mesma família ativa com número maior | REWORK | Capstone sem mudança de loop | Estado temporário que altera regras da especialização |

## GUERREIRO

Identidade global: postura/stagger, guarda, impacto, Fúria, contra-ataque, execução e domínio de armas. Não deve ser apenas `melee_damage + damage_reduction`.

### Casas/Caminhos

| Caminho | Estado atual | Decisão | Mecânica central proposta |
|---|---|---|---|
| Vanguarda | defesa + `active_guard_taunt` repetido | REWORK | **Guarda** acumula ao bloquear/receber impacto frontal; gasta Guarda para interceptar, provocar e criar janela de contra-ataque |
| Fúria Carmesim | dano/lifesteal + Bloodlust | REWORK | **Fúria** cresce ao causar/receber pressão; escolhas entre gastar em burst ou manter para risco crescente |
| Mestre de Armas | buffs genéricos de melee/attack speed | REWORK | **Aberturas** surgem ao alternar categorias de arma/ataque; cada categoria consome Abertura de modo diferente |
| Vínculo Rúnico | melee + magic_power + Smite/AoE | REWORK | **Cargas Rúnicas** gravadas por golpes/guard; runas escolhidas alteram próximo ataque sem virar spellbook paralelo |
| Comandante | redução/heal + group heal | REWORK | **Moral/Ordens** geradas por ações coordenadas; ordens ofensiva/defensiva/avanço alteram equipe e próprio guerreiro |

### Especializações do Guerreiro

| Especialização | Decisão | Loop único obrigatório |
|---|---|---|
| Bastião | REWORK | bloquear -> armazenar Guarda -> converter em barreira/interceptação -> descarregar sem dano infinito |
| Senhor da Guerra | REWORK | provocar/marcar ameaça -> aliados acertam alvo -> gera Moral -> ordem de abertura |
| Juggernaut | REWORK | avançar sob pressão -> acumular Inércia -> resistir stagger -> impacto frontal consome Inércia |
| Saqueador de Sangue | REWORK | ferir -> gerar Sede -> lifesteal limitado em alvo ferido -> execução renova janela |
| Nascido da Fúria | REWORK | manter Fúria -> escolher momento -> consumir tudo em Frenesi curto -> exaustão posterior |
| Colosso da Dor | REWORK | dano recebido -> Dor armazenada com cap -> ataque pesado converte Dor em stagger/dano controlado |
| Mestre das Lâminas | REWORK | sequência de golpes distintos -> Cadência -> finisher rápido; errar quebra Cadência |
| Mestre do Duelo | REWORK | guarda/parry preciso -> Riposta armada -> próximo ataque ganha efeito de postura, não só dano |
| Quebra-Titãs | REWORK | ataque pesado/carregado -> Impacto -> elites/bosses acumulam quebra de postura resistente |
| Cavaleiro Rúnico | REWORK | gravar runa -> carregar com melee -> consumir em imbuimento/explosão pequena |
| Quebra-Feitiços | REWORK | bloquear dano mágico/projétil -> armazenar Selo -> próxima defesa/ataque interrompe ou reduz cast |
| Lâmina da Tempestade | REWORK | alternar avanço e golpe -> Carga -> corrente curta; ficar parado perde Carga |
| Porta-Estandarte | REWORK | posicionar estandarte -> lutar na formação -> gerar Moral -> reposicionar custa janela |
| Estrategista | REWORK | marcar prioridade -> alternar ordens -> cumprir condição -> próxima ordem melhora qualitativamente |
| Guarda de Ferro | REWORK | vincular aliado -> interceptar parcela limitada -> gerar Guarda -> contra-ataque protetor |

## ARQUEIRO

Identidade global: distância, precisão, trajetória, preparação, marcação, perfuração, ricochete, armadilhas e reposicionamento. Não deve ser apenas `ranged_damage + crit`.

### Casas/Caminhos

| Caminho | Decisão | Mecânica central proposta |
|---|---|---|
| Atirador | REWORK | **Mira** aumenta mantendo distância/linha de visão; movimento brusco ou erro reduz Mira |
| Guardião Selvagem | REWORK | **Instinto/Presa** conecta marcação, sobrevivência, companheiro e armadilhas |
| Escaramuçador | REWORK | **Momentum** só cresce ao mover/reposicionar entre disparos; ficar parado encerra bônus |
| Arqueiro Arcano | REWORK | **Marcas Elementais** nas flechas e reações simples; não copiar o arsenal de spells do Mago |
| Artífice | REWORK | **Cargas de Dispositivo** preparadas por recarga/munição; gastas em utilidade, bombas ou mecanismos |

### Especializações do Arqueiro

| Especialização | Decisão | Loop único obrigatório |
|---|---|---|
| Franco-Atirador | REWORK | permanecer estável -> revelar ponto fraco -> tiro carregado -> reposicionar após disparo |
| Olho Mortal | REWORK | acertos consecutivos precisos -> Precisão -> janela de crítico/execução; erro zera parte |
| Balístico | REWORK | alinhar alvos -> perfurar com perda por alvo -> recuperar valor com ângulo/overpenetration |
| Mestre das Feras | REWORK | marcar presa -> ordenar companheiro -> alternar agressão/proteção -> ataque coordenado |
| Armadilheiro | REWORK | preparar rota -> colocar armadilha -> conduzir presa -> detonar/consumir marca; bosses recebem slow/resistência |
| Sobrevivencialista | REWORK | adaptar-se a dano/bioma/combate -> escolher utilidade temporária -> sacrificar dano bruto |
| Corredor do Vento | REWORK | mover lateralmente -> Momentum -> disparo em movimento -> cadeia termina ao ficar parado |
| Acrobata | REWORK | salto/queda/reposicionamento preciso -> Ângulo -> próximo tiro ganha trajetória/utilidade; sem dodge universal |
| Guerrilheiro | REWORK | atacar -> romper linha de visão -> mudar posição -> reabrir janela de emboscada |
| Arco Ígneo | REWORK | flechas aplicam Calor -> acumular -> detonar controladamente; não lançar Fire spells |
| Arco Glacial | REWORK | flechas acumulam Frio -> slow -> quebrar Frio com tiro preparado; bosses resistem hard CC |
| Arco da Tempestade | REWORK | alternar alvos/ângulos -> Carga -> corrente para alvo secundário com teto |
| Besteiro | REWORK | recarga deliberada -> preparar rajada -> escolher perfuração ou estabilidade -> descarregar |
| Bombardeiro | REWORK | gerar cargas -> escolher munição explosiva -> área com limite por alvo/segundo |
| Engenheiro | REWORK | implantar dispositivo -> controlar zona -> manter/reposicionar -> sacrificar mobilidade por cobertura |

## ASSASSINO

Identidade global: abertura, marcas, veneno/sangramento, execução, combo, reposicionamento e risco. Não deve depender de uma esquiva universal removida.

### Casas/Caminhos

| Caminho | Decisão | Mecânica central proposta |
|---|---|---|
| Sombra | REWORK | **Abertura** criada ao quebrar visão/furtividade/reposicionar; só o primeiro golpe consome |
| Veneno | REWORK | **Doses** com tipos/limites; outras ações consomem doses para efeitos distintos |
| Duelista | REWORK | **Vantagem** por spacing/parry/counter, não por dodge universal |
| Sabotador | REWORK | **Preparação** por dispositivos colocados antes do confronto; controle espacial em vez de dano cru |
| Lâmina Mística | REWORK | **Ecos de Alma** roubados por condições específicas e consumidos em melee híbrido |

### Especializações do Assassino

| Especialização | Decisão | Loop único obrigatório |
|---|---|---|
| Lâmina Noturna | REWORK | sair da sombra -> primeiro golpe forte -> marcar -> desaparecer/reabrir, sem buff contínuo |
| Fantasma | REWORK | gastar Energia para fase/reposicionamento curto -> vulnerabilidade de saída -> ataque preparado |
| Executor | REWORK | criar vulnerabilidade -> baixar alvo ao limiar -> execução limitada contra bosses |
| Alquimista | REWORK | escolher fórmula -> aplicar dose -> converter em bomba/antídoto -> trocar fórmula conforme situação |
| Arauto da Peste | REWORK | empilhar doença -> espalhar versão reduzida -> manter múltiplos alvos sem burst excessivo |
| Toxicologista | REWORK | identificar alvo -> escolher toxina apropriada -> explorar fraqueza; troca tem custo/cooldown |
| Esgrimista | REWORK | controlar distância curta -> contra-tempo -> estocada de resposta |
| Dançarino de Lâminas | REWORK | alternar alvos/ângulos -> manter Dança -> finisher; focar um alvo reduz manutenção |
| Contra-Lâmina | REWORK | parry preciso -> armazenar Resposta -> contra-ataque com efeito situacional |
| Demolidor | REWORK | plantar/carrear carga -> armar -> detonar com teto de burst e friendly-safety apropriada |
| Infiltrador | REWORK | evitar combate -> ganhar Disfarce/Entrada -> cumprir objetivo/primeiro contato consome estado |
| Mestre dos Fios | REWORK | montar fios -> controlar corredor -> alvo cruza -> marca/slow -> reposicionar rede |
| Caçador de Bruxos | REWORK | identificar conjurador -> acertar durante cast -> roubar/selar recurso limitado -> janela de execução |
| Lâmina da Alma | REWORK | golpes condicionais geram Ecos -> armazenar até cap -> corte místico consome quantidade escolhida |
| Andarilho do Vazio | REWORK | teleporte curto próprio -> cria Dívida/Vazio -> próximo golpe consome; spam aumenta risco |

## MAGO — correções obrigatórias sobre a auditoria Iron's

A arquitetura `Iron's = spells` e `RPG Stats = buildcraft` permanece correta, mas quatro pontos devem ser tratados antes de considerar o design fechado:

1. `mag_acc_instant_cast` não pode continuar chamado **Conjuração Instantânea** se apenas reduzir cooldown. Preferência: fazer o node modificar cast time real de uma spell elegível; se a API exata não permitir de forma segura, renomear para uma mecânica de recuperação temporal coerente.
2. Escola **Nature não é sinônimo de SUMMON**. Modificadores de summon devem exigir spell/resultado que realmente invoque entidade compatível; Nature não-invocativa deve ter categoria própria ou filtros explícitos.
3. **Holy não deve cair automaticamente em Arcane** para fins de toda passiva. Evocation/Arcane e Holy precisam de filtros claros para evitar que o Mago herde sinergias tematicamente sagradas sem decisão de build.
4. Toda spell citada como alvo de `ADAPT/REPLACE` deve possuir detector/adapter executável correspondente. Documento não pode prometer interação que o código não reconhece.

Também renomear nodes que deixaram de conceder uma spell própria. Ex.: um node que antes se chamava `Estilhaço Glacial` e agora apenas modifica Ice spells deve ter nome/descrição de modifier, não fingir que desbloqueia um feitiço.

## Regras de implementação posterior

- Preservar IDs quando possível para não quebrar saves; mudar nome/descrição/efeito em vez de trocar ID sem migração.
- Cada caminho ganha um estado/motor próprio antes de receber números maiores.
- Técnicas e signatures devem ter verbos diferentes ou condições diferentes; signature não pode ser apenas a technique com valor/duração maior.
- Ascension muda regras temporariamente, respeita caps e nunca é liberada pela Casa secundária.
- Bosses recebem resistência a hard CC, não imunidade total.
- Sem restaurar dodge universal.
- Mago não recria catálogo paralelo de spells.
- Build/CI e teste server-authoritative obrigatórios após cada classe.