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
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
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
@AgentTool(risk = AgentToolRisk.READ_ONLY, permissions = {"ont:metric:query", "ont:metric:execute"})
public class ExecuteMetricAnalysisTool {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExecuteMetricAnalysisTool.class);
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
            MetricRespVO published = support.published(metricId, version, context);
            List<String> dimensions = input.getDimensionPropertyIds();
            if (dimensions != null && dimensions.size() > 5) throw new IllegalArgumentException("维度最多5个");
            MetricQueryReqVO request = new MetricQueryReqVO();
            request.setMetricId(metricId); request.setVersion(version);
            List<Long> dimensionIds = new ArrayList<>();
            if (dimensions != null) for (String dimension : dimensions) dimensionIds.add(support.positiveId(dimension));
            request.setDimensionPropertyIds(dimensionIds);
            request.setFilters(support.filters(input.getFilters()));
            request.setStartTime(optionalTime(input.getStartTime())); request.setEndTime(optionalTime(input.getEndTime()));
            boolean timeRestricted = request.getStartTime() != null || request.getEndTime() != null
                    || published.getTimePropertyId() != null && request.getFilters().stream()
                    .anyMatch(filter -> published.getTimePropertyId().equals(filter.getPropertyId()));
            AgentToolResult scopeCheck = com.datamaster.module.ai.agent.tool.AskTimeScope.validate(context, timeRestricted, input.getTimeScopeQuote());
            if (scopeCheck != null) return scopeCheck;
            if ((request.getStartTime() == null) != (request.getEndTime() == null)) {
                return unavailableParameters(context, "时间范围只提供了一端，正式指标查询尚未执行");
            }
            if (request.getStartTime() != null) {
                if (published.getTimePropertyId() == null) {
                    return unavailableParameters(context, "该指标没有已发布的时间属性，不能回答指定时间范围的问题");
                }
                var time = support.semanticProperty(published, published.getTimePropertyId());
                String type = time == null || !(time.get("dataType") instanceof String dataType)
                        ? null : dataType.toLowerCase(java.util.Locale.ROOT);
                if (!com.datamaster.module.ontology.service.metric.MetricRules.temporal(type)) {
                    throw new IllegalStateException("Invalid published time property");
                }
                String format = "date".equalsIgnoreCase(type) ? "YYYY-MM-DD" : "YYYY-MM-DDTHH:mm:ss（业务本地时间，不带时区后缀）";
                try {
                    com.datamaster.module.ontology.service.analysis.MetricQueryBuilder.typedValue(request.getStartTime(), type);
                    com.datamaster.module.ontology.service.analysis.MetricQueryBuilder.typedValue(request.getEndTime(), type);
                } catch (com.datamaster.common.exception.ServiceException invalidRange) {
                    return unavailableParameters(context, "指标时间范围格式无效，应使用 " + format + "；本轮尚未执行查询");
                }
                boolean ordered = "date".equalsIgnoreCase(type)
                        ? java.time.LocalDate.parse(request.getStartTime()).isBefore(java.time.LocalDate.parse(request.getEndTime()))
                        : java.time.LocalDateTime.parse(request.getStartTime()).isBefore(java.time.LocalDateTime.parse(request.getEndTime()));
                if (!ordered) {
                    return unavailableParameters(context, "指标时间范围的开始时间必须早于结束时间；本轮尚未执行查询");
                }
            }
            if (dimensionIds.isEmpty()) {
                // A scalar aggregate has one result row; a model hint must not block it or limit source rows.
                request.setLimit(1);
            } else {
                Integer limit = input.getLimit();
                if (limit != null && (limit < 1 || limit > 100)) {
                    return unavailableParameters(context, "分组查询的返回行数须为 1 到 100，也可省略；本轮尚未执行查询");
                }
                request.setLimit(support.limit(limit, 100, 100));
            }
            return queryAndFormat(request, published, context);
        } catch (Exception ex) {
            AgentToolResult classified = com.datamaster.module.ai.agent.runtime.AgentToolRejection.classifiedFailure(ex,
                    context == null ? null : context.getOperationId());
            if (classified != null) return classified;
            var diagnostic = com.datamaster.module.ai.agent.runtime.AgentToolFailureDiagnostics.capture(ex);
            log.error("指标查询工具失败 diagnosticId={} operationId={} category={} reason={}", diagnostic.getId(),
                    context == null ? null : context.getOperationId(), diagnostic.getCategory(), diagnostic.getReason(), diagnostic.getStack());
            AgentToolResult failed = support.result(AgentToolResult.FAILED, null,
                    "指标查询失败，诊断编号=" + diagnostic.getId(), context);
            failed.getMetadata().put("diagnosticId", diagnostic.getId());
            return failed;
        }
    }
    /** Replays only server-owned query conditions after the conversation scope has been checked. */
    public AgentToolResult replay(MetricQueryReqVO request) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:query", "ont:metric:execute");
        if (denied != null) return denied;
        return queryAndFormat(request, support.published(request.getMetricId(), request.getVersion(), context), context);
    }

    private AgentToolResult queryAndFormat(MetricQueryReqVO request, MetricRespVO published, AgentToolContext context) {
        MetricQueryRespVO response = metricAnalysisService.query(request);
        if (response == null) return support.result(AgentToolResult.FAILED, null, "指标分析没有返回结果", context);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metricId", String.valueOf(response.getMetricId() == null ? request.getMetricId() : response.getMetricId()));
        data.put("version", String.valueOf(response.getVersion() == null ? request.getVersion() : response.getVersion()));
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
        // Keep semantic scope visible to the reviewer while the replay request remains private.
        data.put("appliedDimensions", request.getDimensionPropertyIds());
        data.put("appliedFilters", request.getFilters());
        data.put("fixedFilters", published.getFixedFilters());
        data.put("queryDefinition", request);
        // Semantic metadata only: no physical source mapping or connection details.
        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("version", "METRIC_SCOPE_V1");
        scope.put("ontologyId", String.valueOf(published.getOntologyId()));
        scope.put("conceptId", String.valueOf(published.getConceptId()));
        scope.put("timePropertyId", published.getTimePropertyId() == null ? null : String.valueOf(published.getTimePropertyId()));
        scope.put("grain", published.getGrain());
        scope.put("fixedFilters", published.getFixedFilters());
        data.put("scopeContract", scope);
        AgentToolResult result = support.result(AgentToolResult.SUCCEEDED, data, "指标分析完成，可解释结果", context);
        result.setNextHints(Collections.singletonList("根据指标名称、单位和列口径解释结果；numeric 字符串原样保留，不要改写为近似数字"));
        return result;
    }

    private AgentToolResult unavailableParameters(AgentToolContext context, String message) {
        return AgentToolResult.recoverableInput(message, context.getOperationId());
    }

    private static String optionalTime(String value) {
        return value == null || value.isBlank() ? null : value;
    }

}
