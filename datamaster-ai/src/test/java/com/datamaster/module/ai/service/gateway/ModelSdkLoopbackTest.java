package com.datamaster.module.ai.service.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real upgraded SDK HTTP transport against loopback fixtures; no external provider or credentials. */
public class ModelSdkLoopbackTest {
    @Test public void sdkSerializesMessagesAndReadsUsageFromLocalProtocol() throws Exception {
        AtomicReference<JsonNode> input = new AtomicReference<>(); AtomicInteger calls = new AtomicInteger();
        HttpServer server = server(input, calls, 200, "{\"id\":\"local-1\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"local-test\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"离线回答\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":7,\"completion_tokens\":3,\"total_tokens\":10}}");
        try {
            ChatResponse response = model(server).chat(ChatRequest.builder()
                    .messages(SystemMessage.from("规则"), UserMessage.from("test exact id 9007199254740993")).build());
            assertEquals("离线回答", response.aiMessage().text());
            assertEquals(Integer.valueOf(7), response.tokenUsage().inputTokenCount());
            assertEquals(Integer.valueOf(10), response.tokenUsage().totalTokenCount());
            assertEquals("system", input.get().path("messages").get(0).path("role").asText());
            assertTrue(input.get().path("messages").get(1).path("content").asText().contains("9007199254740993"));
            assertEquals(1, calls.get());
        } finally { server.stop(0); }
    }

    @Test public void sdkPreservesToolCallIdsArgumentsAndToolResultContinuation() throws Exception {
        AtomicReference<JsonNode> input = new AtomicReference<>(); AtomicInteger calls = new AtomicInteger();
        HttpServer server = server(input, calls, 200, "{\"id\":\"local-2\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"local-test\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call-local\",\"type\":\"function\",\"function\":{\"name\":\"lookup\",\"arguments\":\"{\\\"id\\\":\\\"9007199254740993\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}]}");
        try {
            ChatModel model = model(server);
            ChatResponse response = model.chat(ChatRequest.builder().messages(UserMessage.from("lookup"))
                    .toolSpecifications(ToolSpecification.builder().name("lookup").description("local mock only").build()).build());
            var request = response.aiMessage().toolExecutionRequests().get(0);
            assertEquals("call-local", request.id()); assertEquals("lookup", request.name());
            assertEquals("{\"id\":\"9007199254740993\"}", request.arguments());
            model.chat(ChatRequest.builder().messages(UserMessage.from("lookup"), response.aiMessage(),
                    ToolExecutionResultMessage.from(request, "local-result")).build());
            JsonNode last = input.get().path("messages").get(2);
            assertEquals("tool", last.path("role").asText());
            assertEquals("call-local", last.path("tool_call_id").asText());
            assertEquals("local-result", last.path("content").asText());
            assertEquals(2, calls.get());
        } finally { server.stop(0); }
    }

    @Test public void providerHttpErrorPropagatesWithoutRetryOrInventedAnswer() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = server(new AtomicReference<>(), calls, 429, "{\"error\":{\"message\":\"local synthetic rate limit\",\"type\":\"rate_limit_error\",\"code\":\"rate_limit\"}}");
        try {
            try { model(server).chat("test"); fail("must propagate upstream failure"); }
            catch (RuntimeException expected) {
                boolean classified = false;
                for (Throwable cause = expected; cause != null; cause = cause.getCause()) {
                    if (cause instanceof dev.langchain4j.exception.HttpException http) {
                        assertEquals(429, http.statusCode()); classified = true; break;
                    }
                }
                assertTrue("SDK must preserve HTTP status", classified);
            }
            assertEquals(1, calls.get());
        } finally { server.stop(0); }
    }

    @Test public void realAiServicesCompletesLocalModelToolModelLoop() throws Exception {
        AtomicInteger requests = new AtomicInteger(); AtomicInteger tools = new AtomicInteger();
        AtomicReference<Throwable> handlerFailure = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            int round = requests.incrementAndGet();
            JsonNode body = new ObjectMapper().readTree(exchange.getRequestBody());
            String message;
            if (round == 1) {
                message = "{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call-loop\",\"type\":\"function\",\"function\":{\"name\":\"lookup\",\"arguments\":\"{}\"}}]}";
            } else {
                boolean resultFound = false;
                for (JsonNode item : body.path("messages")) {
                    if ("tool".equals(item.path("role").asText()) && "call-loop".equals(item.path("tool_call_id").asText())
                            && "local-safe-result".equals(item.path("content").asText())) resultFound = true;
                }
                if (!resultFound || round != 2) handlerFailure.set(new AssertionError("unexpected tool continuation payload"));
                message = "{\"role\":\"assistant\",\"content\":\"done from local fixture\"}";
            }
            String output = "{\"id\":\"local-loop\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"local-test\",\"choices\":[{\"index\":0,\"message\":"
                    + message + ",\"finish_reason\":\"" + (round == 1 ? "tool_calls" : "stop") + "\"}]}";
            byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        try {
            var specification = ToolSpecification.builder().name("lookup").description("mock lookup, no business I/O").build();
            dev.langchain4j.service.tool.ToolExecutor executor = (request, memoryId) -> {
                assertEquals("call-loop", request.id()); tools.incrementAndGet(); return "local-safe-result";
            };
            LocalAssistant assistant = dev.langchain4j.service.AiServices.builder(LocalAssistant.class).chatModel(model(server))
                    .tools(java.util.Collections.singletonMap(specification, executor))
                    .toolExecutionErrorHandler(dev.langchain4j.service.tool.ToolExecutionErrorHandler.failInvocation()).build();
            assertEquals("done from local fixture", assistant.chat("local integration test"));
            assertEquals(2, requests.get()); assertEquals(1, tools.get()); assertNull(handlerFailure.get());
        } finally { server.stop(0); }
    }

    public interface LocalAssistant { String chat(String question); }

    private ChatModel model(HttpServer server) {
        return OpenAiChatModel.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                .apiKey("local-test-placeholder").modelName("local-test").maxRetries(0)
                .timeout(Duration.ofSeconds(10)).logRequests(false).logResponses(false).build();
    }
    private HttpServer server(AtomicReference<JsonNode> input, AtomicInteger calls, int status, String output) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet(); input.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start(); return server;
    }
}
