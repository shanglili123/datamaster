package com.datamaster.ingestion.service;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.ingestion.config.IngestionProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Kafka 接入消息和动态目标表的基础 Schema 校验。 */
@Service
public class IngestionSchemaValidator {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");

    @Resource
    private IngestionProperties properties;

    /** 返回 null 表示校验通过，否则返回可直接写入接入日志的错误原因。 */
    public String validate(JSONObject record, String targetTable) {
        if (record == null) {
            return "消息体不能为空";
        }
        if (targetTable == null || !IDENTIFIER.matcher(targetTable).matches()) {
            return "目标表名不合法，只允许字母、数字、下划线、$，且必须以字母或下划线开头: " + targetTable;
        }
        String tableError = validateAllowedTable(targetTable);
        if (tableError != null) {
            return tableError;
        }
        String keyError = validateKeyColumns(record);
        if (keyError != null) {
            return keyError;
        }
        String version = properties.getSchemaVersion();
        if (version != null && !version.trim().isEmpty()
                && !version.trim().equals(String.valueOf(record.get("__schemaVersion")))) {
            return "消息 Schema 版本不匹配，期望 " + version + ", 实际 " + record.get("__schemaVersion");
        }
        Set<String> columns = new HashSet<>();
        for (String key : record.keySet()) {
            if (key == null || key.startsWith("__")) {
                continue;
            }
            if (!IDENTIFIER.matcher(key).matches()) {
                return "消息字段名不合法: " + key;
            }
            if (!columns.add(key.toLowerCase(Locale.ROOT))) {
                return "消息包含大小写不同但名称相同的重复字段: " + key;
            }
        }
        boolean delete = isDelete(record);
        if (!delete) {
            List<String> required = properties.getRequiredColumns();
            if (required != null) {
                for (String column : required) {
                    if (column == null || !IDENTIFIER.matcher(column).matches()) {
                        return "required-columns 配置包含非法字段名: " + column;
                    }
                    if (!columns.contains(column.toLowerCase(Locale.ROOT)) || valueForColumn(record, column) == null) {
                        return "消息缺少必填字段: " + column;
                    }
                }
            }
        }
        return null;
    }

    private String validateAllowedTable(String targetTable) {
        List<String> allowed = properties.getAllowedTables();
        if (allowed == null || allowed.isEmpty()) {
            return null;
        }
        for (String item : allowed) {
            if (item != null && targetTable.equalsIgnoreCase(item.trim())) {
                return null;
            }
        }
        return "目标表不在 ingestion allowed-tables 白名单中: " + targetTable;
    }

    private String validateKeyColumns(JSONObject record) {
        Object raw = record.get("__keys");
        if (raw != null) {
            String value = String.valueOf(raw).trim();
            if (value.startsWith("[")) {
                try {
                    com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSON.parseArray(value);
                    for (int i = 0; i < array.size(); i++) {
                        String error = validateIdentifier(String.valueOf(array.getString(i)), "__keys");
                        if (error != null) return error;
                    }
                    return null;
                } catch (Exception e) {
                    return "消息 __keys 不是合法 JSON 数组";
                }
            }
            for (String item : value.split(",")) {
                String error = validateIdentifier(item.trim(), "__keys");
                if (error != null) return error;
            }
            return null;
        }
        List<String> configured = properties.getKeyColumns();
        if (configured == null || configured.isEmpty()) {
            return "未配置冲突判定主键列，请设置 key-columns 或在消息中提供 __keys";
        }
        if (configured != null) {
            for (String column : configured) {
                String error = validateIdentifier(column, "key-columns");
                if (error != null) return error;
            }
        }
        return null;
    }

    private String validateIdentifier(String value, String source) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            return source + " 包含非法字段名: " + value;
        }
        return null;
    }

    private Object valueForColumn(JSONObject record, String column) {
        Object direct = record.get(column);
        if (direct != null) {
            return direct;
        }
        for (String key : record.keySet()) {
            if (key != null && key.equalsIgnoreCase(column)) {
                return record.get(key);
            }
        }
        return null;
    }

    private boolean isDelete(JSONObject record) {
        String raw = record.getString("__op");
        if (raw == null || raw.trim().isEmpty()) raw = record.getString("__operation");
        if (raw == null || raw.trim().isEmpty()) raw = record.getString("__eventType");
        if (raw == null) return false;
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return "D".equals(value) || "DELETE".equals(value) || "REMOVE".equals(value);
    }
}
