import psycopg2

DB_CONFIG = {
    "host": "192.168.93.174",
    "port": 5432,
    "dbname": "datamaster",
    "user": "datamaster",
    "password": "datamaster",
}

OUTPUT_FILE = r"D:\dev\dataMaster\sql\datamaster_dump.sql"


def quote_str(s):
    return "'" + s.replace("'", "''") + "'"


def format_value(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (bytes, bytearray)):
        return quote_str(v.decode("utf-8", errors="replace"))
    return quote_str(str(v))


def column_default(pg_default):
    if pg_default is None:
        return None
    return pg_default


conn = psycopg2.connect(**DB_CONFIG)
conn.autocommit = True
cur = conn.cursor()

tables = []
cur.execute(
    """
    SELECT n.nspname, c.relname
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
      AND n.nspname NOT IN ('pg_catalog', 'information_schema')
    ORDER BY n.nspname, c.relname
    """
)
tables = cur.fetchall()

out = []
out.append("-- DataMaster Database Dump")
out.append("-- Database: datamaster")
out.append("-- Host: {}:{}".format(DB_CONFIG["host"], DB_CONFIG["port"]))
out.append("")

total_tables = 0
for schema, table in tables:
    qtable = '"{}"."{}"'.format(schema, table)

    cur.execute(
        """
        SELECT a.attname,
               pg_catalog.format_type(a.atttypid, a.atttypmod),
               a.attnotnull,
               pg_catalog.pg_get_expr(d.adbin, d.adrelid) AS default_expr
        FROM pg_catalog.pg_attribute a
        LEFT JOIN pg_catalog.pg_attrdef d
          ON d.adrelid = a.attrelid AND d.adnum = a.attnum
        WHERE a.attrelid = %s::regclass
          AND a.attnum > 0
          AND NOT a.attisdropped
        ORDER BY a.attnum
        """,
        ('"{}"."{}"'.format(schema, table),),
    )
    cols = cur.fetchall()

    cur.execute("SELECT COUNT(1) FROM {}".format(qtable))
    row_count = cur.fetchone()[0]

    out.append("-- Table: {} ({} rows)".format(table, row_count))
    col_lines = []
    for name, ttype, notnull, default_expr in cols:
        nn = "NOT NULL" if notnull else "NULL"
        line = '    "{}" {} {}'.format(name, ttype, nn)
        if default_expr is not None:
            line += " DEFAULT {}".format(default_expr)
        col_lines.append(line + ",")
    out.append('CREATE TABLE IF NOT EXISTS "{}" ('.format(table))
    out.extend(col_lines)
    out.append(");")
    out.append("")

    cur.execute("SELECT * FROM {}".format(qtable))
    rows = cur.fetchall()
    for row in rows:
        values = ", ".join(format_value(v) for v in row)
        out.append('INSERT INTO "{}" VALUES ({});'.format(table, values))
    out.append("")
    total_tables += 1

out.append("-- End of dump")

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(out))

print("Done! {} tables dumped to {}".format(total_tables, OUTPUT_FILE))
conn.close()
