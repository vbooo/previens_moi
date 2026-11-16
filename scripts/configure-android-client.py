#!/usr/bin/env python3
"""Configure Android Studio with public mobile values only; no network or key output."""
import argparse
import base64
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "fr.previensmoi.prototype.remote"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--play", action="store_true", help="Configure fr.previensmoi.app separately from the prototype")
    args = parser.parse_args()
    package = "fr.previensmoi.app" if args.play else PACKAGE
    values = {}
    for line in (ROOT / ".local/cloud/supabase.env").read_text().splitlines():
        name, sep, value = line.partition("=")
        if sep and not name.lstrip().startswith("#"):
            values[name.strip()] = value.strip().strip("\"'")
    url = values["SUPABASE_URL"]
    match = re.fullmatch(r"https://([a-z0-9]{20})\.supabase\.co", url)
    assert match, "Dedicated Supabase URL missing"
    anon = values["SUPABASE_ANON_KEY"]
    payload = json.loads(base64.urlsafe_b64decode(anon.split(".")[1] + "==="))
    assert payload.get("role") == "anon" and payload.get("ref") == match.group(1), "Public client identity mismatch"
    firebase = json.loads((ROOT / (".local/cloud/play-google-services.json" if args.play else ".local/cloud/google-services.json")).read_text())
    if args.play:
        original = json.loads((ROOT / ".local/cloud/google-services.json").read_text())
        assert firebase["project_info"] == original["project_info"], "Use the same Firebase project for the new Android app"
    client = next(c for c in firebase["client"] if c["client_info"]["android_client_info"]["package_name"] == package)
    public = {
        "serviceUrl": url + "/functions/v1/",
        "authUrl": url + "/auth/v1/",
        "authPublicKey": anon,
        "firebasePackageName": package,
        "firebaseAppId": client["client_info"]["mobilesdk_app_id"],
        "firebaseProjectId": firebase["project_info"]["project_id"],
        "firebaseApiKey": client["api_key"][0]["current_key"],
        "firebaseSenderId": firebase["project_info"]["project_number"],
    }
    assert all(isinstance(v, str) and v and not any(c in v for c in "\r\n\\") for v in public.values()), "Invalid public value"
    target = ROOT / ("android/play-client.properties" if args.play else "android/client.properties")
    target.write_text("# Generated public Android configuration. No server keys.\n" + "".join(f"{k}={v}\n" for k, v in public.items()))
    target.chmod(0o600)
    print("Separate Play client configured; prototype unchanged. No provider calls." if args.play else "Android Studio configured for remoteDebug. Public client values only; no provider calls.")

if __name__ == "__main__":
    main()
