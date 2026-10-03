# Changelog v1.9.0-alpha.1 — Soulslike Foundation

## Atributos e origens

- O sistema agora usa Vitalidade, Tenacidade, Força, Destreza, Inteligência, Fé e Arcano.
- Agilidade de saves antigos é migrada para Tenacidade.
- As cinco classes começam com 33 pontos totais, distribuídos conforme a identidade da origem.
- Força, Vitalidade e Destreza usam retornos decrescentes depois de 20 e 35.
- Inteligência escala Mana/dano mágico; Fé escala Paladino/cura; Arcano escala aflições/ocultismo/Sorte.

## Stamina e esquiva

- Nova barra universal de Stamina na HUD e no menu K.
- Ataques físicos, disparos, corrida e esquiva consomem Stamina.
- Ataques exaustos causam 65% do dano normal.
- Alt esquerdo + WASD executa esquiva direcional validada pelo servidor.
- A esquiva custa 30, tem recarga de 0,8s e 0,4s de invulnerabilidade.

## Ecos e morte

- XP passa a ser apresentado como Ecos.
- Morrer armazena os Ecos atuais na posição e dimensão da morte.
- Aproximar-se a 2,5 blocos recupera os Ecos automaticamente.
- Uma segunda morte substitui e apaga a perda anterior.
- O HUD mostra quantidade e distância dos Ecos perdidos.
- Level up não cura mais automaticamente.

## Chefes

- Tier 2+ ganha segunda fase com 55% de vida e +10% de pressão ofensiva.
- Tier 4+ ganha fase final com 25% de vida e +18% de pressão ofensiva.
- A transição é avisada aos jogadores próximos.
- Nenhuma vida adicional foi acrescentada além do scaling que já existia.

## Fora desta versão

- Frascos.
- Peso, requisitos ou scaling de equipamentos.
- Postura e quebra de postura.
