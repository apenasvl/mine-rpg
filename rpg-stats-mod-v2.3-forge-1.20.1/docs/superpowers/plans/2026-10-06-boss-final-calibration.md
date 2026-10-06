# Boss final calibration implementation plan

> **For agentic workers:** Execute inline with superpowers:executing-plans. User authorized corrections without further permission, preserving UI and existing mechanics.

**Goal:** Close attributable boss damage gaps and produce explicit per-entity calibration coverage before changing numeric balance.
**Architecture:** Preserve existing BossScaler and class mitigation. Associate the first fall with an accepted upward boss hit, with world/time/landing and lifecycle bounds; never infer a boss from proximity. Fall uses class mitigation, never the boss attack multiplier. Environmental traps remain environmental.
**Tech Stack:** Forge 1.20.1, Java 17, Yarn development/SRG production, native pinned mods.
**Spec:** docs/superpowers/specs/2026-10-06-rc-scope.md

## Global constraints
- No UI rework, classes, multiclass, new spell catalog or repeated imports.
- Off-class coefficient remains 0.625.
- Only pinned Legendary, Marium and BoMD bosses; manual profiles win.
- No claims of arena or complete-modpack validation from stationary fixtures.

## Review focus
- Boss launch must not buff ordinary falls/PvP; no attack multiplier on landing.
- Rejected/canceled boss hits must not grant fall protection.
- Expiry, second landing, logout/death/dimension/restart clear attribution.
- Party/class armor mitigation stays on the existing path.
- Source-free trap damage remains environmental.

### Task 1: Reproduce the boss launch landing gap
**Files:** gametest/BossFallDamageGameTests.java; .github/workflows/forge-build.yml.
**Interfaces:** consumes CombatHandler.modifyIncomingDamage and ForgeEvents.confirmedDamage; produces an assertion that the same attributable fall receives existing class mitigation, ordinary fall does not.
- [ ] Add native regression for all four classes, accepted upward hit, ordinary/canceled hit, expiry and lifecycle reset.
- [ ] Run native regression on the unchanged damage pipeline and observe failure.

### Task 2: Track only confirmed launch falls
**Files:** boss/BossLaunchTracker.java; combat/CombatHandler.java; forge/ForgeEvents.java; mixin/LivingEntityDamageMixin.java.
**Interfaces:** tracker records accepted upward boss hit, resolves a pending fall, consumes it after damage return; no NBT changes.
- [ ] Add the smallest tracker and reuse existing class boss defense, excluding BossScaler damage multiplication for falls.
- [ ] Clear after first fall/landing/expiry and player lifecycle transitions.
- [ ] Run regression and existing class/boss GameTests; inspect native logs.

### Task 3: Record calibration evidence
**Files:** tools/audit_boss_calibration.py; docs/boss-calibration-audit.md.
- [ ] Enumerate all configured bosses, native registry attributes, reference levels, profile factors, attack route coverage and missing arena evidence.
- [ ] Validate counts/IDs against pinned baselines and pack manifest.
- [ ] Review PR A diff, compile/build and test native binaries; keep PR stacked on approved UI until integration is valid.

Subsequent plans: PR B relative-level XP/farm/fallback; PR C weapons/classes; PR D release/persistence/multiplayer.
