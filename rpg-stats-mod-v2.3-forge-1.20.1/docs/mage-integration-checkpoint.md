# Mage integration checkpoint

Status: implementation preserved; final compilation and native integration validation are NOT complete.

The GitHub Actions run for 87e464ccf108ed5913766db99010241b3ca03463 failed before every job started after the user reported exhausting the included Actions minutes. Future checkpoint pushes must include [skip ci] until a deliberate validation route is available.

Verified before quota block:
- Native Winter Heart, Ice Barrier, Invisibility/Ghost Attack, curse Weakness/Fate, targeted Root/Time Lock, Portal/Charge/Burning Dash preparation and next-hit consumption passed on 623f59546fed53f4f77a1956de33232e62b1d677.
- On that commit the only failing native test was the summon fixture using the wrong ownership interface.
- Actual installed native diagnostics on 7ead657c146ad27498f7426f58aa3b8078bc5206 identify IMagicSummon, not the MagicSummon interface found in older upstream branches. The native entity was alive, in the same world, and indexed.
- GuideScreen routes and corruption helper (14 boundary cases) passed locally. Earlier builds/client tests passed before the final summon integration.
- 7ead657 also reported elementalSignatureConsumesNativeDummyMark failing. This archer test must be rechecked; do not dismiss it as flaky without evidence.

Measured controlled Returning Knight damage at level50/INT50, maximum native spell rank, one native damage callback (fixed random seed; not DPS or an entire channeled cast): Fireball5=63.359985 HP; ConeOfCold10=20.373535 HP; LightningLance10=66 HP; Accelerator MagicArrow10=52.345215 HP.

Pending verification:
- Compile optional native ownership adapter and core default/delegation APIs.
- Run the corrected native summon test: real Zombie/Skeleton/Vex ownership, world discovery, focus, Transfer healing, removal, diversity and command headroom.
- Check all existing native, physical role, archer, no-Iron and client tests.
- Review the final summon integration (including Astral defense/formation).
- Build the final JAR and verify its hash against the tested native artifact. Do not distribute a new JAR as validated before this step.

Native adapter supports runtime IMagicSummon and older MagicSummon reflectively, strict ownership, alive/same world/radius48, at most32 tracked entities. Transfer heals20% without changing native expiry. Passive summon damage capped35%; Assault/Army use the strongest single command (+12/+15%), final50% cap. Existing spell profiles and target budgets remain in place.

## Continuation: Frost progression

Fragility at level 30 no longer depends on the level-45 Deep Freeze talent. At 100 Frost, purchased Fragility consumes Frost and arms +8% raw magic for 3 seconds. Without Deep Freeze it grants no slow/freeze. Purchased Deep Freeze preserves the existing mob freeze (25 ticks) and boss slow (60 ticks, amplifier 0); bosses never gain frozenTicks.

Fresh local verification: MageFrostCycleTest passed 6 progression/control cases after reproducing the old dependency failure; MageCorruptionTextTest passed 14 HUD cases plus guide-copy checks; validate_corruption_guide.py passed; Java syntax parser accepted 26 files. Syntax parsing is not full type checking. The new nativeIceFragilityWorksBeforeDeepFreeze regression is written but has not run inside Minecraft.

Local Gradle compilation could not start: the Gradle 8.8 distribution is absent and services.gradle.org is unreachable from this environment. No Actions runs were requested. Full Forge compilation, native integration checks and final JAR delivery remain pending.
