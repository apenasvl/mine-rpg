"""Checks implemented domain logic and Java syntax; does not launch Minecraft."""
from pathlib import Path
import subprocess
root=Path(__file__).resolve().parents[1]
subprocess.run(["python3",str(root/"tools/run_house_tests.py")],check=True)
subprocess.run(["java",str(root/"tools/ParseSources.java"),str(root/"src/main/java")],check=True)
subprocess.run(["python3",str(root/"tools/validate_forge.py")],check=True)
print("Forge compilation and in-game testing are separate required gates; NOT certified by these checks.")
