"""Synthetic report fixtures test import correctness, never encounter balance."""
import copy
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from import_boss_arena_evidence import import_evidence

ROOT = Path(__file__).resolve().parents[1]


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.matrix = json.loads((ROOT / 'compat/boss-arena-matrix.json').read_text())
        self.report = {
            'schema': 1, 'session': '211acb95-0997-47d9-8741-1e8b76c00001',
            'initial': {'registry_id': 'soulsweapons:returning_knight', 'class': 'MAGO',
                        'level': 45, 'declared_build': 'balanced', 'boss_hp': 500,
                        'boss_max_hp': 500, 'boss_ai_disabled': False, 'boss_no_gravity': False,
                        'player_game_mode': 'SURVIVAL', 'player_no_gravity': False,
                        'player_invulnerable': False, 'world_difficulty': 'NORMAL',
                        'server_runtime_class': 'net.minecraft.server.integrated.IntegratedServer',
                        'stats': {'INTELIGENCIA': 50}, 'equipment': [{'id': 'minecraft:air'}]},
            'outcome': 'BOSS_DEAD', 'elapsed_ticks': 2400, 'wall_seconds': 120,
            'started_boss_full_health': True, 'ttk_seconds': 120,
            'damage_dealt': 500, 'damage_received': 80, 'boss_attributed_damage_received': 70,
            'other_damage_to_boss': 0, 'other_attackers': 0, 'confirmed_player_hits': 10,
            'player_dps_wall': 500 / 120, 'net_health_recovered': 30,
            'sampled_resource_depletion': 90, 'sampled_stamina_depletion': 20,
            'samples': [{'tick': 0, 'boss_hp': 500, 'boss_max_hp': 500,
                         'player_hp': 40, 'rpg_cooldowns_ticks': {}},
                        {'tick': 2400, 'boss_hp': 0, 'boss_max_hp': 500,
                         'player_hp': 10, 'rpg_cooldowns_ticks': {}}],
            'incoming_sources': {'fall|unattributed': 10, 'mob|soulsweapons:returning_knight': 70},
            'manual_markers': [], 'forge_version': '47.4.10',
            'mods': {'rpgstats': 'test', 'soulsweapons': 'test'},
        }

    def pair(self, report=None):
        raw = (json.dumps(report or self.report) + '\n').encode()
        review = {'schema': 1, 'session': (report or self.report)['session'],
                  'report_sha256': hashlib.sha256(raw).hexdigest(),
                  'reviewer': 'test-only reviewer', 'build_legal': True,
                  'build_notes': 'Test fixture; not real calibration.',
                  'replay_reference': 'test-only replay',
                  'runtime_scope': 'NATIVE_SELECTION',
                  'runtime_notes': 'Original native mods, not the full modpack.',
                  'mechanics': {key: 'Reviewed in test fixture, not real combat.' for key in
                               ('phases', 'special_attacks', 'summons', 'indirect_sources',
                                'native_cooldowns', 'vulnerability_windows', 'movement',
                                'combos', 'misses', 'uptime')}}
        return raw, review

    def test_import_preserves_raw_metrics_without_completing_other_cases(self):
        original = copy.deepcopy(self.matrix)
        result = import_evidence(self.matrix, [self.pair()])
        measured = [c for c in result['cases'] if c['status'] == 'MEASURED_REVIEWED']
        self.assertEqual(1, len(measured))
        trial = measured[0]['trials'][0]
        self.assertEqual(120, trial['ttk_seconds'])
        self.assertEqual(80, trial['damage_received'])
        self.assertEqual(500 / 120, trial['player_dps'])
        self.assertEqual('NATIVE_SELECTION', trial['runtime_scope'])
        self.assertFalse(result['release_ready'])
        self.assertEqual(original, self.matrix)
        self.assertEqual(219, sum(c['status'] == 'NOT_RUN' for c in result['cases']))

    def test_death_is_measured_without_a_win_or_ttk(self):
        report = copy.deepcopy(self.report)
        report.update(outcome='PLAYER_DEAD', ttk_seconds=None)
        report['samples'][-1].update(player_hp=0, boss_hp=300)
        result = import_evidence(self.matrix, [self.pair(report)])
        trial = next(c for c in result['cases'] if c['status'] != 'NOT_RUN')['trials'][0]
        self.assertEqual('PLAYER_DEAD', trial['outcome'])
        self.assertIsNone(trial['ttk_seconds'])

    def test_accepts_forge_production_integrated_server_name(self):
        report = copy.deepcopy(self.report)
        report['initial']['server_runtime_class'] = 'net.minecraft.client.server.IntegratedServer'
        result = import_evidence(self.matrix, [self.pair(report)])
        self.assertEqual(1, result['coverage_summary']['measured_cases'])

    def test_rejects_pvp_and_malformed_samples_or_json(self):
        report = copy.deepcopy(self.report)
        report['incoming_sources'] = {'player|minecraft:player': 80}
        with self.assertRaises(ValueError):
            import_evidence(self.matrix, [self.pair(report)])
        report = copy.deepcopy(self.report)
        report['samples'][-1]['boss_hp'] = 5
        with self.assertRaises(ValueError):
            import_evidence(self.matrix, [self.pair(report)])
        raw, review = self.pair()
        duplicate = raw.replace(b'"schema": 1', b'"schema": 1, "schema": 1', 1)
        review['report_sha256'] = hashlib.sha256(duplicate).hexdigest()
        with self.assertRaisesRegex(ValueError, 'duplicate JSON'):
            import_evidence(self.matrix, [(duplicate, review)])

    def test_rejects_controls_interruption_partial_hp_and_external_damage(self):
        variants = [({'boss_ai_disabled': True}, {}), ({'player_game_mode': 'CREATIVE'}, {}),
                    ({'player_invulnerable': True}, {}), ({'boss_no_gravity': True}, {}),
                    ({'server_runtime_class': 'net.minecraft.test.GameTestServer'}, {}),
                    ({'world_difficulty': 'PEACEFUL'}, {}),
                    ({}, {'outcome': 'MANUAL_STOP'}), ({}, {'started_boss_full_health': False}),
                    ({}, {'other_damage_to_boss': 1}), ({}, {'other_attackers': 1})]
        for initial, root in variants:
            with self.subTest(initial=initial, root=root):
                report = copy.deepcopy(self.report)
                report['initial'].update(initial)
                report.update(root)
                with self.assertRaises(ValueError):
                    import_evidence(self.matrix, [self.pair(report)])

    def test_rejects_hash_mismatch_unreviewed_and_false_full_pack_claim(self):
        raw, review = self.pair()
        variants = [{'report_sha256': '0' * 64}, {'build_legal': False},
                    {'replay_reference': ''}, {'runtime_scope': 'FULL_MODPACK'},
                    {'mechanics': {}}]
        for change in variants:
            with self.subTest(change=change):
                with self.assertRaises(ValueError):
                    import_evidence(self.matrix, [(raw, {**review, **change})])

    def test_rejects_unknown_case_duplicates_and_nonfinite_or_inconsistent_metrics(self):
        variants = [({'level': 49}, {}), ({'class': 'PALADINO'}, {}),
                    ({}, {'wall_seconds': float('nan')}), ({}, {'damage_dealt': -1}),
                    ({}, {'ttk_seconds': 1}), ({}, {'player_dps_wall': 999}),
                    ({}, {'boss_attributed_damage_received': 81})]
        for initial, root in variants:
            report = copy.deepcopy(self.report)
            report['initial'].update(initial)
            report.update(root)
            with self.subTest(initial=initial, root=root), self.assertRaises(ValueError):
                import_evidence(self.matrix, [self.pair(report)])
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            import_evidence(self.matrix, [self.pair(), self.pair()])

    def test_repeat_trials_are_retained_and_missing_cases_remain_not_run(self):
        second = copy.deepcopy(self.report)
        second['session'] = '211acb95-0997-47d9-8741-1e8b76c00002'
        result = import_evidence(self.matrix, [self.pair(), self.pair(second)])
        self.assertEqual(2, result['coverage_summary']['reviewed_trials'])
        self.assertEqual(1, result['coverage_summary']['measured_cases'])
        self.assertEqual(2, len(next(c for c in result['cases'] if c['status'] != 'NOT_RUN')['trials']))

    def test_rejects_damage_hit_contradictions(self):
        for change in ({'confirmed_player_hits': 0}, {'damage_dealt': 0, 'player_dps_wall': 0}):
            report = copy.deepcopy(self.report)
            report.update(outcome='PLAYER_DEAD', ttk_seconds=None)
            report['samples'][-1].update(player_hp=0, boss_hp=300)
            report.update(change)
            with self.subTest(change=change), self.assertRaises(ValueError):
                import_evidence(self.matrix, [self.pair(report)])

    def test_rejects_duplicate_session_with_alternate_uuid_spelling(self):
        second = copy.deepcopy(self.report)
        second['session'] = self.report['session'].upper()
        with self.assertRaises(ValueError):
            import_evidence(self.matrix, [self.pair(), self.pair(second)])

    def test_both_dead_requires_mutual_outcome(self):
        report = copy.deepcopy(self.report)
        report.update(outcome='PLAYER_DEAD', ttk_seconds=None)
        report['samples'][-1]['player_hp'] = 0
        with self.assertRaises(ValueError):
            import_evidence(self.matrix, [self.pair(report)])
        report['outcome'] = 'MUTUAL_DEATH'
        result = import_evidence(self.matrix, [self.pair(report)])
        trial = next(c for c in result['cases'] if c['status'] != 'NOT_RUN')['trials'][0]
        self.assertEqual('MUTUAL_DEATH', trial['outcome'])
        self.assertIsNone(trial['ttk_seconds'])

    def test_cli_invalid_input_does_not_overwrite_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'bad.json').write_text('{}')
            (root / 'output.json').write_text('existing evidence')
            completed = subprocess.run([sys.executable, str(ROOT / 'tools/import_boss_arena_evidence.py'),
                                        '--matrix', str(ROOT / 'compat/boss-arena-matrix.json'),
                                        '--report', str(root / 'bad.json'), '--review', str(root / 'bad.json'),
                                        '--output', str(root / 'output.json')], capture_output=True, text=True)
            self.assertNotEqual(0, completed.returncode)
            self.assertEqual('existing evidence', (root / 'output.json').read_text())


if __name__ == '__main__':
    unittest.main()
