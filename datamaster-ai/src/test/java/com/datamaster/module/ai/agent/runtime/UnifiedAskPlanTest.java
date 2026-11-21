package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.datamaster.module.ai.agent.runtime.UnifiedAskPlan.Delivery.*;
import static org.junit.Assert.*;

public class UnifiedAskPlanTest {
    private final AgentToolContext context = new AgentToolContext("turn", 1L, "tester", 2L, "space", Map.of());

    @Test public void nativeReturnDependsOnDeliveryAndEvidenceRequirements() {
        assertTrue(new UnifiedAskPlan(Set.of(ANSWER), false).permitsNativeAnswer());
        assertTrue(new UnifiedAskPlan(Set.of(ADVICE), false).permitsNativeAnswer());
        assertTrue(new UnifiedAskPlan(Set.of(REPORT), false).permitsNativeReport());
        assertFalse(new UnifiedAskPlan(Set.of(ANSWER), true).permitsNativeAnswer());
        assertFalse(new UnifiedAskPlan(Set.of(ANSWER, REPORT), false).permitsNativeAnswer());
        assertFalse(new UnifiedAskPlan(Set.of(REPORT, ADVICE), false).permitsNativeReport());
        assertTrue(new UnifiedAskPlan(Set.of(PROPOSAL), false).verifiedQuery());
    }

    @Test public void combinesQueryEvidenceWithTheRequestedArtifact() {
        var plan = new UnifiedAskPlan(Set.of(ANSWER, REPORT), true);
        var metric = result("execute_metric_analysis", "turn", Map.of("rows", List.of()));
        var report = result("generate_data_report", "turn", Map.of("artifactKind", "REPORT"));
        var staleReport = result("generate_data_report", "old-turn", Map.of("artifactKind", "REPORT"));
        assertFalse(plan.hasRequiredArtifacts(List.of(metric), context));
        assertFalse(plan.hasRequiredArtifacts(List.of(metric, staleReport), context));
        assertTrue(plan.hasRequiredArtifacts(List.of(metric, report), context));
    }

    @Test public void proposalThatNeedsInputIsNotACompletedDeliverable() {
        var plan = new UnifiedAskPlan(Set.of(PROPOSAL), false);
        var metric = result("execute_metric_analysis", "turn", Map.of("rows", List.of()));
        var pending = result("build_decision_proposal", "turn", Map.of(
                "artifactKind", "PROPOSAL", "status", "NEED_INPUT", "candidates", List.of()));
        assertFalse(plan.hasRequiredArtifacts(List.of(metric, pending), context));
    }

    @Test public void frameworkStopsAfterAnImmediateToolWithoutAnotherModelCall() {
        var modelCalls = new java.util.concurrent.atomic.AtomicInteger();
        var spec = dev.langchain4j.agent.tool.ToolSpecification.builder()
                .name("native_reply_example").description("Produce native reply").build();
        var model = new dev.langchain4j.model.chat.ChatModel() {
            @Override public dev.langchain4j.model.chat.response.ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest request) {
                assertEquals("Immediate tool must not trigger another model call", 1, modelCalls.incrementAndGet());
                return dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(
                        dev.langchain4j.data.message.AiMessage.from(dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                                .id("native-1").name(spec.name()).arguments("{}").build())).build();
            }
        };
        var agent = dev.langchain4j.service.AiServices.builder(UnifiedAskAgentService.Coordinator.class).chatModel(model)
                .toolProvider(request -> dev.langchain4j.service.tool.ToolProviderResult.builder()
                        .add(spec, (call, memoryId) -> "ready", dev.langchain4j.agent.tool.ReturnBehavior.IMMEDIATE).build())
                .build();
        assertEquals(dev.langchain4j.model.output.FinishReason.TOOL_EXECUTION, agent.plan("question").finishReason());
        assertEquals(1, modelCalls.get());
    }

    private static AgentToolResult result(String name, String operationId, Map<String, Object> data) {
        var result = AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "test", operationId);
        result.setToolName(name);
        return result;
    }
}
