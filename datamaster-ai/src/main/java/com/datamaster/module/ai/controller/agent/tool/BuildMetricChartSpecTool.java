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
public class BuildMetricChartSpecTool {
    @Resource private MetricAgentToolSupport support;

    @Tool(name = "build_chart_spec", value = "基于本轮execute_metric_analysis的真实结果生成声明式table/bar/line配置。不接收模型提供的数据行、JS或HTML，不绘制或保存图表；截断或不适合的结果退回表格，不能宣称前端已渲染。")
    public AgentToolResult execute(AgentToolRequest.BuildMetricChartSpec input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:query", "ont:metric:execute");
        if (denied != null) return denied;
        try {
            Long id = support.positiveId(input == null ? null : input.getMetricId());
            Integer version = support.positiveVersion(input == null ? null : input.getVersion());
            support.published(id, version, context);
            Map<?, ?> evidence = MetricResultEvidence.latest(String.valueOf(id), String.valueOf(version), context);
            return support.result(AgentToolResult.SUCCEEDED, MetricChartSpec.build(evidence, input.getChartType()), "声明式展示配置已生成，尚未渲染图表", context);
        } catch (Exception ex) {
            return support.result(AgentToolResult.NEED_INPUT, null, "请先执行指标查询，再选择AUTO/TABLE或与完整结果兼容的BAR/LINE", context);
        }
    }
}
