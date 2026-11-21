package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class DecisionAnalyzeToolInvocationTest {
    @Test public void metricEvidenceRequiresSuccessfulQueryFromCurrentOperation() {
        var context = new AgentToolContext("current", 1L, "tester", 2L, "space", Map.of());
        var result = AgentToolResult.of(AgentToolResult.SUCCEEDED, Map.of("rows", java.util.List.of()), "result", "current");
        result.setToolName("get_metric_definition");
        assertFalse(UnifiedAskAgentService.isMetricEvidence(result));
        result.setToolName("execute_metric_analysis");
        assertTrue(UnifiedAskAgentService.isMetricEvidence(result));
        assertTrue(UnifiedAskAgentService.isCurrent(result, context));
        result.setOperationId("previous");
        assertFalse(UnifiedAskAgentService.isCurrent(result, context));
        result.setStatus(AgentToolResult.FAILED);
        assertFalse(UnifiedAskAgentService.isMetricEvidence(result));
    }
}
