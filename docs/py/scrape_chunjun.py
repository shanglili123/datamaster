import urllib.request, ssl, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=30, context=ctx).read().decode('utf-8', 'ignore')

# 1) list connectors dir to find kafka doc path
apis = [
    'https://api.github.com/repos/DTStack/chunjun/contents/docs/connectors',
    'https://api.github.com/repos/DTStack/chunjun/contents/docs',
]
for a in apis:
    try:
        data = json.loads(fetch(a))
        print('==', a)
        for it in data:
            print(' ', it.get('type'), it.get('name'))
    except Exception as e:
        print('FAIL', a, e)
