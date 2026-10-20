"""Destructive only in a new disposable DB of the dedicated local S3 stack."""
import datetime as dt,json,os,subprocess,uuid
from pathlib import Path
import psycopg
R=Path(__file__).resolve().parents[1]
DOCKER='/Applications/Docker.app/Contents/Resources/bin/docker';CONTAINER='supabase_db_previens-moi-s3'
BASE='postgresql://postgres:postgres@127.0.0.1:56322/'
BACKUP=R/'.local/s3-backup';BACKUP.mkdir(exist_ok=True)
def run(*args,**kwargs):return subprocess.run(args,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kwargs)
def main():
 assert json.loads((R/'.local/s3-local-status.json').read_text())['API_URL']=='http://127.0.0.1:56321'
 target='s3_restore_'+uuid.uuid4().hex[:12];owner=str(uuid.uuid4())
 # Disposable identity existing before snapshot, erased after it: resurrected snapshots must deny it.
 with psycopg.connect(BASE+'postgres') as c:
  c.execute("insert into auth.users(id,instance_id,aud,role,email,created_at,updated_at) values(%s,'00000000-0000-0000-0000-000000000000','authenticated','authenticated',%s,now(),now())",(owner,'restore-'+uuid.uuid4().hex+'@example.test'))
  c.execute('insert into private.accounts(id) values(%s)',(owner,))
 archive=BACKUP/'database.dump'
 with archive.open('wb') as output:
  subprocess.run([DOCKER,'exec',CONTAINER,'pg_dump','-U','postgres','-d','postgres','--format=custom','--schema=auth','--schema=private','--schema=public','--schema=extensions','--no-owner','--no-privileges'],stdout=output,stderr=subprocess.PIPE,check=True)
 os.chmod(archive,0o600)
 with psycopg.connect(BASE+'postgres') as c:
  c.execute('insert into private.erasure_ledger(owner_id) values(%s)',(owner,))
  markers=c.execute('select owner_id,erased_at from private.erasure_ledger').fetchall()
 marker_file=BACKUP/'erasure-ledger.json';marker_file.write_text(json.dumps([{'id':str(u),'at':t.isoformat()} for u,t in markers]));os.chmod(marker_file,0o600)
 created=False
 try:
  run(DOCKER,'exec',CONTAINER,'createdb','-U','postgres',target);created=True
  run(DOCKER,'exec',CONTAINER,'psql','-U','postgres','-d',target,'-c','drop schema public cascade')
  with archive.open('rb') as source:
   result=subprocess.run([DOCKER,'exec','-i',CONTAINER,'pg_restore','-U','postgres','-d',target,'--no-owner','--no-privileges','--exit-on-error'],stdin=source,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
  if result.returncode:
   # Only static diagnostics; never print restored contents or private values.
   (BACKUP/'restore-error.txt').write_bytes(result.stderr)
   raise RuntimeError('Local restore failed; inspect ignored diagnostic file')
  with psycopg.connect(BASE+target) as c:
   assert c.execute('select count(*) from private.accounts where id=%s',(owner,)).fetchone()[0]==1
   assert c.execute('select count(*) from private.erasure_ledger where owner_id=%s',(owner,)).fetchone()[0]==0
   for marker in json.loads(marker_file.read_text()):c.execute('insert into private.erasure_ledger(owner_id,erased_at) values(%s,%s) on conflict do nothing',(marker['id'],marker['at']))
   c.execute('select public.s3_replay_erasure()')
  blocked=False
  try:
   with psycopg.connect(BASE+target) as c:
    c.execute("select set_config('request.jwt.claim.sub',%s,true)",(owner,));c.execute('select private.owner()')
  except psycopg.errors.RaiseException:blocked=True
  assert blocked
  report={'passed':True,'createdAt':dt.datetime.now(dt.timezone.utc).isoformat(),'backupOutsideDatabaseVolume':True,'restoredInSeparateDisposableDatabase':True,'erasureAfterSnapshotReplayed':True,'oldIdentityDenied':True,'archiveBytes':archive.stat().st_size,'remoteBackup':False,'limits':'Local dump/restore proves reconstruction and erasure replay, not independent hosting, encryption-key recovery or cloud recovery time.'}
  (R/'validation/s3/local-backup-restore.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
 finally:
  if created:run(DOCKER,'exec',CONTAINER,'dropdb','-U','postgres',target)
  with psycopg.connect(BASE+'postgres') as c:
   c.execute('delete from private.accounts where id=%s',(owner,));c.execute('delete from auth.users where id=%s',(owner,))
if __name__=='__main__':main()
