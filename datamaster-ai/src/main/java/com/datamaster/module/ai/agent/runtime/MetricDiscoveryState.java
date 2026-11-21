package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A semantic no-match may only follow complete discovery, before any query or denied tool. */
final class MetricDiscoveryState {
    private MetricDiscoveryState() { }

    static boolean mayChooseDataQa(String operationId) {
        boolean completeCatalogue = false;
        for (AgentToolResult result : AgentToolContextHolder.results()) {
            if (result == null || !Objects.equals(operationId, result.getOperationId())) return false;
            if (!AgentToolResult.SUCCEEDED.equals(result.getStatus())) return false;
            if (!"search_metrics".equals(result.getToolName()) && !"get_metric_definition".equals(result.getToolName())) return false;
            if ("search_metrics".equals(result.getToolName()) && result.getData() instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) result.getData();
                Object rows = data.get("rows");
                if (rows instanceof List && Boolean.TRUE.equals(data.get("catalogSearch"))
                        && Boolean.TRUE.equals(data.get("searchComplete"))) completeCatalogue = true;
            }
        }
        return operationId != null && completeCatalogue;
    }
}
