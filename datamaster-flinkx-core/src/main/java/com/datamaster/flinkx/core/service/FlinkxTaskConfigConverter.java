package com.datamaster.flinkx.core.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.flinkx.core.model.FlinkxJobBundle;
import com.datamaster.flinkx.core.model.FlinkxJobConfig;
import com.datamaster.flinkx.core.transform.TransformSqlBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlinkxTaskConfigConverter {

    public static FlinkxJobBundle convert(JSONObject taskParams) {
        JSONObject reader = taskParams.getJSONObject("reader");
        JSONObject writer = taskParams.getJSONObject("writer");
        JSONArray transitions = taskParams.getJSONArray("transition");

        Map<String, Object> flinkx = new LinkedHashMap<>();
        flinkx.put("job", buildJob(taskParams, reader, writer, transitions));
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

    private static Map<String, Object> buildJob(JSONObject taskParams, JSONObject reader, JSONObject writer, JSONArray transitions) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("content", buildContent(reader, writer, transitions));
        job.put("setting", buildSetting(taskParams));
        return job;
    }

    private static Map<String, Object> buildContent(JSONObject reader, JSONObject writer, JSONArray transitions) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reader", reader == null ? new LinkedHashMap<>() : reader.toJavaObject(Map.class));
        content.put("writer", writer == null ? new LinkedHashMap<>() : writer.toJavaObject(Map.class));
        if (transitions != null && !transitions.isEmpty()) {
            String transformSql = buildTransformSql(transitions, reader);
            if (transformSql != null) {
                Map<String, Object> transformer = new LinkedHashMap<>();
                transformer.put("transformSql", transformSql);
                content.put("transformer", transformer);
            }
        }
        return content;
    }

    private static String buildTransformSql(JSONArray transitions, JSONObject reader) {
        List<String> sourceColumns = extractColumns(reader);
        return TransformSqlBuilder.build(transitions, sourceColumns);
    }

    private static List<String> extractColumns(JSONObject reader) {
        List<String> cols = new ArrayList<>();
        if (reader == null) return cols;
        JSONObject param = reader.getJSONObject("parameter");
        if (param == null) return cols;
        Object column = param.get("column");
        if (column instanceof JSONArray) {
            JSONArray arr = (JSONArray) column;
            for (int i = 0; i < arr.size(); i++) {
                Object item = arr.get(i);
                if (item instanceof String) {
                    cols.add((String) item);
                } else if (item instanceof JSONObject) {
                    String name = ((JSONObject) item).getString("name");
                    if (name != null) cols.add(name);
                }
            }
        } else if (column instanceof String && org.apache.commons.lang3.StringUtils.isNotBlank((String) column)) {
            cols.add((String) column);
        }
        return cols;
    }

    private static Map<String, Object> buildSetting(JSONObject taskParams) {
        Map<String, Object> setting = new LinkedHashMap<>();
        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("channel", 1);
        setting.put("speed", speed);

        Map<String, Object> errorLimit = new LinkedHashMap<>();
        JSONObject settingConfig = nestedObject(taskParams, "setting");
        JSONObject errorLimitConfig = nestedObject(settingConfig, "errorLimit");
        JSONObject restoreConfig = nestedObject(settingConfig, "restore");
        JSONObject logConfig = nestedObject(settingConfig, "log");

        errorLimit.put("record", intValue(firstPresent(errorLimitConfig.get("record"), taskParams.get("errorLimitRecord")), 100));
        errorLimit.put("percentage", doubleValue(firstPresent(errorLimitConfig.get("percentage"), taskParams.get("errorLimitPercentage")), 0.1));
        setting.put("errorLimit", errorLimit);

        Map<String, Object> restore = new LinkedHashMap<>();
        restore.put("maxRowNumForCheckpoint", intValue(firstPresent(restoreConfig.get("maxRowNumForCheckpoint"), taskParams.get("maxRowNumForCheckpoint")), 10000));
        restore.put("isRestore", booleanValue(firstPresent(restoreConfig.get("isRestore"), taskParams.get("isRestore")), true));
        restore.put("restoreColumnName", stringValue(firstPresent(restoreConfig.get("restoreColumnName"), taskParams.get("restoreColumnName")), ""));
        restore.put("restoreColumnIndex", intValue(firstPresent(restoreConfig.get("restoreColumnIndex"), taskParams.get("restoreColumnIndex")), 0));
        setting.put("restore", restore);

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("isLogger", booleanValue(firstPresent(logConfig.get("isLogger"), taskParams.get("isLogger")), true));
        log.put("level", stringValue(firstPresent(logConfig.get("level"), taskParams.get("logLevel")), "info"));
        log.put("path", stringValue(firstPresent(logConfig.get("path"), taskParams.get("logPath")), ""));
        log.put("pattern", stringValue(firstPresent(logConfig.get("pattern"), taskParams.get("logPattern")), ""));
        setting.put("log", log);

        setting.put("resourceMode", taskParams.getString("resourceMode"));
        return setting;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject nestedObject(JSONObject object, String key) {
        if (object == null) {
            return new JSONObject();
        }
        Object value = object.get(key);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof Map) {
            return new JSONObject((Map<String, Object>) value);
        }
        return new JSONObject();
    }

    private static Object firstPresent(Object... values) {
        for (Object value : values) {
            if (value != null && !"".equals(String.valueOf(value).trim())) {
                return value;
            }
        }
        return null;
    }

    private static int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double doubleValue(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value);
        return "1".equals(text) || "yes".equalsIgnoreCase(text) || Boolean.parseBoolean(text);
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
