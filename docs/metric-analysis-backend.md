# 受控指标查询后端交付说明

本说明仅覆盖新增后端、SQL及未运行的JUnit4测试，作为父方案的部署补充，不是BI/图表/看板方案，也不包含AI调用。指标、维度及受控查询可以供后续语义能力复用。

## 默认关闭与配置

没有自动修改任何application配置。以下均为需由部署方显式配置的精确属性；ID与账号名仅为虚构示例，不含凭据。

```yaml
ontology:
  metric-query:
    enabled: false
    approved-binding-ids: "810001,810002"
    allowlisted-readonly-usernames: "ads_metric_reader"
    timeout-seconds: 15
    max-concurrent: 4
```

- `enabled`默认false；disabled在任何来源连接创建前拒绝执行，保存/查看条件不受开关影响。
- `approved-binding-ids`默认空；空表示没有批准来源。严格解析逗号分隔正Long，拒绝空项、重复、溢出等。绑定批准不是数据权限。
- `allowlisted-readonly-usernames`默认空；实际数据源解析出的username须精确匹配。空名单拒绝。配置名单不能证明账号只读，必须由DBA保障。
- `timeout-seconds`默认15，合法1..60，驱动不支持设置则拒绝。
- `max-concurrent`默认4，合法1..16，每进程非阻塞Semaphore；集群总并发须额外运维控制。

## 部署顺序与安全前提

1. 先部署第一批指标定义及不可变版本表；备份后人工审核并执行`sql/update_20261027_metric_analysis.sql`。本交付未执行迁移。
2. DBA先完成ADS安全视图/表的行、租户、空间隔离，仅登记允许汇总的非敏感列；专用账号只授予该安全来源SELECT，不能使用root、平台管理账号或任意跨库查询账号。数据库级行权限不是本实现提供的能力。
3. 在资产平台登记相同datasource+table及所有参与列，启用严格资产治理并授权当前空间/用户。治理关闭、warn、未注册、CATALOG降级、accessAllowed非true全部拒绝。
4. 确保保存的数据源type为DORIS，配置解析`DbQueryProperty.dbName`与发布snapshot.databaseName精确相同；snapshot.schemaName仅空或同库。不能改写连接库名来执行旧snapshot。仅数据库、表、列的安全ASCII标识符可用，不支持点、空格、SQL片段。
5. 显式批准绑定ID和专用只读账号名，再人工分配按钮权限、检查空间成员关系。运行使用严格成员校验，无admin/ops成员绕过；保存分析始终只对owner可见，admin也不能读取他人条件。
6. 在隔离环境人工验证只读驱动语义、SQL超时、治理拒绝和资源关闭后再启用。`Connection.setReadOnly(true)`不支持或`isReadOnly()`非true均拒绝；连接只读标志不是数据库SELECT-only授权的替代品。

绑定ID可被重指向，因此运行时会检查当前ConceptTable的conceptId、datasourceId、databaseName、schemaName、tableName与snapshot一致；变更后旧版本仍可查看/保存条件，但不能借用原白名单自动执行，需人工审查来源和重新发布/批准。度量、维度、类型、固定过滤等计算定义一律来自不可变发布snapshot，而非当前草稿或最新属性映射。

所有SQL实际参与字段（度量、所选维度、时间、固定过滤、用户过滤）全部发送到`resolveTable`，入口`ONTOLOGY_METRIC_QUERY`，空间ID/code只取Ontology数据库记录。`allowedColumns`非null必须含所有使用列，任何denied列均拒绝；另调用hide检查（scene='2'，权限等级不可用时null）。

现有治理接口仅暴露hide而非replace结果。本实现使用已有assets-interface内AssetsAssetColumnMapper检查登记元数据，任何参与列sensitiveLevelId非null、未登记或重复登记均拒绝，保守覆盖可能替换/隐藏的敏感输入，不在聚合alias后套脱敏。资产敏感元数据准确性以及白名单安全ADS视图仍需管理员负责，不声称全量行权限或聚合后自然脱敏。

## API与数据契约

统一前缀`/ont/metric-analysis`，保持CommonResult包装。

| 方法 | 路径 | 权限 | 返回data |
| --- | --- | --- | --- |
| POST | /query | ont:metric:execute | MetricQueryRespVO |
| GET | /page?ontologyId=...&name=...&pageNum=1&pageSize=20 | ont:analysis:list | PageResult(rows,total) |
| POST | 空路径 | ont:analysis:add | Long id |
| PUT | 空路径 | ont:analysis:edit | Integer影响行数 |
| GET | /{id} | ont:analysis:query | MetricAnalysisRespVO |
| DELETE | /{id} | ont:analysis:remove | Integer影响行数 |

SQL仅在唯一的`ont:ontology:list` C菜单下动态分配按钮ID，不创建BI菜单，不自动赋任何角色权限。

Query必填metricId、显式正version；dimensionPropertyIds默认[]最多5；filters默认[]最多20，固定+用户过滤总和最多20；limit默认100合法1..500。显式null集合视作[]，显式null limit视作100。所有输入服务端再次校验，不能自由输入SQL、表达式、跨表、客户端alias或替换固定条件。

只要指标当前为PUBLISHED，可以执行任意存在的已发布历史版本；DISABLED/DRAFT不执行。校验metric、ontology、concept、code及snapshot版本身份。时间属性存在则必须同时提供startTime/endTime，严格ISO LocalDate或LocalDateTime解析，start<end，使用[start,end)；datetime不接受空格、时区或offset。无时间属性不能传时间范围。暂不分桶、无自动grain变换。用户只过滤发布维度/时间属性，固定过滤永远AND，不会覆盖口径。

聚合白名单SUM、COUNT、COUNT_DISTINCT、AVG、MIN、MAX；COUNT无value可用COUNT(*)，SUM/AVG仅numeric。所有标量按snapshot类型转换；integer拒小数；IN限1..100且不接受null/nested；字符串最长2000。预编译SQL只使用问号参数，limit也为参数。查询limit+1后截断，分组按全部实际选定维度排序，总量不按发布维度误分组。查询执行器无共享缓存、不主动归档数据。注意Agent调用方会把工具结果加入流程、审计和会话记忆，可能随这些记录持久化；不等于整个平台不存结果，详见数据分析Agent实施方案的持久化边界。

响应columns为`{key,label,dataType}`，rows key严格`d_<propertyId>`与`metric_value`。**所有numeric结果（含COUNT/整数维度）作为十进制字符串返回**，避免JavaScript精度丢失，columns.dataType保留语义类型；boolean保持boolean，date/datetime保持ISO字符串，SQL NULL为null。其余字段metricId/version/metricName/unit/truncated/executedAt/elapsedMs/limit/notice按约定提供。

Saved Req含id、ontologyId、name（必填≤100）、description（≤1000）、query、revision（更新必填）。Resp含id、ontologyId、name、description、query、revision、createTime、updateTime。owner由SecurityUtils赋值；表物理删除无逻辑删除，更新事务行锁检查revision。metric_id/version复合外键保护来源，名称不独占。每次列表/详情/修改/删除均校验空间成员与指标版本归属；保存验证定义而不连接来源DB，不自动升级版本；run重新检查当前可执行性。只存规范化请求，不存SQL、凭据或结果。

## 交付与验证状态

新增Java仅Java8语法，测试使用已有JUnit4；未改pom、已有Java文件、前端、应用配置或git。未构建、编译、测试、lint、联网或执行SQL；需要父会话合并第一批代码后由用户本地验证。
