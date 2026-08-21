package com.datamaster.module.collector.utils.ds.component;

import org.apache.commons.collections4.MapUtils;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.model.DsResource;

import java.util.*;

/**
 * <P>
 * 用途:字段合并转换组件
 * </p>
 *
 * <p>
 * taskParams 结构:
 * {
 *   "mergeFieldName": "FULL_NAME",   // 合并后的输出字段名
 *   "delimiter": " ",                // 合并分隔符(可空，为空时直接拼接)
 *   "handleNull": "1",               // 空值处理: 1忽略(跳过NULL) 2保留(输出空串) 默认1
 *   "trimSpace": "1",                // 是否去除字段首尾空格: 1是 2否 默认1
 *   "tableFields": [                 // 参与合并的源字段列表
 *     { "columnName": "FIRST_NAME" },
 *     { "columnName": "LAST_NAME" }
 *   ]
 * }
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-12 16:31
 **/
public class FieldMergeTransitionComponent implements ComponentItem {
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
        return TaskComponentTypeEnum.FIELD_MERGE.getCode();
    }

    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion, TaskComponentTypeEnum componentType, Map<String, Object> taskParams, String resourceUrl, List<DsResource> resourceList) {
        Map<String, Object> node = mapNode(nodeCode, nodeVersion, componentType);

        Map<String, Object> parameter = new HashMap<>();
        parameter.put("mergeFieldName", MapUtils.getString(taskParams, "mergeFieldName"));
        parameter.put("delimiter", MapUtils.getString(taskParams, "delimiter"));
        parameter.put("handleNull", MapUtils.getString(taskParams, "handleNull", "1"));
        parameter.put("trimSpace", MapUtils.getString(taskParams, "trimSpace", "1"));
        parameter.put("tableFields", taskParams.getOrDefault("tableFields", new ArrayList<>()));
        node.put("parameter", parameter);
        return node;
    }
}
