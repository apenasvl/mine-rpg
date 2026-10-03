# Guerreiro — passe completo

Estado: implementação server-authoritative concluída; validação estática automatizada. Runtime
Minecraft continua pendente até teste real do jogador.

## Contrato de classe

- Maior pancada física direta de referência, sem dano permanente gratuito.
- Frontline: Impacto, Guarda, Fúria, Riposta, Runas e Moral exigem ações de combate.
- Early game continua incompleto: Caminho no 10, especialização no 25, Engine 30, Technique 35,
  Conversion 40, Risk 45, Signature 48 e Ascension 50.
- Dano secundário usa cap, ICD e guarda de recursão. Correntes/explosões nunca repetem o alvo
  principal como novo hit de scaling completo.
- Caminho secundário continua em escala reduzida e nunca concede especialização/Ascension.

## Caminhos

| Caminho | Loop implementado | Decisão real |
|---|---|---|
| Vanguarda | receber/atacar -> Guarda -> postura/parry -> contra-pressão | manter Guarda para segurança ou gastar na janela |
| Fúria Carmesim | causar/receber pressão -> Fúria -> Frenesi -> Exaustão | segurar recurso ou aceitar downtime por burst |
| Mestre de Armas | trocar/encadear ritmo -> Abertura -> Riposta/Impacto | ferramenta e timing importam mais que bônus passivo |
| Vínculo Rúnico | melee/defesa -> Cargas/Selo -> imbuimento/counter | runa ofensiva, defensiva ou móvel sem spellbook paralelo |
| Comandante | formar/marcar -> Moral -> Ordem -> cumprir condição | ofensiva, defesa e avanço possuem condições distintas |

## Especializações

| Especialização | Motor e payoff |
|---|---|
| Bastião | Guarda alimenta barreira; Conversion estende absorção curta à formação |
| Senhor da Guerra | marca ameaça; acertos de aliados geram Moral; Ascension empodera Ordem |
| Juggernaut | movimento gera Inércia; técnica consome Inércia em resistência/controle frontal |
| Saqueador de Sangue | ferir gera Sede; cura tem cap/ICD; alvo em execução renova a janela |
| Nascido da Fúria | consome Fúria em Frenesi; kills prolongam Ascension; fim gera Exaustão real |
| Colosso da Dor | dano recebido armazena Dor; golpe comprometido a consome em impacto limitado |
| Mestre das Lâminas | sequência no mesmo alvo cria Cadência; finisher reduz uma recarga e reinicia |
| Mestre do Duelo | parry arma Riposta; próximo golpe aplica quebra de ritmo/postura |
| Quebra-Titãs | golpes grandes constroem Impacto; elite/boss recebe abertura curta e resistente |
| Cavaleiro Rúnico | Cargas viram corte híbrido; Conversion espalha dano menor só em alvos extras |
| Quebra-Feitiços | dano indireto/mágico armazena Selo; próximo golpe aplica interrupção/debuff |
| Lâmina da Tempestade | movimento + golpe gera Carga; descarga encadeia em 1 alvo (2 na Ascension) |
| Porta-Estandarte | estandarte fixa zona física; lutar dentro dela mantém Moral e formação |
| Estrategista | Ordem ofensiva, defensiva ou avanço exige 3 ações; conclusão empodera a próxima |
| Guarda de Ferro | intercepta 25% com cap e paga parte do dano; Ascension sobe limite sem imunidade |

## Limites

- Multiplicador condicional global da camada de classe continua limitado a 1.32.
- Colosso da Dor: proc principal no máximo 4.5, uma vez por 24 ticks.
- Cavaleiro Rúnico: alvo principal no máximo 4.0; splash no máximo 2.25 em 2 alvos (3 na Ascension).
- Tempestade: 2.75 somente em alvo adicional; nunca repete o alvo original.
- Guarda de Ferro: interceptação normal no máximo 3.5; Ascension no máximo 5.0.
- Saqueador: cura por proc no máximo 1.25 e ICD de 16 ticks.
- Bosses recebem controle mais curto; HP/fases vanilla/mod não são substituídos.

## Benchmark

O alvo de design continua sendo aproximadamente 28 de dano para espada de ferro, Guerreiro nível
50 e janela física ativa. Este valor é referência de runtime, não resultado confirmado por teste
local. O validador garante caps, gates e ausência de multiplicador persistente novo; a confirmação
exata deve ser feita com `/rpg debug on` dentro do Minecraft.

