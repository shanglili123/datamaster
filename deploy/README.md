# DataMaster 一键部署

主入口是 Bash 脚本：

```bash
./deploy/start-all.sh
```

Windows 可以用包装入口：

```powershell
.\deploy\start-all.ps1
```

部署过程只使用 Bash、`ssh`、`scp` 和单容器 `docker run`，不使用 Ansible，也不使用 Docker Compose。

## 部署前确认

先改 `deploy/deploy.yml` 里带 `*` 注释的配置，重点确认：

- SSH 目标：每个组件部署到哪台机器。
- 组件 IP：应用、DS、Redis、PG 之间实际可访问的地址。
- `base_dir`：远程统一部署根目录，默认 `/data/datamaster`。
- PG/Redis 端口和密码。
- DolphinScheduler token、API 端口、worker group。
- 离线包文件名和目录是否与 `deploy/deploy.yml` 一致。
- Chunjun/Flink 是否已经放到 `deploy/packages/soft/chunjun` 和 `deploy/packages/soft/flink`。

## 离线包目录

把文件放到：

```text
deploy/packages
├── components
│   ├── postgres-15.tar
│   ├── redis-7.2.tar
│   ├── datamaster-server-ce-1.4.0.tar
│   ├── datamaster-quality-ce-1.4.0.tar
│   ├── apache-zookeeper-3.8.4-bin.tar.gz
│   ├── apache-dolphinscheduler-3.2.2-bin.tar.gz
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
datamaster_server_image_tar: datamaster-server-ce-1.4.0.tar
datamaster_quality_image_tar: datamaster-quality-ce-1.4.0.tar
zookeeper_install_tgz: apache-zookeeper-3.8.4-bin.tar.gz
dolphinscheduler_install_tgz: apache-dolphinscheduler-3.2.2-bin.tar.gz
database_init_jar_src: packages/components/datamaster-db-init.jar
postgresql_init_sql_src: packages/sql/datamaster.sql
dolphinscheduler_init_sql_src: packages/sql/dolphinscheduler.sql
soft_package_src: packages/soft
```

脚本处理方式：

- `components/*.tar`：存在则上传到远程 `{{ base_dir }}/packages` 并执行 `docker load -i`；不存在则跳过，继续使用服务器已有镜像。
- `components/*.tar.gz`：上传到远程 `{{ base_dir }}/packages`，ZooKeeper 和 DolphinScheduler 安装脚本会自动解压。
- `components/datamaster-db-init.jar`：上传到远程后执行，用于创建数据库、用户并导入 SQL。
- `sql/datamaster.sql` 和 `sql/dolphinscheduler.sql`：上传到远程 `{{ base_dir }}/init-sql` 后由初始化 jar 导入。
- `soft/chunjun/` 和 `soft/flink/`：目录里有实际文件时上传到远程 `{{ base_dir }}/soft`。

## 执行过程

`deploy/start-all.sh` 会按下面顺序执行：

1. 读取 `deploy/deploy.yml`，把模板里的 `{{ variable }}` 替换成实际值。
2. 通过 `ssh`/`scp` 连接每台目标机器，创建 `base_dir` 下的组件目录。
3. 部署 PostgreSQL：上传镜像 tar 并 `docker load`，再用 `docker run` 启动 PG。
4. 初始化数据库：上传 `datamaster-db-init.jar`、`datamaster.sql`、`dolphinscheduler.sql`，然后执行 jar 创建库、用户并导入 SQL。
5. 部署 Redis：上传镜像 tar、生成 `redis.conf`，再用 `docker run` 启动 Redis。
6. 部署 ZooKeeper：上传 tar 包，解压并生成 systemd 服务。
7. 部署 DolphinScheduler：上传 tar 包、Chunjun、Flink，解压后自动修改 DS 数据源和运行环境，再生成 systemd 服务。
8. 部署 DataMaster server 和 quality：生成 `application-prod.yml`，上传镜像 tar 并启动容器。

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

## ZooKeeper 执行流程

ZooKeeper 使用 tar 包解压加 systemd 部署。

脚本会先创建远程目录：

```text
{{ zookeeper_dir }}
{{ zookeeper_data_dir }}
{{ zookeeper_log_dir }}
{{ remote_package_dir }}
```

然后上传：

```text
deploy/packages/components/apache-zookeeper-3.8.4-bin.tar.gz
```

到：

```text
{{ remote_package_dir }}/apache-zookeeper-3.8.4-bin.tar.gz
```

接着渲染并执行：

```text
deploy/templates/install-zookeeper.sh.j2
```

远程安装脚本会解压到：

```text
{{ zookeeper_dir }}/apache-zookeeper-3.8.4-bin
```

并生成：

```text
{{ zookeeper_dir }}/apache-zookeeper-3.8.4-bin/conf/zoo.cfg
```

主要配置：

```text
dataDir={{ zookeeper_data_dir }}
dataLogDir={{ zookeeper_log_dir }}
clientPort={{ zookeeper_client_port }}
admin.enableServer=false
```

最后创建并启动 systemd 服务：

```text
datamaster-zookeeper.service
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

数据源来自 `deploy/deploy.yml`：

```yaml
postgresql_ip: "<POSTGRESQL_IP>"
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

运行环境也来自 `deploy/deploy.yml`：

```yaml
zookeeper_ip: "<ZOOKEEPER_IP>"
zookeeper_client_port: 2181
dolphinscheduler_resource_dir: "{{ base_dir }}/dolphinscheduler-resource"
chunjun_home: "{{ remote_soft_dir }}/chunjun"
flink_home: "{{ remote_soft_dir }}/flink"
```

会写入：

```text
REGISTRY_ZOOKEEPER_CONNECT_STRING=<ZOOKEEPER_IP>:2181
RESOURCE_STORAGE_TYPE=LOCAL
RESOURCE_LOCAL_BASE_PATH=/data/datamaster/dolphinscheduler-resource
CHUNJUN_HOME=/data/datamaster/soft/chunjun
FLINK_HOME=/data/datamaster/soft/flink
```

最后会创建并启动这些 systemd 服务：

```text
datamaster-dolphinscheduler-api.service
datamaster-dolphinscheduler-master.service
datamaster-dolphinscheduler-worker.service
datamaster-dolphinscheduler-alert.service
```

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
DolphinScheduler API 地址和 token
DataMaster quality 回调地址
DS resource 路径
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
postgresql -> {{ postgresql_ip }}
redis -> {{ redis_ip }}
dolphinscheduler -> {{ dolphinscheduler_ip }}
```

## DataMaster Quality 执行流程

质量服务使用单容器 `docker run` 部署。

脚本会创建远程目录：

```text
{{ quality_conf_dir }}
{{ quality_log_dir }}
{{ quality_job_log_dir }}
```

然后检查本地镜像 tar：

```yaml
datamaster_quality_image_tar_src: packages/components/datamaster-quality-ce-1.4.0.tar
```

如果文件存在，会上传到远程并执行 `docker load -i`。

脚本会根据 `deploy/templates/datamaster-quality-application-prod.yml.j2` 生成：

```text
{{ quality_conf_dir }}/application-prod.yml
```

质量服务配置会写入：

```text
PostgreSQL 地址、端口、库名、用户名、密码
Redis 地址、端口、密码
```

最后删除旧容器并重新启动：

```text
容器名：datamaster-quality
端口：{{ datamaster_quality_port }}:8083
配置文件：{{ quality_conf_dir }}/application-prod.yml -> /usr/app/jar/application-prod.yml
日志目录：{{ quality_log_dir }} -> /usr/app/jar/logs
任务日志目录：{{ quality_job_log_dir }} -> /usr/app/jar/job-log
```

容器启动时还会增加 host 映射：

```text
postgresql -> {{ postgresql_ip }}
redis -> {{ redis_ip }}
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
├── datamaster-quality          # 数据质量（独立服务，端口 8083）
├── datamaster-etl              # ETL/Spark 任务
├── datamaster-server           # 主服务入口（端口 8080）
├── datamaster-view             # 前端（Vue 3 + Vite）
├── sql/                        # 数据库脚本
├── deploy/                     # 部署脚本和配置
├── docker/                     # Docker 辅助脚本
└── upload/                     # 运行时上传目录
```

## 服务架构

- **主服务**（datamaster-server）：端口 8080，含系统管理、汇聚、资产、目录、服务等模块
- **质量服务**（datamaster-quality）：独立进程，端口 8083，通过 HTTP 与主服务通信
- **前端**（datamaster-view）：Vite 开发服务器，端口 81，代理 `/dev-api` 到主服务 8080
- **数据库**：PostgreSQL（业务库 `datamaster`、DS 调度库 `dolphinscheduler`）
- **调度器**：DolphinScheduler（容器部署，ZooKeeper + API + Master + Worker + Alert）

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

# 质量服务地址（主服务通过此地址调用 quality）
path:
  quality_url: http://127.0.0.1:8083/quality
```

### 4. 启动后端

```bash
mvn clean package -pl datamaster-server -am -DskipTests
java -jar datamaster-server/target/datamaster-server-*.jar
```

质量服务单独启动：

```bash
mvn clean package -pl datamaster-quality -am -DskipTests
java -jar datamaster-quality/target/datamaster-quality-*.jar
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

quality 启动时自动检查 `quality_error_storage_config` 表，有配置则使用 JDBC 写入，无配置则跳过错误明细写入。
