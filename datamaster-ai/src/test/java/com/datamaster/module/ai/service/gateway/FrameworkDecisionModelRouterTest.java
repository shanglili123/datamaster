package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.router.RoutingChatModel;
import org.junit.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FrameworkDecisionModelRouterTest {
    @Test
    public void frameworkSemanticDecisionCanEscalateShortButComplexText() {
        AtomicInteger calls = new AtomicInteger();
        assertEquals("advanced", router("REPORT_TEMPLATE", classifier("ADVANCED", false, calls)).chat("短问题也需要复杂推理"));
        assertEquals(1, calls.get());
    }

    @Test
    public void simpleDecisionCanSelectEconomyAndUnknownDefaultsToAdvanced() {
        assertEquals("economy", router("REPORT_TEMPLATE", classifier("ECONOMY", false, new AtomicInteger())).chat("摘要"));
        assertEquals("advanced", router("REPORT_TEMPLATE", classifier("NEED_INPUT", false, new AtomicInteger())).chat("不明确"));
        assertEquals("advanced", router("REPORT_TEMPLATE", classifier(null, true, new AtomicInteger())).chat("拒答"));
    }

    @Test
    public void hardDecisionAndToolBoundariesCannotBeDowngradedByClassifier() {
        AtomicInteger calls = new AtomicInteger();
        ChatModel classifier = classifier("ECONOMY", false, calls);
        assertEquals("advanced", router("DECISION", classifier).chat("决策"));
        ChatResponse response = router("ASK_METRIC", classifier).chat(ChatRequest.builder()
                .messages(UserMessage.from("query"))
                .toolSpecifications(ToolSpecification.builder().name("query").description("query").build()).build());
        assertEquals("advanced", response.aiMessage().text());
        assertEquals(0, calls.get());
    }

    @Test
    public void escapedPayloadLimitDefaultsToAdvancedWithoutCallingClassifier() {
        AtomicInteger calls = new AtomicInteger();
        assertEquals("advanced", router("REPORT_TEMPLATE", classifier("ECONOMY", false, calls))
                .chat("\\".repeat(11999)));
        assertEquals(0, calls.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unrelatedClassifierConfigurationFailureMustStillPropagate() {
        ChatModel classifier = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                throw new IllegalArgumentException("invalid provider configuration");
            }
        };
        router("REPORT_TEMPLATE", classifier).chat("summary");
    }

    @Test(expected = RejectedExecutionException.class)
    public void capacityFailureMustNotSilentlyStartAnotherProviderRequest() {
        ChatModel classifier = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) { throw new RejectedExecutionException("full"); }
        };
        router("REPORT_TEMPLATE", classifier).chat("summary");
    }

    private ChatModel classifier(String value, boolean refused, AtomicInteger calls) {
        return new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                calls.incrementAndGet();
                String json = "{\"answers\":[{\"name\":\"classify\",\"value\":"
                        + (value == null ? "null" : "\"" + value + "\"") + ",\"refused\":" + refused + "}]}";
                return ChatResponse.builder().aiMessage(AiMessage.from(json)).build();
            }
        };
    }

    private RoutingChatModel router(String scene, ChatModel classifier) {
        return RoutingChatModel.builder().route("economy", named("economy")).route("advanced", named("advanced"))
                .defaultRoute("advanced").router(new FrameworkDecisionModelRouter(scene, 12000, classifier)).build();
    }

    private ChatModel named(String name) {
        return new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(name)).build();
            }
        };
    }
}
