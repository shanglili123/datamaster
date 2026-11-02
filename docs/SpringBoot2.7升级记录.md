# Spring Boot 2.7.18 过渡升级记录

## 后续 Java 17 阶段（2026-11-02，当前状态优先）

用户要求继续升级，当前 `dev_1029_2.0` 保留 Boot 2.7.18，Java 编译与主服务运行基线现改为 17，详见 `docs/Java17迁移记录.md`。随后经用户授权，使用 JDK 17.0.19 + Maven 3.8.1 完成主服务及依赖 36 个反应堆项目、独立 db-init 的主源码编译，均 BUILD SUCCESS；未运行测试、打包或启动。下文 2026-10-30 的启动成功结果只属于当时的 Java 8 产物，不证明 Java 17 运行已经通过验证。旧分支 `dev_1023` 未修改。

## 当前升级分支（2026-10-30）

### 启动验证结果（已通过）

2026-10-30 18:38 在 `dev_1029_2.0` 分支以 JDK8 完成 `mvn -pl datamaster-server -am package -DskipTests`（BUILD SUCCESS），实际启动 `datamaster-server.jar`：**Started DataMasterApplication in 32.138 seconds**，服务已启动。

验证条件：隔离端口18989、主库连接附加 `default_transaction_read_only=on`（防启动副作用写库）、关闭动作执行/工作流 worker、run guard 和血缘同步。HTTP冒烟：`/captchaImage` 返回200成功；`/login` GET 正确拒绝；未带Token访问 `/getInfo`、`/ai/model-gateway/stats` 均返回业务401认证失败，Security过滤链在Boot2.7下正常。验证后已停止测试实例。

### 启动期修复的依赖环（Boot2.6+默认禁止循环引用）

均为单点 `@Lazy` 延迟业务期依赖或移除无调用注入，未开启 `spring.main.allow-circular-references`：

| 类 | 修复 |
| --- | --- |
| AssetsDatasourceServiceImpl | standardsModelApiService、collectorEtlTaskService 延迟 |
| DatasourceApiServiceImpl | assetsDatasourceApiService（适配器唯一依赖）延迟 |
| AssetsAssetServiceImpl | IAssetsDatasourceService、CollectorEtlTaskService、CollectorEtlTaskInstanceService 延迟 |
| AssetsAssetColumnServiceImpl | IAssetsAssetService 延迟 |
| CatalogDbServiceImpl | 数据源接口、表服务、列服务延迟 |
| CatalogTableServiceImpl | 移除未使用的资产接口注入；CatalogTaskService 延迟 |
| CollectorEtlTaskInstanceServiceImpl | ICollectorEtlTaskService 延迟 |
| CollectorEtlNodeInstanceServiceImpl | ICollectorEtlTaskInstanceService 延迟 |
| TaxonomyTagAssetRelServiceImpl | ITaxonomyTagService 延迟 |

另修复打包问题：server 模块默认 maven-jar-plugin 2.4 增量打包可能沿用旧 BOOT-INF/lib 导致修复不生效，已显式配置 maven-jar-plugin 3.1.1 + forceCreation。

### 仍未验证

- **单元测试未运行**：此前一次带测试构建中 datamaster-metadata-core 39个测试19个失败（JSqlParser血缘提取与资产身份），需确认是否为升级前既有失败；治理模块45个测试通过。
- 关闭的后台 worker（动作执行/工作流/血缘/run guard）、嵌入式Redis、SSE问数、DB-GPT、模型调用、ETL任务链路、前端联调均未在本轮启动验证覆盖。
- 生产/开发正常配置（非只读、worker开启）下的启动未验证。

以下为更早的排查过程记录。

### 实际启动排查（历史）

已按用户要求在当前分支JDK8运行依赖模块package（跳过测试）并多次实际启动18989端口。构建通过不代表启动通过；此前启动连续暴露元数据库/表/列、任务/实例/节点实例、数据源适配器、标签关联的初始化依赖环，已逐项限定延迟注入或移除无实际调用的资产接口。

另发现server使用默认maven-jar-plugin2.4增量打包时可能沿用旧BOOT-INF/lib。已显式配置既有版本属性3.1.1并forceCreation=true，核对嵌套metadata class与本轮target/classes一致后才继续启动。

测试启动使用独立18989端口、主连接只读选项及关闭动作执行/工作流队列/run guard开关；没有停止用户既有服务、没有调用模型或执行写业务验收。后台启动尚在排查，最终成功与否以后续实际HTTP和日志结果为准。本轮没有重新运行单元测试，先前39个元数据测试19失败仍未解决。


已确认并仅修改当前`dev_1029_2.0`分支：后端版本2.0.0，Boot BOM与server打包插件统一2.7.18，Java编译目标仍1.8，LangChain4j仍0.35.0。重新加入数据源反向依赖的单点@Lazy、匿名注解发现Ant/PathPattern兼容和动态API的BuilderConfiguration匹配。不修改SecurityConfig白名单或任务Token校验，不开启全局循环依赖。

本轮仅代码迁移，未再次运行构建、测试、启动或数据库迁移。此前构建与测试结果属于当时工作区，不能作为当前分支验证结果。没有切换、提交或修改dev_1023。

用户随后启动报告第二条循环：DatasourceApiServiceImpl → AssetsDatasourceServiceImpl → AssetsTableGovernanceApiServiceImpl → CatalogTableServiceImpl → CatalogDbServiceImpl → DatasourceApiServiceImpl。已将CatalogDbServiceImpl的数据源接口延迟注入，该接口仅在详情查询时使用，无初始化调用。另发现CatalogDbServiceImpl与CatalogTableServiceImpl存在直接双向注入，将库服务持有的表服务延迟注入；不改变业务方法、不开放全局循环引用。本次未执行编译或重启，尚不能确认所有启动依赖环均已消除。

以下为旧分支撤回经过与先前验证的历史记录，不覆盖以上当前状态。


## 最终分支口径（2026-10-30，优先于下文）

用户明确要求：`dev_1023`保留旧1.x基线，升级仅属于`dev_1029_2.0`。当前1023已恢复到历史提交23051f9f的版本配置：后端1.4.0、Java8、Boot2.5.15、LangChain4j0.35.0；容器恢复Java8，db-init恢复1.0.0，UI恢复其原package版本3.8.8（不是后端版本）。

根/服务端POM的Boot2.7改动已撤回；本轮循环依赖@Lazy、PathPattern动态映射适配、匿名地址发现适配及删除的WebConfig均撤回至分支提交版本。未修改现有SecurityConfig白名单或任务Token校验，任务回调body修复仍保留。此操作没有切换/提交/修改1029分支，没有回退业务代码。

未在回退后运行编译/测试或启动服务，旧target与flattened POM尚需下次构建再生成；下文编译通过仅属于已撤回的升级状态，不证明当前旧版本启动结果。IDE项目SDK请使用JDK8。

## 本次范围（以下为撤回前记录）

项目发布版本保持2.0.0。仅将Spring Boot从2.5.15迁到2.7.18，保留Java8编译目标、javax命名空间和LangChain4j0.35.0，不恢复此前已回退的Java17 POM设置。

- 根pom新增spring-boot.version=2.7.18，spring-boot-dependencies使用该属性。
- datamaster-server的spring-boot-maven-plugin使用同一属性，避免BOM与repackage插件版本分离。
- 暂保留现有Framework5.3.33与Security5.7.12 BOM覆盖，保持本轮升级边界。没有声称已经核验整个依赖树。
- 不批量升级Druid、MyBatis-Plus、动态数据源、PageHelper、Knife4j、Redis/Neo4j客户端，不改第三方服务。

## 已做静态检查

安全配置当前已是SecurityFilterChain，无需因Boot2.7改为Security6。antMatchers及javax仍属于现有Boot2技术线，不在此时做Jakarta替换。

仅检查源POM，生成的.flattened-pom.xml和target等不手工修改。此类旧版本信息需要后续构建重新生成。

## 验证重点（尚未执行）

1. Boot2.6起默认禁止循环依赖，若启动暴露循环应定位Bean引用并解耦/局部延迟，不默认加allow-circular-references=true。
2. 默认MVC路径匹配变化可能影响API文档、历史路由、静态资源；先实际验证，不预先全局切回Ant匹配。
3. 登录/验证码/JWT、SecurityFilterChain、接口PreAuthorize、跨域及SSE/WebSocket鉴权。
4. Druid/多数据源/MyBatis-Plus/Join/PageHelper拦截器、事务和分页。现有不同模块MyBatis-Plus3.5.2/3.5.3声明需后续依赖树确认，未在本轮擅自统一。
5. Redis序列化与连接池、JCache/Ehcache、Neo4j、Validation、Knife4j、Thymeleaf。
6. server重打包与实际启动，数据源驱动、本地系统JAR以及探查/采集/质量任务。
7. AI数据分析、流程确认、动态模型配置及调用统计的真实联调；Boot升级不等于这些功能已经验证。

## 与Java17方案的关系

Java17仍是后续计划，当前POM已按用户要求恢复Java8目标。此前Dockerfile/README/AGENTS中的Java17基线未随POM回退，部署前应明确最终JDK方案并同步这些文件，不能据这些文字认定当前编译目标已经是17。Java8字节码可在较高JDK运行，但依赖运行兼容仍需检查。

Boot2.7.18是2.x过渡基线，不代表长期社区安全支持承诺。后续Java17、LangChain4j以及Boot3/Jakarta应分别实施，避免一次引入多个变量。

## 已定位启动依赖环（2026-10-30）

- 用户启动暴露`StandardsModelServiceImpl → DatasourceApiServiceImpl → AssetsDatasourceServiceImpl → IStandardsModelApiService`的初始化环；Boot2.6+默认禁用循环引用使原先隐含的依赖环显现。
- 已在`AssetsDatasourceServiceImpl.standardsModelApiService`单个反向依赖加`@Lazy`，该接口仅业务方法使用，PostConstruct缓存初始化不调用模型服务；未开启`spring.main.allow-circular-references`，未覆盖其他升级修改。
- 已使用本机JDK17执行`mvn -pl datamaster-server -am -DskipTests -Ddocker.build.skip=true -Ddocker.push.skip=true -Ddocker.print.skip=true compile`，2026-10-30 10:15:44 BUILD SUCCESS，退出码0，用时2分01秒。编译目标仍按当前POM为Java8；未运行测试/重启服务，本次不证明所有Bean启动都已通过。
- 用户要求继续核查昨日权限/白名单及动态路由改动，检查中，不能据编译成功宣称权限验收完成。

## 权限与动态映射检查（2026-10-30）

- 昨日`PermitAllUrlProperties`改为遍历MVC映射Bean，并兼容Ant/PathPattern条件；当前匿名注解集合未发现新增全局放行，SecurityConfig仍以anyRequest.authenticated收尾。现有大范围命名空间白名单没有因此扩大，不等于已经审计所有既存公开接口。
- 新组件root/stage回调使用方法级@Anonymous绕过用户登录但强制X-DataMaster-Task-Token哈希/发布版本检查；动态/services接口仍有@ServiceCheckClientToken及MVC拦截器。未恢复全局/**，未去掉Token校验。
- 发现动态RequestMappingInfo仍默认Ant构建，升级后绑定Boot主PathPattern映射会不一致；已在`MappingHandlerMapping.getRequestMapping`加入`.options(requestMappingHandlerMapping.getBuilderConfiguration())`，注册/注销共享该构造。2026-10-30 10:24:31定向后端及依赖编译BUILD SUCCESS，退出码0，用时1分54秒；尚未运行动态API请求验收。
- 旧单任务增量prepare/complete要求用户登录/回调权限，而旧TaskConverter HTTP未带凭据，为已发现兼容缺口；用户范围是新任务，不擅自给旧回调匿名放行。若要恢复旧任务需另接任务Token，不能简单加白名单。

## 执行状态

已修改源POM并记录方案。按项目约定未执行Maven编译、构建、测试、lint、依赖解析、数据库迁移、容器构建或服务重启。只有完成编译、启动和关键链路回归后才可称升级验证通过。
