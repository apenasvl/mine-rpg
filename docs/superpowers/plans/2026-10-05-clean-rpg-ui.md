# Clean RPG UI implementation
Approved specification: graphite panels, ivory text, class accents; compact choices with one selected detail sheet; explicit confirmation; inspectable talents. Preserve four classes, level/mastery requirements, network actions and combat balance.

## Task 1: Awakening and class selection
Files: gui/ClassSelectionLayout, ClassCardWidget, ClassSelectionView, AwakeningView, CleanRpgUi.
Keep ClassSelectionState pending/timeout behavior. Four horizontal previews, detail region below, independent confirm hitbox. Check layout at seven GUI sizes and actual client selection before confirmation.

## Task 2: Character progression
Files: gui/StatsScreen, RpgButton. Keep 820x470 viewport/input scaling; replace ornate raster panels with thin flat frames. Five House emblems / three specialization choices / four secondary Houses above a selected detail sheet. Preview must not transmit; confirmation eligibility remains level/mastery restricted; pending prevents duplicates. Show actual talent/effect data and affinity penaltyText.

## Task 3: Talent inspection and verification
Persist clicked node details across mouse motion and server rebuild. Scroll full description/effects in fixed sidebar, preserve learn/equip callbacks. Client probe screenshots awakening, all four class previews, House/spec/affinity, learned/locked talent and resizing. Run existing UI tests, Gradle compilation and existing CI; review branch; publish tested JAR.

## Review focus
Hitboxes match scaled render; no immediate permanent choice on preview; pending retry; no misleading invented bonuses; long Portuguese descriptions readable; optional branches safe; balance code untouched.
