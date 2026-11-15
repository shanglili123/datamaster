package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.runtime.AgentToolFlowHaltException;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class DecisionBudgetTest {
    @Test public void cancellationStopsBeforeProgressOrAnotherTool() {
        AtomicBoolean cancelled = new AtomicBoolean(true); AtomicInteger calls = new AtomicInteger();
        DecisionBudget budget = new DecisionBudget(cancelled, message -> calls.incrementAndGet());
        try { budget.use("must not run"); fail(); }
        catch (AgentToolFlowHaltException expected) { assertEquals(AgentToolResult.NEED_INPUT, expected.getResult().getStatus()); }
        assertEquals(0, calls.get());
    }
    @Test public void sharedBudgetAllowsExactlyTwentyFourActivities() {
        AtomicInteger calls = new AtomicInteger(); DecisionBudget budget = new DecisionBudget(new AtomicBoolean(), message -> calls.incrementAndGet());
        for (int i = 0; i < 24; i++) budget.use("mock activity");
        try { budget.use("must not run"); fail(); }
        catch (AgentToolFlowHaltException expected) { assertEquals(AgentToolResult.NEED_INPUT, expected.getResult().getStatus()); }
        assertEquals(24, calls.get());
    }
    @Test public void cancellationAfterOneActivityPreventsContinuation() {
        AtomicBoolean cancelled = new AtomicBoolean(); AtomicInteger calls = new AtomicInteger();
        DecisionBudget budget = new DecisionBudget(cancelled, message -> calls.incrementAndGet());
        budget.use("first"); cancelled.set(true);
        try { budget.check(); fail(); } catch (AgentToolFlowHaltException expected) { assertNotNull(expected.getResult()); }
        assertEquals(1, calls.get());
    }
}
