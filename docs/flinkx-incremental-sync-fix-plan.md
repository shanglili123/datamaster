# FLINKX 增量同步修复实施计划

最后更新：2026-06-02

本文档记录 DataMaster 离线 FLINKX（DolphinScheduler `CHUNJUN`）增量同步链路的阻塞问题、实施方案和完成进度。后续每完成一个步骤，立即更新状态、改动文件和验证结果。

## 状态说明

| 状态 | 含义 |
| --- | --- |
| 已完成 | 已完成实现；如需验证，已记录验证结果。 |
| 进行中 | 当前正在设计或实现。 |
| 待处理 | 尚未开始。 |
| 阻塞 | 依赖外部确认、环境或其他前置条件。 |

## 目标链路

将增量 FLINKX 工作流调整为：

```text
HTTP 增量准备节点
  -> CHUNJUN 执行节点
  -> HTTP 完成回写节点
```

核心原则：

1. 本次运行中的 CHUNJUN 参数通过 DolphinScheduler 参数替换注入，不再通过 `updateTask()` 更新新版流程定义。
2. 首次同步允许目标表为空，目标表 `MAX(...)` 为空时使用发布时持久化的初始游标。
3. 完成回写节点负责显式回写 FLINKX 任务状态、开始时间和结束时间，并与 RabbitMQ 流程实例监听保持幂等。
4. 同一 DataMaster 任务禁止重叠执行，避免两个流程实例消费相同增量窗口。
5. 复制和二次发布必须同步维护 HTTP 节点 URL、参数占位符和增量扩展元数据。

## 当前实现基线

以下能力已经存在，后续修复在此基础上继续：

| 能力 | 当前实现 | 状态 |
| --- | --- | --- |
| 增量准备 HTTP 节点 | `TaskConverter.buildIncrementalPrepareHttpTask()` 创建 `PUT /col/etlTask/incremental/prepare/{taskId}` 节点。 | 已完成 |
| 运行窗口计算 | `CollectorEtlIncrementalServiceImpl.prepareIncrementalTask()` 查询源表和目标表最大值并生成动态 `where`。 | 已完成 |
| MAX 查询重试 | `RelationalIncrementalBoundaryQuery` 最多重试 3 次，并校验表名、字段名。 | 已完成 |
| 字段映射 | `TaskConverter.resolveFlinkxIncrementalConfig()` 按源字段顺序找到目标增量字段。 | 已完成 |
| 失败下线 | 准备失败时返回 HTTP 500，并尝试下线 DS 任务。 | 已完成 |
| 流程实例回写 | `ProcessListener` 消费 RabbitMQ 中的 DolphinScheduler 流程实例消息，调用 `ICollectorEtlTaskInstanceService` 创建或更新任务实例。 | 已完成 |

## 阻塞问题

| No. | 优先级 | 问题 | 根因 | 处理方向 | 状态 |
| --- | --- | --- | --- | --- | --- |
| 1 | P1 | 本次运行中的 CHUNJUN 仍可能使用旧参数。 | 准备回调调用 `CollectorEtlIncrementalServiceImpl.updateDsTask()`，通过 `dsEtlTaskService.updateTask()` 修改新版流程定义。DS 3.2.2 已经按启动时版本构建当前执行图。 | 已改为 DolphinScheduler 参数替换：CHUNJUN JSON 保存 `${<准备节点名称>.response}`，准备节点直接返回本次完整 job JSON，不再运行期更新流程定义。 | 已完成 |
| 2 | P1 | 空目标表无法完成首次增量同步。 | `RelationalIncrementalBoundaryQuery` 将 `MAX(...) == null` 当作查询失败；`incrementStart` 未持久化，也未作为兜底值使用。 | 查询成功但 MAX 为空时正常返回 `null`；扩展表持久化初始游标；准备节点优先使用目标 MAX，目标为空时回退初始游标。 | 已完成 |
| 3 | P1 | 复制增量任务后，HTTP 节点仍回调原始任务。 | DS `batchCopy()` 复制了固化的原始 `taskId` URL，现有复制逻辑只复制扩展信息。 | 副本创建后重建并更新 DS 三节点定义，将准备和完成回调 URL 改为副本 `taskId`，同步刷新节点与关系元数据。 | 已完成 |
| 4 | P2 | 并发触发会重复消费同一窗口。 | 准备入口没有按任务加锁，也没有重叠执行校验。 | 使用 Redis 原子 `setIfAbsent` 建立按 `taskId` 隔离的运行占用；完成回调、准备异常、RabbitMQ 流程终态和 TTL 均可释放。 | 已完成 |
| 5 | P2 | 从增量切回全量后仍残留旧增量配置。 | `fillIncrementalExt(..., config, ...)` 在 `config == null` 时直接返回。 | 二次发布为全量时显式清空增量模板、边界、数据源、表、字段以及准备/完成节点元数据。 | 已完成 |
| 6 | P2 | FLINKX 缺少显式完成回写节点。 | 当前仅依赖 RabbitMQ `ProcessListener` 消费流程实例消息。 | 已新增 CHUNJUN 后置 HTTP 节点，使用 `${system.workflow.instance.id}` 传入流程实例 ID，回写状态、开始时间和结束时间。 | 已完成 |

## 实施阶段

### 阶段 1：确认 DS 3.2.2 参数替换契约

| No. | 任务 | 实施记录 | 验收口径 | 状态 |
| --- | --- | --- | --- | --- |
| 1.1 | 确认 DS 3.2.2 HTTP 节点如何向后续 CHUNJUN 节点传递输出参数。 | 已核对 DS 3.2.2 官方源码：HTTP 任务将响应体写入 `<任务名>.response`；CHUNJUN 写入 job 文件前执行参数替换。 | 已完成源码确认；最小工作流运行验证并入 6.1。 | 已完成 |
| 1.2 | 定义参数名称和格式。 | 采用 `${<准备节点名称>.response}`，响应体直接为完整 CHUNJUN job JSON。完成 URL 使用 `${system.workflow.instance.id}`。 | ID、TIME 窗口继续由准备服务生成并做 SQL 转义。 | 已完成 |
| 1.3 | 移除运行期流程定义更新。 | 已删除 `CollectorEtlIncrementalServiceImpl.updateDsTask()` 路径；准备回调只返回运行期 JSON。 | 静态扫描未发现旧 `updateDsTask`。 | 已完成 |

### 阶段 2：持久化初始游标并支持空目标表

| No. | 任务 | 实施记录 | 验收口径 | 状态 |
| --- | --- | --- | --- | --- |
| 2.1 | 从前端 FLINKX 草稿中提取初始游标。 | `TaskConverter.resolveFlinkxIncrementalConfig()` 已读取 ID 模式 `incrementStart` 和时间模式单字段 `cursorTime`。 | 待数据库联调确认发布结果。 | 已完成 |
| 2.2 | 增加扩展表字段。 | 已增加 `INCREMENTAL_INITIAL_VALUE`，同步 DO、VO、DTO、Mapper、初始化 SQL 和升级 SQL。 | 静态检查已确认字段齐全。 | 已完成 |
| 2.3 | 允许空目标表。 | `RelationalIncrementalBoundaryQuery.queryMaxValue()` 对成功查询出的 `null` 直接返回；异常仍保留 3 次重试。 | 待空表联调。 | 已完成 |
| 2.4 | 增加兜底规则。 | 准备服务按“目标 MAX -> 初始游标”取开始边界；源表 MAX 为空时注入 `1 = 0`，安全执行空窗口。 | 待场景联调。 | 已完成 |

### 阶段 3：构建三节点工作流与完成回写

| No. | 任务 | 实施记录 | 验收口径 | 状态 |
| --- | --- | --- | --- | --- |
| 3.1 | 新增完成 HTTP 节点定义。 | `TaskConverter` 已生成 `prepare HTTP -> CHUNJUN -> complete HTTP` 三节点、三关系和三坐标。 | 待发布联调。 | 已完成 |
| 3.2 | 新增完成回调 API。 | `CollectorEtlIncrementalController` 已增加 `PUT /complete/{taskId}`，查询参数携带 `processInstanceId`。 | 待 DS 调用联调。 | 已完成 |
| 3.3 | 复用任务实例回写语义。 | 完成服务按 DS 流程实例 ID 更新已有实例状态和起止时间；若 RabbitMQ 创建消息尚未到达，则保留 RabbitMQ 最终回写并记录告警。 | 待重复回调联调。 | 已完成 |
| 3.4 | 持久化完成节点元数据。 | 已新增完成节点 ID、名称、编码、版本和关系 ID 字段，并同步对象、Mapper 和 SQL。 | 静态检查已确认字段齐全。 | 已完成 |

### 阶段 4：防止重叠执行

| No. | 任务 | 实施记录 | 验收口径 | 状态 |
| --- | --- | --- | --- | --- |
| 4.1 | 增加按任务维度的 Redis 分布式锁。 | `IRedisService` 已增加带 TTL 的原子 `setIfAbsent`。准备回调使用 `taskId` 维度运行占用。 | 待并发联调。 | 已完成 |
| 4.2 | 增加运行中占用状态。 | 占用值保存 DS 流程实例 ID；同一流程重试允许继续，不同流程实例被拒绝。默认 TTL 为 86400 秒。 | 待并发联调。 | 已完成 |
| 4.3 | 补齐释放路径。 | 完成回调、准备异常和 RabbitMQ 流程终态均幂等释放；TTL 为最终保护。 | 待异常场景联调。 | 已完成 |

### 阶段 5：复制任务和全量切换

| No. | 任务 | 实施记录 | 验收口径 | 状态 |
| --- | --- | --- | --- | --- |
| 5.1 | 复制后重建副本 URL。 | `copyCreateEtlTask()` 在创建副本 `taskId` 后调用 `rebuildCopiedIncrementalTask()`，重建 DS 三节点定义并重写两个 URL。 | 待副本联调。 | 已完成 |
| 5.2 | 复制全部增量扩展字段。 | 已复制模板、数据源、表、字段、初始游标和三节点元数据；副本不继承旧运行窗口 JSON。历史任务缺少初始游标时，优先从草稿重解析，其次回退旧开始边界。 | 待副本联调。 | 已完成 |
| 5.3 | 增量切回全量时清理元数据。 | `clearIncrementalExt()` 使用显式 `lambdaUpdate().set(..., null)` 清空旧字段，避免 MyBatis 非空更新策略漏清。 | 待全量切换联调。 | 已完成 |

### 阶段 6：验证与上线检查

按仓库约定，不主动运行编译、构建、lint 或测试命令；只有用户明确要求验证时才执行。实现完成后至少准备以下手工验证清单：

| No. | 场景 | 预期结果 | 状态 |
| --- | --- | --- | --- |
| 6.1 | 非空目标表执行增量任务。 | CHUNJUN 使用本次目标 MAX 和源 MAX 组成的动态窗口；DS 流程版本不随执行增长。 | 待验证 |
| 6.2 | 空目标表首次执行。 | 开始边界回退到持久化初始游标，任务正常进入 CHUNJUN。 | 待验证 |
| 6.3 | 空源表执行。 | 不生成非法窗口，不产生重复或无意义写入。 | 待验证 |
| 6.4 | 并发触发同一任务两次。 | 第二次准备请求被拒绝，只有一个 CHUNJUN 实例写入目标表。 | 待验证 |
| 6.5 | 复制增量任务后执行副本。 | 回调 URL 携带副本 `taskId`，原任务扩展记录和上下线状态不变。 | 待验证 |
| 6.6 | 增量任务二次发布切回全量。 | DS 恢复单 CHUNJUN 节点；扩展表不残留增量元数据。 | 待验证 |
| 6.7 | RabbitMQ 与完成 HTTP 回调重复到达。 | 实例状态与起止时间最终一致，不重复创建实例。 | 待验证 |
| 6.8 | 准备节点失败。 | HTTP 返回 500，CHUNJUN 不执行，占用标记可释放，失败日志可定位。 | 待验证 |

## 预计改动文件

| 模块 | 文件或目录 | 计划改动 |
| --- | --- | --- |
| collector biz | `service/etl/impl/CollectorEtlIncrementalServiceImpl.java` | 参数替换、初始游标兜底、锁、完成回写、异常清理。 |
| collector biz | `controller/admin/etl/CollectorEtlIncrementalController.java` | 增加完成回调入口。 |
| collector biz | `service/etl/ICollectorEtlIncrementalService.java` | 扩展准备和完成回写契约。 |
| collector biz | `service/etl/incremental/RelationalIncrementalBoundaryQuery.java` | MAX 为空时正常返回。 |
| collector biz | `utils/TaskConverter.java` | 参数占位符、三节点定义、关系、locations、初始游标提取。 |
| collector biz | `utils/model/FlinkxIncrementalConfig.java` | 增加初始游标。 |
| collector biz | `service/etl/impl/CollectorEtlTaskServiceImpl.java` | 发布、复制、全量切换和元数据维护。 |
| collector api/biz | `CollectorEtlTaskExt*` DTO、VO、DO、Mapper | 增加初始游标和完成节点元数据。 |
| SQL | PostgreSQL 初始化和升级脚本 | 增加扩展表字段。 |

## 当前进度

| 日期 | 项目 | 记录 | 状态 |
| --- | --- | --- | --- |
| 2026-06-02 | 现状审查 | 已确认 4 个上线阻塞项，以及全量切换残留和 FLINKX 完成回写缺口。 | 已完成 |
| 2026-06-02 | 方案选择 | 问题 1 明确采用 DolphinScheduler 参数替换；FLINKX 增加后置 HTTP 完成回写节点。 | 已完成 |
| 2026-06-02 | 代码定位 | 已定位准备入口、运行期 `updateTask()`、空目标表异常、复制逻辑、`fillIncrementalExt()`、RabbitMQ `ProcessListener`。 | 已完成 |
| 2026-06-02 | DS 参数契约确认 | 已核对 DS 3.2.2 官方源码，确认 HTTP 响应变量、CHUNJUN 占位符替换和 `${system.workflow.instance.id}`。 | 已完成 |
| 2026-06-02 | 代码修改 | 已实现参数替换、空目标表兜底、初始游标持久化、三节点工作流、完成回写、并发占用、终态释放、复制重建和全量清理。 | 已完成 |
| 2026-06-02 | 静态检查 | Mapper XML 可解析；初始化 SQL 和升级 SQL 包含新增字段；`git diff --check` 通过；静态扫描未发现旧运行期 `updateDsTask()`。 | 已完成 |
| 2026-06-02 | 编译与运行验证 | 遵循仓库约定，未主动运行编译、测试或 DS 联调。 | 待处理 |

## DS 3.2.2 源码依据

- `HttpTask.addDefaultOutput()` 将 HTTP 响应体写入 `<任务名>.response`。
- `ChunJunTask.buildChunJunJsonFile()` 在写 job JSON 文件前执行参数替换。
- `TaskConstants.PARAMETER_WORKFLOW_INSTANCE_ID` 定义为 `system.workflow.instance.id`。
- 官方源码：`https://github.com/apache/dolphinscheduler/tree/3.2.2`

## 决策记录

| 日期 | 决策 | 原因 |
| --- | --- | --- |
| 2026-06-02 | 不再通过运行期 `updateTask()` 更新本次 CHUNJUN JSON。 | DS 3.2.2 在流程实例启动时已按版本构建执行图，更新新版定义无法影响当前实例。 |
| 2026-06-02 | 使用 DS 参数替换完成运行窗口注入。 | 这是用户指定方案，可保持单一三节点工作流，并避免准备节点再触发独立 CHUNJUN 工作流。 |
| 2026-06-02 | 新增 CHUNJUN 后置 HTTP 回调。 | RabbitMQ 流程实例消息继续保留，但 FLINKX 需要显式回写任务状态和执行时间。 |
| 2026-06-02 | 并发控制采用“短锁 + 运行占用标记”。 | 仅在准备方法内加锁无法覆盖 CHUNJUN 实际执行时间，仍可能重复消费相同窗口。 |

## 记录规则

- 每完成一个任务，立即更新对应状态。
- 在实施记录中写明实际变更文件和关键方法。
- 验证列记录实际执行的命令或手工检查；未执行时明确写“未执行”。
- 不覆盖工作区中用户已有的未提交修改。
- 数据库字段名称如在实现时调整，需同步更新本文档。
