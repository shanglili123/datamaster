# PostgreSQL 数据血缘 v2 部署与验收

## 变更范围

新的表/字段加工血缘存储在平台 PostgreSQL；资产详情、元数据表详情、独立血缘分析页共用 v2 接口。旧 Neo4j 数据和本体对象/动作血缘保持不动，不做启动导入或删图。

## 上线顺序

1. 在测试环境审核并手工执行：
   - `sql/update_20261020_data_lineage.sql`
   - `sql/update_20261020_lineage_outbox.sql`
2. 部署后端及前端。
3. 配置 `LINEAGE_JDBC_ENABLED=true` 后重启应用。默认关闭，避免未迁移的环境启动后扫描缺失表。
4. 旧 Neo4j 的 `LINEAGE_ENABLED` 只控制保留的旧功能，不是新 JDBC 血缘的必要条件；不需要删除其配置或数据。

本次代码改造没有执行上述迁移、连接数据库或运行构建/测试。

## 生成与重建

- 新发布的 ETL 使用实际生成的 reader/writer job 产物捕获，不按画布调度边推断数据关系。
- Pipeline 按实际逻辑节点与每个输出分开捕获，避免不同节点输入/输出串线。
- 快照进入 `col_lineage_outbox`，提交后由 worker 轮询；失败有限退避重试。`DONE` 表示同步接口已处理，不等于 SQL 完整解析或任务运行成功。
- PostgreSQL producer 的 `status/error_code/stale` 表示解析与有效性；不支持、身份歧义和解析失败不会伪装成完整结果。
- 任务下线标为非活动；删除产生 tombstone。旧发布恢复仅复用已捕获的发布快照，不从当前草稿推断。
- 外部调度器与平台数据库并非分布式事务；本地提交失败不能保证交付，需人工核对并重建。

显式重放入口（需 `col:etlTask:edit` 及真实空间成员权限）：

```http
POST /col/etlTask/lineage/rebuild/{taskId}?dryRun=true
POST /col/etlTask/lineage/rebuild/{taskId}?dryRun=false&expectedSequence={预检返回sequence}
```

重放只读取已有的发布捕获记录，不发布/运行 ETL、不执行 SQL、不导入 Neo4j。历史任务没有发布捕获记录时返回 `SKIPPED/NO_PUBLISHED_SNAPSHOT`，需要重新核实发布配置后再发布，不能把草稿冒充已发布事实。

## 查看入口与权限

- 资产详情 → 资产血缘：`ast:asset:query`，另查真实空间、连接和资产可见性。
- 元数据表详情 → 血缘分析，或独立血缘分析选择中心元数据表：`cat:table:query`，另查当前空间成员和元数据数据范围。
- 图默认双向 1 跳，最多 5 跳、200 节点/500 边及扫描、时间预算；一次表→任务→表计为 1 跳。
- 图/关系表切换读取同一份真实数据；任务节点是空间内配置事实，运行状态未知，不表示已获任务执行详情或已执行成功。
- 字段双端需明确字段空间授权。仅表可见不授予字段可见；未配置可靠字段授权时隐藏字段，提示 `FIELD_VISIBILITY_UNRESOLVED`，保留表级关系。
- 资产与元数据 ID 不可互换；按连接、catalog/schema/table 结构身份定位，歧义不取第一条，不按 host 或表名前缀合并。

## SQL 与字段支持边界

- 支持明确 SELECT 列、别名、普通表达式/函数列引用、JOIN、普通子查询、非递归 CTE、UNION ALL。
- 解析器可处理显式目标列 INSERT SELECT、CTAS、简单单目标 UPDATE。只有真实发布产物中有可定位的语句才会形成声明关系，不抓取任意用户临时查询作为持久目标血缘。
- reader query → 实际 transformer → writer 显式有序列进行组合；普通直拷贝按真实列序映射。
- 物理 SQL 星号缺少可靠元数据时保守降级；动态 SQL、递归 CTE、窗口/复杂方言、歧义列等提示缺口，不正则猜来源。
- 只读查询和空表 CREATE TABLE/模型物化不产生虚构的加工边；常量、系统时间等没有物理源字段。
- 当前发布器 SQL-only 任务不能确认有实际可执行 SQL 产物时，记录捕获失败，不把草稿 SQL 当运行事实。

## 推荐验收

1. 在测试库走一次直拷贝、改名、表达式转换及单读多写；核查源/目标及字段顺序。
2. Pipeline 配置 A→B、C→D，确认没有 A→D 等假路径；合表 SQL 的实际来源可见。
3. 重发换目标、发布为不支持来源、下线、删除和重复/迟到同步，确认旧关系不会冒充最新成功。
4. MySQL 两段 db.table、PostgreSQL schema.table、带引用大小写字段，同一物理对象在直连与 SQL 路线命中相同身份。
5. 在资产、元数据详情和独立分析页核对同一中心的图/表数据；切中心、方向、粒度、深度、空间无旧响应串图。
6. 字段未授权、跨空间、同名多表以及大图预算场景得到正确拒绝/降级/截断，不泄漏凭据、SQL或未授权节点。
7. `LINEAGE_ENABLED=false` 且不启动 Neo4j 时，新 JDBC 数据血缘仍可使用；保留 Neo4j 的环境另回归对象/动作血缘。

测试源码已提供在 metadata/collector 的 `src/test` 下；按仓库要求尚未主动运行。
