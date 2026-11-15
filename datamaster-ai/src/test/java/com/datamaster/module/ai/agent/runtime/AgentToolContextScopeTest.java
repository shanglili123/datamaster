package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class AgentToolContextScopeTest {
    @Test
    public void threadsNeverShareIdentityResultsOrSuccessfulCalls() throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);
        try {
            java.util.List<java.util.concurrent.Future<String>> tasks = new java.util.ArrayList<>();
            for (String id : new String[]{"owner-a", "owner-b"}) tasks.add(pool.submit(() -> {
                AgentToolContext context = new AgentToolContext(id, id.equals("owner-a") ? 1L : 2L,
                        id, id.equals("owner-a") ? 10L : 20L, id, Collections.emptyMap());
                try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(context)) {
                    AgentToolResult result = AgentToolResult.of(AgentToolResult.SUCCEEDED, id, "ok", id);
                    AgentToolContextHolder.record(result);
                    AgentToolContextHolder.recordCall(id, Collections.emptyMap(), result);
                    barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                    assertSame(context, AgentToolContextHolder.get());
                    assertSame(result, AgentToolContextHolder.lastResult());
                    assertEquals(1, AgentToolContextHolder.results().size());
                    assertFalse(AgentToolContextHolder.hasSuccessfulCall(id.equals("owner-a") ? "owner-b" : "owner-a", Collections.emptyMap()));
                }
                assertNull(AgentToolContextHolder.get()); assertTrue(AgentToolContextHolder.results().isEmpty());
                return id;
            }));
            assertEquals("owner-a", tasks.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals("owner-b", tasks.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); AgentToolContextHolder.clear(); }
    }

    @Test
    public void exceptionInNestedScopeRestoresOuterStateAndCloseIsIdempotent() {
        AgentToolContext outer = new AgentToolContext("outer", 1L, "owner", 2L, "space", Collections.emptyMap());
        AgentToolContextHolder.set(outer);
        AgentToolContextHolder.Scope scope = AgentToolContextHolder.scope(new AgentToolContext("child", 3L, "child", 4L, "other", Collections.emptyMap()));
        try {
            try (AgentToolContextHolder.Scope ignored = scope) { throw new IllegalStateException("mock failure"); }
        } catch (IllegalStateException expected) {
            assertSame(outer, AgentToolContextHolder.get()); scope.close(); assertSame(outer, AgentToolContextHolder.get());
        } finally { AgentToolContextHolder.clear(); }
    }

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
