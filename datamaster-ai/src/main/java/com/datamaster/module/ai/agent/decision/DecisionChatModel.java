package com.datamaster.module.ai.agent.decision;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Set;

/** Budget checks apply to every framework model round, not just explicit tool requests. */
public final class DecisionChatModel implements ChatLanguageModel {
    private final ChatLanguageModel delegate;
    private final DecisionBudget budget;

    public DecisionChatModel(ChatLanguageModel delegate, DecisionBudget budget) {
        this.delegate = delegate;
        this.budget = budget;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        budget.use("正在组织决策分析");
        Response<AiMessage> result = delegate.generate(messages); budget.check(); return result;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> tools) {
        budget.use("正在组织决策分析");
        Response<AiMessage> result = delegate.generate(messages, tools); budget.check(); return result;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification tool) {
        budget.use("正在组织决策分析");
        Response<AiMessage> result = delegate.generate(messages, tool); budget.check(); return result;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        budget.use("正在组织决策分析");
        ChatResponse result = delegate.chat(request); budget.check(); return result;
    }

    @Override
    public Set<Capability> supportedCapabilities() { return delegate.supportedCapabilities(); }
}
