import unittest
from inspect_boss_routes import inspect_bytecode
class Routes(unittest.TestCase):
 def test_exact_descriptor_and_owner_call_preserved(self):
  text='''public class native.ModWeapon {\n  public void strike(net.minecraft.world.entity.LivingEntity);\n    descriptor: (Lnet/minecraft/world/entity/LivingEntity;)V\n    Code:\n       1: invokevirtual #2 // Method net/minecraft/world/entity/LivingEntity.m_6469_:(Lnet/minecraft/world/damagesource/DamageSource;F)Z\n}\n'''
  r=inspect_bytecode(text)
  self.assertEqual(r[0]['class'],'native.ModWeapon');self.assertEqual(r[0]['method'],'strike')
  self.assertEqual(r[0]['descriptor'],'(Lnet/minecraft/world/entity/LivingEntity;)V')
  self.assertIn('m_6469_',r[0]['calls'][0]);self.assertEqual(r[0]['status'],'needs-owner-and-runtime-verification')
 def test_no_claim_for_unrelated_method(self):
  self.assertEqual(inspect_bytecode('public class native.A {\n  public void draw();\n    descriptor: ()V\n    Code:\n       0: return\n}\n'),[])
if __name__=='__main__':unittest.main()
