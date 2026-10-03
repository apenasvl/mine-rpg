# Archer balance pass 3 — specialization identity and AoE budgets

## Goal

Finish the first real review of all 15 Archer specializations. Existing mechanics that already have a clear gameplay verb stay in `ClassMechanics`; specializations that were mostly a timer/gauge or tooltip receive a bounded projectile-event mechanic without raising Archer's permanent single-target damage budget.

## Marksman

- **Sniper** — already functional: stationary setup, long-range condition and prepared-shot window. No extra proc added.
- **Deadeye** — already functional as consecutive-target precision. No second critical layer was added because that would double-dip physical critical scaling.
- **Ballistician** — gained actual piercing identity: every third projectile hit can damage one valid target geometrically behind the first. Secondary hit is 30% of the triggering hit, capped at 4.5, and never re-hits the original target.

## Wild Warden

- **Beastmaster** — existing pet orders/buffs remain the identity.
- **Trapper** — existing prepared slow/control remains; bosses already receive reduced control duration.
- **Survivalist** — existing defensive window remains and pass 2 now lets Instinct extend that survival window.

## Skirmisher

- **Windrunner** — Momentum and movement remain its engine.
- **Acrobat** — airborne projectile hits now recover 0.35 Focus, with an 8-tick internal cooldown so multishot cannot refill the resource instantly.
- **Guerrilla** — attack/reposition/ambush window remains its engine.

## Arcane Archer

- **Flamebow** — during the elemental reaction window, burning can propagate to up to four nearby hostile targets. This is DoT/area pressure rather than another direct-damage multiplier.
- **Frostbow** — during the elemental reaction window, the target receives stronger control; bosses keep the weaker version.
- **Stormbow** — every third projectile hit can chain to one nearby hostile target for 28% of the triggering hit, capped at 4.5, with an internal cooldown.

## Artificer

- **Crossbow Expert** — keeps its prepared burst/crossbow-ready engine from `ClassMechanics`. Weapon-tag enforcement remains part of the later external-weapon compatibility pass.
- **Bombardier** — an armed bomb shot now deals bounded AoE damage to up to three secondary targets. The original target never receives an extra hit; each secondary hit is 25% of the trigger, capped at 4.0, with multishot protection.
- **Engineer** — device-zone shots pulse Slowness + Weakness around the impact area instead of gaining more raw damage. Boss durations are reduced.

## Hierarchy guardrail

This pass deliberately avoids adding extra single-target hits to the same victim. Warrior remains the intended highest committed physical single-hit reference. Archer gains value through range, piercing, chaining, DoT propagation and control.

The +27% persistent Archer progression and the existing class-mechanic multiplier cap remain unchanged.

## Next class block

With Archer's persistent curve, tactical gauges and specialization verbs covered, the next balance block should be **Assassin**: prepared burst may approach the Warrior benchmark, but sustained output must remain below that peak and must depend on Opening, Doses, Advantage/Parry, Preparation or Soul Echoes.

## Testing policy

CI/static contracts and the Forge build verify compilation and the intended budgets. Runtime Minecraft testing is intentionally deferred until several class blocks are complete.
