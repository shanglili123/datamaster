package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** 对 Agent Tool 参数/结果做递归脱敏，避免连接凭据进入模型上下文和审计表。 */
public final class AgentPayloadSanitizer {

    private static final Pattern INLINE_SECRET = Pattern.compile(
            "(?i)(password|passwd|pwd|api[_-]?key|token|secret)\\s*[:=]\\s*(\\\"[^\\\"]*\\\"|'[^']*'|[^,;\\s]+)");

    private AgentPayloadSanitizer() {
    }

    public static String sanitizeJson(Object value) {
        try {
            Object tree = JSON.parse(JSON.toJSONString(value));
            return JSON.toJSONString(sanitizeValue(null, tree));
        } catch (Exception ignored) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    public static Object sanitizeValue(String key, Object value) {
        if (isSensitiveKey(key)) return "***";
        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), sanitizeValue(String.valueOf(entry.getKey()), entry.getValue()));
            }
            return result;
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<Object>) value) result.add(sanitizeValue(null, item));
            return result;
        }
        if (value instanceof String) {
            String text = (String) value;
            if (looksLikeJson(text)) {
                try {
                    Object parsed = JSON.parse(text);
                    return JSON.toJSONString(sanitizeValue(key, parsed));
                } catch (Exception ignored) {
                    // 按普通文本继续处理。
                }
            }
            return INLINE_SECRET.matcher(text).replaceAll("$1=***");
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String normalized = key.toLowerCase().replace("_", "").replace("-", "");
        return normalized.contains("password") || normalized.contains("passwd")
                || normalized.equals("pwd") || normalized.contains("apikey")
                || normalized.contains("token") || normalized.contains("secret")
                || normalized.contains("privatekey") || normalized.contains("credential")
                || normalized.equals("authorization");
    }

    private static boolean looksLikeJson(String text) {
        String value = text == null ? "" : text.trim();
        return (value.startsWith("{") && value.endsWith("}")) || (value.startsWith("[") && value.endsWith("]"));
    }
}
