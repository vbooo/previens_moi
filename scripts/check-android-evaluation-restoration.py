"""Controlled pending evaluation + real process death. Uses the last actual local Android preview.
No new provider call is reachable; only a test evaluation payload is temporarily changed.
"""
import json,re,subprocess,sys,time,xml.etree.ElementTree as ET
from pathlib import Path
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R/'service/tests'));import integration as local
PKG='fr.previensmoi.prototype.remote';ADB=str(R/'.local/sdk/platform-tools/adb')
def adb(*args):return subprocess.run([ADB,'-s','emulator-5554',*args],capture_output=True,check=True,timeout=25).stdout
def nodes():
 adb('shell','uiautomator','dump','/data/local/tmp/previens-moi-s3-evaluation.xml')
 try:return list(ET.fromstring(adb('exec-out','cat','/data/local/tmp/previens-moi-s3-evaluation.xml')).iter('node'))
 except ET.ParseError:return []
row=local.sql("select id::text,payload from private.evaluations where payload->>'status'='unsupported' order by created_at desc limit 1")
assert row,'Run real local Android test first'
identity,old=row[0];pending={'id':identity,'status':'pending','draftRevision':old['draftRevision'],'updatedAt':local.ISO()}
report={'scope':'controlled local pending response; real Android process death; no AI/provider activation','passed':False,'newProviderCalls':0}
try:
 assert not local.sql('select enabled from private.admission_capacity',one=True)
 local.sql('update private.evaluations set payload=%s where id=%s',(local.Jsonb(pending),identity))
 before=local.sql('select count(*) from private.evaluations',one=True)
 encrypted=adb('exec-out','run-as',PKG,'cat','no_backup/evaluation.enc')
 assert len(encrypted)>=29 and 'Préviens-moi des annonces de ma salle'.encode() not in encrypted
 adb('shell','am','force-stop',PKG);adb('shell','am','start','-n',PKG+'/fr.previensmoi.app.MainActivity')
 tapped=False
 for _ in range(12):
  for node in nodes():
   if node.get('enabled')=='true' and node.get('text') in ['Prévisualiser le suivi','Preview this watch']:
    bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
    if len(bounds)==4 and bounds[3]>bounds[1]:adb('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2));tapped=True;break
  if tapped:break
  time.sleep(.25)
 assert tapped,'Preview button must be available after process restart'
 shown=False
 for _ in range(15):
  texts=[n.get('text','') for n in nodes()]
  if any(t.startswith(('Analyse encore en attente.','Analysis is still pending.')) for t in texts):shown=True;break
  time.sleep(.25)
 after=local.sql('select count(*) from private.evaluations',one=True)
 assert shown,'Same analysis must stay pending, not become a new unsupported evaluation'
 assert before==after,'A new evaluation UUID must never be generated after process death'
 report.update(passed=True,sameEvaluationIdentifier=True,noDuplicateEvaluation=True,encryptedPreviewRestored=True,pendingShown=True)
finally:
 local.sql('update private.evaluations set payload=%s where id=%s',(local.Jsonb(old),identity))
 (R/'validation/s3/android-evaluation-restoration.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
