# DataMaster 一键部署

## 当前部署范围与更新边界

- 后端继续使用单个 `datamaster-server.jar` + systemd，前端 `dist` 由 Nginx 提供；不按 Maven 业务模块拆分服务。
- 2026-11-16 已按用户要求使用 `sql/backup_database.py` 从当前开发库导出，并原样替换 `deploy/packages/sql/datamaster.sql`：231 张表、48,371 行数据，未清洗或过滤，未执行恢复验证。导出包含现有用户、数据源配置、日志及测试数据，仅按已确认范围用于当前部署，不应未经审阅作为对外通用安装包发布。
- 全量 SQL 只导入空库；已有用户对象的库跳过全量导入，不自动删表/重灌。正常应用更新建议只选 `datamaster_app_servers,nginx_servers`。
- PostgreSQL 容器使用自带 `psql` 导入，支持 `COPY FROM stdin`；主部署不再调用初始化 JAR，也不要求 PostgreSQL 主机安装 Java。
- Redis 容器内固定 `6379`，宿主端口由 `redis_port` 控制；服务端和应用使用同一密码，健康检查执行认证 PING。
- Nginx 对 `/prod-api/ai/ask-data/chat/stream`、`/prod-api/ai/ask-data/dbgpt/chat/stream` 单独关闭代理缓冲，普通 API 保持原配置。
- DB-GPT 容器直接启动镜像内程序，不在启动时安装 Python 依赖；离线镜像须提前包含 `psycopg2-binary` 等所需依赖。
- 不改模型动态路由、指标授权等业务能力，不调整旧 Docker 构建脚本或 ingestion 子部署脚本。

主入口是 Bash 脚本：

```bash
./deploy/start-all.sh
```

Windows 可以用包装入口：

```powershell
.\deploy\start-all.ps1
```

正式部署前可以先做本地配置预检，不会连接远程机器，也不会在 `--check` 模式生成 token 文件：

```bash
./deploy/start-all.sh --check
```

```powershell
.\deploy\start-all.ps1 -Check
```

如果需要在部署前检查目标机器的运行环境，可以执行远程只读预检。该模式会先执行本地配置检查，再通过 SSH 检查目标机的 sudo 非交互权限、Docker/Docker daemon、Java、systemd、磁盘空间、目录权限和配置端口占用；不会上传文件、删除目录、创建容器或重启服务：

```bash
./deploy/start-all.sh --remote-check
./deploy/start-all.sh --remote-check --limit datamaster_app_servers,nginx_servers
```

PowerShell 包装入口：

```powershell
.\deploy\start-all.ps1 -RemoteCheck
```

端口已被当前服务占用时只输出 warning（升级场景通常是正常的）；Docker、Java、sudo、磁盘或目录权限检查失败会使预检失败。可通过 `remote_check_min_free_mb` 调整根文件系统最低剩余空间阈值，默认 1024 MB。

部署或重启后可以执行远程只读健康检查。它会检查 PostgreSQL/Redis/DB-GPT/Nginx 容器是否运行、DolphinScheduler 和 DataMaster systemd 单元是否 active，并确认对应服务端口正在监听；不会重启服务或修改配置：

```bash
./deploy/start-all.sh --health-check
./deploy/start-all.sh --health-check --limit datamaster_app_servers,nginx_servers
```

PowerShell 包装入口：

```powershell
.\deploy\start-all.ps1 -HealthCheck
```

健康检查失败时会返回非零退出码，适合接入发布流水线。它是运行状态检查，不替代真实业务接口、Kafka 消费和 Doris 写入联调。

如果希望部署命令本身在所有组件完成后自动执行同一套检查，可以追加 `--post-health-check`；只有健康检查通过时部署命令才返回成功：

```bash
./deploy/start-all.sh --post-health-check
```

PowerShell：

```powershell
.\deploy\start-all.ps1 -PostHealthCheck
```

健康检查默认最多等待 120 秒、每 5 秒重试一次，可通过 `health_check_timeout_seconds` 和 `health_check_interval_seconds` 调整。

部署脚本默认在覆盖远程文件或目录前创建备份，备份位于 `deployment_backup_dir`，并按 `deployment_backup_retention` 保留同一目标最近版本。目录备份使用同文件系统硬链接快照，仍会占用 inode；如果现场磁盘布局不支持硬链接，可将 `deployment_backup_enabled` 设为 `false`，但生产环境建议保留开启。

查看某个目标的可用备份：

```bash
bash ./deploy/rollback.sh --list datamaster_app /data/datamaster/app/conf/application-prod.yml
```

明确恢复最近备份（必须显式使用 `--restore`）：

```bash
bash ./deploy/rollback.sh --restore datamaster_app /data/datamaster/app/conf/application-prod.yml
```

Windows 可使用包装入口：

```powershell
.\deploy\rollback.ps1 -Component datamaster_app -RemotePath /data/datamaster/app/conf/application-prod.yml -Restore
```

组件配置了多台主机时必须明确指定目标主机，例如：

```bash
bash ./deploy/rollback.sh --host 192.168.93.175 --list dolphinscheduler /data/datamaster/soft/chunjun
```

回滚只恢复指定文件或目录，不会回滚 PostgreSQL/Redis 持久化数据，也不会自动重启服务；恢复后请重新执行 `--health-check`。

预检还会执行生产配置安全校验。`deployment_environment` 默认按生产环境处理，以下情况会直接失败且不会修改远程机器：

- PostgreSQL 管理员密码、业务密码、Redis 密码或 DB-GPT API key 为空或仍使用 `postgres`、`datamaster`、`password`、`123456` 等示例弱口令；
- 开启外部 ingestion 决策触发，但没有同时配置 `ingestion_decision_trigger_secret` 和 `ingestion_decision_trigger_ref`；
- 开启 Neo4j 血缘，但没有配置非空的 `neo4j_password`。

请在部署前把真实密钥通过私有配置文件或环境变量注入，不要提交到 Git。DB-GPT key 也可以通过 `DASHSCOPE_API_KEY` 环境变量提供。仅限明确隔离的开发/测试环境，才可以在配置中设置：

```yaml
deployment_environment: dev
allow_insecure_defaults: true
```

该豁免会在预检输出 warning；生产环境不要使用。

脚本打印 SSH 命令时会自动把 PostgreSQL/Redis/Neo4j 密码、DolphinScheduler token、模型 API key 和 ingestion 触发密钥替换为 `<redacted>`；实际远程命令仍使用原始值。

部署过程只使用 Bash、`ssh`、`scp` 和单容器 `docker run`，不使用 Ansible，也不使用 Docker Compose。脚本会启用 SSH 连接复用，同一轮部署里每台机器通常只需要输入一次 SSH 密码。文件和目录先由 SSH 用户上传到远程临时目录，再由配置的 sudo 命令安装/替换到最终目录；非 root 用户不需要直接写入 `base_dir`，但必须具备 sudo 非交互权限。

## 部署前确认

先改 `deploy/deploy.yml` 里带 `*` 注释的配置，重点确认：

- SSH 目标：每个组件部署到哪台机器。
- 组件 IP：应用、DS、Redis、PG 之间实际可访问的地址。
- `base_dir`：远程统一部署根目录，默认 `/data/datamaster`。
- PG/Redis 端口和密码。
- `jwt_secret`：DataMaster JWT 签名密钥，必须使用随机值；部署模板会覆盖源码中的默认值。生产模板同时关闭历史万能密码和 Knife4j basic/debug 能力。
- DolphinScheduler API 端口。token 可以留空，脚本会自动生成并写入 DS 库。
- DolphinScheduler tenant code：必须和 DS worker 执行任务使用的 Linux 用户一致，默认 `root`。
- DB-GPT 部署地址、端口和模型密钥：`dbgpt_ssh_host`、`dbgpt_port`、`dashscope_api_key`、`ai_skill_model_*`。
- DataMaster 内置 ingestion 是否启用及连接配置：`ingestion_*`。Kafka/Doris 集群由独立脚本 `deploy/ingestion/deploy.sh` 部署，不混入主部署组。
- Neo4j 血缘是否启用：`lineage_enabled`、`neo4j_uri`、`neo4j_username`、`neo4j_password`。Neo4j 默认关闭，不是核心业务必需组件。
- 离线包文件名和目录是否与 `deploy/deploy.yml` 一致。
- Chunjun/Flink 是否已经放到 `deploy/packages/soft/chunjun` 和 `deploy/packages/soft/flink`。

## 离线包目录

把文件放到：

```text
deploy/packages
├── components
│   ├── postgres-15.8-alpine.tar
│   ├── redis-6-alpine.tar
│   ├── dbgpt-openai-latest.tar
│   ├── datamaster-server.jar
│   ├── dist/
│   └── apache-dolphinscheduler-<dolphinscheduler_version>-bin.tar.gz
├── soft
│   ├── chunjun/
│   └── flink/
└── sql
    ├── datamaster.sql
    └── dolphinscheduler.sql
```

上面的文件名和目录名来自 `deploy/deploy.yml`：

```yaml
postgresql_image_tar: postgres-15.8-alpine.tar
redis_image_tar: redis-6-alpine.tar
dolphinscheduler_version: "3.4.1"
dolphinscheduler_install_tgz: apache-dolphinscheduler-{{ dolphinscheduler_version }}-bin.tar.gz
postgresql_init_sql_src: packages/sql/datamaster.sql
dolphinscheduler_init_sql_src: packages/sql/dolphinscheduler.sql
soft_package_src: packages/soft
```

脚本处理方式：

- `components/*.tar`：存在则上传到远程 `{{ base_dir }}/packages` 并执行 `docker load -i`；不存在则跳过，继续使用服务器已有镜像。
- `components/*.tar.gz`：上传到远程 `{{ base_dir }}/packages`，DolphinScheduler 安装脚本会自动解压。
- `sql/datamaster.sql` 和 `sql/dolphinscheduler.sql`：以受限权限上传到远程 `{{ base_dir }}/init-sql`，由 PostgreSQL 容器内的 `psql` 仅向空库导入；不需要 `datamaster-db-init.jar`。
- `soft/chunjun/` 和 `soft/flink/`：目录里有实际文件时上传到远程 `{{ base_dir }}/soft`。
- 文件通过 SSH 用户可写的 `/tmp/datamaster-deploy-file.*` 临时文件上传，再由 sudo `install` 写入最终路径；目录通过 `/tmp/datamaster-deploy-dir.*` 暂存后由 sudo 替换目标目录。部署失败时可能残留临时文件/目录，应由运维按命名前缀清理。

后端生成的 `application-prod.yml` 还会写入：

- `datamaster.ingestion`：Kafka、Doris、冲突策略和决策触发配置；
- `datamaster.ontology.workflow`：Workflow 异步 Worker 开关、轮询间隔、批量大小和陈旧运行恢复阈值（`WORKFLOW_WORKER_ENABLED`、`WORKFLOW_WORKER_DELAY_MS`、`WORKFLOW_WORKER_BATCH_SIZE`、`WORKFLOW_WORKER_STALE_TIMEOUT_MS`，默认 300000 毫秒且运行时最小 60000 毫秒）；
- `datamaster.ontology.webhook`：外部 Webhook 动作的 SSRF 白名单和端口限制。生产建议设置 `WEBHOOK_ALLOWED_HOSTS`（逗号分隔的精确 host 或 `*.example.com`）和 `WEBHOOK_ALLOWED_PORTS`（默认 `80,443`）；空白 host 白名单仍会阻止内网/保留地址，但允许公网地址；
- `datamaster.lineage`：Neo4j 血缘开关；
- `spring.data.neo4j`：启用血缘时使用的连接配置。

ingestion 消费代码随 DataMaster server 运行；Kafka、Doris 集群单独维护，部署说明见 `deploy/ingestion/README.md`。Neo4j 仍是外部可选中间件。

数据到达触发默认使用主服务进程内调用的 `/ont/action/trigger/data-arrival`；只有拆分为独立接收进程时，才需要配置 `ingestion_decision_base_url`、`ingestion_decision_trigger_ref` 和 `ingestion_decision_trigger_secret`。

## 执行过程

`deploy/start-all.sh` 会按下面顺序执行：

1. 读取 `deploy/deploy.yml`，把模板里的 `{{ variable }}` 替换成实际值。
2. 通过 `ssh`/`scp` 连接每台目标机器，创建 `base_dir` 下的组件目录。
3. 部署 PostgreSQL：上传镜像 tar 并 `docker load`，再用 `docker run` 启动 PG。
4. 准备数据库：通过容器内 `psql` 创建缺失的用户和数据库，分别判断业务库/DS 库是否为空；仅向空库导入完整 SQL。已有用户对象则跳过，不修改已有数据库所有者或重置已有用户密码。DS 新装成功后才进行旧回调地址适配；部署仍会按配置写入 DS tenant/API token，因此重部署 PostgreSQL 组并非完全只读。
5. 可选业务升级 SQL：`postgresql_app_upgrade_sql_src` 默认空，完整新基线不再重复执行历史六份增量。只有现场明确需要升级已有库时，才填写经过审核且尚未执行的脚本（逗号分隔）；脚本不会自动推断迁移状态。
6. 部署 Redis：上传镜像 tar、生成 `redis.conf`，再用 `docker run` 启动 Redis。
7. 部署 DolphinScheduler：上传 tar 包、Chunjun、Flink，解压后自动修改 DS 数据源、JDBC 注册中心和运行环境，再生成 systemd 服务。
8. 部署 DB-GPT：上传镜像 tar 并用 `docker run` 启动 `datamaster-dbgpt`。
9. 部署 DataMaster server：上传 `datamaster-server.jar`，生成 `application-prod.yml` 和 systemd 服务后启动。
10. 部署 Nginx：上传前端 `dist`，生成 `nginx.conf` 并启动 `datamaster-nginx` 容器。

其中 DataMaster 配置文件只下发 ingestion 的连接参数和开关；Kafka、Doris 集群不在第 9 步安装，需要单独执行 `deploy/ingestion/deploy.sh`。

## 单独部署

可以用 `--limit` 只部署某一个或几个组件。`--limit` 的值是部署组名，多个组用英文逗号分隔：

```bash
./deploy/start-all.sh --limit postgresql_servers
./deploy/start-all.sh --limit redis_servers
./deploy/start-all.sh --limit dolphinscheduler_servers
./deploy/start-all.sh --limit dbgpt_servers
./deploy/start-all.sh --limit datamaster_app_servers
./deploy/start-all.sh --limit nginx_servers
```

也可以组合部署：

```bash
./deploy/start-all.sh --limit postgresql_servers,redis_servers
./deploy/start-all.sh --limit dolphinscheduler_servers
./deploy/start-all.sh --limit datamaster_app_servers,nginx_servers
```

预检同样支持 `--limit`，只检查对应组件需要的配置和本地文件；不会因为未选择的组件缺少配置而失败：

```bash
./deploy/start-all.sh --check --limit dolphinscheduler_servers
./deploy/start-all.sh --check --limit datamaster_app_servers,nginx_servers
```

不传 `--limit` 时，默认部署：

```text
postgresql_servers,redis_servers,dolphinscheduler_servers,dbgpt_servers,datamaster_app_servers,nginx_servers
```

DolphinScheduler 默认使用 JDBC 注册中心，不需要 ZooKeeper。

## 启停服务

组件部署完成后，可以用 `deploy/start`、`deploy/stop` 或 `deploy/service.sh` 对远程服务做启动、停止、重启和状态查看。命令会读取 `deploy/deploy.yml` 中对应组件的 `*_ssh_host`，并在目标机器上执行 Docker 或 systemd 命令。

启动服务：

```bash
./deploy/start postgresql
./deploy/start redis
./deploy/start dolphinscheduler
./deploy/start dbgpt
./deploy/start backend
./deploy/start nginx
./deploy/start all
```

停止服务：

```bash
./deploy/stop nginx
./deploy/stop backend
./deploy/stop dbgpt
./deploy/stop dolphinscheduler
./deploy/stop redis
./deploy/stop postgresql
./deploy/stop all
```

重启和查看状态使用 `service.sh`：

```bash
./deploy/service.sh restart dolphinscheduler
./deploy/service.sh restart backend
./deploy/service.sh status all
./deploy/service.sh status dolphinscheduler
```

支持的服务名：

```text
postgresql, redis, dolphinscheduler, dbgpt, backend, app, nginx, all
```

别名：

```text
postgresql: postgres, pg
dolphinscheduler: ds
backend: app, datamaster-server
```

`all` 的启动顺序是 PostgreSQL、Redis、DolphinScheduler、DB-GPT、backend、Nginx；停止顺序相反。

> 注意：`service.sh` 只负责远程 Docker/systemd 启停，不会自动检查端口、磁盘、Docker 镜像 tag、Java/Python 版本或数据库备份。上线前请结合根目录的 [`docs/项目现状与缺陷分析.md`](../docs/项目现状与缺陷分析.md) 完成环境核对。

## 完整数据库 SQL 与初始化

主部署使用 PostgreSQL 镜像自带 `psql`，不再调用 `deploy/db-init` 的 JDBC 分号执行器。历史初始化工具源码/构建入口保留，但不是当前主部署步骤。

请从已完成结构更新的数据库导出完整普通文本 SQL，包含结构及所需初始化数据，例如在有 PostgreSQL 客户端的环境中执行（连接信息通过安全方式配置，不在命令中写密码）：

```bash
pg_dump --format=plain --no-owner --no-privileges --file=datamaster.sql datamaster
```

将审核后的文件替换到 `deploy/packages/sql/datamaster.sql`。导出中不要包含 `--create` 产生的建库/切库指令、额外 `BEGIN/COMMIT`、角色/所有者切换或只能在超级用户下恢复的内容；客户端版本应与目标 PostgreSQL 15 兼容。允许默认的 `COPY FROM stdin`，不必强制转换成 INSERT。排除实际密钥、不需要的业务/测试数据；不要把生产全量数据作为通用安装包提交。

业务库和 DS 库分别检查：只要存在用户表、视图、序列、函数或类型等对象，就不执行对应的完整 SQL。空库导入在同一事务内检查并执行，使用事务级 advisory lock 防止本脚本并发重复导入；失败即退出，不继续部署。该锁不能阻止其他工具同时操作数据库，初始化期间仍须避免外部写入。

- 默认 `postgresql_app_upgrade_sql_src: ""`，不重复执行已包含在完整基线中的历史迁移。
- 已有库不会被完整 SQL 更新；需要变更时人工备份后选择明确的升级 SQL，不通过删库重灌升级。
- 已存在用户的密码和数据库所有者不会被重置；配置必须与现场实际值一致，密码轮换另行处理。
- 文件备份不等于数据库备份；恢复数据库必须另行明确授权执行。

DS API token 不需要提前知道。`deploy.yml` 里的 `dolphinscheduler_token` 留空时，脚本会第一次运行时生成：

```text
deploy/.runtime/dolphinscheduler.token
```

之后会复用这个 token。数据库初始化后，脚本会把同一个 token 写入 DS 库：

```text
t_ds_access_token
```

DataMaster server 的 `application-prod.yml` 也会使用同一个 token。

DS tenant 也由部署配置控制：

```yaml
dolphinscheduler_tenant_code: root
```

这个值必须和 DS worker 执行任务使用的 Linux 用户一致。默认 `root` 是因为当前 systemd 方式部署 DS 时服务由 root 启动；如果现场改成 `datamaster`、`dolphinscheduler` 等用户，就把这里改成对应 Linux 用户。

初始化后，脚本会把它写入 DS 库：

```text
t_ds_tenant
```

DS 安装脚本也会检查这个 Linux 用户；如果不是 `root` 且用户不存在，会自动 `useradd`。DataMaster server 的 `application-prod.yml` 会写入：

```yaml
ds:
  tenant_code: <dolphinscheduler_tenant_code>
```

## PostgreSQL 执行流程

PostgreSQL 使用单容器 `docker run` 部署，不使用 Compose。

脚本会先创建远程目录：

```text
{{ postgresql_data_dir }}
{{ postgresql_conf_dir }}
{{ postgresql_log_dir }}
```

然后检查本地镜像 tar：

```yaml
postgresql_image_tar_src: packages/components/postgres-15.8-alpine.tar
```

如果文件存在，会上传到：

```text
{{ remote_package_dir }}/postgres-15.8-alpine.tar
```

并执行：

```bash
docker load -i {{ remote_package_dir }}/postgres-15.8-alpine.tar
```

之后会删除旧容器并重新启动：

```text
容器名：datamaster-postgresql
镜像：postgres:15.8-alpine
端口：{{ postgresql_port }}:5432
数据目录：{{ postgresql_data_dir }} -> /var/lib/postgresql/data
日志目录：{{ postgresql_log_dir }} -> /var/log/postgresql
```

PG 启动后，脚本等待 `pg_isready` 成功，使用容器内 `psql` 创建缺失用户/库，再仅向空库导入完整 SQL。不会调用初始化 JAR，也不会在已有库重新执行全量 dump。

## Redis 执行流程

Redis 使用单容器 `docker run` 部署。

脚本会先创建远程目录：

```text
{{ redis_data_dir }}
{{ redis_conf_dir }}
{{ redis_log_dir }}
```

然后检查本地镜像 tar：

```yaml
redis_image_tar_src: packages/components/redis-6-alpine.tar
```

如果文件存在，会上传到远程并执行 `docker load -i`。

脚本会根据 `deploy/templates/redis.conf.j2` 生成：

```text
{{ redis_conf_dir }}/redis.conf
```

配置内容包括：

```text
bind 0.0.0.0
port 6379
appendonly yes
dir /data
logfile ""
requirepass "<经过转义的 redis_password>"
```

`redis_port` 只控制宿主端口，容器内固定监听 `6379`。密码以字符串转义后同时下发 Redis 服务端、Spring Redis 和 DS Redis 客户端，避免空格、引号、`#` 等改变配置含义。空密码渲染为 `requirepass ""`，仅用于明确隔离的开发环境，生产预检仍拒绝空密码。健康检查通过 `REDISCLI_AUTH` 执行真实 PING，不再因配置了密码就跳过认证检查；命令展示会脱敏。

最后会删除旧容器并重新启动：

```text
容器名：datamaster-redis
镜像：redis:6-alpine
端口：{{ redis_port }}:6379
数据目录：{{ redis_data_dir }} -> /data
配置文件：{{ redis_conf_dir }}/redis.conf -> /etc/redis/redis.conf
日志目录：{{ redis_log_dir }} -> /logs
```

## DolphinScheduler 自动配置

DS 的配置不是手工改静态文件，而是在远程解压后由模板脚本自动处理：

```text
deploy/templates/install-dolphinscheduler.sh.j2
```

脚本会生成并执行远程安装脚本，自动修改/写入：

```text
{{ dolphinscheduler_dir }}/datamaster-ds-env.sh
{{ dolphinscheduler_dir }}/apache-dolphinscheduler-{{ dolphinscheduler_version }}-bin/conf/common.properties
{{ dolphinscheduler_dir }}/apache-dolphinscheduler-{{ dolphinscheduler_version }}-bin/conf/dolphinscheduler_env.sh
{{ dolphinscheduler_dir }}/apache-dolphinscheduler-{{ dolphinscheduler_version }}-bin/*/conf/application.yaml
```

数据源地址由 `postgresql_ssh_host` 推导，端口和账号来自 `deploy/deploy.yml`：

```yaml
postgresql_ssh_host: "<POSTGRESQL_IP>"
postgresql_port: 5432
dolphinscheduler_database: dolphinscheduler
postgresql_user: datamaster
postgresql_password: "datamaster"
```

会写成：

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<POSTGRESQL_IP>:5432/dolphinscheduler?stringtype=unspecified
SPRING_DATASOURCE_USERNAME=datamaster
SPRING_DATASOURCE_PASSWORD=datamaster
```

运行环境也来自 `deploy/deploy.yml`。DolphinScheduler 注册中心使用 JDBC，直接复用 DS 的 PostgreSQL 数据源，不再依赖 ZooKeeper：

```yaml
dolphinscheduler_resource_dir: "{{ base_dir }}/dolphinscheduler-resource"
chunjun_home: "{{ remote_soft_dir }}/chunjun"
flink_home: "{{ remote_soft_dir }}/flink"
```

会写入：

```text
REGISTRY_TYPE=jdbc
RESOURCE_STORAGE_TYPE=LOCAL
RESOURCE_LOCAL_BASE_PATH=/data/datamaster/dolphinscheduler-resource
CHUNJUN_HOME=/data/datamaster/soft/chunjun
FLINK_HOME=/data/datamaster/soft/flink
```

脚本还会把各 `*/conf/application.yaml` 里的 `registry.type` 改成 `jdbc`。对缺少 `spring.datasource` 的节点配置，例如 worker，也会补入同一套 PostgreSQL 数据源，避免服务启动后继续尝试连接 ZooKeeper。

DS API token 由部署脚本自动生成或复用：

```yaml
dolphinscheduler_token: ""
```

脚本会在 PG 初始化完成后写入 DS 库的 `t_ds_access_token`，同时把它渲染到 DataMaster server 配置里的 `ds.token`。

最后会创建并启动这些 systemd 服务：

```text
datamaster-dolphinscheduler-api.service
datamaster-dolphinscheduler-master.service
datamaster-dolphinscheduler-worker.service
datamaster-dolphinscheduler-alert.service
```

## DB-GPT 执行流程

DB-GPT 使用单容器 `docker run` 部署。

脚本会先创建远程目录：

```text
{{ dbgpt_data_dir }}
{{ dbgpt_message_dir }}
```

然后检查本地镜像 tar：

```yaml
dbgpt_image_tar_src: packages/components/dbgpt-openai-latest.tar
```

如果文件存在，会上传到远程并执行 `docker load -i`；不存在则使用服务器已有镜像或在线拉取能力。

最后删除旧容器并重新启动：

```text
容器名：datamaster-dbgpt
端口：{{ dbgpt_port }}:5670
数据目录：{{ dbgpt_data_dir }} -> /app/pilot/data
消息目录：{{ dbgpt_message_dir }} -> /app/pilot/message
```

`dashscope_api_key` 会注入为 `DASHSCOPE_API_KEY`。如果现场不用通义代理模型，需要同步调整 `dbgpt_image` 或 DB-GPT 启动命令。

容器直接执行 `dbgpt start webserver --config /app/configs/dbgpt-proxy-tongyi.toml`，启动时不再执行 `pip install`。离线镜像必须提前包含 PostgreSQL 驱动 `psycopg2-binary` 及该版本所需依赖；本次脚本修改未构建或检查镜像内容。

## DataMaster Server 执行流程

主服务当前使用 `java -jar` + systemd 部署，不使用 DataMaster 后端容器。

DataMaster 2.0 主服务要求 Java 17+。部署前在后端主机安装兼容的 JDK/JRE，并在 `deploy.yml` 中填写实际存在的绝对路径：

```yaml
datamaster_java_bin: /opt/jdks/jdk-17/bin/java
```

示例目录不会由脚本自动安装。预检和实际部署都会检查所选 Java 的版本，后端使用该路径生成 systemd `ExecStart`；不会修改系统 `PATH`、`JAVA_HOME` 或 DolphinScheduler/Flink/CHUNJUN 的 JVM。PostgreSQL 初始化已改用容器内 `psql`，不再读取 `database_init_java_bin` 或 `database_init_jar_src`，旧私有配置中的这两个键可移除。

脚本会创建远程目录：

```text
{{ app_conf_dir }}
{{ app_log_dir }}
{{ app_upload_dir }}
```

然后检查本地后端 jar：

```yaml
datamaster_server_jar_src: packages/components/datamaster-server.jar
```

脚本会上传 jar 到：

```text
{{ app_jar_path }}
```

脚本只使用 `deploy/config/datamaster-server-application-prod.yml.j2` 生成：

```text
{{ app_conf_dir }}/application-prod.yml
```

主服务配置会写入：

```text
PostgreSQL 地址、端口、库名、用户名、密码
Redis 地址、端口、密码
DolphinScheduler API 地址和自动生成/复用的 token
DS resource 路径
DB-GPT 地址、问数 chat mode、Skill 知识空间
AI Skill 模型增强配置
ingestion 的 Kafka、Doris、冲突策略和决策触发配置
Neo4j 血缘开关和连接配置
```

最后生成并重启 systemd 服务：

```text
服务名：datamaster-server.service
端口：{{ datamaster_server_port }}
工作目录：{{ app_dir }}
配置文件：{{ app_conf_dir }}/application-prod.yml
日志：systemd journal
上传目录：{{ app_upload_dir }}
```

systemd 服务会设置：

```text
SPRING_PROFILES_ACTIVE=prod
DS_INCREMENTAL_PREPARE_URL
DS_INCREMENTAL_COMPLETE_URL
AI_SKILL_MODEL_API_KEY
```

## 配置文件

- 统一配置：`deploy/deploy.yml`
- 应用、systemd、Nginx 配置模板：`deploy/config`，脚本显式读取该目录，不回退到旧副本。
- Redis、DolphinScheduler 安装模板：`deploy/templates`。
- `deploy/templates/datamaster-server-application-prod.yml.j2` 仅保留旧路径说明，不再保存第二份应用配置。

`deploy/deploy.yml` 同时声明 SSH 目标、组件 IP、端口、镜像、账号密码、组件目录。远程统一部署根目录也在这里：

```yaml
base_dir: /data/datamaster
```

---

# 本地开发环境

当前本地开发依赖运行在 `Ubuntu` WSL 的 Docker 中，统一使用仓库下的辅助脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\docker\start-dev-services.ps1
```

该脚本启动现有 PostgreSQL、Redis 和 DolphinScheduler 容器，不会删除或重建数据卷。详细说明见 `docker/README.md`。

后端和前端启动：

```bash
mvn -pl datamaster-server -am package
java -jar datamaster-server/target/datamaster-server.jar --server.port=8989

cd datamaster-ui
npm install
npm run dev
```

当前工程模块以根 `pom.xml` 为准，主要包括 `datamaster-governance`、`datamaster-metadata`、`datamaster-assets`、`datamaster-ontology`、`datamaster-service`、`datamaster-collector`、`datamaster-ai` 和 `datamaster-ingestion`。完整架构和模块流程见根目录 `README.md`。

## 质量模块错误明细存储配置

质量模块将校验错误明细写入业务库 JDBC 表而非 MongoDB。配置方式：

1. 登录系统 → 质量 → 存储配置
2. 选择一个 JDBC 类型的数据源（MySQL/PostgreSQL/DM8/Oracle/Kingbase8/SQL_Server/DB2/ClickHouse/Doris/Hive/MariaDB/OSCAR）
3. 填写表名（默认 `quality_error_data`）
4. 保存后主服务自动通知 quality 刷新缓存

质量模块启动时自动检查 `quality_error_storage_config` 表，有配置则使用 JDBC 写入，无配置则跳过错误明细写入。
