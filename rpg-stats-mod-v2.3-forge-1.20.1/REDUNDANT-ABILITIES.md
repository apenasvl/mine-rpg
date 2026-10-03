# Habilidades redundantes — auditoria global

## Resumo

A redundância principal não está somente em nomes parecidos. Ela está na implementação: dezenas de nodes diferentes convergem para os mesmos poucos efeitos passivos e ativos.

O objetivo deste documento é impedir que o rework apenas troque descrições enquanto mantém o mesmo backend genérico.

## Grupo A — progressão por sufixo idêntico

`ClassAbilityRegistry` resolve nodes gerados pelo template usando sufixos, portanto especializações diferentes recebem exatamente a mesma lógica.

| Sufixo | Efeito executável atual | Diagnóstico |
|---|---|---|
| `_core_awakening` | `resource_flat +3` | mesma mecânica nas quatro classes |
| `_core_flow` | `resource_on_hit +1` | mesmo gatilho, embora os recursos devam ter loops diferentes |
| `_core_form` | +2% tipo de dano | stat aceitável, mas repetido |
| `_core_guard` | +2% redução | stat genérico |
| `_core_efficiency` | -4% custo | stat genérico |
| `_core_tempo` | `rotation_damage +4%` | mesma rotação para todas |
| `_core_resolve` | proteção em HP baixo | safety net idêntica |
| `_core_combo` | três ações = +3 recurso | combo artificial idêntico |
| `_setup` | `prepared_cast` | preparação igual para todos os caminhos |
| `_reaction` | `cast_guard` | reação igual para todos |
| `_synergy` | `rotation_damage` | sinergia igual para todos |
| `_economy` | -5% custo | stat genérico aceitável como suporte |
| `_engine` | `prepared_cast` | motores de especialização que não são motores próprios |
| `_conversion` | `combo_resource` | "conversão" que na prática repete recurso genérico |
| `_risk` | +dano e vulnerabilidade em HP baixo | mesmo risco/recompensa em fantasias diferentes |

### Correção

Esses sufixos podem continuar existindo como posições/IDs para preservar saves, mas não podem continuar determinando o efeito apenas pelo nome. Cada caminho/especialização deve possuir registro explícito ou uma camada de mecânica por domínio.

## Grupo B — poucas ativas usadas por dezenas de nodes

Famílias atuais reutilizadas:

- `active_guard_taunt`
- `active_bloodlust`
- `active_group_heal`
- `active_smite`
- `active_arrow_rain`
- `active_dash`
- `active_smoke`
- `active_aoe`
- `active_power`

O roteamento atual usa substrings do ID (`van`, `bers`, `cler`, `mark`, `skirm`, `shadow`, `venom` etc.). Isso cria falsos poderes diferentes.

### Exemplos de mesma mecânica / números diferentes

#### Guarda/provocação

Vanguarda, Bastião, Juggernaut e outros IDs defensivos podem cair em `active_guard_taunt`. Technique, signature e ascension frequentemente continuam sendo a mesma ação com valor/duração maiores.

**Substituir por:** Guarda acumulada, interceptação, Inércia, Riposta, área protegida ou ameaça marcada, dependendo da spec.

#### Cura em área

Comandante, Clérigo, Juramento/Farol e várias signatures podem cair em `active_group_heal`.

**Problema:** liderança marcial, cura clerical e aura de juramento viram a mesma cura com números diferentes.

**Substituir por:** Moral/ordem, Graça/triagem, Convicção/aura e Radiância/conversão, respectivamente.

#### Chuva de flechas

Atirador/Artífice e outros IDs ranged podem compartilhar `active_arrow_rain`.

**Problema:** precisão, balística, armadilha e engenharia não deveriam convergir para AoE de flechas.

**Substituir por:** ponto fraco, perfuração, armadilha, munição preparada ou dispositivo.

#### Dash

Escaramuçador, Duelista, Vazio/Fantasma e outros caminhos móveis podem compartilhar `active_dash`.

**Problema:** movimento de arqueiro, reposicionamento de duelista e fase do vazio tornam-se a mesma habilidade.

**Substituir por:** Momentum/ângulo, Vantagem/spacing, fase com Energia/Dívida do Vazio. Não restaurar dodge universal.

#### Fumaça

Sombra/Infiltrador reutilizam `active_smoke`.

**Problema:** furtividade ofensiva e infiltração/preparação viram a mesma área de fumaça.

**Substituir por:** Abertura consumida no primeiro golpe vs Entrada/Disfarce construído fora de combate.

#### AoE genérica

Veneno, Sabotador, Arqueiro Arcano e outros IDs podem cair em `active_aoe`.

**Problema:** toxina, explosivo e elemento se resumem a explosão mágica.

**Substituir por:** Doses consumidas, carga armada/dispositivo e reação elemental de flecha.

#### Smite genérico

Guerreiro Rúnico, Inquisidor/Aurora e afins podem cair em `active_smite`.

**Problema:** runa marcial e julgamento sagrado ficam funcionalmente iguais.

**Substituir por:** consumo de Carga Rúnica no golpe vs consumo de Julgamento/Radiância.

#### Power/Bloodlust

Várias signatures/ascensions convergem em buff genérico de poder.

**Problema:** capstone muda só porcentagem/duração.

**Substituir por:** estado temporário que altera regras do motor da especialização e possui custo/saída clara.

## Grupo C — passivas por substring

O registry atual usa verificações como `contains("van")`, `contains("bers")`, `contains("rune")`, `contains("ward")`, `contains("skirm")`, `contains("venom")`, `contains("cler")` etc.

Isso faz todos os nodes de uma família receberem os mesmos dois stats básicos.

### Exemplos

- Guerreiro defensivo: `damage_reduction + resource_on_hurt` repetido.
- Guerreiro Berserker: `melee_damage + lifesteal` repetido.
- Guerreiro Rúnico: `melee_damage + magic_power` repetido.
- Arqueiro padrão: `ranged_damage + crit_chance` repetido.
- Escaramuçador: `move_speed + ranged_damage` repetido.
- Assassino padrão: `melee_damage + crit_chance` repetido.
- Veneno: `poison_hit + magic_power` repetido.
- Paladino: combinações de `magic_power`, `melee_damage`, `damage_reduction`, `heal_on_kill`.

Esses stats podem continuar como pequenos `STAT NODE`, porém não podem representar a maior parte de uma especialização.

## Grupo D — conteúdo legado anterior às árvores expandidas

`AbilityRegistry` ainda contém habilidades antigas como:

### Guerreiro
`golpe_esmagador`, `furia`, `pele_aco`, `veterano`, `colosso` e subclasses antigas com vários upgrades puramente numéricos.

### Arqueiro
`olho_aguia`, `reflexos`, `tiro_certeiro`, `vento`, `lenda`, além de subclasses antigas com ranged/move/crit repetidos.

### Assassino
`lamina`, `passo`, `veneno`, `sombra`, `ceifador`, além de Ninja/Alquimista/Espectro antigos com progressão principalmente de stats.

### Paladino
`luz`, `escudo`, `julgamento`, `aura`, `santificado`, além de Templário/Clérigo/Inquisidor antigos com melee/magic/defense repetidos.

### Decisão

Antes de apagar esses IDs, verificar se ainda podem existir em saves antigos, migração ou telas legadas. Se forem legacy-only, manter migração compatível e impedir que coexistam com a nova árvore como fonte dupla de bônus.

## Grupo E — technique -> signature -> ascension vertical

O template atual cria a sequência:

`_technique` -> `_signature` -> `_ascension`

A diferença frequentemente é apenas:

- valor maior;
- duração maior;
- cooldown maior;
- custo maior.

Isso é exatamente o padrão proibido.

### Regra nova

- **Technique:** ensina a ferramenta central.
- **Signature:** exige ou consome o motor da build de forma diferente.
- **Ascension:** muda temporariamente uma regra do motor.

Exemplo Guerreiro/Berserker:

- Technique: gastar parte da Fúria em golpe que aumenta stagger.
- Signature: consumir toda Fúria para entrar em Frenesi.
- Ascension: durante Frenesi, execução prolonga a janela, mas cura recebida cai; não é apenas `Frenesi com +X%`.

## Grupo F — habilidades semanticamente iguais entre classes

Mesmo quando IDs/efeitos não são literalmente iguais, estes padrões devem ser evitados:

1. `Após habilidade, próximo ataque ganha dano` em todas as classes.
2. `Abaixo de 40% HP, ganha dano/defesa` como motor de múltiplas specs.
3. `3 ações diferentes geram recurso` para todas.
4. `matar inimigo recupera recurso` como principal sustain.
5. `dash + próximo hit mais forte` repetido em Guerreiro, Arqueiro, Assassino e Mago.
6. `marca -> +% dano` sem diferença de consumo/comportamento.
7. `ascension = buff temporário de dano/velocidade` em toda spec.

Cada classe deve responder uma pergunta diferente:

- Guerreiro: **consigo quebrar postura e sobreviver ao compromisso do ataque?**
- Arqueiro: **estou na posição/distância/linha correta?**
- Assassino: **já preparei a abertura para entrar e sair?**
- Paladino: **gasto meu recurso protegendo ou julgando?**
- Mago: **qual spell/escola/sequência transforma melhor o estado da build?**

## Grupo G — Mago + Iron's

Não reintroduzir redundância removida no rework do Mago.

- Projétil elemental próprio vs spell real do Iron's: redundante.
- Heal/barreira/dash/teleport próprios quando há equivalente Iron's: redundante, salvo mecânica única explicitamente justificada.
- Nature não equivale automaticamente a summon.
- Holy não equivale automaticamente a Arcane para todas as passivas.
- `Conjuração Instantânea` não pode significar apenas CDR sem renomeação.
- Todo modifier prometido no documento precisa ter detector/adapter real.

## Critério de aprovação de um node após rework

Um node mecânico só é considerado não-redundante se pelo menos uma destas condições for verdadeira:

- cria estado/stacks próprios;
- muda condição de uso;
- muda alvo/área/trajectory/posicionamento;
- consome ou converte recurso de modo único;
- cria interação com postura/guard/parry/mark/status/summon/spell;
- introduz risco/recompensa específico;
- altera qualitativamente technique/signature;
- cria decisão entre duas saídas válidas.

Apenas aumentar dano, duração, velocidade, cura, alcance ou reduzir cooldown/custo não é suficiente para classificar um node como nova mecânica.