# RPG Stats v2.2 Compat Framework Alpha

Esta versão continua a v2.1 Action Core. O RPG funciona com apenas Fabric API e ganha uma camada opcional, data-driven e cacheada para conectar bosses, armas, magias, equipamentos e quests de outros mods.

Principais arquivos:

- `COMPATIBILITY.md`: formato dos dados, APIs e regras para adaptadores;
- `MODPACK-DESIGN.md`: arquitetura recomendada para o modpack;
- `CHANGELOG-v2.2.md`: alterações desta versão;
- `VALIDATION-v2.2.md`: testes feitos e checklist dentro do jogo.

## Compilar no Windows

1. instale Java/JDK 17;
2. extraia o ZIP;
3. abra a pasta que contém `gradlew.bat`;
4. clique na barra de endereço do Explorador, digite `cmd` e pressione Enter;
5. execute `gradlew.bat build` ou `.\gradlew.bat build` no PowerShell;
6. pegue o JAR sem `sources` em `build\libs`;
7. coloque o JAR no diretório `mods` do perfil Fabric 1.20.1.

Dependências obrigatórias: Minecraft 1.20.1, Java 17, Fabric Loader 0.19.5 ou mais recente e Fabric API compatível com 1.20.1.

Use `/rpg compat` no jogo para listar as integrações opcionais detectadas.

