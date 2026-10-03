# Cosmic class selection artwork

`cosmic_atlas.png` is a 1672 x 941, text-free PNG derived with the built-in image-generation editor from the user's supplied class-selection reference (2026-09-29). The edit removed written labels while preserving the character art, frames, icons and layout. The source reference is user-provided; the runtime asset is bundled locally and makes no network calls.

Edit prompt: remove every written word, letter and number, seamlessly restore the dark areas, and preserve all non-text art, positions, colors, poses, frames, icons, five House diamonds, and the 16:9 composition. Do not invent or simplify elements.

One atlas keeps the four original artworks visually consistent. `ClassSelectionView` draws the surroundings, and each `ClassCardWidget` draws its own region independently. All class names, descriptions, resources, House counts, tags, selection states and confirmation text are rendered live. No baked-in labels or invisible click targets are used.

Source regions (x, y, width, height):

- Warrior: 125, 177, 703, 325
- Mage: 840, 177, 709, 325
- Archer: 125, 512, 703, 320
- Assassin: 840, 512, 709, 320
- Confirmation: 1230, 840, 400, 70

The viewport fits the complete composition without changing its aspect ratio. Widget bounds are projected into Minecraft GUI coordinates; hit testing and keyboard focus remain native. This intentionally uses uniform scaling rather than stretching the illustrated frames independently.

Validation: run `python tools/run_ui_tests.py`, then the existing Forge workflow. Visual acceptance additionally requires opening the awakened, classless player's Stats screen at 1280x720 and 1920x1080, changing GUI scale, checking all four selections and House tooltips, confirming once, and reopening after server synchronization.
