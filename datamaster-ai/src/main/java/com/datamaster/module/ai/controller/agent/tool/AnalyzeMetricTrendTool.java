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
public class AnalyzeMetricTrendTool {
    @Resource private MetricAgentToolSupport support;

    @Tool(name = "analyze_trend", value = "分析本轮完整date维度查询结果，按日期排序、计算相邻日变化及首尾差值。不接收模型数据行，不自动分桶或补零，不预测、不做异常显著性或因果归因；拒绝截断及非date维度。")
    public AgentToolResult execute(AgentToolRequest.AnalyzeMetricResult input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:query", "ont:metric:execute");
        if (denied != null) return denied;
        try {
            Long id = support.positiveId(input == null ? null : input.getMetricId());
            Integer version = support.positiveVersion(input == null ? null : input.getVersion());
            support.published(id, version, context);
            Map<?, ?> evidence = MetricResultEvidence.latest(String.valueOf(id), String.valueOf(version), context);
            return support.result(AgentToolResult.SUCCEEDED, MetricResultAnalysis.analyze(evidence, true), "日期序列描述性变化已计算", context);
        } catch (Exception ex) {
            return support.result(AgentToolResult.NEED_INPUT, null, "请先按已发布date维度执行完整数值查询；最多20个日期且不能截断，不支持时间戳自动分桶", context);
        }
    }
}
