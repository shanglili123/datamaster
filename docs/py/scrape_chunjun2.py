import urllib.request, ssl, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=30, context=ctx).read().decode('utf-8', 'ignore')

def ls(path):
    u = 'https://api.github.com/repos/DTStack/chunjun/contents/' + path
    try:
        data = json.loads(fetch(u))
        return [(it.get('type'), it.get('name'), it.get('path')) for it in data]
    except Exception as e:
        return [('ERR', str(e), path)]

for p in ['docs_zh', 'docs_zh/docs', 'docs_zh/connectors', 'docs_zh/docs/connectors']:
    print('==', p)
    for t,n,pp in ls(p):
        print('  ', t, n)
