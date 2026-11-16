#!/usr/bin/env python3
"""Check the optimized connected APK. --build-fixture makes an offline CI artifact,
not a distributable client: public placeholders never identify a working backend.
Production signing, Play policy and actual provider delivery remain separate gates.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'fr.previensmoi.prototype.remote'


def fixture_settings():
    def b64(value):
        return base64.urlsafe_b64encode(json.dumps(value).encode()).decode().rstrip('=')
    return {
        'localService': 'false',
        'firebasePackageName': 'fr.previensmoi.app',
        'serviceUrl': 'https://release-check.example.test/functions/v1/',
        'authUrl': 'https://release-check.example.test/auth/v1/',
        'authPublicKey': b64({'alg': 'HS256'}) + '.' + b64({'role': 'anon'}) + '.not-a-real-signature',
        'firebaseAppId': '1:123456789:android:abcdef0123456789',
        'firebaseProjectId': 'release-check-fixture',
        'firebaseApiKey': 'public-fixture-not-a-real-api-key',
        'firebaseSenderId': '123456789',
    }


def run(command):
    result = subprocess.run([str(x) for x in command], capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError('Android artifact inspection command failed.')
    return result.stdout


def inspect(apk, sdk, mapping, package=PACKAGE):
    aapt = sdk / 'build-tools/36.0.0/aapt2'
    manifest = run([aapt, 'dump', 'xmltree', apk, '--file', 'AndroidManifest.xml'])
    badging = run([aapt, 'dump', 'badging', apk])
    resources = run([aapt, 'dump', 'resources', apk])
    network_path = re.search(r'xml/remote_network_security\s+\(\) \(file\) (\S+) type=XML', resources)
    if not network_path:
        raise RuntimeError('Release network policy missing from the compiled resources.')
    network = run([aapt, 'dump', 'xmltree', apk, '--file', network_path[1]])
    truthy = lambda attribute, text: bool(re.search(attribute + r'\([^\n]*?\)=(?:true|0xffffffff|\(type 0x12\)0xffffffff)', text))
    checks = {
        'connectedPackage': "package: name='" + package + "'" in badging,
        'notDebuggable': not truthy('debuggable', manifest),
        'backupsDisabled': 'allowBackup(0x01010280)=false' in manifest and 'fullBackupContent(0x010104eb)=false' in manifest,
        'httpsOnlyIncludingLoopback': 'cleartextTrafficPermitted' in network and not truthy('cleartextTrafficPermitted', network) and 'domain-config' not in network,
        'internetPermission': "name='android.permission.INTERNET'" in badging,
        'notificationPermission': "name='android.permission.POST_NOTIFICATIONS'" in badging,
        'noSensitiveDevicePermission': not any('android.permission.' + p + "'" in badging for p in ('READ_CONTACTS','ACCESS_FINE_LOCATION','RECORD_AUDIO','CAMERA','READ_SMS')),
        'r8MappingPresent': mapping.is_file() and mapping.stat().st_size > 0,
    }
    forbidden_classes = ('fr.previensmoi.data.demo.', 'fr.previensmoi.app.DemoModule',
                         'fr.previensmoi.app.DemoViewModel', 'androidx.compose.ui.tooling.ComposeViewAdapter')
    mapping_text = mapping.read_text() if mapping.exists() else ''
    checks['noDemoOrPreviewClasses'] = not any(re.search('^' + re.escape(c), mapping_text, re.M) for c in forbidden_classes)
    with zipfile.ZipFile(apk) as archive:
        checks['noConfigurationOrFixtureFiles'] = not any(
            x.endswith(('google-services.json', 'firebase-service-account.json', '.env', '.keystore', '.jks'))
            or x.startswith('assets/journey-') for x in archive.namelist())
        payloads = [archive.read(x) for x in archive.namelist() if x.endswith(('.dex', '.json', '.properties'))]
        checks['noPrivateKeyOrProviderKeyMarkers'] = not any(
            re.search(rb'-----BEGIN (?:RSA |EC )?PRIVATE KEY-----|sk-proj-[A-Za-z0-9_-]{20,}|fc-[A-Za-z0-9]{25,}', data)
            for data in payloads)
        roles = []
        for data in payloads:
            for token in re.findall(rb'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_.-]+', data):
                try:
                    roles.append(json.loads(base64.urlsafe_b64decode(token.split(b'.')[1] + b'===' )).get('role'))
                except (ValueError, TypeError, UnicodeDecodeError):
                    pass
        checks['noPrivilegedSupabaseJwt'] = 'service_role' not in roles
    return {
        'artifactSha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
        'apkBytes': apk.stat().st_size,
        'checks': checks,
        'passed': all(checks.values()),
        'limits': ['Static binary checks and compilation do not prove real notification delivery or seven days of operation.',
                   'Unsigned artifact; no production signing identity created or configured.'],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build-fixture', action='store_true')
    parser.add_argument('--sdk', type=Path, default=Path(os.environ.get('ANDROID_HOME', ROOT / '.local/sdk')))
    parser.add_argument('--apk', type=Path, default=ROOT / 'android/app/build/outputs/apk/remote/release/app-remote-release-unsigned.apk')
    parser.add_argument('--report', type=Path, default=ROOT / 'android/app/build/reports/release-check.json')
    parser.add_argument('--play', action='store_true', help='Check the definitive Play package with separate client settings')
    args = parser.parse_args()
    if args.build_fixture:
        settings = ['-P' + name + '=' + value for name, value in fixture_settings().items()]
        if args.play: settings.append('-PplayApplicationId=fr.previensmoi.app')
        command = [str(ROOT / 'android/gradlew'), '-p', str(ROOT / 'android'), '--no-daemon', '--max-workers=2',
                   'assembleRemoteRelease', 'bundleRemoteRelease', 'lintRemoteRelease', *settings]
        # Exercise refusals against deliberately synthetic values, never real secrets.
        invalid = (
            {'localService': 'true'},
            {'serviceUrl': 'http://127.0.0.1/functions/v1/'},
            {'firebaseAppId': ''},
            {'authPublicKey': 'sb_secret_not-a-client-key'},
        )
        for override in invalid:
            values = {**fixture_settings(), **override}
            refusal = subprocess.run(
                [str(ROOT / 'android/gradlew'), '-p', str(ROOT / 'android'), '--no-daemon',
                 'validateRemoteReleaseConfiguration', *['-P' + k + '=' + v for k, v in values.items()]],
                capture_output=True, text=True,
            )
            if refusal.returncode == 0 or 'Release requires' not in refusal.stdout + refusal.stderr:
                raise RuntimeError('Release configuration refusal not verified: ' + next(iter(override)))
        subprocess.run(command, check=True)
    result = inspect(args.apk, args.sdk, ROOT / 'android/app/build/outputs/mapping/remoteRelease/mapping.txt', 'fr.previensmoi.app' if args.play else PACKAGE)
    result['configurationKind'] = 'offline-public-fixture' if args.build_fixture else 'supplied-client-configuration'
    result['productionReady'] = False
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))
    raise SystemExit(0 if result['passed'] else 1)


if __name__ == '__main__':
    main()
