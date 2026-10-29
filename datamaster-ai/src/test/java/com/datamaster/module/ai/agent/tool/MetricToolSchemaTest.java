package com.datamaster.module.ai.agent.tool;

import com.datamaster.module.ai.controller.agent.tool.ExecuteMetricAnalysisTool;
import com.datamaster.module.ai.controller.agent.tool.SearchMetricsTool;
import com.datamaster.module.ai.controller.agent.tool.GetMetricDefinitionTool;
import com.datamaster.module.ai.controller.agent.tool.CompareMetricPeriodsTool;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 使用项目当前版本的框架检查具体 DTO Schema，避免 List<Map> 启动失败。 */
public class MetricToolSchemaTest {
    @Test
    public void searchUsesConcreteRequestSchema() throws Exception {
        assertEquals("search_metrics", ToolSpecifications.toolSpecificationFrom(
                SearchMetricsTool.class.getMethod("execute", AgentToolRequest.SearchMetrics.class)).name());
    }

    @Test
    public void definitionAndComparisonUseConcreteSchemas() throws Exception {
        assertEquals("get_metric_definition", ToolSpecifications.toolSpecificationFrom(
                GetMetricDefinitionTool.class.getMethod("execute", AgentToolRequest.GetMetricDefinition.class)).name());
        assertEquals("compare_periods", ToolSpecifications.toolSpecificationFrom(
                CompareMetricPeriodsTool.class.getMethod("execute", AgentToolRequest.CompareMetricPeriods.class)).name());
    }

    @Test
    public void remainingAnalysisToolsUseConcreteSchemas() throws Exception {
        assertEquals("analyze_distribution", ToolSpecifications.toolSpecificationFrom(
                com.datamaster.module.ai.controller.agent.tool.AnalyzeMetricDistributionTool.class.getMethod("execute", AgentToolRequest.AnalyzeMetricResult.class)).name());
        assertEquals("analyze_trend", ToolSpecifications.toolSpecificationFrom(
                com.datamaster.module.ai.controller.agent.tool.AnalyzeMetricTrendTool.class.getMethod("execute", AgentToolRequest.AnalyzeMetricResult.class)).name());
        assertEquals("build_chart_spec", ToolSpecifications.toolSpecificationFrom(
                com.datamaster.module.ai.controller.agent.tool.BuildMetricChartSpecTool.class.getMethod("execute", AgentToolRequest.BuildMetricChartSpec.class)).name());
        assertEquals("save_analysis", ToolSpecifications.toolSpecificationFrom(
                com.datamaster.module.ai.controller.agent.tool.SaveMetricAnalysisTool.class.getMethod("execute", AgentToolRequest.SaveMetricAnalysis.class)).name());
    }

    @Test
    public void analysisUsesConcreteFilterSchemaAndStringIds() throws Exception {
        assertNotNull(ToolSpecifications.toolSpecificationFrom(
                ExecuteMetricAnalysisTool.class.getMethod("execute", AgentToolRequest.ExecuteMetricAnalysis.class)).parameters());
        assertEquals(String.class, AgentToolRequest.ExecuteMetricAnalysis.class.getDeclaredField("metricId").getType());
        assertEquals(String.class, AgentToolRequest.AnalysisFilter.class.getDeclaredField("propertyId").getType());
        assertEquals(String.class, AgentToolRequest.AnalysisFilter.class.getDeclaredField("value").getType());
    }
}
