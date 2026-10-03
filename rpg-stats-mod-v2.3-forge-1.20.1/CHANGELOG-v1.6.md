# v1.6 — Compact Combat HUD

## Objetivo
Remover a HUD de recurso do centro/miolo da tela e deixar o combate limpo.

## Mudanças
- Recurso principal agora fica à esquerda da hotbar, em uma faixa de apenas 17 px de altura.
- Centro da tela fica livre; nada do RPG cobre mira, crosshair ou área central de combate.
- Mana/Fúria/Foco/Energia/Fé mostram nome, valor e barra fina.
- Mago ganhou faixa tática micro para Concentração, Corrupção e Fragmentos Temporais.
- Recurso da multiclass aparece em uma microbarra separada, sem criar painel grande.
- R/Z/X/C foram movidos para o canto inferior direito.
- Slots ativos agora exibem cooldown real sincronizado pelo servidor.
- Cooldown é interpolado no cliente para animação fluida e corrigido periodicamente pelo servidor.
- Slots vazios ficaram discretos; slots prontos recebem apenas uma linha de destaque.
- Nenhum valor de balanceamento, árvore ou habilidade da v1.5 foi alterado.
