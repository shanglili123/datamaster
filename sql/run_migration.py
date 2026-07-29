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

MIGRATION_FILES = [
    r"D:\dev\dataMaster\sql\update_20260728_project_columns_to_space.sql",
    r"D:\dev\dataMaster\sql\update_20260728_space_menu_routes.sql",
]

if len(sys.argv) > 1:
    MIGRATION_FILES = sys.argv[1:]

conn = psycopg2.connect(**DB_CONFIG)
conn.autocommit = True
cur = conn.cursor()

success = 0
failed = 0
for file_path in MIGRATION_FILES:
    filename = file_path.rsplit("\\", 1)[-1]
    print(f"\n=== Running {filename} ===")
    with open(file_path, "r", encoding="utf-8") as f:
        sql = f.read()
    statements = split_sql_statements(sql)
    for stmt in statements:
        try:
            cur.execute(stmt)
            print(f"  OK: {stmt[:80]}...")
            success += 1
        except Exception as e:
            print(f"  FAIL: {stmt[:80]}... -> {e}")
            failed += 1

print(f"\nDone! {success} succeeded, {failed} failed")
conn.close()
