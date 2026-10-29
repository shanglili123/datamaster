package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** 每次框架模型请求独立准入，工具执行期间不占模型槽位；不改变原模型协议。 */
public final class GovernedChatLanguageModel implements ChatLanguageModel {
    private final ChatLanguageModel delegate;
    private final AiInvocationGateway gateway;
    private final String scene;
    private final String model;
    private final String provider;

    public GovernedChatLanguageModel(ChatLanguageModel delegate, AiInvocationGateway gateway, String scene, String model) {
        this(delegate, gateway, scene, null, model);
    }

    public GovernedChatLanguageModel(ChatLanguageModel delegate, AiInvocationGateway gateway, String scene, String provider, String model) {
        this.delegate = delegate; this.gateway = gateway; this.scene = scene; this.model = model; this.provider = provider;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return invoke(() -> delegate.generate(messages));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> tools) {
        return invoke(() -> delegate.generate(messages, tools));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification tool) {
        return invoke(() -> delegate.generate(messages, tool));
    }

    private Response<AiMessage> invoke(Supplier<Response<AiMessage>> action) {
        try (AiInvocationGateway.Invocation call = gateway.begin("MODEL", scene, provider, model, false, AiInvocationGateway.snapshotContext())) {
            try {
                Response<AiMessage> response = action.get();
                if (response == null) throw new IllegalStateException("模型响应为空");
                success(call, response.tokenUsage());
                return response;
            } catch (RuntimeException ex) {
                call.failure("PROVIDER", null);
                throw ex;
            }
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try (AiInvocationGateway.Invocation call = gateway.begin("MODEL", scene, provider, model, false, AiInvocationGateway.snapshotContext())) {
            try {
                ChatResponse response = delegate.chat(request);
                if (response == null) throw new IllegalStateException("模型响应为空");
                success(call, response.tokenUsage());
                return response;
            } catch (RuntimeException ex) {
                call.failure("PROVIDER", null);
                throw ex;
            }
        }
    }

    private void success(AiInvocationGateway.Invocation call, TokenUsage usage) {
        call.success(usage == null || usage.inputTokenCount() == null ? null : usage.inputTokenCount().longValue(),
                usage == null || usage.outputTokenCount() == null ? null : usage.outputTokenCount().longValue());
    }

    @Override
    public Set<Capability> supportedCapabilities() { return delegate.supportedCapabilities(); }
}
