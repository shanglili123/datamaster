# -*- coding: utf-8 -*-
"""
Restore missing tax_category rows from datamaster_dump.sql (2026-07-29).

Root cause: migration update_20260730_unified_category_table.sql merged 12 old
TAX_*_CAT tables into tax_category with ON CONFLICT (id) DO NOTHING, but old
tables all used small integer ids starting at 1, so rows whose ids collided
with earlier-inserted tables (ASSET first, then TASK/CLEAN) were silently
dropped.

Strategy:
- Re-insert missing rows with NEW snowflake-like ids (avoid PK collision).
- Remap parent_id within each source table; also fix surviving rows whose
  parent_id points at a restored row.
- Keep original `code` values (business data references categories by code).
- space_id/space_code taken from current tax_category rows (1 /
  BANK_CUSTOMER_ACCOUNT).

Usage: python restore_tax_category.py [--apply]
"""
import re
import sys
import datetime
import psycopg2

DUMP = r'd:\dev\dataMaster\sql\datamaster_dump.sql'
APPLY = '--apply' in sys.argv

# column order per dump DDL (17 cols each)
STD_COLS = ['id', 'name', 'parent_id', 'sort_order', 'description', 'code',
            'valid_flag', 'del_flag', 'create_by', 'creator_id', 'create_time',
            'update_by', 'updater_id', 'update_time', 'remark', 'project_id',
            'project_code']
COLS = {t: STD_COLS for t in [
    'tax_model_cat', 'tax_data_elem_cat', 'tax_data_dev_cat', 'tax_document_cat',
    'tax_tag_cat', 'tax_job_cat', 'tax_discover_task_cat', 'tax_asset_cat',
    'tax_task_cat', 'tax_clean_cat']}
COLS['tax_api_cat'] = STD_COLS[:15] + ['project_code', 'project_id']
COLS['tax_quality_cat'] = ['id', 'name', 'parent_id', 'sort_order', 'code',
                           'description', 'valid_flag', 'del_flag', 'create_by',
                           'creator_id', 'create_time', 'update_by', 'updater_id',
                           'update_time', 'remark', 'project_id', 'project_code']

# source table -> cat_type
CAT_TYPE = {
    'tax_api_cat': 'API',
    'tax_model_cat': 'MODEL',
    'tax_data_elem_cat': 'DATA_ELEM',
    'tax_data_dev_cat': 'DATA_DEV',
    'tax_job_cat': 'DATA_DEV',
    'tax_document_cat': 'DOCUMENT',
    'tax_quality_cat': 'QUALITY',
    'tax_tag_cat': 'TAG',
    'tax_task_cat': 'TASK',
    'tax_discover_task_cat': 'TASK',
    'tax_clean_cat': 'CLEAN',
    'tax_asset_cat': 'ASSET',
}

# types to restore fully (all rows missing); partial types handled per-id
FULL_TYPES = {'API', 'MODEL', 'DATA_ELEM', 'DATA_DEV', 'DOCUMENT', 'QUALITY', 'TAG'}
PARTIAL_TYPES = {'TASK', 'CLEAN', 'ASSET'}


def parse_values(s):
    """Parse a SQL VALUES tuple string into python values (str/int/None)."""
    vals, i, n = [], 0, len(s)
    while i < n:
        c = s[i]
        if c in ' \t\r\n':
            i += 1
            continue
        if c == ',':
            i += 1
            continue
        if c == "'":
            j = i + 1
            buf = []
            while j < n:
                ch = s[j]
                if ch == '\\':
                    buf.append(s[j + 1])
                    j += 2
                    continue
                if ch == "'":
                    if j + 1 < n and s[j + 1] == "'":
                        buf.append("'")
                        j += 2
                        continue
                    break
                buf.append(ch)
                j += 1
            vals.append(''.join(buf))
            i = j + 1
        else:
            j = i
            while j < n and s[j] not in ',':
                j += 1
            tok = s[i:j].strip()
            if tok.upper() == 'NULL':
                vals.append(None)
            else:
                try:
                    vals.append(int(tok))
                except ValueError:
                    vals.append(tok)
            i = j
    return vals


def load_dump_rows():
    content = open(DUMP, encoding='utf-8').read()
    rows_by_table = {}
    for t in COLS:
        rows = []
        for m in re.finditer(r'INSERT INTO "' + t + r'" VALUES \((.*?)\);\s*$', content, re.M):
            vals = parse_values(m.group(1))
            if len(vals) != len(COLS[t]):
                raise SystemExit('bad row in %s: %d vals: %s' % (t, len(vals), m.group(1)[:120]))
            rows.append(dict(zip(COLS[t], vals)))
        rows_by_table[t] = rows
    return rows_by_table


def new_id_gen():
    base = 2080000000000000000
    counter = [0]
    def gen():
        counter[0] += 1
        return base + counter[0]
    return gen


def main():
    dump = load_dump_rows()
    conn = psycopg2.connect(host='192.168.93.174', port=5432, dbname='datamaster',
                            user='datamaster', password='datamaster')
    cur = conn.cursor()
    cur.execute('select id, cat_type, parent_id from tax_category')
    existing = cur.fetchall()
    existing_pairs = set((r[0], r[1]) for r in existing)
    cur.execute('select distinct space_id, space_code from tax_category')
    space_row = cur.fetchone()
    space_id, space_code = (space_row or (1, 'BANK_CUSTOMER_ACCOUNT'))

    gen = new_id_gen()
    inserts = []          # (row_dict_final)
    parent_fixes = []     # (cat_type, old_parent_id, new_parent_id)
    seen_data_dev_ids = set()

    for t in COLS:
        cat_type = CAT_TYPE[t]
        for r in dump[t]:
            old_id = r['id']
            if t == 'tax_job_cat':
                if old_id in seen_data_dev_ids:
                    continue  # legacy duplicate of tax_data_dev_cat
            if t == 'tax_data_dev_cat':
                seen_data_dev_ids.add(old_id)
            if cat_type in PARTIAL_TYPES and (old_id, cat_type) in existing_pairs:
                continue  # survived migration with matching cat_type
            inserts.append((t, cat_type, r))

    # assign new ids and build old->new map per source table
    id_map_by_table = {}
    final_rows = []
    for t, cat_type, r in inserts:
        m = id_map_by_table.setdefault(t, {})
        m[r['id']] = gen()

    for t, cat_type, r in inserts:
        m = id_map_by_table[t]
        nid = m[r['id']]
        pid = r['parent_id']
        new_pid = m.get(pid, pid) if pid not in (None, 0) else (pid or 0)
        final_rows.append({
            'id': nid, 'cat_type': cat_type, 'name': r['name'],
            'parent_id': new_pid, 'sort_order': r['sort_order'],
            'description': r['description'], 'code': r['code'],
            'space_id': space_id, 'space_code': space_code,
            'valid_flag': str(r['valid_flag']) in ('1', 'true', 'True'),
            'del_flag': str(r['del_flag']) in ('1', 'true', 'True'),
            'create_by': r['create_by'], 'creator_id': r['creator_id'],
            'create_time': r['create_time'], 'update_by': r['update_by'],
            'updater_id': r['updater_id'], 'update_time': r['update_time'],
        })
        # surviving rows of partial types whose parent was dropped;
        # only primary tables to avoid ambiguous old-id matches across tables
        if t in ('tax_task_cat', 'tax_clean_cat', 'tax_asset_cat'):
            for eid, ecat, eparent in existing:
                if ecat == cat_type and eparent == r['id']:
                    parent_fixes.append((eid, nid))

    # report
    from collections import Counter
    cnt = Counter(fr['cat_type'] for fr in final_rows)
    print('rows to insert by cat_type:', dict(cnt))
    print('total inserts:', len(final_rows))
    print('parent fixes for surviving rows:', len(parent_fixes))
    for fr in final_rows[:5]:
        print(' sample:', fr['cat_type'], fr['id'], fr['name'], fr['code'], 'parent=', fr['parent_id'])

    if not APPLY:
        print('\nDRY RUN only. Re-run with --apply to execute.')
        conn.close()
        return

    sql = """INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order,
        description, code, space_id, space_code, valid_flag, del_flag,
        create_by, creator_id, create_time, update_by, updater_id, update_time)
        VALUES (%(id)s, %(cat_type)s, %(name)s, %(parent_id)s, %(sort_order)s,
        %(description)s, %(code)s, %(space_id)s, %(space_code)s, %(valid_flag)s,
        %(del_flag)s, %(create_by)s, %(creator_id)s, %(create_time)s,
        %(update_by)s, %(updater_id)s, %(update_time)s)
        ON CONFLICT (id) DO NOTHING"""
    cur2 = conn.cursor()
    for fr in final_rows:
        cur2.execute(sql, fr)
    for eid, nid in parent_fixes:
        cur2.execute('update tax_category set parent_id=%s where id=%s', (nid, eid))
    conn.commit()
    cur2.execute('select cat_type, count(*) from tax_category group by 1 order by 1')
    print('after restore:', cur2.fetchall())
    conn.close()


if __name__ == '__main__':
    main()
