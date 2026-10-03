#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
trees = (ROOT / "src/main/java/com/rpgstats/classes/ClassTrees.java").read_text(encoding="utf-8")
audit = (ROOT / "CROSS-CLASS-LEVEL-AUDIT.md").read_text(encoding="utf-8")
summary = (ROOT / "WARRIOR-BALANCE-PASS.md").read_text(encoding="utf-8")
summary_flat = " ".join(summary.split())

for gate in (
    '10, 1, true',
    '25, 1, false',
    '30, 2, false',
    '35, 3, false',
    '40, 4, false',
    '45, 5, false',
    '48, 6, false',
    '50, 7, false',
):
    assert gate in trees, f"progression gate missing: {gate}"

for level in ("| 1 |", "| 10 |", "| 25 |", "| 40 |", "| 50 |"):
    assert level in audit, f"cross-class checkpoint missing: {level}"

for role in (
    "maior pancada física direta",
    "burst preparado",
    "alcance/uptime/precisão/controle",
    "versatilidade/AoE/controle",
):
    assert role in audit

assert "Paladino" not in audit and "PALADINO" not in audit
assert "Runtime Minecraft continua pendente" in summary_flat
print("Cross-class level 1/10/25/40/50 audit validation OK")
