"""Run SQL migration files in order, each file wrapped in a transaction."""
import psycopg2
import re
import sys


def split_sql_statements(sql):
    """Split SQL by semicolons, respecting dollar-quoting and string literals."""
    statements = []
    current = []
    i = 0
    while i < len(sql):
        c = sql[i]
        if c == "-" and i + 1 < len(sql) and sql[i + 1] == "-":
            i += 2
            while i < len(sql) and sql[i] not in "\r\n":
                i += 1
        elif c == "'":
            current.append(c)
            i += 1
            while i < len(sql) and sql[i] != "'":
                if sql[i] == "\\":
                    current.append(sql[i])
                    i += 1
                current.append(sql[i])
                i += 1
            if i < len(sql):
                current.append(sql[i])
                i += 1
        elif c == "$" and i + 1 < len(sql) and sql[i + 1] == "$":
            current.append("$$")
            i += 2
            while i + 1 < len(sql) and not (sql[i] == "$" and sql[i + 1] == "$"):
                current.append(sql[i])
                i += 1
            if i + 1 < len(sql):
                current.append("$$")
                i += 2
        elif c == ";":
            stmt = "".join(current).strip()
            if stmt:
                statements.append(stmt)
            current = []
            i += 1
        else:
            current.append(c)
            i += 1
    stmt = "".join(current).strip()
    if stmt:
        statements.append(stmt)
    return statements


DB_CONFIG = {
    "host": "192.168.93.174",
    "port": 5432,
    "dbname": "datamaster",
    "user": "datamaster",
    "password": "datamaster",
}

# Execute from 07-30 forward, in chronological order
MIGRATION_FILES = [
    r"D:\dev\dataMaster\sql\update_20260803_svc_gateway_and_remove_asset_type23.sql",
]

conn = psycopg2.connect(**DB_CONFIG)
# Use manual transaction control per file
conn.autocommit = False

total_ok = 0
total_fail = 0

for file_path in MIGRATION_FILES:
    filename = file_path.rsplit("\\", 1)[-1]
    print(f"\n=== {filename} ===")
    with open(file_path, "r", encoding="utf-8") as f:
        sql = f.read()

    statements = split_sql_statements(sql)

    cur = conn.cursor()
    file_ok = 0
    file_fail = 0
    stmt_errors = []

    try:
        for stmt in statements:
            try:
                cur.execute(stmt)
                preview = stmt[:80].replace("\n", " ")
                print(f"  OK  [{cur.rowcount} rows] {preview}...")
                file_ok += 1
            except Exception as e:
                preview = stmt[:80].replace("\n", " ")
                err_msg = str(e)[:120]
                print(f"  FAIL {preview}... -> {err_msg}")
                file_fail += 1
                stmt_errors.append((preview, err_msg))

        if file_fail > 0:
            print(f"  >>> ROLLBACK ({file_fail} failures) <<<")
            conn.rollback()
            total_fail += file_fail
            # Don't add file_ok since the whole file was rolled back
        else:
            conn.commit()
            print(f"  >>> COMMIT ({file_ok} statements) <<<")
            total_ok += file_ok
    except Exception as tx_err:
        print(f"  >>> TRANSACTION ERROR: {tx_err} - ROLLBACK <<<")
        conn.rollback()
        total_fail += 1
    finally:
        cur.close()

conn.close()
print(f"\n=== DONE: {total_ok} succeeded, {total_fail} failed ===")
