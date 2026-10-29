package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.analysis.MetricPeriodComparison;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.*;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryReqVO;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryRespVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricFilterVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/** 两次受控总量查询，共用版本与过滤；不在模型中计算正式差值和增长率。 */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class CompareMetricPeriodsTool {
    @Resource private MetricAgentToolSupport support;
    @Resource private IMetricAnalysisService analysisService;

    @Tool(name = "compare_periods", value = "比较本轮检索指标的本期和基期总量。必须明确同一指标版本、两个非重叠时间范围以及共用过滤；只支持数值总量，不分组、不自动推测同比环比日期。后端确定性计算差值及增长率；基期零/负数或无数据会明确标记，不自由SQL、不保存分析。")
    public AgentToolResult execute(AgentToolRequest.CompareMetricPeriods input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:query", "ont:metric:execute");
        if (denied != null) return denied;
        try {
            if (input == null) return support.result(AgentToolResult.NEED_INPUT, null, "请提供指标版本及明确的本期、基期时间范围", context);
            Long id = support.positiveId(input.getMetricId());
            Integer version = support.positiveVersion(input.getVersion());
            if (!support.wasOffered(id, version, context)) return support.result(AgentToolResult.NEED_INPUT, null, "请先search_metrics并选择其返回的指标版本", context);
            MetricRespVO snapshot = support.published(id, version, context);
            Map<String, Object> time = support.semanticProperty(snapshot, snapshot.getTimePropertyId());
            if (time == null) return support.result(AgentToolResult.NEED_INPUT, null, "该指标没有已发布的时间属性，不能比较时间段", context);
            boolean sameDuration = MetricPeriodComparison.validateRanges((String) time.get("dataType"),
                    input.getCurrentStartTime(), input.getCurrentEndTime(), input.getBaselineStartTime(), input.getBaselineEndTime());
            List<MetricFilterVO> filters = support.filters(input.getFilters());
            // 避免共享时间过滤与两段范围交叉，造成其中一期被意外过滤为空。
            for (MetricFilterVO filter : filters) {
                if (!snapshot.getDimensionPropertyIds().contains(filter.getPropertyId())
                        || snapshot.getTimePropertyId().equals(filter.getPropertyId())) {
                    return support.result(AgentToolResult.NEED_INPUT, null, "两期共用过滤仅支持非时间的已发布维度", context);
                }
            }
            MetricQueryReqVO current = query(id, version, filters, input.getCurrentStartTime(), input.getCurrentEndTime());
            MetricQueryReqVO baseline = query(id, version, filters, input.getBaselineStartTime(), input.getBaselineEndTime());
            // 两个计划都先校验，再发起第一条外部查询；每次执行仍独立经过服务端授权。
            com.datamaster.module.ontology.service.analysis.MetricQueryBuilder.Plan currentPlan =
                    com.datamaster.module.ontology.service.analysis.MetricQueryBuilder.compile(snapshot, current);
            com.datamaster.module.ontology.service.analysis.MetricQueryBuilder.compile(snapshot, baseline);
            if (!com.datamaster.module.ontology.service.metric.MetricRules.numeric(currentPlan.getColumns().get(0).getDataType())) {
                return support.result(AgentToolResult.NEED_INPUT, null, "时间段比较仅支持数值型指标", context);
            }
            MetricQueryRespVO currentResult = analysisService.query(current);
            MetricQueryRespVO baselineResult = analysisService.query(baseline);
            Map<String, Object> data = MetricPeriodComparison.calculate(value(currentResult, id, version), value(baselineResult, id, version));
            data.put("metricId", String.valueOf(id)); data.put("version", String.valueOf(version));
            data.put("metricName", snapshot.getName()); data.put("unit", snapshot.getUnit());
            data.put("currentPeriod", period(input.getCurrentStartTime(), input.getCurrentEndTime(), currentResult));
            data.put("baselinePeriod", period(input.getBaselineStartTime(), input.getBaselineEndTime(), baselineResult));
            data.put("equalDuration", sameDuration);
            data.put("notice", "按相同发布版本和过滤分别查询两期总量；区间[start,end)。"
                    + (sameDuration ? "两期时长相同。" : "两期时长不同，未按天或时长归一化，不能直接认定为标准同比/环比。")
                    + "两次查询不保证同一数据快照；执行时间不是刷新时间。基期为零/负数时不定义增长率；不做因果归因。");
            return support.result(AgentToolResult.SUCCEEDED, data, "两期总量比较完成；差值及增长率由后端十进制计算", context);
        } catch (Exception ex) {
            return support.result(AgentToolResult.FAILED, null, "时间段比较失败，请核对日期、数值指标口径、过滤及执行授权", context);
        }
    }

    private MetricQueryReqVO query(Long id, Integer version, List<MetricFilterVO> filters, String start, String end) {
        MetricQueryReqVO request = new MetricQueryReqVO();
        request.setMetricId(id); request.setVersion(version); request.setFilters(filters);
        request.setDimensionPropertyIds(Collections.emptyList());
        request.setStartTime(start); request.setEndTime(end); request.setLimit(1);
        return request;
    }

    private String value(MetricQueryRespVO result, Long id, Integer version) {
        if (result == null || !id.equals(result.getMetricId()) || !version.equals(result.getVersion())
                || !Boolean.FALSE.equals(result.getTruncated()) || result.getRows() == null || result.getRows().size() != 1
                || result.getColumns() == null || result.getColumns().size() != 1
                || !"metric_value".equals(result.getColumns().get(0).getKey())
                || !com.datamaster.module.ontology.service.metric.MetricRules.numeric(result.getColumns().get(0).getDataType())) {
            throw new IllegalArgumentException("必须是未截断的单值数值指标结果");
        }
        Map<String, Object> row = result.getRows().get(0);
        if (row == null || !row.containsKey("metric_value")) throw new IllegalArgumentException("缺少数值");
        Object value = row.get("metric_value");
        if (value != null && !(value instanceof String)) throw new IllegalArgumentException("数值必须保留十进制字符串");
        return (String) value;
    }

    private Map<String, Object> period(String start, String end, MetricQueryRespVO result) {
        Map<String, Object> period = new LinkedHashMap<>();
        period.put("startTime", start); period.put("endTime", end);
        period.put("executedAt", result.getExecutedAt()); period.put("elapsedMs", result.getElapsedMs());
        return period;
    }
}
