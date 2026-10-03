# Iron's + Mage class integration audit

## Resultado

Iron's continua sendo o arsenal de spells. RPG Stats continua sendo a classe Mago, as Casas, especializações, progressão e buildcraft.

O problema encontrado era parcial: dano/escolas já conversavam com várias partes da árvore, mas o **ciclo real de cast do Iron's** ainda não alimentava algumas mecânicas que haviam sido escritas para o pipeline antigo de `mage_active`. Isso deixava Flow, Triad, Fragmentos, Echoes e algumas especializações inconsistentes.

Esta fase liga casts/hits reais do Iron's à árvore do Mago sem criar um segundo catálogo de spells.

## Core do Mago

Casts reais do Iron's agora alimentam:

- Fluxo Arcano: 3 spells diferentes preparam -12% de Mana para a próxima;
- Guarda Arcana: Mana realmente gasta pelo Iron's conta para a janela de 35 Mana;
- Eco Arcano: pode armar um eco de +35% no primeiro hit aceito, ainda limitado pelo perfil/budget da spell;
- Concentração: uma spell ofensiva que realmente acerta gera Concentração uma vez por cast, não por projétil;
- preparação/causal loop/forma arcana/winter heart passam a alterar o custo real do Iron's quando a condição existe.

## Cinco Casas

### Elemental
Fire/Ice/Lightning do Iron's já usam Calor, Frio, Carga, reações e os modificadores elementais. Isso foi preservado.

### Arcana
Spells do Iron's agora fornecem categorias para Sequência/Tríade, geram Selos Arcanos e Ciclo Perfeito. O Finalizador consome o selo no próximo hit real do Iron's.

### Conjuração
O RPG agora modifica o atributo nativo `SUMMON_DAMAGE` do Iron's, que é o atributo que as próprias entidades invocadas consultam. O bônus RPG é limitado a +25%. Nature deixa de receber bônus de summon indiscriminadamente: somente spells classificadas como `SUMMON` recebem esse multiplicador.

### Oculta
Blood/Eldritch casts reais passam a gerar Corrupção com gate de 1s. Eficiência Proibida, Eclipse/Forma e custos condicionais entram no custo real do Iron's.

### Temporal
Casts reais alimentam Ritmo Temporal, Fragmentos, Momentum e portanto o CDR real das spells do Iron's.

## Quinze especializações

1. **Pyromancer** — Fire spells aplicam Calor; Combustão, Inferno Controlado e Phoenix continuam no loop real.
2. **Cryomancer** — Ice spells aplicam Frio; Freeze/Shatter continuam; Winter Heart também afeta custo e dano de Ice de forma limitada.
3. **Stormcaller** — Lightning aplica Carga/Sobrecarga; área e mobilidade continuam ligadas às spells reais.
4. **Runist** — spells do Iron's lançadas perto das próprias runas recebem +5% de sinergia antes dos caps/budget.
5. **Illusionist** — Scapegoat/Invisibility continuam gerando cargas; Reflexo Arcano e Mirror Hall agora funcionam com hits do Iron's e só uma vez por cast.
6. **Telemancer** — Teleport/steps armam Compressão; Black Hole/Gravity Fissure continuam usando os modificadores espaciais.
7. **Conjurer** — summons reais passam a usar o bônus nativo `SUMMON_DAMAGE` do Iron's, incluindo Assault/Ephemeral dentro do cap.
8. **Animist** — Nature/Holy reais alimentam Vida/Terra/Caça conforme a spell; Harmony passa a ser alcançável pelo arsenal real.
9. **Astral Forger** — Summon Swords/Echoing Strikes/Magic Missile/Magic Arrow e Ender continuam alimentando o arsenal astral.
10. **Bloodmancer** — Blood aplica Hemorragia; Ray of Siphoning/Devour recebem a Transfusão limitada e boss-efficient.
11. **Curseweaver** — Blood/Eldritch já aplicam Fraqueza/Fragilidade; Blight/Wither Skull/Heartstop agora alimentam Ruína e podem completar a Marca do Destino.
12. **Hexblade** — alternância melee → Iron spell → melee continua alimentando Spellblade; Shadow Slash continua com modificador específico.
13. **Accelerator** — casts reais agora geram Momentum; Haste/Charge e Distorted Time alteram CDR real do Iron's.
14. **Stagnator** — Slow/Root/Arcane Shackle passam a alimentar Time Lock de forma boss-safe.
15. **Reverser** — Temporal Echo agora é consumido pela próxima spell ofensiva real do Iron's, antes dos caps/budget.

## Proteções de balanceamento

- Echo, Mirror Hall, Temporal Echo, rune synergy e demais boosts entram **antes** de `IronsSpellBalance.applyRpgScaling`;
- depois ainda passam pelo hard cap global;
- por último passam pelo orçamento por `caster + target + spell` da fase anterior;
- portanto multi-hit, channels e volleys não transformam um proc de especialização em multiplicação por projétil;
- Conjuration/SUMMON_DAMAGE tem cap próprio de +25%;
- Nature não é mais sinônimo de summon para o multiplicador de dano.

## Teste de runtime recomendado

- Arcana: lançar 3 categorias diferentes e confirmar Selo/Finalizador;
- Temporal/Accelerator: 3 casts rápidos, observar Fragmento e redução de cooldown;
- Illusionist: Invisibility/Scapegoat + spell ofensiva; Mirror Hall em sequência de casts;
- Reverser: armar Temporal Echo e acertar uma spell do Iron's;
- Conjurer: comparar dano de um mesmo summon antes/depois dos nodes de Conjuração;
- Bloodmancer/Curseweaver: Blood/Eldritch em um alvo e observar Hemorragia/Ruína/Fragilidade;
- Runist: comparar a mesma spell dentro e fora do raio de uma runa;
- repetir testes multi-hit para confirmar que o orçamento single-target anterior continua valendo.
