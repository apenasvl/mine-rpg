# Balance v1.9.0-alpha.1 — Soulslike Foundation

Este arquivo registra os números iniciais usados nos testes. A v1.9 não adiciona frascos, sistema de equipamentos, postura ou quebra de postura.

## Origens de classe

Todas começam com 33 pontos. A origem define o começo da build; não aumenta o orçamento total.

| Classe | VIT | TEN | FOR | DES | INT | FÉ | ARC | Total |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Guerreiro | 8 | 8 | 10 | 3 | 1 | 2 | 1 | 33 |
| Mago | 5 | 5 | 2 | 4 | 10 | 3 | 4 | 33 |
| Arqueiro | 6 | 7 | 3 | 10 | 2 | 2 | 3 | 33 |
| Assassino | 5 | 7 | 3 | 9 | 2 | 1 | 6 | 33 |
| Paladino | 8 | 6 | 7 | 3 | 2 | 6 | 1 | 33 |

O personagem recebe 2 PA e 1 PH por nível, até o nível 50. Cada atributo base tem limite 50.

## Retornos decrescentes

Força, Vitalidade e Destreza possuem soft caps em 20 e 35.

| Faixa | Força: dano físico por ponto | Vitalidade: HP por ponto | Destreza: velocidade de ataque por ponto |
|---|---:|---:|---:|
| 1–20 | +0,10 | +0,40 HP | +0,010 |
| 21–35 | +0,07 | +0,25 HP | +0,007 |
| 36–50 | +0,04 | +0,10 HP | +0,004 |
| Total em 50 | +3,65 de dano | +13,25 HP | +0,365 de velocidade |

Outros escalamentos têm limites próprios:

- Destreza: até +12,5% de dano à distância.
- Inteligência: até +12,5% de dano mágico.
- Fé: até +10% de dano mágico para builds com Paladino e +20% de cura.
- Arcano: até +7,5% de dano mágico para Assassino/Oculto, +60% de duração de aflições e +1 de Sorte.
- Bônus combinado de atributos no dano mágico: máximo +22%.

## Stamina e esquiva

| Ação | Custo / regra |
|---|---:|
| Ataque corpo a corpo que acerta | 9 |
| Ataque à distância que acerta | 6 |
| Corrida | 6 por segundo |
| Esquiva | 30 |
| Ataque físico exausto | 65% do dano normal |
| Recarga da esquiva | 0,8 s |
| Invulnerabilidade da esquiva | 0,4 s |
| Atraso para regenerar | 1,2 s |

A Stamina começa em 100. Tenacidade aumenta o máximo até 152,5 e a regeneração de 12/s até 18/s. A esquiva é validada no servidor e exige contato com o chão.

## Ecos e morte

- Apenas os Ecos ainda não convertidos em nível são perdidos.
- A recuperação ocorre automaticamente a até 2,5 blocos da posição gravada.
- Posição e dimensão são persistidas.
- Uma segunda morte apaga a recuperação anterior e grava a nova perda.
- Subir de nível não cura o personagem.

## Fases de chefes

- Tier 2 ou superior: segunda fase a 55% de vida, com multiplicador ofensivo de fase 1,10x.
- Tier 4 ou superior: fase final a 25% de vida, com multiplicador ofensivo de fase 1,18x.
- A fase nunca volta para trás se o chefe se curar.
- A transição não acrescenta vida e não acumula modificadores de HP.

## Primeira matriz de teste

Comparar, em solo e com dois jogadores:

1. personagem nível 1 contra zumbi, esqueleto e aranha;
2. nível 10 contra mobs com armadura;
3. nível 25 com caminho e multiclasse;
4. nível 50 contra equipamento de Netherita vanilla;
5. chefes tier 2, 4 e 5 nas transições de fase;
6. builds puras e híbridas medindo dano por segundo, sobrevivência, cura e tempo sem Stamina.

Registrar tempo para matar, dano recebido, porcentagem de Stamina vazia e número de habilidades usadas por minuto. Esses dados devem orientar a v1.9 beta.
