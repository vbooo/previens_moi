#!/usr/bin/env python3
"""Offline validation of the API specification and positive/negative wire fixtures.
Does not prove authentication, transaction isolation, provider quality or deployment.
"""
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker
from referencing import Registry, Resource
from openapi_spec_validator import validate

ROOT = Path(__file__).resolve().parents[1]
spec = json.loads((ROOT / 'contracts/application-v1.openapi.json').read_text())
cases = json.loads((ROOT / 'contracts/application-v1.examples.json').read_text())
validate(spec)
snapshot = ROOT / 'service/supabase/functions/_shared/contract.json'
if snapshot.exists():
    assert json.loads(snapshot.read_text()) == spec, 'Edge contract snapshot is stale'
uri = 'https://contracts.previens-moi.invalid/application-v1'
resource = Resource.from_contents(dict(spec, **{'$schema': 'https://json-schema.org/draft/2020-12/schema'}))
registry = Registry().with_resource(uri, resource)
for case in cases:
    schema = {'$ref': uri + '#/components/schemas/' + case['schema']}
    validator = Draft202012Validator(schema, registry=registry, format_checker=FormatChecker())
    errors = list(validator.iter_errors(case['payload']))
    assert (not errors) == case['valid'], f"Unexpected result for {case['name']}: {[e.message for e in errors]}"
operations = [op for path in spec['paths'].values() for op in path.values()]
assert len({op['operationId'] for op in operations}) == len(operations)
for path, methods in spec['paths'].items():
    for operation in methods.values():
        if path.startswith('/api/'):
            assert operation['security'] == [{'UserJWT': []}], path
        elif path not in ('/public/v1/health', '/public/v1/preview-sessions', '/public/v1/support'):
            assert operation['security'] == [{'PreviewToken': []}], path
assert all('.invalid/' in server['url'] for server in spec['servers']), 'No deployed environment claimed'
print(json.dumps({'openapi': 'valid', 'operations': len(operations), 'schemas': len(spec['components']['schemas']),
                  'fixtures_passed': len(cases), 'positive': sum(c['valid'] for c in cases),
                  'negative': sum(not c['valid'] for c in cases),
                  'network_calls': 0, 'scope': 'specification and examples only'}, indent=2))
