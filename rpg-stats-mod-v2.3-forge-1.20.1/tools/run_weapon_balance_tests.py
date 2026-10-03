from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
sets=[('WarriorHealingTests',['src/main/java/com/rpgstats/combat/WarriorHealingPolicy.java','tools/weapon-balance-tests/WarriorHealingTests.java']),('com.rpgstats.combat.ArcherAimPolicyTests',['src/main/java/com/rpgstats/combat/ArcherAimPolicy.java','tools/combat-tests/ArcherAimPolicyTests.java']),('com.rpgstats.compat.WeaponTypePolicyTests',['src/main/java/com/rpgstats/compat/WeaponTypePolicy.java','tools/weapon-type-tests/WeaponTypePolicyTests.java'])]
for name,files in sets:
 with tempfile.TemporaryDirectory() as out:
  subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-d',out,*[str(root/x) for x in files]],check=True)
  subprocess.run(['java','-cp',out,name],check=True)
