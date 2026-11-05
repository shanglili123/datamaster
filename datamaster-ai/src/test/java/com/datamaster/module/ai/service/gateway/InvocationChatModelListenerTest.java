package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.ChatRequestOptions;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class InvocationChatModelListenerTest {
    private static class MemoryRepository extends AiInvocationRepository {
        private int rows;
        private AiInvocationGateway.Invocation last;
        MemoryRepository() { super(null); }
        @Override boolean insert(AiInvocationGateway.Invocation call) { rows++; last = call; return true; }
    }

    @Test
    public void frameworkListenerAndWrapperPersistOnlyOneMeasuredInvocation() {
        MemoryRepository repository = new MemoryRepository();
        AiInvocationGateway gateway = new AiInvocationGateway(1, 1, repository);
        ChatModel delegate = new ChatModel() {
            @Override public List<ChatModelListener> listeners() { return Collections.singletonList(new InvocationChatModelListener()); }
            @Override public ChatResponse doChat(ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from("ok")).tokenUsage(new TokenUsage(10, 5)).build();
            }
        };
        ChatModel model = new GovernedChatLanguageModel(delegate, gateway, "TEST", "TEST", "model");
        model.chat(ChatRequest.builder().messages(UserMessage.from("test")).build(), ChatRequestOptions.EMPTY);
        assertEquals(1, repository.rows);
        assertEquals("SUCCESS", repository.last.status);
        assertEquals(Long.valueOf(15), repository.last.totalTokens());
        assertEquals(Long.valueOf(0), gateway.metrics().get("inflight"));
    }

    @Test
    public void admissionRejectsBeforeProviderEvenWhenListenerErrorsWouldBeIgnored() {
        MemoryRepository repository = new MemoryRepository();
        AiInvocationGateway gateway = new AiInvocationGateway(1, 1, repository);
        AtomicInteger providerCalls = new AtomicInteger();
        ChatModel delegate = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                providerCalls.incrementAndGet();
                throw new AssertionError("must not call provider");
            }
        };
        ChatModel model = new GovernedChatLanguageModel(delegate, gateway, "TEST", "model");
        try (AiInvocationGateway.Invocation busy = gateway.begin("MODEL", "TEST", "model", false)) {
            try { model.chat("test"); fail("must reject"); }
            catch (RejectedExecutionException expected) { assertEquals(0, providerCalls.get()); }
        }
        assertEquals(Long.valueOf(1), gateway.metrics().get("rejected"));
        assertEquals(Long.valueOf(0), gateway.metrics().get("inflight"));
    }

    @Test
    public void sdkTimeoutWithoutJavaTimeoutCauseIsStillClassifiedAsTimeout() {
        MemoryRepository repository = new MemoryRepository();
        AiInvocationGateway gateway = new AiInvocationGateway(1, 1, repository);
        ChatModel delegate = new ChatModel() {
            @Override public List<ChatModelListener> listeners() { return Collections.singletonList(new InvocationChatModelListener()); }
            @Override public ChatResponse doChat(ChatRequest request) {
                throw new dev.langchain4j.exception.TimeoutException("sensitive upstream timeout");
            }
        };
        ChatModel model = new GovernedChatLanguageModel(delegate, gateway, "TEST", "model");
        try { model.chat("test"); fail("must propagate"); }
        catch (dev.langchain4j.exception.TimeoutException expected) { /* No message is persisted. */ }
        assertEquals(1, repository.rows);
        assertEquals("FAILURE", repository.last.status);
        assertEquals("TIMEOUT", repository.last.category);
        assertNull(repository.last.httpStatus);
        assertNull(repository.last.totalTokens());
        assertEquals(Long.valueOf(0), gateway.metrics().get("inflight"));
    }

    @Test
    public void httpFailureIsClassifiedWithoutPersistingProviderMessage() {
        MemoryRepository repository = new MemoryRepository();
        AiInvocationGateway gateway = new AiInvocationGateway(1, 1, repository);
        ChatModel delegate = new ChatModel() {
            @Override public List<ChatModelListener> listeners() { return Collections.singletonList(new InvocationChatModelListener()); }
            @Override public ChatResponse doChat(ChatRequest request) {
                throw new dev.langchain4j.exception.HttpException(429, "sensitive upstream response");
            }
        };
        ChatModel model = new GovernedChatLanguageModel(delegate, gateway, "TEST", "model");
        try { model.chat("test"); fail("must propagate"); }
        catch (dev.langchain4j.exception.HttpException expected) { assertEquals(429, expected.statusCode()); }
        assertEquals(1, repository.rows);
        assertEquals("HTTP", repository.last.category);
        assertEquals(Integer.valueOf(429), repository.last.httpStatus);
        assertNull(repository.last.totalTokens());
        assertEquals(Long.valueOf(0), gateway.metrics().get("inflight"));
    }
}
