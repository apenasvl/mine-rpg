#!/usr/bin/env python3
"""Validação estática do RPGStats v1.5 sem depender das classes do Minecraft/Fabric."""
from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/rpgstats"


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def duplicates(values):
    seen, dup = set(), set()
    for value in values:
        if value in seen:
            dup.add(value)
        seen.add(value)
    return sorted(dup)


def strip_java_noise(text):
    """Remove strings/chars/comentários para checagem simples de delimitadores."""
    out = []
    i = 0
    state = "code"
    while i < len(text):
        c = text[i]
        n = text[i + 1] if i + 1 < len(text) else ""
        if state == "code":
            if c == '"': state = "string"; out.append(' ')
            elif c == "'": state = "char"; out.append(' ')
            elif c == '/' and n == '/': state = "line"; out.extend('  '); i += 1
            elif c == '/' and n == '*': state = "block"; out.extend('  '); i += 1
            else: out.append(c)
        elif state == "string":
            if c == '\\': out.extend('  '); i += 1
            elif c == '"': state = "code"; out.append(' ')
            else: out.append(' ')
        elif state == "char":
            if c == '\\': out.extend('  '); i += 1
            elif c == "'": state = "code"; out.append(' ')
            else: out.append(' ')
        elif state == "line":
            if c == '\n': state = "code"; out.append('\n')
            else: out.append(' ')
        elif state == "block":
            if c == '*' and n == '/': state = "code"; out.extend('  '); i += 1
            else: out.append('\n' if c == '\n' else ' ')
        i += 1
    return ''.join(out)


def balanced_delimiters(path):
    text = strip_java_noise(path.read_text(encoding="utf-8"))
    pairs = {'}': '{', ')': '(', ']': '['}
    openings = set(pairs.values())
    stack = []
    line = 1
    for c in text:
        if c == '\n':
            line += 1
        elif c in openings:
            stack.append((c, line))
        elif c in pairs:
            if not stack or stack[-1][0] != pairs[c]:
                return False, f"fechamento {c} inválido na linha {line}"
            stack.pop()
    if stack:
        return False, f"delimitador {stack[-1][0]} aberto na linha {stack[-1][1]}"
    return True, ""


errors = []
warnings = []

# ---------- Nodes / registries ----------
rpg_class = read("src/main/java/com/rpgstats/classes/RPGClass.java")
rpg_subclass = read("src/main/java/com/rpgstats/classes/RPGSubclass.java")
mage_trees = read("src/main/java/com/rpgstats/classes/MageTrees.java")
base_registry = read("src/main/java/com/rpgstats/ability/AbilityRegistry.java")
mage_registry = read("src/main/java/com/rpgstats/ability/MageAbilityRegistry.java")
mage_handler = read("src/main/java/com/rpgstats/combat/MageCombatHandler.java")

base_nodes = re.findall(r'\bnode\("([^"]+)"', rpg_class + "\n" + rpg_subclass)
mage_nodes = re.findall(r'\bn\("([^"]+)"', mage_trees)
all_nodes = base_nodes + mage_nodes

# n(id, name, desc, cost, prereq, level, tier, cross) — formato fixo de MageTrees.
mage_record_pattern = re.compile(
    r'n\("([^"]+)",\s*"([^"]+)",\s*"([^"]*)",\s*(\d+),\s*(null|"[^"]+"),\s*(\d+),\s*(\d+),\s*(true|false)\)'
)
mage_records = {}
for match in mage_record_pattern.finditer(mage_trees):
    node_id, name, desc, cost, prereq, level, tier, cross = match.groups()
    mage_records[node_id] = {
        "name": name, "desc": desc, "cost": int(cost),
        "prereq": None if prereq == "null" else prereq.strip('\"'),
        "level": int(level), "tier": int(tier), "cross": cross == "true",
    }

base_registry_ids = re.findall(r'\bput\("([^"]+)"', base_registry)
mage_registry_ids = re.findall(r'\bput\("([^"]+)"', mage_registry)
all_registry_ids = base_registry_ids + mage_registry_ids

for label, values in [
    ("nodes", all_nodes), ("registry", all_registry_ids),
    ("Mage nodes", mage_nodes), ("Mage registry", mage_registry_ids),
]:
    dup = duplicates(values)
    if dup:
        errors.append(f"{label} duplicados: {dup}")

if len(base_nodes) != 80:
    errors.append(f"Esperados 80 nodes legados não-Mago; encontrados {len(base_nodes)}")
if len(mage_nodes) != 162:
    errors.append(f"Esperados 162 nodes do Mago; encontrados {len(mage_nodes)}")
if len(all_nodes) != 242:
    errors.append(f"Esperados 242 nodes totais; encontrados {len(all_nodes)}")
if len(mage_registry_ids) != 162:
    errors.append(f"Esperadas 162 entradas MageAbilityRegistry; encontradas {len(mage_registry_ids)}")
if len(all_registry_ids) != 242:
    errors.append(f"Esperadas 242 entradas de registry; encontradas {len(all_registry_ids)}")

missing_registry = sorted(set(all_nodes) - set(all_registry_ids))
orphan_registry = sorted(set(all_registry_ids) - set(all_nodes))
if missing_registry:
    errors.append(f"Nodes sem registry: {missing_registry}")
if orphan_registry:
    errors.append(f"Registry sem node: {orphan_registry}")

# ---------- Estrutura Mago ----------
core_block = re.search(r'public static List<SkillNode> core\(\) \{(.*?)\n    \}', mage_trees, re.S)
if not core_block or len(re.findall(r'\bn\("', core_block.group(1))) != 12:
    errors.append("Núcleo do Mago deve ter exatamente 12 nodes")

path_methods = ["pathElemental", "pathArcana", "pathConjuration", "pathOccult", "pathTemporal"]
for method in path_methods:
    m = re.search(rf'public static List<SkillNode> {method}\(\) \{{(.*?)\n    \}}', mage_trees, re.S)
    if not m:
        errors.append(f"Método de Casa ausente: {method}")
        continue
    block = m.group(1)
    count = len(re.findall(r'\bn\("', block))
    cross = len(re.findall(r',\s*true\)\s*,?', block))
    if count != 9:
        errors.append(f"{method}: esperado 9 nodes, encontrado {count}")
    if cross != 3:
        errors.append(f"{method}: esperado 3 nodes cross-house, encontrado {cross}")

spec_methods = re.findall(r'public static List<SkillNode> (spec\w+)\(\)', mage_trees)
if len(spec_methods) != 15:
    errors.append(f"Esperadas 15 especializações; encontrados {len(spec_methods)} métodos")
for method in spec_methods:
    m = re.search(rf'public static List<SkillNode> {method}\(\) \{{(.*?)\n    \}}', mage_trees, re.S)
    if not m:
        errors.append(f"Bloco de especialização ausente: {method}")
        continue
    count = len(re.findall(r'\bn\("', m.group(1)))
    if count != 7:
        errors.append(f"{method}: esperado 7 nodes incluindo Ascensão, encontrado {count}")

asc_lines = [line for line in mage_trees.splitlines() if '_asc_' in line and 'n("' in line]
if len(asc_lines) != 15:
    errors.append(f"Esperadas 15 Ascensões; encontradas {len(asc_lines)}")
for line in asc_lines:
    if not re.search(r',\s*3,\s*"[^\"]+",\s*50,\s*7,\s*false\)\s*[,)]?\s*$', line.strip()):
        errors.append("Ascensão fora do padrão custo=3, level=50, tier=7: " + line.strip())

# ---------- Reachability / budget sanity ----------
if len(mage_records) != 162:
    errors.append(f"Parser estrutural do MageTrees esperava 162 records; encontrou {len(mage_records)}")

for node_id, rec in mage_records.items():
    if rec["cost"] < 1 or rec["cost"] > 3:
        errors.append(f"Custo fora da faixa 1–3 em {node_id}: {rec['cost']}")
    if rec["level"] < 1 or rec["level"] > 50:
        errors.append(f"Level requirement fora de 1–50 em {node_id}: {rec['level']}")
    prereq = rec["prereq"]
    if prereq is not None and prereq not in mage_records:
        errors.append(f"Pré-requisito inexistente em {node_id}: {prereq}")

def chain_to(node_id):
    chain, seen = [], set()
    current = node_id
    while current is not None:
        if current in seen:
            errors.append(f"Ciclo de pré-requisito detectado em {node_id}: {current}")
            return []
        seen.add(current)
        chain.append(current)
        rec = mage_records.get(current)
        if rec is None:
            return []
        current = rec["prereq"]
    return list(reversed(chain))

def chain_cost(node_id):
    return sum(mage_records[x]["cost"] for x in chain_to(node_id))

core_mastery_cost = chain_cost("mag_core_mastery")
if core_mastery_cost > 10:
    errors.append(f"Maestria Arcana custa {core_mastery_cost} PH mínimos e não cabe naturalmente até o nível 10")

path_masteries = {
    "elemental": "mag_ele_mastery",
    "arcana": "mag_arc_high_magic",
    "conjuration": "mag_conj_mastery",
    "occult": "mag_occ_ritual",
    "temporal": "mag_time_mastery",
}
for label, mastery in path_masteries.items():
    total = core_mastery_cost + chain_cost(mastery)
    if total > 25:
        errors.append(f"Casa {label} exige {total} PH mínimos antes da especialização; ultrapassa orçamento natural do nível 25")

spec_to_mastery = {
    "pyr": "mag_ele_mastery", "cryo": "mag_ele_mastery", "storm": "mag_ele_mastery",
    "rune": "mag_arc_high_magic", "illu": "mag_arc_high_magic", "tele": "mag_arc_high_magic",
    "summ": "mag_conj_mastery", "anim": "mag_conj_mastery", "astral": "mag_conj_mastery",
    "blood": "mag_occ_ritual", "curse": "mag_occ_ritual", "hex": "mag_occ_ritual",
    "acc": "mag_time_mastery", "stag": "mag_time_mastery", "rev": "mag_time_mastery",
}
for asc in [node for node in mage_records if "_asc_" in node]:
    spec_prefix = asc.split("_")[1]
    mastery = spec_to_mastery.get(spec_prefix)
    if mastery is None:
        errors.append(f"Ascensão sem Casa mapeada no validator: {asc}")
        continue
    total = core_mastery_cost + chain_cost(mastery) + chain_cost(asc)
    if total > 50:
        errors.append(f"Ascensão inalcançável com 50 PH naturais: {asc} exige {total}")

for node_id, rec in mage_records.items():
    if rec["cross"] and (rec["tier"] > 3 or rec["cost"] != 1):
        errors.append(f"Node cross-house fora da regra tier<=3/custo1: {node_id}")

# ---------- Ativas ----------
active_mage_ids = re.findall(r'put\("([^"]+)"[^\n]*SkillEffect\.active\(', mage_registry)
active_cases = set(re.findall(r'case\s+"(mag_[^"]+)"\s*->', mage_handler))
missing_active_cases = sorted(set(active_mage_ids) - active_cases)
if missing_active_cases:
    errors.append(f"Ativas do Mago sem case no MageCombatHandler: {missing_active_cases}")
if len(active_mage_ids) != 71:
    errors.append(f"Esperadas 71 ativas do Mago; encontradas {len(active_mage_ids)}")

# ---------- Nodes vazios devem ter algum consumidor estrutural ----------
empty_mage_registry = re.findall(r'put\("([^"]+)"\);', mage_registry)
consumer_text = "\n".join(
    p.read_text(encoding="utf-8")
    for p in JAVA.rglob("*.java")
    if p.name not in {"MageTrees.java", "MageAbilityRegistry.java"}
)
# Maestrias/roots podem ser puro requisito; Sensibilidade Temporal agora controla fragmentMax.
unused_empty = [node for node in empty_mage_registry if node not in consumer_text]
if unused_empty:
    errors.append(f"Nodes Mage sem SkillEffect e sem consumidor estrutural: {unused_empty}")

# ---------- Regras críticas ----------
stats_manager = read("src/main/java/com/rpgstats/stats/StatsManager.java")
player_stats = read("src/main/java/com/rpgstats/stats/PlayerStats.java")
client = read("src/main/java/com/rpgstats/RPGStatsClient.java")
mod = read("src/main/java/com/rpgstats/RPGStatsMod.java")

required_constants = {
    "MAX_LEVEL = 50": stats_manager,
    "SKILL_POINTS_PER_LEVEL = 1": stats_manager,
    "PATH_LEVEL = 10": stats_manager,
    "SPECIALIZATION_LEVEL = 25": stats_manager,
    "CROSS_PATH_LEVEL = 30": stats_manager,
    "MAX_CROSS_PATH_NODES = 6": stats_manager,
    "MAX_CROSS_NODES_PER_PATH = 3": stats_manager,
    "ACTIVE_SLOTS = 4": player_stats,
}
for pattern, source in required_constants.items():
    if pattern not in source:
        errors.append(f"Constante/regra esperada ausente: {pattern}")

if 'stats.skillPoints = 1' not in stats_manager:
    errors.append("PH inicial do nível 1 não encontrado em selectClass")
if 'ServerLivingEntityEvents.AFTER_DEATH' not in mod:
    errors.append("XP de kill não está registrado em AFTER_DEATH")
if 'ALLOW_DEATH.register' in mod:
    errors.append("Hook obsoleto ALLOW_DEATH ainda presente")

stale_patterns = {
    "secondarySubclass": "modelo antigo de subclasse secundária",
    ".selectedActiveNode": "campo runtime antigo de ativa única",
    "smiteCharges": "modelo antigo de Smite por cargas",
}
all_java = "\n".join(p.read_text(encoding="utf-8") for p in JAVA.rglob("*.java"))
for pattern, label in stale_patterns.items():
    if pattern in all_java:
        errors.append(f"Referência obsoleta encontrada ({label}): {pattern}")

# ---------- Keybind / idiomas ----------
for key in range(1, 5):
    token = f'key.rpgstats.ability{key}'
    if token not in client:
        errors.append(f"Keybind ausente no cliente: {token}")
    for lang in ["pt_br.json", "en_us.json"]:
        data = json.loads(read(f"src/main/resources/assets/rpgstats/lang/{lang}"))
        if token not in data:
            errors.append(f"Tradução ausente em {lang}: {token}")

# ---------- Delimitadores Java ----------
for path in JAVA.rglob("*.java"):
    ok, reason = balanced_delimiters(path)
    if not ok:
        errors.append(f"Delimitadores inválidos em {path.relative_to(ROOT)}: {reason}")

print(f"Nodes base: {len(base_nodes)}")
print(f"Nodes Mago: {len(mage_nodes)}")
print(f"Nodes totais: {len(all_nodes)}")
print(f"Registry total: {len(all_registry_ids)}")
print(f"Ativas Mago: {len(active_mage_ids)}")
print(f"Casas: {len(path_methods)} | Especializações: {len(spec_methods)} | Ascensões: {len(asc_lines)}")
print(f"Custo mínimo até Maestria Arcana: {core_mastery_cost} PH")
print("Cobertura registry:", "OK" if not missing_registry and not orphan_registry else "FALHA")
print("Cobertura ativas Mago:", "OK" if not missing_active_cases else "FALHA")

if warnings:
    print("\nAVISOS:")
    for warning in warnings:
        print(" -", warning)

if errors:
    print("\nFALHAS:")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("\nVALIDAÇÃO ESTÁTICA v1.5: OK")
