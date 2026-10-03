from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
files=[root/'src/main/java/com/rpgstats/compat/PhysicalActionPolicy.java',root/'src/main/java/com/rpgstats/compat/ArmorBonusPolicy.java',root/'tools/combat-tests/PhysicalActionTests.java']
with tempfile.TemporaryDirectory() as out:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-d',out,*map(str,files)],check=True)
 subprocess.run(['java','-cp',out,'PhysicalActionTests'],check=True)
