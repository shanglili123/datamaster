package com.datamaster.flinkx.core.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.flinkx.core.model.FlinkxJobBundle;
import com.datamaster.flinkx.core.model.FlinkxJobConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlinkxTaskConfigConverter {

    public static FlinkxJobBundle convert(JSONObject taskParams) {
        JSONObject reader = taskParams.getJSONObject("reader");
        JSONObject writer = taskParams.getJSONObject("writer");
        JSONArray transitions = taskParams.getJSONArray("transition");

        Map<String, Object> flinkx = new LinkedHashMap<>();
        flinkx.put("job", buildJob(taskParams));
        flinkx.put("content", buildContent(reader, writer, transitions));
        flinkx.put("setting", buildSetting(taskParams));

        FlinkxJobConfig jobConfig = FlinkxJobConfig.builder()
                .jobName(taskParams.getString("name"))
                .jobType("FlinkX")
                .mode("standalone")
                .schedulerName("dolphinscheduler")
                .flinkx(flinkx)
                .build();

        return FlinkxJobBundle.builder()
                .jobConfig(jobConfig)
                .reader(reader == null ? null : reader.toJavaObject(Map.class))
                .writer(writer == null ? null : writer.toJavaObject(Map.class))
                .transitions(transitions)
                .build();
    }

    private static Map<String, Object> buildJob(JSONObject taskParams) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("content", buildContent(taskParams.getJSONObject("reader"), taskParams.getJSONObject("writer"), taskParams.getJSONArray("transition")));
        job.put("setting", buildSetting(taskParams));
        return job;
    }

    private static Map<String, Object> buildContent(JSONObject reader, JSONObject writer, JSONArray transitions) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reader", reader == null ? new LinkedHashMap<>() : reader.toJavaObject(Map.class));
        content.put("writer", writer == null ? new LinkedHashMap<>() : writer.toJavaObject(Map.class));
        if (transitions != null && !transitions.isEmpty()) {
            List<Map<String, Object>> transformerList = new java.util.ArrayList<>();
            for (int i = 0; i < transitions.size(); i++) {
                JSONObject trans = transitions.getJSONObject(i);
                Map<String, Object> tf = new LinkedHashMap<>();
                String componentType = trans.getString("componentType");
                if (componentType != null) {
                    tf.put("name", resolveTransformerName(componentType));
                }
                JSONObject param = trans.getJSONObject("parameter");
                tf.put("parameter", param == null ? new LinkedHashMap<>() : param.toJavaObject(Map.class));
                transformerList.add(tf);
            }
            content.put("transformer", transformerList);
        }
        return content;
    }

    private static String resolveTransformerName(String componentType) {
        switch (componentType) {
            case "VALUE_MAP":             return "dx_valuemap";
            case "FIELD_DERIVATION":      return "dx_fieldderivation";
            case "DATA_DEDUPLICATION":    return "dx_deduplication";
            case "SELECT_FIELDS":         return "dx_selectfields";
            case "ADD_CONSTANT":          return "dx_addconstant";
            case "SORT_RECORD":           return "dx_sort";
            case "SPARK_CLEAN":           return "dx_clean";
            default:                      return "dx_" + componentType.toLowerCase();
        }
    }

    private static Map<String, Object> buildSetting(JSONObject taskParams) {
        Map<String, Object> setting = new LinkedHashMap<>();
        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("channel", 1);
        setting.put("speed", speed);
        
        Map<String, Object> errorLimit = new LinkedHashMap<>();
        errorLimit.put("record", 0);
        errorLimit.put("percentage", 0.0);
        setting.put("errorLimit", errorLimit);
        
        setting.put("resourceMode", taskParams.getString("resourceMode"));
        return setting;
    }
}
