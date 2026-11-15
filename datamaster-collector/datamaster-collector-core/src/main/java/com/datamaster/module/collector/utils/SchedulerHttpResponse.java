package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;

/** Accept only a positive execution ID or DS 3.4 HTTP's successful response envelope. */
public final class SchedulerHttpResponse {
    private SchedulerHttpResponse() { }

    public static Long executionId(String raw) {
        if (raw == null || raw.length() > 1024) throw invalid();
        String value = raw.trim();
        if (value.startsWith("{")) {
            try {
                JSONObject envelope = JSON.parseObject(value);
                if (envelope == null || envelope.size() != 2 || !envelope.containsKey("statusCode")
                        || !envelope.containsKey("body") || !(envelope.get("statusCode") instanceof Number)
                        || !"200".equals(String.valueOf(envelope.get("statusCode")))
                        || !(envelope.get("body") instanceof String)) throw invalid();
                value = ((String) envelope.get("body")).trim();
            } catch (RuntimeException ex) { throw invalid(); }
        }
        if (!value.matches("[1-9][0-9]{0,18}")) throw invalid();
        try { return Long.valueOf(value); }
        catch (NumberFormatException ex) { throw invalid(); }
    }

    private static ServiceException invalid() {
        return new ServiceException("运行执行ID或调度HTTP响应无效");
    }
}
