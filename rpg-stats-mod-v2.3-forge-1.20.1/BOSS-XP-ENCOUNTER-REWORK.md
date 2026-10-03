# Boss XP e Encounter Scaling — auditoria e plano

## Decisões fechadas

- Mobs comuns/fáceis não escalam com o nível RPG do jogador.
- Scaling dinâmico é reservado para bosses, minibosses relevantes e encontros especiais.
- RPG Stats não cria segunda vida, revive, barra extra ou fase artificial. Vida/fases originais pertencem ao Minecraft ou ao mod do boss.
- Dificuldade adicional de boss será baseada no nível RPG médio dos participantes e na quantidade de participantes reais.
- HP não define sozinho o tier de um boss. Overrides/tags conhecidos têm prioridade; Threat Score será apenas fallback.
- XP de boss é próprio do RPG Stats e separado do XP vanilla.
- Não haverá XP por quests. A API `QuestXpService` e atalhos equivalentes devem ser removidos.
- First Kill, recommended level, underlevel bonus, overlevel reduction, profiles de scaling, flags por atributo, contribution score, debug e proteção contra farm estão aprovados para fases posteriores.

## Correção aprovada para Contribution Score

Contribution Score deve ser normalizado pela capacidade esperada do jogador no nível RPG dele. Um jogador nível 10 enfrentando um boss de progressão 40 não pode ser descartado por causar pouco dano absoluto. O score deve medir contribuição relativa/atividade válida, não comparar dano bruto com jogadores muito acima de nível.

Exemplo de princípio:

- dano válido ponderado contra dano esperado para o nível/build;
- tanking/dano recebido relevante;
- cura/proteção de participantes;
- tempo realmente ativo no encounter;
- thresholds baixos o suficiente para jogadores underlevel legítimos participarem;
- proteção contra um único hit seguido de AFK.

## Auditoria da main antes da mudança

### XP

`PlayerStats.xpToNext(level)` usa `100 * level^1.55`.

`StatsManager.computeKillXp`:

- boss: delega para `BossScaler.getXpReward`;
- hostil comum: `maxHp * 0.18`, limitado a 2..12 XP;
- passivo: `maxHp * 0.05`, limitado a 1..3 XP;
- demais: limitado a 1..6 XP.

`StatsManager.addXpFromKill` já possui:

- bônus de primeira kill de boss de 1.5x;
- redução por farm somente para mobs não-boss (após 15 e 40 kills).

### Problema real do Warden

A tag `rpgstats:bosses/tier_3` inclui o Warden. Esse perfil entrega apenas 180 XP e adiciona +55% de vida. Uma primeira kill recebe 1.5x = 270 XP. Isso explica o teste em que o Warden com ~755–775 HP levou o jogador apenas do nível 1 ao 2.

### Scaling atual

`BossScaler` atualmente:

- considera todo `HostileEntity` candidato;
- classifica hostis por HP, podendo escalar mobs fortes que não são bosses;
- aplica bônus fixo de HP por tier mesmo sem considerar nível RPG;
- escala pela quantidade de players apenas por proximidade;
- recalcula grupo a cada 5 segundos;
- cria fases artificiais por porcentagem de HP (`SEGUNDA FASE` / `FASE FINAL`);
- aumenta dano adicionalmente nessas fases artificiais.

Isso conflita com as novas decisões.

## Implementação em fases

### Fase 1 — saneamento e vanilla

1. Remover qualquer caminho de XP por quests.
2. Remover fases/segunda vida artificiais do RPG Stats.
3. Parar de escalar mobs comuns só por HP.
4. Preservar vida base dos bosses até o novo scaling por nível estar pronto.
5. Dar override explícito ao Warden e revisar XP de Warden/Wither/Ender Dragon.
6. Manter sistema data-driven e compatibilidade atual de Iron's intactos.
7. Validar build/CI antes de avançar.

### Fase 2 — EncounterState multiplayer

1. Participantes event-driven.
2. Média de nível RPG dos participantes.
3. Party scaling com diminishing returns.
4. Grace period de saída/morte.
5. Preservação de percentual de HP ao rescalar.
6. Recalcular atributos apenas quando estado mudar.

### Fase 3 — XP/tiers avançados

1. `BossTier`/perfis claros.
2. recommended min/max level.
3. First Kill revisado.
4. underlevel bonus e overlevel reduction.
5. diminishing returns de farm de boss.
6. Threat Score fallback sem usar HP já escalado.
7. Contribution Score normalizado por nível do participante.

### Fase 4 — compatibilidade de bosses modded

Adicionar definições verificadas, sem inventar IDs, para:

- Terramity
- Legendary Monsters
- Aquamirae
- Soulslike Weaponry

Overrides explícitos prevalecem sobre detecção automática.

### Fase 5 — arma/equipamento e debug

- categorias/tags para Simply Swords, Soulslike Weaponry e Too Many Bows;
- profiles/flags de scaling por boss;
- modo debug de encounter desligado por padrão;
- testes de performance e dedicated server.

## Regra de entrega

Não juntar todas as fases num único commit/PR. Cada fase deve compilar, passar validações e manter save/servidor estáveis antes da próxima.