"""Compile real dependency-free boss/equipment policies, not copied formulas."""
from pathlib import Path
import subprocess,tempfile
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'src/main/java/com/rpgstats'
files=[SRC/'integration/FixedBossProfile.java',SRC/'compat/bosses/FixedBossPolicy.java']
files += [SRC/'integration/EquipmentRules.java', SRC/'compat/bosses/EquipmentPolicy.java']
files+=list((ROOT/'tools/boss-balance-tests').glob('*.java'))
with tempfile.TemporaryDirectory(prefix='rpg-boss-tests-') as out:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',out,*map(str,files)],check=True)
 subprocess.run(['java','-cp',out,'FixedBossTests'],check=True)
 subprocess.run(['java','-cp',out,'EquipmentTests'],check=True)
