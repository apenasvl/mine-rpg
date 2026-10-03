# RPG Stats & Classes v1.9.0-alpha.1 — Soulslike Foundation (Fabric 1.20.1)

A v1.9 mantém a UI compacta e as árvores da v1.8, mas torna a progressão e o combate mais soulslike: sete atributos, origens de classe equilibradas, stamina, esquiva direcional, Ecos recuperáveis e fases universais de chefes.

Esta etapa **não adiciona** frascos, peso/equipamentos, postura ou quebra de postura.

O projeto passa a ter **810 nodes** no total e **295 habilidades ativas**, com caps globais para impedir que a quantidade de escolhas vire power creep.

## Controles

| Tecla / comando | Ação |
|---|---|
| **K** | Abrir menu RPG |
| **R** | Usar habilidade equipada no slot 1 |
| **Z** | Usar habilidade equipada no slot 2 |
| **X** | Usar habilidade equipada no slot 3 |
| **C** | Usar habilidade equipada no slot 4 |
| **Alt esquerdo + direção** | Esquiva direcional |
| `/rpg` ou `/rpg open` | Abrir menu |
| `/rpg addxp <jogador> <qtd>` | Adicionar Ecos de progressão (OP) |
| `/rpg addpoints <jogador> <qtd>` | Adicionar PA (OP) |
| `/rpg reset <jogador>` | Resetar progresso RPG (OP) |

As habilidades ativas podem ser equipadas diretamente na árvore. Cada node mantém **cooldown próprio**.

## HUD compacta da v1.9

- Mana/Fúria/Foco/Energia/Fé ficam em uma barra baixa **à esquerda da hotbar**.
- R/Z/X/C ficam **no canto inferior direito**, deixando mira e centro da tela livres.
- Cooldowns aparecem nos próprios slots com contagem e overlay.
- Mago mostra Concentração, Corrupção e Fragmentos Temporais em micro-indicadores.
- Recurso de multiclass aparece em microbarra separada.
- Stamina e Ecos perdidos aparecem em linhas pequenas acima do recurso principal.

## Progressão soulslike

- nível máximo: **50**;
- nível 1: escolha da origem de classe, sempre com **33 pontos iniciais**;
- **2 PA** por level up;
- **1 PH por nível**, incluindo 1 PH inicial após escolher a classe no nível 1;
- XP necessário: `100 * nivel^1.55`;
- Inteligência não multiplica XP;
- atributos base têm limite 50 e retornos menores após 20/35;
- o XP de progressão é apresentado como **Ecos**;
- morrer deixa os Ecos não convertidos em nível no local da morte;
- chegar a até 2,5 blocos recupera os Ecos; morrer novamente apaga a perda anterior;
- subir de nível não cura mais o jogador automaticamente.

### Sete atributos

| Atributo | Função principal |
|---|---|
| Vitalidade | HP com retornos decrescentes |
| Tenacidade | Stamina máxima e regeneração |
| Força | Dano físico, com caps suaves |
| Destreza | Velocidade de ataque e dano à distância |
| Inteligência | Mana e dano mágico |
| Fé | Recurso do Paladino e potência de cura |
| Arcano | Aflições, ocultismo e Sorte |

### Stamina

- base: **100**; máximo com 50 Tenacidade: **152,5**;
- esquiva: **30** de Stamina, 0,8s de recarga e 0,4s de invulnerabilidade;
- ataque corpo a corpo: **9**; ataque à distância: **6**;
- atacar exausto ainda funciona, mas causa apenas **65%** do dano;
- corrida consome **6/s**;
- regeneração: **12–18/s**, iniciando 1,2s após o último gasto.

### Marcos do Mago

| Nível | Marco |
|---:|---|
| 1 | Núcleo do Mago |
| 10 | Escolha de uma das 5 Casas, após Maestria Arcana |
| 25 | Escolha de uma das 3 especializações da Casa |
| 25 | Multiclasse geral também fica disponível |
| 30 | Até 6 nodes iniciais de outras Casas do Mago, máximo 3 por Casa |
| 50 | Ascensão da especialização escolhida |

Escolher uma Casa não impede totalmente aprendizado lateral, mas a especialização e a Ascensão permanecem exclusivas. Isso permite híbridos internos sem deixar o jogador comprar toda a classe.

## Mago v1.5

### Recursos

**Mana**
- base: **100**;
- `+2 Mana` por ponto total de Inteligência;
- teto do pool do Mago: **280** antes de futuras fontes externas explicitamente integradas;
- regeneração base: **3 Mana/s**;
- passivas podem melhorar regen e reserva dentro dos caps do sistema.

**Concentração Arcana**
- intervalo: `0–100`;
- é gerada por acertos mágicos, críticos e execução correta de mecânicas;
- dano recebido remove Concentração com gate/cap para não punir dezenas de hits pequenos de forma absurda;
- Ascensões importantes consomem Concentração em vez de serem spamáveis por Mana.

Casas específicas ainda adicionam recursos próprios:
- **Oculta:** Corrupção `0–100`;
- **Temporal:** Fragmentos Temporais.

## 5 Casas / 15 especializações

### Casa Elemental
DPS, área, marcas e reações entre Fogo/Gelo/Raio.

- **Piromante** — Calor, Combustão, DoT e pressão de área.
- **Criomante** — Frio, barreira, Frozen e controle com proteção especial para bosses.
- **Tempestário** — Carga, chain lightning e mobilidade.

### Casa Arcana
Combos técnicos, selos e manipulação direta de magia.

- **Runista** — runas, redes e zonas preparadas.
- **Ilusionista** — clones, evasão, engano e ecos controlados.
- **Telemante** — Blink, portais, repulsão e singularidades.

### Casa da Conjuração
Vínculo, invocações virtuais, formações e suporte.

- **Conjurador** — familiar, guardião e grande conjuração.
- **Animista** — espíritos de Vida/Terra/Caça e suporte real.
- **Forjador Astral** — lâmina, escudo, torre e formações de constructos.

### Casa Oculta
Risco/recompensa, corrupção, vida e maldições.

- **Sanguimante** — vida como recurso, hemorragia e sustain ofensivo.
- **Maledicente** — Fraqueza, Fragilidade, Ruína e Marca do Destino.
- **Hexblade** — magia corpo a corpo, parry, blink strike e imbuimentos.

### Casa Temporal
Controle de ritmo, cooldowns, stasis e reversão.

- **Acelerador** — mobilidade, Momentum e recuperação de cooldown.
- **Estagnador** — slow, stasis, projéteis desacelerados e Time Lock.
- **Reversor** — Marca Temporal, Rewind, Eco e Segunda Chance.

## Tamanho das árvores

| Parte | Nodes |
|---|---:|
| Núcleo do Mago | 12 |
| 5 Casas (9 cada) | 45 |
| 15 especializações (7 cada, incluindo Ascensão) | 105 |
| **Total do Mago** | **162** |
| Árvores das outras classes | 648 |
| **Total registrado no projeto** | **810** |

A quantidade alta é intencional: com apenas 50 níveis e custos de 1–3 PH, um personagem precisa construir uma rota. Ele não consegue comprar os 162 nodes.

## Filosofia de balanceamento para Minecraft

Os números foram mantidos próximos da escala de combate de Minecraft em vez de usar multiplicadores gigantes.

Faixas-alvo iniciais:
- magia básica: **4–6 HP**;
- magia média: **6–9 HP**;
- magia pesada: **9–14 HP**;
- ultimates/Ascensões: normalmente **12–20 HP** de dano total quando ofensivas, dependendo de área, risco e setup;
- crítico mágico: **1,5x**;
- bosses têm resistência ou versões reduzidas de hard CC, roots, pulls e certos bônus de dano.

Caps importantes:
- poder mágico permanente da árvore: **+60%**;
- crítico mágico permanente: **20%**;
- recuperação permanente de cooldown: **25%**;
- recuperação temporária de cooldown: **50%**;
- spell lifesteal: **10%**;
- redução de custo de Mana: **45%**.

Esses caps são guardrails, não promessa de balanceamento final. O ajuste definitivo deve ser feito com testes de TTK no jogo contra vanilla, Netherite, elites e bosses do modpack.

## Regras importantes implementadas

- efeitos de controle forte são reduzidos em bosses;
- efeitos periódicos possuem gates/caps para evitar proc storms;
- Concentração por AOE/chain é limitada por cast;
- Shared Flow de summons tem cap de Mana por segundo;
- refund de summon tem gate interno para expirações simultâneas;
- Transferência de summon estende a **duração restante em 20%**, não usa HP fictício;
- Rewind recupera no máximo 4 HP e Dívida Temporal devolve parte dessa cura gradualmente sem matar o jogador;
- Reescrever Destino não restaura itens, consumíveis ou cooldowns;
- Slow de projéteis ignora projéteis próprios/aliados;
- magia de sangue nunca pode reduzir o usuário abaixo do piso de segurança definido;
- Echo/clone não pode recursivamente gerar outro echo;
- Ascensões possuem cooldown mínimo e custo especial.

## Multiclasse

A multiclasse geral continua disponível no nível 25:
- a classe secundária deve ser diferente da principal;
- acesso apenas aos **dois primeiros tiers reais** da árvore base secundária;
- nodes secundários custam `custo original + 1 PH`;
- a classe secundária usa pool de recurso próprio;
- não concede uma segunda especialização completa.

O aprendizado lateral entre Casas do Mago é outro sistema e não substitui multiclass.

## Interface v1.9

A tela **K** apresenta a progressão completa de todas as classes:
- painel menor, cabeçalho compacto e barra lateral reduzida;
- abas curtas: Stats, Classe, Caminho, Ramo e Multi;
- sete atributos em linhas legíveis, com efeito exato e valor base/total;
- seleção dos 5 Caminhos em cartões de duas colunas;
- seleção dos 3 Ramos em cartões largos e separados;
- árvore vertical por níveis, sem cartões sobrepostos;
- visualização de outros Caminhos para aprendizado cruzado após o nível 30;
- quatro slots ativos `R / Z / X / C`;
- tooltips quebram textos longos e mostram descrição, efeitos, custo, nível, pré-requisitos e ação do clique;
- HUD mostra Mana, Concentração e, quando aplicável, Corrupção/Fragmentos.

## Ecos e bosses

Mantém as correções da v1.3/v1.4:
- Ecos de kill em `AFTER_DEATH`;
- tiers de boss com Ecos próprios;
- Ender Dragon e Wither tratados como bosses explícitos;
- scaling não acumula HP em reload;
- reescala preserva porcentagem de vida;
- multiplayer scaling é reavaliado;
- dano de boss passa pelo pipeline que cobre ataques customizados com atacante identificável.
- chefes de tier 2+ entram na segunda fase com 55% de vida;
- chefes de tier 4+ entram na fase final com 25% de vida;
- as fases elevam a pressão ofensiva em 10%/18%, sem adicionar mais vida.

Existe `StatsManager.addQuestXp(...)` para integração posterior com quests.

## Compilar

Requer:
- Java 17;
- Minecraft 1.20.1;
- Fabric Loader;
- Fabric API.

No Windows, abra um terminal na pasta do projeto e rode:

```bat
gradlew.bat build
```

Saída esperada:

```text
build/libs/rpgstats-1.9.0-alpha.1.jar
```

Depois coloque o `.jar` na pasta `mods` da instância Fabric 1.20.1.

## Validação estática

Sem iniciar Minecraft:

```bash
python tools/validate_project.py
```

O script atual verifica as 5 classes, 25 caminhos, 75 especializações/Ascensões,
810 nodes, IDs duplicados, ligação dos registries e sanidade estrutural dos arquivos Java.
