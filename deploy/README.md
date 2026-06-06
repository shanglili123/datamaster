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

## 配置文件

- 统一配置：`deploy/deploy.yml`
- 模板目录：`deploy/templates`

`deploy/deploy.yml` 同时声明 SSH 目标、组件 IP、端口、镜像、账号密码、组件目录。远程统一部署根目录也在这里：

```yaml
base_dir: /data/datamaster
```
