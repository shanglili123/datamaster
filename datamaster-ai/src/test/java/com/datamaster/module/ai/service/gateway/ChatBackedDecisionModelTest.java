package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.exception.ContentFilteredException;
import dev.langchain4j.exception.InvalidDecisionResponseException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.decision.request.ChoiceQuestion;
import dev.langchain4j.model.decision.request.DecisionRequest;
import dev.langchain4j.model.decision.response.DecisionResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;
import dev.langchain4j.service.decision.DecisionServices;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChatBackedDecisionModelTest {
    public enum Level { ECONOMY, ADVANCED }
    public interface Decisions {
        @Decide("选择模型等级") Level classify(@V("question") String question);
    }

    @Test
    public void frameworkDecisionServiceUsesExistingChatProtocolWithoutInventingProbability() {
        ChatBackedDecisionModel model = model("{\"answers\":[{\"name\":\"classify\",\"value\":\"ECONOMY\",\"refused\":false}]}");
        Decisions service = DecisionServices.builder(Decisions.class).decisionModel(model).build();
        assertEquals(Level.ECONOMY, service.classify("summarize"));
        DecisionResponse response = model.decide(request());
        assertTrue(response.choice("classify").probabilities().isEmpty());
        assertNull(response.choice("classify").confidence());
        assertEquals(Integer.valueOf(7), response.tokenUsage().inputTokenCount());
    }

    @Test(expected = ContentFilteredException.class)
    public void refusalCannotBeConvertedToAnApprovedChoice() {
        Decisions service = DecisionServices.builder(Decisions.class).decisionModel(model(
                "{\"answers\":[{\"name\":\"classify\",\"value\":null,\"refused\":true}]}" )).build();
        service.classify("unknown");
    }

    @Test(expected = InvalidDecisionResponseException.class)
    public void rejectsChoiceOutsideFrameworkQuestionOptions() {
        model("{\"answers\":[{\"name\":\"classify\",\"value\":\"EXECUTE\",\"refused\":false}]}").decide(request());
    }

    @Test(expected = InvalidDecisionResponseException.class)
    public void rejectsDuplicateAnswersRatherThanOverwriting() {
        model("{\"answers\":[{\"name\":\"classify\",\"value\":\"ECONOMY\",\"refused\":false},"
                + "{\"name\":\"classify\",\"value\":\"ADVANCED\",\"refused\":false}]}").decide(request());
    }

    @Test
    public void rejectsContentAndContentListsInNamedFieldsBeforeCallingModel() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        ChatBackedDecisionModel adapter = new ChatBackedDecisionModel(new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                calls.incrementAndGet();
                throw new AssertionError("unsupported content must not reach classifier");
            }
        });
        Object image = dev.langchain4j.data.message.ImageContent.from("https://example.com/photo.png");
        for (Object value : java.util.Arrays.asList(image, java.util.Collections.singletonList(image))) {
            DecisionRequest request = DecisionRequest.builder()
                    .inputFields(java.util.Collections.singletonMap("photo", value))
                    .question("classify", ChoiceQuestion.builder().text("level")
                            .option("ECONOMY").option("ADVANCED").build()).build();
            try {
                adapter.decide(request);
                fail("must reject unsupported content");
            } catch (dev.langchain4j.exception.UnsupportedFeatureException expected) {
                assertEquals(0, calls.get());
            }
        }
    }

    @Test
    public void plainNamedFieldsRemainSupported() {
        DecisionRequest request = DecisionRequest.builder()
                .inputFields(java.util.Collections.singletonMap("question", "summary"))
                .question("classify", ChoiceQuestion.builder().text("level")
                        .option("ECONOMY").option("ADVANCED").build()).build();
        DecisionResponse response = model("{\"answers\":[{\"name\":\"classify\",\"value\":\"ECONOMY\",\"refused\":false}]}")
                .decide(request);
        assertEquals("ECONOMY", response.choice("classify").value());
    }

    private DecisionRequest request() {
        return DecisionRequest.builder().input("summary")
                .question("classify", ChoiceQuestion.builder().text("level")
                        .option("ECONOMY").option("ADVANCED").build()).build();
    }

    private ChatBackedDecisionModel model(String output) {
        return new ChatBackedDecisionModel(new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                assertTrue(request.toolSpecifications() == null || request.toolSpecifications().isEmpty());
                return ChatResponse.builder().aiMessage(AiMessage.from(output)).tokenUsage(new TokenUsage(7, 3)).build();
            }
        });
    }
}
