package com.datamaster.module.ai.agent.tool;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.runtime.AgentToolAuditService;
import com.datamaster.module.ai.agent.runtime.DecisionAgentService;
import com.datamaster.module.ai.agent.decision.DecisionBudget;
import com.datamaster.module.ai.agent.runtime.AgentToolFlowHaltException;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.service.tool.ToolExecutor;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/** Fake models only: a platform stop must never become another model/tool round. */
public class AgentToolExecutionHaltTest {
    @Test
    public void executorPreservesTypedHaltAndItsResult() throws Exception {
        HaltingTool tool = new HaltingTool();
        Method method = HaltingTool.class.getMethod("execute");
        try {
            AgentToolExecutors.create(tool, method).execute(call("halt"), null);
            fail("must propagate the stop signal");
        } catch (AgentToolFlowHaltException expected) {
            assertSame(tool.result, expected.getResult());
        }
    }

    @Test
    public void aiServiceStopsOnTypedHaltWithoutCallingModelAgain() throws Exception {
        HaltingTool tool = new HaltingTool();
        Method method = HaltingTool.class.getMethod("execute");
        AtomicInteger modelCalls = new AtomicInteger();
        Assistant assistant = assistant(Collections.singletonMap(ToolSpecifications.toolSpecificationFrom(method),
                AgentToolExecutors.create(tool, method)), "halt", modelCalls);
        try {
            assistant.chat("test");
            fail("must stop rather than send the exception to the model");
        } catch (AgentToolFlowHaltException expected) {
            assertSame(tool.result, expected.getResult());
        }
        assertEquals(1, modelCalls.get());
    }

    @Test
    public void aiServiceStopsWhenBudgetFailsBeforeToolExecution() {
        AtomicInteger modelCalls = new AtomicInteger();
        AgentToolResult limit = AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "调用预算已用完", "operation");
        ToolSpecification specification = ToolSpecification.builder().name("budget").description("Test budget stop").build();
        Assistant assistant = assistant(Collections.singletonMap(specification, (request, memoryId) -> {
            throw new AgentToolFlowHaltException(limit);
        }), "budget", modelCalls);
        try {
            assistant.chat("test");
            fail("must stop on budget rejection");
        } catch (AgentToolFlowHaltException expected) {
            assertSame(limit, expected.getResult());
        }
        assertEquals(1, modelCalls.get());
    }

    @Test
    public void catalogStopsForEveryGovernedTerminalStatus() throws Exception {
        for (String status : new String[]{AgentToolResult.NEED_INPUT,
                AgentToolResult.NEED_CONFIRMATION, AgentToolResult.FAILED}) {
            RecordedTool tool = new RecordedTool(status);
            try (StaticApplicationContext application = new StaticApplicationContext()) {
                application.getBeanFactory().registerSingleton("recordedTool", tool);
                AgentToolCatalog catalog = new AgentToolCatalog();
                java.lang.reflect.Field applicationField = AgentToolCatalog.class.getDeclaredField("applicationContext");
                applicationField.setAccessible(true);
                applicationField.set(catalog, application);
                catalog.initialize();
                AtomicInteger modelCalls = new AtomicInteger();
                Assistant assistant = assistant(catalog.frameworkTools(), "recorded", modelCalls);
                try {
                    AgentToolContextHolder.set(null);
                    assistant.chat("test");
                    fail("must stop for " + status);
                } catch (AgentToolFlowHaltException expected) {
                    assertEquals(status, expected.getResult().getStatus());
                } finally {
                    AgentToolContextHolder.clear();
                }
                assertEquals(1, modelCalls.get());
                assertEquals(1, tool.executions);
            }
        }
    }

    @Test
    public void manualCatalogInvocationPreservesNeedInput() throws Exception {
        HaltingTool tool = new HaltingTool();
        try (StaticApplicationContext application = new StaticApplicationContext()) {
            application.getBeanFactory().registerSingleton("haltingTool", tool);
            AgentToolCatalog catalog = new AgentToolCatalog();
            java.lang.reflect.Field applicationField = AgentToolCatalog.class.getDeclaredField("applicationContext");
            applicationField.setAccessible(true);
            applicationField.set(catalog, application);
            catalog.initialize();
            AgentToolResult result = catalog.invoke("halt", null, Collections.emptyMap());
            assertSame(tool.result, result);
            assertEquals(AgentToolResult.NEED_INPUT, result.getStatus());
            assertEquals("halt", result.getToolName());
        }
    }

    @Test
    public void reflectionFailureIsNotReturnedAsSuccessfulToolText() throws Exception {
        Method method = FailingTool.class.getMethod("execute");
        AtomicInteger modelCalls = new AtomicInteger();
        Assistant assistant = assistant(Collections.singletonMap(ToolSpecifications.toolSpecificationFrom(method),
                AgentToolExecutors.create(new FailingTool(), method)), "failure", modelCalls);
        try {
            assistant.chat("test");
            fail("must not turn a failed tool into a model-visible result");
        } catch (RuntimeException expected) {
            assertEquals(1, modelCalls.get());
        }
    }

    @Test
    public void haltPreventsRemainingToolsInTheSameModelBatch() {
        AtomicInteger modelCalls = new AtomicInteger();
        AtomicInteger laterCalls = new AtomicInteger();
        AgentToolResult pending = AgentToolResult.of(AgentToolResult.NEED_CONFIRMATION, null, "请确认", "operation");
        ToolSpecification halt = ToolSpecification.builder().name("halt").description("Test stop").build();
        ToolSpecification later = ToolSpecification.builder().name("later").description("Must not execute").build();
        Map<ToolSpecification, ToolExecutor> tools = new java.util.LinkedHashMap<>();
        tools.put(halt, (request, memoryId) -> { throw new AgentToolFlowHaltException(pending); });
        tools.put(later, (request, memoryId) -> { laterCalls.incrementAndGet(); return "unexpected"; });
        ChatModel model = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                if (modelCalls.incrementAndGet() != 1) throw new AssertionError("model resumed after stop");
                ToolExecutionRequest second = ToolExecutionRequest.builder().id("call-2").name("later").arguments("{}").build();
                return ChatResponse.builder().aiMessage(AiMessage.from(call("halt"), second)).build();
            }
        };
        Assistant assistant = AiServices.builder(Assistant.class).chatModel(model).tools(tools)
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocation()).build();
        try {
            assistant.chat("test");
            fail("must stop the entire tool batch");
        } catch (AgentToolFlowHaltException expected) {
            assertSame(pending, expected.getResult());
        }
        assertEquals(1, modelCalls.get());
        assertEquals(0, laterCalls.get());
    }

    @Test
    public void successfulToolStillReturnsToTheModel() {
        AtomicInteger modelCalls = new AtomicInteger();
        ToolSpecification specification = ToolSpecification.builder().name("success").description("Test success").build();
        ChatModel model = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                int round = modelCalls.incrementAndGet();
                if (round > 2) throw new AssertionError("unexpected extra model round");
                return ChatResponse.builder().aiMessage(round == 1 ? AiMessage.from(call("success")) : AiMessage.from("done")).build();
            }
        };
        Map<ToolSpecification, ToolExecutor> tools = Collections.singletonMap(specification, (request, memoryId) -> "ok");
        Assistant assistant = AiServices.builder(Assistant.class).chatModel(model).tools(tools)
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocation()).build();
        assertEquals("done", assistant.chat("test"));
        assertEquals(2, modelCalls.get());
    }

    @Test
    public void decisionBoundaryRecordsFailureInsteadOfTreatingExceptionTextAsSuccess() throws Exception {
        DecisionAgentService service = new DecisionAgentService();
        java.lang.reflect.Field auditField = DecisionAgentService.class.getDeclaredField("audit");
        auditField.setAccessible(true);
        auditField.set(service, new AgentToolAuditService() {
            @Override public void record(String name, AgentToolContext context, Map<String, Object> arguments,
                                         AgentToolResult result, long durationMs) { /* No database. */ }
        });
        AgentToolContext context = new AgentToolContext("operation", 1L, "tester", 2L, "space", Collections.emptyMap());
        Map<ToolSpecification, ToolExecutor> tools = service.bounded(new FailingTool(), context,
                new DecisionBudget(new java.util.concurrent.atomic.AtomicBoolean(), activity -> { }));
        AgentToolContextHolder.set(context);
        try {
            tools.values().iterator().next().execute(call("failure"), null);
            fail("must stop on failed decision tool");
        } catch (AgentToolFlowHaltException expected) {
            assertEquals(AgentToolResult.FAILED, expected.getResult().getStatus());
            assertEquals(1, AgentToolContextHolder.results().size());
            assertEquals(AgentToolResult.FAILED, AgentToolContextHolder.lastResult().getStatus());
        } finally {
            AgentToolContextHolder.clear();
        }
    }

    private Assistant assistant(Map<ToolSpecification, ToolExecutor> tools, String toolName,
                                AtomicInteger modelCalls) {
        ChatModel model = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                if (modelCalls.incrementAndGet() != 1) throw new AssertionError("model resumed after stop");
                return ChatResponse.builder().aiMessage(AiMessage.from(call(toolName))).build();
            }
        };
        return AiServices.builder(Assistant.class).chatModel(model).tools(tools)
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocation()).build();
    }

    private static ToolExecutionRequest call(String name) {
        return ToolExecutionRequest.builder().id("call-1").name(name).arguments("{}").build();
    }

    public interface Assistant { String chat(String text); }

    @AgentTool(risk = AgentToolRisk.READ_ONLY)
    public static class HaltingTool {
        final AgentToolResult result = AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "需要补充业务条件", "operation");
        @Tool(name = "halt", value = "Test typed stop")
        public String execute() { throw new AgentToolFlowHaltException(result); }
    }

    public static class FailingTool {
        @Tool(name = "failure", value = "Test reflection failure")
        public String execute() { throw new IllegalStateException("must not reach model"); }
    }

    @AgentTool(risk = AgentToolRisk.READ_ONLY)
    public static class RecordedTool {
        private final String status;
        private int executions;
        RecordedTool(String status) { this.status = status; }
        @Tool(name = "recorded", value = "Test governed terminal result")
        public AgentToolResult execute() {
            executions++;
            AgentToolResult result = AgentToolResult.of(status, null, "stop", "operation");
            result.setToolName("recorded");
            AgentToolContextHolder.record(result);
            return result;
        }
    }
}
