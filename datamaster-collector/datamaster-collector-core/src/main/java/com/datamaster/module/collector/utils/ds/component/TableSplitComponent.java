package com.datamaster.module.collector.utils.ds.component;

import org.apache.commons.collections4.MapUtils;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.model.DsResource;

import java.util.*;

/**
 * <P>
 * 用途:拆表组件(一个输入流拆分为多个目标表输出)
 * </p>
 *
 * <p>
 * 拆表是专用输出节点:一个拆表节点对应多个目标表,运行时展开为多个 DB_WRITER。
 * taskParams 结构:
 * {
 *   "targetTables": [              // 目标表列表,每个元素与 DB_WRITER 的 taskParams 同构
 *     {
 *       "batchSize": 1024,
 *       "preSql": "",
 *       "postSql": "",
 *       "target_datasource_id": 1,
 *       "target_table_name": "T1",
 *       "target_columns": ["ID","NAME"],
 *       "writeModeType": 1,
 *       "selectedColumns": ["ID"],
 *       "writerDatasource": { "datasourceId": 1, ... }
 *     }
 *   ]
 * }
 * </p>
 *
 * <p>
 * parse2 输出(归一化):{ nodeCode, nodeVersion, componentType, parameter: { targetTables: [...] } }
 * TaskConverter 的 switch case TABLE_SPLIT 遍历 parameter.targetTables,
 * 每个元素调用 DBWriterComponent.parse2 展开为一个 writer 加入 writerList。
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-12 16:31
 **/
public class TableSplitComponent implements ComponentItem {
    @Override
    public Map<String, Object> parse(Map<String, Object> params) {
        Map<String, Object> taskParams = new LinkedHashMap<>();

        taskParams.put("localParams", params.getOrDefault("localParams", new ArrayList<>())); // 默认空列表
        taskParams.put("rawScript", params.getOrDefault("rawScript", "")); // 默认空字符串
        taskParams.put("resourceList", params.getOrDefault("resourceList", new ArrayList<>())); // 默认空列表
        taskParams.put("programType", params.getOrDefault("programType", DEFAULT_PROGRAM_TYPE)); // 默认程序类型为 "JAVA"
        taskParams.put("mainClass", params.get("mainClass")); // 默认主类

        // mainJar是Map，且resourceName字段为默认值
        Map<String, Object> mainJar = new HashMap<>();
        mainJar.put("resourceName", params.get("resourceName"));
        taskParams.put("mainJar", mainJar);

        taskParams.put("deployMode", params.getOrDefault("deployMode", DEFAULT_DEPLOY_MODE)); // 默认部署模式为 "client"
        taskParams.put("mainArgs", params.getOrDefault("mainArgs", new HashMap<>())); // 默认空Map
        taskParams.put("driverCores", params.getOrDefault("driverCores", DEFAULT_DRIVER_CORES)); // 默认驱动核心数
        taskParams.put("driverMemory", params.getOrDefault("driverMemory", DEFAULT_DRIVER_MEMORY)); // 默认驱动内存
        taskParams.put("numExecutors", params.getOrDefault("numExecutors", DEFAULT_NUM_EXECUTORS)); // 默认执行器数量
        taskParams.put("executorMemory", params.getOrDefault("executorMemory", DEFAULT_EXECUTOR_MEMORY)); // 默认执行器内存
        taskParams.put("executorCores", params.getOrDefault("executorCores", DEFAULT_EXECUTOR_CORES)); // 默认执行器核心数
        taskParams.put("sqlExecutionType", params.getOrDefault("sqlExecutionType", DEFAULT_SQL_EXECUTION_TYPE)); // 默认SQL执行类型为 "SCRIPT"
        return taskParams;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.TABLE_SPLIT.getCode();
    }

    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion, TaskComponentTypeEnum componentType, Map<String, Object> taskParams, String resourceUrl, List<DsResource> resourceList) {
        Map<String, Object> node = mapNode(nodeCode, nodeVersion, componentType);

        Map<String, Object> parameter = new HashMap<>();
        // 归一化目标表列表:每个元素透传(与 DB_WRITER 的 taskParams 同构)
        parameter.put("targetTables", taskParams.getOrDefault("targetTables", new ArrayList<>()));
        node.put("parameter", parameter);
        return node;
    }
}
