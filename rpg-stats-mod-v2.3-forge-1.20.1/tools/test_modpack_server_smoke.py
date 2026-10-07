import tempfile
import unittest
from pathlib import Path
from run_modpack_server_smoke import stage_modpack, validated_boot


class ModpackServerSmokeTests(unittest.TestCase):
    def test_boot_requires_actual_forge_normal_server_and_clean_shutdown(self):
        log = ('Forge mod loading, version 47.4.10, for MC 1.20.1\n'
               '[Server thread/INFO] [minecraft/DedicatedServer]: Done (12.0s)! For help, type "help"\n'
               '[Server thread/INFO] [minecraft/MinecraftServer]: Stopping server\n')
        self.assertEqual('47.4.10', validated_boot(log, 0))
        for invalid in (log.replace('47.4.10', '47.4.0'), log.replace('DedicatedServer', 'GameTestServer'),
                        log.replace('Stopping server', 'Crash report saved')):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                validated_boot(invalid, 0)
        with self.assertRaises(ValueError):
            validated_boot(log, 1)

    def test_stages_all_mods_separates_resources_and_preserves_config_bytes(self):
        inventory = {'files': [{'projectID': 1, 'fileID': 2, 'archive_kind': 'FORGE_MOD'},
                               {'projectID': 3, 'fileID': 4, 'archive_kind': 'FORGE_LIBRARY_CONTAINER'},
                               {'projectID': 5, 'fileID': 6, 'archive_kind': 'RESOURCE_PACK'}]}
        config = {'files': {'soulsweapons/native.json': '{"damage":1.0}\r\n'},
                  'excluded_nontext': ['epicfight/native/ServerCommunicationHelper.dll']}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            jars = root / 'jars'; jars.mkdir()
            for name in ('1-2.jar', '3-4.jar', '5-6.jar'):
                (jars / name).write_bytes(name.encode())
            main = root / 'rpg.jar'; main.write_bytes(b'current RPG')
            server = root / 'server'
            result = stage_modpack(inventory, jars, config, main, server)
            self.assertEqual(3, len(list((server / 'mods').glob('*.jar'))))
            self.assertEqual(1, len(list((server / 'resourcepacks').glob('*.zip'))))
            self.assertEqual(b'{"damage":1.0}\r\n', (server / 'config/soulsweapons/native.json').read_bytes())
            self.assertEqual(2, result['native_mod_files'])
            self.assertFalse(result['client_validated'])
            self.assertFalse(result['encounters_validated'])

    def test_unknown_archive_and_config_traversal_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            main = root / 'rpg.jar'; main.write_bytes(b'RPG')
            bad = {'files': [{'projectID': 1, 'fileID': 2, 'archive_kind': 'UNSUPPORTED_ARCHIVE'}]}
            with self.assertRaises(ValueError):
                stage_modpack(bad, root, {'files': {}, 'excluded_nontext': []}, main, root / 'server')
            with self.assertRaises(ValueError):
                stage_modpack({'files': []}, root, {'files': {'../outside': 'bad'}, 'excluded_nontext': []},
                              main, root / 'server')

    def test_stale_mods_are_rejected_without_deleting_them(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            mods = root / 'server/mods'; mods.mkdir(parents=True)
            (mods / 'old.jar').write_bytes(b'old')
            main = root / 'rpg.jar'; main.write_bytes(b'RPG')
            with self.assertRaises(ValueError):
                stage_modpack({'files': []}, root, {'files': {}, 'excluded_nontext': []}, main, root / 'server')
            self.assertEqual(b'old', (mods / 'old.jar').read_bytes())


if __name__ == '__main__':
    unittest.main()
