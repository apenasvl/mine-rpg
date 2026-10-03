# Arqueiro: armas externas e decisões táticas

A PR mantém os IDs dos nodes e saves. As 15 especializações têm 45 ativas com nomes próprios; os efeitos e condições completos vêm de `ClassAbilityRegistry` e aparecem no Codex.

## Armas

O RPG reconhece `BowItem`, `CrossbowItem` e as tags `rpgstats:weapons/bow` e `rpgstats:weapons/crossbow`. A arma é registrada no lançamento real por `stopUsingItem`/`shootAll`, inclusive pela mão secundária. Itens arremessados não contam como tiros de arco. Trocar de arma antes do impacto não muda a origem do tiro.

O mod externo continua criando o projétil e consumindo munição/durabilidade. O RPG não substitui flechas, não acelera o ciclo nativo e não injeta encantamentos. Projéteis lançados pelo mesmo dono no mesmo tick do servidor compartilham uma ativação: apenas o primeiro dano positivo confirmado gera recursos, avança motores e consome preparações. Essa regra também limita arcos capazes de lançar vários disparos independentes no mesmo tick.

Dano cancelado ou zerado não dispara efeitos. Explosões e fogo com origem no projétil não passam outra vez pelo escalonamento do RPG. Efeitos suplementares do RPG usam a guarda central de procs; o dano suplementar na mesma vítima é adiado 11 ticks e limitado a 4 por disparo. Flechas que acertam mais de 60s depois do lançamento não geram recompensas.

A tag `rpgstats:weapons/native_area` identifica armas com efeitos próprios em área; `rpgstats:native_area_projectiles` permite classificar munições. O Bombardeiro consome sua preparação, mas omite a explosão adicional nessas armas/munições. Datapacks podem estender essas tags.

## Armadilhas físicas

Simply Bear Traps é instalado separadamente. Preparar Armadilhas e Rede Territorial exigem armadilhas abertas do próprio jogador em até 8 blocos. No máximo duas são preparadas por ativação. Capturas de armadilhas de outro dono não concedem marcas.

O adaptador lê o dono e a vítima realmente capturada do `BearTrapEntity` da versão 1.0.0, sem alterar a captura, dano, materiais, rearmamento ou assets nativos. Não simula captura com um efeito de Lentidão. Se o contrato da versão instalada mudar, a integração falha fechada e não cobra uma preparação inválida.

Cada vítima concede uma marca no máximo uma vez durante a preparação, com intervalo de 10s entre recompensas para a mesma vítima. A marca dura 6s e é consumida por um tiro confirmado. Cobrar Captura pode ser preparada com uma presa marcada em até 24 blocos mesmo atrás de cobertura; a preparação não causa dano nem consome a marca por si só. A Rede Territorial exige mover-se 2 blocos da posição de ativação para converter a marca em dano suplementar. As regras nativas `beartrapmod:trap_immune`, `beartrapmod:bosses` e `trapBosses` continuam controlando quais entidades são capturáveis.

Lentidão e Fraqueza das novas habilidades duram no máximo 22 ticks em entidades reconhecidas pelo BossScaler.

Estados temporários são descartados ao morrer, sair ou trocar dimensão; não deixam dispositivos persistentes nem carregam chunks.

## Dependências para testar em Forge 1.20.1

| Mod | Versão | Projeto / arquivo CurseForge |
|---|---|---|
| More Bows and Arrows | 5.0.1 | 888468 / 6843671 |
| Too Many Bows | 4.1.2 | 1141533 / 8735189 |
| Simply Bear Traps | 1.0.0 | 1643250 / 8595248 |
| MonoLib | 4.1.0 | 968432 / 8543117 |
| Architectury | 9.2.14 | 419699 / 5137938 |
| Curios | 5.14.1+1.20.1 | 309927 / 6418456 |
| GeckoLib | 4.8.4 | 388172 / 8285794 |

Esses mods não são empacotados no JAR do RPG nem tornam-se dependências obrigatórias do RPG. O perfil Gradle `-ParcherCompatTests` carrega os binários fixados e exige sua presença nos testes de integração.

## Validação

A CI executa os validadores, os GameTests sem mods externos, os GameTests com os três mods e suas dependências e depois o build final. Além das regressões anteriores de todas as classes, os testes cobrem arma incorreta, multishot, controle cancelado, avanço real, recuperação com Instinto, alvo prioritário, captura de dono correto e munição/projéteis dos mods.

O teste automático não substitui a avaliação manual de sensação de movimento, balanceamento PvP, todas as combinações de munição ou cada arco do catálogo. A PR permanece Draft para esse teste antes do merge.
