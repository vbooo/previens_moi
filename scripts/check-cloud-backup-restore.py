#!/usr/bin/env python3
"""Requires explicit approval for cloud Auth/business export. Never writes to cloud.
Creates an encrypted copy on this Mac and tests reconstruction in a disposable DB.
It does not configure a new host, deploy restored data or enable background execution.
"""
import datetime as dt,hashlib,hmac,json,os,secrets,subprocess,urllib.parse,uuid
from pathlib import Path
import psycopg
R=Path(__file__).resolve().parents[1];DOCKER='/Applications/Docker.app/Contents/Resources/bin/docker';CONTAINER='supabase_db_previens-moi-s3';LOCAL='postgresql://postgres:postgres@127.0.0.1:56322/'
def run(args,**kw):return subprocess.run(args,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kw)
def fingerprints(c):
 out={}
 for schema,table in c.execute("select schemaname,tablename from pg_tables where schemaname in ('private','auth') order by 1,2").fetchall():
  q=psycopg.sql.SQL("select count(*),md5(coalesce(string_agg(h,'' order by h),'')) from (select md5(to_jsonb(t)::text) h from {}.{} t) s").format(psycopg.sql.Identifier(schema),psycopg.sql.Identifier(table))
  out[schema+'.'+table]=c.execute(q).fetchone()
 return out

def main():
 if '--approved-sensitive-export' not in os.sys.argv:raise SystemExit('Approval required: export includes Auth identities/sessions and application data.')
 credentials={}
 for line in (R/'.local/cloud/supabase.env').read_text().splitlines():
  k,sep,v=line.partition('=')
  if sep:credentials[k]=v.strip().strip('\"\'')
 uri=(R/'service/supabase/.temp/pooler-url').read_text().strip();u=urllib.parse.urlparse(uri)
 assert u.hostname and u.hostname.endswith('.pooler.supabase.com') and 'sfmifkbofiwslkvjtmaj' in (u.username or '')
 stamp=dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ');folder=R/'.local/cloud-backups'/stamp;folder.mkdir(parents=True,mode=0o700)
 keys=R/'.local/backup-keys';keys.mkdir(mode=0o700,exist_ok=True);keyfile=keys/(stamp+'.key');keyfile.write_bytes(secrets.token_hex(32).encode());keyfile.chmod(0o600)
 target='s3_cloud_restore_'+uuid.uuid4().hex[:10];created=False;report={'createdAt':stamp,'passed':False,'cloudModified':False,'restoredIntoNewCloudProject':False,'scope':'Cloud application/Auth snapshot reconstructed in a disposable local database. No managed-platform failover, Vault/SMTP/Firebase key backup or protection against loss of this Mac.'}
 try:
  child=dict(os.environ,PGPASSWORD=credentials['SUPABASE_DB_PASSWORD'],PGSSLMODE='require')
  with psycopg.connect(uri,password=credentials['SUPABASE_DB_PASSWORD'],sslmode='require',connect_timeout=15) as cloud:
   cloud.execute('set transaction isolation level repeatable read read only');cloud.execute("set time zone 'UTC'");before=fingerprints(cloud);snapshot=cloud.execute('select pg_export_snapshot()').fetchone()[0]
   raw=run([DOCKER,'exec','-e','PGPASSWORD','-e','PGSSLMODE',CONTAINER,'pg_dump','-h',u.hostname,'-p',str(u.port or 5432),'-U',urllib.parse.unquote(u.username or ''),'-d',(u.path or '/postgres').lstrip('/'),'--snapshot='+snapshot,'--format=custom','--schema=auth','--schema=private','--schema=public','--schema=extensions','--no-owner','--no-privileges'],env=child).stdout
  assert 0<len(raw)<=20_000_000
  encrypted=run(['openssl','enc','-aes-256-cbc','-pbkdf2','-iter','200000','-salt','-pass','file:'+str(keyfile)],input=raw).stdout
  archive=folder/'application.dump.enc';archive.write_bytes(encrypted);archive.chmod(0o600)
  # Separate HMAC domain; verify authentication before allowing CBC decryption.
  mac_key=hashlib.sha256(b'previens-moi-backup-integrity-v1\0'+keyfile.read_bytes()).digest();tag=hmac.digest(mac_key,encrypted,'sha256');tagfile=folder/'application.dump.mac';tagfile.write_bytes(tag);tagfile.chmod(0o600)
  assert not hmac.compare_digest(tag,hmac.digest(mac_key,encrypted[:-1]+bytes([encrypted[-1]^1]),'sha256'))
  assert hmac.compare_digest(tagfile.read_bytes(),hmac.digest(mac_key,archive.read_bytes(),'sha256'))
  decrypted=run(['openssl','enc','-d','-aes-256-cbc','-pbkdf2','-iter','200000','-pass','file:'+str(keyfile)],input=archive.read_bytes()).stdout;assert decrypted==raw
  run([DOCKER,'exec',CONTAINER,'createdb','-U','postgres',target]);created=True
  run([DOCKER,'exec',CONTAINER,'psql','-U','postgres','-d',target,'-c','drop schema public cascade'])
  restored=subprocess.run([DOCKER,'exec','-i',CONTAINER,'pg_restore','-U','postgres','-d',target,'--no-owner','--no-privileges','--exit-on-error'],input=decrypted,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
  if restored.returncode:
   diagnostic=folder/'restore-error.txt';diagnostic.write_bytes(restored.stderr);diagnostic.chmod(0o600);raise RuntimeError('Private restore diagnostic saved')
  with psycopg.connect(LOCAL+target) as local:
   local.execute("set time zone 'UTC'");assert before==fingerprints(local),'Snapshot row fingerprints differ'
   for table in ['capacity','admission_capacity','worker_schedule','page_change_pilot']:local.execute(psycopg.sql.SQL('update private.{} set enabled=false').format(psycopg.sql.Identifier(table)))
   local.execute('update private.method_qualifications set qualified=false')
   if local.execute("select to_regclass('private.general_budget') is not null").fetchone()[0]:local.execute('update private.general_budget set enabled=false,qualified=false')
  report.update(passed=True,tableCount=len(before),rows=sum(n for n,_ in before.values()),fingerprintsMatch=True,snapshotConsistent=True,archiveBytes=len(encrypted),encrypted=True,integrityVerifiedBeforeDecrypt=True,tamperedArchiveRejected=True,keySeparatedFromArchive=True,executionClosedInRestoredDatabase=True,originalCloudUntouched=True)
 except Exception as e:
  report['failure']=type(e).__name__
  if isinstance(e,subprocess.CalledProcessError):
   f=folder/'command-error.txt';f.write_bytes(e.stderr or b'');f.chmod(0o600)
 finally:
  if created:run([DOCKER,'exec',CONTAINER,'dropdb','-U','postgres',target])
  (R/'validation/s3/cloud-backup-restore.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
 if not report['passed']:raise SystemExit(1)
if __name__=='__main__':main()
