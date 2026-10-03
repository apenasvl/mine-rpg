# Mage character menu artwork

Five Houses use the user's exact frame/accent colors: Elemental #F3A58E,
Arcana #B9ACF2, Conjuration #91D6BE, Occult #D89ABF, Temporal #8FCEEE.
Each 1254x1254 PNG contains four 627x627 cells: House at upper left,
then its three specializations in reading order. `MageTheme` owns the mapping.
Specializations inherit their House frame color and have distinct artwork.
The generic unassigned Mage uses the Arcana emblem; no House is assigned by UI.

`frame.png` is grayscale, tinted at draw time with the House color and sliced
at 0/256/998/1254. This keeps corners proportionate on headers, sidebars and
buttons. Resource packs may replace these files, preserving dimensions/regions.
No labels, costs, levels or progress are baked into the PNGs.

Generated with the built-in image generation tool for this project. Prompt:
four equally sized cells in a 2x2 square atlas, luminous etched fantasy magical
emblems on near-black background, fine silver linework tinted by the House
palette, restrained glow, no text or UI. Subjects in reading order:

- elemental: elemental triad / phoenix flame / glacial crown / lightning vortex
- arcana: rune circles / engraved runic seal / fractured mirror / spatial portal
- conjuration: summoning gate / spectral familiar / ancestral spirit / astral forge
- occult: thorned ritual seal / blood ritual / cursed talisman / ritual sword
- temporal: clock/hourglass / accelerated orbits / crystallized stasis / reversing spiral

Frame prompt: grayscale silver crystalline filigree on a near-black square panel,
straight double ruled edges, ornate corners, plain center, no text, orthographic.

Mage pages render in an 820x470 logical viewport fitted uniformly to the window.
Mouse events use its inverse transform. Other classes retain their existing UI.
Gameplay, node IDs, server validation and save data remain unchanged.

Validation: `python3 tools/run_mage_ui_tests.py`; real Forge screenshot probe:
`bash gradlew -I tools/mage-preview.init.gradle runClient` (requires a display).
Probe sources are opt-in and excluded from normal distributable JARs.
