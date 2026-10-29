package com.datamaster.module.ai.agent.analysis;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MetricResultEvidenceTest {
    @Test
    public void usesOnlySuccessfulCurrentOperationQueryEvidence() {
        AgentToolContext context = new AgentToolContext("current", 1L, "tester", 2L, "test", Collections.emptyMap());
        AgentToolContextHolder.set(context);
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("metricId", "123"); data.put("version", "1");
            AgentToolResult foreign = AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "", "other");
            foreign.setToolName("execute_metric_analysis");
            AgentToolContextHolder.record(foreign);
            try {
                MetricResultEvidence.latest("123", "1", context);
                fail("must not accept another operation");
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
            AgentToolResult current = AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "", "current");
            current.setToolName("execute_metric_analysis");
            AgentToolContextHolder.record(current);
            assertEquals("123", MetricResultEvidence.latest("123", "1", context).get("metricId"));
        } finally {
            AgentToolContextHolder.clear();
        }
    }
}
