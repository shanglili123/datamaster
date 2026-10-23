package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 将数据库字段元数据映射为 ChunJun/Flink 列类型，同时保留字段顺序。 */
public final class FieldSchemaSupport {
    private static final Pattern DECIMAL = Pattern.compile("(?:DECIMAL|NUMERIC|NUMBER)\\s*\\((\\d+)\\s*(?:,\\s*(-?\\d+)\\s*)?\\)");
    private FieldSchemaSupport() { }

    public static List<JSONObject> columns(Object selected, Object details, Object metadata) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        if (details instanceof List) {
            for (Object raw : (List<?>) details) {
                if (raw instanceof Map) {
                    Map<String, Object> field = (Map<String, Object>) raw;
                    String name = name(field);
                    if (!name.isEmpty()) byName.put(name, field);
                }
            }
        }
        List<?> values = selected instanceof List ? (List<?>) selected
                : selected instanceof String ? Arrays.asList(((String) selected).split(",")) : Collections.emptyList();
        List<JSONObject> result = new ArrayList<>();
        for (Object raw : values) {
            String name = raw instanceof Map ? name((Map<?, ?>) raw) : text(raw);
            if (name.isEmpty()) continue;
            Map<String, Object> field = new LinkedHashMap<>();
            Map<String, Object> detail = byName.get(name);
            if (detail == null) {
                for (Map.Entry<String, Map<String, Object>> entry : byName.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(name)) { detail = entry.getValue(); break; }
                }
            }
            if (detail != null) field.putAll(detail);
            if (raw instanceof Map) field.putAll((Map<String, Object>) raw);
            JSONObject column = new JSONObject();
            column.put("name", name);
            Object type = first(field, "type", "columnType", "dataType", "resultType");
            if (type == null && isMetadata(metadata, name, "writeTimeColumn", "write_time")) type = "TIMESTAMP";
            if (type == null && isMetadata(metadata, name, "sourceUpdateTimeColumn", "source_update_time")) type = "TIMESTAMP";
            column.put("type", flinkType(type, field));
            for (String key : Arrays.asList("index", "value", "format", "parseFormat", "splitter", "isPart", "isKey")) {
                if (field.get(key) != null) column.put(key, field.get(key));
            }
            Object nullable = first(field, "nullable", "nullableFlag");
            if (nullable != null) column.put("nullable", Boolean.TRUE.equals(nullable) || "1".equals(String.valueOf(nullable)) || "true".equalsIgnoreCase(String.valueOf(nullable)));
            Object length = first(field, "length", "columnLength");
            if (length != null) column.put("length", length);
            result.add(column);
        }
        return result;
    }

    public static String flinkType(Object raw, Map<String, Object> field) {
        String type = text(raw).toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (type.isEmpty() || "STRING".equals(type) || "TEXT".equals(type) || "CLOB".equals(type)
                || type.contains("CHAR") || "UUID".equals(type) || "JSON".equals(type) || "JSONB".equals(type)) return "STRING";
        Matcher decimal = DECIMAL.matcher(type);
        if (decimal.matches() || Arrays.asList("NUMERIC", "DECIMAL", "NUMBER", "BIGNUMBER").contains(type)) {
            int precision, scale;
            if (decimal.matches()) {
                precision = Integer.parseInt(decimal.group(1)); scale = decimal.group(2) == null ? 0 : Integer.parseInt(decimal.group(2));
            } else {
                precision = integer(first(field, "precision", "columnLength"), 38);
                scale = integer(first(field, "scale", "columnScale"), 0);
            }
            if (precision < 1 || precision > 38 || scale < 0 || scale > precision) {
                throw new IllegalArgumentException("字段 DECIMAL 精度超出 Flink 支持范围：" + precision + "," + scale);
            }
            return "DECIMAL(" + precision + "," + scale + ")";
        }
        if (Arrays.asList("BIGINT", "INT8", "LONG", "BIGSERIAL").contains(type)) return "BIGINT";
        if (Arrays.asList("INT", "INTEGER", "INT4", "SERIAL", "MEDIUMINT").contains(type)) return "INT";
        if (Arrays.asList("SMALLINT", "INT2", "SHORT").contains(type)) return "SMALLINT";
        if (Arrays.asList("TINYINT", "BYTE").contains(type)) return "TINYINT";
        if (Arrays.asList("DOUBLE", "DOUBLE PRECISION", "FLOAT8", "BINARY_DOUBLE").contains(type)) return "DOUBLE";
        if (Arrays.asList("FLOAT", "REAL", "FLOAT4", "BINARY_FLOAT").contains(type)) return "FLOAT";
        if (Arrays.asList("BOOL", "BOOLEAN", "BIT").contains(type)) return "BOOLEAN";
        if ("DATE".equals(type)) return "DATE";
        if (type.startsWith("TIMESTAMP_LTZ") || "TIMESTAMPTZ".equals(type) || type.contains("WITH TIME ZONE")) {
            throw new IllegalArgumentException("带时区字段需要显式规范为本地 TIMESTAMP 后读取，不能静默丢失时区：" + type);
        }
        if (type.startsWith("TIMESTAMP") || Arrays.asList("DATETIME", "DATETIME2", "SMALLDATETIME").contains(type)) return "TIMESTAMP";
        if (type.startsWith("TIME")) return "TIME";
        if (Arrays.asList("BYTES", "BYTEA", "BLOB", "BINARY", "VARBINARY", "LONGVARBINARY").contains(type)) return "BYTES";
        throw new IllegalArgumentException("未支持的字段类型：" + type + "，请在源 SQL 显式转换或配置类型映射");
    }

    public static String name(Map<?, ?> field) {
        for (String key : Arrays.asList("name", "columnName", "key")) if (!text(field.get(key)).isEmpty()) return text(field.get(key));
        return "";
    }
    private static boolean isMetadata(Object raw, String name, String key, String fallback) {
        Map<?, ?> metadata = raw instanceof Map ? (Map<?, ?>) raw : Collections.emptyMap();
        String column = text(metadata.get(key)); return (column.isEmpty() ? fallback : column).equalsIgnoreCase(name);
    }
    private static Object first(Map<String, Object> values, String... keys) {
        for (String key : keys) if (!text(values.get(key)).isEmpty()) return values.get(key);
        return null;
    }
    private static int integer(Object value, int fallback) { return text(value).isEmpty() ? fallback : Integer.parseInt(text(value)); }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}
