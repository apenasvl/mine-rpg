# Auditoria Forge 1.20.1 — RPG Stats v2.3

Data: 2026-09-10
Branch: `audit/forge-1.20.1`

## Escopo

Auditoria de entrada do mod, eventos Forge, rede, persistência, mixins, separação client/server, Gradle e riscos de build. Nenhuma feature nova foi implementada.

## Resultado resumido

### OK / estrutura coerente

- `@Mod("rpgstats")` é o entrypoint principal.
- Eventos de gameplay estão no `MinecraftForge.EVENT_BUS` por `@Mod.EventBusSubscriber(... Bus.FORGE)`.
- Registro de teclas/HUD fica em subscriber `Dist.CLIENT` do bus MOD; tick/logout do cliente ficam em subscriber `Dist.CLIENT` do bus FORGE.
- Rede usa `SimpleChannel`, direções explícitas C2S/S2C, limite de payload de 512 bytes, validação de sender/namespace/rate-limit e mutações enfileiradas na main thread.
- Persistência mantém a chave NBT `rpgstats.data/rpgstats`, com migrações de versões anteriores e limpeza de multiclass legado.
- Mixins têm refmap configurado e ficam declarados em `rpgstats.mixins.json`.
- `mods.toml` declara Forge 47.2.x e Minecraft 1.20.1.
- Não há metadata Fabric ativa nem dependência runtime Fabric API/Loader no `build.gradle`.

## Achados

### P0 — build real ainda precisa ser provado

O projeto usa Architectury Loom 1.6.422 em plataforma Forge com Yarn mappings, em vez do ForgeGradle tradicional. Isto pode ser válido, mas só a compilação real confirma nomes/métodos remapeados, mixins e geração do refmap/JAR. A validação existente no repositório é explicitamente estática e não substitui `gradlew build`.

A branch contém um workflow de CI que executa Java 17 + `./gradlew --no-daemon --stacktrace build` para transformar essa incerteza em erro reproduzível.

### P1 — bug na escala da Casa secundária

`AbilityRegistry.sumPassive(..., secondaryOnly)` ignora o parâmetro `secondaryOnly` e chama `get(realId(nodeId))`. Para nodes `borrowed_*`, isso remove o prefixo antes de `get`, pulando `HouseRules.scaled(...)`.

Consequências:

- passivas da Casa secundária podem entrar com 100% do valor em caminhos que usam `sumPassive`;
- chamadas com `secondaryOnly=false` podem incluir passivas secundárias;
- chamadas com `secondaryOnly=true` não filtram somente a secundária.

Isso contradiz a regra de Casa secundária reduzida por categoria e deve ser corrigido antes do balanceamento/Iron's.

### P1 — build/configuração dependem fortemente do Loom

`settings.gradle` ainda inclui Maven Fabric e o projeto usa Yarn mappings. Isso não significa que Fabric esteja carregando em runtime, mas aumenta o risco de confundir uma migração de loader com uma troca completa para APIs/names oficiais Forge. Não remover isto às cegas: primeiro deixar o CI provar o estado atual; depois decidir se permanece Loom Forge ou se migra para ForgeGradle.

### P2 — persistência por mixin é funcional em conceito, mas precisa teste de ciclo de vida

O NBT é anexado diretamente ao `PlayerEntity` via mixin, e `PlayerEvent.Clone` copia o `PlayerStats` do jogador original para o novo. O desenho evita depender do lifecycle de capabilities, mas precisa teste real de morte/respawn, retorno do End, troca de dimensão e servidor dedicado.

### P2 — mixins de dano/morte são ponto de compatibilidade sensível

`LivingEntityDamageMixin` intercepta `damage` e `onDeath`; `ServerPlayerDeathMixin` também intercepta `ServerPlayerEntity.onDeath`. A lógica tenta impedir recompensa duplicada e só premia após morte confirmada, o que é bom, mas estes hooks são os pontos mais sujeitos a conflito com outros mods de combate/bosses. Precisam teste em runtime depois que o core compilar.

### P2 — divergência de design já presente, não corrigida nesta auditoria

O começo ainda aplica 33 pontos de atributos específicos ao escolher a classe. Isso não é problema de Forge, mas diverge da direção de começo mais neutro/universal definida para o RPG. Deve ficar para uma etapa de design/balanceamento depois do core compilar.

## Ordem recomendada

1. Fazer `gradlew build` passar no CI/Windows.
2. Corrigir erros reais de compilação/remapeamento antes de refactors.
3. Corrigir `AbilityRegistry.sumPassive` para respeitar Casa secundária e escala por categoria.
4. Testar cliente + servidor dedicado: login, sync, tela, packet C2S, respawn, dimensão.
5. Testar mixins de dano/morte e BossScaler em runtime.
6. Só então integrar Iron's Spells e revisar habilidades redundantes do Mago.
