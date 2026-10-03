import unittest,tempfile,zipfile,hashlib,io
from pathlib import Path
from unittest.mock import patch
from install_combat_profile import install_profile
from inspect_combat_mods import InspectionError
from test_combat_inspection import TOML

class InstallerTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.destination=Path(self.tmp.name)
  out=io.BytesIO()
  with zipfile.ZipFile(out,'w') as jar:jar.writestr('META-INF/mods.toml',TOML)
  self.data=out.getvalue();self.entry={'modId':'example','version':'1.0','coordinate':'example-123:456','sha256':hashlib.sha256(self.data).hexdigest()}
 def test_success_installs_verified_binary(self):
  with patch('urllib.request.urlopen',return_value=io.BytesIO(self.data)):
   self.assertEqual(install_profile(self.destination,[self.entry]),1)
  self.assertEqual((self.destination/'example-123-456.jar').read_bytes(),self.data)
 def test_bad_hash_does_not_install_anything(self):
  with patch('urllib.request.urlopen',return_value=io.BytesIO(self.data)),self.assertRaises(InspectionError):
   install_profile(self.destination,[self.entry|{'sha256':'0'*64}])
  self.assertEqual(list(self.destination.iterdir()),[])
 def test_duplicate_version_is_not_deleted(self):
  old=self.destination/'older.jar';old.write_bytes(self.data)
  with patch('urllib.request.urlopen',return_value=io.BytesIO(self.data)),self.assertRaises(InspectionError):install_profile(self.destination,[self.entry])
  self.assertEqual(old.read_bytes(),self.data);self.assertEqual(len(list(self.destination.iterdir())),1)
 def test_excluded_mod_cannot_remain_in_selected_profile(self):
  with zipfile.ZipFile(self.destination/'rogues.jar','w') as jar:jar.writestr('META-INF/mods.toml',TOML.replace('example','rogues'))
  with patch('urllib.request.urlopen') as download,self.assertRaises(InspectionError):install_profile(self.destination,[self.entry])
  download.assert_not_called()
if __name__=='__main__':unittest.main()
