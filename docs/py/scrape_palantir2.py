import urllib.request, ssl, re, html as htmllib, os

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE


def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=30, context=ctx).read().decode('utf-8', 'ignore')


def strip_to_text(h):
    h = re.sub(r'<script.*?</script>', ' ', h, flags=re.S)
    h = re.sub(r'<style.*?</style>', ' ', h, flags=re.S)
    m = re.search(r'<main[^>]*>(.*?)</main>', h, re.S)
    body = m.group(1) if m else h
    body = re.sub(r'<(h[1-6]|p|li|br|div|section|td|tr|th)[^>]*>', '\n', body, flags=re.I)
    body = re.sub(r'<[^>]+>', '', body)
    body = htmllib.unescape(body)
    lines = [ln.strip() for ln in body.split('\n')]
    lines = [ln for ln in lines if ln]
    # 去掉侧边栏导航噪声:正文一般在 "注意：以下翻译" 之后
    txt = '\n'.join(lines)
    idx = txt.find('机器翻译')
    if idx > 0:
        txt = txt[idx:]
    return txt


urls = {
    'edits-how-applied': 'https://palantir.com/docs/zh/foundry/object-edits/how-edits-applied/',
    'edits-materializations': 'https://palantir.com/docs/zh/foundry/object-edits/materializations/',
    'edits-overview': 'https://palantir.com/docs/zh/foundry/object-edits/overview/',
    'action-overview': 'https://palantir.com/docs/zh/foundry/action-types/overview/',
    'action-webhooks': 'https://palantir.com/docs/zh/foundry/action-types/webhooks/',
}

outdir = os.path.join(os.path.dirname(__file__), 'palantir_docs')
os.makedirs(outdir, exist_ok=True)

for name, url in urls.items():
    try:
        h = fetch(url)
    except Exception as e:
        print(name, 'FAIL', e)
        continue
    txt = strip_to_text(h)
    fn = os.path.join(outdir, name + '.txt')
    with open(fn, 'w', encoding='utf-8') as f:
        f.write('URL: ' + url + '\n\n' + txt)
    print(name, 'len=', len(txt))
