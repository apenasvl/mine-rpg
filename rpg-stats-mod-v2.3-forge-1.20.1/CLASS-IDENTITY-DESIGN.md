# Identidade de gameplay das classes

Este documento define o loop de combate desejado antes da implementação. A meta é que trocar de Caminho/Especialização mude decisões e ritmo de jogo, não apenas porcentagens.

## Princípios

- Minecraft continua sendo o núcleo de movimento, ataque, defesa e equipamento.
- RPG Stats adiciona camadas de decisão: recursos, estados, janelas, marcas, postura, execução, reação e posicionamento.
- Poucas técnicas ativas importantes; evitar hotbar de MOBA.
- Nodes numéricos existem, mas sustentam mecânicas.
- Nenhum caminho deve ser resumível a `mesma classe + X% dano/defesa`.
- Casa/Caminho primário define o loop. Complementos secundários ajudam, mas não entregam segunda especialização/capstone/Ascension.

# GUERREIRO

Pergunta de combate: **como eu crio e exploro uma quebra de postura sem perder meu próprio ritmo?**

Loop base: pressionar -> gerar Fúria/Impacto -> escolher guarda, ataque carregado ou counter -> quebrar postura -> executar -> reiniciar pressão.

## Vanguarda

Loop: bloquear/absorver impacto frontal -> acumular Guarda -> decidir entre manter segurança ou gastar Guarda -> provocar/interceptar -> contra-atacar.

- **Bastião:** transforma Guarda em barreira/interceptação de equipe; vence por estabilidade.
- **Senhor da Guerra:** controla ameaça e cria abertura para aliados; vence por coordenação.
- **Juggernaut:** transforma avanço sob pressão em Inércia; vence ocupando espaço e resistindo stagger.

## Fúria Carmesim

Loop: causar/receber pressão -> acumular Fúria -> manter para bônus arriscado ou gastar -> burst -> janela de vulnerabilidade/exaustão.

- **Saqueador de Sangue:** Sede surge ao ferir; sustain depende de continuar atacando o alvo correto.
- **Nascido da Fúria:** guarda Fúria para Frenesi curto e explosivo; gasto total cria downtime.
- **Colosso da Dor:** armazena parte do dano sofrido como Dor; ataques pesados convertem Dor em stagger/impacto.

## Mestre de Armas

Loop: usar uma categoria de arma -> criar Abertura -> trocar ferramenta/ritmo -> consumir Abertura com efeito específico -> repetir.

- **Mestre das Lâminas:** cadência de golpes precisos; erro quebra sequência.
- **Mestre do Duelo:** guarda/parry -> Riposta -> ataque técnico focado em postura.
- **Quebra-Titãs:** ataques lentos/carregados -> Impacto -> quebra de postura de elites/bosses.

## Vínculo Rúnico

Loop: acertar/defender -> gerar Carga Rúnica -> escolher runa/imbuimento -> consumir carga em próximo golpe -> reconstruir.

- **Cavaleiro Rúnico:** imbuimentos ofensivos em melee, sem virar spellcaster paralelo.
- **Quebra-Feitiços:** converte defesa contra magia/projétil em selo antimagia/counter.
- **Lâmina da Tempestade:** alterna avanço e golpe para manter Carga elétrica; parar perde pressão.

## Comandante

Loop: marcar/formar -> aliados ou próprio jogador cumprem condição -> gerar Moral -> emitir Ordem -> explorar janela tática.

- **Porta-Estandarte:** zona física de formação; posicionamento importa.
- **Estrategista:** alterna ordens e recompensa cumprimento de condição.
- **Guarda de Ferro:** escolta/intercepta dano limitado e converte proteção em contra-pressão.

# ARQUEIRO

Pergunta de combate: **estou na distância, linha e posição certas para transformar precisão em vantagem?**

Loop base: posicionar -> preparar -> marcar/medir distância -> disparar -> avaliar trajetória/resultado -> reposicionar.

## Atirador

Loop: manter distância e linha de visão -> acumular Mira -> escolher tiro normal ou preparado -> perder parte da Mira ao errar/mover demais.

- **Franco-Atirador:** estabilidade revela ponto fraco; tiro forte exige preparação e reposicionamento posterior.
- **Olho Mortal:** sequência de acertos precisos constrói Precisão e abre janela de crítico/execução.
- **Balístico:** alinha múltiplos inimigos e explora perfuração/overpenetration com dano decrescente.

## Guardião Selvagem

Loop: identificar Presa -> gerar Instinto por rastrear/controlar -> usar companheiro/armadilha -> adaptar defesa/ataque.

- **Mestre das Feras:** ordens de companheiro e ataques coordenados.
- **Armadilheiro:** desenha rotas, conduz inimigos e controla terreno; boss recebe slow/resistência.
- **Sobrevivencialista:** converte vantagem ambiental/defensiva em utilidade, aceitando menor dano bruto.

## Escaramuçador

Loop: disparar -> mover/reorientar -> ganhar Momentum -> disparar de novo -> manter fluxo sem permanecer parado.

- **Corredor do Vento:** velocidade lateral e tiro em movimento alimentam Momentum.
- **Acrobata:** salto, queda e mudança de ângulo preparam o próximo tiro; não depende de dodge universal.
- **Guerrilheiro:** ataca, rompe linha de visão e reaparece de outro ângulo para renovar Emboscada.

## Arqueiro Arcano

Loop: flechas aplicam marcas elementais -> preparar combinação -> consumir marca -> reação simples e controlada. Não lança o catálogo de spells do Mago.

- **Arco Ígneo:** Calor -> detonação/DoT moderado.
- **Arco Glacial:** Frio -> slow -> quebra controlada.
- **Arco da Tempestade:** Carga -> corrente para alvo secundário com teto.

## Artífice

Loop: preparar/recargar -> gerar Cargas de Dispositivo -> escolher munição/ferramenta -> gastar carga -> nova preparação.

- **Besteiro:** recarga deliberada prepara rajada ou perfuração.
- **Bombardeiro:** munição explosiva com limite por alvo/segundo.
- **Engenheiro:** dispositivo temporário controla área e cobra posicionamento/manutenção.

# ASSASSINO

Pergunta de combate: **eu já criei a abertura certa para comprometer meu burst ou devo reposicionar?**

Loop base: preparar -> criar Abertura/Marca -> entrar -> combo curto -> executar/consumir estado -> sair/reposicionar.

## Sombra

Loop: quebrar visão/furtividade/reposicionar -> ganhar Abertura -> primeiro golpe consome -> decidir entre permanecer ou desaparecer.

- **Lâmina Noturna:** primeiro golpe saindo da sombra é o foco; bônus não fica permanente.
- **Fantasma:** gasta Energia para fase/reposicionamento curto e assume risco na saída.
- **Executor:** cria vulnerabilidade e termina alvos abaixo do limiar; bosses usam versão reduzida.

## Veneno

Loop: escolher toxina -> aplicar Doses -> decidir entre manter DoT ou consumir doses -> trocar fórmula conforme alvo.

- **Alquimista:** alterna fórmulas ofensivas/defensivas e converte doses em bomba/antídoto.
- **Arauto da Peste:** mantém múltiplos alvos e espalha versão reduzida das Doses.
- **Toxicologista:** escolhe toxina especializada para tipo/estado do alvo; troca possui custo.

## Duelista

Loop: spacing/guarda/parry -> gerar Vantagem -> contra-tempo -> consumir em resposta -> voltar ao neutro.

- **Esgrimista:** distância curta e ritmo espaçado criam estocada de resposta.
- **Dançarino de Lâminas:** alternar alvos/ângulos mantém Dança e prepara finisher.
- **Contra-Lâmina:** parry preciso armazena Resposta para um contra-ataque situacional.

## Sabotador

Loop: preparar terreno -> armar dispositivo -> induzir inimigo a entrar -> disparar efeito -> reposicionar rede.

- **Demolidor:** explosivos preparados, fortes mas limitados por burst/cooldown.
- **Infiltrador:** tempo fora de combate gera Entrada/Disfarce consumido no primeiro contato.
- **Mestre dos Fios:** fios definem corredores, aplicam marca/slow e fornecem informação espacial.

## Lâmina Mística

Loop: cumprir condição melee -> roubar Eco de Alma -> armazenar até cap -> escolher quanto consumir -> golpe híbrido/utility.

- **Caçador de Bruxos:** foca conjuradores; acerto durante cast gera selo/roubo limitado.
- **Lâmina da Alma:** acumula Ecos e escolhe consumo parcial/total em corte místico.
- **Andarilho do Vazio:** teleporte curto cria Dívida do Vazio; próximo golpe paga/consome, spam aumenta risco.

# MAGO

Pergunta de combate: **qual spell, escola e sequência transforma melhor o estado da minha build?**

Regra estrutural: Iron's Spells é o arsenal; RPG Stats não compete com um segundo catálogo de projéteis/curas/dashes genéricos.

## Casa Elemental

Loop: spell elemental -> aplicar estado -> combinar/acumular -> reação -> escolher continuar pressão ou trocar escola.

- **Piromante:** Calor/Ember -> Combustão -> Overheat risco/recompensa.
- **Criomante:** Frio -> controle -> Estilhaçar.
- **Tempestário:** Carga Estática -> correntes/Sobrecarga -> mobilidade elétrica.

## Casa Arcana

Loop: preparar/sequenciar spells -> acumular padrão -> modificar próxima spell -> reset/repetição.

- **Runista:** runa preparada reage à próxima spell real.
- **Ilusionista:** engano/invisibilidade cria cargas/eco e janela de Ataque Fantasma.
- **Telemante:** spells espaciais alimentam Compressão/controle, sem duplicar Teleport/Black Hole.

## Casa da Conjuração

Loop: invocar entidade compatível -> gastar/gerenciar Vínculo -> emitir ordem/formação -> renovar/substituir summon.

- **Conjurador:** quantidade/peso/ordem de summons reais.
- **Animista:** posturas e suporte; Nature não deve ser tratada automaticamente como summon.
- **Forjador Astral:** formação ofensiva/defensiva/escolta acionada por arsenal espectral compatível.

## Casa Oculta

Loop: ganhar Corrupção/risco -> usar Blood/Eldritch -> converter vida/estado -> decidir purificar ou continuar pressionando.

- **Sanguimante:** vida, Hemorragia e lifesteal limitado.
- **Maledicente:** debuffs reais alimentam Fragilidade/Ruína/propagação.
- **Hexblade:** alterna spell e melee, parry/imbuimento e ritmo Spellblade.

## Casa Temporal

Loop: casts e mobilidade geram ritmo/Fragmentos -> manipular cooldown/tempo -> gastar recurso em janela tática -> reconstruir.

- **Acelerador:** Momentum, cast time/CDR e Overclock. `Conjuração Instantânea` precisa afetar cast time real ou ser renomeada.
- **Estagnador:** Slow/Stasis/controle, com resistência especial em bosses.
- **Reversor:** snapshot próprio -> Marca -> risco -> Rewind seguro sem tocar em `ICastData` do Iron's.

# Checklist de identidade antes da implementação

Uma proposta só é aceita se todas forem verdadeiras:

1. O loop pode ser explicado sem mencionar apenas `% de dano`.
2. A technique e a signature têm condições/verbos diferentes.
3. A Ascension muda temporariamente regras, não apenas multiplica números.
4. A especialização possui pelo menos uma decisão real de recurso/estado/posição.
5. O loop continua compreensível no Minecraft sem hotbar excessiva.
6. Bosses não são trivializados por hard CC ou execução.
7. Não depende da antiga esquiva universal.
8. Mago usa spells do Iron's, não catálogo duplicado.