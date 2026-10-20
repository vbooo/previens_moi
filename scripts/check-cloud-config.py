#!/usr/bin/env python3
"""Inspect dedicated cloud configuration without printing keys or contacting providers."""
import base64,json,os,re,subprocess,sys
from pathlib import Path
R=Path(__file__).resolve().parents[1];C=R/'.local/cloud';PACKAGE='fr.previensmoi.prototype.remote'
def settings():
 values={};path=C/'supabase.env'
 if path.exists():
  for line in path.read_text().splitlines():
   if line.strip() and not line.lstrip().startswith('#'):
    name,sep,value=line.partition('=')
    if sep:
     value=value.strip()
     if len(value)>=2 and value[0]==value[-1] and value[0] in (chr(34),chr(39)):value=value[1:-1]
     values[name.strip()]=value
 return values

def inspect():
 env=settings();checks={};url=env.get('SUPABASE_URL','');m=re.fullmatch(r'https://([a-z0-9]{20})\.supabase\.co',url)
 checks['dedicatedSupabaseUrl']=bool(m)
 for name,role in [('SUPABASE_ANON_KEY','anon'),('SUPABASE_SERVICE_ROLE_KEY','service_role')]:
  try:
   key=env.get(name,'');assert re.fullmatch(r'[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+',key)
   payload=json.loads(base64.urlsafe_b64decode(key.split('.')[1]+'==='));checks[name]=payload.get('role')==role and (not m or payload.get('ref')==m.group(1))
  except Exception:checks[name]=False
 checks['databasePassword']=bool(env.get('SUPABASE_DB_PASSWORD'))
 public=None;account=None;client=None
 try:
  public=json.loads((C/'google-services.json').read_text());client=next(c for c in public['client'] if c['client_info']['android_client_info']['package_name']==PACKAGE)
  checks['firebaseAndroidApp']=bool(re.fullmatch(r'[a-z][a-z0-9-]{4,62}',public['project_info']['project_id']) and re.fullmatch(r'\d+',public['project_info']['project_number']) and re.fullmatch(r'[A-Za-z0-9_:.-]+',client['client_info']['mobilesdk_app_id']) and re.fullmatch(r'[A-Za-z0-9_-]+',client['api_key'][0]['current_key']))
 except Exception:checks['firebaseAndroidApp']=False
 try:
  account=json.loads((C/'firebase-service-account.json').read_text());checks['firebaseServerIdentity']=bool(account.get('type')=='service_account' and account.get('project_id')==public['project_info']['project_id'] and account.get('client_email','').endswith('.iam.gserviceaccount.com') and account.get('private_key','').startswith('-----BEGIN PRIVATE KEY-----'))
 except Exception:checks['firebaseServerIdentity']=False
 for path in [C/'supabase.env',C/'firebase-service-account.json']:
  if path.exists():path.chmod(0o600)
 return env,public,client,checks

def main():
 env,public,client,checks=inspect();complete=all(checks.values())
 report={'configurationComplete':complete,'checks':checks,'remoteAccessVerified':False,'cloudDeployed':False,'methodQualificationVerified':False,'networkCalls':0,'note':'Files only; this does not validate IAM, project quotas, email delivery or real FCM.'}
 (R/'validation/s3/external-readiness.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
 if '--build' not in sys.argv:return
 if not complete:raise SystemExit('Dedicated configuration incomplete; nothing built or deployed')
 props={'localService':'false','serviceUrl':env['SUPABASE_URL']+'/functions/v1/','authUrl':env['SUPABASE_URL']+'/auth/v1/','authPublicKey':env['SUPABASE_ANON_KEY'],'firebaseAppId':client['client_info']['mobilesdk_app_id'],'firebaseProjectId':public['project_info']['project_id'],'firebaseApiKey':client['api_key'][0]['current_key'],'firebaseSenderId':public['project_info']['project_number']}
 # Only public Android values enter the build. The server identity never becomes a Gradle property.
 runtime=dict(os.environ,JAVA_HOME=str(R/'.local/jdk/Contents/Home'),GRADLE_USER_HOME=str(R/'.local/gradle-home'))
 result=subprocess.run([str(R/'android/gradlew'),'-p',str(R/'android'),'assembleRemoteDebug','--max-workers=2','--console=plain',*[f'-P{k}={v}' for k,v in props.items()]],env=runtime)
 raise SystemExit(result.returncode)
if __name__=='__main__':main()
