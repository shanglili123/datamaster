package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.runtime.DecisionAgentService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class DecisionToolSchemaTest {
    @Test
    public void decisionToolsHaveConcreteJava8SchemasAndNoDispatchTool() {
        int count = 0;
        for (Class<?> type : new Class<?>[] { DecisionAgentService.DecisionTools.class, DecisionAgentService.PlanningTools.class }) {
            for (Method method : type.getMethods()) {
                Tool tool = method.getAnnotation(Tool.class);
                if (tool == null) continue;
                assertNotNull(ToolSpecifications.toolSpecificationFrom(method));
                assertFalse(tool.name().contains("submit"));
                assertFalse(tool.name().contains("execute"));
                count++;
            }
        }
        assertEquals(3, count);
    }
}
