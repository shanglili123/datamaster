package com.datamaster.module.ai.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** Guard every Tool's model-facing ID contract, not just datasource creation. */
public class AgentToolIdContractTest {

    private static final String ID = "2100228457732182017";

    @Test
    public void allRequestIdsMustBeStrings() {
        int checked = 0;
        for (Class<?> dto : AgentToolRequest.class.getDeclaredClasses()) {
            for (Field field : dto.getDeclaredFields()) {
                if (field.getName().endsWith("Id")) {
                    assertEquals(dto.getName() + "." + field.getName(), String.class, field.getType());
                    checked++;
                } else if (field.getName().endsWith("Ids")) {
                    assertEquals(List.class, field.getType());
                    assertEquals(String.class, ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
                    checked++;
                }
            }
        }
        assertTrue("No request IDs inspected", checked > 0);
    }

    @Test
    public void everyAnnotatedToolMustGenerateStringIdSchemas() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(AgentTool.class));
        int checked = 0;
        for (BeanDefinition bean : scanner.findCandidateComponents("com.datamaster.module.ai")) {
            Class<?> type = Class.forName(bean.getBeanClassName());
            for (Method method : type.getMethods()) {
                if (method.getAnnotation(Tool.class) == null) continue;
                ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
                if (specification.parameters() != null) assertIdSchemas(specification.parameters().properties());
                checked++;
            }
        }
        assertTrue("No framework Tool schemas inspected", checked > 0);
    }

    @SuppressWarnings("unchecked")
    private void assertIdSchemas(Map<String, ?> properties) {
        if (properties == null) return;
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            if (!(entry.getValue() instanceof Map)) continue;
            Map<String, Object> schema = (Map<String, Object>) entry.getValue();
            if (entry.getKey().endsWith("Id")) assertEquals(entry.getKey(), "string", schema.get("type"));
            if (entry.getKey().endsWith("Ids")) {
                assertEquals(entry.getKey(), "array", schema.get("type"));
                assertEquals("string", ((Map<String, Object>) schema.get("items")).get("type"));
            }
            if (schema.get("properties") instanceof Map) assertIdSchemas((Map<String, Object>) schema.get("properties"));
        }
    }

    @Test
    public void frameworkExecutorMustPreserveSnowflakeId() throws Exception {
        SampleTool tool = new SampleTool();
        Method method = SampleTool.class.getMethod("execute", AgentToolRequest.PublishProbeTask.class);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put(method.getParameters()[0].getName(), java.util.Collections.singletonMap("taskId", ID));
        new DefaultToolExecutor(tool, method).execute(ToolExecutionRequest.builder()
                .id("test").name("id_test").arguments(JSON.toJSONString(arguments)).build(), null);
        assertEquals(ID, tool.observedId);
        assertEquals(Long.valueOf(ID), AgentToolIds.longValue(tool.observedId));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nestedOutputsAndConfirmationMetadataMustKeepIdsAsStrings() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("taskId", Long.valueOf(ID));
        nested.put("actionIds", Arrays.asList(Long.valueOf(ID)));
        nested.put("datasource_id", Long.valueOf(ID));
        nested.put("port", 5432);
        AgentToolResult result = AgentToolResult.of(AgentToolResult.SUCCEEDED,
                java.util.Collections.singletonMap("tasks", Arrays.asList(nested)), "ok", "operation");
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Map<String, Object> task = (Map<String, Object>) ((List<?>) data.get("tasks")).get(0);
        assertEquals(ID, task.get("taskId"));
        assertEquals(Arrays.asList(ID), task.get("actionIds"));
        assertEquals(ID, task.get("datasource_id"));
        assertEquals(5432, task.get("port"));
        result.setMetadata(java.util.Collections.singletonMap("arguments", nested));
        assertEquals(ID, ((Map<?, ?>) result.getMetadata().get("arguments")).get("taskId"));
    }

    @Test
    public void invalidNumericFormatsMustNotBeSilentlyTruncated() {
        for (String invalid : Arrays.asList("2100228457732182017.0", "2.100228457732182e18", "9223372036854775808")) {
            try {
                AgentToolIds.longValue(invalid);
                fail("Accepted invalid ID: " + invalid);
            } catch (ServiceException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    public void placeholderIdsMustTellAgentToResolveByName() {
        try {
            AgentToolIds.longValue("ds-789");
            fail("Accepted placeholder datasource ID");
        } catch (ServiceException expected) {
            assertTrue(expected.getMessage().contains("list_datasources"));
            assertTrue(expected.getMessage().contains("get_datasource"));
        }
    }

    public static class SampleTool {
        private String observedId;

        @Tool(name = "id_test", value = "ID contract test")
        public String execute(AgentToolRequest.PublishProbeTask input) {
            observedId = input.getTaskId();
            return observedId;
        }
    }
}
