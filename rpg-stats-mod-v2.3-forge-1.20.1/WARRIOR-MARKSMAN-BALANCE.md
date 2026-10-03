# Guerreiro e Atirador — Forge 1.20.1

Armas continuam utilizáveis por outras classes; o dano físico fora da afinidade permanece em 62,5%.

Cura de combate do Guerreiro: 50% com armas fora da classe; 70% com armas de duas mãos da classe; 100% com arma normal da classe. Aplica-se o menor fator, sem multiplicar as duas penalidades. Uma arma rápida na mão secundária compartilha a penalidade da dupla. Roubo de vida, cura por Sede, cura por abate e cura direta nativa do Simply Swords compartilham o teto de 1,5 ponto de vida em qualquer janela de 20 ticks. Poções, regeneração vanilla e cura de suporte continuam separadas.

Sede: ganho proporcional ao dano confirmado, com referência de 8 pontos e máximo de um ganho a cada 8 ticks. O bônus da especialização continua existindo; armas fora da afinidade e de duas mãos reduzem o ganho. O segundo ganho duplicado foi removido. Não se consome Sede quando o teto de cura impede recuperar vida.

Simply Swords: afinidade usa os dados `weapon_attributes` do próprio mod, incluindo herança do Better Combat. Sai, rapieira e soul knife usam afinidade de Assassino. Dados nativos desconhecidos recebem o fator conservador de dano. Arquivos de dados e tags podem ajustar categorias sem API obrigatória do mod.

Atirador (caminho principal, fundamento desbloqueado): flecha totalmente carregada pode adquirir um monstro visível entre 8 e 28 blocos, dentro de um cone de 6 graus. Custa 6 Foco e tem intervalo de 40 ticks. Correção máxima de 1,5 grau por tick, durante até 8 ticks, com desvio total limitado a 6 graus. Preserva velocidade e gravidade; não acrescenta dano. Obstáculos encerram a correção; não se troca o alvo durante o voo. Jogadores, aliados e animais domesticados são excluídos. Bestas e projéteis de área não recebem assistência.

Verificação inclui políticas puras e GameTests no servidor Forge. A suíte completa carrega o JAR original Simply Swords 1.70.2 (Forge 1.20.1, CurseForge 8746028), além dos mods de bosses e arcos já usados. O teste automatizado não substitui uma partida longa para avaliar a sensação de mira e a dificuldade de todas as arenas.
