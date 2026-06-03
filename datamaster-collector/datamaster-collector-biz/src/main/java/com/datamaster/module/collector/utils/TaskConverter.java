

package com.datamaster.module.collector.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.datamaster.api.ds.api.etl.*;
import com.datamaster.api.ds.api.etl.ds.*;
import com.datamaster.common.config.DsRedisConfig;
import com.datamaster.common.config.RabbitmqConfig;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.utils.datax.FlinkxJson;
import com.datamaster.module.collector.utils.ds.component.ComponentFactory;
import com.datamaster.module.collector.utils.model.DsResource;
import com.datamaster.module.collector.utils.model.FlinkxIncrementalConfig;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class TaskConverter {
    @Resource
    private FlinkxJson flinkxJson;

    private static String resourceName;
    private static String defaultMainClass;
    private static String defaultMaster;
    private static String resourceUrl;
    private static RabbitmqConfig rabbitmqConfig;
    private static DsRedisConfig dsRedisConfig;

    @Value("${ds.spark.main_jar}")
    private void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Value("${ds.spark.main_class}")
    private void setDefaultMainClass(String defaultMainClass) {
        this.defaultMainClass = defaultMainClass;
    }

    @Value("${ds.spark.master_url}")
    private void setDefaultMaster(String defaultMaster) {
        this.defaultMaster = defaultMaster;
    }

    @Value("${ds.resource_url}")
    private void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    @Resource
    private void setRabbitmqConfig(RabbitmqConfig rabbitmqConfig) {
        this.rabbitmqConfig = rabbitmqConfig;
    }

    @Resource
    private void setDsRedisConfig(DsRedisConfig dsRedisConfig) {
        this.dsRedisConfig = dsRedisConfig;
    }

    // 默认配置常量
    private static final long DEFAULT_ENVIRONMENT_CODE = 133155949418208L; // 默认环境编码
    private static final String DEFAULT_WORKER_GROUP = "default"; // 默认工作组
    private static final String DEFAULT_FLAG = "YES"; // 默认标志，表示节点启用
    private static final String DEFAULT_IS_CACHE = "NO"; // 默认不启用缓存
    private static final String DEFAULT_TASK_PRIORITY = "MEDIUM"; // 默认任务优先级
    private static final String DEFAULT_TASK_TYPE = "SPARK"; // 默认任务类型，SPARK或DATAX等
    private static final String DEFAULT_PROGRAM_TYPE = "JAVA"; // 默认程序类型，JAVA
    private static final String DEFAULT_MAIN_JAR = "file:/dolphinscheduler/default/resources/spart-demo-1.0.jar"; // 默认主Jar路径
    private static final String DEFAULT_DEPLOY_MODE = "client"; // 默认部署模式
    private static final int DEFAULT_DRIVER_CORES = 1; // 默认驱动核心数
    private static final String DEFAULT_DRIVER_MEMORY = "2G"; // 默认驱动内存
    private static final int DEFAULT_NUM_EXECUTORS = 1; // 默认执行器数量
    private static final String DEFAULT_EXECUTOR_MEMORY = "4G"; // 默认执行器内存
    private static final int DEFAULT_EXECUTOR_CORES = 2; // 默认执行器核心数
    private static final String DEFAULT_SQL_EXECUTION_TYPE = "SCRIPT"; // 默认SQL执行类型
    private static final String DEFAULT_CONDITION_TYPE = "NONE"; // 默认条件类型为 "NONE"

    private static final int DEFAULT_TASK_failRetryTimes = 0; // failRetryTimes失败重试次数
    private static final int DEFAULT_TASK_delayTime = 0; // delayTime延时执行时间
    private static final int DEFAULT_TASK_failRetryInterval = 1; // failRetryInterval失败重试间隔

    private static final String HTTP_CHECK_CONDITION = "STATUS_CODE_DEFAULT";
    private static final int HTTP_CONNECT_TIMEOUT = 60000;
    private static final int HTTP_SOCKET_TIMEOUT = 60000;
    private static final Set<String> SUPPORTED_INCREMENTAL_TIME_FORMATS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "yyyy-MM-dd",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    FlinkxIncrementalConfig.DEFAULT_TIME_FORMAT)));



    public static final String TASK_INSTANCE_LOG_KEY = "log:taskInstanceLog:";//任务实例日志key

    public static final String PROCESS_INSTANCE_LOG_KEY = "log:processInstanceLog:";//流程实例日志key

    public static final String ETL_READER_ID_KEY = "etl:reader:id:";

    public static final String ETL_READER_DATE_KEY = "etl:reader:date:";

    private static String resolveWorkerGroup(String projectWorkerGroup, Object configuredWorkerGroup) {
        if (StringUtils.isNotEmpty(projectWorkerGroup)) {
            return projectWorkerGroup;
        }
        if (configuredWorkerGroup != null && StringUtils.isNotEmpty(String.valueOf(configuredWorkerGroup))) {
            return String.valueOf(configuredWorkerGroup);
        }
        return DEFAULT_WORKER_GROUP;
    }

    public static DsTaskSaveReqDTO buildDsTaskSaveReq(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {//名字
        return buildDsTaskSaveReq(CollectorEtlNewNodeSaveReqVO, null);
    }

    public static DsTaskSaveReqDTO buildDsTaskSaveReq(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, String projectWorkerGroup) {//名字
        //创建返回实体
        DsTaskSaveReqDTO dsTaskSaveReqDTO = new DsTaskSaveReqDTO();
        //1、封装基础参数
        dsTaskSaveReqDTO.setName(CollectorEtlNewNodeSaveReqVO.getName());
        if(StringUtils.isNotEmpty(CollectorEtlNewNodeSaveReqVO.getCode())){
            dsTaskSaveReqDTO.setProcessDefinitionCode(Long.parseLong(CollectorEtlNewNodeSaveReqVO.getCode()));
        }
        dsTaskSaveReqDTO.setDescription(CollectorEtlNewNodeSaveReqVO.getDescription());
        dsTaskSaveReqDTO.setExecutionType(CollectorEtlNewNodeSaveReqVO.getExecutionType());


        //2、封装节点信息 DATAX、SPARK
        String taskDefinition = buildTaskDefinition(CollectorEtlNewNodeSaveReqVO.getTaskDefinitionList(), projectWorkerGroup);

        String taskRelation = buildTaskRelationJson(CollectorEtlNewNodeSaveReqVO.getTaskRelationJson());

        String location = buildTaskNodeLocations(CollectorEtlNewNodeSaveReqVO.getLocations());


        dsTaskSaveReqDTO.setTaskDefinitionJson(taskDefinition);
        dsTaskSaveReqDTO.setTaskRelationJson(taskRelation);
        dsTaskSaveReqDTO.setLocations(location);


        return dsTaskSaveReqDTO;
    }

    private static String buildTaskNodeLocations(List<Map<String, Object>> locations) {
        // 解析输入的 JSON 字符串为 List
        List<Map<String, Object>> list = locations;

        List<Map<String, Object>> result = new ArrayList<>();

        // 遍历每个节点坐标信息
        for (Map<String, Object> location : list) {
            Map<String, Object> locationMap = new HashMap<>();

            // 填充必要字段
            locationMap.put("taskCode", Long.parseLong(String.valueOf(location.getOrDefault("taskCode", 0L)))); // 默认 taskCode 为 0
            locationMap.put("x", location.getOrDefault("x", 0)); // 默认 x 为 0
            locationMap.put("y", location.getOrDefault("y", 0)); // 默认 y 为 0

            // 将处理后的坐标信息加入结果列表
            result.add(locationMap);
        }

        // 返回处理后的 JSON 字符串
        return JSON.toJSONString(result);
    }

    private static String buildTaskRelationJson(String taskRelationJson) {
        // 解析输入的 JSON 字符串为 List
        List<Map<String, Object>> list = JSONUtils.convertTaskDefinitionJson(taskRelationJson);

        List<Map<String, Object>> result = new ArrayList<>();

        // 遍历每个关系节点
        for (Map<String, Object> relation : list) {
            Map<String, Object> relationMap = new HashMap<>();

            // 填充默认值和必要字段
            relationMap.put("id", relation.getOrDefault("dsId", null)); // 默认 id 为 0
            relationMap.put("preTaskCode", relation.getOrDefault("preTaskCode", 0L)); // 默认 preTaskCode 为 0
            relationMap.put("preTaskVersion", relation.getOrDefault("preTaskVersion", 0)); // 默认 preTaskVersion 为 0
            relationMap.put("postTaskCode", relation.getOrDefault("postTaskCode", 0L)); // 默认 postTaskCode 为 0
            relationMap.put("postTaskVersion", relation.getOrDefault("postTaskVersion", 0)); // 默认 postTaskVersion 为 0
            relationMap.put("conditionType", relation.getOrDefault("conditionType", DEFAULT_CONDITION_TYPE)); // 默认条件类型为 "NONE"

            // 将处理后的节点关系加入结果列表
            result.add(relationMap);
        }

        // 返回处理后的 JSON 字符串
        return JSON.toJSONString(result);
    }

    /**
     * 构建任务定义
     *
     * @param taskDefinitionJson 任务定义JSON字符串
     * @return 构建后的任务定义JSON字符串
     */
    public static String buildTaskDefinition(String taskDefinitionJson) {
        return buildTaskDefinition(taskDefinitionJson, null);
    }

    public static String buildTaskDefinition(String taskDefinitionJson, String projectWorkerGroup) {
        List<Map<String, Object>> list = JSONUtils.convertTaskDefinitionJson(taskDefinitionJson);

        List<Map<String, Object>> result = new ArrayList<>();

        //自定义参数
//        Map<String, Object> definitionJsonMap = JSONUtils.convertTaskDefinitionJsonMap(draftJson);


        // 遍历每个任务定义
        for (Map<String, Object> task : list) {
            // 处理每个 task 的默认值和必要字段
            Map<String, Object> taskMap = new HashMap<>();

            // 设置基本信息字段
            taskMap.put("id", task.getOrDefault("dsId", null)); // 默认 id 为 0
            taskMap.put("name", task.getOrDefault("name", "")); // 默认空字符串
            taskMap.put("code", task.getOrDefault("code", 0L)); // 默认 code 为 0L
            taskMap.put("version", task.getOrDefault("version", 0)); // 默认版本号为1
            taskMap.put("description", task.getOrDefault("description", "")); // 默认描述为空
            taskMap.put("workerGroup", resolveWorkerGroup(projectWorkerGroup, task.get("workerGroup"))); // 项目专属工作组优先
            taskMap.put("environmentCode", task.getOrDefault("environmentCode", DEFAULT_ENVIRONMENT_CODE)); // 默认环境编码
            taskMap.put("flag", DEFAULT_FLAG); // 默认 flag 为 "YES"
            taskMap.put("isCache", task.getOrDefault("isCache", DEFAULT_IS_CACHE)); // 默认 isCache 为 "NO"
            taskMap.put("taskPriority", task.getOrDefault("taskPriority", DEFAULT_TASK_PRIORITY)); // 默认任务优先级为 "MEDIUM"
            taskMap.put("taskType", task.getOrDefault("taskType", DEFAULT_TASK_TYPE)); // 默认任务类型为 "SPARK"
            taskMap.put("taskExecuteType", "BATCH");

            //2025-06-25 新增配置项默认值
            taskMap.put("failRetryTimes", MapUtils.getObject(task,"failRetryTimes",DEFAULT_TASK_failRetryTimes));
            taskMap.put("delayTime", MapUtils.getObject(task,"delayTime",DEFAULT_TASK_delayTime));
            taskMap.put("failRetryInterval", MapUtils.getObject(task,"failRetryInterval",DEFAULT_TASK_failRetryInterval));

            //组件taskParams的封装
            String componentType = String.valueOf(task.get("componentType")); //组件类型
            Map<String, Object> params = (Map<String, Object>) MapUtils.getObject(task, "taskParams");

            //根据类型存入默认数据
            if (StringUtils.equals(TaskComponentTypeEnum.SPARK_CLEAN.getCode(), componentType)
                    || StringUtils.equals(TaskComponentTypeEnum.SPARK_SQL_DEV.getCode(), componentType)) {
                params.put("mainClass", defaultMainClass);
                params.put("resourceName", resourceName);
                params.put("master", defaultMaster);
            }
            // 提取参数
//            params.put("driverCores", MapUtils.getObject(definitionJsonMap, "driverCores", DEFAULT_DRIVER_CORES));
//            params.put("driverMemory", MapUtils.getObject(definitionJsonMap, "driverMemory", DEFAULT_DRIVER_MEMORY));
//            params.put("numExecutors", MapUtils.getObject(definitionJsonMap, "numExecutors", DEFAULT_NUM_EXECUTORS));
//            params.put("executorMemory", MapUtils.getObject(definitionJsonMap, "executorMemory", DEFAULT_EXECUTOR_MEMORY));
//            params.put("executorCores", MapUtils.getObject(definitionJsonMap, "executorCores", DEFAULT_EXECUTOR_CORES));
//            params.put("yarnQueue", MapUtils.getObject(definitionJsonMap, "yarnQueue", ""));

            // 将任务的taskParams加入到taskMap中
            taskMap.put("taskParams", ComponentFactory.getComponentItem(componentType).parse(params));

            // 将填充好的任务加入到结果列表
            result.add(taskMap);
        }

        // 返回处理后的JSON字符串
        return JSON.toJSONString(result);
    }


    /**
     * 将 CollectorEtlNewNodeSaveReqVO 和 ProcessDefinition 转换为 CollectorEtlTaskSaveReqVO
     *
     * @param CollectorEtlNewNodeSaveReqVO 外部请求的任务数据
     * @param data                   流程定义数据
     * @return 转换后的 CollectorEtlTaskSaveReqVO
     */
    public static CollectorEtlTaskSaveReqVO convertToCollectorEtlTaskSaveReqVO(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, ProcessDefinition data) {
        // 创建 CollectorEtlTaskSaveReqVO 对象
        CollectorEtlTaskSaveReqVO createReqVO = new CollectorEtlTaskSaveReqVO();

        // 填充任务数据
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());//任务类型
        createReqVO.setName(data.getName()); // 任务名称
        createReqVO.setCode(String.valueOf(data.getCode())); // 任务编码
        createReqVO.setVersion(data.getVersion()); // 版本号
        createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
        createReqVO.setProjectCode(String.valueOf(data.getProjectCode())); // 项目编码
        createReqVO.setDescription(CollectorEtlNewNodeSaveReqVO.getDescription()); // 描述
        createReqVO.setLocations(data.getLocations()); // 节点坐标信息
        createReqVO.setLocations(data.getLocations()); // 节点坐标信息
        createReqVO.setDsId(data.getId()); // DolphinScheduler的ID

        String releaseState = CollectorEtlNewNodeSaveReqVO.getReleaseState();
        // 根据 releaseState 设置 status（0: 未上线, 1: 已上线）
        if (StringUtils.equals("-2", releaseState) || StringUtils.equals("-3", releaseState)) {
            createReqVO.setStatus(releaseState); // 已上线
        } else if ("offline".equalsIgnoreCase(data.getReleaseState())) {
            createReqVO.setStatus("0"); // 未上线
        } else if ("online".equalsIgnoreCase(data.getReleaseState())) {
            createReqVO.setStatus("1"); // 已上线
        } else {
            createReqVO.setStatus("0"); // 未上线
        }
        createReqVO.setRemark(""); // 默认备注（可根据需要调整）

        createReqVO.setExecutionType(data.getExecutionType());//执行执行策略
        // 填充创建者和更新时间信息
        createReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
        createReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
        createReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
        createReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
        createReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
        createReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

        createReqVO.setPersonCharge(CollectorEtlNewNodeSaveReqVO.getPersonCharge());
        createReqVO.setContactNumber(CollectorEtlNewNodeSaveReqVO.getContactNumber());
        createReqVO.setCatCode(CollectorEtlNewNodeSaveReqVO.getCatCode());
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());

        //暂时没意义参数
        createReqVO.setTimeout(CollectorEtlNewNodeSaveReqVO.getTimeout());


        // 返回生成的 CollectorEtlTaskSaveReqVO
        return createReqVO;
    }

    public static CollectorEtlTaskLogSaveReqVO fromCollectorEtlTaskLogSaveReqVO(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, ProcessDefinition processDefinition) {
        // 创建 CollectorEtlTaskSaveReqVO 对象
        CollectorEtlTaskLogSaveReqVO createReqVO = new CollectorEtlTaskLogSaveReqVO();
        ProcessDefinitionLog data = processDefinition.getProcessDefinitionLog();
        // 填充任务数据
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());
        createReqVO.setName(data.getName()); // 任务名称
        createReqVO.setCode(String.valueOf(data.getCode())); // 任务编码
        createReqVO.setVersion(data.getVersion()); // 版本号
        createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
        createReqVO.setProjectCode(String.valueOf(data.getProjectCode())); // 项目编码
        createReqVO.setDescription(CollectorEtlNewNodeSaveReqVO.getDescription()); // 描述
        createReqVO.setLocations(data.getLocations()); // 节点坐标信息
        createReqVO.setDsId(data.getId()); // DolphinScheduler的ID

        // 根据 releaseState 设置 status（0: 未上线, 1: 已上线）
        if ("online".equalsIgnoreCase(data.getReleaseState())) {
            createReqVO.setStatus("1"); // 已上线
        } else if ("offline".equalsIgnoreCase(data.getReleaseState())) {
            createReqVO.setStatus("0"); // 未上线
        } else {
            createReqVO.setStatus("0"); // 未上线
        }
        createReqVO.setRemark(""); // 默认备注（可根据需要调整）

        createReqVO.setExecutionType(data.getExecutionType());//执行执行策略
        // 填充创建者和更新时间信息
        createReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
        createReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
        createReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
        createReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
        createReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
        createReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

        //暂时没意义参数
        createReqVO.setPersonCharge(CollectorEtlNewNodeSaveReqVO.getPersonCharge());
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());
        createReqVO.setTimeout(CollectorEtlNewNodeSaveReqVO.getTimeout());


        // 返回生成的 CollectorEtlTaskSaveReqVO
        return createReqVO;
    }

    /**
     * 将 CollectorEtlNewNodeSaveReqVO 和 ProcessDefinition 转换为 CollectorEtlTaskSaveReqVO
     *
     * @param CollectorEtlNewNodeSaveReqVO 外部请求的任务数据
     * @param task                   流程定义数据
     * @return 转换后的 CollectorEtlTaskSaveReqVO
     */
    public static CollectorEtlTaskLogSaveReqVO fromCollectorEtlTaskLogSaveReqVO(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, CollectorEtlTaskSaveReqVO task) {
        // 创建 CollectorEtlTaskSaveReqVO 对象
        CollectorEtlTaskLogSaveReqVO createReqVO = new CollectorEtlTaskLogSaveReqVO();
        // 填充任务数据
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());
        createReqVO.setName(task.getName()); // 任务名称
        createReqVO.setCode(task.getCode()); // 任务编码
        createReqVO.setVersion(task.getVersion()); // 版本号
        createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
        createReqVO.setProjectCode(task.getProjectCode()); // 项目编码
        createReqVO.setDescription(CollectorEtlNewNodeSaveReqVO.getDescription()); // 描述
        createReqVO.setLocations(task.getLocations()); // 节点坐标信息
        createReqVO.setDsId(task.getId()); // DolphinScheduler的ID

        // 根据 releaseState 设置 status（0: 未上线, 1: 已上线）
        if ("online".equalsIgnoreCase(task.getStatus())) {
            createReqVO.setStatus("1"); // 已上线
        } else if ("offline".equalsIgnoreCase(task.getStatus())) {
            createReqVO.setStatus("0"); // 未上线
        } else {
            createReqVO.setStatus("0"); // 未上线
        }
        createReqVO.setRemark(""); // 默认备注（可根据需要调整）

        createReqVO.setExecutionType(task.getExecutionType());//执行执行策略
        // 填充创建者和更新时间信息
        createReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
        createReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
        createReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
        createReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
        createReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
        createReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

        //暂时没意义参数
        createReqVO.setPersonCharge(CollectorEtlNewNodeSaveReqVO.getPersonCharge());
        createReqVO.setType(CollectorEtlNewNodeSaveReqVO.getType());
        createReqVO.setTimeout(CollectorEtlNewNodeSaveReqVO.getTimeout());


        // 返回生成的 CollectorEtlTaskSaveReqVO
        return createReqVO;
    }

    public static CollectorEtlTaskLogSaveReqVO fromCollectorEtlTaskSaveReqVO(CollectorEtlTaskSaveReqVO CollectorEtlTaskSaveReqVO) {
        CollectorEtlTaskLogSaveReqVO logSaveReqVO = new CollectorEtlTaskLogSaveReqVO();

        // 基本字段直接映射
        logSaveReqVO.setType(CollectorEtlTaskSaveReqVO.getType());
        logSaveReqVO.setName(CollectorEtlTaskSaveReqVO.getName());
        logSaveReqVO.setCode(CollectorEtlTaskSaveReqVO.getCode());
        logSaveReqVO.setVersion(CollectorEtlTaskSaveReqVO.getVersion());
        logSaveReqVO.setProjectId(CollectorEtlTaskSaveReqVO.getProjectId());
        logSaveReqVO.setProjectCode(CollectorEtlTaskSaveReqVO.getProjectCode());
        logSaveReqVO.setPersonCharge(CollectorEtlTaskSaveReqVO.getPersonCharge());
        logSaveReqVO.setLocations(CollectorEtlTaskSaveReqVO.getLocations());
        logSaveReqVO.setDescription(CollectorEtlTaskSaveReqVO.getDescription());
        logSaveReqVO.setTimeout(CollectorEtlTaskSaveReqVO.getTimeout());
        logSaveReqVO.setExtractionCount(CollectorEtlTaskSaveReqVO.getExtractionCount());
        logSaveReqVO.setWriteCount(CollectorEtlTaskSaveReqVO.getWriteCount());
        logSaveReqVO.setDsId(CollectorEtlTaskSaveReqVO.getDsId());
        logSaveReqVO.setRemark(CollectorEtlTaskSaveReqVO.getRemark());
        logSaveReqVO.setStatus(CollectorEtlTaskSaveReqVO.getStatus());


        // 填充创建者和更新时间信息
        logSaveReqVO.setCreatorId(CollectorEtlTaskSaveReqVO.getProjectId()); // 假设项目ID为创建者ID（根据需求调整）
        logSaveReqVO.setCreateBy(CollectorEtlTaskSaveReqVO.getName()); // 假设任务名称为创建者（根据需求调整）
        logSaveReqVO.setCreateTime(CollectorEtlTaskSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
        logSaveReqVO.setUpdatorId(CollectorEtlTaskSaveReqVO.getProjectId()); // 假设项目ID为更新者ID（根据需求调整）
        logSaveReqVO.setUpdateBy(CollectorEtlTaskSaveReqVO.getName()); // 假设任务名称为更新者（根据需求调整）
        logSaveReqVO.setUpdateTime(CollectorEtlTaskSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

        return logSaveReqVO;
    }


    public static List<CollectorEtlNodeSaveReqVO> convertToCollectorEtlNodeSaveReqVOList(ProcessDefinition processDefinition, CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        List<CollectorEtlNodeSaveReqVO> resultList = new ArrayList<>();

        //取出入参数的信息
        List<Map<String, Object>> list = JSONUtils.convertTaskDefinitionJson(CollectorEtlNewNodeSaveReqVO.getTaskDefinitionList());

        // 遍历 ProcessDefinition 中的 taskDefinitionList
        for (TaskDefinition taskDefinition : processDefinition.getTaskDefinitionList()) {
            //获取前端前端封装的节点定义数据
            Map<String, Object> taskDefinitionMap = list.stream().filter(item -> {
                String code = MapUtils.getString(item, "code", "");
                return StringUtils.equals(taskDefinition.getCode(), code);
            }).findFirst().get();
            CollectorEtlNodeSaveReqVO createReqVO = new CollectorEtlNodeSaveReqVO();
            // 1. 任务相关信息
            createReqVO.setTaskType(CollectorEtlNewNodeSaveReqVO.getType());//任务类型
            createReqVO.setType(taskDefinition.getTaskType()); // 节点类型
            createReqVO.setComponentType(String.valueOf(taskDefinitionMap.get("componentType")));//组件类型
            createReqVO.setName(taskDefinition.getName()); // 任务名称
            createReqVO.setCode(String.valueOf(taskDefinition.getCode())); // 任务编码
            createReqVO.setVersion(taskDefinition.getVersion()); // 任务版本
            createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
            createReqVO.setProjectCode(String.valueOf(taskDefinition.getProjectCode())); // 项目编码

            createReqVO.setPriority(String.valueOf(taskDefinition.getTaskPriority()));//任务优先级
            createReqVO.setFailRetryTimes((long) taskDefinition.getFailRetryTimes());
            createReqVO.setFailRetryInterval((long) taskDefinition.getFailRetryInterval());
            createReqVO.setTimeout((long) taskDefinition.getTimeout());
            createReqVO.setDelayTime((long) taskDefinition.getDelayTime());
            createReqVO.setCpuQuota((long) taskDefinition.getCpuQuota());
            createReqVO.setMemoryMax((long) taskDefinition.getMemoryMax());
            createReqVO.setDescription(taskDefinition.getDescription());
            createReqVO.setDsId(taskDefinition.getId()); // 将任务的 dsId 设置为节点的 dsId

            createReqVO.setParameters(getTaskParamsAsJson(list, String.valueOf(taskDefinition.getCode()))); // 节点参数

            // 填充创建者和更新时间信息
            createReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
            createReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
            createReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
            createReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
            createReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
            createReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

            // 添加到结果列表
            resultList.add(createReqVO);
        }
        return resultList;
    }

    public static String getTaskParamsAsJson(List<Map<String, Object>> list, String code) {
        // 查找匹配的 taskParams
        Optional<Map<String, Object>> matchingTaskParams = list.stream()
                .filter(task -> task != null && StringUtils.equals(code, MapUtils.getString(task, "code")))
                .map(task -> (Map<String, Object>) MapUtils.getObject(task, "taskParams"))
                .filter(taskParams -> taskParams != null)
                .findFirst();

        // 如果找到了匹配的 taskParams，转为 JSON 并返回
        return matchingTaskParams.map(taskParams -> JSONUtils.toJson(taskParams))  // 调用 JSONUtils 转换为 JSON
                .orElse(null);  // 如果没有找到匹配项，返回 null
    }


    public static List<CollectorEtlNodeLogSaveReqVO> convertToCollectorEtlNodeLogSaveReqVOList(ProcessDefinition processDefinition, CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        List<CollectorEtlNodeLogSaveReqVO> resultList = new ArrayList<>();

        //取出入参数的信息
        List<Map<String, Object>> list = JSONUtils.convertTaskDefinitionJson(CollectorEtlNewNodeSaveReqVO.getTaskDefinitionList());

        // 遍历 ProcessDefinition 中的 taskDefinitionList
        for (TaskDefinition taskDefinition : processDefinition.getTaskDefinitionList()) {
            //获取前端前端封装的节点定义数据
            Map<String, Object> taskDefinitionMap = list.stream().filter(item -> {
                String code = MapUtils.getString(item, "code", "");
                return StringUtils.equals(taskDefinition.getCode(), code);
            }).findFirst().get();
            CollectorEtlNodeLogSaveReqVO createReqVO = new CollectorEtlNodeLogSaveReqVO();

            // 1. 任务相关信息
            createReqVO.setTaskType(CollectorEtlNewNodeSaveReqVO.getType());//任务类型
            createReqVO.setType(taskDefinition.getTaskType()); // 节点类型
            createReqVO.setComponentType(String.valueOf(taskDefinitionMap.get("componentType")));//组件类型
            createReqVO.setName(taskDefinition.getName()); // 任务名称
            createReqVO.setCode(String.valueOf(taskDefinition.getCode())); // 任务编码
            createReqVO.setVersion((long) taskDefinition.getVersion()); // 任务版本
            createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
            createReqVO.setProjectCode(String.valueOf(taskDefinition.getProjectCode())); // 项目编码

            createReqVO.setPriority(String.valueOf(taskDefinition.getTaskPriority()));//任务优先级
            createReqVO.setFailRetryTimes((long) taskDefinition.getFailRetryTimes());
            createReqVO.setFailRetryInterval((long) taskDefinition.getFailRetryInterval());
            createReqVO.setTimeout((long) taskDefinition.getTimeout());
            createReqVO.setDelayTime((long) taskDefinition.getDelayTime());
            createReqVO.setCpuQuota((long) taskDefinition.getCpuQuota());
            createReqVO.setMemoryMax((long) taskDefinition.getMemoryMax());
            createReqVO.setDescription(taskDefinition.getDescription());
            createReqVO.setDsId(taskDefinition.getId()); // 将任务的 dsId 设置为节点的 dsId

            createReqVO.setParameters(getTaskParamsAsJson(list, String.valueOf(taskDefinition.getCode()))); // 节点参数

            // 填充创建者和更新时间信息
            createReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
            createReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
            createReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
            createReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
            createReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
            createReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

            // 添加到结果列表
            resultList.add(createReqVO);
        }
        return resultList;
    }

    public static long getIdByCode(List<CollectorEtlNodeDO> CollectorEtlNodeDOList, String code, long preTaskVersion) {
        return CollectorEtlNodeDOList.stream()
                .filter(task -> StringUtils.equals(task.getCode(), code) && task.getVersion() == preTaskVersion)  // 匹配 code
                .map(CollectorEtlNodeDO::getId)  // 获取对应的 id
                .findFirst()  // 如果找到匹配项，返回第一个
                .orElse(-1L);  // 如果没有找到，返回默认值 -1
    }

    public static List<CollectorEtlTaskNodeRelSaveReqVO> convertToCollectorEtlTaskNodeRelSaveReqVOList(ProcessDefinition data, CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, List<CollectorEtlNodeDO> CollectorEtlNodeBatch, CollectorEtlTaskSaveReqVO CollectorEtlTaskSaveReqVO) {
        List<CollectorEtlTaskNodeRelSaveReqVO> resultList = new ArrayList<>();

        // 遍历 data 下的 taskRelationList，生成 CollectorEtlTaskNodeRelSaveReqVO
        for (ProcessTaskRelation taskRelation : data.getTaskRelationList()) {
            CollectorEtlTaskNodeRelSaveReqVO taskNodeRelSaveReqVO = new CollectorEtlTaskNodeRelSaveReqVO();

            // 1. 填充任务节点关系相关字段
            taskNodeRelSaveReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
            taskNodeRelSaveReqVO.setProjectCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getProjectCode())); // 项目编码

            // 任务相关字段
            taskNodeRelSaveReqVO.setTaskId(CollectorEtlTaskSaveReqVO.getId()); // 任务ID
            taskNodeRelSaveReqVO.setTaskCode(String.valueOf(data.getCode())); // 任务编码
            taskNodeRelSaveReqVO.setTaskVersion(data.getVersion()); // 任务版本

            // 前节点相关字段
            taskNodeRelSaveReqVO.setPreNodeId(getIdByCode(CollectorEtlNodeBatch, String.valueOf(taskRelation.getPreTaskCode()), taskRelation.getPreTaskVersion())); // 前节点ID
            taskNodeRelSaveReqVO.setPreNodeCode(String.valueOf(taskRelation.getPreTaskCode())); // 前节点编码
            taskNodeRelSaveReqVO.setPreNodeVersion(taskRelation.getPreTaskVersion()); // 前节点版本

            // 后节点相关字段
            taskNodeRelSaveReqVO.setPostNodeId(getIdByCode(CollectorEtlNodeBatch, String.valueOf(data.getCode()), taskRelation.getPreTaskVersion())); // 后节点ID
            taskNodeRelSaveReqVO.setPostNodeCode(String.valueOf(taskRelation.getPostTaskCode())); // 后节点编码
            taskNodeRelSaveReqVO.setPostNodeVersion(taskRelation.getPostTaskVersion()); // 后节点版本

            // 可选字段
            taskNodeRelSaveReqVO.setRemark(null); // 备注

            // 2. 填充新增/修改相关信息
            taskNodeRelSaveReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
            taskNodeRelSaveReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
            taskNodeRelSaveReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
            taskNodeRelSaveReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
            taskNodeRelSaveReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
            taskNodeRelSaveReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

            // 添加到结果列表
            resultList.add(taskNodeRelSaveReqVO);
        }

        return resultList;
    }


    public static List<CollectorEtlTaskNodeRelLogSaveReqVO> convertToCollectorEtlTaskNodeRelLogSaveReqVOList(ProcessDefinition data, CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, List<CollectorEtlNodeLogDO> CollectorEtlNodeBatch, CollectorEtlTaskLogSaveReqVO CollectorEtlTaskSaveReqVO) {
        List<CollectorEtlTaskNodeRelLogSaveReqVO> resultList = new ArrayList<>();

        // 遍历 data 下的 taskRelationList，生成 CollectorEtlTaskNodeRelSaveReqVO
        for (ProcessTaskRelation taskRelation : data.getTaskRelationList()) {
            CollectorEtlTaskNodeRelLogSaveReqVO taskNodeRelSaveReqVO = new CollectorEtlTaskNodeRelLogSaveReqVO();

            // 1. 填充任务节点关系相关字段
            taskNodeRelSaveReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
            taskNodeRelSaveReqVO.setProjectCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getProjectCode())); // 项目编码

            // 任务相关字段
            taskNodeRelSaveReqVO.setTaskId(CollectorEtlTaskSaveReqVO.getId()); // 任务ID
            taskNodeRelSaveReqVO.setTaskCode(String.valueOf(data.getCode())); // 任务编码
            taskNodeRelSaveReqVO.setTaskVersion(data.getVersion()); // 任务版本

            // 前节点相关字段
            taskNodeRelSaveReqVO.setPreNodeId(getCollectorEtlNodeLogDOIdByCode(CollectorEtlNodeBatch, String.valueOf(taskRelation.getPreTaskCode()), taskRelation.getPreTaskVersion())); // 前节点ID
            taskNodeRelSaveReqVO.setPreNodeCode(String.valueOf(taskRelation.getPreTaskCode())); // 前节点编码
            taskNodeRelSaveReqVO.setPreNodeVersion(taskRelation.getPreTaskVersion()); // 前节点版本

            // 后节点相关字段
            taskNodeRelSaveReqVO.setPostNodeId(getCollectorEtlNodeLogDOIdByCode(CollectorEtlNodeBatch, String.valueOf(data.getCode()), taskRelation.getPreTaskVersion())); // 后节点ID
            taskNodeRelSaveReqVO.setPostNodeCode(String.valueOf(taskRelation.getPostTaskCode())); // 后节点编码
            taskNodeRelSaveReqVO.setPostNodeVersion(taskRelation.getPostTaskVersion()); // 后节点版本

            // 可选字段
            taskNodeRelSaveReqVO.setRemark(null); // 备注

            // 2. 填充新增/修改相关信息
            taskNodeRelSaveReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
            taskNodeRelSaveReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
            taskNodeRelSaveReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
            taskNodeRelSaveReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
            taskNodeRelSaveReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
            taskNodeRelSaveReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

            // 添加到结果列表
            resultList.add(taskNodeRelSaveReqVO);
        }

        return resultList;
    }


    public static long getCollectorEtlNodeLogDOIdByCode(List<CollectorEtlNodeLogDO> CollectorEtlNodeDOList, String code, long preTaskVersion) {
        return CollectorEtlNodeDOList.stream()
                .filter(task -> StringUtils.equals(task.getCode(), code) && task.getVersion() == preTaskVersion)  // 匹配 code
                .map(CollectorEtlNodeLogDO::getId)  // 获取对应的 id
                .findFirst()  // 如果找到匹配项，返回第一个
                .orElse(-1L);  // 如果没有找到，返回默认值 -1
    }


    /**
     * 工具方法，生成 DsSchedulerSaveReqDTO。
     *
     * @param crontab               Cron 表达式
     * @param processDefinitionCode 任务编码
     * @return DsSchedulerSaveReqDTO
     */
    public static DsSchedulerSaveReqDTO createSchedulerRequest(String crontab, String processDefinitionCode) {
        return createSchedulerRequest(crontab, processDefinitionCode, null);
    }

    public static DsSchedulerSaveReqDTO createSchedulerRequest(String crontab, String processDefinitionCode, String projectWorkerGroup) {
        // 获取当前时间
        String startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 获取100年后的时间
        long currentTime = System.currentTimeMillis();
        String endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(currentTime + 100L * 365 * 24 * 60 * 60 * 1000));

        // 创建 DsSchedulerSaveReqDTO 并设置默认值
        DsSchedulerSaveReqDTO dto = new DsSchedulerSaveReqDTO();
        dto.setSchedule(String.format("{\"startTime\":\"%s\",\"endTime\":\"%s\",\"crontab\":\"%s\",\"timezoneId\":\"Asia/Shanghai\"}",
                startTime, endTime, crontab));
        dto.setProcessDefinitionCode(processDefinitionCode);
        dto.setFailureStrategy("CONTINUE");
        dto.setWorkerGroup(resolveWorkerGroup(projectWorkerGroup, null));
        dto.setTenantCode("default");

        return dto;
    }


    /**
     * 将 DsSchedulerRespDTO 转换为 CollectorEtlSchedulerSaveReqVO
     *
     * @param dsSchedulerRespDTO DsSchedulerRespDTO
     * @param CollectorEtlTaskDO
     * @return CollectorEtlSchedulerSaveReqVO
     */
    public static CollectorEtlSchedulerSaveReqVO convertToCollectorEtlSchedulerSaveReqVO(DsSchedulerRespDTO dsSchedulerRespDTO, CollectorEtlTaskDO CollectorEtlTaskDO) {
        // 创建 CollectorEtlSchedulerSaveReqVO 对象
        CollectorEtlSchedulerSaveReqVO reqVO = new CollectorEtlSchedulerSaveReqVO();

        // 从 dsSchedulerRespDTO 中提取数据并填充 reqVO
        Schedule schedule = dsSchedulerRespDTO.getData();

        reqVO.setStartTime(schedule.getStartTime());
        reqVO.setEndTime(schedule.getEndTime());
        reqVO.setTimezoneId(schedule.getTimezoneId());
        reqVO.setCronExpression(schedule.getCrontab());
        reqVO.setFailureStrategy("1");

        // 可以根据需要填写默认值或处理 dsId 和备注等字段
        reqVO.setDsId(schedule.getId()); // 假设 dsId 和 id 相同
        reqVO.setRemark(null); // 备注可以根据实际需求进行修改

        return reqVO;
    }


    /**
     * 工具方法，生成 DsSchedulerUpdateReqDTO。
     *
     * @param id                    调度ID
     * @param crontab               Cron 表达式
     * @param processDefinitionCode 任务编码
     * @return DsSchedulerUpdateReqDTO
     */
    public static DsSchedulerUpdateReqDTO createSchedulerUpdateRequest(Long id, String crontab, String processDefinitionCode) {
        return createSchedulerUpdateRequest(id, crontab, processDefinitionCode, null);
    }

    public static DsSchedulerUpdateReqDTO createSchedulerUpdateRequest(Long id, String crontab, String processDefinitionCode, String projectWorkerGroup) {
        // 获取当前时间
        String startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 获取100年后的时间
        long currentTime = System.currentTimeMillis();
        String endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(currentTime + 100L * 365 * 24 * 60 * 60 * 1000));

        // 创建 DsSchedulerUpdateReqDTO 并设置默认值
        DsSchedulerUpdateReqDTO dto = new DsSchedulerUpdateReqDTO();
        dto.setId(id); // 设置调度ID
        dto.setSchedule(String.format("{\"startTime\":\"%s\",\"endTime\":\"%s\",\"crontab\":\"%s\",\"timezoneId\":\"Asia/Shanghai\"}",
                startTime, endTime, crontab));
        dto.setProcessDefinitionCode(processDefinitionCode);
        dto.setFailureStrategy("CONTINUE");
        dto.setWorkerGroup(resolveWorkerGroup(projectWorkerGroup, null));
        dto.setTenantCode("default");

        return dto;
    }


    /**
     * 将 DsSchedulerSaveReqDTO 转换为 CollectorEtlSchedulerSaveReqVO
     *
     * @param CollectorEtlNewNodeSaveReqVO
     * @return CollectorEtlSchedulerSaveReqVO
     */
    public static CollectorEtlSchedulerSaveReqVO convertToCollectorEtlSchedulerSaveReqVO(Long taskId, String taskCode, CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        // 创建 CollectorEtlSchedulerSaveReqVO 对象
        CollectorEtlSchedulerSaveReqVO reqVO = new CollectorEtlSchedulerSaveReqVO();

        // 直接从 dsSchedulerSaveReqDTO 中填充字段
        reqVO.setTaskId(taskId);
        reqVO.setTaskCode(taskCode);

        // 获取当前时间
        String startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 获取100年后的时间
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime + 100L * 365 * 24 * 60 * 60 * 1000);

        reqVO.setStartTime(new Date());
        reqVO.setEndTime(date);
        reqVO.setTimezoneId("Asia/Shanghai"); // 默认时区

        reqVO.setCronExpression(CollectorEtlNewNodeSaveReqVO.getCrontab());
        reqVO.setFailureStrategy("1");
        reqVO.setStatus("0");


        // 填充dsId，假设dsId与ID相同
        reqVO.setDsId((long) -1);

        // 备注可以根据需求填写

        return reqVO;
    }

    // 方法解析 schedule 字段中的时间部分
    private static Date parseStartTime(String scheduleJson) {
        // 提取并解析开始时间 (假设你有方法从 JSON 解析出来)
        return new Date(); // 示例，实际需要提取出对应的时间
    }

    private static Date parseEndTime(String scheduleJson) {
        // 提取并解析结束时间 (假设你有方法从 JSON 解析出来)
        return new Date(); // 示例，实际需要提取出对应的时间
    }

    public static List<String> getPreAndPostNodeCodeList(List<CollectorEtlTaskNodeRelRespVO> CollectorEtlTaskNodeRelRespVOList) {
        List<String> result = new ArrayList<>();
        for (CollectorEtlTaskNodeRelRespVO vo : CollectorEtlTaskNodeRelRespVOList) {
            result.add(vo.getPreNodeCode());  // 添加 preNodeCode
            result.add(vo.getPostNodeCode()); // 添加 postNodeCode
        }
        return result;  // 返回 List<String>
    }


    public static DsStartTaskReqDTO createDsStartTaskReqDTO(String processDefinitionCode) {
        return createDsStartTaskReqDTO(processDefinitionCode, null);
    }

    public static DsStartTaskReqDTO createDsStartTaskReqDTO(String processDefinitionCode, String projectWorkerGroup) {
        // 获取当前日期，格式为 "yyyy-MM-dd"
        String currentDate = DateUtil.today();
        // 构造 scheduleTime 字段，固定格式 "yyyy-MM-dd 00:00:00"
        String scheduleTime = String.format("{\"complementStartDate\":\"%s 00:00:00\",\"complementEndDate\":\"%s 00:00:00\"}", currentDate, currentDate);

        // 使用 builder 模式创建 DsStartTaskReqDTO 对象，其他字段均为写死的值
        return DsStartTaskReqDTO.builder()
                .processDefinitionCode(JSONUtils.convertToLong(processDefinitionCode))
                .failureStrategy("CONTINUE")
                .warningType(DEFAULT_CONDITION_TYPE)
                .processInstancePriority(DEFAULT_TASK_PRIORITY)
                .workerGroup(resolveWorkerGroup(projectWorkerGroup, null))
                .scheduleTime(scheduleTime)
                .build();
    }


    /**
     * 构建etl坐标信息数据
     *
     * @param locations
     * @param code
     * @return
     */
    public static String buildEtlTaskLocationsJson(List<Map<String, Object>> locations, String code) {
        List<Map<String, Object>> locationList = new ArrayList<>();

        Map<String, Object> location = locations.get(0);
        Map<String, Object> locationMap = new HashMap<>();
        // 填充必要字段
        locationMap.put("taskCode", Long.parseLong(code)); // 默认 taskCode 为 0
        locationMap.put("x", location.getOrDefault("x", 0)); // 默认 x 为 0
        locationMap.put("y", location.getOrDefault("y", 0)); // 默认 y 为 0
        locationList.add(locationMap);
        return JSON.toJSONString(locationList);
    }

    public static String buildIncrementalFlinkxTaskLocationsJson(List<Map<String, Object>> locations,
                                                                 String prepareCode, String flinkxCode,
                                                                 String completeCode) {
        Map<String, Object> sourceLocation = locations == null || locations.isEmpty()
                ? new HashMap<>() : locations.get(0);
        double x = MapUtils.getDoubleValue(sourceLocation, "x", 0D);
        double y = MapUtils.getDoubleValue(sourceLocation, "y", 0D);
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(buildLocation(prepareCode, x - 220D, y));
        result.add(buildLocation(flinkxCode, x, y));
        result.add(buildLocation(completeCode, x + 220D, y));
        return JSON.toJSONString(result);
    }

    private static Map<String, Object> buildLocation(String code, double x, double y) {
        Map<String, Object> location = new HashMap<>();
        location.put("taskCode", Long.parseLong(code));
        location.put("x", x);
        location.put("y", y);
        return location;
    }

    /**
     * 构建etl节点关系json数据
     *
     * @param code
     * @return
     */
    public static String buildEtlTaskRelationJson(Long id, String code) {
        return buildEtlTaskRelationJson(id, code, 0);
    }

    public static String buildEtlTaskRelationJson(Long id, String code, Integer version) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> taskRelation = new HashMap<>();
        taskRelation.put("id", id);
        taskRelation.put("preTaskCode", 0);
        taskRelation.put("preTaskVersion", 0);
        taskRelation.put("postTaskCode", Long.parseLong(code));
        taskRelation.put("postTaskVersion", version == null ? 0 : version);
        taskRelation.put("conditionType", "NONE");
        result.add(taskRelation);
        return JSON.toJSONString(result);
    }

    public static String buildIncrementalFlinkxTaskRelationJson(Long prepareRelationId, Long flinkxRelationId,
                                                                Long completeRelationId, String prepareCode,
                                                                String flinkxCode, String completeCode) {
        return buildIncrementalFlinkxTaskRelationJson(
                prepareRelationId, flinkxRelationId, completeRelationId,
                prepareCode, 0, flinkxCode, 0, completeCode, 0);
    }

    public static String buildIncrementalFlinkxTaskRelationJson(Long prepareRelationId, Long flinkxRelationId,
                                                                Long completeRelationId,
                                                                String prepareCode, Integer prepareVersion,
                                                                String flinkxCode, Integer flinkxVersion,
                                                                String completeCode, Integer completeVersion) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(buildRelation(prepareRelationId, "0", 0, prepareCode, defaultVersion(prepareVersion)));
        result.add(buildRelation(flinkxRelationId, prepareCode, defaultVersion(prepareVersion),
                flinkxCode, defaultVersion(flinkxVersion)));
        result.add(buildRelation(completeRelationId, flinkxCode, defaultVersion(flinkxVersion),
                completeCode, defaultVersion(completeVersion)));
        return JSON.toJSONString(result);
    }

    private static int defaultVersion(Integer version) {
        return version == null ? 0 : version;
    }

    private static Map<String, Object> buildRelation(Long id, String preCode, int preVersion,
                                                     String postCode, int postVersion) {
        Map<String, Object> relation = new HashMap<>();
        relation.put("id", id);
        relation.put("preTaskCode", Long.parseLong(preCode));
        relation.put("preTaskVersion", preVersion);
        relation.put("postTaskCode", Long.parseLong(postCode));
        relation.put("postTaskVersion", postVersion);
        relation.put("conditionType", "NONE");
        return relation;
    }


    /**
     * 构建etl节点定义json数据
     *
     * @param id        etl节点id
     * @param name      etl节点美年广场
     * @param code      etl节点编码
     * @param version   etl节点版本
     * @param mainArgs  etl节点参数
     * @param draftJson
     * @return
     */
    public static String buildEtlTaskDefinitionJson(Long id, String name, String code, Integer version, Map<String, Object> mainArgs, String draftJson) {
        return buildEtlTaskDefinitionJson(id, name, code, version, mainArgs, draftJson, null);
    }

    public static String buildEtlTaskDefinitionJson(Long id, String name, String code, Integer version, Map<String, Object> mainArgs, String draftJson, String projectWorkerGroup) {
        List<Map<String, Object>> result = new ArrayList<>();
        //自定义参数
        Map<String, Object> definitionJsonMap = JSONUtils.convertTaskDefinitionJsonMap(draftJson);

        // 处理每个 task 的默认值和必要字段
        Map<String, Object> taskMap = new HashMap<>();

        // 设置基本信息字段
        taskMap.put("id", id); // 默认 id 为 0
        taskMap.put("name", name); // 默认空字符串
        taskMap.put("code", code); // 默认 code 为 0L
        taskMap.put("version", version); // 默认版本号为1
        taskMap.put("description", ""); // 默认描述为空
        taskMap.put("workerGroup", resolveWorkerGroup(projectWorkerGroup, definitionJsonMap.get("workerGroup"))); // 项目专属工作组优先
        taskMap.put("environmentCode", DEFAULT_ENVIRONMENT_CODE); // 默认环境编码
        taskMap.put("flag", DEFAULT_FLAG); // 默认 flag 为 "YES"
        taskMap.put("isCache", DEFAULT_IS_CACHE); // 默认 isCache 为 "NO"
        taskMap.put("taskPriority", MapUtils.getObject(definitionJsonMap,"taskPriority",DEFAULT_TASK_PRIORITY)); // 默认任务优先级为 "MEDIUM"
        taskMap.put("taskType", DEFAULT_TASK_TYPE); // 默认任务类型为 "SPARK"
        taskMap.put("taskExecuteType", "BATCH");

        //2025-06-25 新增配置项默认值
        taskMap.put("failRetryTimes", MapUtils.getObject(definitionJsonMap,"failRetryTimes",DEFAULT_TASK_failRetryTimes));
        taskMap.put("delayTime", MapUtils.getObject(definitionJsonMap,"delayTime",DEFAULT_TASK_delayTime));
        taskMap.put("failRetryInterval", MapUtils.getObject(definitionJsonMap,"failRetryInterval",DEFAULT_TASK_failRetryInterval));

        Map<String, Object> taskParams = new LinkedHashMap<>();

        taskParams.put("localParams", new ArrayList<>()); // 默认空列表
        taskParams.put("rawScript", ""); // 默认空字符串
        taskParams.put("resourceList", new ArrayList<>()); // 默认空列表
        taskParams.put("programType", DEFAULT_PROGRAM_TYPE); // 默认程序类型为 "JAVA"
        taskParams.put("mainClass", defaultMainClass);

        // mainJar是Map，且resourceName字段为默认值
        Map<String, Object> mainJar = new HashMap<>();
        mainJar.put("resourceName", resourceName);
        taskParams.put("mainJar", mainJar);
        taskParams.put("deployMode", DEFAULT_DEPLOY_MODE); // 默认部署模式为 "client"
        taskParams.put("mainArgs", Base64.encode(JSON.toJSONString(mainArgs))); // 默认空字符串
        taskParams.put("master", defaultMaster); // 默认Spark master URL
        taskParams.put("driverCores",MapUtils.getObject(definitionJsonMap,"driverCores",DEFAULT_DRIVER_CORES) ); // 默认驱动核心数
        taskParams.put("driverMemory",MapUtils.getObject(definitionJsonMap,"driverMemory",DEFAULT_DRIVER_MEMORY) ); // 默认驱动内存
        taskParams.put("numExecutors", MapUtils.getObject(definitionJsonMap,"numExecutors",DEFAULT_NUM_EXECUTORS)); // 默认执行器数量
        taskParams.put("executorMemory",MapUtils.getObject(definitionJsonMap,"executorMemory",DEFAULT_EXECUTOR_MEMORY) ); // 默认执行器内存
        taskParams.put("executorCores",MapUtils.getObject(definitionJsonMap,"executorCores",DEFAULT_EXECUTOR_CORES) ); // 默认执行器核心数
        taskParams.put("yarnQueue",MapUtils.getObject(definitionJsonMap,"yarnQueue","") ); // 默认执行器核心数
        taskParams.put("sqlExecutionType", DEFAULT_SQL_EXECUTION_TYPE); // 默认SQL执行类型为 "SCRIPT"

        // 将任务的taskParams加入到taskMap中
        taskMap.put("taskParams", taskParams);

        // 将填充好的任务加入到结果列表
        result.add(taskMap);
        // 返回处理后的JSON字符串
        return JSON.toJSONString(result);
    }

    /**
     * 判断是否使用 FlinkX 执行引擎
     * 前端存储引擎类型在 taskType 字段（值为 "SPARK" 或 "FLINK"）
     */
    public static boolean isFlinkxEngine(String draftJson) {
        if (StringUtils.isEmpty(draftJson)) {
            return false;
        }
        Map<String, Object> definitionJsonMap = JSONUtils.convertTaskDefinitionJsonMap(draftJson);
        String taskType = String.valueOf(definitionJsonMap.get("taskType"));
        return "FLINK".equalsIgnoreCase(taskType) || "FlinkX".equalsIgnoreCase(taskType);
    }

    /**
     * 构建 FlinkX 任务定义 JSON (DS CHUNJUN 原生类型)
     */
    public static String buildEtlTaskDefinitionJsonFlinkx(Long id, String name, String code, Integer version,
                                                          String flinkxJobJson, String draftJson) {
        return buildEtlTaskDefinitionJsonFlinkx(id, name, code, version, flinkxJobJson, draftJson, null);
    }

    public static String buildEtlTaskDefinitionJsonFlinkx(Long id, String name, String code, Integer version,
                                                          String flinkxJobJson, String draftJson, String projectWorkerGroup) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> definitionJsonMap = JSONUtils.convertTaskDefinitionJsonMap(draftJson);

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", id);
        taskMap.put("name", name);
        taskMap.put("code", code);
        taskMap.put("version", version);
        taskMap.put("description", "");
        taskMap.put("workerGroup", resolveWorkerGroup(projectWorkerGroup, definitionJsonMap.get("workerGroup")));
        taskMap.put("environmentCode", DEFAULT_ENVIRONMENT_CODE);
        taskMap.put("flag", DEFAULT_FLAG);
        taskMap.put("isCache", DEFAULT_IS_CACHE);
        taskMap.put("taskPriority", MapUtils.getObject(definitionJsonMap, "taskPriority", DEFAULT_TASK_PRIORITY));
        taskMap.put("taskType", "CHUNJUN");
        taskMap.put("taskExecuteType", "BATCH");
        taskMap.put("failRetryTimes", MapUtils.getObject(definitionJsonMap, "failRetryTimes", DEFAULT_TASK_failRetryTimes));
        taskMap.put("delayTime", MapUtils.getObject(definitionJsonMap, "delayTime", DEFAULT_TASK_delayTime));
        taskMap.put("failRetryInterval", MapUtils.getObject(definitionJsonMap, "failRetryInterval", DEFAULT_TASK_failRetryInterval));

        Map<String, Object> taskParams = new LinkedHashMap<>();
        taskParams.put("localParams", new ArrayList<>());
        taskParams.put("resourceList", new ArrayList<>());
        taskParams.put("customConfig", 1);
        taskParams.put("json", flinkxJobJson);
        taskParams.put("deployMode", MapUtils.getObject(definitionJsonMap, "deployMode", "local"));
        taskParams.put("others", MapUtils.getObject(definitionJsonMap, "others", ""));
        taskMap.put("taskParams", taskParams);

        result.add(taskMap);
        return JSON.toJSONString(result);
    }

    public static String buildIncrementalFlinkxTaskDefinitionJson(Long prepareId, String prepareName,
                                                                  String prepareCode, Integer prepareVersion,
                                                                  Long flinkxId, String flinkxName,
                                                                  String flinkxCode, Integer flinkxVersion,
                                                                  Long completeId, String completeName,
                                                                  String completeCode, Integer completeVersion,
                                                                  String prepareCallbackUrl, String completeCallbackUrl,
                                                                  String draftJson, String projectWorkerGroup) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(buildIncrementalPrepareHttpTask(prepareId, prepareName, prepareCode, prepareVersion,
                prepareCallbackUrl, draftJson, projectWorkerGroup));
        result.addAll(JSONUtils.convertTaskDefinitionJson(buildEtlTaskDefinitionJsonFlinkx(
                flinkxId, flinkxName, flinkxCode, flinkxVersion, buildHttpResponsePlaceholder(prepareName),
                draftJson, projectWorkerGroup)));
        result.add(buildIncrementalCompleteHttpTask(completeId, completeName, completeCode, completeVersion,
                completeCallbackUrl, draftJson, projectWorkerGroup));
        return JSON.toJSONString(result);
    }

    private static String buildHttpResponsePlaceholder(String taskName) {
        return "${" + taskName + ".response}";
    }

    private static Map<String, Object> buildIncrementalPrepareHttpTask(Long id, String name, String code,
                                                                       Integer version, String callbackUrl,
                                                                       String draftJson, String projectWorkerGroup) {
        return buildIncrementalHttpTask(id, name, code, version, callbackUrl, draftJson, projectWorkerGroup);
    }

    private static Map<String, Object> buildIncrementalCompleteHttpTask(Long id, String name, String code,
                                                                        Integer version, String callbackUrl,
                                                                        String draftJson, String projectWorkerGroup) {
        return buildIncrementalHttpTask(id, name, code, version, callbackUrl, draftJson, projectWorkerGroup);
    }

    private static Map<String, Object> buildIncrementalHttpTask(Long id, String name, String code,
                                                                Integer version, String callbackUrl,
                                                                String draftJson, String projectWorkerGroup) {
        Map<String, Object> definitionJsonMap = JSONUtils.convertTaskDefinitionJsonMap(draftJson);
        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("code", code);
        task.put("version", version == null ? 0 : version);
        task.put("description", "");
        task.put("workerGroup", resolveWorkerGroup(projectWorkerGroup, definitionJsonMap.get("workerGroup")));
        task.put("environmentCode", DEFAULT_ENVIRONMENT_CODE);
        task.put("flag", DEFAULT_FLAG);
        task.put("isCache", DEFAULT_IS_CACHE);
        task.put("taskPriority", MapUtils.getObject(definitionJsonMap, "taskPriority", DEFAULT_TASK_PRIORITY));
        task.put("taskType", "HTTP");
        task.put("taskExecuteType", "BATCH");
        task.put("failRetryTimes", MapUtils.getObject(definitionJsonMap, "failRetryTimes", DEFAULT_TASK_failRetryTimes));
        task.put("delayTime", MapUtils.getObject(definitionJsonMap, "delayTime", DEFAULT_TASK_delayTime));
        task.put("failRetryInterval", MapUtils.getObject(definitionJsonMap, "failRetryInterval", DEFAULT_TASK_failRetryInterval));

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("localParams", new ArrayList<>());
        params.put("resourceList", new ArrayList<>());
        params.put("httpMethod", "PUT");
        params.put("httpBody", "");
        params.put("httpCheckCondition", HTTP_CHECK_CONDITION);
        params.put("httpParams", new ArrayList<>());
        params.put("url", callbackUrl);
        params.put("condition", "");
        params.put("connectTimeout", HTTP_CONNECT_TIMEOUT);
        params.put("socketTimeout", HTTP_SOCKET_TIMEOUT);
        task.put("taskParams", params);
        return task;
    }

    @SuppressWarnings("unchecked")
    public static FlinkxIncrementalConfig resolveFlinkxIncrementalConfig(Map<String, Object> mainArgs) {
        Map<String, Object> reader = (Map<String, Object>) mainArgs.get("reader");
        Map<String, Object> writer = (Map<String, Object>) mainArgs.get("writer");
        if (reader == null || writer == null) {
            return null;
        }
        Map<String, Object> readerParam = (Map<String, Object>) reader.get("parameter");
        Map<String, Object> writerParam = (Map<String, Object>) writer.get("parameter");
        if (readerParam == null || writerParam == null) {
            return null;
        }

        String readModeType = String.valueOf(readerParam.get("readModeType"));
        String incrementalType;
        String sourceColumn;
        if ("2".equals(readModeType)) {
            incrementalType = FlinkxIncrementalConfig.TYPE_ID;
            sourceColumn = extractIdIncrementColumn(readerParam);
        } else if ("3".equals(readModeType)) {
            incrementalType = FlinkxIncrementalConfig.TYPE_TIME;
            sourceColumn = extractTimeIncrementColumn(readerParam);
        } else {
            return null;
        }

        List<String> sourceColumns = toColumnNames(readerParam.get("column"));
        List<String> targetColumns = toColumnNames(writerParam.get("target_column"));
        int sourceIndex = sourceColumns.indexOf(sourceColumn);
        if (sourceIndex < 0 || sourceIndex >= targetColumns.size()) {
            throw new ServiceException("增量字段 " + sourceColumn + " 未配置到目标表字段映射中");
        }

        return FlinkxIncrementalConfig.builder()
                .incrementalType(incrementalType)
                .sourceDatasourceId(toLong(readerParam.get("datasourceId"), "源数据源"))
                .targetDatasourceId(toLong(writerParam.get("datasourceId"), "目标数据源"))
                .sourceTableName(connectionTable(readerParam, "源表"))
                .targetTableName(connectionTable(writerParam, "目标表"))
                .sourceIncrementColumn(sourceColumn)
                .targetIncrementColumn(targetColumns.get(sourceIndex))
                .incrementalInitialValue(extractIncrementalInitialValue(readerParam, incrementalType))
                .incrementalTimeFormat(extractIncrementalTimeFormat(readerParam, incrementalType))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static String extractIncrementalTimeFormat(Map<String, Object> readerParam, String incrementalType) {
        if (!FlinkxIncrementalConfig.TYPE_TIME.equals(incrementalType)) {
            return null;
        }
        Map<String, Object> config = (Map<String, Object>) readerParam.get("dateIncrementConfig");
        String format = config == null ? null : String.valueOf(config.get("dateFormat"));
        if (StringUtils.isBlank(format) || "null".equals(format)) {
            return FlinkxIncrementalConfig.DEFAULT_TIME_FORMAT;
        }
        if (!SUPPORTED_INCREMENTAL_TIME_FORMATS.contains(format)) {
            throw new ServiceException("不支持的时间增量格式: " + format);
        }
        return format;
    }

    @SuppressWarnings("unchecked")
    private static String extractIncrementalInitialValue(Map<String, Object> readerParam, String incrementalType) {
        Object value;
        if (FlinkxIncrementalConfig.TYPE_ID.equals(incrementalType)) {
            Map<String, Object> config = (Map<String, Object>) readerParam.get("idIncrementConfig");
            value = config == null ? null : config.get("incrementStart");
        } else {
            Map<String, Object> config = (Map<String, Object>) readerParam.get("dateIncrementConfig");
            List<Map<String, Object>> columns = config == null ? null : (List<Map<String, Object>>) config.get("column");
            value = columns == null || columns.isEmpty() ? null : columns.get(0).get("cursorTime");
        }
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            throw new ServiceException("增量同步初始游标不能为空");
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static String extractIdIncrementColumn(Map<String, Object> readerParam) {
        Map<String, Object> config = (Map<String, Object>) readerParam.get("idIncrementConfig");
        String column = config == null ? null : String.valueOf(config.get("incrementColumn"));
        if (StringUtils.isBlank(column) || "null".equals(column)) {
            throw new ServiceException("ID增量字段不能为空");
        }
        return column;
    }

    @SuppressWarnings("unchecked")
    private static String extractTimeIncrementColumn(Map<String, Object> readerParam) {
        Map<String, Object> config = (Map<String, Object>) readerParam.get("dateIncrementConfig");
        List<Map<String, Object>> columns = config == null ? null : (List<Map<String, Object>>) config.get("column");
        Set<String> names = new LinkedHashSet<>();
        if (columns != null) {
            for (Map<String, Object> column : columns) {
                Object name = column.get("incrementColumn");
                if (name != null && StringUtils.isNotBlank(String.valueOf(name))) {
                    names.add(String.valueOf(name));
                }
            }
        }
        if (names.size() != 1) {
            throw new ServiceException("时间增量同步必须配置且只能配置一个增量字段");
        }
        return names.iterator().next();
    }

    @SuppressWarnings("unchecked")
    private static String connectionTable(Map<String, Object> parameter, String label) {
        Map<String, Object> connection = (Map<String, Object>) parameter.get("connection");
        Object table = connection == null ? null : connection.get("table");
        if (table instanceof List && !((List<?>) table).isEmpty()) {
            table = ((List<?>) table).get(0);
        }
        if (table == null || StringUtils.isBlank(String.valueOf(table))) {
            throw new ServiceException(label + "不能为空");
        }
        return String.valueOf(table);
    }

    @SuppressWarnings("unchecked")
    private static List<String> toColumnNames(Object rawColumns) {
        List<String> result = new ArrayList<>();
        if (!(rawColumns instanceof List)) {
            return result;
        }
        for (Object rawColumn : (List<Object>) rawColumns) {
            if (rawColumn instanceof Map) {
                Object name = ((Map<String, Object>) rawColumn).get("name");
                if (name != null) {
                    result.add(String.valueOf(name));
                }
            } else if (rawColumn != null) {
                result.add(String.valueOf(rawColumn));
            }
        }
        return result;
    }

    private static Long toLong(Object value, String label) {
        if (value == null) {
            throw new ServiceException(label + "不能为空");
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ServiceException(label + "不合法: " + value);
        }
    }

    public static List<CollectorEtlNodeSaveReqVO> convertToCollectorEtlNodeSaveReqVOList(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, String taskDefinitionJson) {
        List<CollectorEtlNodeSaveReqVO> resultList = new ArrayList<>();

        //取出入参数的信息
        List<CollectorEtlNodeSaveReqVO> list = JSON.parseArray(taskDefinitionJson, CollectorEtlNodeSaveReqVO.class);

        // 遍历 ProcessDefinition 中的 taskDefinitionList
        for (CollectorEtlNodeSaveReqVO createReqVO : list) {
            // 1. 任务相关信息
            createReqVO.setType(createReqVO.getTaskType());//节点类型
            createReqVO.setTaskType(CollectorEtlNewNodeSaveReqVO.getType());//任务类型
            createReqVO.setVersion(1); // 任务版本
            createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
            createReqVO.setProjectCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getProjectCode())); // 项目编码
            // 填充创建者和更新时间信息
            createReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
            createReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
            createReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
            createReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
            createReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
            createReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间
            createReqVO.setParameters(JSON.toJSONString(createReqVO.getTaskParams()));
            // 添加到结果列表
            resultList.add(createReqVO);
        }
        return resultList;
    }

    public static List<CollectorEtlNodeSaveReqVO> convertToCollectorEtlNodeSaveReqVOList(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, Integer nodeVersion) {
        List<CollectorEtlNodeSaveReqVO> resultList = new ArrayList<>();
        //取出入参数的信息
        List<CollectorEtlNodeSaveReqVO> list = JSON.parseArray(CollectorEtlNewNodeSaveReqVO.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);

        // 遍历 ProcessDefinition 中的 taskDefinitionList
        for (CollectorEtlNodeSaveReqVO createReqVO : list) {
            // 1. 任务相关信息
            createReqVO.setType(createReqVO.getTaskType());//节点类型
            createReqVO.setTaskType(CollectorEtlNewNodeSaveReqVO.getType());//任务类型
            createReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
            createReqVO.setProjectCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getProjectCode())); // 项目编码
            // 填充创建者和更新时间信息
            createReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
            createReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
            createReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
            createReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
            createReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
            createReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间
            createReqVO.setParameters(JSON.toJSONString(createReqVO.getTaskParams()));
            // 添加到结果列表
            resultList.add(createReqVO);
        }
        if (nodeVersion != null) {
            list.forEach(i -> i.setVersion(nodeVersion));
        }
        return resultList;
    }


    public static List<CollectorEtlNodeLogSaveReqVO> convertToCollectorEtlNodeLogSaveReqVOList(List<CollectorEtlNodeSaveReqVO> CollectorEtlNodeSaveReqVOList) {
        List<CollectorEtlNodeLogSaveReqVO> resultList = new ArrayList<>();
        for (CollectorEtlNodeSaveReqVO CollectorEtlNodeSaveReqVO : CollectorEtlNodeSaveReqVOList) {
            resultList.add(BeanUtils.toBean(CollectorEtlNodeSaveReqVO, CollectorEtlNodeLogSaveReqVO.class));
        }
        return resultList;
    }

    public static List<CollectorEtlTaskNodeRelSaveReqVO> convertToCollectorEtlTaskNodeRelSaveReqVOList(List<CollectorEtlNodeDO> CollectorEtlNodeBatch, CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO, CollectorEtlTaskSaveReqVO CollectorEtlTaskSaveReqVO) {
        List<CollectorEtlTaskNodeRelSaveReqVO> resultList = new ArrayList<>();
        List<ProcessTaskRelation> list = JSON.parseArray(CollectorEtlNewNodeSaveReqVO.getTaskRelationJson(), ProcessTaskRelation.class);
        // 遍历 data 下的 taskRelationList，生成 CollectorEtlTaskNodeRelSaveReqVO
        for (ProcessTaskRelation taskRelation : list) {

            CollectorEtlTaskNodeRelSaveReqVO taskNodeRelSaveReqVO = new CollectorEtlTaskNodeRelSaveReqVO();

            // 1. 填充任务节点关系相关字段
            taskNodeRelSaveReqVO.setProjectId(CollectorEtlNewNodeSaveReqVO.getProjectId()); // 项目ID
            taskNodeRelSaveReqVO.setProjectCode(String.valueOf(CollectorEtlNewNodeSaveReqVO.getProjectCode())); // 项目编码

            // 任务相关字段
            taskNodeRelSaveReqVO.setTaskId(CollectorEtlTaskSaveReqVO.getId()); // 任务ID
            taskNodeRelSaveReqVO.setTaskCode(CollectorEtlTaskSaveReqVO.getCode()); // 任务编码
            taskNodeRelSaveReqVO.setTaskVersion(CollectorEtlTaskSaveReqVO.getVersion()); // 任务版本

            // 前节点相关字段
            taskNodeRelSaveReqVO.setPreNodeCode(String.valueOf(taskRelation.getPreTaskCode())); // 前节点编码
            taskNodeRelSaveReqVO.setPreNodeVersion(taskRelation.getPreTaskVersion()); // 前节点版本
            if (StringUtils.isNotEmpty(taskNodeRelSaveReqVO.getPreNodeCode()) && taskNodeRelSaveReqVO.getPreNodeVersion() == 0) {
                taskNodeRelSaveReqVO.setPreNodeVersion(1);
            }
            taskNodeRelSaveReqVO.setPreNodeId(getIdByCode(CollectorEtlNodeBatch, taskNodeRelSaveReqVO.getPreNodeCode(), taskNodeRelSaveReqVO.getPreNodeVersion())); // 前节点ID

            // 后节点相关字段
            taskNodeRelSaveReqVO.setPostNodeCode(String.valueOf(taskRelation.getPostTaskCode())); // 后节点编码
            taskNodeRelSaveReqVO.setPostNodeVersion(taskRelation.getPostTaskVersion()); // 后节点版本
            if (StringUtils.isNotEmpty(taskNodeRelSaveReqVO.getPostNodeCode()) && taskNodeRelSaveReqVO.getPostNodeVersion() == 0) {
                taskNodeRelSaveReqVO.setPostNodeVersion(1);
            }
            taskNodeRelSaveReqVO.setPostNodeId(getIdByCode(CollectorEtlNodeBatch, taskNodeRelSaveReqVO.getPostNodeCode(), taskNodeRelSaveReqVO.getPostNodeVersion())); // 前节点ID

            // 可选字段
            taskNodeRelSaveReqVO.setRemark(null); // 备注

            // 2. 填充新增/修改相关信息
            taskNodeRelSaveReqVO.setCreatorId(CollectorEtlNewNodeSaveReqVO.getCreatorId()); // 假设项目ID为创建者ID（根据需求调整）
            taskNodeRelSaveReqVO.setCreateBy(CollectorEtlNewNodeSaveReqVO.getCreateBy()); // 假设任务名称为创建者（根据需求调整）
            taskNodeRelSaveReqVO.setCreateTime(CollectorEtlNewNodeSaveReqVO.getCreateTime()); // 设置当前时间为创建时间
            taskNodeRelSaveReqVO.setUpdatorId(CollectorEtlNewNodeSaveReqVO.getUpdatorId()); // 假设项目ID为更新者ID（根据需求调整）
            taskNodeRelSaveReqVO.setUpdateBy(CollectorEtlNewNodeSaveReqVO.getUpdateBy()); // 假设任务名称为更新者（根据需求调整）
            taskNodeRelSaveReqVO.setUpdateTime(CollectorEtlNewNodeSaveReqVO.getUpdateTime()); // 设置当前时间为更新时间

            // 添加到结果列表
            resultList.add(taskNodeRelSaveReqVO);
        }
        return resultList;
    }

    public static List<CollectorEtlTaskNodeRelLogSaveReqVO> convertToCollectorEtlTaskNodeRelLogSaveReqVOList(List<CollectorEtlTaskNodeRelSaveReqVO> CollectorEtlTaskNodeRelSaveReqVOS) {
        List<CollectorEtlTaskNodeRelLogSaveReqVO> resultList = new ArrayList<>();
        for (CollectorEtlTaskNodeRelSaveReqVO CollectorEtlNodeSaveReqVO : CollectorEtlTaskNodeRelSaveReqVOS) {
            resultList.add(BeanUtils.toBean(CollectorEtlNodeSaveReqVO, CollectorEtlTaskNodeRelLogSaveReqVO.class));
        }
        return resultList;
    }

    /**
     * 构建etl参数数据
     *
     * @return
     */
    public static Map<String, Object> buildEtlTaskParams(String taskDefinitionList, Map<String, CollectorEtlNodeSaveReqVO> nodeMap, Map<String, Object> taskInfo, List<DsResource> resourceList) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> transitionList = new ArrayList<>();
        List<CollectorEtlNodeSaveReqVO> nodeList = JSON.parseArray(taskDefinitionList, CollectorEtlNodeSaveReqVO.class);
        for (CollectorEtlNodeSaveReqVO CollectorEtlNodeSaveReqVO : nodeList) {
            Integer version = 1;
            if (nodeMap.containsKey(CollectorEtlNodeSaveReqVO.getCode())) {
                version = nodeMap.get(CollectorEtlNodeSaveReqVO.getCode()).getVersion();
            }
            //组件类型 本方法含有 DB_READER、EXCEL_READER、CSV_READER、SPARK_CLEAN、DB_WRITER
            String componentType = CollectorEtlNodeSaveReqVO.getComponentType();
            TaskComponentTypeEnum taskComponentTypeEnum = TaskComponentTypeEnum.findEnumByType(componentType);
            Map<String, Object> data = ComponentFactory.getComponentItem(componentType)
                    .parse2(CollectorEtlNodeSaveReqVO.getCode(), version, taskComponentTypeEnum, CollectorEtlNodeSaveReqVO.getTaskParams(), resourceUrl, resourceList);
            data.put("nodeName", CollectorEtlNodeSaveReqVO.getName());
            data.put("projectCode", taskInfo.get("projectCode"));
            switch (taskComponentTypeEnum) {
                case DB_READER:
                case EXCEL_READER:
                case CSV_READER:
                    result.put("reader", data);
                    break;
                case SPARK_CLEAN:
                case SORT_RECORD:
                case FIELD_DERIVATION:
                case DATA_DEDUPLICATION:
                case VALUE_MAP:
                case ADD_CONSTANT:
                case SELECT_FIELDS:
                    transitionList.add(data);
                    break;
                case DB_WRITER:
                    result.put("writer", data);
                    break;
            }
        }
        //配置config
        Map<String, Object> config = new HashMap<>();
        config.put("taskInfo", taskInfo);
        // EtlApplication.java 连接的 Redis 配置信息（用于获取最新数据源信息，保障任务执行）
        config.put("redis", dsRedisConfig);
        config.put("rabbitmq", rabbitmqConfig);
        config.put("resourceUrl", resourceUrl);
        result.put("transition", transitionList);
        result.put("config", config);
        return result;
    }
}
