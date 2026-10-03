"""Run real Java domain logic without game dependencies. Stubs are outside src/main."""
from pathlib import Path
import subprocess
import tempfile
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'src/main/java/com/rpgstats'
files = list((SRC/'classes').glob('*.java')) + list((SRC/'ability').glob('*.java'))
files += [SRC/'tree/SkillNode.java', SRC/'stats/Stat.java', SRC/'stats/PlayerStats.java',
          SRC/'stats/SkillNodeHolder.java', SRC/'combat/MageBalance.java', SRC/'gui/ArcaneHudText.java']
files += list((ROOT/'tools/house-tests').rglob('*.java'))
with tempfile.TemporaryDirectory(prefix='rpg-house-tests-') as out:
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17',
                    '-encoding','UTF-8','-d',out,*map(str,files)],check=True)
    subprocess.run(['java','-cp',out,'HouseTests'],check=True)
