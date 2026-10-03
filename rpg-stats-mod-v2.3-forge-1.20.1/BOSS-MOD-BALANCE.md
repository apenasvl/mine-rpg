# RPG Stats — bosses e progressão, Forge 1.20.1

## Instalação

1. Feche o Minecraft.
2. Retire o JAR antigo do RPG Stats da pasta `mods`.
3. Coloque `rpg-stats-forge-1.20.1-bosses.jar` nessa pasta.
4. Mantenha os mods e suas dependências instalados pelo CurseForge. O RPG não inclui esses mods.

Instale o mesmo JAR do RPG no cliente e no servidor. Não use duas versões do RPG Stats juntas. Não precisa de instalador Python, KubeJS ou configuração manual para ativar estes perfis.

## Versões usadas na validação

- Minecraft 1.20.1, Forge 47.4.0.
- Legendary Monsters 2.2.3: arquivo CurseForge 8902349.
- Marium's Soulslike Weaponry 1.4.10: arquivo 8946380.
- Bosses of Mass Destruction Forge 1.1.2: arquivo 5067728.
- T.O e Assassin's Craft não fazem parte desta integração.

A combinação completa também iniciou com Combat Roll, Immersive Armors, os mods de arco anteriores e Iron's Spells 3.16.3, mais suas dependências. Presence Footsteps continua opcional no cliente.

## O que mudou

- 15 bosses e 13 minibosses receberam faixas fixas de nível. O dano não aumenta automaticamente conforme o nível da vítima.
- O grupo aumenta a resistência do encontro com retornos decrescentes, limitado a oito participantes reais.
- 54 armas de Marium foram adaptadas: modelos e ataques básicos preservados, requisitos de classe, nível e atributos verificados no servidor.
- Poderes nativos de armas, armaduras, teclas e summons que contornavam o RPG foram substituídos pelas habilidades das classes. Outros 46 itens de combate ficaram bloqueados; permanecem no inventário. A chave Somber mantém sua interação com fechaduras.
- Armaduras destes dois mods têm defesa e robustez limitadas ao orçamento da netherite. Poderes extras nativos não fazem parte da build.
- Após nível 20, cada nível acrescenta 0,6 de vida máxima real. Atualize o Codex usando `/rpg guia`.
- Golpes completamente absorvidos não contam como contribuição; o golpe final é registrado antes de distribuir a recompensa.
- XP de conclusão é dividido entre participantes que realmente causaram dano, tankaram ou ajudaram aliados. Apenas ficar perto não gera recompensa.
- A transição do Moonknight não recompensa XP. A dupla Day Stalker/Night Prowler usa o vínculo nativo de UUID e só conclui após os dois serem derrotados.

## Validação e limites

Os sete trabalhos da execução 36804730096 passaram. Os testes com os três mods originais passaram 55/55 tanto no perfil de bosses quanto na combinação completa. O JAR entregue tem o mesmo SHA256 do JAR usado no teste completo.

O ataque comum real do Colossus e o do Returning Knight foram testados contra as quatro classes no nível25 com netherite ProteçãoIV e uma build legal defensiva. Essas duas referências pressionam o nível25 em até três golpes confirmados; nos níveis35 e45, respectivamente, as quatro classes sobrevivem a um golpe comum.

Darkin Blade/Guerreiro, Pure Moonlight Greatsword/Mago e Moonveil/Assassino ficaram abaixo do teto de115% do dano básico da referência legal do RPG nas janelas de5,30 e60 segundos. Este teste não cobre todas as combinações de Casas, especializações ou encantamentos de outros mods.

Os demais bosses têm progressão inicial configurada e aplicação verificada. Seus tempos de luta em arena, ataques sem autoria explícita e todas as janelas especiais ainda não receberam calibração individual completa. A sonda estacionária preserva invulnerabilidades nativas e não comprova tempo de derrota de uma luta real. Os alvos90–240s para bosses e30–90s para minibosses são objetivos de ajuste, não uma garantia desta versão.

O vínculo da dupla é resolvido entre entidades carregadas na mesma dimensão. As contribuições transferidas ficam no NBT do sobrevivente; descarregar o parceiro antes da primeira morte ainda exige validação adicional. Os modelos dos três mods não tiveram um teste visual completo de cliente nesta etapa.

## Faixas configuradas

| Níveis | Encontros |
|---|---|
| 1–10 | Exploração e monstros comuns; sem novo boss dedicado |
| 11–20 | Night Shade; Warped Fungussus |
| 21–30 | Draugr, Lich; Skeletosaurus, Lava Eater, Shulker Mimic, Dune Sentinel e Resurrected Knight |
| 31–40 | Chaos Monarch, Cloud Golem, Possessed Paladin, Gauntlet e Void Blossom; Colossus, Endersent, Frostbitten Golem, Withered Abomination, Ancient Guardian, Annihilation Pursuer e Beheaded Knight |
| 41–50 | Accursed Lord, Returning Knight, Moonknight, Day Stalker/Night Prowler, Obliterator e Obsidilith |

Nível da faixa é uma recomendação de progressão, sem imunidade artificial contra jogadores abaixo dela. Os requisitos e decisões por ID estão em `VALIDACAO.json`.

SHA256 do JAR: `5f2cdbc97755e3da9431e1939559e7a021f7b02af516a6eb1eb39a9187ba65e1`.
