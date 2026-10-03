#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"
class_trees = (SRC / "classes/ClassTrees.java").read_text(encoding="utf-8")

# Progression-RPG contract: the full build must not exist at level 1.
for token in (
    '"_core_awakening"', '1, 1, false',
    '"_core_mastery"', '10, 6, false',
    '"_foundation"', '10, 1, true',
    '"_signature"', '19, 6, false',
    '"_mastery"', '22, 7, false',
    '"_initiation"', '25, 1, false',
    '"_engine"', '30, 2, false',
    '"_technique"', '35, 3, false',
    '"_conversion"', '40, 4, false',
    '"_risk"', '45, 5, false',
    '"_signature"', '48, 6, false',
    '"_ascension"', '50, 7, false',
):
    assert token in class_trees, f"progression band contract changed: {token}"

print("RPG progression curve validation OK")
