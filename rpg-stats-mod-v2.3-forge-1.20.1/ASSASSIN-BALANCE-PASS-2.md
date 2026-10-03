# Assassin balance pass 2 — specialization payoff

This pass makes the weaker late-game Assassin specializations do something tangible without adding a permanent raw-damage multiplier.

- Demolitionist stores pressure during `ass_demolition`; once Engine is unlocked it can release a bounded detonation. Conversion later adds controlled splash.
- Wiremaster turns an armed device into control/debuff rather than another damage multiplier.
- Soulknife converts prepared echoes into a small capped echo cut during `ass_soul_strike`; it is gated behind Engine and an internal cooldown.
- These effects respect the RPG progression curve: specialization Initiation begins at 25, Engine at 30 and Conversion at 40. The early game does not receive these endgame payoffs.
- Single-target extra procs stay small and conditional so prepared Assassin burst can approach Warrior, but normal Assassin DPS should not become the physical benchmark.

No Paladin work is included. Paladin stays last in the class-balance order.
