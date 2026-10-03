# Validação — v2.3 Forge alpha

## Executado

- 3.288 checks de domínio Java aprovados pelo harness herdado.
- 48 arquivos Java aceitos pelo parser sintático do javac.
- Contratos estáticos Forge aprovados: entrypoints, metadata, refmap declarado,
  ausência de imports Fabric, cobertura das ações de rede e ciclo de vida.
- 24 arquivos das regras comparados byte a byte com o ZIP v2.2: idênticos.

Os testes de domínio usam stubs NBT fora de src/main. Eles não inicializam Minecraft.
O parser Java não resolve tipos nem assinaturas Forge/Yarn.
Não foi testada renderização, servidor dedicado ou compatibilidade com Iron's.

## Build real: NÃO confirmado

Java 17 está disponível, mas Gradle 8.8 não está instalado/em cache.
A tentativa de acesso ao repositório externo foi bloqueada/cancelada pela camada
de acesso à rede do ambiente. Não foi possível resolver a ferramenta e as
dependências para executar compileJava/remapJar. Nenhum JAR instalável foi gerado.

Execute COMPILAR-FORGE.bat no Windows com JDK 17. O build deve passar por
compileJava e remapJar e pela tarefa verifyForgeJar, que verifica mods.toml,
config dos mixins e refmap no JAR final. Envie build-forge.log em caso de falha.

## Matriz de testes em jogo — todos pendentes

| Cenário | Verificar |
|---|---|
| Core sozinho, cliente e servidor dedicado | abrir sem Fabric ou mods opcionais |
| Personagem novo | despertar, escolher classe, atributos, PA/PH |
| Casas e especializações | principal/secundária, requisitos, Ascensão |
| Todas as classes | ativação R/Z/X/C, custo, cooldown e dano |
| Mago | Mana, Concentração, Corrupção, Fragmentos e summons |
| HUD e menu | teclas remapeadas, detalhes de habilidade comprada, telas pequenas |
| Logout/login | dados persistidos; cache visual limpo |
| Morte/respawn | Ecos e habilidades preservados conforme regra existente |
| Morte cancelada por outro mod | não conceder XP nem perder Ecos prematuramente |
| Duas mortes com respawn | aplicar perda de Ecos em cada nova entidade de jogador |
| Boss | XP uma vez por morte, scaling e percentagem de vida |
| Troca de dimensão e retorno do End | NBT e atributos, ausência de duplicação |
| Reload de datapack | recarregar os perfis e tags |
| Dois jogadores | rede, ações, custos e sincronização |
| Mods opcionais ausentes/presentes | detectar sem classes externas obrigatórias |

Não confundir detecção de Iron's com integração de suas spells. Testes de Mana
unificada e modificadores externos pertencem à próxima etapa.
