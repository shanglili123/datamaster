# -*- coding: utf-8 -*-
"""
Revert water-themed names in tax_category back to bank-themed names.

Root cause: the 2026-08-07 restore used datamaster_dump.sql (16:26, pre-rename,
water-themed). datamaster_dump_20260729.sql (16:55) already contains the
bank-themed names. The rename happened 7/29 morning (before the 7/30 migration).

Matching strategy (safe with duplicate codes):
  key = (cat_type, code, current_name) -> new_name from new dump,
  verified against old dump: current name must equal old dump name for same code.

Updates rows with id >= 2080000000000000000 (restored yesterday).

Usage: python fix_cat_names_bank.py [--apply]
"""
import sys
import re
import psycopg2

APPLY = '--apply' in sys.argv

SOURCES = [
    ('tax_api_cat', 'API'),
    ('tax_model_cat', 'MODEL'),
    ('tax_data_elem_cat', 'DATA_ELEM'),
    ('tax_data_dev_cat', 'DATA_DEV'),
    ('tax_document_cat', 'DOCUMENT'),
    ('tax_quality_cat', 'QUALITY'),
    ('tax_tag_cat', 'TAG'),
    ('tax_task_cat', 'TASK'),
    ('tax_discover_task_cat', 'TASK'),
    ('tax_clean_cat', 'CLEAN'),
    ('tax_asset_cat', 'ASSET'),
]
DESC_POS = {t: 4 for t, _ in SOURCES}
DESC_POS['tax_quality_cat'] = 5
CODE_POS = {t: 5 for t, _ in SOURCES}
CODE_POS['tax_quality_cat'] = 4


def parse_rows(content, tbl):
    rows = []
    for m in re.finditer(r'INSERT INTO "%s" VALUES (\(.*?\));' % tbl, content):
        vals = []
        s = m.group(1)[1:-1]
        i, buf, instr = 0, '', False
        while i < len(s):
            ch = s[i]
            if instr:
                if ch == "'":
                    if i + 1 < len(s) and s[i + 1] == "'":
                        buf += "'"
                        i += 2
                        continue
                    instr = False
                    i += 1
                    continue
                buf += ch
            else:
                if ch == "'":
                    instr = True
                elif ch == ',':
                    vals.append(buf.strip())
                    buf = ''
                else:
                    buf += ch
            i += 1
        vals.append(buf.strip())
        rows.append(vals)
    return rows


def unq(v):
    return None if v == 'NULL' else v


old_content = open(r'd:\dev\dataMaster\sql\datamaster_dump.sql', encoding='utf-8').read()
new_content = open(r'd:\dev\dataMaster\sql\datamaster_dump_20260729.sql', encoding='utf-8').read()

# maps: (cat_type, code) -> dict(old_name -> (new_name, new_desc))
pair_map = {}
for tbl, cat_type in SOURCES:
    old_rows = parse_rows(old_content, tbl)
    new_rows = parse_rows(new_content, tbl)
    new_by_id = {}
    for vals in new_rows:
        try:
            new_by_id[int(vals[0])] = vals
        except ValueError:
            pass
    for vals in old_rows:
        try:
            rid = int(vals[0])
        except ValueError:
            continue
        code = unq(vals[CODE_POS[tbl]])
        old_name = unq(vals[1])
        if code is None:
            continue
        nv = new_by_id.get(rid)
        if nv is None:
            continue
        key = (cat_type, code)
        pair_map.setdefault(key, {})[old_name] = (unq(nv[1]), unq(nv[DESC_POS[tbl]]))
        # also register whitespace-normalized key to tolerate stray spaces/tabs
        pair_map.setdefault(key, {})[old_name.strip()] = (unq(nv[1]), unq(nv[DESC_POS[tbl]]))

print('pair_map keys: %d' % len(pair_map))

conn = psycopg2.connect(host='192.168.93.174', port=5432, dbname='datamaster',
                        user='datamaster', password='datamaster')
cur = conn.cursor()
cur.execute("""select id, cat_type, name, code, description from tax_category
where id >= 2080000000000000000 order by cat_type, id""")
restored = cur.fetchall()
print('restored rows: %d' % len(restored))

updates, ambiguous, unmatched, already = [], [], [], 0
for rid, cat_type, name, code, desc in restored:
    cands = pair_map.get((cat_type, code))
    if not cands:
        unmatched.append((rid, cat_type, code, name))
        continue
    if name in cands:
        new_name, new_desc = cands[name]
    elif name.strip() in cands:
        new_name, new_desc = cands[name.strip()]
    else:
        ambiguous.append((rid, cat_type, code, name, list(cands.keys())))
        continue
    if new_name == name and new_desc == desc:
        already += 1
    else:
        updates.append((new_name, new_desc, rid, name, new_name))

print('to update: %d | already ok: %d | unmatched: %d | ambiguous: %d'
      % (len(updates), already, len(unmatched), len(ambiguous)))
for u in updates[:15]:
    print('  UPDATE %s: %r -> %r' % (u[2], u[3], u[4]))
for a in ambiguous:
    print('  AMBIGUOUS:', a)
for u2 in unmatched:
    print('  UNMATCHED:', u2)

if APPLY and updates:
    cur.executemany('update tax_category set name=%s, description=%s where id=%s',
                    [(u[0], u[1], u[2]) for u in updates])
    conn.commit()
    print('APPLIED: %d rows updated' % len(updates))
else:
    conn.rollback()
    print('dry run (use --apply to execute)')
conn.close()
