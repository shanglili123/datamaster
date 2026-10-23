package com.datamaster.module.ai.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;

/** 保证平台治理注解与 LangChain4j Tool 元数据保持职责分离。 */
public class AgentToolAnnotationTest {

    @Test
    public void shouldKeepToolIdentityOnLangChain4jAnnotation() throws Exception {
        Method method = SampleTool.class.getDeclaredMethod("execute");
        Tool tool = method.getAnnotation(Tool.class);

        assertNotNull(tool);
        assertEquals("sample_tool", tool.name());
        assertEquals("示例 Tool", tool.value()[0]);
        assertFalse(Arrays.stream(AgentTool.class.getDeclaredMethods())
                .anyMatch(item -> "name".equals(item.getName()) || "description".equals(item.getName())));
    }

    @AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
    private static class SampleTool {

        @Tool(name = "sample_tool", value = "示例 Tool")
        public void execute() {
        }
    }
}
