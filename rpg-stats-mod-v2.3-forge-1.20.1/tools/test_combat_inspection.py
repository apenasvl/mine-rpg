import unittest, tempfile, zipfile
from pathlib import Path
from inspect_combat_mods import inspect_jar, InspectionError

TOML = '''modLoader="javafml"
loaderVersion="[47,)"
[[mods]]
modId="example"
version="1.0"
[[dependencies.example]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.20.2)"
'''
class InspectionTests(unittest.TestCase):
    def jar(self, metadata=None):
        self.tmp = tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        p=Path(self.tmp.name)/'mod.jar'
        with zipfile.ZipFile(p,'w') as z:
            for name, text in (metadata or {'META-INF/mods.toml':TOML}).items(): z.writestr(name,text)
        return p
    def test_forge(self):
        self.assertEqual(inspect_jar(self.jar(), {'modId':'example','version':'1.0'})['modId'],'example')
    def test_fabric_rejected(self):
        with self.assertRaises(InspectionError): inspect_jar(self.jar({'fabric.mod.json':'{}'}),{})
    def test_old_mc_rejected(self):
        with self.assertRaises(InspectionError): inspect_jar(self.jar({'META-INF/mods.toml':TOML.replace('[1.20.1,1.20.2)','[1.12.2]')}),{})
    def test_checksum_rejected(self):
        with self.assertRaises(InspectionError): inspect_jar(self.jar(),{'sha256':'0'*64})
    def test_forge_loader_version_rejected(self):
        with self.assertRaises(InspectionError): inspect_jar(self.jar({'META-INF/mods.toml':TOML.replace('[47,)','[48,)')}),{})
    def test_compatible_lower_mc_bound(self):
        self.assertEqual(inspect_jar(self.jar({'META-INF/mods.toml':TOML.replace('[1.20.1,1.20.2)','[1.20,)')}),{})['modId'],'example')
    def test_dependency_version_rejected(self):
        with self.assertRaises(InspectionError): inspect_jar(self.jar(),{},installed={'forge':'47.4.0','minecraft':'1.19.4'})
    def test_missing_required_dependency(self):
        metadata=TOML+'\n[[dependencies.example]]\nmodId="missing"\nmandatory=true\nversionRange="[1,)"\n'
        with self.assertRaises(InspectionError): inspect_jar(self.jar({'META-INF/mods.toml':metadata}),{}, installed={'forge','minecraft','example'})
if __name__=='__main__': unittest.main()
