# DataMaster 2.0.0 升级与多 JDK 运行方案

## 1. 目标与范围

版本统一为2.0.0：父Maven的dataMaster.version、继承的后端模块、前端package版本、独立db-init及server示例镜像标签。第三方组件版本和历史数据库备份不批量替换；.flattened-pom.xml属于生成产物，由后续构建重新生成，不手工改写。

平台构建与主服务基线为Java17。采用多JDK并存、按进程明确选JVM，不替换全局Java、不无差别修改DolphinScheduler Worker环境，不把所有任务强制迁到同一JVM。

本方案先明确部署和验证顺序，本次不安装JDK、不启动容器、不执行构建、测试或迁移。

## 2. JDK选择规则

| 进程/场景 | 选择策略 |
| --- | --- |
| Maven/IDE构建平台模块 | JDK17，release=17，产物需要Java17+加载 |
| DataMaster主服务、独立db-init | 显式Java17绝对路径或17容器镜像 |
| DS启动独立Java17任务 | 只在该任务子进程设置JAVA_HOME/PATH，优先绝对java命令 |
| DolphinScheduler自身服务 | 保留已有受支持JDK，不因平台升级自动更改 |
| CHUNJUN/Flink及其插件 | 保留已验证执行环境；如果加载本项目17字节码，必须专门迁移实际加载它的JVM或另作独立进程隔离 |

“把JDK17路径加在前面”只对之后新启动的进程生效，不改变已经运行的Worker/Flink JVM。子进程用17也不会改变父Worker版本；同样，插件被父进程直接加载时不能只给另一个子进程设PATH。

## 3. 多JDK目录与启动方式

以下仅示例目录，部署时替换为实际已安装路径，不包含安装/下载行为。

### Linux独立进程

```bash
/opt/jdks/jdk-17/bin/java -jar /opt/datamaster/datamaster.jar
```

确实需要子工具读取JAVA_HOME时，限制到当前命令环境：

```bash
JAVA_HOME=/opt/jdks/jdk-17 PATH=/opt/jdks/jdk-17/bin:$PATH \
  /opt/jdks/jdk-17/bin/java -jar /opt/datamaster/task.jar
```

不得在公共profile或DS全局env中盲目覆盖已有JAVA_HOME。生产启动脚本应校验java路径存在与实际版本，并记录启动JDK版本而非密钥/完整环境变量。

### Windows PowerShell独立进程

```powershell
& 'D:\jdks\jdk-17\bin\java.exe' -jar 'D:\apps\datamaster\datamaster.jar'
```

构建时IDE Project SDK、Maven Runner和运行配置都选择17；只改IDE语言级别不等于Maven使用17。不修改系统注册表或全局PATH。

### 当前WSL Docker Worker

如DS任务需要独立JDK17，可将经过批准的JDK安装目录只读挂载到Worker内，例如 `/opt/jdks/jdk-17`，在任务命令中显式调用。Windows路径不能直接成为Linux容器JAVA_HOME，JDK操作系统/CPU架构必须匹配容器。

保留已有CHUNJUN_HOME、FLINK_HOME、启动wrapper以及本地Flink1.16 jar URL补丁，避免平台JDK迁移破坏已有任务环境。此步骤需要部署确认后再实施，本次未改任何挂载或二进制。

### DataMaster容器

主服务Dockerfile已改用eclipse-temurin:17-jre，代码示例标签统一2.0.0。私网/离线环境需要将真实可用的17镜像同步到受信仓库再引用；不能假设原私库已有相同标签。

## 4. 分阶段升级

### 阶段A：统一项目版本与Java基线

- 后端统一2.0.0，前端2.0.0，db-init2.0.0。
- compiler3.13.0、release17、Enforcer17+、Surefire3.2.5。
- 暂留Boot2.5.15、Framework5.3.33、Security5.7.12、LangChain4j0.35.0，先验证JDK变化。
- 不为迁移而批量改写业务语法或javax命名空间。

### 阶段B：构建与启动验证

经用户授权后在JDK17上执行全模块编译/测试和server启动验证。重点覆盖认证/JJWT/JAXB、代理反射、Swagger、连接池/JDBC、Redis、POI、Hadoop及本地系统JAR。

出现错误逐项定位依赖，不用大量--add-opens掩盖全部问题。不得宣称Java17可直接加载所有旧依赖而无需验证。

### 阶段C：执行环境与端到端任务

区分HTTP调用DS、独立Java任务、同JVM插件三类执行；仅独立任务可直接选择专用JDK17。验证采集/ETL/质量检查和状态回写。平台仅通过HTTP管理第三方服务时，不要求它们与平台使用相同JDK。

### 阶段D：AI框架独立升级

Java17运行基线通过后，再选择具体LangChain4j目标版本，核对ToolExecutor/Schema/ChatMemory/模型接口/Listener/API兼容，单独回归8个分析工具、指标草稿确认、动态模型API与调用计量。

本方案不指定未经核验的“最新版”版本号，不将Boot3/jakarta迁移绑入当前Java17步骤。模型路由/监控/评估可以先在已有框架完善，不必为概念升级全部依赖。

## 5. 数据库、配置与安全

- 版本提升不自动执行SQL、不重建数据库。
- 先盘点实际已执行迁移，再执行未落库文件；指标迁移已执行的不能重复CREATE。
- 模型统计/供应商/动态配置迁移状态须查询确认，不因文件存在就认为已执行。
- 动态Token加密主密钥保留在部署环境，升级和回滚都需保留密钥，不写入仓库。
- 配置、上传、数据库、模型服务和DS状态与应用产物分别管理，不能用替换JAR冒充数据回滚。

## 6. 验收门槛

- 构建、主服务、db-init实际选择JDK17，前后端显示/包版本一致。
- Java17编译/测试及启动通过，不出现UnsupportedClassVersionError或未处理反射异常。
- 多JDK任务互不污染，旧任务保持原JVM，指定17任务确实在17执行。
- DataAgent仅建设流程/指标草稿；数据分析页正确选择正式指标或探索链路。
- 模型供应商切换、API密钥加解密、并发准入、调用统计和未知Token口径验证。
- Doris查询、原DS任务、CHUNJUN wrapper/Flink补丁及回写节点全部完成回归。
- 长请求、超时、取消、权限、确认防重放通过试点，未通过不得标记2.0.0生产可用。

## 7. 发布与回滚

应用产物按版本目录保存，例如releases/1.4.0与releases/2.0.0；启动配置明确java绝对路径。发布2.0.0时切到17，回滚旧产物时恢复原版本启动命令/JDK配置，不卸载其他JDK。

新增表默认保留，不因回滚应用删除指标、版本、个人分析或调用记录；如需结构回滚另写审核脚本，不自动drop。模型密钥、ADS数据和第三方任务配置不随应用文件回滚。

## 8. 本次落地状态

已写方案、修改版本源和已有示例镜像标签。Java17构建/镜像基线此前已修改。没有安装JDK、改动DS/Flink容器、运行构建/测试/迁移、发布镜像或重启服务。配置变更不等于兼容验证通过。
