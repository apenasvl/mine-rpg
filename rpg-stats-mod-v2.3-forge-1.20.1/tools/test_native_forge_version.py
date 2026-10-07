import unittest
from run_production_boss_tests import validated_runtime_forge
class NativeForgeVersionTests(unittest.TestCase):
    def test_runtime_version_is_loader_evidence_not_installer_filename(self):
        log='Downloading forge-1.20.1-47.4.10-installer.jar\nForge mod loading, version 47.4.0, for MC 1.20.1\n'
        with self.assertRaisesRegex(RuntimeError,'runtime Forge mismatch'):
            validated_runtime_forge(log,'47.4.10')
    def test_supported_runtime_loader_evidence(self):
        for version in ('47.4.0','47.4.10'):
            self.assertEqual(version,validated_runtime_forge('Forge mod loading, version '+version+', for MC 1.20.1',version))
    def test_missing_loader_evidence_rejected(self):
        with self.assertRaisesRegex(RuntimeError,'runtime Forge mismatch'):
            validated_runtime_forge('All178required tests passed','47.4.10')
if __name__=='__main__':unittest.main()
