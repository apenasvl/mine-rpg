from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
sources=[root/'src/main/java/com/rpgstats/gui'/name for name in ['MageTheme.java','MageViewport.java']]
assert all(p.exists() for p in sources), 'RED: mage theme routing and fitted viewport are missing'
sources.append(root/'tools/ui-tests/MageThemeTests.java')
with tempfile.TemporaryDirectory() as out:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',out,*map(str,sources)],check=True)
 subprocess.run(['java','-cp',out,'MageThemeTests'],check=True)
