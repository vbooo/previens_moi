#!/usr/bin/env python3
"""Build/test Android against only the dedicated local Supabase stack; no secrets printed."""
import json
import os
from pathlib import Path
import subprocess
import sys
root=Path(__file__).resolve().parents[1]
status=json.loads((root/'.local/s3-local-status.json').read_text())
assert status['API_URL']=='http://127.0.0.1:56321'
env=dict(os.environ,JAVA_HOME=str(root/'.local/jdk/Contents/Home'),GRADLE_USER_HOME=str(root/'.local/gradle-home'))
props=['-PlocalService=true','-PserviceUrl=http://10.0.2.2:56321/functions/v1/','-PauthUrl=http://10.0.2.2:56321/auth/v1/','-PauthPublicKey='+status['ANON_KEY']]
tasks=sys.argv[1:] or ['assembleRemoteDebug','connectedRemoteDebugAndroidTest']
result=subprocess.run([str(root/'android/gradlew'),'-p',str(root/'android'),*props,*tasks,'--max-workers=2','--console=plain'],env=env)
raise SystemExit(result.returncode)
