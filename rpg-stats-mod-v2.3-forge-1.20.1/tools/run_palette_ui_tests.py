"""Compile actual palette methods in isolation; no Minecraft rendering stubs needed."""
from pathlib import Path
import re, subprocess, tempfile
root=Path(__file__).resolve().parents[1]
source=(root/'src/main/java/com/rpgstats/gui/RpgUiTheme.java').read_text()
methods=[]
for name in ['accent','classNeutral','houseAccent','themedAccent','primaryAccent','mix']:
    start=source.index('    public static int '+name+'(')
    opening=source.index('{',start); depth=1; end=opening+1
    while depth:
        depth += (source[end]=='{')-(source[end]=='}'); end+=1
    methods.append(source[start:end])
paths=re.findall(r'^    ([A-Z_]+)\(RPGClass\.',(root/'src/main/java/com/rpgstats/classes/RPGPath.java').read_text(),re.M)
java='enum RPGClass { GUERREIRO,MAGO,ARQUEIRO,ASSASSINO }\nenum RPGPath {'+','.join(paths)+'}\npublic class PaletteTests {\n'+'\n'.join(methods)+r'''
public static void main(String[] args) {
    int[] neutral={0xFF241A1D,0xFF171F2D,0xFF18241E,0xFF232733};
    int i=0;
    for(RPGClass c:RPGClass.values()) {
        if(classNeutral(c)!=neutral[i++]) throw new AssertionError("Class neutral mismatch");
        for(RPGPath p:RPGPath.values()) {
            if(themedAccent(c,p,null)!=houseAccent(p)) throw new AssertionError("Primary must keep exact color alone");
            if(primaryAccent(c,p)!=houseAccent(p)) throw new AssertionError("Primary signature mismatch");
            for(RPGPath s:RPGPath.values()) {
                int a=houseAccent(p), b=houseAccent(s), expected=0xFF000000;
                for(int shift:new int[]{0,8,16}) expected|=Math.round((((a>>shift)&255)*3+((b>>shift)&255))/4f)<<shift;
                if(themedAccent(c,p,s)!=expected) throw new AssertionError("Expected ordered 75/25 palette");
            }
        }
    }
    if(themedAccent(RPGClass.MAGO,RPGPath.MAGE_TEMPORAL,RPGPath.MAGE_ARCANA)==themedAccent(RPGClass.MAGO,RPGPath.MAGE_ARCANA,RPGPath.MAGE_TEMPORAL)) throw new AssertionError("House order lost");
    System.out.println("PASS: four exact neutrals, exact primary accents and ordered 75/25 House blends");
}
}
'''
with tempfile.TemporaryDirectory() as out:
    p=Path(out)/'PaletteTests.java';p.write_text(java)
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-d',out,str(p)],check=True)
    subprocess.run(['java','-cp',out,'PaletteTests'],check=True)
