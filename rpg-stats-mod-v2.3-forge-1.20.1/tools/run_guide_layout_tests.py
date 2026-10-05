from pathlib import Path
import tempfile,subprocess
root=Path(__file__).resolve().parents[1]
sources=[root/'src/main/java/com/rpgstats/gui/MageViewport.java',root/'tools/ui-tests/GuideLayoutTests.java']
layout=root/'src/main/java/com/rpgstats/gui/GuideLayout.java'
if layout.exists():sources.append(layout)
with tempfile.TemporaryDirectory(prefix='rpg-guide-') as out:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-d',out,*map(str,sources)],check=True)
 subprocess.run(['java','-cp',out,'GuideLayoutTests'],check=True)
