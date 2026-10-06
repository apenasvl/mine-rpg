# Boss Calibration B — real encounters

Use main after independently merged infrastructure PR9. Preserve UI, classes, weapons and rewards; no new RPG classes, permanently.

The first deliverable is opt-in server-side observation of real encounters, not simulated DPS. Admin starts recording an existing boss and declares offensive/balanced/defensive build. Observe confirmed health loss, incoming sources without invented ownership, resource/stamina and positions, server ticks and wall time. Record player/boss death and interruptions. Export JSON with native mod/Forge versions and equipment/stats. No healing, movement, cooldown, AI or combat modification.

Representative matrix: every current native profile × four existing classes at reference level with balanced build; three priority anchors (Colossus, Returning Knight, Obsidilith) also sample offensive/defensive builds and reference−5/+5/+15 where legal (level cap50). Deduplicate capped levels. Mark impossible very-overlevel trials unavailable. All initial cases NOT_RUN, with no invented TTK.

Boss target90–240s, miniboss30–90s are review references, not assertions on isolated hits. Native phases, summons, native spell cooldowns and manual dodging require real sessions and explicit annotations. Recorded total incoming damage includes unattributed/environment/PvP; separately attributed boss damage uses existing reliable attribution. HP delta plus confirmed damage measures net recovery; it does not identify heal spell source. Tick time and wall time are separate.

Full 62-mod ZIP/Forge47.4.10 validation, legal build equipment and actual encounters remain required before numerical tuning or release. Automation fixtures validate recorder correctness only and are never marked as full encounter wins.
