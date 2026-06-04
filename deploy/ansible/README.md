# DataMaster 自动化部署

这套部署脚本面向 Linux 服务器，按组件 IP 分发部署，便于把数据库、中间件、调度器和应用拆到不同服务器。RabbitMQ 已按当前规划移除，未纳入部署。

## 服务器前提

所有目标服务器建议使用同一类 Linux 发行版，例如 CentOS 7/8、Rocky Linux、Ubuntu Server 或 openEuler。脚本默认使用 Linux 绝对路径，例如 `/opt/datamaster`、`/opt/soft`、`/dolphinscheduler`。

容器部署服务器需要提前安装：

- Docker
- Docker Compose 插件，即支持 `docker compose` 命令

物理机部署 ZooKeeper 和 DolphinScheduler 的服务器需要提前安装：

- JDK 8
- `tar`
- `systemd`

Ansible 控制机可以是任意能 SSH 到这些 Linux 服务器的机器。

## 部署形态

| 组件 | 部署方式 | 说明 |
| --- | --- | --- |
| PostgreSQL | Docker Compose | 挂载 `data`、`init`、`logs`，首次启动自动执行 `sql/postgresql/initialization/V1.5.0_PG.sql` |
| Redis | Docker Compose | 挂载 `data`、`conf`、`logs`，开启 AOF |
| MongoDB | Docker Compose | 挂载 `data`、`conf`、`logs`、`init`，初始化质量服务用户和 `quality_error_data` 集合 |
| ZooKeeper | 物理机 | 预留独立安装脚本和 systemd 服务，方便后续改配置 |
| DolphinScheduler | 物理机 | 预留独立安装脚本、systemd 服务、`/dolphinscheduler` 资源目录和 `/opt/soft` 插件目录 |
| DataMaster Server | Docker Compose | 挂载 `application-prod.yml`、日志和上传目录 |
| DataMaster Quality | Docker Compose | 挂载 `application-prod.yml`、日志和任务日志目录 |

## 使用步骤

1. 在控制机安装依赖：

   ```bash
   python3 -m pip install ansible
   ```

2. 准备服务器清单：

   ```bash
   cd deploy/ansible
   cp inventory.example.ini inventory.ini
   ```

   修改 `inventory.ini` 中每个组件的 `ansible_host`、`ansible_user`、`ansible_password`。生产环境建议改用 SSH Key 或 Ansible Vault。

3. 修改公共变量：

   编辑 `group_vars/all.yml`，至少替换：

   - `postgresql_ip`
   - `redis_ip`
   - `mongodb_ip`
   - `zookeeper_ip`
   - `dolphinscheduler_ip`
   - `datamaster_app_ip`
   - `datamaster_quality_ip`
   - `deploy_root`
   - PostgreSQL、MongoDB、Redis、DolphinScheduler token 等密码或端口

4. 准备物理机安装包：

   如果服务器不能联网下载包，把以下文件放到 `deploy/ansible/packages/`：

   - `apache-zookeeper-3.8.4-bin.tar.gz`
   - `apache-dolphinscheduler-3.2.2-bin.tar.gz`

   Chunjun、Flink 插件建议放在 DolphinScheduler 机器的 `/opt/soft` 下，保持和本地运行约定一致：

   - `CHUNJUN_HOME=/opt/soft/chunjun`
   - `FLINK_HOME=/opt/soft/flink`

5. 执行部署：

   ```bash
   ansible-playbook -i inventory.ini site.yml
   ```

   也可以只部署某类组件：

   ```bash
   ansible-playbook -i inventory.ini site.yml --limit postgresql_servers
   ansible-playbook -i inventory.ini site.yml --limit redis_servers,mongodb_servers
   ansible-playbook -i inventory.ini site.yml --limit physical_scheduler_hosts
   ```

## 目录约定

默认部署根目录是 `/opt/datamaster`：

```text
/opt/datamaster
├── postgresql
│   ├── data
│   ├── init
│   └── logs
├── redis
│   ├── data
│   ├── conf
│   └── logs
├── mongodb
│   ├── data
│   ├── conf
│   ├── init
│   └── logs
├── zookeeper
├── dolphinscheduler
├── app
└── quality
```

DolphinScheduler 资源目录默认是 `/dolphinscheduler`，插件和执行依赖默认放在 `/opt/soft`，这样后续添加 Flink、Chunjun、JDBC 插件不需要重新构建镜像。

## 注意事项

- PostgreSQL 初始化 SQL 只会在容器首次创建空数据目录时执行；已有数据目录不会重复执行。
- MongoDB 初始化脚本同样只在首次初始化数据库时执行。
- 新增脚本不部署 RabbitMQ。
- 目标服务器需要已有 Docker 和 Docker Compose 插件；物理机调度器服务器需要 JDK 8。
- DolphinScheduler 的真实生产配置项可能还需要根据安装包版本补齐，脚本会生成服务文件并保留环境文件 `datamaster-ds-env.sh` 供上线前确认。
