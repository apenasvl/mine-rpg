# Balanceamento no porte Forge

Esta etapa preserva o balanceamento da v2.2.
Os arquivos de classes, árvores, abilities, PlayerStats, StatsApplier e cálculos
de combate permanecem idênticos ao ZIP base; não houve remoção de habilidades.
Os dados JSON de bosses, armas, equipamentos e magias também foram mantidos.
As tabelas antigas ficam em docs/legacy/BALANCE.md e nos arquivos BALANCE-v*.md.

O que muda nesta etapa é a infraestrutura: loader, eventos, rede e registro de recursos.
A verificação byte a byte protege contra mudanças acidentais nas regras; ela não
prova comportamento idêntico em jogo, pois os eventos do Forge e outros mods podem
interagir de forma diferente.

Nenhum bônus de Iron's é aplicado automaticamente. A escolha de uma versão do Iron's,
a auditoria A/B/C do Mago e a unificação de Mana continuam pendentes.
