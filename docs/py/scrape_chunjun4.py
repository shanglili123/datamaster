import urllib.request, ssl
import urllib.parse

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch(u):
    req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=40, context=ctx).read().decode('utf-8', 'ignore')

raw = 'https://raw.githubusercontent.com/DTStack/chunjun/master/'
files = [
    'docs/docs_en/ChunJun Connector/kafka/kafka-sink.md',
    'chunjun-examples/json/binlog/binlog_kafka_partition_by_key.json',
    'chunjun-examples/json/kafka/mysql_kafka.json',
    'chunjun-connectors/chunjun-connector-kafka/src/main/java/com/dtstack/chunjun/connector/kafka/sink/PartitionStrategy.java',
    'chunjun-connectors/chunjun-connector-kafka/src/main/java/com/dtstack/chunjun/connector/kafka/conf/KafkaConfig.java',
]
for f in files:
    print('\n\n########## ', f, ' ##########')
    try:
        print(fetch(raw + urllib.parse.quote(f)))
    except Exception as e:
        print('FAIL', e)

import urllib.parse
