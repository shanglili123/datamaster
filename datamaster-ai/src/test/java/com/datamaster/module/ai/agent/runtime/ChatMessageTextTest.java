package com.datamaster.module.ai.agent.runtime;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChatMessageTextTest {
    @Test
    public void readsConcreteMessageTextWithoutChatMessageTextApi() {
        assertEquals("user", ChatMessageText.text(UserMessage.from("user")));
        assertEquals("system", ChatMessageText.text(SystemMessage.from("system")));
        assertEquals("answer", ChatMessageText.text(AiMessage.from("answer")));
        assertEquals("result", ChatMessageText.text(ToolExecutionResultMessage.from("id", "tool", "result")));
        assertNull(ChatMessageText.text(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesToSilentlyCollapseMultipleUserContents() {
        ChatMessageText.text(UserMessage.from(TextContent.from("first"), TextContent.from("second")));
    }

    @Test
    public void toolOnlyAiMessageRetainsAbsentText() {
        ToolExecutionRequest request = ToolExecutionRequest.builder().id("id").name("tool").arguments("{}").build();
        assertNull(ChatMessageText.text(AiMessage.from(request)));
    }
}
