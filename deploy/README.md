# DataMaster 一键部署

主入口是 Bash 脚本：

```bash
./deploy/start-all.sh
```

Windows 可以用包装入口：

```powershell
.\deploy\start-all.ps1
```

正式部署前可以先做本地配置预检，不会连接远程机器：

```bash
./deploy/start-all.sh --check
```

```powershell
.\deploy\start-all.ps1 -Check
```

部署过程只使用 Bash、`ssh`、`scp` 和单容器 `docker run`，不使用 Ansible，也不使用 Docker Compose。脚本会启用 SSH 连接复用，同一轮部署里每台机器通常只需要输入一次 SSH 密码。

## 部署前确认

先改 `deploy/deploy.yml` 里带 `*` 注释的配置，重点确认：

- SSH 目标：每个组件部署到哪台机器。
- 组件 IP：应用、DS、Redis、PG 之间实际可访问的地址。
- `base_dir`：远程统一部署根目录，默认 `/data/datamaster`。
- PG/Redis 端口和密码。
- DolphinScheduler API 端口。token 可以留空，脚本会自动生成并写入 DS 库。
- DolphinScheduler tenant code：必须和 DS worker 执行任务使用的 Linux 用户一致，默认 `root`。
- DB-GPT 部署地址、端口和模型密钥：`dbgpt_ssh_host`、`dbgpt_port`、`dashscope_api_key`、`ai_skill_model_*`。
- 离线包文件名和目录是否与 `deploy/deploy.yml` 一致。
- Chunjun/Flink 是否已经放到 `deploy/packages/soft/chunjun` 和 `deploy/packages/soft/flink`。

## 离线包目录

把文件放到：

```text
deploy/packages
├── components
│   ├── postgres-15.tar
│   ├── redis-7.2.tar
│   ├── dbgpt-openai-latest.tar
│   ├── datamaster-server.jar
│   ├── dist/
│   ├── apache-dolphinscheduler-<dolphinscheduler_version>-bin.tar.gz
│   └── datamaster-db-init.jar
├── soft
│   ├── chunjun/
│   └── flink/
└── sql
    ├── datamaster.sql
    └── dolphinscheduler.sql
```

上面的文件名和目录名来自 `deploy/deploy.yml`：

```yaml
postgresql_image_tar: postgres-15.tar
redis_image_tar: redis-7.2.tar
dolphinscheduler_version: "3.4.1"
dolphinscheduler_install_tgz: apache-dolphinscheduler-{{ dolphinscheduler_version }}-bin.tar.gz
database_init_jar_src: packages/components/datamaster-db-init.jar
postgresql_init_sql_src: packages/sql/datamaster.sql
dolphinscheduler_init_sql_src: packages/sql/dolphinscheduler.sql
soft_package_src: packages/soft
```

脚本处理方式：

- `components/*.tar`：存在则上传到远程 `{{ base_dir }}/packages` 并执行 `docker load -i`；不存在则跳过，继续使用服务器已有镜像。
- `components/*.tar.gz`：上传到远程 `{{ base_dir }}/packages`，DolphinScheduler 安装脚本会自动解压。
- `components/datamaster-db-init.jar`：上传到远程后执行，用于创建数据库、用户并导入 SQL。
- `sql/datamaster.sql` 和 `sql/dolphinscheduler.sql`：上传到远程 `{{ base_dir }}/init-sql` 后由初始化 jar 导入。
- `soft/chunjun/` 和 `soft/flink/`：目录里有实际文件时上传到远程 `{{ base_dir }}/soft`。
- 文件直接上传到最终部署目录，不使用 `/tmp` 或其他远程中转目录。

## 执行过程

`deploy/start-all.sh` 会按下面顺序执行：

1. 读取 `deploy/deploy.yml`，把模板里的 `{{ variable }}` 替换成实际值。
2. 通过 `ssh`/`scp` 连接每台目标机器，创建 `base_dir` 下的组件目录。
3. 部署 PostgreSQL：上传镜像 tar 并 `docker load`，再用 `docker run` 启动 PG。
4. 初始化数据库：上传 `datamaster-db-init.jar`、`datamaster.sql`、`dolphinscheduler.sql`，然后执行 jar 创建库、用户并导入 SQL，再写入 DS tenant 和 DS API token。
5. 可选应用业务升级 SQL：只有配置 `postgresql_app_upgrade_sql_src` 时才会额外执行；全量 SQL 包场景默认不需要。
6. 部署 Redis：上传镜像 tar、生成 `redis.conf`，再用 `docker run` 启动 Redis。
7. 部署 DolphinScheduler：上传 tar 包、Chunjun、Flink，解压后自动修改 DS 数据源、JDBC 注册中心和运行环境，再生成 systemd 服务。
8. 部署 DB-GPT：上传镜像 tar 并用 `docker run` 启动 `datamaster-dbgpt`。
9. 部署 DataMaster server：上传 `datamaster-server.jar`，生成 `application-prod.yml` 和 systemd 服务后启动。
10. 部署 Nginx：上传前端 `dist`，生成 `nginx.conf` 并启动 `datamaster-nginx` 容器。

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

预检同样支持 `--limit`，只检查对应组件需要的配置和本地文件：

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

## 初始化 jar

重新生成数据库初始化 jar：

```bash
./deploy/build-db-init.sh
```

jar 会初始化：

- 业务库：`datamaster`
- DS 库：`dolphinscheduler`
- 业务用户：`datamaster/datamaster`

PG 容器管理员账号默认是 `postgres/postgres`，只用于初始化。

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
postgresql_image_tar_src: packages/components/postgres-15.tar
```

如果文件存在，会上传到：

```text
{{ remote_package_dir }}/postgres-15.tar
```

并执行：

```bash
docker load -i {{ remote_package_dir }}/postgres-15.tar
```

之后会删除旧容器并重新启动：

```text
容器名：datamaster-postgresql
镜像：postgres:15
端口：{{ postgresql_port }}:5432
数据目录：{{ postgresql_data_dir }} -> /var/lib/postgresql/data
日志目录：{{ postgresql_log_dir }} -> /var/log/postgresql
```

PG 启动后，脚本会等待 `pg_isready` 成功，再执行数据库初始化 jar。

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
redis_image_tar_src: packages/components/redis-7.2.tar
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
logfile /logs/redis.log
requirepass {{ redis_password }}
```

如果 `redis_password` 为空，就不会写 `requirepass`。最后会删除旧容器并重新启动：

```text
容器名：datamaster-redis
镜像：redis:7.2
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

## DataMaster Server 执行流程

主服务使用单容器 `docker run` 部署。

脚本会创建远程目录：

```text
{{ app_conf_dir }}
{{ app_log_dir }}
{{ app_upload_dir }}
```

然后检查本地镜像 tar：

```yaml
datamaster_server_image_tar_src: packages/components/datamaster-server-ce-1.4.0.tar
```

如果文件存在，会上传到远程并执行 `docker load -i`。

脚本会根据 `deploy/templates/datamaster-server-application-prod.yml.j2` 生成：

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
```

最后删除旧容器并重新启动：

```text
容器名：datamaster-server
端口：{{ datamaster_server_port }}:8080
配置文件：{{ app_conf_dir }}/application-prod.yml -> /usr/app/jar/application-prod.yml
日志目录：{{ app_log_dir }} -> /usr/app/jar/logs
上传目录：{{ app_upload_dir }} -> /usr/app/jar/upload
```

容器启动时还会增加 host 映射：

```text
postgresql -> derived from {{ postgresql_ssh_host }}
redis -> derived from {{ redis_ssh_host }}
dolphinscheduler -> {{ dolphinscheduler_ip }}
dbgpt -> derived from {{ dbgpt_ssh_host }}
```

## 配置文件

- 统一配置：`deploy/deploy.yml`
- 模板目录：`deploy/templates`

`deploy/deploy.yml` 同时声明 SSH 目标、组件 IP、端口、镜像、账号密码、组件目录。远程统一部署根目录也在这里：

```yaml
base_dir: /data/datamaster
```

---

# 开发环境搭建（基于 DEPLOY.md 更新）

> 以下内容根据 DEPLOY.md 整理，已按当前项目现状修正。

## 系统要求

| 组件 | 版本 |
|------|------|
| JDK | 1.8 |
| Node.js | 18+ |
| yarn | v1.22.22+ |
| Maven | 3.6+ |
| PostgreSQL | 15（容器部署） |
| Redis | 7.2（容器部署） |
| Docker | 1.13.1+ |

**移除组件：** RabbitMQ（已从业务流程中移除）、MongoDB（质量模块错误明细已改为 JDBC 存储）、DM8/MySQL（主数据库统一为 PostgreSQL）。

## 项目模块结构

```
dataMaster/
├── datamaster-common           # 公共模块（工具类、数据源注册）
├── datamaster-system           # 系统管理
├── datamaster-assets           # 数据资产
├── datamaster-collector        # 数据汇聚/采集（含 collector-biz/sub）
├── datamaster-service          # 数据服务
├── datamaster-catalog          # 数据目录
├── datamaster-quality          # 数据质量模块
├── datamaster-etl              # ETL/Spark 任务
├── datamaster-server           # 主服务入口（端口 8080）
├── datamaster-view             # 前端（Vue 3 + Vite）
├── sql/                        # 数据库脚本
├── deploy/                     # 部署脚本和配置
├── docker/                     # Docker 辅助脚本
└── upload/                     # 运行时上传目录
```

## 服务架构

- **主服务**（datamaster-server）：端口 8080，含系统管理、汇聚、资产、目录、服务、质量等模块
- **前端**（datamaster-view）：Vite 开发服务器，端口 81，代理 `/dev-api` 到主服务 8080
- **数据库**：PostgreSQL（业务库 `datamaster`、DS 调度库 `dolphinscheduler`）
- **调度器**：DolphinScheduler（JDBC 注册中心 + API + Master + Worker + Alert）

## 本地开发环境启动

### 1. 依赖服务

使用 Docker 启动基础设施：

```bash
# PostgreSQL
docker run -d --name datamaster-postgresql -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15

# Redis
docker run -d --name datamaster-redis -p 6379:6379 redis:7.2

# DolphinScheduler（本地开发可跳过，或使用 WSL 容器集群）
```

**WSL 本地 DolphinScheduler 集群**：通过 Docker 运行在 WSL Ubuntu 中，端口已映射到 Windows（PG 5432、Redis 6379）。

### 2. 初始化数据库

执行 SQL 脚本创建业务表：

```bash
# PostgreSQL
psql -h 127.0.0.1 -U postgres -d datamaster -f sql/postgresql/datamaster.sql
```

升级脚本位于 `sql/postgresql/upgrade/`，按版本目录排列。

### 3. 后端配置（application-dev.yml）

```yaml
# 主数据源
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/datamaster?stringtype=unspecified
    username: postgres
    password: postgres

# Redis
redis:
  host: 127.0.0.1
  port: 6379

# 质量任务执行地址（默认指向主服务自身）
path:
  quality_url: http://127.0.0.1:8080/quality/qualityTaskExecutor
```

### 4. 启动后端

```bash
mvn clean package -pl datamaster-server -am -DskipTests
java -jar datamaster-server/target/datamaster-server-*.jar
```

### 5. 启动前端

```bash
cd datamaster-view
yarn install
yarn run dev
```

访问 `http://localhost:81`。

## 质量模块错误明细存储配置

质量模块将校验错误明细写入业务库 JDBC 表而非 MongoDB。配置方式：

1. 登录系统 → 质量 → 存储配置
2. 选择一个 JDBC 类型的数据源（MySQL/PostgreSQL/DM8/Oracle/Kingbase8/SQL_Server/DB2/ClickHouse/Doris/Hive/MariaDB/OSCAR）
3. 填写表名（默认 `quality_error_data`）
4. 保存后主服务自动通知 quality 刷新缓存

质量模块启动时自动检查 `quality_error_storage_config` 表，有配置则使用 JDBC 写入，无配置则跳过错误明细写入。
