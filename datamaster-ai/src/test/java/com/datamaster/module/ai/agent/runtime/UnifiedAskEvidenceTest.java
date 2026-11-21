package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UnifiedAskEvidenceTest {
    private final AgentToolContext context = new AgentToolContext("current", 1L, "tester", 2L, "space", Map.of());

    @Test public void foreignResultsAndDiscoveryAloneCannotAnswer() {
        var foreign = result("execute_metric_analysis", AgentToolResult.SUCCEEDED, "previous", Map.of("rows", List.of()));
        var discovery = result("search_metrics", AgentToolResult.SUCCEEDED, "current", Map.of("rows", List.of()));
        assertFalse(UnifiedAskAgentService.hasAnswerEvidence(List.of(foreign, discovery), context));
    }

    @Test public void missingInputCanContinueButExplicitFailureCannot() {
        var incomplete = result("execute_metric_analysis", AgentToolResult.NEED_INPUT, "current", null);
        assertFalse(UnifiedAskAgentService.recoverableInput(incomplete, context));
        var repairable = AgentToolResult.recoverableInput("请补全条件", "current");
        assertTrue(UnifiedAskAgentService.recoverableInput(repairable, context));
        assertFalse(UnifiedAskAgentService.hasAnswerEvidence(List.of(repairable), context));
        repairable.setStatus(AgentToolResult.FAILED);
        assertFalse(UnifiedAskAgentService.recoverableInput(repairable, context));
    }

    @Test public void permissionDenialIsAuditedWithoutExecutingToolBody() throws Exception {
        var aspect = new AgentToolAspect();
        var audit = org.mockito.Mockito.mock(AgentToolAuditService.class);
        var field = AgentToolAspect.class.getDeclaredField("auditService");
        field.setAccessible(true);
        field.set(aspect, audit);
        var factory = new org.springframework.aop.aspectj.annotation.AspectJProxyFactory(new AgentToolInvocation());
        factory.addAspect(aspect);
        AgentToolInvocation proxy = factory.getProxy();
        try (var ignored = AgentToolContextHolder.scope(context)) {
            var denied = proxy.execute("lookup", context, new String[]{"ai:ask-data:query"}, Map.of(),
                    () -> { throw new AssertionError("unauthorized tool body executed"); });
            assertEquals(AgentToolResult.FAILED, denied.getStatus());
            assertEquals("当前账号没有调用此能力的权限", AgentToolRejection.publicFailure(denied, "fallback"));
            assertSame(denied, AgentToolContextHolder.lastResult());
            org.mockito.Mockito.verify(audit).record(org.mockito.Mockito.eq("lookup"), org.mockito.Mockito.same(context),
                    org.mockito.Mockito.eq(Map.of()), org.mockito.Mockito.same(denied), org.mockito.Mockito.anyLong());
        }
    }

    @Test public void sdkContinuesAfterRepairableInputButStopsAfterPermissionDenial() {
        for (boolean denied : new boolean[]{false, true}) {
            var attempts = new java.util.concurrent.atomic.AtomicInteger();
            var rounds = new java.util.concurrent.atomic.AtomicInteger();
            var spec = dev.langchain4j.agent.tool.ToolSpecification.builder().name("lookup").description("Lookup evidence").build();
            dev.langchain4j.service.tool.ToolExecutor tool = (call, memoryId) -> UnifiedAskAgentService.toolReply(
                    attempts.incrementAndGet() > 1 ? result("lookup", AgentToolResult.SUCCEEDED, "current", Map.of("found", true))
                            : denied ? AgentToolRejection.Reason.PERMISSION_DENIED.result("current")
                            : AgentToolResult.recoverableInput("请换一个检索条件", "current"));
            var model = new dev.langchain4j.model.chat.ChatModel() {
                @Override public dev.langchain4j.model.chat.response.ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest request) {
                    int round = rounds.incrementAndGet();
                    var message = round == 3 ? dev.langchain4j.data.message.AiMessage.from("done")
                            : dev.langchain4j.data.message.AiMessage.from(dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                            .id("lookup-" + round).name(spec.name()).arguments("{}").build());
                    return dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(message).build();
                }
            };
            var agent = dev.langchain4j.service.AiServices.builder(UnifiedAskAgentService.Coordinator.class).chatModel(model)
                    .tools(Map.of(spec, tool)).toolExecutionErrorHandler(dev.langchain4j.service.tool.ToolExecutionErrorHandler.failInvocation()).build();
            if (denied) {
                try {
                    agent.plan("question");
                    fail("permission denial must halt the tool loop");
                } catch (AgentToolFlowHaltException stopped) {
                    assertEquals("当前账号没有调用此能力的权限", AgentToolRejection.publicFailure(stopped.getResult(), "fallback"));
                }
                assertEquals(1, rounds.get());
                assertEquals(1, attempts.get());
            } else {
                assertEquals("done", agent.plan("question").content());
                assertEquals(3, rounds.get());
                assertEquals(2, attempts.get());
            }
        }
    }

    @Test public void comparisonReferencesStoreConditionsNotValues() {
        Map<String, String> comparison = Map.of("metricId", "12", "version", "1", "currentStartTime", "2026-01-01");
        var result = result("compare_periods", AgentToolResult.SUCCEEDED, "current", Map.of(
                "currentValue", "10", "baselineValue", "2", "comparisonDefinition", comparison));
        var reference = UnifiedAskTurnStore.replayReference(result, "current");
        assertNotNull(reference);
        assertEquals("compare_periods", reference.getToolName());
        assertEquals(Map.of("comparisonDefinition", comparison), reference.getData());
        assertNull(UnifiedAskTurnStore.replayReference(result, "previous"));
    }

    @Test public void missingTableDefinitionIsNotEvidenceButVisibleDefinitionIs() {
        var empty = result("describe_source_table", AgentToolResult.SUCCEEDED, "current", Map.of("definitions", List.of()));
        var visible = result("describe_source_table", AgentToolResult.SUCCEEDED, "current", Map.of(
                "definitions", List.of(Map.of("name", "订单", "source", "ASSET"))));
        assertFalse(UnifiedAskAgentService.hasAnswerEvidence(List.of(empty), context));
        assertTrue(UnifiedAskAgentService.hasAnswerEvidence(List.of(visible), context));
        assertFalse(UnifiedAskAgentService.isMetricEvidence(visible));
    }

    @Test public void exploratoryReplyIsNotFormalEvidence() {
        var reply = result("ask_data", AgentToolResult.SUCCEEDED, "current", Map.of(
                "verification", "UNVERIFIED_EXPLORATORY", "remoteReply", "10"));
        assertFalse(UnifiedAskAgentService.hasAnswerEvidence(List.of(reply), context));
        reply.setToolName("another_provider_tool");
        assertTrue(UnifiedAskAgentService.isUnverifiedExploration(reply));
        assertFalse(UnifiedAskAgentService.hasAnswerEvidence(List.of(reply), context));
        var verified = result("execute_metric_analysis", AgentToolResult.SUCCEEDED, "current", Map.of("rows", List.of()));
        var evidence = UnifiedAskAgentService.answerEvidence(List.of(reply, verified), context);
        assertEquals(1, evidence.size());
        assertEquals(verified.getToolName(), evidence.get(0).get("tool"));
        assertFalse(UnifiedAskAgentService.isMetricEvidence(reply));
        assertNull(UnifiedAskTurnStore.replayReference(reply, "current"));
    }

    private static AgentToolResult result(String tool, String status, String operation, Object data) {
        var result = AgentToolResult.of(status, data, "test", operation);
        result.setToolName(tool);
        return result;
    }
}
