"""Audit exact CurseForge manifest binaries. Metadata inventory is NOT runtime validation."""
import argparse
import concurrent.futures
import hashlib
import io
import json
import re
import time
import tomllib
import urllib.request
import zipfile
from pathlib import Path
from inspect_combat_mods import includes_version


def inspect_archive(data, origin='ROOT', depth=0):
    if depth > 8:
        raise ValueError('nested archive depth exceeds audit limit')
    result = {'sha256': hashlib.sha256(data).hexdigest(), 'mods': [], 'loaders': [], 'findings': []}
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        names = set(archive.namelist())
        manifest = archive.read('META-INF/MANIFEST.MF').decode(errors='replace') if 'META-INF/MANIFEST.MF' in names else ''
        manifest = re.sub(r'\r?\n ', '', manifest)
        if 'META-INF/mods.toml' in names:
            metadata = tomllib.loads(archive.read('META-INF/mods.toml').decode())
            result['loaders'].append({'loader': metadata.get('modLoader'),
                                      'range': metadata.get('loaderVersion'), 'origin': origin})
            for entry in metadata.get('mods', []):
                version = entry.get('version')
                if version == '${file.jarVersion}':
                    match = re.search(r'^Implementation-Version: (.+)$', manifest, re.M)
                    version = match[1].strip() if match else None
                result['mods'].append({'modId': entry['modId'], 'version': version, 'origin': origin,
                                       'dependencies': metadata.get('dependencies', {}).get(entry['modId'], [])})
        elif not re.search(r'^FMLModType: (LIBRARY|LANGPROVIDER|GAMELIBRARY)\s*$', manifest, re.M):
            result['findings'].append({'kind': 'UNSUPPORTED_ARCHIVE', 'origin': origin})
        if 'META-INF/jarjar/metadata.json' in names:
            nested = json.loads(archive.read('META-INF/jarjar/metadata.json'))
            for entry in nested.get('jars', []):
                path = entry['path']
                child = inspect_archive(archive.read(path), origin + '/' + path, depth + 1)
                for key in ('mods', 'loaders', 'findings'):
                    result[key].extend(child[key])
    return result


def manifest_entries(manifest):
    minecraft = manifest.get('minecraft', {})
    loaders = minecraft.get('modLoaders', [])
    if minecraft.get('version') != '1.20.1' or len(loaders) != 1 or loaders[0].get('id') != 'forge-47.4.10':
        raise ValueError('requires supplied Minecraft1.20.1/Forge47.4.10 manifest')
    entries = manifest.get('files', [])
    seen = set()
    if not entries:
        raise ValueError('empty modpack manifest')
    for entry in entries:
        project, file = entry.get('projectID'), entry.get('fileID')
        if type(project) is not int or type(file) is not int or project <= 0 or file <= 0 or project in seen:
            raise ValueError('invalid/duplicate manifest project or file pin')
        seen.add(project)
    return entries


def range_result(version, requirement):
    # Do not silently flatten Maven qualifiers or unions into numeric versions.
    if not isinstance(version, str) or not isinstance(requirement, str):
        return None
    if not re.fullmatch(r'\d+(?:\.\d+)*', version) or not re.fullmatch(r'[\d.,\[\]() ]+', requirement):
        return None
    if requirement.count(',') > 1:
        return None
    try:
        return includes_version(version, requirement)
    except (ValueError, IndexError):
        return None


def audit_manifest(manifest, directory):
    reports, findings = [], []
    installed = {'minecraft': '1.20.1', 'forge': '47.4.10'}
    owners = {}
    for entry in manifest_entries(manifest):
        path = Path(directory) / f"{entry['projectID']}-{entry['fileID']}.jar"
        if not path.is_file():
            raise ValueError('missing exact manifest file: ' + path.name)
        report = inspect_archive(path.read_bytes())
        report.update(projectID=entry['projectID'], fileID=entry['fileID'])
        reports.append(report)
        findings.extend({'file': path.name, **finding} for finding in report['findings'])
        for loader in report['loaders']:
            compatible = range_result('47.4.10', loader['range']) if loader['loader'] in ('javafml', 'lowcodefml') else None
            if compatible is not True:
                findings.append({'kind': 'LOADER_RANGE' if compatible is False else 'LANGUAGE_LOADER_REQUIRES_RUNTIME_VALIDATION',
                                 'file': path.name, **loader})
        for mod in report['mods']:
            identifier = mod['modId']
            if identifier in owners:
                previous = owners[identifier]
                kind = ('DUPLICATE_MOD_ID' if previous['origin'] == mod['origin'] == 'ROOT'
                        else 'NESTED_DUPLICATE_REQUIRES_RUNTIME_RESOLUTION')
                findings.append({'kind': kind, 'modId': identifier, 'file': path.name,
                                 'previousVersion': installed[identifier], 'version': mod['version']})
            else:
                installed[identifier] = mod['version']
                owners[identifier] = mod
    for report in reports:
        for mod in report['mods']:
            for dependency in mod['dependencies']:
                if not dependency.get('mandatory', False):
                    continue
                identifier = dependency['modId']
                finding = {'modId': mod['modId'], 'dependency': identifier,
                           'side': dependency.get('side', 'BOTH'), 'required': dependency.get('versionRange'),
                           'installed': installed.get(identifier)}
                if identifier not in installed:
                    findings.append({'kind': 'MISSING_DEPENDENCY', **finding})
                else:
                    compatible = range_result(installed[identifier], dependency.get('versionRange'))
                    if compatible is not True:
                        findings.append({'kind': 'INCOMPATIBLE_VERSION' if compatible is False
                                         else 'RANGE_REQUIRES_RUNTIME_VALIDATION', **finding})
    return {'schema': 1, 'status': 'INVENTORY_WITH_FINDINGS' if findings else 'INVENTORY_COMPLETE',
            'forge': '47.4.10', 'resolved_files': len(reports), 'files': reports,
            'installed_mods': installed, 'dependency_findings': findings,
            'runtime_validated': False, 'release_ready': False,
            'limitations': 'Includes client/server declared dependencies and nested descriptors. No game boot, '
                           'full fights, configs, JarJar resolution or Maven qualifier ordering proven.'}


def download_entry(entry, directory):
    project, file = entry['projectID'], entry['fileID']
    descriptor = f'mod-{project}'
    url = f'https://cursemaven.com/curse/maven/{descriptor}/{file}/{descriptor}-{file}.jar'
    path = directory / f'{project}-{file}.jar'
    temporary = path.with_suffix('.download')
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=90) as response:
                temporary.write_bytes(response.read())
            with zipfile.ZipFile(temporary) as archive:
                archive.namelist()
            temporary.replace(path)
            return
        except (OSError, zipfile.BadZipFile):
            temporary.unlink(missing_ok=True)
            if attempt == 2:
                raise
            time.sleep(attempt + 1)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--jars', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--download', action='store_true')
    args = parser.parse_args()
    raw = args.manifest.read_bytes()
    manifest = json.loads(raw)
    entries = manifest_entries(manifest)
    args.jars.mkdir(parents=True, exist_ok=True)
    if args.download:
        failures = []
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            jobs = {pool.submit(download_entry, entry, args.jars): entry for entry in entries}
            for future in concurrent.futures.as_completed(jobs):
                entry = jobs[future]
                try:
                    future.result()
                    print(f"RESOLVED {entry['projectID']}:{entry['fileID']}", flush=True)
                except Exception as error:
                    failures.append({'projectID': entry['projectID'], 'fileID': entry['fileID'], 'error': str(error)})
        if failures:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps({'status': 'INCOMPLETE_DOWNLOAD', 'failures': failures,
                                               'runtime_validated': False, 'release_ready': False}, indent=2) + '\n')
            raise RuntimeError(f'{len(failures)} exact files unavailable; no substituted versions')
    result = audit_manifest(manifest, args.jars)
    result['manifest_sha256'] = hashlib.sha256(raw).hexdigest()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'status': result['status'], 'resolved_files': result['resolved_files'],
                      'findings': len(result['dependency_findings']), 'runtime_validated': False}))


if __name__ == '__main__':
    main()
