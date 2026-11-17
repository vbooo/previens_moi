#!/usr/bin/env python3
"""Create the local Play upload identity once, without displaying passwords.
This is an upload key, not the final app-signing key managed by Google Play.
No network, deployment or purchase. Never overwrites an existing identity.
"""
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
DESTINATION = ROOT / '.local/play-signing'


def main():
    if DESTINATION.exists():
        raise SystemExit('Upload identity already exists; preserve it. Nothing changed.')
    keytool = ROOT / '.local/jdk/Contents/Home/bin/keytool'
    if not keytool.is_file():
        raise SystemExit('Local JDK keytool missing. Nothing changed.')
    DESTINATION.parent.mkdir(exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix='play-signing-', dir=DESTINATION.parent))
    password = secrets.token_hex(32)
    env = dict(os.environ, PREVIENS_UPLOAD_PASSWORD=password)
    try:
        result = subprocess.run([
            str(keytool), '-genkeypair', '-keystore', str(work / 'upload-keystore.p12'),
            '-storetype', 'PKCS12', '-alias', 'upload', '-keyalg', 'RSA',
            '-keysize', '3072', '-validity', '10000',
            '-dname', 'CN=Previens-moi Upload, OU=Android, O=Previens-moi, C=FR',
            '-storepass:env', 'PREVIENS_UPLOAD_PASSWORD',
            '-keypass:env', 'PREVIENS_UPLOAD_PASSWORD',
        ], env=env, capture_output=True)
        if result.returncode:
            raise RuntimeError('Upload identity generation failed; no settings installed.')
        for filename, extra in [('upload-certificate.der', []), ('upload-certificate.pem', ['-rfc'])]:
            result = subprocess.run([
                str(keytool), '-exportcert', '-keystore', str(work / 'upload-keystore.p12'),
                '-alias', 'upload', '-storepass:env', 'PREVIENS_UPLOAD_PASSWORD',
                '-file', str(work / filename), *extra,
            ], env=env, capture_output=True)
            if result.returncode:
                raise RuntimeError('Public upload certificate export failed; no settings installed.')
        (work / 'signing.properties').write_text(
            '# Private upload credentials. Never commit, share or print.\n'
            'storeFile=../.local/play-signing/upload-keystore.p12\n'
            'keyAlias=upload\n'
            'storePassword=' + password + '\n'
            'keyPassword=' + password + '\n'
        )
        for item in work.iterdir():
            item.chmod(0o600)
        work.rename(DESTINATION)
        print('Upload identity and public certificates created in .local/play-signing; no secret displayed.')
        print('Keep the keystore and signing.properties together in a secure independent backup.')
        print('No Google Play app-signing enrollment, publication or purchase performed.')
    finally:
        if work.exists():
            shutil.rmtree(work)


if __name__ == '__main__':
    main()
