from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
files=['src/main/java/com/rpgstats/balance/ClassBalance.java','src/main/java/com/rpgstats/balance/GlobalCaps.java','tools/combat-tests/PhysicalProgressionBudgetTests.java']
with tempfile.TemporaryDirectory(prefix='rpg-physical-progression-') as out:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',out,*[str(root/f) for f in files]],check=True)
 subprocess.run(['java','-cp',out,'PhysicalProgressionBudgetTests'],check=True)
