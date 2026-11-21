package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import java.util.Map;

/** Exploration is allowed for absence of definitions, never for denied or failed metric execution. */
public final class AskDataRoutingPolicy {
    private AskDataRoutingPolicy() { }

    public static boolean requiresFormalMetric(String question) {
        if (question == null) return false;
        return question.contains("正式指标") || question.contains("已发布指标") || question.contains("指标口径")
                || question.contains("保存分析") || question.contains("发布版本");
    }

    public static boolean mayExplore(AskUnifiedReqVO request, AgentRunResponse response) {
        return request != null && response != null && !requiresFormalMetric(request.getQuestion())
                && !Boolean.TRUE.equals(request.getConfirmed()) && request.getOperationId() == null
                && request.getContinuationOperationId() == null && request.getAnalysisOperationId() == null
                && "qa".equalsIgnoreCase(request.getResponseMode() == null ? "qa" : request.getResponseMode())
                && AgentToolResult.NEED_INPUT.equals(response.getStatus())
                && response.getToolResults() != null && !response.getToolResults().isEmpty()
                && response.getOperationId() != null
                && eligibleDiscovery(response);
    }

    private static boolean eligibleDiscovery(AgentRunResponse response) {
        boolean completeCatalogue = false;
        boolean chosen = false;
        boolean onlyEmptySearches = true;
        java.util.List<AgentToolResult> results = response.getToolResults();
        for (int i = 0; i < results.size(); i++) {
            AgentToolResult result = results.get(i);
            if (result == null || !response.getOperationId().equals(result.getOperationId())) return false;
            if ("use_data_qa".equals(result.getToolName())) {
                if (i != results.size() - 1 || !completeCatalogue || !AgentToolResult.NEED_INPUT.equals(result.getStatus())
                        || !"NO_APPLICABLE_METRIC".equals(result.getMetadata().get("routingChoice"))) return false;
                chosen = true;
                continue;
            }
            if (!AgentToolResult.SUCCEEDED.equals(result.getStatus())
                    || !("search_metrics".equals(result.getToolName()) || "get_metric_definition".equals(result.getToolName()))) return false;
            if (result.getData() instanceof Map && "search_metrics".equals(result.getToolName())) {
                Map<?, ?> data = (Map<?, ?>) result.getData();
                if (Boolean.TRUE.equals(data.get("catalogSearch")) && Boolean.TRUE.equals(data.get("searchComplete"))) completeCatalogue = true;
            }
            onlyEmptySearches &= completeEmptySearch(result);
        }
        return chosen || onlyEmptySearches;
    }

    private static boolean completeEmptySearch(AgentToolResult result) {
        if (result == null || !"search_metrics".equals(result.getToolName())
                || !AgentToolResult.SUCCEEDED.equals(result.getStatus()) || !(result.getData() instanceof Map)) return false;
        Map<?, ?> data = (Map<?, ?>) result.getData();
        return Boolean.TRUE.equals(data.get("catalogSearch")) && Boolean.TRUE.equals(data.get("searchComplete"))
                && data.get("total") instanceof Number && ((Number) data.get("total")).longValue() == 0;
    }
}
