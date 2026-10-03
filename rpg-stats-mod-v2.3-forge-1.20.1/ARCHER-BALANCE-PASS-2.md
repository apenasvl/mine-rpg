# Archer balance pass 2 — Aim, Focus and Instinct

## Goal

Close the three tactical debts found in pass 1 without increasing Archer's permanent raw-damage budget. The Archer should become stronger by maintaining positioning and spending class state correctly, not by receiving another unconditional damage multiplier.

## Aim

Marksman Aim is no longer permanent. It decays every tick:

- slowly while the player is in combat;
- faster outside combat;
- Focus can stabilize the decay for a limited time.

This makes high Aim something the player maintains instead of something that can be stacked once and carried forever.

## Focus

`arc_focus` is no longer dead state. It now acts as a tactical stabilizer for Aim:

- while Aim is active, Focus reduces Aim decay by 55%;
- maintaining this stabilization continuously drains Focus;
- if combat has ended and there is no Aim left to stabilize, Focus itself decays.

Focus therefore improves consistency rather than becoming another flat damage stat. Kills and the Archer core utility already generate it, so those existing sources now have a real payoff.

## Wild Warden Instinct

`arc_instinct` now has an explicit defensive spend. Whenever an Archer opens an `arc_survival` window, up to 2 Instinct are consumed and extend that protection by up to 20 ticks (1 second).

This gives Wild Warden a different power budget from Marksman:

- Marksman spends positioning and Aim for safer ranged pressure;
- Wild Warden builds Instinct and converts it into survival time;
- Skirmisher still owns Momentum/mobility;
- Arcane Archer owns elemental setup;
- Artificer owns device windows.

## Balance guardrails

This pass does not change the +27% persistent Archer progression from pass 1 and does not raise the global class-mechanic damage cap. It changes uptime/consistency, not the raw maximum hit.

The next Archer pass should review all 15 Archer specializations and their burst/AoE budgets before moving to Assassin.

## Testing policy

Static contracts and the Forge build validate the wiring and numeric budgets. Runtime Minecraft testing is intentionally deferred until several class blocks are complete.
