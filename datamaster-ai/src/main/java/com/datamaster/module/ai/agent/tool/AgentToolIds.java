package com.datamaster.module.ai.agent.tool;

import com.datamaster.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts model-facing identifiers to database Long values only at the service boundary.
 * Tool DTOs intentionally expose IDs as strings: JSON numbers cannot represent Snowflake
 * identifiers exactly in all model/provider runtimes.
 */
public final class AgentToolIds {

    private AgentToolIds() {
    }

    public static Long longValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String text = value.trim();
        if (!text.matches("-?[0-9]+")) {
            throw new ServiceException("无效资源 ID：" + text
                    + "。平台 ID 必须是完整的十进制整数；不要使用 ds-xxx、table-xxx 等占位符或名称。"
                    + "请先调用 list_datasources，或调用 get_datasource(datasourceName=数据源名称) 获取真实 ID。");
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ex) {
            throw new ServiceException("ID 超出 Long 范围: " + text);
        }
    }

    public static List<Long> longValues(List<String> values) {
        if (values == null) return null;
        List<Long> result = new ArrayList<>();
        for (String value : values) result.add(longValue(value));
        return result;
    }
}
