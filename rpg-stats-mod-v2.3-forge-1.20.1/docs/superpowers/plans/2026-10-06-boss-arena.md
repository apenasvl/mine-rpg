# Boss Arena Implementation Plan

> Execute inline with superpowers:executing-plans; user explicitly authorized the next safe stage without further design handoffs.

**Goal:** Capture reviewable real-fight evidence for Boss Calibration B before tuning numbers.
**Architecture:** Opt-in bounded recorder consumes confirmed damage/death and server ticks; writes JSON under rpgstats/arena. Matrix tool reads current native profiles and native attribute baseline, never a fixed historical list.
**Tech Stack:** Forge1.20.1, Java17/Gson, Python unittest.
**Spec:** docs/superpowers/specs/2026-10-06-boss-arena-design.md

## Global Constraints
- No new RPG classes; no UI, rewards or weapons changes.
- No movement/AI/healing/damage/resource modification.
- Full encounters and full modpack remain NOT_RUN until measured.

## Review Focus
- Rejected damage must not be counted.
- Death/interruption must not fabricate a successful TTK.
- Other attackers/environment remain explicit, not inferred boss ownership.
- Disabled recording has no entity scans or per-player sample allocation.
- Logout/dimension/server stop must bound and clean sessions.

### Task1: representative matrix
- [x] RED unit tests exact current IDs, four classes, capped/deduplicated levels, no measured outcomes.
- [x] Implement tools/build_boss_arena_matrix.py, generate compat/boss-arena-matrix.json.
- [x] Run tests and check current28 profiles.

### Task2: real encounter observation
- [x] Add GameTests for confirmed/rejected damage, death/interruption, isolated sessions and cleanup.
- [x] Add debug/ArenaRecorder.java; admin /rpg debug arena start <boss> <build>, stop and mark commands.
- [x] Wire confirmed damage/death, tick and forget to the opt-in observer.
- [x] Run native CI; review; retain Draft while real encounter measurements remain missing. Head7e0c618:8/8green,178GameTests including loaded Forge47.4.10.

### Task3: encounter evidence (required before B completion)
- [x] Add strict reviewed-evidence import, preserving raw trials and NOT_RUN cases; never approve full modpack or release automatically.
- [x] Add exact62-file modpack inventory job and snapshot/config fingerprints; verify metadata against actual Forge47.4.10 and preserve unresolved findings. Inventory is not runtime/encounter proof.
- [ ] Execute matrix real fights and annotate phases/summons/cooldowns/movement/windows.
- [ ] Validate Forge47.4.10 complete current ZIP, compare isolated47.4.0.
- [ ] Tune only observed outliers; review four-class tier coverage and native replays.
