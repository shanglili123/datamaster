package com.datamaster.module.ai.agent.decision;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.ChatRequestOptions;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.Set;

/** Budget checks apply to every framework model round, not just explicit tool requests. */
public final class DecisionChatModel implements ChatModel {
    private final ChatModel delegate;
    private final DecisionBudget budget;

    public DecisionChatModel(ChatModel delegate, DecisionBudget budget) {
        this.delegate = delegate;
        this.budget = budget;
    }

    @Override
    public ChatResponse chat(ChatRequest request, ChatRequestOptions options) {
        budget.use("正在组织决策分析");
        ChatResponse result = delegate.chat(request, options);
        budget.check();
        return result;
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() { return delegate.defaultRequestParameters(); }

    @Override
    public Set<Capability> supportedCapabilities() { return delegate.supportedCapabilities(); }
}
