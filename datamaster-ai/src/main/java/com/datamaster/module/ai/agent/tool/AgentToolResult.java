package com.datamaster.module.ai.agent.tool;

import com.alibaba.fastjson2.JSON;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/** Tool 的稳定返回格式，避免 Agent 直接依赖业务模块内部 VO。 */
public class AgentToolResult {

    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String RUNNING = "RUNNING";
    public static final String NEED_INPUT = "NEED_INPUT";
    public static final String NEED_CONFIRMATION = "NEED_CONFIRMATION";
    public static final String FAILED = "FAILED";

    private boolean success;
    private String status;
    private Object data;
    private String message;
    private String operationId;
    private String toolName;
    private List<String> nextHints = new ArrayList<>();
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public static AgentToolResult of(String status, Object data, String message, String operationId) {
        AgentToolResult result = new AgentToolResult();
        result.success = SUCCEEDED.equals(status) || RUNNING.equals(status);
        result.status = status;
        result.data = normalizeIdValues(data);
        result.message = message;
        result.operationId = operationId;
        return result;
    }

    public static AgentToolResult failure(String message) {
        return of(FAILED, null, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = normalizeIdValues(data);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public List<String> getNextHints() {
        return nextHints == null ? Collections.emptyList() : nextHints;
    }

    public void setNextHints(List<String> nextHints) {
        this.nextHints = nextHints == null ? new ArrayList<>() : nextHints;
    }

    public Map<String, Object> getMetadata() {
        return metadata == null ? Collections.emptyMap() : metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : normalizeMap(metadata);
    }

    /**
     * Snowflake IDs must never be exposed to the model as JSON numbers. A model/provider
     * may parse a 19-digit value as IEEE-754 and change its last digits before passing it
     * to the next Tool (the datasource id 8017 became 8016 in production). Normalize all
     * id-shaped fields centrally so every Tool gets the same protection.
     */
    @SuppressWarnings("unchecked")
    private static Object normalizeIdValues(Object value) {
        if (value instanceof Map) return normalizeMap((Map<String, Object>) value);
        if (value instanceof Collection) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : (Collection<?>) value) normalized.add(normalizeIdValues(item));
            return normalized;
        }
        // A few legacy Tools return VO/DO objects directly. Convert those response
        // beans to a JSON map before exposing them to the model, so nested id fields
        // receive the same string normalization as map-based Tool responses.
        if (value != null && !(value instanceof CharSequence)
                && !(value instanceof Number) && !(value instanceof Boolean)) {
            try {
                Object jsonValue = JSON.parse(JSON.toJSONString(value));
                if (jsonValue != value) return normalizeIdValues(jsonValue);
            } catch (Exception ignored) {
                // Keep the original value if it is not JSON-serializable.
            }
        }
        return value;
    }

    private static Map<String, Object> normalizeMap(Map<String, Object> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object item = entry.getValue();
            String normalizedKey = key == null ? "" : key.replace("_", "").toLowerCase();
            if (key != null && ("id".equals(normalizedKey)
                    || normalizedKey.endsWith("id") || normalizedKey.endsWith("ids"))) {
                if (item instanceof Number) item = String.valueOf(item);
                else if (item instanceof Collection) {
                    List<String> ids = new ArrayList<>();
                    for (Object id : (Collection<?>) item) {
                        if (id == null) ids.add(null);
                        else ids.add(String.valueOf(id));
                    }
                    item = ids;
                }
            } else {
                item = normalizeIdValues(item);
            }
            normalized.put(key, item);
        }
        return normalized;
    }
}
