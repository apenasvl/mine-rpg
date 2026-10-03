# Assassin balance pass 3 — prepared burst closure

This pass closes the Assassin's shared burst loop without turning Combo into passive always-on damage.

## Opening is a real opening

`ass_opening` and the per-target Shadow opening now behave as first-hit states. The hit still reaches the normal ClassMechanics pipeline and specialization hooks, then the opening is cleared. Nightblade and other opening-dependent effects therefore get their intended first strike instead of carrying the same bonus through a long sequence.

## Combo is spent

A prepared melee hit can spend Combo only when the Assassin has at least 3 Combo and one of the real burst conditions is active: Opening, Riposte, Soul Strike, Execute Window or Void Debt.

The spend is capped at 1.5 Combo and has a short internal lock. Before the late `Risk` node, the universal reward is Energy/resource recovery (70% of the Combo spent), not another free raw-damage multiplier. At level 45+, the existing Risk node can still grant its conditional +3.5% inside the normal ClassMechanics cap, and that same hit then spends Combo so the bonus cannot be maintained indefinitely.

## Why this fits the progression RPG curve

- Levels 1–9 do not have House mechanics.
- House gameplay begins at 10 and teaches preparation/positioning.
- Specialization comes online from 25 onward.
- Stronger specialist procs from pass 2 require Engine (30) and Conversion (40) where appropriate.
- Risk damage is a level-45 investment.
- Level 50 Ascension remains the completed-build state.

The early Assassin is therefore fragile and incomplete. The endgame Assassin earns short, repeatable burst windows through setup, but Warrior remains the reference for the hardest committed physical hit.

## Specialization status after passes 1–3

Shadow: Nightblade/Phantom/Executioner already have opening, phase and execute conditions. Venom: Alchemist/Plaguebringer/Toxicologist are differentiated by formula, spread and control. Duelist: Fencer/Blade Dancer/Counterblade use tempo, target alternation and parry/riposte. Saboteur: Demolitionist now has bounded stored-damage release, Infiltrator uses entry/escape and Wiremaster converts control into debuff. Mystic: Hexkiller keeps its hunt condition, Soulknife now has a capped real echo cut and Voidwalker uses blink/debt risk.

No Paladin changes are included; Paladin stays last.
