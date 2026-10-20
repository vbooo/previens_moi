#!/usr/bin/env python3
"""Destructive to DEMO app data only: real process death, DataStore + saved navigation.
Run after installing demoDebug on a dedicated emulator. No network/provider calls.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--serial", required=True)
parser.add_argument("--adb", required=True)
parser.add_argument("--output", required=True, type=Path)
args = parser.parse_args()
package = "fr.previensmoi.prototype.demo"
draft = "Saved draft after process death"

def adb(*parts):
    return subprocess.run([args.adb, "-s", args.serial, *parts], check=True, capture_output=True, timeout=25).stdout

def shell(*parts):
    return adb("shell", *parts).decode().strip()

def tree():
    shell("uiautomator", "dump", "/sdcard/previens-moi-process.xml")
    raw = adb("exec-out", "cat", "/sdcard/previens-moi-process.xml")
    try:
        return ET.fromstring(raw)
    except ET.ParseError:
        # UIAutomator can return before its first dump exists while the app starts.
        return ET.Element("not-ready")

def await_tree(predicate):
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        value = tree()
        if predicate(value):
            return value
        time.sleep(0.4)
    raise AssertionError("Expected visible state not reached")

def contains(root, text):
    return any(text in n.get("text", "") for n in root.iter("node"))

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1+x2)//2), str((y1+y2)//2))

args.output.mkdir(parents=True, exist_ok=True)
shell("settings", "put", "secure", "stylus_handwriting_enabled", "0")
shell("pm", "clear", package)
shell("cmd", "locale", "set-app-locales", package, "--user", "0", "--locales", "en")
shell("cmd", "uimode", "night", "no")
shell("settings", "put", "system", "font_scale", "1.0")
shell("am", "start", "-n", package + "/fr.previensmoi.app.MainActivity")
root = await_tree(lambda r: any(n.get("class") == "android.widget.EditText" for n in r.iter("node")))
tap(next(n for n in root.iter("node") if n.get("class") == "android.widget.EditText"))
await_tree(lambda r: any(n.get("class") == "android.widget.EditText" and n.get("focused") == "true" for n in r.iter("node")))
shell("input", "text", draft.replace(" ", "%s"))
await_tree(lambda r: contains(r, draft))
if "mInputShown=true" in shell("dumpsys", "input_method"):
    shell("input", "keyevent", "KEYCODE_BACK")
root = await_tree(lambda r: contains(r, "Preview this watch"))
tap(next(n for n in root.iter("node") if n.get("text") == "Preview this watch"))
await_tree(lambda r: contains(r, "Your proposed watch"))
before = shell("pidof", package)
assert before, "Application process was absent before kill"
shell("am", "start", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
await_tree(lambda r: any(n.get("package") == "com.google.android.apps.nexuslauncher" for n in r.iter("node")))
# Background transition writes Activity saved state; the proposal screen above proves evaluation completed.
time.sleep(1)
shell("am", "kill", package)
# am kill is allowed to defer a cached process; run-as can terminate this debuggable app's own UID.
check = subprocess.run([args.adb, "-s", args.serial, "shell", "pidof", package], capture_output=True)
method = "background -> am kill -> launch existing task"
if check.stdout.strip():
    shell("run-as", package, "kill", "-9", before)
    method = "background -> kill -9 under demo UID -> launch existing task"
for _ in range(20):
    check = subprocess.run([args.adb, "-s", args.serial, "shell", "pidof", package], capture_output=True)
    if not check.stdout.strip():
        break
    time.sleep(0.25)
else:
    raise AssertionError("Process remained alive; this is not a process-death test")
shell("am", "start", "-n", package + "/fr.previensmoi.app.MainActivity")
root = await_tree(lambda r: contains(r, draft) and contains(r, "Demo reset."))
after = shell("pidof", package)
assert before != after
assert not contains(root, "Your proposed watch"), "Old proposal must not be confirmable"
assert not contains(root, "Active in demo"), "Restore must not activate anything"
(args.output / "process-restored.png").write_bytes(adb("exec-out", "screencap", "-p"))
result = {"status": "passed", "device": args.serial, "old_pid": before, "new_pid": after,
          "draft_restored": True, "reset_explained": True, "old_proposal_confirmable": False,
          "automatic_activation": False, "method": method}
(args.output / "process-restoration.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2))
