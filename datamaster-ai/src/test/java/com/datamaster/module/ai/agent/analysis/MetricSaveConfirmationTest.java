package com.datamaster.module.ai.agent.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.runtime.AgentRunResponse;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MetricSaveConfirmationTest {
    private AgentToolResult pending(String tool, String hash) {
        AgentToolResult result = AgentToolResult.of(AgentToolResult.NEED_CONFIRMATION, null, "确认", "operation");
        result.setToolName(tool);
        result.getMetadata().put("confirmationHash", hash);
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("metricId", "123"); query.put("version", "1");
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("name", "保存的分析"); arguments.put("query", query);
        result.getMetadata().put("arguments", arguments);
        return result;
    }
    private String json(AgentToolResult... results) {
        AgentRunResponse response = new AgentRunResponse();
        response.setToolResults(Arrays.asList(results));
        return JSON.toJSONString(response);
    }

    @Test
    public void restoresOnlyLatestPendingSaveAndServerArguments() {
        String stored = json(pending("save_analysis", "hash1"));
        AgentToolResult pending = MetricSaveConfirmation.pending(AgentToolResult.NEED_CONFIRMATION, stored);
        assertNotNull(pending);
        assertEquals("保存的分析", MetricSaveConfirmation.arguments(pending).getName());
        assertEquals("123", MetricSaveConfirmation.arguments(pending).getQuery().getMetricId());
        assertNull(MetricSaveConfirmation.pending(AgentToolResult.SUCCEEDED, stored));
    }

    @Test
    public void doesNotReplayHistoricalSaveWhenAnotherToolNeedsConfirmation() {
        assertNull(MetricSaveConfirmation.pending(AgentToolResult.NEED_CONFIRMATION,
                json(pending("save_analysis", "hash1"), pending("delete_datasource", "hash2"))));
    }

    @Test
    public void consumedReceiptPreventsReplayEvenIfStalePendingWasMergedAgain() {
        AgentToolResult consumed = pending("save_analysis", "hash1");
        consumed.getMetadata().put("confirmationConsumed", true);
        assertNull(MetricSaveConfirmation.pending(AgentToolResult.NEED_CONFIRMATION,
                json(consumed, pending("save_analysis", "hash1"))));
    }
}
