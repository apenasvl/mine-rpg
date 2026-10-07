"""Import reviewed recorder evidence without manufacturing encounters or release approval.

Review annotations are human attestations, not anti-tampering proof or automated
build legality checks. Full-modpack approval requires independent validation and
is deliberately unsupported here. The source matrix and reports stay untouched.
"""
import argparse
import copy
import hashlib
import json
import math
import os
import tempfile
import uuid
from pathlib import Path

MECHANICS = ('phases', 'special_attacks', 'summons', 'indirect_sources',
             'native_cooldowns', 'vulnerability_windows', 'movement', 'combos', 'misses', 'uptime')
OUTCOMES = {'BOSS_DEAD', 'PLAYER_DEAD', 'MUTUAL_DEATH'}
MAX_BYTES = 4 * 1024 * 1024


def require(condition, message):
    if not condition:
        raise ValueError(message)


def number(value, label, positive=False):
    require(type(value) in (int, float) and math.isfinite(value), 'invalid number: ' + label)
    require(value > 0 if positive else value >= 0, 'invalid range: ' + label)
    return value


def text(value):
    return isinstance(value, str) and bool(value.strip())


def strict_json(raw):
    require(len(raw) <= MAX_BYTES, 'evidence exceeds 4 MiB')

    def pairs(items):
        result = {}
        for key, value in items:
            require(key not in result, 'duplicate JSON field: ' + key)
            result[key] = value
        return result

    try:
        return json.loads(raw, object_pairs_hook=pairs)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ValueError('invalid JSON evidence') from error


def validate_report(raw, review):
    report = strict_json(raw)
    require(isinstance(report, dict) and report.get('schema') == 1, 'unsupported report schema')
    require(isinstance(review, dict) and review.get('schema') == 1, 'unsupported review schema')
    session = report.get('session')
    try:
        parsed_session = uuid.UUID(session)
    except (ValueError, TypeError, AttributeError) as error:
        raise ValueError('invalid session UUID') from error
    require(str(parsed_session) == session, 'session UUID must use canonical recorder spelling')
    digest = hashlib.sha256(raw).hexdigest()
    require(review.get('session') == session and review.get('report_sha256') == digest,
            'review does not match report session/hash')
    require(review.get('build_legal') is True, 'build needs manual legality review')
    for key in ('reviewer', 'build_notes', 'replay_reference', 'runtime_notes'):
        require(text(review.get(key)), 'missing review annotation: ' + key)
    require(review.get('runtime_scope') == 'NATIVE_SELECTION',
            'full modpack is not approved by this importer; runtime scope must be NATIVE_SELECTION')
    mechanics = review.get('mechanics')
    require(isinstance(mechanics, dict) and all(text(mechanics.get(k)) for k in MECHANICS),
            'missing mechanics/replay review')
    initial = report.get('initial')
    require(isinstance(initial, dict), 'missing initial snapshot')
    for key in ('boss_ai_disabled', 'boss_no_gravity', 'player_no_gravity', 'player_invulnerable'):
        require(initial.get(key) is False, 'controlled/non-survival fixture: ' + key)
    require(initial.get('player_game_mode') == 'SURVIVAL', 'requires survival combat')
    require(initial.get('world_difficulty') in ('EASY', 'NORMAL', 'HARD'), 'invalid difficulty')
    runtime = initial.get('server_runtime_class')
    require(runtime in ('net.minecraft.server.integrated.IntegratedServer',
                        'net.minecraft.client.server.IntegratedServer',
                        'net.minecraft.server.dedicated.MinecraftDedicatedServer',
                        'net.minecraft.server.dedicated.DedicatedServer'),
            'requires real server runtime, not GameTest fixture')
    require(type(initial.get('level')) is int and 1 <= initial['level'] <= 50, 'invalid level')
    require(isinstance(initial.get('stats'), dict) and initial['stats'], 'missing build stats')
    require(isinstance(initial.get('equipment'), list) and initial['equipment'], 'missing build gear')
    identifier = initial.get('registry_id')
    require(text(identifier) and ':' in identifier, 'invalid boss ID')
    mods = report.get('mods')
    require(isinstance(mods, dict) and text(mods.get('rpgstats'))
            and text(mods.get(identifier.split(':')[0])), 'missing RPG/native boss runtime versions')
    require(report.get('forge_version') in ('47.4.0', '47.4.10'), 'unreviewed Forge version')
    require(report.get('outcome') in OUTCOMES, 'interrupted/manual recording is not a completed encounter')
    require(report.get('started_boss_full_health') is True, 'requires full-health encounter start')
    hp = number(initial.get('boss_hp'), 'initial boss HP', True)
    maximum = number(initial.get('boss_max_hp'), 'initial boss max HP', True)
    require(abs(hp - maximum) < .01, 'inconsistent initial full-health flag')
    require(report.get('other_damage_to_boss') == 0 and type(report.get('other_attackers')) is int
            and report['other_attackers'] == 0, 'external damage/attackers invalidate solo trial')
    wall = number(report.get('wall_seconds'), 'wall seconds', True)
    ticks = report.get('elapsed_ticks')
    require(type(ticks) is int and 0 < ticks <= 12000 and wall <= 1200, 'invalid recording duration')
    numeric = ('damage_dealt', 'damage_received', 'boss_attributed_damage_received',
               'net_health_recovered', 'sampled_resource_depletion', 'sampled_stamina_depletion',
               'player_dps_wall')
    for key in numeric:
        number(report.get(key), key)
    require(type(report.get('confirmed_player_hits')) is int and report['confirmed_player_hits'] >= 0,
            'invalid confirmed hit count')
    require((report['damage_dealt'] > 0) == (report['confirmed_player_hits'] > 0),
            'confirmed damage and hit count contradict recorder contract')
    require(report['boss_attributed_damage_received'] <= report['damage_received'] + .001,
            'boss attribution exceeds total received damage')
    require(math.isclose(report['player_dps_wall'], report['damage_dealt'] / wall,
                         rel_tol=1e-5, abs_tol=.001), 'DPS does not match confirmed damage/wall time')
    sources = report.get('incoming_sources')
    require(isinstance(sources, dict), 'missing incoming source breakdown')
    require(all(isinstance(key, str) and key.rsplit('|', 1)[-1] != 'minecraft:player'
                for key in sources), 'PvP damage invalidates solo trial')
    require(math.isclose(sum(number(v, 'incoming source damage') for v in sources.values()),
                         report['damage_received'], rel_tol=1e-5, abs_tol=.001),
            'incoming source totals do not match damage received')
    samples = report.get('samples')
    require(isinstance(samples, list) and 2 <= len(samples) <= 605, 'missing/bounded samples')
    previous = -1
    for sample in samples:
        require(isinstance(sample, dict), 'invalid sample')
        tick = sample.get('tick')
        require(type(tick) is int and previous <= tick <= ticks, 'invalid sample ordering')
        previous = tick
        for key in ('boss_hp', 'boss_max_hp', 'player_hp'):
            number(sample.get(key), 'sample ' + key)
        require(sample['boss_hp'] <= sample['boss_max_hp'] + .01, 'sample HP exceeds max HP')
    require(samples[0]['tick'] == 0 and samples[-1]['tick'] == ticks, 'missing start/final samples')
    require(abs(samples[0]['boss_hp'] - hp) < .01, 'initial sample differs from initial HP')
    outcome = report['outcome']
    if outcome in ('BOSS_DEAD', 'MUTUAL_DEATH'):
        require(samples[-1]['boss_hp'] == 0, 'boss death lacks final zero HP')
    if outcome in ('PLAYER_DEAD', 'MUTUAL_DEATH'):
        require(samples[-1]['player_hp'] == 0 and report.get('ttk_seconds') is None,
                'player death cannot have successful TTK')
        if outcome == 'PLAYER_DEAD':
            require(samples[-1]['boss_hp'] > 0, 'both dead requires mutual outcome')
    else:
        require(samples[-1]['player_hp'] > 0 and report['damage_dealt'] > 0,
                'boss victory needs a surviving participant and confirmed player damage')
        ttk = number(report.get('ttk_seconds'), 'TTK', True)
        require(math.isclose(ttk, wall, rel_tol=1e-5), 'TTK does not match complete recording')
    return report, digest


def import_evidence(matrix, evidence):
    require(isinstance(matrix, dict) and matrix.get('schema') == 1, 'unsupported matrix')
    result = copy.deepcopy(matrix)
    index = {(c['registry_id'], c['class'], c['level'], c['build']): c for c in result['cases']}
    require(len(index) == len(result['cases']), 'duplicate matrix case')
    require(all(c['status'] == 'NOT_RUN' for c in result['cases']),
            'use original NOT_RUN plan and supply all evidence; do not incrementally overwrite trials')
    sessions = set()
    for raw, review in evidence:
        report, digest = validate_report(raw, review)
        require(report['session'] not in sessions, 'duplicate session evidence')
        sessions.add(report['session'])
        initial = report['initial']
        case = index.get((initial['registry_id'], initial['class'], initial['level'], initial['declared_build']))
        require(case is not None, 'report does not match a planned case')
        case['status'] = 'MEASURED_REVIEWED'
        case.setdefault('trials', []).append({
            'session': report['session'], 'report_sha256': digest, 'outcome': report['outcome'],
            'ttk_seconds': report['ttk_seconds'], 'elapsed_ticks': report['elapsed_ticks'],
            'wall_seconds': report['wall_seconds'], 'player_dps': report['player_dps_wall'],
            'damage_received': report['damage_received'],
            'boss_attributed_damage_received': report['boss_attributed_damage_received'],
            'net_health_recovered': report['net_health_recovered'],
            'sampled_resource_depletion': report['sampled_resource_depletion'],
            'sampled_stamina_depletion': report['sampled_stamina_depletion'],
            'forge_version': report['forge_version'], 'mods': report['mods'],
            'runtime_scope': review['runtime_scope'], 'review': copy.deepcopy(review),
        })
    measured = sum(c['status'] == 'MEASURED_REVIEWED' for c in result['cases'])
    result['coverage'] = 'Reviewed native-selection trials; missing cases remain NOT_RUN. Not release approval.'
    result['coverage_summary'] = {'measured_cases': measured, 'planned_cases': len(index),
                                  'reviewed_trials': len(sessions), 'not_run_cases': len(index) - measured}
    result['release_ready'] = False
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--matrix', required=True, type=Path)
    parser.add_argument('--report', required=True, action='append', type=Path)
    parser.add_argument('--review', required=True, action='append', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    temporary = None
    try:
        require(len(args.report) == len(args.review), 'each report requires its corresponding review')
        require(args.output.resolve() not in {p.resolve() for p in [args.matrix, *args.report, *args.review]},
                'output must not overwrite input evidence')
        result = import_evidence(strict_json(args.matrix.read_bytes()),
                                 [(p.read_bytes(), strict_json(r.read_bytes()))
                                  for p, r in zip(args.report, args.review)])
        args.output.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile(mode='w', dir=args.output.parent, delete=False) as handle:
            temporary = Path(handle.name)
            json.dump(result, handle, indent=2, allow_nan=False)
            handle.write('\n')
        os.replace(temporary, args.output)
        print(json.dumps(result['coverage_summary']))
    except (ValueError, KeyError, TypeError, OSError) as error:
        parser.exit(2, 'Evidence rejected: ' + str(error) + '\n')
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()


if __name__ == '__main__':
    main()
