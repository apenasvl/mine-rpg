#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"
errors = []

def text(rel): return (ROOT / rel).read_text(encoding="utf-8")
def enum_count(rel):
    body = text(rel).split("public final", 1)[0]
    return len(re.findall(r"^\s{4}[A-Z][A-Z0-9_]+\(", body, re.M))

mage = text("src/main/java/com/rpgstats/classes/MageTrees.java")
mage_ids = re.findall(r'\bn\("([^"]+)"', mage)

paths_src = text("src/main/java/com/rpgstats/classes/RPGPath.java")
path_prefixes = re.findall(r'ClassTrees\.path\("([^"]+)"', paths_src)
specs_src = text("src/main/java/com/rpgstats/classes/RPGSpecialization.java")
spec_prefixes = re.findall(r'ClassTrees\.spec\("([^"]+)"', specs_src)

core_prefixes = ["war", "arc", "ass", "pal"]
core_suffixes = ["awakening", "flow", "form", "guard", "efficiency", "tempo",
                 "resolve", "combo", "utility", "power", "sustain", "mastery"]
path_suffixes = ["foundation", "discipline", "setup", "technique", "reaction",
                 "synergy", "economy", "signature", "mastery"]
spec_suffixes = ["initiation", "engine", "technique", "conversion", "risk", "signature", "ascension"]

generated = [f"{p}_core_{s}" for p in core_prefixes for s in core_suffixes]
generated += [f"{p}_{s}" for p in path_prefixes for s in path_suffixes]
generated += [f"{p}_{s}" for p in spec_prefixes for s in spec_suffixes]
all_ids = mage_ids + generated

checks = {
    "classes": enum_count("src/main/java/com/rpgstats/classes/RPGClass.java"),
    "paths": enum_count("src/main/java/com/rpgstats/classes/RPGPath.java"),
    "specializations": enum_count("src/main/java/com/rpgstats/classes/RPGSpecialization.java"),
    "mage_nodes": len(mage_ids),
    "other_nodes": len(generated),
    "total_nodes": len(all_ids),
    "other_actives": len(core_prefixes) + len(path_prefixes) * 2 + len(spec_prefixes) * 3,
}
expected = {"classes":5, "paths":25, "specializations":75, "mage_nodes":162,
            "other_nodes":648, "total_nodes":810, "other_actives":224}
for key, value in expected.items():
    if checks[key] != value: errors.append(f"{key}: esperado {value}, encontrado {checks[key]}")

duplicates = sorted({x for x in all_ids if all_ids.count(x) > 1})
if duplicates: errors.append("IDs duplicados: " + ", ".join(duplicates))

if "return ClassAbilityRegistry.get(nodeId);" not in text("src/main/java/com/rpgstats/ability/AbilityRegistry.java"):
    errors.append("ClassAbilityRegistry não está ligado ao registry principal")
if "mod_version=1.7.0-alpha.2" not in text("gradle.properties"):
    errors.append("versão Gradle incorreta")
if "loader_version=0.19.5" not in text("gradle.properties"):
    errors.append("Fabric Loader incorreto")

# Sanidade estrutural simples em todos os Java; strings são removidas antes da contagem.
for java in SRC.rglob("*.java"):
    source = java.read_text(encoding="utf-8")
    stripped = re.sub(r'"(?:\\.|[^"\\])*"', '""', source)
    stripped = re.sub(r'/\*.*?\*/', '', stripped, flags=re.S)
    stripped = re.sub(r'//.*', '', stripped)
    for opening, closing in [("{", "}"), ("(", ")"), ("[", "]")]:
        if stripped.count(opening) != stripped.count(closing):
            errors.append(f"delimitadores em {java.relative_to(ROOT)}: {opening}/{closing}")

print("Classes:", checks["classes"])
print("Caminhos:", checks["paths"])
print("Especializações / Ascensões:", checks["specializations"])
print("Nodes Mago:", checks["mage_nodes"])
print("Nodes outras classes:", checks["other_nodes"])
print("Nodes totais:", checks["total_nodes"])
print("Ativas outras classes:", checks["other_actives"])
print("Ativas totais esperadas: 295")
if errors:
    print("\nFALHAS:")
    for error in errors: print(" -", error)
    sys.exit(1)
print("\nVALIDAÇÃO ESTÁTICA v1.7: OK")
