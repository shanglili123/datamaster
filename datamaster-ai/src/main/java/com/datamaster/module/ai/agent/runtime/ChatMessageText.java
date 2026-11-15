package com.datamaster.module.ai.agent.runtime;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

/** Text-only memory compatibility for the current LangChain4j message API. */
final class ChatMessageText {
    private ChatMessageText() { }

    static String text(ChatMessage message) {
        if (message instanceof UserMessage user) {
            if (!user.hasSingleText()) {
                throw new IllegalArgumentException("Text-only chat memory cannot store multimodal user messages");
            }
            return user.singleText();
        }
        if (message instanceof AiMessage ai) return ai.text();
        if (message instanceof SystemMessage system) return system.text();
        if (message instanceof ToolExecutionResultMessage tool) return tool.text();
        if (message == null) return null;
        throw new IllegalArgumentException("Unsupported chat memory message type");
    }
}
