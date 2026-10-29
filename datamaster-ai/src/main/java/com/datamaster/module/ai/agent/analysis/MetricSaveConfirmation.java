package com.datamaster.module.ai.agent.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.runtime.AgentRunResponse;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import java.util.List;

/** 只恢复最后一个待确认工具的服务端参数，不从确认消息重新抽取名称和查询条件。 */
public final class MetricSaveConfirmation {
    private MetricSaveConfirmation() { }

    public static AgentToolResult pending(String status, String resultJson) {
        if (!AgentToolResult.NEED_CONFIRMATION.equals(status) || resultJson == null) return null;
        AgentRunResponse response = JSON.parseObject(resultJson, AgentRunResponse.class);
        List<AgentToolResult> results = response == null ? null : response.getToolResults();
        if (results == null || results.isEmpty()) return null;
        AgentToolResult last = results.get(results.size() - 1);
        if (last == null || !"save_analysis".equals(last.getToolName())
                || !AgentToolResult.NEED_CONFIRMATION.equals(last.getStatus())
                || Boolean.TRUE.equals(last.getMetadata().get("confirmationConsumed"))) return null;
        Object hash = last.getMetadata().get("confirmationHash");
        if (!(hash instanceof String) || ((String) hash).isEmpty()) return null;
        for (AgentToolResult previous : results) {
            if (previous != null && "save_analysis".equals(previous.getToolName())
                    && hash.equals(previous.getMetadata().get("confirmationHash"))
                    && Boolean.TRUE.equals(previous.getMetadata().get("confirmationConsumed"))) return null;
        }
        return last;
    }

    public static AgentToolRequest.SaveMetricAnalysis arguments(AgentToolResult pending) {
        Object arguments = pending == null ? null : pending.getMetadata().get("arguments");
        if (arguments == null) throw new IllegalArgumentException("缺少待确认的分析参数");
        AgentToolRequest.SaveMetricAnalysis result = JSON.parseObject(JSON.toJSONString(arguments), AgentToolRequest.SaveMetricAnalysis.class);
        if (result == null || result.getQuery() == null) throw new IllegalArgumentException("待确认的分析参数无效");
        return result;
    }
}
