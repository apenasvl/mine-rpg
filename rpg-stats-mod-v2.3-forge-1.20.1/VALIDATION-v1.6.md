# Validação v1.6 — Compact Combat HUD

## Checks concluídos

- `tools/validate_project.py`: OK
  - 80 nodes base
  - 162 nodes do Mago
  - 242 nodes totais
  - 242 entradas no registry
  - 71 ativas do Mago
  - 5 Casas / 15 especializações / 15 Ascensões
  - cobertura de registry: OK
  - cobertura das ativas do Mago: OK
- varredura `javac` sem dependências Minecraft/Fabric: nenhum erro sintático detectado; os erros restantes são imports/tipos externos esperados sem o classpath do mod.
- cooldowns da HUD não são persistidos no save: continuam pertencendo a `CombatState`.
- somente cooldowns dos quatro slots equipados são enviados ao cliente.
- o cliente interpola os ticks entre sincronizações; o servidor corrige o valor quatro vezes por segundo enquanto existir cooldown visível.
- nenhuma fórmula de dano, custo, Mana, progressão ou árvore da v1.5 foi alterada.

## Build Gradle

O `gradlew compileJava` completo não pôde ser executado neste ambiente porque o Gradle Wrapper tenta acessar `services.gradle.org`, que não possui resolução de rede disponível aqui. Compile no ambiente local antes de usar o JAR final.
