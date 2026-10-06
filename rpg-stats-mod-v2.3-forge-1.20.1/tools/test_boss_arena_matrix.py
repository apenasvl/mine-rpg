import json
import unittest
from pathlib import Path
from build_boss_arena_matrix import build_matrix

ROOT=Path(__file__).resolve().parents[1]
class ArenaMatrixTests(unittest.TestCase):
    def setUp(self):
        self.profiles=json.loads((ROOT/'src/main/resources/data/rpgstats/rpgstats/native_bosses.json').read_text())['entries']
        self.native=json.loads((ROOT/'compat/boss-native-calibration-baseline.json').read_text())['bosses']
        self.matrix=build_matrix(self.profiles,self.native)
    def test_current_registry_profiles_and_four_classes(self):
        self.assertEqual({p['id'] for p in self.profiles},{p['registry_id'] for p in self.matrix['profiles']})
        for profile in self.profiles:
            base=[c for c in self.matrix['cases'] if c['registry_id']==profile['id'] and c['build']=='balanced' and c['level']==profile['fixed']['reference_level']]
            self.assertEqual({'GUERREIRO','MAGO','ARQUEIRO','ASSASSINO'},{c['class'] for c in base})
    def test_no_fabricated_results_and_no_duplicate_capped_levels(self):
        seen=set()
        for c in self.matrix['cases']:
            key=(c['registry_id'],c['class'],c['level'],c['build'])
            self.assertNotIn(key,seen);seen.add(key)
            self.assertLessEqual(c['level'],50);self.assertGreaterEqual(c['level'],1)
            self.assertEqual('NOT_RUN',c['status']);self.assertIsNone(c['ttk_seconds']);self.assertIsNone(c['outcome'])
    def test_high_tier_very_overlevel_is_unavailable_not_faked(self):
        p=next(p for p in self.matrix['profiles'] if p['registry_id']=='soulsweapons:returning_knight')
        self.assertEqual('UNAVAILABLE_LEVEL_CAP',p['very_overlevel'])
    def test_missing_native_baseline_is_an_error(self):
        with self.assertRaisesRegex(ValueError,'missing native'):
            build_matrix(self.profiles,[])
if __name__=='__main__':unittest.main()
