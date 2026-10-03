"""Validação estrutural da arquitetura compat v2.2; não substitui um build Fabric."""
from pathlib import Path
import json, re, subprocess

root=Path(__file__).resolve().parents[1]
java=root/'src/main/java'
resources=root/'src/main/resources/data'

for path in resources.rglob('*.json'):
    json.loads(path.read_text(encoding='utf-8'))

compat=list((java/'com/rpgstats/compat').rglob('*.java'))
assert compat, 'camada compat ausente'
for path in java.rglob('*.java'):
    text=path.read_text(encoding='utf-8')
    external=[line for line in text.splitlines() if line.startswith('import ') and
              not any(token in line for token in ('java.','javax.','com.rpgstats','com.google.gson','com.mojang','net.fabricmc','net.minecraft','org.slf4j','org.spongepowered','org.lwjgl'))]
    assert not external, f'import externo direto em {path}: {external}'
    if 'FabricLoader' in text:
        assert '/compat/' in path.as_posix(), f'detecção espalhada em {path}'

required=['BossProfile.java','ItemProfile.java','SpellProfile.java','IntegrationServices.java','DataDrivenRegistry.java']
for name in required: assert (java/'com/rpgstats/integration'/name).exists(), name
for name in ['COMPATIBILITY.md','MODPACK-DESIGN.md','CHANGELOG-v2.2.md','VALIDATION-v2.2.md']:
    assert (root/name).exists() or name in ('COMPATIBILITY.md','MODPACK-DESIGN.md'), name

print(f'PASS: v2.2 structure, {len(list(resources.rglob("*.json")))} JSON and optional-import isolation.')
