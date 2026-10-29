package com.datamaster.module.ai.controller.agent.tool.metricdraft;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.runtime.AgentPayloadSanitizer;
import com.datamaster.module.ai.agent.runtime.AgentRunResponse;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** DataAgent-only confirmation protocol, deliberately separate from save_analysis. */
public final class MetricDraftConfirmation {
    public static final String TOOL = "save_metric_draft";
    private MetricDraftConfirmation() { }

    /** Only the DB operation's final tool result is eligible. Historical consumption wins over merges. */
    public static AgentToolResult pending(String status, String resultJson) {
        if (!AgentToolResult.NEED_CONFIRMATION.equals(status) || resultJson == null) return null;
        AgentRunResponse state = JSON.parseObject(resultJson, AgentRunResponse.class);
        List<AgentToolResult> results = state == null ? null : state.getToolResults();
        if (results == null || results.isEmpty()) return null;
        AgentToolResult last = results.get(results.size() - 1);
        if (last == null || !TOOL.equals(last.getToolName())
                || !TOOL.equals(last.getMetadata().get("toolName"))
                || !AgentToolResult.NEED_CONFIRMATION.equals(last.getStatus())
                || Boolean.TRUE.equals(last.getMetadata().get("confirmationConsumed"))) return null;
        Object hash = last.getMetadata().get("confirmationHash");
        if (!(hash instanceof String) || !((String) hash).matches("[0-9a-f]{64}")) return null;
        for (AgentToolResult previous : results) {
            if (previous != null && TOOL.equals(previous.getToolName())
                    && hash.equals(previous.getMetadata().get("confirmationHash"))
                    && Boolean.TRUE.equals(previous.getMetadata().get("confirmationConsumed"))) return null;
        }
        return last;
    }

    /** Runtime must replay these server-persisted arguments, not extract them from a confirmation message. */
    public static MetricDraftToolRequest.Save arguments(AgentToolResult pending) {
        Object value = pending == null ? null : pending.getMetadata().get("arguments");
        MetricDraftToolRequest.Save result = value == null ? null
                : JSON.parseObject(JSON.toJSONString(value), MetricDraftToolRequest.Save.class);
        if (result == null || result.getPreviewOperationId() == null
                || result.getPreviewIndex() == null || result.getPreviewIndex() < 0 || result.getPreviewIndex() > 4
                || result.getPreviewDigest() == null || !result.getPreviewDigest().matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("缺少有效的待确认预览参数");
        }
        return result;
    }

    /** Ordered canonical JSON with string IDs; no JVM bean/map iteration order dependency. */
    public static String canonicalJson(Object value) {
        return JSON.toJSONString(ordered(JSON.parse(JSON.toJSONString(value))));
    }

    private static Object ordered(Object value) {
        if (value instanceof Map) {
            Map<String, Object> result = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), ordered(entry.getValue()));
            }
            return result;
        }
        if (value instanceof Collection) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Collection<?>) value) result.add(ordered(item));
            return result;
        }
        return value;
    }

    public static String hash(Object definition) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(canonicalJson(definition).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b & 0xff));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static String text(String value, int max) {
        if (value == null) return null;
        if (value.length() > max) throw new IllegalArgumentException("业务说明长度超限");
        return String.valueOf(AgentPayloadSanitizer.sanitizeValue(null, value.trim()));
    }
}
