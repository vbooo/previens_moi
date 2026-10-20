#!/usr/bin/env python3
"""Controlled pending fixture + real process stop. Local Supabase/emulator only.
Run after LocalServiceTest using direct adb instrumentation (keeps app data installed).
This is not a real provider activation or a new paid trial.
"""
import copy
import json
from pathlib import Path
import shlex
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'service/tests'))
import integration as local
PKG='fr.previensmoi.prototype.remote'
ADB=str(ROOT/'.local/sdk/platform-tools/adb')
def adb(*args,timeout=20):return subprocess.run([ADB,'-s','emulator-5554',*args],capture_output=True,timeout=timeout,check=True).stdout
owner=local.sql("select id::text from auth.users where email ~ '^android-[0-9]+@example.test$' order by created_at desc limit 1",one=True)
assert owner,'First run the real local email-code Android test'
fixtures=json.loads((ROOT/'contracts/application-v1.examples.json').read_text())
def plan(v):
 if isinstance(v,dict):
  if all(k in v for k in ['criteria','draft','method','expiresAt']):return v
  for item in v.values():
   found=plan(item)
   if found:return found
 if isinstance(v,list):
  for item in v:
   found=plan(item)
   if found:return found
p=copy.deepcopy(plan(fixtures));p['id']=str(local.uuid.uuid4());p['expiresAt']=(local.NOW()+local.dt.timedelta(hours=1)).isoformat().replace('+00:00','Z')
watch=str(local.uuid.uuid4());command=local.v7();report={'scope':'injected pending local fixture; real Android process death; no provider activation','passed':False,'newProviderCalls':0}
try:
 local.sql("insert into private.watches(id,owner_id,plan,status,remote_state) values(%s,%s,%s,'pending','starting')",(watch,owner,local.Jsonb(p)))
 receipt={'id':command,'state':'pending','watchId':watch,'updatedAt':local.ISO(),'remoteState':'starting'}
 local.sql('insert into private.commands(owner_id,id,fingerprint,receipt) values(%s,%s,%s,%s)',(owner,command,'restoration-fixture',local.Jsonb(receipt)))
 local.sql("insert into private.jobs(watch_id,command_id,generation,kind) values(%s,%s,1,'start')",(watch,command))
 adb('shell','am','force-stop',PKG)
 shell='run-as '+PKG+' sh -c '+shlex.quote("printf '%s' "+command+' > no_backup/command-'+owner)
 adb('shell',shell)
 before=local.sql('select count(*) from private.commands where owner_id=%s',(owner,),True)
 adb('shell','am','start','-n',PKG+'/fr.previensmoi.app.MainActivity')
 shown=False
 for _ in range(12):
  adb('shell','uiautomator','dump','/data/local/tmp/previens-moi-s3-restore.xml')
  xml=adb('exec-out','cat','/data/local/tmp/previens-moi-s3-restore.xml')
  texts=[n.get('text','') for n in ET.fromstring(xml).iter('node')]
  if any(t in ['Preparing','En préparation'] for t in texts):shown=True;break
  time.sleep(.5)
 assert shown,'Pending watch must be rendered after authenticated reload'
 stored=adb('exec-out','run-as',PKG,'cat','no_backup/command-'+owner).decode()
 after=local.sql('select count(*) from private.commands where owner_id=%s',(owner,),True)
 assert stored==command and before==after==1,'No new command identity may be created'
 image=adb('exec-out','screencap','-p')
 (ROOT/'validation/s3/android-phone/remote-pending-restored.png').write_bytes(image)
 report.update(passed=True,samePendingIdentifier=True,noDuplicateCommand=True,pendingShown=True,sessionRestoredFromKeystore=True)
finally:
 local.sql('delete from private.commands where owner_id=%s and id=%s',(owner,command))
 local.sql('delete from private.jobs where watch_id=%s',(watch,))
 local.sql('delete from private.watches where id=%s',(watch,))
 try:adb('shell','run-as',PKG,'rm','-f','no_backup/command-'+owner)
 except subprocess.SubprocessError:pass
 (ROOT/'validation/s3/android-process-restoration.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
