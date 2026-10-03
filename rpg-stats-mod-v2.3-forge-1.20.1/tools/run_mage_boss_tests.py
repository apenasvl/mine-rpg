from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
files=['src/main/java/com/rpgstats/combat/MageBalance.java','src/main/java/com/rpgstats/compat/irons/IronsSpellBalance.java','src/main/java/com/rpgstats/compat/irons/MageBossBalance.java','tools/hud-tests/MageBossBudgetTests.java']
with tempfile.TemporaryDirectory(prefix='rpg-mage-boss-') as out:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',out,*[str(root/f) for f in files]],check=True)
 subprocess.run(['java','-cp',out,'MageBossBudgetTests'],check=True)
