package com.datamaster.module.ai.service.gateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.math.BigDecimal;

/** 仅提取上游明确报告的usage，不按正文长度估算，不递归猜测工具内部用量。 */
public final class InvocationUsage {
    private Long input;
    private Long output;
    public Long getInput() { return input; }
    public Long getOutput() { return output; }

    public static InvocationUsage fromJson(String text) {
        InvocationUsage result = new InvocationUsage();
        if (text == null || text.length() > 2000000) return result;
        try {
            JSONObject object = JSON.parseObject(text);
            JSONObject usage = object == null ? null : object.getJSONObject("usage");
            if (usage == null) return result;
            result.input = count(usage.containsKey("prompt_tokens") ? usage.get("prompt_tokens") : usage.get("input_tokens"));
            result.output = count(usage.containsKey("completion_tokens") ? usage.get("completion_tokens") : usage.get("output_tokens"));
        } catch (RuntimeException ignored) { /* Missing/malformed usage is unknown, not zero. */ }
        return result;
    }

    public void acceptLine(String line) {
        if (line == null) return;
        String text = line.trim();
        if (text.startsWith("data:")) text = text.substring(5).trim();
        if (!text.startsWith("{")) return;
        InvocationUsage measured = fromJson(text);
        // Streaming usage is normally a cumulative final snapshot. Never sum repeated SSE events.
        if (measured.input != null) input = measured.input;
        if (measured.output != null) output = measured.output;
    }

    private static Long count(Object value) {
        if (!(value instanceof Number) && !(value instanceof String)) return null;
        try {
            long result = new BigDecimal(String.valueOf(value)).longValueExact();
            return result >= 0 ? result : null;
        } catch (ArithmeticException | NumberFormatException ignored) { return null; }
    }
}
