"""Exercise production selection/layout logic with Java 17; no Minecraft stubs."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
GUI = ROOT / "src/main/java/com/rpgstats/gui"
sources = [GUI / "ClassSelectionState.java", GUI / "ClassSelectionLayout.java",
           ROOT / "tools/ui-tests/ClassSelectionTests.java"]
if not all(path.exists() for path in sources):
    raise AssertionError("RED: class selection confirmation and viewport layout are not implemented")
with tempfile.TemporaryDirectory(prefix="rpg-ui-tests-") as out:
    subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "--release", "17",
                    "-encoding", "UTF-8", "-d", out, *map(str, sources)], check=True)
    subprocess.run(["java", "-cp", out, "ClassSelectionTests"], check=True)
