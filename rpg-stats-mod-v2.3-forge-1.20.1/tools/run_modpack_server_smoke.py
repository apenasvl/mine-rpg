"""Normal dedicated-server boot with supplied native pack. Never an encounter or client test."""
import argparse
import hashlib
import json
import os
import queue
import re
import shutil
import signal
import subprocess
import threading
import time
from pathlib import Path
from audit_modpack_manifest import audit_manifest, download_entry, manifest_entries
from run_production_boss_tests import validated_runtime_forge

DONE = re.compile(r'\[minecraft/DedicatedServer\]: Done \([0-9.]+s\)!')


def validated_boot(log, exit_code):
    try:
        version = validated_runtime_forge(log, '47.4.10')
    except RuntimeError as error:
        raise ValueError(str(error)) from error
    if exit_code != 0 or not DONE.search(log) or 'Stopping server' not in log:
        raise ValueError('normal dedicated-server boot and clean shutdown not proven')
    return version


def stage_modpack(inventory, jars, configuration, main_jar, server):
    if server.exists() and any(server.iterdir()):
        raise ValueError('existing server content would invalidate exact-pack staging; use a fresh work directory')
    mods = server / 'mods'; mods.mkdir(parents=True, exist_ok=True)
    resources = server / 'resourcepacks'; resources.mkdir(exist_ok=True)
    count = resource_count = 0
    for entry in inventory['files']:
        kind = entry['archive_kind']
        if kind not in ('FORGE_MOD', 'FORGE_LIBRARY_CONTAINER', 'RESOURCE_PACK'):
            raise ValueError('cannot stage unclassified archive')
        project, file = entry['projectID'], entry['fileID']
        if type(project) is not int or type(file) is not int or project <= 0 or file <= 0:
            raise ValueError('invalid native file pin')
        name = f'{project}-{file}'
        source = jars / (name + '.jar')
        if kind == 'RESOURCE_PACK':
            shutil.copyfile(source, resources / (name + '.zip')); resource_count += 1
        else:
            shutil.copyfile(source, mods / (name + '.jar')); count += 1
    shutil.copyfile(main_jar, mods / 'rpgstats.jar')
    for relative, text in configuration['files'].items():
        path = Path(relative)
        if path.is_absolute() or '..' in path.parts or not isinstance(text, str):
            raise ValueError('invalid configuration path/text')
        target = server / 'config' / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(text.encode('utf-8'))
    return {'scope': 'NORMAL_DEDICATED_SERVER_SMOKE', 'native_mod_files': count,
            'resource_packs_staged': resource_count, 'config_files_staged': len(configuration['files']),
            'excluded_nontext_configs': configuration['excluded_nontext'],
            'redacted_private_config': configuration.get('redacted_private', []),
            'client_validated': False, 'encounters_validated': False, 'release_ready': False,
            'limitations': 'Linux dedicated server/new normal world; no players or fights. Resource packs '
                           'are staged separately, not activated on a client. Windows nontext config is excluded.'}


def run_server(server, timeout=600, cleanup_grace=15):
    # Process group makes bounded cleanup include the launcher and its Java child.
    process = subprocess.Popen(['bash', 'run.sh', 'nogui'], cwd=server, stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                               encoding='utf-8', errors='replace', start_new_session=True)
    lines = queue.Queue()

    def consume():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)

    reader = threading.Thread(target=consume, daemon=True); reader.start()
    deadline, started, stopped = time.monotonic() + timeout, None, False
    captured = []
    try:
        while process.poll() is None:
            now = time.monotonic()
            if now > deadline:
                raise TimeoutError('normal server startup/shutdown exceeded timeout')
            if started is not None and now - started >= 10 and not stopped:
                process.stdin.write('list\nstop\n'); process.stdin.flush(); stopped = True
            try:
                line = lines.get(timeout=.5)
            except queue.Empty:
                continue
            if line is None:
                break
            captured.append(line)
            print(line, end='', flush=True)
            if DONE.search(line) and started is None:
                started = now
        return_code = process.wait(timeout=30)
        reader.join(timeout=5)
        while not lines.empty():
            line = lines.get_nowait()
            if line is not None:
                captured.append(line)
        log = ''.join(captured)
        (server / 'smoke-console.log').write_text(log)
        return return_code, log
    finally:
        if process.poll() is None or reader.is_alive():
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            reader.join(timeout=cleanup_grace)
            # A terminated launcher does not prove its Java child terminated.
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait(timeout=15)
            reader.join(timeout=5)
        while not lines.empty():
            line = lines.get_nowait()
            if line is not None:
                captured.append(line)
        if not (server / 'smoke-console.log').exists():
            (server / 'smoke-console.log').write_text(''.join(captured))
        process.stdin.close(); process.stdout.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--snapshot', required=True, type=Path)
    parser.add_argument('--rpg-jar', required=True, type=Path)
    parser.add_argument('--installer', required=True, type=Path)
    parser.add_argument('--work', required=True, type=Path)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    result = {'status': 'FAIL', 'scope': 'NORMAL_DEDICATED_SERVER_SMOKE',
              'client_validated': False, 'encounters_validated': False, 'release_ready': False}
    try:
        manifest_bytes = (args.snapshot / 'manifest.json').read_bytes()
        manifest = json.loads(manifest_bytes)
        jars = args.work / 'native-files'; jars.mkdir(exist_ok=True)
        for entry in manifest_entries(manifest):
            download_entry(entry, jars)
        inventory = audit_manifest(manifest, jars)
        (args.work / 'native-inventory.json').write_text(json.dumps(inventory, indent=2) + '\n')
        configuration = json.loads((args.snapshot / 'configs.json').read_text())
        server = args.work / 'server'
        result.update(stage_modpack(inventory, jars, configuration, args.rpg_jar, server))
        result['main_jar_sha256'] = hashlib.sha256(args.rpg_jar.read_bytes()).hexdigest()
        result['manifest_sha256'] = hashlib.sha256(manifest_bytes).hexdigest()
        result['configs_snapshot_sha256'] = hashlib.sha256((args.snapshot / 'configs.json').read_bytes()).hexdigest()
        shutil.copyfile(args.installer, server / 'forge-installer.jar')
        subprocess.run(['java', '-jar', 'forge-installer.jar', '--installServer'], cwd=server,
                       check=True, timeout=600)
        (server / 'eula.txt').write_text('eula=true\n')
        (server / 'user_jvm_args.txt').write_text('-Xmx4G\n')
        (server / 'server.properties').write_text('online-mode=false\nserver-ip=127.0.0.1\ndifficulty=normal\nlevel-seed=0\n'
                                               'view-distance=3\nsimulation-distance=3\nmax-tick-time=120000\n')
        exit_code, log = run_server(server)
        result['exit_code'] = exit_code
        result['runtime_forge'] = validated_boot(log, exit_code)
        result['status'] = 'SERVER_BOOT_PASS'
    except Exception as error:
        result['error'] = str(error)
        raise
    finally:
        (args.work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')


if __name__ == '__main__':
    main()
