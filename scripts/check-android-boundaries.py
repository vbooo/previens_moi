#!/usr/bin/env python3
"""Small explicit boundary guard; compilation and behavior tests remain necessary."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1] / "android/app/src"
errors = []
for path in root.rglob("*.kt"):
    if path.relative_to(root).parts[0].startswith(("test", "androidTest")): continue
    imports = re.findall(r"^import (.+)$", path.read_text(), re.M)
    if "/domain/" in str(path):
        allowed = ("kotlin.", "kotlinx.coroutines.", "fr.previensmoi.domain.")
        errors += [f"{path}: forbidden domain import {i}" for i in imports if not i.startswith(allowed)]
    if "/presentation/" in str(path):
        errors += [f"{path}: presentation imports data {i}" for i in imports if i.startswith("fr.previensmoi.data.")]
for path in root.rglob("AndroidManifest.xml"):
    for permission in ET.parse(path).getroot().findall("uses-permission"):
        name = permission.get("{http://schemas.android.com/apk/res/android}name")
        action = permission.get("{http://schemas.android.com/tools}node")
        if name == "android.permission.INTERNET" and action != "remove" and "/remote/" not in str(path):
            errors.append(f"{path}: demo network permission")
if "--merged" in sys.argv:
    manifest = root.parent / "build/intermediates/merged_manifests/demoDebug/processDemoDebugManifest/AndroidManifest.xml"
    if not manifest.exists(): errors.append("Build demoDebug before checking its merged manifest")
    else:
        permissions = [e.get("{http://schemas.android.com/apk/res/android}name") for e in ET.parse(manifest).getroot().findall("uses-permission")]
        if "android.permission.INTERNET" in permissions: errors.append("Merged demo manifest has INTERNET permission")
if errors: raise SystemExit("\n".join(errors))
print("Android boundaries: passed (domain, presentation, source manifest)")
