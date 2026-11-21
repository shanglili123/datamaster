package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.service.tool.ToolExecutor;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class MetricAnalysisToolProviderTest {
    @Test public void installedSdkRefreshesToolsAfterEachGovernedStep() {
        String operation = "current";
        AgentToolContext context = new AgentToolContext(operation, 1L, "test", 2L, "space", Collections.emptyMap());
        Map<ToolSpecification, ToolExecutor> tools = tools(operation);
        AtomicInteger rounds = new AtomicInteger();
        ChatModel model = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                int round = rounds.incrementAndGet();
                Set<String> names = request.parameters().toolSpecifications().stream().map(ToolSpecification::name).collect(Collectors.toSet());
                if (round == 1) assertEquals(Collections.singleton("search_metrics"), names);
                else if (round == 2) assertEquals(new HashSet<>(Arrays.asList("search_metrics", "get_metric_definition")), names);
                else assertEquals(new HashSet<>(Arrays.asList("search_metrics", "get_metric_definition", "execute_metric_analysis")), names);
                if (round == 4) return ChatResponse.builder().aiMessage(AiMessage.from("verified")).build();
                String name = round == 1 ? "search_metrics" : round == 2 ? "get_metric_definition" : "execute_metric_analysis";
                return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder().id("call-"+round).name(name).arguments("{}").build())).build();
            }
        };
        try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(context)) {
            Assistant assistant = AiServices.builder(Assistant.class).chatModel(model)
                    .toolProvider(new MetricAnalysisToolProvider(tools, operation))
                    .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocation()).build();
            assertEquals("verified", assistant.chat("test"));
            assertEquals(4, rounds.get());
        }
    }

    @Test public void emptyFailedAndForeignSearchesDoNotUnlockTools() {
        try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(new AgentToolContext("current",1L,"test",2L,"space",Collections.emptyMap()))) {
            MetricAnalysisToolProvider provider = new MetricAnalysisToolProvider(tools("current"), "current");
            record("search_metrics", AgentToolResult.SUCCEEDED, "foreign", offered());
            record("search_metrics", AgentToolResult.FAILED, "current", offered());
            record("search_metrics", AgentToolResult.SUCCEEDED, "current", Collections.singletonMap("rows", Collections.emptyList()));
            record("get_metric_definition", AgentToolResult.SUCCEEDED, "foreign", Collections.emptyMap());
            assertEquals(Collections.singleton("search_metrics"), names(provider));
            record("search_metrics", AgentToolResult.SUCCEEDED, "current", offered());
            assertEquals(new HashSet<>(Arrays.asList("search_metrics","get_metric_definition")), names(provider));
        }
    }

    @Test public void dataQaChoiceRequiresCompleteDiscoveryAndIsNotExposedToDecisionSpecialist() {
        try (AgentToolContextHolder.Scope ignored=AgentToolContextHolder.scope(new AgentToolContext("current",1L,"test",2L,"space",Collections.emptyMap()))) {
            MetricAnalysisToolProvider ordinary=new MetricAnalysisToolProvider(tools("current"),"current",true);
            assertFalse(names(ordinary).contains("use_data_qa"));
            Map<String,Object> catalogue=new LinkedHashMap<>(offered());catalogue.put("catalogSearch",true);catalogue.put("searchComplete",true);
            record("search_metrics",AgentToolResult.SUCCEEDED,"current",catalogue);
            assertTrue(names(ordinary).contains("use_data_qa"));
            assertFalse(names(new MetricAnalysisToolProvider(tools("current"),"current",false)).contains("use_data_qa"));
            record("execute_metric_analysis",AgentToolResult.FAILED,"current",Collections.emptyMap());
            assertFalse(names(ordinary).contains("use_data_qa"));
        }
    }

    private static Set<String> names(MetricAnalysisToolProvider provider) {
        return provider.provideTools(null).tools().keySet().stream().map(ToolSpecification::name).collect(Collectors.toSet());
    }
    private static Map<ToolSpecification, ToolExecutor> tools(String operation) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        for (String name : Arrays.asList("search_metrics","get_metric_definition","execute_metric_analysis")) {
            tools.put(ToolSpecification.builder().name(name).description(name).build(), (request,memory) -> {
                record(name, AgentToolResult.SUCCEEDED, operation, "search_metrics".equals(name) ? offered() : Collections.emptyMap());
                return "{}";
            });
        }
        return tools;
    }
    private static Map<String,Object> offered() {
        Map<String,Object> row = new LinkedHashMap<>();row.put("id","123");row.put("publishedVersion","1");row.put("status","PUBLISHED");
        return Collections.singletonMap("rows", Collections.singletonList(row));
    }
    private static void record(String name, String status, String operation, Object data) {
        AgentToolResult result = AgentToolResult.of(status,data,"test",operation);result.setToolName(name);AgentToolContextHolder.record(result);
    }
    public interface Assistant { String chat(String input); }
}
