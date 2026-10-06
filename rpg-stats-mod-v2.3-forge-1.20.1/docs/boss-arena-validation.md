# Boss Calibration B — collecting real encounters

Base main4bffa80 after separately merged UI8 and infrastructure9. Current list is generated from native_bosses.json, not a fixed PR number.28 profiles/220 representative trials in compat/boss-arena-matrix.json are ALL NOT_RUN. Four classes only; no new RPG classes, permanently.

## Recording in Minecraft

Admin permission2 is required. Use normal survival combat, native boss AI enabled, gravity enabled, legal allocated stats/gear, normal resource costs and cooldowns. Start at full boss HP before the fight; keep unrelated players away for solo trials.

Example:

```mcfunction
/rpg debug arena start @e[type=soulsweapons:returning_knight,limit=1,sort=nearest] balanced
/rpg debug arena mark phase52
/rpg debug arena mark summons
/rpg debug arena stop
```

Build labels: offensive, balanced, defensive. They describe the intended build; legality is explicitly NOT automatically approved. Confirm against normal progression and save equipment/stats review with evidence. Confirmed death schedules finalization after the damage return, so the fatal hit is included. Logout, dimension and server stop interrupt/clean state. Session limit8, maximum12000ticks/1200wall seconds. Markers are bounded manual annotations, not detected boss phases.

Export uses a bounded background writer and immutable report text; successful writes log RPG_ARENA_REPORT, failures log an explicit error. JSON: game directory/rpgstats/arena/<session UUID>.json. Contains actual health loss confirmed after damage acceptance/mitigation, damage to boss from others, incoming source breakdown, reliable boss-attributed incoming damage, HP recovery without inferred healing source, resources (native Iron mana via existing read bridge for Mago), stamina, RPG cooldown snapshots, held items/gear NBT, positions, boss distance, mod versions and Forge version. Samples every20ticks; HP/resource net changes sampled each server tick. Disabled observer does not scan entities.

TTK is emitted only after actual boss death when recording started at full boss health. Manual stops and interruptions never become wins. Wall seconds and game ticks are separate; lag changes their relationship. Actual resource costs that spend/regenerate in the same tick may cancel in the net sample; native spell cooldowns/misses/uptime, vulnerability windows, summon ownership and phase logic need additional native instrumentation/manual replay. No proximity-based ownership is added.

## Validation boundaries

Recorder GameTests verify confirmed/rejected damage and lifecycle cleanup. They disable boss AI/use controlled players and are NOT encounter wins or balance evidence. Native isolated Forge47.4.0 tests are NOT the62-mod current ZIP/Forge47.4.10. Inspect runtime/AI/gravity/mod metadata before accepting an arena result.

Next: execute representative matrix with legal builds and real native combat for Legendary/Marium/BoMD, review outliers around boss90–240s/miniboss30–90s, then tune only supported numbers. B remains Draft until those measurements and current modpack validation exist. XP relative/farm/Threat and weapons remain later PRs.

A revisão reproduziu fechamento precoce em dano letal antes do retorno de damage; o gravador agora aguarda o dano confirmado. A fixture de delta de saúde prepara o scaling vanilla antes da medição, pois a entrada no encounter muda o HP máximo preservando porcentagem. Isso não altera o comportamento de produção.
