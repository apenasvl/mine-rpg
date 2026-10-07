"""Exercise actual ZIP descriptors and manifest pins; no runtime compatibility claims."""
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from audit_modpack_manifest import audit_manifest, inspect_archive
from inspect_combat_mods import inspect_jar, InspectionError


def jar_bytes(mod='fixture', version='1.0', loader_range='[47,)', dependencies='', nested=None):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, 'w') as jar:
        if mod:
            jar.writestr('META-INF/mods.toml', f'modLoader="javafml"\nloaderVersion="{loader_range}"\n'
                         f'[[mods]]\nmodId="{mod}"\nversion="{version}"\n' + dependencies)
        jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nImplementation-Version: 1.0\n'
                     + ('FMLModType: LIBRARY\n' if not mod else ''))
        if nested:
            jar.writestr('META-INF/jarjar/metadata.json', json.dumps({'jars': [{'path': 'META-INF/jarjar/lib.jar'}]}))
            jar.writestr('META-INF/jarjar/lib.jar', nested)
    return stream.getvalue()


class RuntimeForgeMetadataTests(unittest.TestCase):
    def inspect(self, loader_range, installed):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'mod.jar'
            path.write_bytes(jar_bytes(loader_range=loader_range))
            return inspect_jar(path, {'library': True}, installed)

    def test_actual_forge10_range_is_used(self):
        self.assertEqual('fixture', self.inspect('[47.4.10,)', {'forge': '47.4.10'})['modId'])

    def test_forge10_is_rejected_when_excluded_by_descriptor(self):
        with self.assertRaises(InspectionError):
            self.inspect('[47.4.0,47.4.10)', {'forge': '47.4.10'})

    def test_explicit_runtime_checks_extra_libraries_without_dependency_install_map(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'extra.jar'
            dep = '[[dependencies.fixture]]\nmodId="native_library"\nmandatory=true\nversionRange="[2,)"\n'
            path.write_bytes(jar_bytes(loader_range='[47.4.10,)', dependencies=dep))
            self.assertEqual('fixture', inspect_jar(path, {'library': True}, forge_version='47.4.10')['modId'])
            path.write_bytes(jar_bytes(loader_range='[47.4.0,47.4.10)'))
            with self.assertRaises(InspectionError):
                inspect_jar(path, {'library': True}, forge_version='47.4.10')


class ModpackAuditTests(unittest.TestCase):
    def manifest(self, *projects):
        return {'minecraft': {'version': '1.20.1', 'modLoaders': [{'id': 'forge-47.4.10', 'primary': True}]},
                'files': [{'projectID': project, 'fileID': 100 + project, 'required': True} for project in projects]}

    def audit(self, manifest, jars):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for project, data in jars.items():
                (root / f'{project}-{100 + project}.jar').write_bytes(data)
            return audit_manifest(manifest, root)

    def test_inventory_all_exact_files_without_runtime_approval(self):
        result = self.audit(self.manifest(1, 2), {1: jar_bytes(), 2: jar_bytes('library')})
        self.assertEqual(2, result['resolved_files'])
        self.assertEqual('INVENTORY_COMPLETE', result['status'])
        self.assertFalse(result['runtime_validated'])
        self.assertFalse(result['release_ready'])
        self.assertEqual([], result['dependency_findings'])

    def test_nested_library_mods_supply_dependencies(self):
        dep = '[[dependencies.fixture]]\nmodId="library"\nmandatory=true\nversionRange="[2,)"\nside="BOTH"\n'
        container = jar_bytes(None, nested=jar_bytes('library', '2.1'))
        result = self.audit(self.manifest(1, 2), {1: jar_bytes(dependencies=dep), 2: container})
        self.assertEqual([], result['dependency_findings'])
        self.assertIn('library', result['installed_mods'])
        self.assertEqual('2.1', result['installed_mods']['library'])

    def test_missing_dependency_and_wrong_minecraft_are_findings_not_pack_approval(self):
        dep = ('[[dependencies.fixture]]\nmodId="absent"\nmandatory=true\nversionRange="[1,)"\nside="CLIENT"\n'
               '[[dependencies.fixture]]\nmodId="minecraft"\nmandatory=true\nversionRange="[1.21]"\nside="BOTH"\n')
        result = self.audit(self.manifest(1), {1: jar_bytes(dependencies=dep)})
        self.assertEqual('INVENTORY_WITH_FINDINGS', result['status'])
        self.assertEqual({'MISSING_DEPENDENCY', 'INCOMPATIBLE_VERSION'},
                         {f['kind'] for f in result['dependency_findings']})

    def test_duplicate_mod_ids_and_excluded_actual_forge_are_reported(self):
        result = self.audit(self.manifest(1, 2), {1: jar_bytes(), 2: jar_bytes(loader_range='[47.4.0,47.4.10)')})
        self.assertEqual({'DUPLICATE_MOD_ID', 'LOADER_RANGE'},
                         {f['kind'] for f in result['dependency_findings']})

    def test_missing_exact_file_duplicate_project_and_wrong_runtime_are_errors(self):
        with self.assertRaises(ValueError):
            self.audit(self.manifest(1), {})
        with self.assertRaises(ValueError):
            self.audit(self.manifest(1, 1), {1: jar_bytes()})
        manifest = self.manifest(1)
        manifest['minecraft']['modLoaders'][0]['id'] = 'forge-47.4.0'
        with self.assertRaises(ValueError):
            self.audit(manifest, {1: jar_bytes()})

    def test_unresolved_version_range_is_explicit(self):
        dep = '[[dependencies.fixture]]\nmodId="library"\nmandatory=true\nversionRange="[1.0-beta,)"\nside="BOTH"\n'
        result = self.audit(self.manifest(1, 2), {1: jar_bytes(dependencies=dep), 2: jar_bytes('library', '1.0-alpha')})
        self.assertEqual('RANGE_REQUIRES_RUNTIME_VALIDATION', result['dependency_findings'][0]['kind'])

    def test_non_forge_archive_is_reported_without_native_metadata(self):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as jar:
            jar.writestr('fabric.mod.json', '{"id":"fabric_only"}')
        result = inspect_archive(stream.getvalue())
        self.assertEqual([], result['mods'])
        self.assertEqual('UNSUPPORTED_ARCHIVE', result['findings'][0]['kind'])

    def test_malformed_loader_ranges_never_get_clean_inventory(self):
        for requirement in ('[47,48]]', '[47,48)[', '(47)', '[48,47)', '[,48]'):
            with self.subTest(requirement=requirement):
                result = self.audit(self.manifest(1), {1: jar_bytes(loader_range=requirement)})
                self.assertEqual('INVENTORY_WITH_FINDINGS', result['status'])

    def test_unresolved_standalone_version_is_not_silently_approved(self):
        result = self.audit(self.manifest(1), {1: jar_bytes(version='${file.unresolved}')})
        self.assertEqual('UNRESOLVED_MOD_VERSION', result['dependency_findings'][0]['kind'])

    def test_resource_pack_is_not_classified_as_a_forge_mod(self):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as archive:
            archive.writestr('pack.mcmeta', '{"pack":{"pack_format":15,"description":"fixture"}}')
            archive.writestr('assets/minecraft/textures/fixture.png', b'fixture')
        result = self.audit(self.manifest(1), {1: stream.getvalue()})
        self.assertEqual('RESOURCE_PACK', result['files'][0]['archive_kind'])
        self.assertEqual(1, result['archive_counts']['RESOURCE_PACK'])
        self.assertEqual([], result['dependency_findings'])
        self.assertEqual({'minecraft', 'forge'}, set(result['installed_mods']))

    def test_fabric_mod_with_pack_metadata_is_not_misclassified_as_resource_pack(self):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as archive:
            archive.writestr('pack.mcmeta', '{"pack":{"pack_format":15}}')
            archive.writestr('fabric.mod.json', '{"id":"fabric_only"}')
            archive.writestr('assets/fabric_only/texture.png', b'fixture')
        result = inspect_archive(stream.getvalue())
        self.assertEqual('UNSUPPORTED_ARCHIVE', result['findings'][0]['kind'])


if __name__ == '__main__':
    unittest.main()
