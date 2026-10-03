# Validacao v1.3.0

## Passou

- 100 nodes encontrados / 100 unicos.
- 100 entradas no AbilityRegistry / 100 unicas.
- Nenhum node removido em relacao ao `rpg-stats-mod-expanded`.
- Mesmas 5 classes e 15 subclasses da base.
- 23 tipos de efeito registrados; todos possuem consumidor no codigo.
- JSON de `fabric.mod.json` e `rpgstats.mixins.json` valido.
- Verificacao sintatica via `javac` sem dependencias nao encontrou erros de parser; os erros restantes sao apenas imports/tipos externos ausentes sem o classpath Fabric/Minecraft.
- APIs criticas usadas foram conferidas contra Yarn/Fabric 1.20.1: AFTER_DEATH, getEntitiesByClass, raycast, DamageTypeTags.BYPASSES_RESISTANCE, DamageSources.indirectMagic, Entity.isTeammate e modifiers de atributo.

## Build Gradle

Foi tentado:

```bash
./gradlew compileJava --no-daemon
```

Neste ambiente a execucao para antes da compilacao porque o Gradle Wrapper nao consegue resolver `services.gradle.org` (`UnknownHostException`). Portanto o JAR final nao foi fabricado aqui.

Em um ambiente com acesso a Maven/Fabric, rode:

```bash
./gradlew build
```

Saida esperada: `build/libs/rpgstats-1.3.0.jar`.
