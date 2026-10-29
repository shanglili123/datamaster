package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class AgentToolContextScopeTest {
    @Test
    public void childClearRestoresOuterContextAndResults() {
        AgentToolContext outer = new AgentToolContext("outer", 1L, "user", 2L, "space", Collections.emptyMap());
        AgentToolContext child = new AgentToolContext("child", 1L, "user", 2L, "space", Collections.emptyMap());
        try {
            AgentToolContextHolder.set(outer);
            AgentToolResult result = AgentToolResult.of(AgentToolResult.SUCCEEDED, "outer", "done", "outer");
            AgentToolContextHolder.record(result);
            AgentToolContextHolder.recordCall("outer_tool", Collections.emptyMap(), result);
            try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(child)) {
                assertSame(child, AgentToolContextHolder.get());
                assertTrue(AgentToolContextHolder.results().isEmpty());
                AgentToolContextHolder.clear();
            }
            assertSame(outer, AgentToolContextHolder.get());
            assertSame(result, AgentToolContextHolder.lastResult());
            assertTrue(AgentToolContextHolder.hasSuccessfulCall("outer_tool", Collections.emptyMap()));
        } finally { AgentToolContextHolder.clear(); }
    }
}
