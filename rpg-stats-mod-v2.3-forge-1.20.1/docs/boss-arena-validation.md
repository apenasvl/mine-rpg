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

Server shutdown drains pending exports for at most5seconds and explicitly logs timeout/interruption; integrated-server restart can reuse the writer. Export uses a bounded background writer and immutable report text; successful writes log RPG_ARENA_REPORT, failures log an explicit error. JSON: game directory/rpgstats/arena/<session UUID>.json. Contains actual health loss confirmed after damage acceptance/mitigation, damage to boss from others, incoming source breakdown, reliable boss-attributed incoming damage, HP recovery without inferred healing source, resources (native Iron mana via existing read bridge for Mago), stamina, RPG cooldown snapshots, held items/gear NBT, positions, boss distance, mod versions and Forge version. Samples every20ticks; HP/resource net changes sampled each server tick. Disabled observer does not scan entities.

TTK is emitted only after actual boss death when recording started at full boss health. Manual stops and interruptions never become wins. Wall seconds and game ticks are separate; lag changes their relationship. Actual resource costs that spend/regenerate in the same tick may cancel in the net sample; native spell cooldowns/misses/uptime, vulnerability windows, summon ownership and phase logic need additional native instrumentation/manual replay. No proximity-based ownership is added.

## Validation boundaries

Recorder GameTests verify confirmed/rejected damage and lifecycle cleanup. They disable boss AI/use controlled players and are NOT encounter wins or balance evidence. Native isolated Forge47.4.0/47.4.10 tests are NOT the62-mod current ZIP. Inspect runtime/AI/gravity/mod metadata before accepting an arena result.

Next: execute representative matrix with legal builds and real native combat for Legendary/Marium/BoMD, review outliers around boss90–240s/miniboss30–90s, then tune only supported numbers. B remains Draft until those measurements and current modpack validation exist. XP relative/farm/Threat and weapons remain later PRs.

## Import reviewed native encounter evidence

Run `tools/import_boss_arena_evidence.py` against the original NOT_RUN matrix. Reports must be real survival encounters, start at full boss HP, and finish with boss/player/mutual death. Interrupted recordings remain useful diagnostic files but do not complete a matrix case. Other damage or attackers invalidate a solo trial. Player/mutual death never becomes a successful TTK. Trials are retained individually; missing cases stay NOT_RUN and no aggregate balance verdict is invented.

Each report needs a separate human review JSON. This is a reviewer attestation tied to the exact report bytes by SHA-256, **not cryptographic proof of honest gameplay or an automatic build-legality check**. The reviewer must inspect the replay and stats/equipment, verify normal combat throughout the session (initial flags alone cannot prove this), and describe native mechanics including missing measurements. Do not change the report bytes after review.

```json
{
  "schema": 1,
  "session": "<session UUID from report>",
  "report_sha256": "<SHA-256 of unchanged report file>",
  "reviewer": "<reviewer name>",
  "build_legal": true,
  "build_notes": "<normal stat budget, nodes and gear; changes during fight reviewed>",
  "replay_reference": "<replay path or URL>",
  "runtime_scope": "NATIVE_SELECTION",
  "runtime_notes": "<actual versions/configuration and selection tested>",
  "mechanics": {
    "phases": "<observations>",
    "special_attacks": "<observations>",
    "summons": "<observations, or explicitly none>",
    "indirect_sources": "<observations and ownership limits>",
    "native_cooldowns": "<observed timings or explicitly unmeasured>",
    "vulnerability_windows": "<observations>",
    "movement": "<dodging, chase, distance>",
    "combos": "<observations>",
    "misses": "<count or explicitly unmeasured>",
    "uptime": "<measurement or explicitly unmeasured>"
  }
}
```

```sh
sha256sum path/to/report.json
python3 tools/import_boss_arena_evidence.py \
  --matrix compat/boss-arena-matrix.json \
  --report path/to/report.json --review path/to/review.json \
  --output build/arena-reviewed.json
```

Repeat `--report`/`--review` in matching order for multiple trials. Supply all reviewed reports each time; the source matrix is preserved. Validation failure leaves an existing output intact. Synthetic tests never get imported into the committed matrix. Output status MEASURED_REVIEWED means a completed, manually reviewed native-selection trial; it does not mean calibrated, every metric measured, or release-ready. Native cooldown/uptime limitations remain explicit in the replay review.

`FULL_MODPACK` is deliberately rejected: this tool cannot prove that all manifest files/configs loaded correctly or validate multiplayer/persistence. Those require independent modpack evidence. The supplied ZIP currently contains a 62-file CurseForge manifest and one overridden RPG JAR, not the other 62 binaries or encounter reports. Importing it alone does not run Minecraft or measure fights.

## Supplied ZIP configuration audit — 2026-10-07

Read directly from the supplied `rpg_astra2.zip`, without changing any config. Manifest SHA-256: `d68f6308fd2d0219e67cb73ec74aaf3c2fe8d21d090734bcc728f899ef4293a0`. Minecraft1.20.1/Forge47.4.10,62 CurseForge file references, one overridden RPG JAR, no arena reports. This identifies the supplied snapshot; it does not prove the user's current installed instance still matches it.

| Config | Values present in the ZIP | Encounter review relevance |
| --- | --- | --- |
| Marium Returning Knight | HP500, armor15, damage modifier1; attack/special/summon cooldown40/80/200ticks | Preserve native timing and summons when measuring a full fight; these are config values, not measured scaled HP/TTK. |
| BoMD Obsidilith | HP300, armor14, attack16, idle healing0.5/tick, anvil explosion strength4 | Record combat uptime and idle regeneration; isolated hits cannot establish encounter duration. |
| Legendary general settings | MiniBoss DamageCap21; natural healing, resistance reduction and teleport-to-spawn enabled | Replay must include damage caps, disengagement and native defensive-effect behavior. No numeric change is justified by config inspection alone. |
| Better Combat server | attack interval cap2ticks; fast attacks enabled; dual-wield attack-speed multiplier1.2000000476837158; movement multiplier while attacking0.5 | Real melee cadence/movement must use the installed combat system; stationary vanilla attack fixtures are insufficient. |

The overridden RPG JAR is named `RPG-Stats-Interface-Limpa-Forge-1.20.1.jar`; its name is not proof of current PR contents. Replace the old RPG JAR with the tested current artifact for future arena sessions, retaining only one RPG JAR. Keep these supplied native configs unless a measured outlier supports a later change.

### Exact modpack binary inventory

`compat/modpack-snapshot/manifest.json` preserves the supplied manifest bytes. `source.json` fingerprints the supplied ZIP and all82config files without republishing binaries. This is the supplied snapshot, not an assertion that the user's installed instance has not changed.

```sh
python3 tools/audit_modpack_manifest.py \
  --manifest compat/modpack-snapshot/manifest.json \
  --jars build/modpack-inventory/jars \
  --output build/modpack-inventory/report.json --download
```

The CI `modpack-inventory` job resolves every exact project/file pin and inspects root/nested Forge descriptors, SHA-256, declared mandatory dependencies and sides, loader ranges, duplicate mod IDs and unsupported archives. No files are swapped or silently omitted; failed exact downloads fail the job and produce an INCOMPLETE_DOWNLOAD report. Third-party JARs are not uploaded as artifacts or packaged into RPG Stats.

All62exact pins were resolved by CI37695055881. Inspection showed that the supplied manifest includes resource packs: project231821(Dramatic Skys),383269(Enhanced Audio),296616(Round Trees). They are not Forge mod JARs. The auditor distinguishes a resource pack using actual ZIP pack metadata/assets, absence of Java bytecode and absence of Fabric mod metadata; it does not silently install these into mods. Final archive counts are exported in the report. A library-only nested archive remains an explicit runtime verification item, not a purported incompatible standalone mod.

INVENTORY_COMPLETE means all declared numeric metadata checks completed; INVENTORY_WITH_FINDINGS means the audit completed with actionable incompatibilities or checks that require Forge's resolver. Both always have `runtime_validated=false` and `release_ready=false`. The CI job's success means the inventory was produced, not that the pack booted. Maven qualifiers/unions, language-provider versions and JarJar duplicate resolution stay explicit findings rather than using an approximate comparator to approve them.

The native production inspector now uses the actual requested/installed Forge version for loader metadata. Previously this one check was hardcoded47.4.0 even for47.4.10; accepted/rejected range tests reproduce the error. The game loader log remains the authoritative evidence of the runtime actually booted. This fix does not change gameplay or prove full encounters.

Fresh review caught an additional call site: full-profile extra libraries did not carry an installed dependency map, so they still took the47.4.0fallback. They now receive the requested Forge version explicitly while avoiding premature dependency checks against an incomplete map. Malformed range syntax and unresolved standalone versions are also reported rather than producing clean metadata inventory. All three review findings were reproduced in failing tests before correction.

A revisão reproduziu fechamento precoce em dano letal antes do retorno de damage; o gravador agora aguarda o dano confirmado. A fixture de delta de saúde prepara o scaling vanilla antes da medição, pois a entrada no encounter muda o HP máximo preservando porcentagem. Isso não altera o comportamento de produção.

## Forge version comparison

The native production harness retains47.4.0 and adds the same full pinned compatibility selection on47.4.10, with original native binaries. It verifies the loaded Forge version from the loader log, not an installer filename. Report/runtime metadata and CI artifacts preserve actual version evidence. This selection still excludes other mods/configs from the62-mod ZIP and is not a complete-pack validation.

Official changelog: https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-changelog.txt . Between0 and10, fixes include reach/container and mixin compatibility, lava ignition, command completion/class loading, block-break reset, an engine fix optimization and ASM update. No change to RPG numbers is justified by that list alone; compare actual runtime tests and later arena data.

## Supplied-pack normal dedicated-server smoke

The `modpack-server-smoke` CI job stages all57Forge mod archives and2Forge library containers from the exact manifest, plus the current compiled RPG JAR. Its3resource packs are staged separately. It installs Forge47.4.10 and launches a normal dedicated server, without GameTest flags, in a new seed0world at normal difficulty, view/simulation distance3. A pass requires the actual47.4.10loader log, normal DedicatedServer Done message and clean shutdown after10seconds. This is boot evidence only: no players, client rendering, fights, multiplayer or persistence approval.

`configs.json` contains81UTF-8 configs from the supplied ZIP:80preserve original bytes, while one disabled web-config service password is blanked for public CI. Its disabled setting is preserved. One Windows native DLL is excluded from the Linux test. The original ZIP remains unchanged; these differences are exported in the smoke report. No native JARs, worlds or configs are uploaded in the CI artifact; logs, exact inventory and result are retained. An incompatible original archive fails visibly rather than being removed or replaced.
