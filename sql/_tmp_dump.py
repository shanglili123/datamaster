import psycopg2
import os

DB_CONFIG = {
    "host": "192.168.93.174",
    "port": 5432,
    "dbname": "datamaster",
    "user": "datamaster",
    "password": "datamaster",
}

from datetime import datetime
date_str = datetime.now().strftime("%Y%m%d")
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), f"datamaster_dump_{date_str}.sql")

conn = psycopg2.connect(**DB_CONFIG)
conn.autocommit = True
cur = conn.cursor()

cur.execute("""
    SELECT tablename FROM pg_tables 
    WHERE schemaname = 'public' 
    ORDER BY tablename
""")
tables = [r[0] for r in cur.fetchall()]
print(f"Found {len(tables)} tables")

skipped = []
exported = 0

with open(OUTPUT_DIR, "w", encoding="utf-8") as f:
    f.write(f"-- DataMaster Database Dump\n")
    f.write(f"-- Database: datamaster\n")
    f.write(f"-- Host: {DB_CONFIG['host']}:{DB_CONFIG['port']}\n\n")

    for table in tables:
        try:
            cur.execute(f'SELECT COUNT(*) FROM "{table}"')
            count = cur.fetchone()[0]
        except Exception as e:
            print(f"  SKIP {table}: {e}")
            skipped.append(table)
            continue

        print(f"Exporting: {table} ({count} rows)")

        try:
            cur.execute("""
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns 
                WHERE table_name = %s AND table_schema = 'public'
                ORDER BY ordinal_position
            """, (table,))
            columns = cur.fetchall()
        except Exception as e:
            print(f"  SKIP columns for {table}: {e}")
            skipped.append(table)
            continue

        f.write(f"\n-- Table: {table} ({count} rows)\n")
        f.write(f'CREATE TABLE IF NOT EXISTS "{table}" (\n')
        col_defs = []
        for col in columns:
            nullable = "NULL" if col[2] == 'YES' else "NOT NULL"
            default = f" DEFAULT {col[3]}" if col[3] else ""
            col_defs.append(f'    "{col[0]}" {col[1]} {nullable}{default}')
        f.write(",\n".join(col_defs))
        f.write("\n);\n\n")

        if count > 0:
            try:
                cur.execute(f'SELECT * FROM "{table}"')
                batch_size = 500
                rows = cur.fetchmany(batch_size)
                while rows:
                    for row in rows:
                        values = []
                        for val in row:
                            if val is None:
                                values.append("NULL")
                            elif isinstance(val, bool):
                                values.append("true" if val else "false")
                            elif isinstance(val, (int,)):
                                values.append(str(val))
                            elif isinstance(val, float):
                                values.append(str(val))
                            else:
                                escaped = str(val).replace("\\", "\\\\").replace("'", "''")
                                values.append(f"'{escaped}'")
                        f.write(f'INSERT INTO "{table}" VALUES ({", ".join(values)});\n')
                    rows = cur.fetchmany(batch_size)
            except Exception as e:
                print(f"  ERROR data for {table}: {e}")
                skipped.append(table)
                continue
        exported += 1

    f.write("\n-- End of dump\n")

conn.close()
fsize = os.path.getsize(OUTPUT_DIR) / 1024 / 1024
print(f"\nDone! Exported {exported} tables, skipped {len(skipped)}: {skipped}")
print(f"File: {OUTPUT_DIR}")
print(f"Size: {fsize:.2f} MB")
