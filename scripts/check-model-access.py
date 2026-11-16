#!/usr/bin/env python3
"""Check local server credentials with a model GET; never generate, log a key or buy credits."""
import json
from pathlib import Path
import re
import sys
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def check():
    settings = {}
    for line in (ROOT / '.local/cloud/general-secrets.env').read_text().splitlines():
        name, separator, value = line.partition('=')
        if separator and not name.lstrip().startswith('#'):
            settings[name.strip()] = value.strip().strip("\"'")
    key = settings['OPENAI_API_KEY']
    model = re.search(r"export const MODEL='([^']+)'", (ROOT / 'service/supabase/functions/_shared/semantic.ts').read_text()).group(1)
    request = urllib.request.Request('https://api.openai.com/v1/models/' + model,
                                     method='GET', headers={'Authorization': 'Bearer ' + key})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            available = json.load(response).get('id') == model
            return {'available': available, 'status': response.status,
                    'reason': 'accessible' if available else 'unexpected_response'}
    except urllib.error.HTTPError as error:
        try:
            code = json.loads(error.read(16384)).get('error', {}).get('code')
        except (ValueError, AttributeError):
            code = None
        reason = ('expired_key' if code == 'expired_secret_key' else
                  'authentication_rejected' if error.code == 401 else
                  'permission_denied' if error.code == 403 else
                  'model_unavailable' if error.code == 404 else 'provider_unavailable')
        return {'available': False, 'status': error.code, 'reason': reason}


if __name__ == '__main__':
    try:
        result = check()
    except (OSError, KeyError, ValueError, AttributeError):
        result = {'available': False, 'status': None, 'reason': 'configuration_or_connection_unavailable'}
    result.update(paidGenerationCalls=0, scope='Local key and model-read access only; does not prove deployed-key equality, Responses permission or billing availability.')
    print(json.dumps(result))
    sys.exit(0 if result['available'] else 1)
