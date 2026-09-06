import urllib.request, ssl, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=40, context=ctx).read().decode('utf-8', 'ignore')

# find default branch
repo = json.loads(fetch('https://api.github.com/repos/DTStack/chunjun'))
br = repo.get('default_branch')
print('default_branch=', br)

tree = json.loads(fetch('https://api.github.com/repos/DTStack/chunjun/git/trees/%s?recursive=1' % br))
paths = [t['path'] for t in tree.get('tree', []) if t['type']=='blob']
kafka = [p for p in paths if 'kafka' in p.lower()]
print('--- kafka-related files ---')
for p in kafka:
    print(' ', p)
