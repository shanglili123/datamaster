# DataMaster 本地 Docker 开发环境

本地开发环境运行在 `Ubuntu` WSL 发行版内的 Docker 中。当前只启动业务需要的开发依赖：

- PostgreSQL
- Redis
- DolphinScheduler ZooKeeper/Master/Worker/Alert/API

MongoDB 和 RabbitMQ 已从业务依赖中移除，不再放入本地开发启动命令。

## 启动开发依赖

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\docker\start-dev-services.ps1
```

这个命令只会执行 `docker start`，不会删除容器、不会重建容器、不会修改已有数据卷。

## 重启开发依赖

```powershell
powershell -ExecutionPolicy Bypass -File .\docker\restart-wsl-containers.ps1
```

## 可选：启用 DolphinScheduler API localhost 入口

如果 Windows 侧不能直接访问 `http://127.0.0.1:12345/`，再显式加这个开关：

```powershell
powershell -ExecutionPolicy Bypass -File .\docker\start-dev-services.ps1 -EnsureDolphinSchedulerLocalhost
```

该开关只处理 DolphinScheduler API 的 `12345` 本地入口，不为 PostgreSQL 或 Redis 创建代理。
