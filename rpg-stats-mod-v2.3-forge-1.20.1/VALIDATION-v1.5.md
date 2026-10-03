# Validation v1.5.0 — Mage Expansion

## Resultado

A revisão estática local passou para a estrutura do projeto v1.5.

Comando:

```bash
python tools/validate_project.py
```

Resultado esperado e obtido:

```text
Nodes base: 80
Nodes Mago: 162
Nodes totais: 242
Registry total: 242
Ativas Mago: 71
Casas: 5 | Especializações: 15 | Ascensões: 15
Custo mínimo até Maestria Arcana: 6 PH
Cobertura registry: OK
Cobertura ativas Mago: OK

VALIDAÇÃO ESTÁTICA v1.5: OK
```

## O que o validator verifica

- 80 nodes das quatro classes ainda não convertidas para o modelo profundo;
- 162 nodes do novo Mago;
- 242 entradas totais de AbilityRegistry;
- duplicatas e registry órfão;
- 12 nodes do núcleo do Mago;
- 5 Casas com 9 nodes cada;
- exatamente 3 nodes cross-house por Casa;
- 15 especializações com 7 nodes cada;
- 15 Ascensões, custo 3 PH, nível 50 e tier 7;
- 71 skills ativas do Mago com implementação no switch do MageCombatHandler;
- nodes sem SkillEffect que dependem de consumidor estrutural;
- requisitos de level e custo dos nodes;
- pré-requisitos inexistentes e ciclos;
- Maestria Arcana alcançável naturalmente antes/ao nível 10;
- maestria de cada Casa alcançável dentro do orçamento natural do nível 25;
- cada Ascensão alcançável com o orçamento natural de 50 PH;
- cross-house apenas em nodes baratos/iniciais;
- quatro active slots e quatro keybinds/traduções;
- ausência de padrões runtime obsoletos importantes;
- delimitadores Java balanceados após remover comentários/strings.

## Sanidade de orçamento

- rota mínima até Maestria Arcana: **6 PH**;
- rota mínima Núcleo + maestria de Casa: permanece abaixo de 25 PH nas 5 Casas;
- rota mínima até uma Ascensão varia aproximadamente de **22 a 29 PH**, dependendo da especialização;
- portanto a build de nível 50 consegue dominar seu caminho principal e ainda tomar decisões opcionais/cross-house, sem ter PH suficiente para comprar a árvore inteira de 162 nodes.

## Sintaxe Java

Também foi executado `javac --release 17` sobre todos os fontes apenas como parser/sanity check. Como as bibliotecas Minecraft/Fabric não estão instaladas no classpath desta sandbox, ele naturalmente retorna erros de imports/símbolos externos. Não apareceram diagnósticos de sintaxe como `illegal start`, `';' expected`, `reached end of file`, `orphaned case` ou delimitadores quebrados.

O subconjunto puro de Java (enums/modelos de árvore, SkillNode, registries do Mago e MageBalance) foi compilado isoladamente com sucesso.

## Gradle build

Foi tentado:

```bash
./gradlew build --stacktrace
```

O wrapper tentou baixar `gradle-8.8-bin.zip`, mas este ambiente não possui resolução de rede para `services.gradle.org` (`UnknownHostException`). Por isso **o JAR final não foi compilado nesta sandbox**.

O teste obrigatório no PC é:

```bat
gradlew.bat build
```

Depois testar o JAR em uma instância Fabric 1.20.1 com Fabric API.
