package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryColumnVO;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryReqVO;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryRespVO;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executes only an existing published metric query; never accepts arbitrary SQL. */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class ExecuteMetricAnalysisTool {
    @Resource private IMetricAnalysisService metricAnalysisService;
    @Resource private MetricAgentToolSupport support;

    @Tool(name = "execute_metric_analysis", value = "执行已发布指标的只读分析。必须先调用 search_metrics 并选择其真实 metricId 和 publishedVersion；只使用结果中的允许维度和结构化过滤条件，禁止自由 SQL。只查询、不保存分析、不生成图表、不执行动作。")
    public AgentToolResult execute(AgentToolRequest.ExecuteMetricAnalysis input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:query", "ont:metric:execute");
        if (denied != null) return denied;
        try {
            if (input == null || input.getMetricId() == null || input.getVersion() == null) return support.result(AgentToolResult.NEED_INPUT, null, "请先选择 search_metrics 返回的真实指标和已发布版本", context);
            Long metricId = support.positiveId(input.getMetricId());
            Integer version = support.positiveVersion(input.getVersion());
            if (!support.wasOffered(metricId, version, context)) {
                return support.result(AgentToolResult.NEED_INPUT, null, "必须先调用 search_metrics 并原样使用其真实指标和发布版本", context);
            }
            support.published(metricId, version, context);
            List<String> dimensions = input.getDimensionPropertyIds();
            if (dimensions != null && dimensions.size() > 5) throw new IllegalArgumentException("维度最多5个");
            MetricQueryReqVO request = new MetricQueryReqVO();
            request.setMetricId(metricId); request.setVersion(version);
            List<Long> dimensionIds = new ArrayList<>();
            if (dimensions != null) for (String dimension : dimensions) dimensionIds.add(support.positiveId(dimension));
            request.setDimensionPropertyIds(dimensionIds);
            request.setFilters(support.filters(input.getFilters()));
            request.setStartTime(input.getStartTime()); request.setEndTime(input.getEndTime());
            request.setLimit(support.limit(input.getLimit(), 100, 100));
            MetricQueryRespVO response = metricAnalysisService.query(request);
            if (response == null) return support.result(AgentToolResult.FAILED, null, "指标分析没有返回结果", context);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("metricId", String.valueOf(response.getMetricId() == null ? metricId : response.getMetricId()));
            data.put("version", String.valueOf(response.getVersion() == null ? version : response.getVersion()));
            data.put("metricName", response.getMetricName()); data.put("unit", response.getUnit());
            List<Map<String, Object>> columns = new ArrayList<>();
            if (response.getColumns() != null) for (MetricQueryColumnVO column : response.getColumns()) {
                if (column == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", column.getKey()); item.put("label", column.getLabel()); item.put("dataType", column.getDataType()); columns.add(item);
            }
            data.put("columns", columns);
            List<Map<String, Object>> sourceRows = response.getRows() == null ? Collections.<Map<String, Object>>emptyList() : response.getRows();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < sourceRows.size() && i < 20; i++) {
                Map<String, Object> row = sourceRows.get(i);
                rows.add(row == null ? Collections.<String, Object>emptyMap() : new LinkedHashMap<>(row));
            }
            data.put("rows", rows);
            data.put("truncated", Boolean.TRUE.equals(response.getTruncated()) || sourceRows.size() > rows.size());
            data.put("notice", response.getNotice());
            data.put("executedAt", response.getExecutedAt());
            data.put("elapsedMs", response.getElapsedMs());
            data.put("limit", response.getLimit());
            data.put("returnedRowCount", rows.size());
            data.put("startTime", request.getStartTime());
            data.put("endTime", request.getEndTime());
            data.put("queryDefinition", request);
            AgentToolResult result = support.result(AgentToolResult.SUCCEEDED, data, "指标分析完成，可解释结果", context);
            result.setNextHints(Collections.singletonList("根据指标名称、单位和列口径解释结果；numeric 字符串原样保留，不要改写为近似数字"));
            return result;
        } catch (Exception ex) {
            return support.result(AgentToolResult.FAILED, null, "指标分析失败；请检查发布版本、允许维度和过滤条件", context);
        }
    }

}
