# RPG Stats v1.4.0 — UI Overhaul

## Novo visual

- Novo tema dark-fantasy/arcano totalmente desenhado por codigo, sem depender de resource pack externo.
- Paleta de destaque por classe.
- Painel central responsivo, header de personagem e XP bar.
- Sidebar com recursos principal/secundario, habilidade ativa, PA e PH.
- Cards de atributos com valores base/total, descricao mecanica e progresso ate o cap base.
- Classes e subclasses agora usam cards grandes e tematicos.
- Arvores de classe, subclasse e multiclasse agora sao realmente desenhadas como arvores, com conexoes entre pre-requisitos.
- Nodes possuem estados visuais: bloqueado, disponivel, desbloqueado e equipado.
- Tooltips de nodes exibem efeitos reais lidos do AbilityRegistry, requisitos, custo e acao disponivel.
- HUD de combate redesenhado e compactado.

## UX

- Skills ativas sao identificadas claramente na arvore.
- A ativa equipada recebe destaque azul e aparece na sidebar/HUD.
- PH/PA nao gastos recebem destaque no header/sidebar.
- Multiclasse possui identidade visual da classe secundaria.
- A tela continua nao pausando o jogo.

## Codigo

- Adicionado `RpgUiTheme.java` para cores e primitivas visuais.
- Adicionado `RpgButton.java` para substituir o visual vanilla dos botoes.
- `StatsScreen.java` refeito em torno de layout responsivo e arvore visual.
- `ResourceHud.java` refeito.
- `AbilityRegistry.activeSummaryForEffect(...)` adicionado apenas como helper de apresentacao; a logica de combate nao foi alterada.

## Preservado da v1.3

- 5 classes.
- 15 subclasses.
- 100 nodes e 100 registros de efeito.
- Multiclasse, recursos separados, cooldown individual, boss scaling e correcoes estruturais da v1.3.
