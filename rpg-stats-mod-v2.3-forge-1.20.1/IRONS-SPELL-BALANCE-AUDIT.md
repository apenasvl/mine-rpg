# Iron's Spells balance audit — RPG Stats

Escopo: integração do Mago com Iron's Spells 'n Spellbooks 1.20.1.

## Regra central

Iron's continua dono do dano nativo, mana, cooldown, cast e comportamento das spells. RPG Stats adiciona progressão, Houses, especializações e mecânicas. O balanceador não reduz o dano nativo da spell: ele limita apenas o bônus adicional colocado pelo RPG Stats.

Isso resolve o principal problema de integração: uma spell single-hit comum não pode receber a mesma amplificação de uma spell multi-hit, persistente, de sustain ou já muito forte no próprio Iron's.

## Perfis

| Perfil | Bônus RPG retido | Multiplicador total máximo | Uso |
|---|---:|---:|---|
| STANDARD | 100% | x2.10 | single-hit simples |
| BURST | 85% | x1.90 | burst/AoE moderado |
| SPAM | 72% | x1.75 | baixo cooldown / repetição rápida |
| HEAVY_BURST | 55% | x1.60 | burst pesado |
| EXTREME | 35% | x1.40 | hit muito alto ou volume total extremo |
| MULTIHIT | 60% | x1.65 | vários projéteis/hits |
| CONTINUOUS | 50% | x1.55 | canalizadas/contínuas |
| PERSISTENT | 45% | x1.50 | zonas/DoT/efeitos persistentes |
| SUMMON | 55% | x1.55 | summons e armas invocadas |
| SUSTAIN | 30% | x1.35 | dano com cura/sustain |
| WEAPON_DERIVED | 30% | x1.30 | dano ligado a arma/objeto; evita double-dip |
| MOBILITY_DAMAGE | 50% | x1.45 | mobilidade que também causa dano |
| UTILITY | 0% | x1.00 | controle, cura, defesa e utilidade |
| UNCLASSIFIED | 70% | x1.70 | fallback seguro para spell nova/desconhecida |

## Spells auditadas

**STANDARD** — acupuncture, wither_skull, fang_strike, spectral_hammer, guiding_bolt, icicle, lightning_bolt, acid_orb, poison_arrow, fire_arrow.

**BURST** — blood_slash, lob_creeper, fireball, magma_bomb, heat_surge, flaming_strike, scorch, divine_smite, cone_of_cold, frostwave, lightning_lance, shockwave, poison_splash, stomp, gluttony, shadow_slash, ice_tomb, frostbite, volt_strike.

**SPAM** — magic_missile, firecracker, gust, firebolt, snowball.

**HEAVY_BURST** — heartstop, sacrifice.

**EXTREME** — magic_arrow, sonic_boom, eldritch_blast, arrow_volley, starfall.

**MULTIHIT** — blood_needles, chain_creeper, flaming_barrage, chain_lightning, ball_lightning, fang_swirl.

**CONTINUOUS** — dragon_breath, fire_breath, sunbeam, ray_of_frost, electrocute, poison_breath.

**PERSISTENT** — black_hole, fang_ward, blaze_storm, wall_of_fire, thunderstorm, blight, firefly_swarm, earthquake, sculk_tentacles, gravity_fissure, raise_hell, ice_spikes, blizzard.

**SUMMON** — raise_dead, summon_vex, summon_polar_bear, wisp, summon_swords.

**SUSTAIN** — devour, ray_of_siphoning.

**WEAPON_DERIVED** — echoing_strikes, telekinesis, throw.

**MOBILITY_DAMAGE** — blood_step, burning_dash, frost_step, charge.

**UTILITY** — counterspell, evasion, teleport, summon_ender_chest, recall, portal, invisibility, shield, summon_horse, slow, wololo, angel_wings, blessing_of_life, cloud_of_regeneration, fortify, greater_heal, healing_circle, heal, haste, cleanse, ice_block, ascension, root, spider_aspect, oakskin, abyssal_shroud, planar_sight, arcane_shackle, scapegoat, touch_dig, pocket_dimension.

Total coberto pelo validator: **111 IDs**.

## Casos de risco confirmados

- `magic_arrow`: dano nativo cresce até 28 no nível máximo da spell, antes de atributos/equipamento; por isso não pode receber a mesma progressão de uma spell comum.
- `sonic_boom`: dano nativo chega a 36, com cooldown alto e cast longo; continua forte, mas o bônus do RPG recebe retenção baixa.
- `eldritch_blast`: 15 por disparo e `2 + spellLevel` recasts; o risco é o dano total da sequência, não um único hit.
- `arrow_volley`: no nível máximo gera 10 linhas x 8 flechas, até 80 projéteis, e remove i-frames entre os hits; por isso foi elevado para EXTREME.
- `starfall`: cast contínuo de 160 ticks, disparando dois cometas a cada quatro ticks; o volume potencial também exige EXTREME.
- `ray_of_siphoning`: é contínua e usa 100% de lifesteal nativo no dano aceito; por isso sustain recebe um cap próprio de x1.35.
- `summon_swords`: invoca três armas por longa duração e já converte spell power em bônus de dano/vida; recebe perfil de summon para evitar multiplicação excessiva.
- `echoing_strikes`: transforma ataques físicos em dano adicional percentual; recebe `WEAPON_DERIVED` para evitar buff de arma + buff mágico completo no mesmo loop.
- `arcane_shackle`: é essencialmente controle/duração/vida da corrente, então não recebe amplificação ofensiva genérica.

## Invariantes

1. Sem nodes/stats do RPG, uma spell continua com o dano que o Iron's produziria.
2. Mana/equipamentos do Iron's continuam autoritativos e não entram nesse balanceador.
3. O perfil atua depois de Inteligência/nível/Houses/nodes e antes do hard cap global.
4. Spells novas que não estejam na lista caem em `UNCLASSIFIED`, que é deliberadamente conservador.
5. O hard cap global x2.25 continua como última defesa, mas os perfis mais perigosos possuem caps bem menores.

## Próximo passo de runtime

Testar pelo menos um exemplar de cada perfil em lvl 1, 25 e 50, usando o mesmo alvo sem armadura/resistência variável. Prioridade: magic_arrow, sonic_boom, eldritch_blast, magic_missile, arrow_volley, starfall, ray_of_siphoning, echoing_strikes, summon_swords e fireball.
