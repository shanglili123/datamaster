# Spring Boot 2.7.18 过渡升级记录（1023已撤回，仅保留历史）

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
