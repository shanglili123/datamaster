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
    private final boolean allowDataQa;

    MetricAnalysisToolProvider(Map<ToolSpecification, ToolExecutor> tools, String operationId) {
        this(tools, operationId, false);
    }

    MetricAnalysisToolProvider(Map<ToolSpecification, ToolExecutor> tools, String operationId, boolean allowDataQa) {
        this.tools = new LinkedHashMap<>(tools);
        this.operationId = operationId;
        this.allowDataQa = allowDataQa;
    }

    @Override public boolean isDynamic() { return true; }

    @Override public ToolProviderResult provideTools(ToolProviderRequest request) {
        boolean offered = hasOfferedMetric();
        boolean definition = offered && hasSuccessfulDefinition();
        boolean hasQuery = AgentToolContextHolder.results().stream().anyMatch(result -> succeeded(result, "execute_metric_analysis"));
        Map<ToolSpecification, ToolExecutor> visible = new LinkedHashMap<>();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
            String name = entry.getKey().name();
            boolean resultAnalysis = List.of("analyze_distribution", "analyze_trend", "build_chart_spec").contains(name);
            if (resultAnalysis ? hasQuery : "search_metrics".equals(name) || offered && "get_metric_definition".equals(name) || definition) {
                visible.put(entry.getKey(), entry.getValue());
            }
        }
        if (allowDataQa && MetricDiscoveryState.mayChooseDataQa(operationId)) {
            ToolSpecification selectDataQa = ToolSpecification.builder().name("use_data_qa")
                    .description("已完整查看当前本体发布目录，确认没有与用户目标概念/粒度/度量相符的指标时，选择普通数据问答。不能用于权限拒绝、查询失败、参数缺失或目录截断。不要执行SQL。")
                    .build();
            visible.put(selectDataQa, (call, memoryId) -> {
                if (!MetricDiscoveryState.mayChooseDataQa(operationId)) throw new IllegalStateException("Metric discovery state changed");
                AgentToolResult selected = AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "没有适用指标，转普通数据问答", operationId);
                selected.setToolName("use_data_qa");
                selected.getMetadata().put("routingChoice", "NO_APPLICABLE_METRIC");
                AgentToolContextHolder.record(selected);
                throw new AgentToolFlowHaltException(selected);
            });
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
