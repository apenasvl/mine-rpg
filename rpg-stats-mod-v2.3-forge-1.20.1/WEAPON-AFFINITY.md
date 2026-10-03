# Armas fora da classe — Forge 1.20.1

Classes são afinidades de armas, não bloqueios de uso. Uma arma fora da afinidade causa 62,5% do dano físico final calculado, após os limites globais: 8 vira 5 em condições equivalentes. Nível e atributos continuam obrigatórios. Armaduras conservam sua regra de classe; habilidades e feitiços não são concedidos por equipar uma arma.

Arcos e bestas pertencem à afinidade Arqueiro, adagas classificadas à do Assassino, cajados classificados à do Mago e espadas/machados comuns à do Guerreiro. Regras de equipamentos definidas em dados têm preferência para armas especiais que não são arcos. Itens sem categoria conhecida não recebem uma afinidade inventada.

Galeforce, Darkmoon Longbow, Kraken Slayer, Kraken Slayer Crossbow e Simon's Bowblade ficam utilizáveis por todas as classes que atendam nível e DEX. Nível 31/DEX 25 para Galeforce, Darkmoon e Simon; nível 45/DEX 35 para Kraken. Seus poderes nativos continuam substituídos pelas habilidades do RPG. O bônus aditivo de dano do Ranged Weapon API do próprio item é removido; velocidade, tempo de puxada e encantamentos são preservados. O fator de progressão existente do item é aplicado ao disparo.

O redutor usa a arma registrada no lançamento: trocar de item antes do impacto não o evita. Explosões/fogo secundários registrados no mesmo disparo também recebem o redutor, sem aplicar novamente a progressão do RPG. Dano de espinhos, dano de feitiços do Iron's e procs já calculados mantêm seus caminhos específicos.

Validação: políticas Java reais, permissões de uso nas quatro classes e GameTest com disparos reais dos cinco itens nativos, consumo de flechas dos quatro arcos, besta carregada, afinidade de gelo e troca de arma antes do impacto.
