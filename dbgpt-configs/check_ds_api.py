import json

with open('/tmp/api.json') as f:
    d = json.load(f)

paths = d['paths']
# Check POST datasources
ds = paths.get('/api/v2/serve/datasources', {})
print('=== POST /api/v2/serve/datasources ===')
print(json.dumps(ds.get('post', {}), indent=2, ensure_ascii=False)[:3000])

print('\n\n=== GET /api/v2/serve/datasources ===')
print(json.dumps(ds.get('get', {}), indent=2, ensure_ascii=False)[:2000])

# Check schema for datasource types
schemas = d.get('components', {}).get('schemas', {})
print('\n\n=== Datasource schemas ===')
for name in ['DatasourceCreateRequest', 'DatasourceServeRequest', 'DatasourceQueryResponse', 'DatasourceType']:
    if name in schemas:
        print(f'\n{name}:')
        print(json.dumps(schemas[name], indent=2, ensure_ascii=False)[:1500])

# Also check datasource-types endpoint
print('\n\n=== datasource-types ===')
dst = paths.get('/api/v2/serve/datasource-types', {})
print(json.dumps(dst, indent=2, ensure_ascii=False)[:2000])
