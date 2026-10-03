# Mage + Iron's balance fix

Objetivo desta revisão: corrigir o feedback de runtime sem transformar o Mago em uma classe de burst sem limite.

## Mana
- Iron's continua sendo a autoridade do pool de Mana quando presente.
- O HUD do RPG Stats não desenha uma segunda barra de Mana nesse caso.
- Equipamentos/Curios/Spellbooks do Iron's preservam integralmente a Mana que concedem.
- O limite de Mana do RPG Stats vale apenas para a parcela adicionada pela progressão do próprio RPG.

## Dano
A progressão persistente do Mago agora vem de três eixos:
- Inteligência: até +22%;
- nível RPG 1–50: até +22%;
- domínio do core: +6%.

A Casa Oculta pode adicionar até +5% extra via Arcano, com teto persistente total de +55% nessa camada.

Isso é aplicado antes dos bônus de Casa, nodes específicos do Iron's e efeitos situacionais. O hard cap global continua em x2.25, então crítico, marcas, fragilidade e outras janelas não escalam sem controle.

## Benchmark
Para uma spell representativa com 14 de dano base:
- build geral lvl 50 / INT 50 / core completo + Core Knowledge + Elemental max: ~24 de dano;
- mesma build em uma spell com node específico do Iron's (+18%): ~28 de dano;
- efeitos situacionais podem superar isso, mas continuam limitados pelo hard cap x2.25.

O benchmark de ~28 foi escolhido para ficar próximo do Guerreiro maximizado reportado no playtest, sem exigir que toda spell comum do Mago cause o mesmo dano de um hit melee máximo. Spells de base maior podem passar desse número por cast, compensadas por Mana, cooldown, cast time e risco/condição da build.
