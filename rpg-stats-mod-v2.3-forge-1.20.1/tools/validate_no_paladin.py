#!/usr/bin/env python3
"""Contract for the four-class RPG after removing Paladin."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"
RES = ROOT / "src/main/resources/assets/rpgstats/lang"
CURRENT_DOCS = (
    ROOT / "README.md",
    ROOT / "CROSS-CLASS-LEVEL-AUDIT.md",
    ROOT / "CLASS-IDENTITY-DESIGN.md",
    ROOT / "CLASS-ABILITY-REWORK-AUDIT.md",
)

TOKENS = ("PALADINO", "PAL_", "pal_", "Paladino", "paladino")
errors = []

for path in SRC.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    for token in TOKENS:
        if token in text:
            errors.append(f"{path.relative_to(ROOT)} still contains {token}")

for path in RES.glob("*.json"):
    text = path.read_text(encoding="utf-8")
    for token in ("pal_", "Paladino", "paladin"):
        if token in text:
            errors.append(f"{path.relative_to(ROOT)} still exposes Paladin content: {token}")

for path in CURRENT_DOCS:
    if not path.exists():
        continue
    text = path.read_text(encoding="utf-8")
    if "Paladino" in text or "PALADINO" in text:
        errors.append(f"{path.relative_to(ROOT)} still documents Paladin as a current class")

if errors:
    print("Paladin removal validation FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Paladin removal validation OK")
