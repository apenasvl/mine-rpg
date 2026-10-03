# RPG Stats v2.2 — Compatibilidade

## Arquitetura

O core continua jogável sozinho. `compat/CompatManager` é o único ponto que detecta mods opcionais; ele nunca importa classes desses mods. Adaptadores recebem somente `CompatContext` e a fachada `IntegrationServices`.

O fluxo é:

1. o servidor carrega JSON e tags de todos os datapacks;
2. `DataDrivenRegistry` monta um snapshot imutável e cacheado;
3. o core ou um adaptador consulta `IntegrationServices`;
4. requisitos, Casa principal/secundária e caps são aplicados pelo core;
5. a decisão final de XP, dano e recursos permanece no servidor.

Pacotes novos:

- `com.rpgstats.compat`: descoberta e adaptadores opcionais;
- `com.rpgstats.integration`: perfis, registro data-driven e fachada pública;
- `com.rpgstats.api`: entrada segura para recompensas de quests;
- `com.rpgstats.balance`: caps globais compartilhados.

## Detecção inicial

| Mod ID | Estado na v2.2 | Ativação segura |
|---|---|---|
| `spell_engine` | descritor preparado | categorias e scaling passam por `IntegrationServices` |
| `bettercombat` | descritor preparado | reservado para tipo de ataque/stamina |
| `bosses_of_mass_destruction` | descritor preparado | bosses podem ser classificados por JSON/tag |
| `soulsweapons` | descritor preparado | armas e bosses podem receber tags/perfis |
| `ftbquests` | descritor preparado | deve chamar `QuestXpService`; sem import direto no core |

Esses descritores detectam presença e expõem o ponto de extensão. Chamadas específicas às APIs externas só devem ser adicionadas depois de conferir versão, licença e assinatura real. Sem esses mods, nenhuma classe externa é carregada.

## Arquivos data-driven

O loader lê todo arquivo `data/<namespace>/rpgstats/*.json`. Cada arquivo possui `type` e `entries`. Um datapack pode adicionar um novo arquivo sem recompilar o mod.

Tipos aceitos:

- `bosses`: `id` ou `tag`, tier, XP, bônus de vida/dano por boss e por jogador, resistências;
- `weapons`: `id` ou `tag`, categorias, requisitos STR/DEX/INT/FÉ/ARC, peso, stamina, velocidade e status;
- `equipment`: mesmo formato de item para peso, afinidade e requisitos;
- `spells`: `id`, categorias, custo de recurso e escala de poder.

Exemplo de boss externo:

```json
{
  "type": "bosses",
  "entries": [{
    "id": "outro_mod:boss_id",
    "tier": 4,
    "xp": 500,
    "health_bonus": 0.65,
    "damage_bonus": 0.20,
    "health_per_player": 0.25,
    "damage_per_player": 0.05,
    "status_resistance": 0.60,
    "stagger_resistance": 0.70
  }]
}
```

IDs exatos têm prioridade; depois vêm tags; por último, a heurística existente usa tipo, vida e pistas no registry ID. A heurística é fallback, não fonte principal.

## Casas e magias externas

Uma magia pode ter várias categorias, por exemplo `elemental` e `lightning`. `IntegrationServices.spellMultiplier` aplica 100% do bônus elegível da Casa principal. A Casa secundária recebe a eficiência reduzida definida em `HouseRules` e mantém suas penalidades de custo. O resultado passa por `GlobalCaps`.

Integrações não devem calcular esse bônus por conta própria. Isso evita que uma spell externa contorne as limitações da Casa secundária.

## Armas

Armas classificadas já participam do dano físico do core. Falhar nos requisitos aplica multiplicador `0.70`. Cumprir requisitos ativa um scaling gradual limitado a `+60%`, e o resultado final ainda respeita o cap global de dano.

Peso, stamina, attack speed e status buildup já fazem parte do perfil público. Adaptadores futuros podem consumi-los, mas devem manter o servidor como autoridade.

## Bosses

Perfis controlam tier, XP, vida, dano e crescimento por jogador. O scaling preserva a porcentagem de vida atual e limita o grupo a quatro jogadores adicionais. Status resistance já reduz a duração do veneno genérico. Stagger resistance permanece como metadado enquanto postura/quebra de postura estiver fora do escopo do core.

O mod não troca a IA de bosses externos.

## Quests

Uma integração chama no servidor:

```java
QuestXpService.grant(player, new Identifier("mod_da_quest", "quest_id"), xp);
```

O serviço rejeita valores inválidos, limita uma recompensa a 25.000 XP e usa o mesmo sistema de nível/persistência do core.

## Caps centrais

`GlobalCaps` limita multiplicador final de dano a 2,25x, crítico a 25%, lifesteal a 12%, redução de cooldown a 35%, attack speed bônus a 40%, cast speed bônus a 35% e redução de dano a 45%. Adaptadores devem chamar esses métodos no fim do cálculo.

## Como adicionar uma integração real

1. confirme Minecraft 1.20.1, Fabric, versão da API, licença e manutenção;
2. mantenha todo import externo dentro do adaptador específico;
3. garanta que a JVM só toque nesse adaptador depois de `isModLoaded`;
4. converta conteúdo externo em `BossProfile`, `ItemProfile` ou `SpellProfile`;
5. encaminhe números para `IntegrationServices` e `GlobalCaps`;
6. teste com o mod ausente, sozinho e junto com as demais integrações.

