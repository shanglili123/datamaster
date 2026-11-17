# DB-GPT 多模型接口自动同步

## 功能

模型调用监控页仍是唯一的模型配置维护入口。启用本功能后：

1. 保存启用的模型配置，在本地事务内写入同步任务；不在保存事务内调用远程 HTTP。
2. 后台每5秒检查已提交任务，小批量注册到 DB-GPT。页面显示待同步、同步中、已同步在线、失败，并提供同步/重试。
3. 每空间、每配置、每连接版本使用独立别名：`dm_<namespace>_s<spaceId>_p<profileId>_v<revision>`；真实供应商模型放 `params.backend`。
4. 数据问数、流式问数、报告按当前空间选择该别名：路由开启时用高级模型，否则用空间默认模型；可选 `modelProfileId` 只能选择当前空间配置。同步模式不接受旧裸 `model` 名称。
5. 问数前再查远端模型健康；丢失注册时重新排队并拒绝当前请求，不回退旧版或公共模型。

不建设统一模型代理，不修改容器文件，不重启容器，不停止或删除远程实例。已有 qwen-plus、向量模型不动。配置升级留下的旧实例须由运维另行清理；禁用/删除本地配置只禁止 DataMaster 再选择它，**不撤销远端已经安装的密钥或阻止绕过 DataMaster 的直接访问**。

## 已核实与未核实

只读查询 `192.168.93.176:5670/openapi.json` 返回版本标识 0.8.1，模型管理接口为：

- GET `/api/v2/serve/model/models`
- GET `/api/v2/serve/model/model-types`
- POST `/api/v2/serve/model/models`
- POST `/api/v2/serve/model/models/start`、`/stop`（当前实现不自动调用）

创建结构：`host,port,model,worker_type:"llm",params`；proxy/openai 参数包含 name、backend、provider、api_base、api_key、concurrency。管理节点从 model-types 返回信息获取，不硬编码容器IP。

**本次没有向远程创建模型、发送供应商密钥或调用模型推理**。独立别名注册、供应商工具兼容及重启行为仍需现场验收。返回 success 不等于可用，代码还会核查健康状态；健康状态也不代表真实供应商调用已成功。

## 上线步骤

1. 备份应用数据库，审阅并手工执行 `sql/update_20261116_dbgpt_model_sync.sql`。该脚本添加模型配置版本及持久同步任务表，不导入密钥、不执行远程请求。
2. 建立 DataMaster 后端到 DB-GPT 管理接口的安全通道和访问控制。只读核查时，该远程管理 GET 未要求鉴权，且是明文 HTTP；**不得据此认为适合直接开放凭据注册**。推荐 TLS + 管理Bearer，或受限的回环 SSH 隧道。DB-GPT管理接口本身还应禁止普通用户直接访问。
3. 在服务器部署配置中接线一次，示例如下；占位符需替换为本环境值，不在源码填真实 Key：

```yaml
db-gpt:
  # 问数服务地址；仍使用现有值
  url: https://dbgpt.example.internal
  model-sync:
    enabled: true
    management-url: https://dbgpt-management.example.internal
    management-token: ${DBGPT_MANAGEMENT_TOKEN}
    namespace: dmprod01
    allow-insecure-http: false
    timeout-millis: 15000
    concurrency: 4
    max-attempts: 3
    poll-interval-ms: 5000
```

namespace 为4到24位小写字母数字，必须稳定、不同 DataMaster 部署不可共用。管理地址须指向**问数实际使用的同一DB-GPT集群**，可以是该集群受保护的管理网关，不能指向另一个环境。

若通过 SSH 隧道，把 management-url 配为本进程可访问的 `http://127.0.0.1:<本地转发端口>`。生产环境应确保DB-GPT自身管理入口访问控制到位，不能仅依赖隧道而保留可被绕过的公开端口。仅在已确认的受控内网中，才可显式 `allow-insecure-http: true`；此选项不是加密，不提供鉴权。

4. 部署后，已有启用配置会由后台补齐任务；页面新增/修改后自动排队，失败可以点击同步/重试，权限沿用 `ai:model-config:edit` 与平台管理员/系统运维空间校验。
5. 等待配置显示在线，设置空间默认模型或路由的高级模型，再验收问数。切换默认/路由不重启容器、不复制配置文件。

默认 `enabled:false`：升级不立即外发存量密钥，页面明确显示“同步未启用”，旧独立DB-GPT模式保持。开启后不允许因为同步失败自动退回独立公共模型。

## 版本和持久任务

模型标识、兼容API地址、provider、非空Key、启用状态变化推进版本；空Key保持原密钥，名称修改不推进版本。每个版本和目标集群一条任务，凭据仍由已有AES-GCM服务在工作线程按空间解密，不存入任务表。

任务使用主库原子领取与5分钟租约，单次轮询最多处理2条；超时后下次先查询别名再决定是否创建。失败按15秒退避，默认最多3次，然后等待人工重试。进程退出后的超时租约可接管。结果只按相同版本、领取令牌和当前启用配置回写；旧结果不能覆盖新配置。远端响应体、供应商Key和原始异常不写状态或日志。

若远端已有相同别名但不健康，当前实现不会强停/删除或覆盖，而是等待恢复、有限重试并提示失败。若需要worker管理操作，应由运维单独处理；不能为了自动同步随意停止已有服务。

## 空间与统计边界

- 同步模式要求规范空间ID/code及成员关系；远端会话ID按用户+空间+本地会话派生，避免不同空间共用会话历史。旧独立模式继续兼容原会话名称。
- 同步模式的工具问数/报告取高级模型，并不实现 DB-GPT 内部每一步 economy/advanced 自动分类或统一代理并发。向量/重排模型不在本次同步范围。
- DB-GPT编排继续是 DBGPT 请求统计，不代表内部模型实际调用数或完整Token；同步管理请求不计为模型调用。
- 供应商Key会被安装至受信DB-GPT，用户只维护一处，**不等于密钥只留DataMaster**。模型管理网络隔离、DB-GPT配置和日志保密仍由部署保障。

## 验证状态与本地命令

遵循仓库规定，未运行编译、测试、Lint、生产构建、数据库迁移或真实模型调用。新增管理接口参数、别名/backend、健康与幂等、受控传输、持久任务、版本隔离、空间选模与会话隔离的测试代码，待本地运行：

```bash
mvn -pl datamaster-ai -am test -Dtest=DbGptModelManagementClientTest,DbGptModelSelectionServiceTest,DbGptModelSyncServiceTest,AiModelProfileServiceTest,AiModelRoutingServiceTest,AiModelProfileControllerTest -Dsurefire.failIfNoSpecifiedTests=false
cd datamaster-ui
npm run eslint:lint
npm run build:prod
```

现场验收应在安全通道建立且获准远程写入后进行：新建模型→READY→真实问数；改Key/地址→新别名且旧版本不覆盖；断网失败可重试；远端重启丢注册后恢复；跨空间不可选；本地禁用后不再选择，但远端旧实例仍存在需人工处理。不得把未运行的mock或GET健康检查当作真实供应商调用成功。
