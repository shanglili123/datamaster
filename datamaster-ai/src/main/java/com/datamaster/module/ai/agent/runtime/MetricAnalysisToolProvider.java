package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Request-local tool visibility. Does not replace tool-side identity, version or permission checks. */
final class MetricAnalysisToolProvider implements ToolProvider {
    private final Map<ToolSpecification, ToolExecutor> tools;
    private final String operationId;

    MetricAnalysisToolProvider(Map<ToolSpecification, ToolExecutor> tools, String operationId) {
        this.tools = new LinkedHashMap<>(tools);
        this.operationId = operationId;
    }

    @Override public boolean isDynamic() { return true; }

    @Override public ToolProviderResult provideTools(ToolProviderRequest request) {
        boolean offered = hasOfferedMetric();
        boolean definition = offered && hasSuccessfulDefinition();
        Map<ToolSpecification, ToolExecutor> visible = new LinkedHashMap<>();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
            String name = entry.getKey().name();
            if ("search_metrics".equals(name) || offered && "get_metric_definition".equals(name) || definition) {
                visible.put(entry.getKey(), entry.getValue());
            }
        }
        return ToolProviderResult.builder().addAll(visible).build();
    }

    private boolean hasOfferedMetric() {
        for (AgentToolResult result : AgentToolContextHolder.results()) {
            if (!succeeded(result, "search_metrics") || !(result.getData() instanceof Map)) continue;
            Object rows = ((Map<?, ?>) result.getData()).get("rows");
            if (!(rows instanceof List)) continue;
            for (Object item : (List<?>) rows) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> row = (Map<?, ?>) item;
                if ("PUBLISHED".equals(row.get("status")) && row.get("id") != null
                        && row.get("publishedVersion") != null && !row.containsKey("metadataNotice")) return true;
            }
        }
        return false;
    }

    private boolean hasSuccessfulDefinition() {
        for (AgentToolResult result : AgentToolContextHolder.results()) {
            if (succeeded(result, "get_metric_definition")) return true;
        }
        return false;
    }

    private boolean succeeded(AgentToolResult result, String name) {
        return result != null && operationId != null && operationId.equals(result.getOperationId())
                && AgentToolResult.SUCCEEDED.equals(result.getStatus()) && Objects.equals(name, result.getToolName());
    }
}
