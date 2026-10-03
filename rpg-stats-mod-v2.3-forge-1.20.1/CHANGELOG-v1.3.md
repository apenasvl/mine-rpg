# Changelog — v1.3.0 Structural Fix

## Criticos corrigidos

- Corrigido `});` extra em `RPGStatsMod.java` que impedia compilacao sintatica.
- Corrigida regeneracao de recurso que descartava alteracoes entre gravacoes NBT.
- Corrigido scaling de HP acumulativo de boss em reload.
- Boss nao e mais curado ao recarregar/recalcular scaling.
- Corrigida recursao/procs indevidos de Thorns.
- `move_speed` e `attack_speed` agora sao aplicados de verdade.

## Combate

- `magic_power` agora afeta dano magico, nao todo dano generico.
- Melee/ranged nao sao aplicados a magia por acidente.
- Bloodlust nao duplica o proprio bonus.
- Critico customizado fica restrito a dano fisico.
- Smite conserva valores distintos (ex.: 1.4x e 1.6x), expira e e consumido no proximo ataque melee.
- AOE usa dano magico atribuido ao jogador e filtra jogadores/passivos.
- Arrow Rain mira o ponto apontado em vez de cair apenas ao redor do caster.
- Lifesteal recebe cap de seguranca por hit.
- Guard usa uma camada customizada de mitigacao, evitando empilhamento oculto com Resistance.
- Guardiao recebeu provocacao de hostis proximos.
- Cura de suporte recebeu variante em area.

## Habilidades

- 100/100 nodes continuam registrados no `AbilityRegistry`.
- Todos os effect IDs registrados possuem consumidor implementado.
- Ativa e escolhida/equipada pela arvore.
- Cooldown passou de global para individual por node.
- HUD e menu mostram a ativa equipada.
- Menu identifica nodes ativos, custo de recurso e cooldown.

## Multiclasse

- Profundidade secundaria usa prerequisitos reais da arvore (tiers 1-2), nao `cost <= 1`.
- Removido campo de subclasse secundaria sem fluxo funcional.
- Classe secundaria ganhou pool de recurso proprio.
- Custo secundario agora e regra inteira e explicita: `custo base + 1 PH`.
- Passivas e ativas secundarias usam prefixo `sec_` sem duplicar registry.

## XP e bosses

- XP movido de `ALLOW_DEATH` para `AFTER_DEATH`.
- Ender Dragon/Wither recebem tier 5 e XP de boss.
- XP de boss passa pelo mesmo classificador do scaler.
- Anti-farm cobre `TameableEntity` e `Ownable` com dono.
- Level-up multiplo mostra PA/PH totais ganhos.
- Adicionado `StatsManager.addQuestXp(...)` para integracao futura.
- Boss scaling e dinamico por jogadores proximos e preserva percentual de vida.
- Bonus de dano de boss foi movido para o pipeline contra jogadores, cobrindo ataques customizados que nao usam `GENERIC_ATTACK_DAMAGE`.
- Mobs comuns nao ficam presos no rastreamento do scaler.

## Rede/UI

- Sync de recurso nao reconstrói a tela inteira quatro vezes por segundo.
- Recurso principal e secundario aparecem separadamente.
- Buffs/cooldowns temporarios sao resetados no respawn e desconexao.
