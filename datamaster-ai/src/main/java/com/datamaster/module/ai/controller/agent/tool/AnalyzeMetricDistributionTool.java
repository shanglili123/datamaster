package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.analysis.*;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.*;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Map;

@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class AnalyzeMetricDistributionTool {
    @Resource private MetricAgentToolSupport support;

    @Tool(name = "analyze_distribution", value = "对本轮execute_metric_analysis的完整单维数值结果做排名、最小最大值分析。不接收模型数据行，拒绝截断结果；不计算跨组占比、集中度或指标总和，不查询新数据。")
    public AgentToolResult execute(AgentToolRequest.AnalyzeMetricResult input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:query", "ont:metric:execute");
        if (denied != null) return denied;
        try {
            Long id = support.positiveId(input == null ? null : input.getMetricId());
            Integer version = support.positiveVersion(input == null ? null : input.getVersion());
            support.published(id, version, context);
            Map<?, ?> evidence = MetricResultEvidence.latest(String.valueOf(id), String.valueOf(version), context);
            return support.result(AgentToolResult.SUCCEEDED, MetricResultAnalysis.analyze(evidence, false), "完整分组排名已计算", context);
        } catch (Exception ex) {
            return support.result(AgentToolResult.NEED_INPUT, null, "请先执行相同指标版本的完整单维数值查询；最多20组且不能截断", context);
        }
    }
}
