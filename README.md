# dataMaster 项目代码理解报告（最终版）
> 说明：本文档基于仓库 `D:/dev/DATAMASTER` 的 README、DEPLOY.md、根 `pom.xml`、后端启动类与配置文件整理，并结合本地初始化编译结果形成。目标是快速建立项目结构、启动链路与后续排查思路。

> **命名说明**：本文档采用项目重构后的命名方案。原模块名与现模块名对照见附录。

## 1. 项目定位

dataMaster 是一个面向企业数据中台、数据治理、数据服务与智能问数场景的前后端分离系统。
核心能力包括：
- 数据集成（ETL）
- 数据开发
- 数据建模
- 元数据管理
- 数据质量
- 数据资产
- 数据服务
- AI 智能问数（Text2SQL / 图表分析）

从 README 来看，它更偏向"企业级数据中台平台"，不是一个单纯的 CRUD 后台。

## 2. 技术栈概览

### 后端
- Java 8
- Spring Boot 2.5.15
- Spring Security
- MyBatis-Plus
- Druid
- Redis
- RabbitMQ
- PageHelper
- Swagger / Knife4j
- JJWT
- Apache POI / Velocity 等工具库

### 前端
- Vue 3
- Vite
- Element UI

### 其他依赖
- Spark
- DolphinScheduler
- Hive
- HBase
- MySQL / DM8 / Oracle / Kingbase8 / Doris 等数据库支持

## 3. 项目结构理解

根目录是一个多模块 Maven 工程，模块拆分较细，整体采用 **"平台入口 + 领域模块 + 基础框架"** 的分层架构。以下逐一说明每个模块的职责及模块间的关系。

### 3.1 模块总览与依赖关系
```
datamaster-server  (启动入口，依赖所有业务模块)
├── datamaster-common/*  (基础框架父模块)
│   ├── datamaster-common-common      ← 注解、枚举、数据源方言、工具类
│   ├── datamaster-common-datasource  ← 动态数据源路由、连接管理
│   ├── datamaster-common-config      ← 配置中心
│   ├── datamaster-common-mybatis     ← MyBatis-Plus 配置扩展
│   └── datamaster-common-websocket   ← WebSocket 消息推送
├── datamaster-system           ← 系统管理（用户、角色、菜单、字典）
├── datamaster-taxonomy         ← 分类管理（资产分类、主题、源系统、清洗规则等）
├── datamaster-standards        ← 数据标准（标准目录、标准文档）
├── datamaster-assets           ← 【核心】数据资产（数据源注册、资产目录、资产申请）
├── datamaster-collector        ← 数据采集（采集任务、汇聚实例）
├── datamaster-service          ← 数据服务（API 发布、SQL 解析执行、限流缓存）
├── datamaster-modeling         ← 数据建模（模型设计）
├── datamaster-catalog          ← 元数据管理（元数据抓取与同步）
├── datamaster-api-ds           ← DolphinScheduler 适配层（HTTP 封装）
├── datamaster-etl              ← Spark ETL 程序（独立 Jar）
├── datamaster-quality          ← 数据质量（质量规则、检测任务，独立微服务）
└── datamaster-view             ← 前端页面（Vue 3 + Vite）
```

### 3.2 框架层详解
#### `datamaster-common` 公共基础框架
含多个子模块，是项目的技术基础设施。
| 子模块 | 职责 | 关键内容 |
|--------|------|----------|
| `datamaster-common-common` | 公共能力 | 数据源方言体系（3 种数据库类型 + 11 种方言实现）、`DataSourceFactory`、`DbDialect`、注解 `@DataSource`、工具类 |
| `datamaster-common-datasource` | 数据库访问层 | 动态数据源路由 `DynamicDataSource`（基于 `AbstractRoutingDataSource`）、ThreadLocal 上下文 `DynamicDataSourceContextHolder`、Spring Security 配置、JWT 令牌、Redis 缓存封装 |
| `datamaster-common-config` | 配置管理 | 应用内轻量定时任务（缓存刷新、状态同步等），区别于 DolphinScheduler 的重调度；配置读取与管理 |
| `datamaster-common-mybatis` | MyBatis 扩展 | MyBatis-Plus 配置扩展、数据源切换 AOP |
| `datamaster-common-websocket` | 即时通信 | WebSocket 消息推送 |

**依赖关系**：`datamaster-common-common` 被所有业务模块直接或间接依赖；`datamaster-common-datasource` 依赖 `datamaster-common-common` 的数据源和方言体系。

### 3.3 业务模块详解

#### `datamaster-system` 系统管理
- **功能**：用户管理、角色管理、菜单管理、部门管理、字典管理、操作日志
- **数据表**：`SYS_USER`、`SYS_ROLE`、`SYS_MENU`、`SYS_DICT_DATA` 等
- **依赖**：`datamaster-common` 全部子模块
- **被依赖**：所有业务模块（通过框架安全组件校验权限）

#### `datamaster-taxonomy` 分类管理（资产元数据管理）
- **功能**：资产分类、主题域、源系统管理、数据集分类、API 分类、模型分类、质量分类、清洗分类、文档分类、任务分类、项目/客户管理、清洗规则、审计规则
- **说明**：这是元数据的元数据，为 `datamaster-assets` 提供分类体系和规则定义
- **数据表**：`TAX_ASSET_CAT`、`TAX_THEME`、`TAX_SOURCE_SYSTEM`、`TAX_CLEAN_RULE` 等
- **依赖**：`datamaster-common`
- **被依赖**：`datamaster-assets`（资产注册时引用分类）、`datamaster-quality`（引用的清洗/审计规则）

#### `datamaster-standards` 数据标准
- **功能**：数据标准目录管理、标准文档管理
- **数据表**：以 `STD_` 前缀
- **依赖**：`datamaster-common`
- **被依赖**：`datamaster-assets`（资产挂载标准）

#### `datamaster-assets` 数据资产（核心模块）
- **功能**：
  - **数据源注册与管理**：支持 23 种外部数据源的动态注册（关系型/分析型/NoSQL/消息队列/文件存储），连接信息加密存储，提供连接测试、表查询、数据预览能力
  - **数据资产目录**：资产登记、分类挂载、资产字段管理
  - **资产申请**：资产审批与使用流程
- **数据表**：`AST_DATASOURCE`（数据源配置）、`AST_ASSET`（资产）、`AST_ASSET_COLUMN`（资产字段）、`AST_ASSET_APPLY`（资产申请）等
- **关键机制**：
  - 启动时将所有已注册数据源加载到 Redis（hash `"datasource"`），供工作节点获取
  - 创建/更新数据源时同步刷新 Redis 缓存
  - 通过 `AbstractDataSourceFactory` 动态创建 JDBC 连接，使用 Hutool 的 `SimpleDataSource` 或 HikariCP
  - 使用 MD5 对连接信息做缓存去重（`CacheDataSourceFactoryBean`）
- **依赖**：`datamaster-common`、`datamaster-taxonomy`（引用分类）
- **被依赖**：`datamaster-service`（数据服务执行 SQL）、`datamaster-collector`（数据采集读取源）、`datamaster-etl`（ETL 读写）、`datamaster-quality`（质量检测）、`datamaster-catalog`（元数据采集）

#### `datamaster-collector` 数据采集
- **功能**：采集任务管理、汇聚实例管理、增量/全量数据同步
- **数据表**：以 `COL_` 前缀
- **依赖**：`datamaster-common`、`datamaster-assets`（读取已注册的数据源）、`datamaster-api-ds`（调用调度平台执行采集任务）
- **被依赖**：无

#### `datamaster-service` 数据服务
- **功能**：API 定义与发布、SQL 解析与执行、请求参数映射、响应处理、缓存/限流/IP 白名单
- **说明**：尽管模块名原名 `ds`，但不是数据源（datasource）管理，而是 **数据服务**（Data Service）API 网关
- **数据表**：`SVC_API`（API 定义）
- **关键机制**：接收到 API 请求后，解析 SQL，通过 `AssetsDataSourceServiceImpl.getDbQuery(id)` 获取目标数据源的 `DbQuery` 对象执行查询
- **依赖**：`datamaster-common`、`datamaster-assets`（执行 SQL 时获取数据源连接）

#### `datamaster-modeling` 数据建模
- **功能**：模型设计、模型管理
- **数据表**：以 `MDL_` 前缀
- **依赖**：`datamaster-common`、`datamaster-assets`（建模时引用数据源）

#### `datamaster-catalog` 元数据管理
- **功能**：元数据抓取、表和字段结构同步、元数据版本管理
- **数据表**：以 `CAT_` 前缀
- **依赖**：`datamaster-common`、`datamaster-assets`（读取数据源信息进行采集）

### 3.4 支撑模块详解

#### `datamaster-api-ds` 调度平台适配层
- **功能**：对 DolphinScheduler 的 HTTP API 封装，提供项目管理、调度配置、执行触发、状态切换等能力
- **关键类**：`SchedulerProjectServiceImpl`、`SchedulerEtlServiceImpl`、`SchedulerEtlExecutorServiceImpl`
- **通信方式**：通过 `SchedulerRequestUtils` + `SchedulerApiType` 枚举统一发起 HTTP 调用
- **能力覆盖**：`CREATE_PROJECT`、`CREATE_SCHEDULE`、`SCHEDULE_ONLINE/OFFLINE`、`POST_EXECUTORS_EXECUTE` 等
- **依赖**：`datamaster-common`
- **被依赖**：`datamaster-collector`（创建采集调度）、`datamaster-etl`（触发 ETL 任务）

#### `datamaster-etl` Spark ETL 程序
- **功能**：基于 Spark 的 ETL 数据处理程序（独立 Jar，不内嵌 Web 容器）
- **入口**：`com.data.matser.spark.etl.EtlApplication`
- **关键类**：`DBWriter`（通过 `AbstractDataSourceFactory` 写入目标库）、`DBUtils`（数据源属性提取）
- **部署方式**：由 DolphinScheduler 调度触发 Spark 任务执行
- **依赖**：`datamaster-common`（复用数据源方言体系）

#### `datamaster-quality` 数据质量（独立微服务）
- **功能**：数据质量规则执行、质量检测任务、审计规则、清洗规则
- **特点**：独立服务（运行在 8083 端口），拥有自己的数据源注册表（独立 `AssetsDatasourceDO` 和 `AssetsDatasourceMapper`），与主服务 `datamaster-assets` 的数据源注册相互隔离
- **关键类**：`AssetsDatasourceQualityServiceImpl`、`QualityTaskExecutorServiceImpl`、`RuleExecutorTask`
- **依赖**：`datamaster-common`、`datamaster-assets`（通过 HTTP/RPC 引用主服务数据）
- **被依赖**：由 `datamaster-server` 配置中的 `quality_url` 通过 HTTP 触发执行

### 3.5 模块间核心调用链路
```
用户操作 → datamaster-assets（注册数据源）        → 写入 AST_DATASOURCE → 同步 Redis
              │
              ├── datamaster-catalog       → 读取数据源 → 采集元数据
              ├── datamaster-collector      → 读取数据源 + datamaster-api-ds → 创建采集调度任务
              ├── datamaster-service        → 读取数据源 → 执行 API 定义的 SQL 查询
               ├── datamaster-quality        → 读取数据源 → 执行质量检测
               └── datamaster-etl            → 读取数据源 → Spark ETL 读写
                     └── → datamaster-api-ds 调用 DolphinScheduler 触发
```

### 3.6 架构特点总结

- **模块化单体**：所有业务模块最终在 `datamaster-server` 同一进程中运行，非微服务架构
- **数据源中心化**：`datamaster-assets` 为所有模块提供统一的数据源注册与连接管理
- **调度双体系**：`datamaster-api-ds`（对接 DolphinScheduler 做流程编排）+ 应用内轻量定时任务
- **数据源加密**：注册的外部数据源密码使用 AES 加密存储，连接时解密
- **Redis 同步**：数据源信息启动时加载到 Redis，各模块通过 Redis 获取最新数据源配置

### 一个值得注意的点
AI 智能模块 `datamaster-intelligence` 已被移除。

## 4. 启动入口

后端主启动类位于：
- `datamaster-server/src/main/java/com/data/matser/BootstrapApplication.java`

这个主类的特征很关键：
- `@ComponentScan(basePackages = {"com.data.matser"})`
  - 扫描整个业务包
- `@ServletComponentScan(basePackages = {"com.data.matser"})`
  - 启用 Servlet 组件扫描
- `@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })`
  - 排除了默认数据源自动配置
  - 说明项目走的是自定义 / 动态数据源链路
- `@EnableFileStorage`
  - 启用了文件存储能力
- `@EnableAspectJAutoProxy(proxyTargetClass = true)`
  - 启用了 AOP 代理

### 启动类带来的直接结论
- 这不是一个只扫本模块的轻量 Boot 应用
- 启动时会 `com.data.matser` 下的大量业务包一起拉起
- 数据源配置不能简单按默认 Spring Boot 方式理解，需要结合项目自己的 datasource 配置链路来看

## 5. 配置链路理解

### 5.1 配置文件层次

| 配置文件 | 作用 | 位置 |
|----------|------|------|
| `application.yml` | 公共配置（端口、日志、MyBatis、Swagger、token 等） | `datamaster-server/src/main/resources/` |
| `application-dev.yml` | 开发环境配置（数据源、Redis、RabbitMQ、调度器） | 同上 |
| `application-prod.yml` | 生产环境配置 | 同上 |
| `application-{module}-dev.yml` | 各业务模块扩展配置（多为空占位文件） | `datamaster-*/src/main/resources/` |

配置导入链路：
```yaml
spring:
  profiles:
    active: dev
  config:
    import:
      - "application-${spring.profiles.active}.yml"           # → application-dev.yml
      - "classpath:application-auth-${spring.profiles.active}.yml"
      - "classpath:application-file-${spring.profiles.active}.yml"
      - "classpath:application-system-${spring.profiles.active}.yml"
      - "classpath:application-mc-${spring.profiles.active}.yml"
```

### 5.2 三层数据源架构
项目中的数据源配置分为三个层次，职责各不相同。

#### 第一层：平台自身数据库（主库）
**用途**：存储平台自己的业务数据（用户、资产、配置等）
**配置位置**：`application-dev.yml` / `application-prod.yml`
**技术选型**：Baomidou Dynamic Datasource + Druid 连接池
通过顶层选择器动态切换：
```yaml
datasource:
  type: PostgreSQL  # 可选: PostgreSQL / MySQL / dm8 / kingbase8 / Oracle

spring:
  datasource:
    dynamic:
      druid:
        initial-size: 5
        maxActive: 20
      datasource:
        master:
          driver-class-name: ${${datasource.type}.driver-class-name}
          url: ${${datasource.type}.url}
          username: ${${datasource.type}.username}
          password: ${${datasource.type}.password}
```

预置的数据库连接模板：
| 数据库类型 | 配置名 | 驱动 |
|-----------|--------|------|
| **PostgreSQL**（默认） | `PostgreSQL` | `org.postgresql.Driver` |
| **MySQL** | `MySQL` | `com.mysql.cj.jdbc.Driver` |
| **达梦 DM8** | `dm8` | `dm.jdbc.driver.DmDriver` |
| **人大金仓 Kingbase8** | `kingbase8` | `com.kingbase8.Driver` |
| **Oracle 12c** | `Oracle` / `oracle` | `oracle.jdbc.OracleDriver`（prod 配置中正确，dev 配置中误用了 PG 驱动）|

#### 第二层：用户注册的外部数据源（核心数据源管理）
**用途**：平台用户通过 UI 动态注册的外部数据源/中间件，供数据查询、元数据采集、质量检测、ETL、数据服务等使用
**管理模块**：`datamaster-assets`（数据资产模块）
**数据表**：`AST_DATASOURCE`
**支持类型**：共 23 种（定义在 `DbType.java`）
| 类别 | 数据库 |
|------|--------|
| **关系型** | MySQL、MariaDB、Oracle 11g、Oracle 12c、PostgreSQL、SQL Server 2008、SQL Server 2012+、DM8、Kingbase8、DB2、神通（OSCAR） |
| **分析型** | ClickHouse、Doris、Hive、Phoenix |
| **NoSQL** | MongoDB、Redis |
| **消息队列** | Kafka、RabbitMQ |
| **文件/存储** | HDFS、FTP、阿里云 OSS |
| **其他** | OTHER（占位，不支持具体操作） |

**关键机制**：
- 连接信息中的密码使用 **AES 加密** 存储
- 启动时将所有有效数据源加载到 **Redis hash `"datasource"`**，供工作节点获取
- 通过 `AbstractDataSourceFactory` 动态创建 JDBC 连接（使用 Hutool SimpleDataSource 或 HikariCP）
- 使用 **MD5 缓存去重**（`CacheDataSourceFactoryBean`），相同连接信息复用 DataSource
- 部分类型已有方言实现（11 种，注册在 `DialectRegistry`），其余类型查询时会提示"该数据库类型正在开发中"

**注册数据源的调用链路**：
```
用户注册数据源 → AssetsDataSourceServiceImpl → 写入 AST_DATASOURCE → 
                                                → 同步 Redis hash
                                                → (其他模块读取 Redis 获取连接信息)
```

#### 第三层：基础设施中间件（平台运行依赖）
**用途**：平台自身运行所需的基础中间件
**配置位置**：`application-dev.yml`

| 中间件 | 用途 | 配置名 |
|--------|------|--------|
| **Redis** | 缓存、Session、数据源信息同步 | `spring.redis` |
| **RabbitMQ** | 异步消息、任务队列 | `spring.rabbitmq` |
| **DolphinScheduler** | ETL 任务调度、工作流编排 | `ds.base_url`（API 地址）|
| **Spark** | ETL 数据处理引擎 | `ds.spark.master_url`、`ds.spark.main_jar` |
| **HDFS** | 分布式文件存储 | `ds.hdfs.url` |
| **调度器 Redis** | DolphinScheduler 专用 Redis（获取最新数据源信息） | `ds.redis` |

### 5.3 动态数据源路由机制

启动类排除了 `DataSourceAutoConfiguration.class`，项目使用自定义数据源路由：

```
请求 → DynamicDataSourceAspect（AOP 拦截 @DS 注解）
    → DynamicDataSourceContextHolder（ThreadLocal 设置当前数据源 key）
    → DynamicDataSource（继承 AbstractRoutingDataSource，根据 key 路由）
    → 目标数据源
```

相关类：
- `DynamicDataSource.java` → 核心路由数据源
- `DynamicDataSourceContextHolder.java` → ThreadLocal 持有当前数据源 key
- `DynamicDataSourceAspect.java` → AOP 切面，日志记录 `@DS` 切换
- `MasterDataSourceConfig.java` → 注入 `datasource.type` 配置
- `@DataSource` 注解 / `DataSourceType` 枚举（MASTER/SLAVE）→ 预留的手动切换方式

## 6. 部署 / 启动依赖理解

从 README 和 DEPLOY.md 看，项目运行通常依赖：
- JDK 1.8
- Maven 3.6+
- Node.js 18+
- yarn 1.22+
- Redis 5+
- RabbitMQ
- 数据库（README 支持 DM8 / MySQL / Oracle 等，DEPLOY.md 也明确给了开发环境数据源配置）
- Spark
- DolphinScheduler 相关服务

### 启动顺序上的直觉
通常建议先完成：
1. 数据库
2. Redis
3. RabbitMQ
4. Spark / 调度器相关服务
5. 后端 `datamaster-server`
6. 前端 `datamaster-view`

### 导入 / 启动最容易踩的坑
- **启动类扫描范围很大**：任何业务模块不完整，都可能影响启动
- **数据源不是默认 Spring Boot 数据源**：主类排除了 `DataSourceAutoConfiguration`
- **外部依赖多**：Redis、RabbitMQ、数据库、Spark、调度器都可能影响最终是否能真正跑起来
- **前端和后端分离启动**：后端先跑通，再处理前端代理更稳妥

## 7. 初始化与构建结果

### 本地环境
当前环境验证结果如下：
- JDK：`1.8.0-262`
- Maven：`3.6.0`
- Node.js：`v24.13.0`
- yarn：`1.22.22`

### 初始化编译验证
我已经在本地执行过一次最小初始化编译：
```bash
mvn -q -DskipTests -pl datamaster-server -am compile
```

结果：
- **成功**
- **退出码 0**

这说明：
- Maven 能正确识别这个多模块项目
- `datamaster-server` 及其依赖模块能够完成编译初始化
- 项目的基础依赖与模块关系在当前环境下是可用的

### 含义
这一步相当于完成了"项目初始化"的核心验证：
- 依赖树可解析
- 模块可以编译
- 进入下一步 IDE 导入 / 运行配置检查是合理的

## 8. 当前代码理解结论

这个项目最核心的入口可以先记为：
- **主启动模块：`datamaster-server`**
- **主类：`BootstrapApplication`**
- **开发环境配置：`application.yml` + `application-dev.yml`**
- **构建方式：多模块 Maven 项目**

### 简化后的理解
如果把项目抽象成一句话：
> dataMaster 是一个以 `datamaster-server` 为启动入口、围绕数据治理和数据服务展开的多模块 Spring Boot 平台，后端依赖 Redis、RabbitMQ、数据库、Spark 和调度器等外部基础设施。

## 9. 建议的后续操作顺序
如果继续往下推进，建议按这个顺序：

1. 在 IDEA 中导入根目录 `D:/dev/dataMaster`
2. 让 IDEA 识别为 Maven 多模块项目
3. 选择 JDK 8
4. 执行 Maven Reload / Reimport
5. 等待依赖下载完成
6. 再跑一次 `datamaster-server` 的编译或启动
7. 检查 `application-dev.yml` 里外部服务配置是否可达
8. 如果要跑完整业务，再补齐数据库、Redis、RabbitMQ、Spark、调度器等依赖

## 10. 小结

当前我对这个项目的判断是：
- 结构是清晰的多模块工程
- 启动入口明确
- 配置链路明确
- 初始化编译已经成功
- 真正运行时的难点主要在外部依赖和业务模块联动

如果后面继续深入，最值得做的是：
- 画出 `datamaster-server → datamaster-common → 业务模块` 的依赖关系
- 梳理 `application-dev.yml` 里的外部服务依赖
- 检查真正能跑起来的最小闭环需要哪些服务
- 再补充模块级别的业务代码理解

---

## 附录：模块名新旧对照

| 用途 | 原名 | 新名 | 目录名 |
|------|------|------|--------|
| 启动入口 | DATAMASTER-server | datamaster-server | datamaster-server |
| 基础框架 | DATAMASTER-framework/* | datamaster-common/* | datamaster-common |
| ├ 公共工具 | DATAMASTER-common | datamaster-common-common | datamaster-common-common |
| ├ 数据源/安全/缓存 | DATAMASTER-mybatis/security/redis | datamaster-common-datasource | datamaster-common-datasource |
| ├ 配置管理/定时任务 | DATAMASTER-config/quartz | datamaster-common-config | datamaster-common-config |
| ├ MyBatis 扩展 | DATAMASTER-mybatis | datamaster-common-mybatis | datamaster-common-mybatis |
| └ 消息推送 | DATAMASTER-websocket | datamaster-common-websocket | datamaster-common-websocket |
| 系统管理 | DATAMASTER-module-system | datamaster-system | datamaster-system |
| 分类管理 | DATAMASTER-module-att | datamaster-taxonomy | datamaster-taxonomy |
| 数据标准 | DATAMASTER-module-dp | datamaster-standards | datamaster-standards |
| 数据资产 | DATAMASTER-module-da | datamaster-assets | datamaster-assets |
| 数据采集 | DATAMASTER-module-dpp | datamaster-collector | datamaster-collector |
| 数据服务 | DATAMASTER-module-ds | datamaster-service | datamaster-service |
| 数据建模 | DATAMASTER-module-dm | datamaster-modeling | datamaster-modeling |
| 元数据管理 | DATAMASTER-module-mc | datamaster-catalog | datamaster-catalog |
| 调度适配 | DATAMASTER-api-ds | datamaster-api-ds | datamaster-api-ds |
| ETL引擎 | DATAMASTER-etl | datamaster-etl | datamaster-etl |
| 数据质量 | DATAMASTER-quality | datamaster-quality | datamaster-quality |

### 类名前缀对照

| 原名前缀 | 对应模块 | 新名前缀 |
|----------|----------|----------|
| Da (Data Asset) | 数据资产 | Assets |
| Dp (Data Profile/Standard) | 数据标准 | Standards |
| Dpp (Data Pipeline Process) | 数据采集 | Collector |
| Ds (Data Service) | 数据服务 | Service |
| Dm (Data Model) | 数据建模 | Modeling |
| Mc (Metadata Catalog) | 元数据管理 | Catalog |
| Att (Attribute/Taxonomy) | 分类管理 | Taxonomy |
| Ai (Artificial Intelligence) | AI智能 | Intelligence |

### 表名前缀对照

| 原名前缀 | 新前缀 | 说明 |
|----------|--------|------|
| DA_ | AST_ | Asset |
| DP_ | STD_ | Standard |
| DPP_ | COL_ | Collection |
| DS_ | SVC_ | Service |
| DM_ | MDL_ | Model |
| MC_ | CAT_ | Catalog |
| ATT_ | TAX_ | Taxonomy |
| AI_ | AI_ | Intelligence（不变）|
| SYS_ | SYS_ | System（不变）|

*本文档为最终整理版，可作为后续导入 IDE、排查启动、做二次开发时的基础说明。*
