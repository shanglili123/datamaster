package com.datamaster.module.ai.service.gateway;

import org.junit.Test;
import java.util.concurrent.RejectedExecutionException;
import static org.junit.Assert.*;

public class AiInvocationGatewayTest {
    private static class MemoryRepository extends AiInvocationRepository {
        private int rows;
        MemoryRepository() { super(null); }
        @Override boolean insert(AiInvocationGateway.Invocation invocation) { rows++; return true; }
    }
    @Test
    public void capacityIsSeparateAndCloseIsIdempotent() {
        MemoryRepository repository = new MemoryRepository();
        AiInvocationGateway gateway = new AiInvocationGateway(1, 1, repository);
        AiInvocationGateway.Invocation model = gateway.begin("MODEL", "TEST", "test-model", false);
        try {
            gateway.begin("MODEL", "TEST", "test-model", false);
            fail("must reject excess concurrency");
        } catch (RejectedExecutionException expected) { assertNotNull(expected); }
        try (AiInvocationGateway.Invocation dbgpt = gateway.begin("DBGPT", "TEST", null, true)) { dbgpt.success(null, null); }
        model.success(10L, 5L); model.close(); model.close();
        assertEquals(Long.valueOf(0), gateway.metrics().get("inflight"));
        assertEquals(Long.valueOf(2), gateway.metrics().get("success"));
        assertEquals(Long.valueOf(1), gateway.metrics().get("rejected"));
        assertEquals(3, repository.rows);
    }
    @Test
    public void parsesMeasuredUsageWithoutSummingRepeatedStreamSnapshots() {
        InvocationUsage usage = new InvocationUsage();
        usage.acceptLine("data: {\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":20}}");
        usage.acceptLine("data: {\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":20}}");
        assertEquals(Long.valueOf(100), usage.getInput());
        assertEquals(Long.valueOf(20), usage.getOutput());
        assertNull(InvocationUsage.fromJson("{\"content\":\"hello\"}").getInput());
        assertNull(InvocationUsage.fromJson("{\"usage\":{\"prompt_tokens\":-1}}").getInput());
    }
}
