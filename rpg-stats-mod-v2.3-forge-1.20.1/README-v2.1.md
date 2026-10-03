# RPG Stats 2.1 Action Core — alpha
Fonte baseada no ZIP 2.0 fornecido. Sem novos chefes ou modpack.
Esta versão implementa a primeira etapa, não certifica todas as habilidades em jogo.

## Compilar no Windows
Instale JDK 17. Abra PowerShell na pasta que contém gradlew.bat:
```powershell
.\gradlew.bat build
```
O primeiro build precisa de internet para Gradle e dependências. O JAR instalável, se o build concluir, estará em build/libs; não use o arquivo sources.jar. Este ZIP contém código, não um JAR já compilado.
Use uma cópia do mundo, Minecraft 1.20.1, Fabric e Fabric API compatíveis com gradle.properties.

Leia CHANGELOG.md, BALANCE.md, MIGRATION.md e VALIDATION.md.

