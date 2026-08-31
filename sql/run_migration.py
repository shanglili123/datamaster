# -*- coding: utf-8 -*-
"""
PostgreSQL 迁移脚本执行器
用法:
    python run_migration.py <sql文件> [--host HOST] [--port PORT] [--db DB] [--user USER] [--password PASS]
    python run_migration.py sql/update_20260831_ont_function_shared.sql

默认连接与 query_debug*.py 一致: 192.168.93.174:5432/datamaster (postgres/postgres)
所有语句在同一事务中执行，任一失败即回滚。
"""
import argparse
import sys

import psycopg2

DEFAULT_HOST = '192.168.93.174'
DEFAULT_PORT = 5432
DEFAULT_DB = 'datamaster'
DEFAULT_USER = 'postgres'
DEFAULT_PASSWORD = 'postgres'


def parse_args():
    parser = argparse.ArgumentParser(description='执行迁移 SQL 脚本（PostgreSQL）')
    parser.add_argument('sql_file', help='SQL 文件路径')
    parser.add_argument('--host', default=DEFAULT_HOST)
    parser.add_argument('--port', type=int, default=DEFAULT_PORT)
    parser.add_argument('--db', default=DEFAULT_DB)
    parser.add_argument('--user', default=DEFAULT_USER)
    parser.add_argument('--password', default=DEFAULT_PASSWORD)
    return parser.parse_args()


def split_statements(sql_text):
    """按行拆分：跳过 -- 注释行，以分号结尾的语句切分。返回非空语句列表。"""
    statements = []
    buf = []
    for line in sql_text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith('--'):
            continue
        buf.append(line)
        if stripped.endswith(';'):
            statements.append('\n'.join(buf).strip())
            buf = []
    tail = '\n'.join(buf).strip()
    if tail:
        statements.append(tail)
    return statements


def main():
    args = parse_args()
    with open(args.sql_file, 'r', encoding='utf-8') as f:
        sql_text = f.read()

    statements = split_statements(sql_text)
    if not statements:
        print('没有可执行的 SQL 语句')
        return 1

    print("连接数据库 %s:%s/%s ..." % (args.host, args.port, args.db))
    conn = psycopg2.connect(
        host=args.host, port=args.port, dbname=args.db,
        user=args.user, password=args.password,
    )
    conn.autocommit = False
    cur = conn.cursor()
    try:
        for i, stmt in enumerate(statements, 1):
            preview = stmt.replace('\n', ' ')[:90]
            print("[%d/%d] %s" % (i, len(statements), preview))
            cur.execute(stmt)
        conn.commit()
        print("成功: %d 条语句全部执行并提交" % len(statements))
    except Exception as e:
        conn.rollback()
        print("失败，已回滚: %s" % e)
        return 1
    finally:
        cur.close()
        conn.close()
    return 0


if __name__ == '__main__':
    sys.exit(main())