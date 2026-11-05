package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

/** Framework callbacks enrich the existing invocation; never allocate another admission lease here. */
public final class InvocationChatModelListener implements ChatModelListener {
    static final String INVOCATION_ATTRIBUTE = "datamaster.invocation";

    @Override
    public void onResponse(ChatModelResponseContext context) {
        Object value = context.attributes().get(INVOCATION_ATTRIBUTE);
        if (value instanceof AiInvocationGateway.Invocation) {
            markSuccess((AiInvocationGateway.Invocation) value, context.chatResponse().tokenUsage());
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        Object value = context.attributes().get(INVOCATION_ATTRIBUTE);
        if (value instanceof AiInvocationGateway.Invocation) {
            InvocationFailureClassifier.mark((AiInvocationGateway.Invocation) value, context.error());
        }
    }

    static void markSuccess(AiInvocationGateway.Invocation call, TokenUsage usage) {
        call.success(usage == null || usage.inputTokenCount() == null ? null : usage.inputTokenCount().longValue(),
                usage == null || usage.outputTokenCount() == null ? null : usage.outputTokenCount().longValue());
    }
}
