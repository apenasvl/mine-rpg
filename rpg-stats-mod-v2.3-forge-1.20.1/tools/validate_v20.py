#!/usr/bin/env python3
"""Auditoria estática v2.0: cobertura de nodes, ativas e regras de árvore."""
from pathlib import Path
import re
import runpy
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"
errors = []

def read(relative):
    return (ROOT / relative).read_text(encoding="utf-8")

# Preserva a validação estrutural já existente, com expectativas da versão anterior.
try:
    runpy.run_path(str(Path(__file__).with_name("validate_v19.py")), run_name="__main__")
except SystemExit as failure:
    if failure.code not in (0, None):
        errors.append("validação estrutural v1.9 falhou")

manager = read("src/main/java/com/rpgstats/stats/StatsManager.java")
ui = read("src/main/java/com/rpgstats/gui/StatsScreen.java")
player = read("src/main/java/com/rpgstats/stats/PlayerStats.java")
registry = read("src/main/java/com/rpgstats/ability/AbilityRegistry.java")
mage_registry = read("src/main/java/com/rpgstats/ability/MageAbilityRegistry.java")
mage_handler = read("src/main/java/com/rpgstats/combat/MageCombatHandler.java")
combat = read("src/main/java/com/rpgstats/combat/CombatHandler.java")

for marker in [
    "SECONDARY_PATH_LEVEL = 32", "SECONDARY_PATH_MAX_DEPTH = 3",
    "selectSecondaryPath", "Sua classe principal só pode aprender habilidades da sua Casa escolhida.",
    "depthOf(realId) > 5",
]:
    if marker not in manager: errors.append("regra de multiclasse/Casa ausente: " + marker)
for marker in ["DATA_VERSION = 8", "secondaryPath", "lastUnlockedNode", "oldVersion < 8"]:
    if marker not in player: errors.append("persistência v2.0 ausente: " + marker)
for marker in ["Última compra:", "Passivas secundárias: 65%", "visual.rect.x", "secondaryPathNodes"]:
    if marker not in ui: errors.append("HUD de detalhes persistentes incompleta: " + marker)
for marker in ["secondaryEffect", "effect.value() * 0.65f"]:
    if marker not in registry: errors.append("escala de passivas secundárias ausente: " + marker)

# Todas as ativas de Mago declaradas no registry precisam de um case executável.
mage_active_ids = re.findall(r'put\("(mag_[^"]+)",\s*SkillEffect\.active', mage_registry)
missing_mage = [node for node in mage_active_ids if ('case "' + node + '"') not in mage_handler]
if missing_mage: errors.append("ativas de Mago sem executor: " + ", ".join(missing_mage))

# Cada ativa genérica que a árvore pode produzir é tratada no executor comum.
active_types = set(re.findall(r'return "(active_[a-z_]+)";', read("src/main/java/com/rpgstats/ability/ClassAbilityRegistry.java")))
active_types.update(re.findall(r'case "(active_[a-z_]+)"', registry))
implemented = set(re.findall(r'"(active_[a-z_]+)"', "\n".join(
    line for line in combat.splitlines() if "case" in line and "active_" in line
)))
missing_generic = sorted(active_types - implemented)
if missing_generic: errors.append("ativas genéricas sem executor: " + ", ".join(missing_generic))

# Nodes de Mago sem SkillEffect ainda devem participar explicitamente de uma mecânica ou ser gate de progressão.
empty_nodes = re.findall(r'put\("(mag_[^"]+)"\);', mage_registry)
allowed_gates = {"mag_core_mastery", "mag_time_sensitivity"}
unwired = [node for node in empty_nodes if node not in allowed_gates and node not in mage_handler]
if unwired: errors.append("nodes de Mago sem efeito ou mecânica: " + ", ".join(unwired))

print("Ativas de Mago auditadas:", len(mage_active_ids))
print("Ativas genéricas auditadas:", len(active_types))
print("Nodes mecânicos do Mago auditados:", len(empty_nodes))
if errors:
    print("\nFALHAS v2.0:")
    for error in errors: print(" -", error)
    sys.exit(1)
print("\nAUDITORIA DE HABILIDADES v2.0: OK")
