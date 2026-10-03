# Compilar e testar no Windows

Requisitos: JDK 17 completo (não apenas JRE), Python 3.11 ou superior e internet para a primeira preparação. Feche o Minecraft enquanto compila/testa. Use uma pasta curta, como C:\RPG, e extraia todo o ZIP antes de executar.

Baixe a branch feature/three-mod-boss-progression em https://github.com/apenasvl/gpt/tree/feature/three-mod-boss-progression usando Code > Download ZIP. Entre na pasta rpg-stats-mod-v2.3-forge-1.20.1 que contém gradlew.bat e build.gradle. Clique na barra de endereço do Explorador, digite cmd e pressione Enter.

Verifique:

```bat
java -version
javac -version
py -3 --version
```

Java e javac devem indicar 17; Python deve ser 3.11 ou superior. Se não forem reconhecidos, revise PATH/JAVA_HOME e reabra o Prompt. Não é necessário instalar Gradle separado; gradlew.bat baixa a versão usada pelo projeto.

Execute um comando por vez e espere terminar:

1. Compilação do RPG e do artefato separado de testes:

```bat
gradlew.bat --no-daemon --max-workers=2 --stacktrace -I tools/production-boss-probe.init.gradle build productionBossProbeJar > compilacao.log 2>&1
```

Abra compilacao.log ao terminar. Só prossiga se aparecer BUILD SUCCESSFUL. Durante o download/compilação, o Prompt pode ficar sem imprimir nada porque a saída está no arquivo. A preparação inicial exige downloads e pode demorar.

2. GameTests do RPG no ambiente de desenvolvimento:

```bat
gradlew.bat --no-daemon --max-workers=2 --stacktrace runGameTestServer > teste-rpg.log 2>&1
```

Este perfil não substitui o teste com Iron's e os bosses reais.

3. Teste completo em servidor Forge separado, com binários reais dos mods e os testes nativos dos magos/bosses:

```bat
py -3 tools/run_production_boss_tests.py --full > teste-mods.log 2>&1
```

O script baixa suas dependências de teste, instala um servidor separado dentro de build/production-boss-full e grava a aceitação da EULA do Minecraft nesse servidor. Leia a EULA em https://aka.ms/MinecraftEULA antes de executar. Não usa sua pasta CurseForge nem seus mundos. O servidor de teste usa até 4 GB de RAM.

Evidência esperada: build/production-boss-full/result.json com status PASS e logs com All N required tests passed. O número N pode mudar conforme os testes são adicionados. Um teste falhando precisa ser investigado; apenas gerar um JAR não significa validar o mod.

Envie compilacao.log, teste-rpg.log, teste-mods.log e, se criado, build/production-boss-full/logs/latest.log. Também envie result.json se o teste completo passar. Não envie a pasta inteira.

O JAR do RPG fica em build/libs. O arquivo terminado em -sources.jar é código-fonte, não o mod instalável. O JAR rpgstats-production-probe.jar é só para testes e não deve ir para seu modpack. Aguarde revisão dos resultados antes de trocar a versão que usa para jogar.

A seleção do launcher Windows/Linux foi verificada isoladamente. A execução completa no Windows ainda precisa ser validada no seu computador; qualquer erro deve ser reportado pelo log.
