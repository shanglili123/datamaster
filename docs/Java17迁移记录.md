# Java 17 迁移记录

## 当前状态（2026-11-02，dev_1029_2.0）

本轮继续上一阶段的 Spring Boot 2.7.18 升级，将平台 Java 编译和主服务运行基线从 8 提升至 17。**配置迁移及用户授权的 Java 17 主源码编译已通过，测试、打包和运行尚未验证**。此前 Java 17 配置曾撤回，旧文档中的“已修改”不能作为当前分支的构建证据。

### 本轮修改

- 根 POM：`java.version=17`，统一 `maven-compiler-plugin 3.13.0` 和 `release=17`；Enforcer 3.5.0 限制 Maven 使用 JDK 17+，Surefire 3.2.5 用于测试运行；增加 test scope 的 Vintage engine，避免启用 JUnit Platform 后漏跑既有 JUnit 4 用例。
- server 删除固定 `source/target=1.8` 和旧 compiler 插件版本覆盖，继承根编译配置；保留现有 MapStruct/Lombok 注解处理器及上一阶段的 JAR 强制重建配置。
- 独立 `deploy/db-init` 不继承主 POM，单独设置 `release=17`、compiler 3.13.0、shade 3.6.0；不因本次 JDK 迁移改工具发布版本。
- server Docker 基础镜像改为 `eclipse-temurin:17-jre`，不假设原私有仓库已有 17 标签。
- 部署新增 `datamaster_java_bin`（后端主机）和 `database_init_java_bin`（PostgreSQL 主机），均为已安装的 Java 17+ 可执行文件绝对路径。预检和实际部署检查版本；systemd 和初始化命令显式使用所选 Java，不再依赖系统默认 `java`。旧版自定义部署配置必须补齐这两个键。
- CA 根证书生成和签发移除 `sun.security.x509`，改用 Bouncy Castle 的公开 X.509 构建/签名 API；将 provider/PKIX 依赖统一为 jdk18on 1.83。返回文件名称、PEM/PKCS#8 格式、RSA/SHA256、有效期及 SAN 规则保持，根证书改为内存编码，避免共享临时文件互相覆盖/删除。未顺带改变既有根证书扩展或信任策略。增加未执行的证书回归测试，覆盖自签名、签名验真、私钥配对、DNS/IPv4/通配符 SAN 和非域名无 SAN。
- 更新 AGENTS、README 和升级方案的当前技术基线。历史业务实施记录不批量改写。

### 明确保持不变

- 后端版本 2.0.0、Spring Boot 2.7.18、Spring Framework 5.3.33、Spring Security 5.7.12、LangChain4j 0.35.0。
- 保留 `javax` 命名空间，不迁移 Boot 3/Jakarta，不批量改写业务语法。
- 不切换分支，不提交，不修改本机/生产的全局 `JAVA_HOME` 或 `PATH`，不安装 JDK、不拉取镜像、不启动或重启服务。
- 不修改 DolphinScheduler、CHUNJUN、Flink 的容器、挂载、二进制或启动 wrapper，保留已有 Flink 1.16.2 本地兼容补丁。
- 不修改数据库、应用密钥、白名单或任务 Token 校验。

## 发布前必须验证

1. IDE Project SDK、Maven Runner 和运行配置实际使用 JDK 17，Maven 至少 3.6.3（compiler 3.13.0、Enforcer 3.5.0 的要求）。Enforcer 允许 17+ 构建，release 固定 17；首次验收建议使用 17。本机默认 Maven 3.6.0 不满足要求，可显式选择 IDEA 自带 Maven 3.8.1，不改系统默认配置。
2. 在 JDK 17 上重新构建，不能沿用 Java 8 阶段的 `target` 或 `.flattened-pom.xml`。上一阶段 JDK 8 打包及隔离启动通过，不代表本轮 Java 17 验证通过。
3. 执行测试并确认原有用例未被跳过。此前 metadata 模块 39 个测试中有 19 个失败，是否为升级前既有问题仍待确认。
4. 验证认证/JJWT/JAXB、Swagger、数据库驱动、多数据源、Redis/嵌入式 Redis、代理反射、POI 导出、Hadoop 3.0.0 及本地 system JAR。
5. 若遇 `InaccessibleObjectException`，按具体依赖升级/修复，不全局无差别加入 `--add-opens`。
6. 本项目继承主 POM 的产物将输出 Java 17 字节码，Java 8 Worker 不能加载。若 DS/Flink/CHUNJUN 在同一 JVM 加载本项目 JAR，需另行迁移实际执行 JVM 或设计独立兼容产物；平台通过 HTTP 调用第三方服务则不要求双方 JDK 相同。未完成执行链路验证前不得直接替换旧 Worker 的任务 JAR。
7. server 镜像依赖可访问的 `eclipse-temurin:17-jre`；离线部署需先按流程同步镜像，不能假定私库已有对应标签。
8. 正常可写配置、后台 worker、ETL、工作流、血缘、AI 流式问数及前端联调仍需单独回归。

## 2026-11-02 用户授权的编译验证

- 默认 Maven 3.6.0 + JDK 17 的首次尝试在插件版本检查阶段失败：compiler 3.13.0、Enforcer 3.5.0 要求 Maven 3.6.3+，未进入源码编译。
- 随后仅在命令内使用本机 JDK 17.0.19 和 IDEA 自带 Maven 3.8.1，没有修改全局环境变量。
- 独立 `deploy/db-init` 执行 `compile -DskipTests`：**BUILD SUCCESS**，日志确认 `javac [debug release 17]`，2026-11-02 15:24:56 完成，耗时 2 分 21 秒。
- 主服务及依赖模块执行 `mvn -pl datamaster-server -am compile -DskipTests -Ddocker.build.skip=true -Ddocker.push.skip=true -Ddocker.print.skip=true`：**BUILD SUCCESS**，36 个反应堆项目全部 SUCCESS，日志确认业务源码使用 `javac [debug release 17]`，2026-11-02 15:29:30 完成，耗时 7 分 05 秒。包括 CA 工具替换后的 common-common 以及 server 均编译通过。
- 本次仅授权并执行编译，未运行测试、打包、镜像构建、数据库迁移或启动服务。

## 2026-11-02 用户授权的打包与隔离启动验证

- 首次 `package -DskipTests` 已于 15:40:05 **BUILD SUCCESS**，耗时 4 分 47 秒；测试源码已编译，但单元测试明确跳过。
- 启动前发现 `CollectorComponentRecoveryService` 的应用就绪监听器不受已有 worker 开关控制，会恢复历史事件并可能触发模型/调度。增加 `ds.component-recovery.startup-enabled`（默认 true），仅本次验证设置 false，正常部署不改变原行为。增加对应未执行的回归用例。
- 包含启动隔离开关的重新打包已于 15:43:45 **BUILD SUCCESS**，耗时 2 分 53 秒；随后使用 127.0.0.1:18989、PostgreSQL 连接级只读、关闭动作/工作流/血缘/入库/run guard/启动恢复，使用独立嵌入式 Redis 12138 进行两次启动尝试，均被数据库连接阻断。两次退出后均确认 18989/12138 释放，未停止既有服务。
- 第一次隔离启动未成功：嵌入式 Redis 12138 在 Java 17 下启动成功，但连接开发 PostgreSQL `192.168.93.174:5432` 超时，导致 DataSource 初始化失败，未到应用就绪阶段。随后只读 TCP 检查发现原 PostgreSQL 端口恢复可达，以相同地址、只读限制和隔离开关重试，15:48:04 再次失败：PostgreSQL JDBC 在认证阶段收到 EOF（`PGStream.receiveChar` / `ConnectionFactoryImpl.doAuthentication`）。端口可达不等于数据库协议/认证可用；未自动启动数据库、切换连接目标或放宽只读约束。
- **实际启动未通过，HTTP 冒烟未执行**；目前日志未出现 Java 17 模块访问/反射异常，但不能因此认定运行兼容已验收。需先恢复配置数据库的可用连接，再继续启动和接口验证。

## 建议的后续本地验证命令

先确认当前 shell 的 Maven 使用 JDK 17，再执行：

```bash
mvn -version
mvn -pl datamaster-server -am clean package -DskipTests
mvn -pl datamaster-server -am test
mvn -f deploy/db-init/pom.xml clean package
```

按项目约定，本轮不主动运行编译、构建、测试或 lint，也不执行数据库迁移。后续 LangChain4j 或 Boot 3 升级分别开展，不与本轮混为一次验收。
