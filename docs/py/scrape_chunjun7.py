import urllib.request, ssl, urllib.parse

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=40, context=ctx).read().decode('utf-8', 'ignore')

raw = 'https://raw.githubusercontent.com/DTStack/chunjun/master/'
files = [
    'docs/docs_en/ChunJun Connector/mysql/mysql-source.md',
    'chunjun-examples/json/mysql/mysql_mysql_realtime.json',
    'chunjun-examples/json/mysql/mysql_hdfs_polling.json',
]
for f in files:
    print('\n########## ', f, ' ##########')
    try:
        print(fetch(raw + urllib.parse.quote(f)))
    except Exception as e:
        print('FAIL', e)
