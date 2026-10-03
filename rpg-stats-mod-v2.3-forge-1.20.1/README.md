# RPG Stats 2.3 — Forge 1.20.1 (alpha)

Porte do ZIP rpg-stats-mod-complete-v2.2-compat-framework.zip.
O projeto foi adaptado para Forge. A compilação completa e o teste no Minecraft
ainda NÃO foram confirmados neste ambiente. Este ZIP contém fontes, não um JAR pronto.

## Compilar no Windows

1. Extraia o ZIP em uma pasta normal.
2. Instale/use JDK 17 (JAVA_HOME deve apontar para o JDK 17).
3. Execute COMPILAR-FORGE.bat com dois cliques.
4. Aguarde: o primeiro build baixa Gradle, Forge e mappings.
5. Se falhar, envie build-forge.log. A janela permanece aberta.
6. Se concluir, o JAR esperado é:
   build/libs/rpgstats-forge-2.3.0-forge-port-alpha.jar

Alternativa no PowerShell, dentro da pasta do projeto:

```powershell
.\gradlew.bat --console=plain build
```

O build inclui verificação do JAR remapeado: mods.toml, config de mixins e refmap
devem estar presentes. Não instale JAR de sources nem JAR de desenvolvimento.

## Instalar depois do build

Crie um perfil Minecraft 1.20.1 com Forge 47.2.0 ou posterior da linha 47.
Copie o JAR normal para mods. Em multiplayer, use a mesma versão do RPG Stats
no cliente e no servidor. Fabric API, Fabric Loader e Architectury API não são
dependências deste mod.

Teste primeiro o core sozinho em mundo separado. Mods do perfil Fabric precisam
de suas respectivas versões Forge; trocar só o loader do perfil não os converte.
Leia MIGRATION.md antes de transportar um mundo ou dados de personagem.

## O que foi preservado

Classes, Casas principal/secundária, especializações, Ascensões, atributos,
habilidades, recursos, custos, caps, árvores, UI e HUD da v2.2.
24 arquivos das regras do jogo foram comparados byte a byte com o ZIP base.

## Iron's

Iron's ainda NÃO está integrado às habilidades ou à Mana. A detecção por
irons_spellbooks está preparada e /rpg compat informa que o adaptador está pendente.
As habilidades atuais do Mago foram preservadas para manter o core utilizável
até a auditoria e integração específica.

## Ferramentas de desenvolvimento

Java 17, Gradle 8.8, Architectury Loom 1.6.422 em modo Forge,
Forge 1.20.1-47.2.0 e Yarn 1.20.1+build.10 para nomes no código-fonte.
Yarn é apenas o mapeamento de desenvolvimento: o JAR final é remapeado para Forge.
Usar esse mapeamento não torna o mod Fabric e não exige uma ponte entre loaders.

Testes locais: python3 tools/validate_project.py.
Eles validam domínio, sintaxe e contratos estáticos. Não substituem o build Forge.
As instruções das versões anteriores ficam em docs/legacy e nos arquivos versionados.
