import hashlib, json, tempfile, unittest, zipfile
from pathlib import Path
from inspect_boss_mods import inspect_jar, InspectionError

class BossInspectionTests(unittest.TestCase):
    def jar(self, root, loader='javafml', minecraft='[1.20.1]', version='1.2.3'):
        path=Path(root)/'native.jar'
        meta=f'''modLoader="{loader}"
loaderVersion="[47,)"
[[mods]]
modId="bossfixture"
version="{version}"
[[dependencies.bossfixture]]
modId="minecraft"
mandatory=true
versionRange="{minecraft}"
[[dependencies.bossfixture]]
modId="native_library"
mandatory=true
versionRange="[2,)"
'''
        with zipfile.ZipFile(path,'w') as z:
            z.writestr('META-INF/mods.toml',meta)
            z.writestr('assets/bossfixture/lang/en_us.json',json.dumps({'entity.bossfixture.giant':'Giant','item.bossfixture.blade':'Blade','item.other.foreign':'Foreign'}))
            z.writestr('data/bossfixture/recipes/blade.json',json.dumps({'result':{'item':'bossfixture:blade'}}))
            z.writestr('native/bossfixture/Giant.class',b'fixture-not-bytecode')
        return path
    def test_correct_native_inventory(self):
        with tempfile.TemporaryDirectory() as d:
            report=inspect_jar(self.jar(d),{'modId':'bossfixture','version':'1.2.3'},{'minecraft':'1.20.1','native_library':'2.0'})
            self.assertEqual(report.get('entities'),[{'id':'bossfixture:giant','label':'Giant','source':'translation-key','verifiedRegistry':False}])
            self.assertEqual(report['items'][0]['id'],'bossfixture:blade')
            self.assertTrue(report['items'][0]['recipeReferenced'])
            self.assertEqual(report['nativeClasses'],['native.bossfixture.Giant'])
    def test_wrong_checksum(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(InspectionError): inspect_jar(self.jar(d),{'sha256':'0'*64})
    def test_wrong_loader(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(InspectionError): inspect_jar(self.jar(d,loader='fabric'))
    def test_wrong_minecraft(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(InspectionError): inspect_jar(self.jar(d,minecraft='[1.21]'))
    def test_wrong_mod_version(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(InspectionError): inspect_jar(self.jar(d),{'version':'9.0'})
    def test_missing_native_dependency(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(InspectionError): inspect_jar(self.jar(d),installed={'minecraft':'1.20.1'})

if __name__=='__main__': unittest.main()
