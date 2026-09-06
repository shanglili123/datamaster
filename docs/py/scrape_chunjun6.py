import urllib.request, ssl, json, urllib.parse

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=40, context=ctx).read().decode('utf-8', 'ignore')

# find mysql / binlog / jdbc / incremental related doc + example files
tree = json.loads(fetch('https://api.github.com/repos/DTStack/chunjun/git/trees/master?recursive=1'))
paths = [t['path'] for t in tree.get('tree', []) if t['type']=='blob']
kws = ['mysql', 'binlog', 'incre', 'restore', 'polling']
for kw in kws:
    hit = [p for p in paths if kw in p.lower() and (p.endswith('.md') or p.endswith('.json'))]
    print('===', kw, '===')
    for p in hit[:25]:
        print('  ', p)
