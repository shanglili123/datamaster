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

SQL_FILE = os.path.join(
    os.path.dirname(__file__),
    "postgresql",
    "upgrade",
    "V1.5.0 upgrade V1.6.0",
    "add-project-id.sql",
)


def main():
    if not os.path.exists(SQL_FILE):
        print(f"Error: SQL file not found at {SQL_FILE}")
        sys.exit(1)

    with open(SQL_FILE, "r", encoding="utf-8") as f:
        sql_content = f.read()

    conn = psycopg2.connect(**DB_CONFIG)
    conn.autocommit = True
    cursor = conn.cursor()

    statements = []
    for stmt in sql_content.split(";"):
        stmt = stmt.strip()
        lines = [l for l in stmt.splitlines() if not l.strip().startswith("--")]
        stmt = " ".join(l.strip() for l in lines if l.strip())
        if stmt:
            statements.append(stmt)

    success = 0
    failed = 0
    for stmt in statements:
        try:
            cursor.execute(stmt + ";")
            print(f"  OK: {stmt[:80]}...")
            success += 1
        except Exception as e:
            print(f"  FAIL: {stmt[:80]}...  -> {e}")
            failed += 1

    cursor.close()
    conn.close()

    print(f"\nDone. {success} succeeded, {failed} failed.")
    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
