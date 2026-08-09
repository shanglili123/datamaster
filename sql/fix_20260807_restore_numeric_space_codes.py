# -*- coding: utf-8 -*-
"""
Fix space codes: revert letter codes (BANK_*) back to numeric DolphinScheduler
project codes, in tax_space and every denormalized space_code column.

Background: on 2026-07-28/29 tax_space.code was changed from the numeric DS
project code to letter slugs and propagated to all business tables. Backend
passes spaceCode directly to DolphinScheduler as {projectCode}
(DsRequestUtils.replaceProjectCode) and even parses it as Long in
CollectorEtlTaskServiceImpl, so publishing/executing probe and develop tasks
broke.

Mapping (by space id, original codes taken from datamaster_dump.sql):
  1 BANK_CUSTOMER_ACCOUNT      -> 174954643786848  (DS project basic_data_group)
  2 BANK_TRANSACTION_PAYMENT   -> 152317955043040
  3 BANK_RISK_COMPLIANCE       -> 152317966055136

Usage: python fix_space_codes.py [--apply]
"""
import sys
import psycopg2

APPLY = '--apply' in sys.argv

MAPPING = {
    'BANK_CUSTOMER_ACCOUNT': '174954643786848',
    'BANK_TRANSACTION_PAYMENT': '152317955043040',
    'BANK_RISK_COMPLIANCE': '152317966055136',
}

conn = psycopg2.connect(host='192.168.93.174', port=5432, dbname='datamaster',
                        user='datamaster', password='datamaster')
cur = conn.cursor()

# find all tables with a space_code column (plus tax_space.code itself)
cur.execute("""select table_name from information_schema.columns
where table_schema='public' and column_name='space_code' order by table_name""")
tables = [r[0] for r in cur.fetchall()]
print('tables with space_code (%d):' % len(tables))
print('  ', tables)

# tables still carrying legacy project_code columns with letter codes
cur.execute("""select table_name from information_schema.columns
where table_schema='public' and column_name='project_code' order by table_name""")
legacy_tables = [r[0] for r in cur.fetchall()]
print('tables with project_code (%d):' % len(legacy_tables))
print('  ', legacy_tables)

plan = []
for t in tables:
    cur.execute('select count(*) from "%s" where space_code in %s'
                % (t, tuple(MAPPING.keys())))
    n = cur.fetchone()[0]
    if n:
        plan.append((t, 'space_code', n))

for t in legacy_tables:
    cur.execute('select count(*) from "%s" where project_code in %s'
                % (t, tuple(MAPPING.keys())))
    n = cur.fetchone()[0]
    if n:
        plan.append((t, 'project_code', n))

cur.execute('select count(*) from tax_space where code in %s' % (tuple(MAPPING.keys()),))
n = cur.fetchone()[0]
if n:
    plan.append(('tax_space', 'code', n))

print('\nupdate plan:')
for t, c, n in plan:
    print('  %s.%s -> %d rows' % (t, c, n))

if not APPLY:
    print('\nDRY RUN only. Re-run with --apply to execute.')
    conn.close()
    sys.exit(0)

total = 0
for t, c, n in plan:
    for old, new in MAPPING.items():
        cur.execute('update "%s" set "%s"=%%s where "%s"=%%s' % (t, c, c), (new, old))
    total += n
conn.commit()
print('updated %d rows across %d tables' % (total, len(plan)))

# verify no letter codes remain
leftover = 0
for t, c, n in plan:
    cur.execute('select count(*) from "%s" where "%s" in %s'
                % (t, c, tuple(MAPPING.keys())))
    leftover += cur.fetchone()[0]
print('leftover letter-code rows:', leftover)
cur.execute('select id, name, code, worker_group from tax_space order by id')
for r in cur.fetchall():
    print('tax_space:', r)
conn.close()
