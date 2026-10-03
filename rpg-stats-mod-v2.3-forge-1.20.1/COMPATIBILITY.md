# Compatibilidade Forge

## Plataforma

Minecraft 1.20.1, Forge linha 47, Java 17.
Build fixado em Forge 47.2.0 e Architectury Loom 1.6.422.
Loom é ferramenta de build, não dependência de jogo; Yarn é o mapeamento do código.

O artefato final usa META-INF/mods.toml, SimpleChannel, eventos Forge e mixins
com refmap. Não contém inicializadores Fabric nem imports Fabric em src/main/java.

## Conteúdo opcional

| ID detectado | Estado |
|---|---|
| irons_spellbooks | presença detectada; hooks de spells e Mana ainda não implementados |
| bettercombat | presença detectada; adaptador de combate específico pendente |
| ftbquests | presença detectada; API interna de XP existe, ponte específica pendente |

/rpg compat descreve detecção, sem prometer uma integração já funcional.
As entradas anteriores de conteúdo Fabric não foram transportadas como integrações Forge.
Não foram adicionadas dependências opcionais obrigatórias.

## APIs internas

IntegrationServices, QuestXpService, HouseRules e GlobalCaps continuam disponíveis.
Perfis são carregados em AddReloadListenerEvent usando o carregador síncrono
do Minecraft. O snapshot é trocado ao terminar o reload.
Os formatos e exemplos JSON da v2.2 permanecem em data/rpgstats/rpgstats.
As limitações de implementação descritas na entrega da v2.2 continuam valendo:
metadados não equivalem a hooks ativos de equipamento, postura ou magia externa.

## Integração futura com Iron's

Fixar a versão do Iron's para Forge 1.20.1 e conferir sua API real antes de compilar
um adaptador. Preservar Casas como builds, classificar spells por categorias,
adaptar passivas e evitar duplicação de custo, Mana, dano e procs.
Nenhuma spell própria foi desativada apenas por detectar o mod.

## Referências de desenvolvimento

- https://github.com/architectury/architectury-loom
- https://docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/

Assinaturas Forge/Yarn usadas no porte ainda precisam ser confirmadas pelo
compilador com as dependências reais; inspeção de fontes não é esse teste.
