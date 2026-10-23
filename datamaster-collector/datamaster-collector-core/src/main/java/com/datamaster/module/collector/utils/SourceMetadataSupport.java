package com.datamaster.module.collector.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集任务来源元数据的统一约定。
 *
 * <p>该配置只描述字段和来源，不负责生成 SQL；SQL 生成由 FlinkX 转换层完成。</p>
 */
public final class SourceMetadataSupport {

    public static final String CONFIG = "sourceMetadata";
    public static final String ENABLED = "enabled";
    public static final String SOURCE_DATASOURCE = "source_datasource";
    public static final String SOURCE_DATABASE = "source_database";
    public static final String SOURCE_TABLE = "source_table";
    public static final String SOURCE_TASK = "source_task";
    public static final String SOURCE_BATCH = "source_batch";
    public static final String WRITE_TIME = "write_time";
    public static final String SOURCE_NATURAL_KEY = "source_natural_key";
    public static final String SOURCE_ROW_HASH = "source_row_hash";
    public static final String SOURCE_UPDATE_TIME = "source_update_time";
    public static final String RECORD_OP = "record_op";

    private SourceMetadataSupport() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> config(Map<String, Object> params) {
        if (params == null) {
            return Collections.emptyMap();
        }
        Object raw = params.get(CONFIG);
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        return Collections.emptyMap();
    }

    /** 历史草稿即使关闭来源元数据，发布时也必须生成来源表和写入时间。 */
    public static Map<String, Object> withRequiredMetadata(Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>(params);
        Map<String, Object> metadata = new LinkedHashMap<>(config(params));
        metadata.put(ENABLED, true);
        result.put(CONFIG, metadata);
        return result;
    }

    public static boolean enabled(Map<String, Object> params) {
        Map<String, Object> config = config(params);
        Object value = config.get(ENABLED);
        if (value == null) {
            value = params == null ? null : params.get("sourceMetadataEnabled");
        }
        return value != null && (Boolean.TRUE.equals(value)
                || "true".equalsIgnoreCase(String.valueOf(value))
                || "1".equals(String.valueOf(value)));
    }

    /** 下游逻辑节点引用上游输出时，保留已有的原始来源字段。 */
    public static boolean preserveExisting(Map<String, Object> params) {
        Object value = config(params).get("preserveExistingSource");
        return Boolean.TRUE.equals(value)
                || "true".equalsIgnoreCase(String.valueOf(value))
                || "1".equals(String.valueOf(value));
    }

    public static String column(Map<String, Object> params, String key, String defaultValue) {
        Object value = config(params).get(key);
        return value == null || String.valueOf(value).trim().isEmpty()
                ? defaultValue : String.valueOf(value).trim();
    }

    /**
     * 返回需要由转换层生成的输出列。source_table 在合表 SQL 已经生成时仍然会被去重。
     */
    public static List<String> metadataColumns(Map<String, Object> params) {
        if (!enabled(params)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        // 顺序与 Flink 转换 SQL 的追加投影一致，writer 按位置接收字段。
        if (optionalEnabled(params, "includeSourceDatasource")) {
            addIfAbsent(result, column(params, "sourceDatasourceColumn", SOURCE_DATASOURCE));
        }
        if (optionalEnabled(params, "includeSourceDatabase")) {
            addIfAbsent(result, column(params, "sourceDatabaseColumn", SOURCE_DATABASE));
        }
        addIfAbsent(result, column(params, "sourceTableColumn", SOURCE_TABLE));
        if (optionalEnabled(params, "includeSourceTask")) {
            addIfAbsent(result, column(params, "sourceTaskColumn", SOURCE_TASK));
        }
        if (optionalEnabled(params, "includeSourceBatch")) {
            addIfAbsent(result, column(params, "sourceBatchColumn", SOURCE_BATCH));
        }
        addIfAbsent(result, column(params, "writeTimeColumn", WRITE_TIME));
        if (optionalEnabled(params, "includeNaturalKey") && !naturalKeyColumns(params).isEmpty()) {
            addIfAbsent(result, column(params, "naturalKeyColumn", SOURCE_NATURAL_KEY));
        }
        if (optionalEnabled(params, "includeSourceUpdateTime")
                && hasConfiguredSourceColumn(params, "sourceUpdateTimeSourceColumn")) {
            addIfAbsent(result, column(params, "sourceUpdateTimeColumn", SOURCE_UPDATE_TIME));
        }
        if (optionalEnabled(params, "includeRecordOp")
                && hasConfiguredSourceColumn(params, "recordOpSourceColumn")) {
            addIfAbsent(result, column(params, "recordOpColumn", RECORD_OP));
        }
        if (optionalEnabled(params, "includeRowHash")) {
            addIfAbsent(result, column(params, "rowHashColumn", SOURCE_ROW_HASH));
        }
        // 连续分层节点只继承上游实际存在的治理列，避免把未落库的可选列
        // 强行加入下游 reader/writer。该列表由发布器根据上游配置生成。
        Object preserved = config(params).get("preservedMetadataColumns");
        if (preserved instanceof List) {
            for (Object item : (List<?>) preserved) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    addIfAbsent(result, String.valueOf(item).trim());
                }
            }
        }
        return result;
    }

    public static boolean optionalEnabled(Map<String, Object> params, String key) {
        Object value = config(params).get(key);
        if (value == null) {
            // 兼容已经配置 naturalKeyColumns 的旧草稿；其它可选字段默认关闭。
            return "includeNaturalKey".equals(key) && !naturalKeyColumns(params).isEmpty();
        }
        return Boolean.TRUE.equals(value)
                || "true".equalsIgnoreCase(String.valueOf(value))
                || "1".equals(String.valueOf(value));
    }

    public static boolean hasConfiguredSourceColumn(Map<String, Object> params, String key) {
        Object value = config(params).get(key);
        return value != null && !String.valueOf(value).trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static List<String> naturalKeyColumns(Map<String, Object> params) {
        Map<String, Object> config = config(params);
        Object raw = config.get("naturalKeyColumns");
        if (!(raw instanceof List)) {
            raw = config.get("sourceKeyColumns");
        }
        List<String> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object item : (List<Object>) raw) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    result.add(String.valueOf(item).trim());
                }
            }
        } else if (raw != null && !String.valueOf(raw).trim().isEmpty()) {
            for (String item : String.valueOf(raw).split(",")) {
                if (!item.trim().isEmpty()) result.add(item.trim());
            }
        }
        if (!result.isEmpty()) {
            return result;
        }

        Object fields = params == null ? null : params.get("tableFields");
        if (fields instanceof List) {
            for (Object item : (List<Object>) fields) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> field = (Map<String, Object>) item;
                Object pk = field.get("pkFlag");
                if (Boolean.TRUE.equals(pk) || "1".equals(String.valueOf(pk))
                        || "true".equalsIgnoreCase(String.valueOf(pk))) {
                    Object name = field.get("columnName");
                    if (name == null) {
                        name = field.get("name");
                    }
                    if (name != null && !String.valueOf(name).trim().isEmpty()) {
                        result.add(String.valueOf(name).trim());
                    }
                }
            }
        }
        return result;
    }

    /** 将来源列追加到 ChunJun reader/writer 的字段列表，避免重复追加。 */
    @SuppressWarnings("unchecked")
    public static List<Object> appendColumns(Object rawColumns, List<String> extraColumns) {
        List<Object> result = new ArrayList<>();
        if (rawColumns instanceof List) {
            result.addAll((List<Object>) rawColumns);
        } else if (rawColumns != null && !String.valueOf(rawColumns).trim().isEmpty()) {
            // 兼容旧草稿把 columns 保存为逗号分隔字符串；不能把整串当成一个列名。
            for (String item : String.valueOf(rawColumns).split(",")) {
                if (!item.trim().isEmpty()) {
                    result.add(item.trim());
                }
            }
        }
        for (String extra : extraColumns) {
            boolean exists = false;
            for (Object item : result) {
                String name;
                if (item instanceof Map) {
                    Map<?, ?> field = (Map<?, ?>) item;
                    Object rawName = field.get("name");
                    if (rawName == null) rawName = field.get("columnName");
                    if (rawName == null) rawName = field.get("key");
                    name = String.valueOf(rawName);
                } else {
                    name = String.valueOf(item);
                }
                if (extra.equalsIgnoreCase(name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(extra);
            }
        }
        return result;
    }

    public static Map<String, Object> copyWithRuntimeValues(Map<String, Object> params,
                                                              String nodeCode,
                                                              String datasourceId,
                                                              String tableName) {
        Map<String, Object> result = new LinkedHashMap<>(config(params));
        result.put(ENABLED, true);
        result.putIfAbsent("sourceDatasourceValue", datasourceId);
        result.putIfAbsent("sourceTaskValue", nodeCode);
        result.putIfAbsent("sourceTableValue", tableName);
        if (!result.containsKey("naturalKeyColumns")) {
            List<String> inferred = naturalKeyColumns(params);
            if (!inferred.isEmpty()) {
                result.put("naturalKeyColumns", inferred);
            }
        }
        return result;
    }

    private static void addIfAbsent(List<String> values, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        for (String item : values) {
            if (item.equalsIgnoreCase(value)) {
                return;
            }
        }
        values.add(value);
    }
}
