import urllib.request, ssl, re, html as htmllib

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE


def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=30, context=ctx).read().decode('utf-8', 'ignore')


def strip_to_text(h):
    # cut out scripts/styles
    h = re.sub(r'<script.*?</script>', ' ', h, flags=re.S)
    h = re.sub(r'<style.*?</style>', ' ', h, flags=re.S)
    # try to isolate <main> or article
    m = re.search(r'<main[^>]*>(.*?)</main>', h, re.S)
    body = m.group(1) if m else h
    # convert block tags to newlines
    body = re.sub(r'<(h[1-6]|p|li|br|div|section|td|tr|th)[^>]*>', '\n', body, flags=re.I)
    body = re.sub(r'<[^>]+>', '', body)
    body = htmllib.unescape(body)
    lines = [ln.strip() for ln in body.split('\n')]
    lines = [ln for ln in lines if ln]
    return '\n'.join(lines)


urls = {
    'virtual-tables': 'https://palantir.com/docs/zh/foundry/data-integration/virtual-tables/',
    'object-storage-v2': 'https://palantir.com/docs/zh/foundry/object-backend/object-storage-v2-breaking-changes/',
    'ontology-overview': 'https://palantir.com/docs/zh/foundry/ontology/overview/',
}

import os
outdir = os.path.join(os.path.dirname(__file__), 'palantir_docs')
os.makedirs(outdir, exist_ok=True)

for name, url in urls.items():
    try:
        h = fetch(url)
    except Exception as e:
        print(name, 'FETCH FAIL', e)
        continue
    txt = strip_to_text(h)
    fn = os.path.join(outdir, name + '.txt')
    with open(fn, 'w', encoding='utf-8') as f:
        f.write('URL: ' + url + '\n\n' + txt)
    print(name, 'textLen=', len(txt), '->', fn)
