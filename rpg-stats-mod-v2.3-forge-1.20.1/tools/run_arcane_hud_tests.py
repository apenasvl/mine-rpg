from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as out:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',out,str(root/'src/main/java/com/rpgstats/gui/ArcaneHudText.java'),str(root/'tools/hud-tests/ArcaneHudTextTests.java')],check=True)
 subprocess.run(['java','-cp',out,'ArcaneHudTextTests'],check=True)
