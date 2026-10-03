# Assassin balance pass 1 — Combo economy and burst preparation

## Goal

Make Assassin burst depend on executing the class loop instead of adding a large permanent damage multiplier. The intended hierarchy remains: a correctly prepared Assassin may approach the Warrior's committed physical hit during a short opening, but it must not sustain that output.

## Combo as the shared burst bridge

`ass_combo` already existed and the Risk node already checked for 3+ Combo, but most Assassin Houses did not feed it consistently. The following real mechanics now contribute a fraction of their gain to Combo:

- Advantage: 35% of gauge gain;
- Soul Echo: 30%;
- Preparation: 25%;
- Blade Dance: 20%;
- kill momentum (`ass_energy_loop`): 50%.

An actual `ass_opening` starting from zero also grants 0.55 Combo. Passive Shadow preparation itself does not feed Combo, so waiting safely outside combat cannot pre-stock the full burst.

## No permanent pre-stock

When combat ends, Assassin combat gauges decay:

- Combo: 0.018/tick;
- Advantage: 0.012/tick;
- Echo: 0.010/tick;
- Preparation: 0.014/tick;
- Dance: 0.015/tick;
- kill momentum: 0.025/tick.

Taking damage still removes 1.5 Combo through the existing class mechanic. This preserves the high-risk identity: maintaining a burst chain requires not getting interrupted.

## What this intentionally does not do

- no Archer-style permanent Assassin damage multiplier;
- no new unconditional critical chance;
- no increase to the global class-mechanic damage cap;
- no change to Mago/Iron's, bosses, XP or other classes.

The next Assassin pass should review missing specialization verbs, especially Demolitionist stored damage/release and the Mystic/Saboteur variants, using bounded proc/AoE budgets rather than stacking another raw multiplier.

## Testing policy

Static contracts plus the Forge build validate wiring and numeric budgets. Runtime Minecraft testing remains deferred until several class blocks are complete.
