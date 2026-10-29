package com.datamaster.module.ai.agent.analysis;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import java.util.*;

/** 只使用本轮服务器已记录的查询结果，模型不能提交或伪造数据行。 */
public final class MetricResultEvidence {
    private MetricResultEvidence() { }

    public static Map<?, ?> latest(String metricId, String version, AgentToolContext context) {
        List<AgentToolResult> results = AgentToolContextHolder.results();
        for (int i = results.size() - 1; i >= 0; i--) {
            AgentToolResult result = results.get(i);
            if (!"execute_metric_analysis".equals(result.getToolName()) || !AgentToolResult.SUCCEEDED.equals(result.getStatus())
                    || !Objects.equals(context.getOperationId(), result.getOperationId()) || !(result.getData() instanceof Map)) continue;
            Map<?, ?> data = (Map<?, ?>) result.getData();
            if (metricId.equals(String.valueOf(data.get("metricId"))) && version.equals(String.valueOf(data.get("version")))) return data;
        }
        throw new IllegalArgumentException("需要本轮相同指标版本的成功查询结果");
    }
}
