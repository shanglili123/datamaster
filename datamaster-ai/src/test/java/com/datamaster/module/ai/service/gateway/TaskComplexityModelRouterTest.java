package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.router.RoutingChatModel;
import org.junit.Test;

import static org.junit.Assert.*;

public class TaskComplexityModelRouterTest {
    @Test
    public void routesSmallTextToEconomyAndLargeOrDecisionRequestsToAdvanced() {
        assertEquals("economy", router("REPORT_TEMPLATE", 100).chat("hello"));
        assertEquals("advanced", router("REPORT_TEMPLATE", 4).chat("hello"));
        assertEquals("advanced", router("DECISION", 100).chat("hello"));
        assertEquals("advanced", router("ACTION_DECISION", 100).chat("hello"));
    }

    @Test
    public void toolRequestsUseAdvancedAndFrameworkPinsContinuationRoute() {
        RoutingChatModel model = router("ASK_METRIC", 100);
        ChatResponse response = model.chat(ChatRequest.builder().messages(UserMessage.from("query"))
                .toolSpecifications(ToolSpecification.builder().name("lookup").description("lookup").build()).build());
        assertEquals("advanced", response.aiMessage().text());
        ToolExecutionRequest tool = ToolExecutionRequest.builder().id("t1").name("lookup").arguments("{}").build();
        AiMessage routedCall = AiMessage.from(tool).toBuilder().attributes(response.aiMessage().attributes()).build();
        ChatResponse continuation = model.chat(ChatRequest.builder().messages(UserMessage.from("query"), routedCall,
                ToolExecutionResultMessage.from(tool, "result")).build());
        assertEquals("advanced", continuation.aiMessage().text());
        assertEquals("advanced", continuation.aiMessage().attributes().get(RoutingChatModel.ROUTE_ATTRIBUTE));
    }

    private RoutingChatModel router(String scene, int limit) {
        return RoutingChatModel.builder().route("economy", fake("economy"))
                .route("advanced", fake("advanced")).defaultRoute("advanced")
                .router(new TaskComplexityModelRouter(scene, limit)).build();
    }

    private ChatModel fake(String name) {
        return new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(name)).build();
            }
        };
    }
}
