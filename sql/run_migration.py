import psycopg2
import sys
import os

DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 5432,
    "dbname": "data_master",
    "user": "datamaster",
    "password": "datamaster",
}

SQL_DIR = os.path.join(
    os.path.dirname(__file__),
    "postgresql",
    "upgrade",
    "V1.5.0 upgrade V1.6.0",
)

SQL_FILES = [
    "fix-mdl-business-category-schema.sql",
    "add-project-id.sql",
    "seed-data-project-association.sql",
]


def run_sql_file(conn, filepath):
    if not os.path.exists(filepath):
        print(f"Error: SQL file not found at {filepath}")
        return False

    print(f"\n=== Running: {os.path.basename(filepath)} ===")

    with open(filepath, "r", encoding="utf-8") as f:
        sql_content = f.read()

    cursor = conn.cursor()
    statements = []
    for stmt in sql_content.split(";"):
        stmt = stmt.strip()
        lines = [l for l in stmt.splitlines() if not l.strip().startswith("--")]
        stmt = " ".join(l.strip() for l in lines if l.strip())
        if stmt:
            statements.append(stmt)

    total_ok = 0
    total_failed = 0
    for stmt in statements:
        try:
            cursor.execute(stmt + ";")
            print(f"  OK: {stmt[:80]}...")
            total_ok += 1
        except Exception as e:
            print(f"  FAIL: {stmt[:80]}...  -> {e}")
            total_failed += 1

    cursor.close()
    print(f"--- {total_ok} succeeded, {total_failed} failed ---")
    return total_failed == 0


def main():
    conn = psycopg2.connect(**DB_CONFIG)
    conn.autocommit = True

    all_ok = True
    for fname in SQL_FILES:
        fpath = os.path.join(SQL_DIR, fname)
        ok = run_sql_file(conn, fpath)
        if not ok:
            all_ok = False

    conn.close()

    print(f"\n{'=' * 40}")
    print("All done." if all_ok else "Some files had failures.")
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
