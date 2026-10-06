# Auditoria de bosses — 6 de outubro de 2026

Base de produção: `6ca97f3`. Os 28 perfis fixos existem no registry nativo observado; as versões dos três mods coincidem com o ZIP `rpg_astra2.zip`.

## Evidência e limites

Baseline nativo: run 36794767189, Native registry/attributes observed; arena calibration pending except two native common-hit fixtures. HP solo abaixo é uma projeção do atributo nativo × fator do perfil; não é tempo de luta nem dano de habilidade.

| Registry ID | Nível recomendado | HP nativo | HP solo previsto | Armor nativo | Ataque base nativo | Fator de dano RPG | Resistência status/stagger |
|---|---:|---:|---:|---:|---:|---:|---|
| legendary_monsters:overgrown_colossus | 35 (31–40) | 170 | 425 | 10 | 14 | 7.5 | 0.65/0.75 |
| soulsweapons:returning_knight | 45 (41–50) | 500 | 1500 | 15 | 15 | 6.5 | 0.65/0.75 |
| soulsweapons:night_shade | 15 (11–20) | 150 | 600 | 2 | 10 | 2 | 0.3/0.35 |
| soulsweapons:draugr_boss | 25 (21–30) | 300 | 1200 | 10 | 10 | 3.5 | 0.45/0.5 |
| soulsweapons:chaos_monarch | 35 (31–40) | 450 | 2250 | 4 | 20 | 5.5 | 0.65/0.75 |
| soulsweapons:accursed_lord_boss | 45 (41–50) | 600 | 3000 | 10 | 20 | 6 | 0.75/0.85 |
| soulsweapons:moonknight | 45 (41–50) | 550 | 1650 | 20 | 15 | 8 | 0.75/0.85 |
| soulsweapons:day_stalker | 45 (41–50) | 600 | 1500 | 15 | 20 | 6 | 0.75/0.85 |
| soulsweapons:night_prowler | 45 (41–50) | 500 | 1250 | 10 | 20 | 6 | 0.75/0.85 |
| legendary_monsters:cloud_golem | 35 (31–40) | 350 | 1750 | 10 | 0 | 5.5 | 0.65/0.75 |
| legendary_monsters:posessed_paladin | 35 (31–40) | 400 | 2000 | 12 | 15 | 7.5 | 0.65/0.75 |
| legendary_monsters:the_obliterator | 45 (41–50) | 450 | 2700 | 14 | 6 | 10 | 0.75/0.85 |
| bosses_of_mass_destruction:lich | 25 (21–30) | 300 | 1200 | 0 | 9 | 4 | 0.45/0.5 |
| bosses_of_mass_destruction:gauntlet | 35 (31–40) | 250 | 1500 | 24 | 16 | 7 | 0.65/0.75 |
| bosses_of_mass_destruction:void_blossom | 35 (31–40) | 350 | 1750 | 20 | 12 | 7 | 0.65/0.75 |
| bosses_of_mass_destruction:obsidilith | 45 (41–50) | 300 | 2100 | 24 | 16 | 8 | 0.75/0.85 |
| legendary_monsters:warped_fungussus | 15 (11–20) | 100 | 200 | 10 | 7 | 2 | 0.3/0.35 |
| legendary_monsters:skeletosaurus | 25 (21–30) | 150 | 375 | 15 | 13 | 3 | 0.45/0.5 |
| legendary_monsters:lava_eater | 25 (21–30) | 170 | 425 | 15 | 11 | 3 | 0.45/0.5 |
| legendary_monsters:endersent | 35 (31–40) | 200 | 500 | 10 | 16 | 7 | 0.65/0.75 |
| legendary_monsters:frostbitten_golem | 35 (31–40) | 220 | 550 | 10 | 16 | 7 | 0.65/0.75 |
| legendary_monsters:shulker_mimic | 25 (21–30) | 200 | 500 | 15 | 12 | 3.5 | 0.45/0.5 |
| legendary_monsters:withered_abomination | 35 (31–40) | 190 | 475 | 12 | 15 | 7.5 | 0.65/0.75 |
| legendary_monsters:ancient_guardian | 35 (31–40) | 170 | 425 | 13 | 20 | 6 | 0.65/0.75 |
| legendary_monsters:dune_sentinel | 25 (21–30) | 170 | 425 | 16 | 16 | 3 | 0.45/0.5 |
| legendary_monsters:annihilation_pursuer | 35 (31–40) | 210 | 525 | 13 | 10 | 9 | 0.65/0.75 |
| legendary_monsters:beheaded_knight | 35 (31–40) | 195 | 487.5 | 14 | 10 | 9 | 0.65/0.75 |
| legendary_monsters:resurrected_knight | 25 (21–30) | 190 | 475 | 12 | 10 | 4 | 0.45/0.5 |

## Rotas confirmadas na inspeção do binário

- Returning Knight, `ReturningKnightGoal.tick`: estágio 21 adiciona movimento vertical (0,1,0) antes de `LivingEntity.hurt`, dano nativo 25 modificado pelo goal. Logo, a velocidade vertical pode ser observada no dano aceito.
- Legendary `ModDamageTypes.causeCutDamage/causeImpaleDamage`: fontes sem atacante. Chamadas encontradas somente em SwingingAxeBlockEntity e SpikeTrapBlock; não atribuir a boss pela proximidade.
- Rotas customizadas de laser, gravidade, nuvem e ghost possuem construtores de fonte com entidades; verificar owner efetivo em runtime, sem adivinhar a partir do tipo de dano.

## Cobertura ainda pendente

Para cada um dos 28 IDs acima: velocidade/knockback/toughness em runtime, fases completas, arena, projéteis/AoE/summons e fonte indireta por ataque; solo e grupo; ofensiva/defensiva/equilibrada nas quatro classes; níveis referência−5/referência/referência+5/muito acima, respeitando limite50.
Boss: alvo90–240s; miniboss:30–90s. Não ajustar HP a partir de DPS contra alvo estacionário. Nenhum tempo de arena foi comprovado por esta auditoria.
Returning Knight e Colossus já possuem fixtures nativas de ataque; todos os perfis têm janela de saída estacionária de Guerreiro. As demais classes/execução das fases não estão cobertas por essa janela.

## Estado dos sistemas

**Implementado:** party diminishing returns e cap8, resistências por perfil, XP fixo/first-kill, Contribution Score de dano/tanque/suporte; arquitetura Iron/RPG, afinidade off-class0.625, proteção de sustain e estado de combate. UI aprovada preservada.
**Incompleto:** calibração nativa por rota/arena, matriz legal de builds e níveis; rewards atualmente divididos igualmente entre elegíveis.
**Faltando:** bonusXP relativo de boss, penalidade overlevel, farm temporal por player/tipo, Threat fallback; teste de restart/save antigo/cliente servidor com ZIP inteiro.
**Regressão a reproduzir:** queda após golpe ascendente não passa pela defesa exclusiva contra bosses. Testes anteriores desligavam gravidade e não comprovavam essa parte.
**Obsoleto:** documentos Fabric e catálogos antigos em docs/legacy, identificador FE reservado para migração; não importar nem reativar.

## Estabilidade/performance — achados para PR C/D

- `ArcherAimAssist` mantém GUIDES/COOLDOWNS; `ForgeEvents.forget` não chama limpeza por jogador. O tick expira guias, mas uma proteção explícita de logout/dimensão é uma lacuna de ciclo de vida a testar.
- `EncounterManager.recordSupport` itera apenas encounters ativos, sem scan de entidades; BossScaler valida somente rastreados a cada100ticks. Preservar essas características.
- Forge do ZIP47.4.10; harness fixado47.4.0. Validação do ZIP completo não equivale à compatibilidade isolada dos três mods.
