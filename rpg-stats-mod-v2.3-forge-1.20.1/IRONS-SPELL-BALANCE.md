# Iron's Spells — balance pass do RPG Stats

Escopo: integração Forge 1.20.1 usada pelo RPG Stats. O objetivo é manter o Iron's como arsenal principal do Mago sem permitir que uma única curva global de progressão transforme spells multi-hit, persistentes, sustain ou baseadas em arma em opções dominantes.

## Regra central

O Mago continua progredindo com Inteligência, nível RPG, Core, Casa, especialização e nodes. A diferença é que o **bônus adicionado pelo RPG Stats** agora é retido de acordo com o formato real da spell. Para alguns formatos que já são muito fortes no Iron's puro existe uma normalização leve do dano nativo. Mana reduction e CDR também são filtrados por perfil; portanto uma spell extrema não recebe ao mesmo tempo todo o aumento de dano, -45% de custo e -50% de cooldown.

| Perfil | Dano nativo | Retém bônus RPG | Multiplicador total máx. | Retém redução de Mana | Retém CDR |
|---|---:|---:|---:|---:|---:|
| STANDARD | 100% | 100% | x2.10 | 100% | 100% |
| BURST | 100% | 85% | x1.90 | 90% | 85% |
| SPAM | 95% | 72% | x1.70 | 80% | 70% |
| HEAVY_BURST | 95% | 55% | x1.55 | 70% | 65% |
| EXTREME | 85% | 35% | x1.35 | 55% | 55% |
| MULTIHIT | 90% | 60% | x1.55 | 75% | 70% |
| CONTINUOUS | 90% | 50% | x1.45 | 75% | 70% |
| PERSISTENT | 90% | 45% | x1.40 | 70% | 65% |
| SUMMON | 95% | 55% | x1.50 | 80% | 70% |
| SUSTAIN | 90% | 30% | x1.30 | 75% | 70% |
| WEAPON_DERIVED | 95% | 25% | x1.25 | 85% | 80% |
| MOBILITY_DAMAGE | 95% | 50% | x1.35 | 90% | 85% |
| UTILITY | 100% | 0% dano | x1.00 dano | 90% | 80% |
| UNCLASSIFIED | 90% | 50% | x1.40 | 75% | 70% |

`UNCLASSIFIED` é propositalmente conservador para uma spell futura do Iron's não entrar no modpack com multiplicador completo sem auditoria.

## Overrides críticos

As spells abaixo recebem regra própria porque o formato delas é muito mais perigoso que a média do perfil:

- **Magic Arrow** — nativo 90%, bônus RPG 40%, teto x1.35. Na referência de 28 nativo: ~30.8 com progressão geral e teto ~37.8.
- **Sonic Boom** — nativo 85%, bônus RPG 30%, teto x1.25. A referência de 36 fica ~36 numa build geral e ~44 no cenário extremo; continua nuke, mas não explode com a progressão.
- **Eldritch Blast** — nativo 75%, bônus RPG 25%, teto x1.20. Cada blast é controlado porque a spell possui vários recasts; o nível alto não multiplica 3–7 hits como se cada um fosse uma spell isolada.
- **Arrow Volley** — nativo 80%, bônus RPG 25%, teto x1.20 pelo volume de projéteis.
- **Starfall** — nativo 85%, bônus RPG 30%, teto x1.25 pela soma de impactos.
- **Ray of Siphoning / Devour** — sustain recebe ganho ofensivo menor porque dano também converte em sobrevivência.
- **Sacrifice** — burst pesado com normalização própria.
- **Echoing Strikes / Shadow Slash** — proteção extra contra double dipping de dano de arma + dano mágico.

## Catálogo auditado — 111 spells

### STANDARD
`acupuncture`, `wither_skull`, `fang_strike`, `spectral_hammer`, `guiding_bolt`, `icicle`, `lightning_bolt`, `acid_orb`, `poison_arrow`, `fire_arrow`

### BURST
`blood_slash`, `lob_creeper`, `fireball`, `magma_bomb`, `heat_surge`, `flaming_strike`, `scorch`, `divine_smite`, `cone_of_cold`, `frostwave`, `lightning_lance`, `shockwave`, `poison_splash`, `stomp`, `gluttony`, `ice_tomb`, `frostbite`, `volt_strike`

### SPAM
`magic_missile`, `firecracker`, `gust`, `firebolt`, `snowball`

### HEAVY_BURST
`heartstop`, `sacrifice`

### EXTREME
`magic_arrow`, `sonic_boom`, `eldritch_blast`, `arrow_volley`, `starfall`

### MULTIHIT
`blood_needles`, `chain_creeper`, `flaming_barrage`, `chain_lightning`, `ball_lightning`, `fang_swirl`

### CONTINUOUS
`dragon_breath`, `fire_breath`, `sunbeam`, `ray_of_frost`, `electrocute`, `poison_breath`

### PERSISTENT
`black_hole`, `fang_ward`, `blaze_storm`, `wall_of_fire`, `thunderstorm`, `blight`, `firefly_swarm`, `earthquake`, `sculk_tentacles`, `gravity_fissure`, `raise_hell`, `ice_spikes`, `blizzard`

### SUMMON
`raise_dead`, `summon_vex`, `summon_polar_bear`, `wisp`, `summon_swords`

### SUSTAIN
`devour`, `ray_of_siphoning`

### WEAPON_DERIVED
`echoing_strikes`, `telekinesis`, `throw`, `shadow_slash`

### MOBILITY_DAMAGE
`blood_step`, `burning_dash`, `frost_step`, `charge`

### UTILITY
`counterspell`, `evasion`, `teleport`, `summon_ender_chest`, `recall`, `portal`, `invisibility`, `shield`, `summon_horse`, `slow`, `wololo`, `angel_wings`, `blessing_of_life`, `cloud_of_regeneration`, `fortify`, `greater_heal`, `healing_circle`, `heal`, `haste`, `cleanse`, `ice_block`, `ascension`, `root`, `spider_aspect`, `oakskin`, `abyssal_shroud`, `planar_sight`, `arcane_shackle`, `scapegoat`, `touch_dig`, `pocket_dimension`

## Invariantes de balanceamento

- Spells STANDARD continuam aproveitando a progressão forte do Mago; não foi feito um nerf global da classe.
- Multi-hit, DoT, persistent e sustain são avaliados pelo **dano total potencial**, não por um hit isolado.
- Dano derivado de arma não pode receber todo o bônus físico da arma e depois todo o multiplicador mágico novamente.
- Utility não recebe progressão ofensiva só por gerar um evento de dano incidental.
- O hard cap global de dano do RPG Stats continua em x2.25 como última proteção.
- Mana do Iron's continua autoritativa e bônus de equipamento não é cortado pelo RPG Stats.
- O balanceamento de spell não muda o sistema de boss/XP e não reintroduz segunda vida, dodge universal ou multiclass.

## Playtest recomendado

Comparar a mesma spell em lvl 1, 25 e 50; depois comparar uma STANDARD, uma SPAM, uma MULTIHIT, uma PERSISTENT e cada uma das cinco EXTREME. Para o endgame, a referência atual é o Guerreiro completo em aproximadamente 28 de dano por golpe: o Mago pode passar disso em casts lentos/caros ou em dano total de uma sequência, mas não deve manter esse valor em cada hit de uma spell com muitos hits/recasts.
