# -*- coding: utf-8 -*-
"""删除标准目录「应急与防汛抗旱标准」、数据研发「水资源与取用水/水质与生态」（软删）。
用法: python del_irrelevant_cats.py [--apply]
"""
import sys
import psycopg2

APPLY = '--apply' in sys.argv

conn = psycopg2.connect(host="192.168.93.174", port=5432, dbname="datamaster",
                        user="datamaster", password="datamaster")
cur = conn.cursor()

# 1. 递归收集目标类目 id
cur.execute("""
with recursive target as (
  select id, cat_type, code, name, parent_id from tax_category
  where (name = '应急与防汛抗旱标准' and cat_type = 'DOCUMENT')
     or (name in ('水资源与取用水','水质与生态') and cat_type in ('TASK','DATA_DEV'))
  union all
  select c.id, c.cat_type, c.code, c.name, c.parent_id
  from tax_category c join target t on c.parent_id = t.id
)
select id, cat_type, code, name from target
""")
targets = cur.fetchall()
ids = [int(r[0]) for r in targets]
print("待软删 tax_category 共 %d 行:" % len(targets))
for r in targets:
    print("  %s %s %s %s" % r)

# 2. 引用任务迁移：cat_code A05/A06 的 col_etl_task（仅水利两棵 TASK 树相关）
cur.execute("""select id, name, cat_code from col_etl_task
               where del_flag='0' and (cat_code like 'A05%%' or cat_code like 'A06%%')""")
tasks = cur.fetchall()
print("\n受影响任务 %d 个（迁移到 核心银行系统 A03）:" % len(tasks))
for r in tasks:
    print("  ", r)

if not APPLY:
    print("\n[DRY-RUN] 未执行任何修改。加 --apply 执行。")
    conn.close()
    sys.exit(0)

cur.execute("update tax_category set del_flag='1' where id = any(%s)", (ids,))
print("tax_category 软删行数:", cur.rowcount)

if tasks:
    task_ids = [int(t[0]) for t in tasks]
    cur.execute("update col_etl_task set cat_code='A03' where id = any(%s)", (task_ids,))
    print("任务迁移行数:", cur.rowcount)

conn.commit()
print("已提交。")
conn.close()
