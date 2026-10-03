# Migração da v2.2 Fabric para a v2.3 Forge

## Base e preservação

Base exclusiva: rpg-stats-mod-complete-v2.2-compat-framework.zip.
Nenhuma versão anterior foi usada para implementar este porte.
O ZIP base foi preservado. O novo projeto fica em uma pasta e ZIP separados.

O identificador rpgstats e a chave NBT rpgstats.data foram preservados.
Dentro dessa chave, o composto rpgstats mantém o formato de PlayerStats da v2.2.
Não houve renomeação ou remoção de IDs de habilidades, Casas ou especializações.
Por isso este porte não aplica reembolso de PH nem troca de árvore.

O método Java de acesso ao composto agora se chama rpgstats$getPersistentData.
Isso evita conflito com Entity#getPersistentData, fornecido pelo Forge.
O nome do método não altera a chave gravada no save.

## Ciclo de vida

- Login: limpa estado transitório, reaplica atributos e sincroniza.
- Clone/respawn: copia o NBT do personagem original e mantém progressão.
- Respawn: refaz recursos máximos, restaura stamina e zera estados táticos como na v2.2.
- Troca de dimensão: reaplica atributos e sincroniza.
- Logout: limpa cooldowns transitórios e limite de pacotes.
- Cliente desconectado: limpa o cache da HUD.
- Encerramento do servidor: limpa os mapas de bosses e rede.
- Morte: recompensa de kill e perda de Ecos ficam em hooks após a morte normal,
  separados do evento cancelável LivingDeathEvent.

O comportamento desses hooks com outros mods ainda exige teste real no Forge.

## Transportar um personagem

Trabalhe numa cópia do mundo antes de abrir qualquer save de Fabric no Forge.
Preservar o NBT do RPG não converte entidades, blocos, dimensões e registros
pertencentes aos demais mods. Não há conversor geral de mundos neste pacote.
Mantenha UUID e dados do personagem correspondentes ao ambiente de destino.
Confira nível, PA/PH, Casas, slots e habilidades depois do primeiro login.

## Rede

Fabric e Forge não se conectam pelo canal antigo.
O protocolo desta edição é forge-2.3-1, com RPG Stats exigido nos dois lados.
Os IDs de ações internas foram mantidos, mas o transporte é SimpleChannel do Forge.
O cliente só solicita alocação/compra/escolha/ativação; os números vêm do servidor.

## Iron's

A migração de loader não desativa habilidades próprias, não concede spells
externas e não unifica Mana com Iron's. Essa é a próxima alteração, depois
de compilar/testar este core e fixar a versão exata do Iron's.
