# Validacao v1.4 — UI Overhaul

## Validacao automatica

`python tools/validate_project.py`

Resultado da revisao:

- 100 nodes encontrados;
- 100 IDs unicos;
- 100 entradas no AbilityRegistry;
- 23 tipos de efeito;
- cobertura do registry: OK;
- consumidores de efeitos: OK.

## Escopo de alteracao

Comparado com a v1.3-fixed, a v1.4 altera apenas a camada visual/documentacao e um helper de apresentacao no `AbilityRegistry`:

- `gui/StatsScreen.java` — refeito;
- `gui/ResourceHud.java` — refeito;
- `gui/RpgButton.java` — novo;
- `gui/RpgUiTheme.java` — novo;
- `ability/AbilityRegistry.java` — somente `activeSummaryForEffect(...)` para a UI reutilizar a descricao de ativas;
- README/changelog/versionamento.

Nenhum handler de combate, boss scaling, persistencia, networking server-side ou formula de progressao foi alterado nesta revisao.

## API 1.20.1 conferida

A UI usa APIs presentes no Yarn 1.20.1:

- `Screen.clearAndInit()`;
- `ButtonWidget` customizado via `renderButton(...)`;
- `DrawContext.fillGradient(...)`;
- `DrawContext.drawTooltip(TextRenderer, List<Text>, ...)`;
- `TextRenderer.trimToWidth(...)`.

## Limitacao do ambiente

O `./gradlew compileJava` nao pode ser concluido neste ambiente porque o Gradle Wrapper tenta baixar o Gradle 8.8 de `services.gradle.org`, e o acesso de rede da sandbox esta bloqueado.

Foi executado `javac` como parser sintatico sobre todo o source tree: nao foram encontrados erros de sintaxe/estrutura Java; os erros restantes sao apenas imports/tipos Minecraft/Fabric ausentes sem o classpath do Gradle.
