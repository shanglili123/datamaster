# 本体指标第一批后端

## 范围与部署

本批只管理指标定义、元数据校验与不可变发布版本，不生成或执行指标 SQL，不连接 Doris，不验证源数据质量、实际表/列存在性或源库访问权限。ADS 命名只能限制适用层级，不是授权证据。

手工审阅并执行 `sql/update_20261027_ontology_metric.sql`；这是一次性 PostgreSQL 新迁移，未执行。权限按钮挂在实际 `ont:ontology:list` 菜单下，不新建一级菜单，不自动授予任何角色。执行后由管理员审核角色权限并刷新权限缓存。

## 接口和状态

- `/ont/metric` 提供 page、详情、新增、PUT更新、删除、validate、publish、disable、versions、versions/{version}。
- page 必须提供 ontologyId。每个读写接口依据数据库指标归属加载本体并校验空间成员；只允许平台 admin/ops 跨空间，不允许 space-admin 或指标创建者身份旁路。
- 新建最低字段为 ontologyId、conceptId、code、name、description。草稿允许其余业务定义不完整。code 小写字母开头，仅含小写字母、数字、下划线，创建后 id/ontologyId/conceptId/code 不可改。
- PUT body 必须带当前 revision；publish/disable query 参数 revision 虽为可选绑定，业务上必须提供并匹配。validate 的 revision 参数不影响定义，也不消耗 revision。
- 当前定义与发布快照分离。编辑已发布指标保留 PUBLISHED 和 publishedVersion，draftChanged 按当前定义是否异于已发布定义计算。历史版本始终可读；当前详情 sourceSnapshot 指向最近一次发布映射，非当前草稿映射。
- PUBLISHED 且定义、实际物理映射均与当前发布快照相同，publish 不产生新版本；DISABLED 重启即使未改业务定义也新建版本。状态修改和有内容变化的更新递增 revision；停用保留全部历史。
- 仅从未发布的 DRAFT 可物理删除，删除后允许同本体重用该编码。发布后的记录不可删除，版本表 FK 同时保护。

## 发布校验与快照

发布要求 owner/unit/grain/aggregation/conceptTableId，除 COUNT(*) 外要求 valuePropertyId。所用属性必须存在且属于该概念，并在选定表绑定有唯一且非空列名的字段映射；SUM/AVG 要求 numeric 属性，时间属性要求 date/datetime/timestamp。维度去重，过滤运算符白名单，IN 要求非空标量数组，其他运算拒绝对象/数组，NULL 运算清除 value。数值过滤以 BigDecimal 解析，接受字符串避免浏览器精度丢失；日期按 ISO 日期/本地日期时间验证。

按照现有 `docs/数仓建设文档.md` 分层约定，限 Doris 且物理表名以 ads_ 开头，或明确位于 ads 数据库。必须有明确 databaseName。

独立 MetricRespVO 快照排除 DO 审计/params 等对象；sourceSnapshot 冻结 datasourceId、datasourceType、databaseName、schemaName、tableName、propertyColumns（属性ID到实际列名/属性编码/类型），validationScope=METADATA_ONLY，不保存密码或数据源连接配置。历史版本 INSERT ONLY，数据库唯一(metric_id,version)，触发器拒绝 UPDATE/DELETE/TRUNCATE；数据库超级用户仍有改变结构/禁用触发器的管理能力。

## 集成必须项与并发边界

所有指标写操作先锁本体 ont_ontology 行，再锁主指标；create 锁本体后重新检查概念/绑定。`MetricMetadataLockMapper.lockOntology(Long)` 返回 Long，SQL 为有效本体 `FOR UPDATE`，供删除引用保护共用。

集成方必须在现有 OntologyServiceImpl.deleteOntology、ConceptServiceImpl.deleteConcept、ConceptTableServiceImpl.deleteConceptTable 的任何级联动作前，用同一本体行锁并检查 MetricDO 对 ontologyId/conceptId/conceptTableId 引用；这三处现有文件不在本工作树产物内修改，父会话负责集成。FK 防物理删除，但不能替代逻辑删除保护。

属性/字段绑定修改不会影响冻结历史；重发重新验证并抓取当前映射。现有属性和字段绑定编辑服务若不参与本体锁，则与发布同时编辑时不提供跨服务完整元数据串行化保证；后续可统一元数据编辑事务的锁约定。现有概念跨本体移动和本体改空间也应避免与指标归属冲突，本批未扩展这些入口。

## 验证

新增 JUnit 4 focused 测试覆盖草稿/发布必填、COUNT(*)、过滤结构与精度、维度去重、revision/归属拒绝、独立快照复制与幂等比较、ADS及类型规则。未执行编译、构建、lint、测试或迁移；事务/数据库约束仍需集成环境验证。
