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
    """引号感知的 SQL 语句切分：
    - 正确识别单引号字符串（含 '' 转义、E'...' 前缀）、双引号标识符、行级 -- 注释
    - 仅在字符串/标识符之外的 ';' 处切分，避免含分号/换行的数据字面量被误切
    返回非空语句列表。
    """
    statements = []
    buf = []
    i, n = 0, len(sql_text)
    line_start = True  # 当前处于行首（用于识别 -- 行注释开头）
    while i < n:
        c = sql_text[i]
        # 行级注释（-- 到行尾）
        if line_start and sql_text.startswith('--', i):
            while i < n and sql_text[i] != '\n':
                i += 1
            continue  # 吞掉整行注释（含最后的换行交给下一循环处理也行）
        if c == '\n':
            line_start = True
        else:
            line_start = False
        # 单引号字符串（含 E'...' 前缀也走同一逻辑）
        if c == "'":
            buf.append(c)
            i += 1
            while i < n:
                if sql_text[i] == '\\' and i + 1 < n:
                    # E'...' 字符串中的反斜杠转义，但普通字符串可能也含，保守处理
                    buf.append(sql_text[i])
                    buf.append(sql_text[i + 1])
                    i += 2
                    continue
                if sql_text[i] == "'":
                    if i + 1 < n and sql_text[i + 1] == "'":
                        buf.append("''")  # 两个单引号=一个转义单引号
                        i += 2
                        continue
                    buf.append("'")
                    i += 1
                    break  # 字符串结束
                buf.append(sql_text[i])
                i += 1
            continue
        # 双引号标识符（其中 "" 为转义）
        if c == '"':
            buf.append(c)
            i += 1
            while i < n:
                if sql_text[i] == '"':
                    if i + 1 < n and sql_text[i + 1] == '"':
                        buf.append('""')
                        i += 2
                        continue
                    buf.append('"')
                    i += 1
                    break
                buf.append(sql_text[i])
                i += 1
            continue
        if c == ';':
            stmt = ''.join(buf).strip()
            if stmt:
                statements.append(stmt)
            buf = []
            i += 1
            continue
        buf.append(c)
        i += 1
    tail = ''.join(buf).strip()
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