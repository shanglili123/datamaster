import json

with open('/app/packages/dbgpt-ext/src/dbgpt_ext/connector/catalog.json') as f:
    d = json.load(f)

for c in d.get('connectors', []):
    print(f"{c.get('type', '?'):20s} {c.get('label', '?')}")
