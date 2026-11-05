package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.ChatRequestOptions;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Set;

/** Admission must remain outside listeners: framework listener exceptions are deliberately swallowed. */
public final class GovernedChatLanguageModel implements ChatModel {
    private final ChatModel delegate;
    private final AiInvocationGateway gateway;
    private final String scene;
    private final String model;
    private final String provider;

    public GovernedChatLanguageModel(ChatModel delegate, AiInvocationGateway gateway, String scene, String model) {
        this(delegate, gateway, scene, null, model);
    }

    public GovernedChatLanguageModel(ChatModel delegate, AiInvocationGateway gateway, String scene, String provider, String model) {
        this.delegate = delegate; this.gateway = gateway; this.scene = scene; this.model = model; this.provider = provider;
    }

    @Override
    public ChatResponse chat(ChatRequest request, ChatRequestOptions options) {
        try (AiInvocationGateway.Invocation call = gateway.begin("MODEL", scene, provider, model, false,
                AiInvocationGateway.snapshotContext())) {
            ChatRequestOptions.Builder requestOptions = ChatRequestOptions.builder();
            if (options != null) requestOptions.listenerAttributes(new java.util.LinkedHashMap<>(options.listenerAttributes()));
            requestOptions.addListenerAttribute(InvocationChatModelListener.INVOCATION_ATTRIBUTE, call);
            try {
                ChatResponse response = delegate.chat(request, requestOptions.build());
                if (response == null) throw new IllegalStateException("模型响应为空");
                // Idempotent fallback also covers custom models without listener support.
                InvocationChatModelListener.markSuccess(call, response.tokenUsage());
                return response;
            } catch (RuntimeException ex) {
                InvocationFailureClassifier.mark(call, ex);
                throw ex;
            }
        }
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() { return delegate.defaultRequestParameters(); }

    @Override
    public Set<Capability> supportedCapabilities() { return delegate.supportedCapabilities(); }
}
