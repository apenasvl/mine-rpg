# Arquitetura do modpack recomendado

O RPG Stats é a camada de regras. Mods externos fornecem conteúdo e apresentação. O perfil deve continuar iniciando quando qualquer mod opcional for removido.

| Camada | Função no modpack | Regra de integração |
|---|---|---|
| Combate | animações, ataques, parry e dodge | usa stamina/tipo de ataque; fallback vanilla continua disponível |
| Magia | spells, efeitos e animações | cada spell recebe categorias; scaling e custo vêm do RPG Stats |
| Armas | modelos, movesets e weapon skills | tags definem tipo, requisitos, scaling, stamina e status |
| Armaduras | variedade visual e defesa | perfis definem peso, requisito, afinidade e bônus de recurso |
| Bosses | encontros e IA | registry ID/tag define tier, XP, resistências e scaling de grupo |
| Quests | objetivos e narrativa | conclusão chama `QuestXpService` no servidor |
| Estruturas | dungeons e arenas | milestones e bosses alimentam progressão, sem dependência obrigatória |
| Exploração | biomas, regiões e descoberta | recompensas controladas; nenhuma árvore depende de um local opcional |
| Worldgen | distribuição do conteúdo | evite remover dimensões/biomas exigidos por saves existentes |

## Perfil recomendado

- **Core obrigatório:** Fabric Loader 0.19.5+, Fabric API e RPG Stats.
- **Conteúdo opcional:** escolha um conjunto enxuto por categoria e valide versões 1.20.1 Fabric.
- **Cliente/servidor:** mods que afetam números, loot, entidades ou progressão devem estar no servidor. Mods puramente visuais podem ficar no cliente quando sua documentação permitir.
- **Memória:** o alvo de 6–8 GB pede seleção controlada de worldgen, estruturas e texturas. Evite vários mods com a mesma função.

## Ordem para montar

1. valide o core sozinho em mundo novo;
2. adicione um mod de combate e confira dano/stamina;
3. adicione magia/armas e escreva tags/perfis;
4. adicione bosses um pacote por vez e meça duração dos encontros;
5. adicione quests e estruturas depois que a curva de XP estiver estável;
6. rode servidor dedicado para conferir autoridade, sincronização e login sem mods somente de cliente.

## Alvos de balanceamento

- luta de boss comum: 3–7 minutos para jogador preparado;
- jogador extra: mais pressão e vida controlada, sem multiplicar a duração linearmente;
- primeira vitória pode dar bônus, repetição não deve ser a melhor fonte infinita de XP;
- equipamento externo deve abrir builds, não substituir automaticamente todo equipamento anterior;
- nenhuma combinação de mods pode superar os caps finais do core.

Antes de fechar uma lista pública de mods, confirme versão, loader, dependências, licença, manutenção e API de cada projeto. A v2.2 deixa os adaptadores preparados, mas não declara dependências opcionais ainda.

