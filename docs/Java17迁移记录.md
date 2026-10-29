# Java 17 迁移记录

## 本次已修改

- 根pom java.version=17，统一maven-compiler-plugin 3.13.0与release=17。
- 根构建增加Enforcer Java版本下限17及Surefire 3.2.5；所有继承模块不再默认输出Java8字节码。
- datamaster-server移除source/target=1.8覆盖，统一release=17，保留既有MapStruct/Lombok处理器。
- 独立deploy/db-init（不继承主pom）release=17，compiler3.13.0、shade3.6.0。
- server Docker基础镜像改eclipse-temurin:17-jre，不假设原私有镜像仓库已有17标签。
- 更新AGENTS/README的Java基线；历史实现记录不批量改写。

## 明确未修改

- 暂保留Spring Boot 2.5.15、Spring Framework5.3.33、Spring Security5.7.12及LangChain4j0.35.0，不把框架/API迁移与JDK基线混为一项。
- 保留javax命名空间，不迁移jakarta。升级Boot3是另一个工作项。
- Lombok1.18.36、MapStruct1.5.5.Final已有较新版本，不盲目修改；已有JAXB显式依赖保留。
- 不安装/切换本机JAVA_HOME，不更改CI或生产机器，不拉镜像、不构建、不启动服务。
- 不修改DolphinScheduler/CHUNJUN/Flink二进制和容器，第三方执行环境不能通过平台pom设置自动升级。

## 发布前必须验证

1. Maven实际使用JDK17，IDE语言级别与运行JDK一致。Enforcer允许17及以上构建，release固定17；首次验收建议使用17。
2. 在JDK17上完成全模块编译/测试及server启动。Boot2.5.15是旧版本，此次并未证明其所有Starter、重打包插件和运行时依赖兼容17。
3. 验证认证登录/JJWT、JAXB、Swagger、数据库驱动、Redis、反射/代理、POI导出及嵌入式Redis。
4. 系统级artemis-http-client本地JAR、Hadoop3.0.0以及第三方驱动需专项回归，不能仅按业务源码语法判断兼容。
5. 所有本项目产物都使用Java17字节码，Java8 Worker不可加载。若平台JAR被DS/Flink/CHUNJUN加载，需要升级对应执行JVM并验证第三方兼容，或者另行明确独立兼容产物策略；不能直接上线旧Worker。
6. 当前FlinK1.16.2/CHUNJUN运行环境不在本次改动范围；全链路任务执行通过前，迁移只能称配置完成，不能称运行通过。
7. 若启动出现InaccessibleObjectException，先确认具体依赖并升级/修复，不能全局无差别加入--add-opens或忽略异常。
8. server镜像需能访问公开eclipse-temurin:17-jre；离线部署需要按实际镜像仓库同步17镜像后改为已验证地址。

## 验证状态

按项目约定未主动运行编译、构建、lint或测试，也未执行任何数据库迁移。当前仅完成构建目标、server运行镜像和文档基线的代码修改，尚未证明全项目Java17兼容。后续LangChain4j升级需单独选择目标版本并修改相关API和Schema测试。
