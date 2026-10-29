package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricPageReqVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.IMetricService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only metric discovery. It never exposes mutable draft definitions or physical source metadata. */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class SearchMetricsTool {
    @Resource private IMetricService metricService;
    @Resource private MetricAgentToolSupport support;

    @Tool(name = "search_metrics", value = "查询当前空间的正式指标候选。必须先调用本工具，再使用结果中的真实 metricId 和已发布 version 调用 execute_metric_analysis；只返回稳定摘要，不返回定义、SQL、物理来源或凭据；不创建、不修改、不保存分析、不生成图表。")
    public AgentToolResult execute(AgentToolRequest.SearchMetrics input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:list", "ont:metric:query");
        if (denied != null) return denied;
        try {
            Long currentOntology = support.currentOntology(context);
            String requestedId = text(input == null ? null : input.getOntologyId());
            Long ontologyId = requestedId == null ? currentOntology : support.positiveId(requestedId);
            // A model-provided ontology is only acceptable when it is already the active
            // operation/page ontology; callers cannot pivot to another ontology in-space.
            if (ontologyId == null) return support.result(AgentToolResult.NEED_INPUT, null, "请先确定当前本体", context);
            if (currentOntology != null && !currentOntology.equals(ontologyId)) {
                return support.result(AgentToolResult.NEED_INPUT, null, "请在当前本体流程中检索指标", context);
            }
            support.assertOntology(ontologyId, context);
            MetricPageReqVO request = new MetricPageReqVO();
            request.setOntologyId(ontologyId);
            request.setName(text(input == null ? null : input.getName()));
            String status = text(input == null ? null : input.getStatus());
            if (status != null && !"PUBLISHED".equals(status)) {
                return support.result(AgentToolResult.NEED_INPUT, null, "分析工具只检索已发布指标；草稿和停用指标请在指标管理查看", context);
            }
            request.setStatus("PUBLISHED");
            request.setPageNum(1);
            request.setPageSize(support.limit(input == null ? null : input.getLimit(), 20, 50));
            PageResult<MetricRespVO> page = metricService.getPage(request);
            List<MetricRespVO> candidates = page == null || page.getRows() == null
                    ? Collections.<MetricRespVO>emptyList() : page.getRows();
            String code = text(input == null ? null : input.getCode());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (MetricRespVO metric : candidates) {
                if (metric == null || (code != null && !code.equalsIgnoreCase(metric.getCode()))) continue;
                rows.add(summary(metric));
            }
            long total = page == null || page.getTotal() == null ? candidates.size() : page.getTotal();
            boolean complete = total <= candidates.size();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("rows", rows);
            data.put("total", total);
            data.put("searchComplete", complete);
            data.put("uniqueMatch", complete && rows.size() == 1);
            data.put("searchedCode", code);
            data.put("searchedOntologyId", String.valueOf(ontologyId));
            AgentToolResult result = support.result(AgentToolResult.SUCCEEDED, data,
                    rows.isEmpty() ? (complete ? "当前条件未找到已发布指标" : "当前首屏候选未命中；searchComplete=false，请缩小名称条件，不能断言指标不存在")
                            : "已返回指标候选；多条候选需请用户选择，唯一匹配时按返回的已发布口径和维度继续分析",
                    context);
            result.setToolName("search_metrics");
            return result;
        } catch (Exception ex) {
            return support.result(AgentToolResult.FAILED, null, "指标检索失败，请检查当前本体和查询条件", context);
        }
    }

    private Map<String, Object> summary(MetricRespVO metric) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", metric.getId() == null ? null : String.valueOf(metric.getId()));
        row.put("code", metric.getCode());
        row.put("name", metric.getName());
        row.put("status", metric.getStatus());
        Integer versionNumber = metric.getPublishedVersion();
        row.put("publishedVersion", versionNumber == null ? null : String.valueOf(versionNumber));
        if ("PUBLISHED".equals(metric.getStatus()) && versionNumber != null && versionNumber > 0) {
            try {
                MetricRespVO published = metricService.version(metric.getId(), versionNumber);
                row.put("name", published.getName());
                row.put("unit", published.getUnit());
                row.put("grain", published.getGrain());
                row.put("aggregation", published.getAggregation());
                row.put("timeProperty", property(published, published.getTimePropertyId()));
                row.put("requiresTimeRange", published.getTimePropertyId() != null);
                row.put("timeRangeConvention", "[startTime,endTime)，使用源数据业务时间，不自动分桶");
                row.put("description", published.getDescription());
                row.put("allowedDimensions", dimensions(published));
            } catch (Exception ignored) {
                row.put("unit", null); row.put("description", null);
                row.put("allowedDimensions", Collections.emptyList());
                row.put("metadataNotice", "发布版本元数据不可用");
            }
        } else {
            row.put("unit", null); row.put("description", null);
            row.put("allowedDimensions", Collections.emptyList());
            if ("PUBLISHED".equals(metric.getStatus())) row.put("metadataNotice", "已发布指标缺少有效发布版本号");
        }
        return row;
    }

    private Map<String, String> property(MetricRespVO published, Long id) {
        if (id == null) return null;
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(id));
        Map<?, ?> source = published.getSourceSnapshot();
        Object mappings = source == null ? null : source.get("propertyColumns");
        Object raw = mappings instanceof Map ? ((Map<?, ?>) mappings).get(String.valueOf(id)) : null;
        if (raw instanceof Map) {
            Map<?, ?> field = (Map<?, ?>) raw;
            result.put("name", field.get("propertyName") == null ? null : String.valueOf(field.get("propertyName")));
            result.put("dataType", field.get("dataType") == null ? null : String.valueOf(field.get("dataType")));
        }
        return result;
    }

    private List<Map<String, String>> dimensions(MetricRespVO published) {
        if (published.getDimensionPropertyIds() == null) return Collections.emptyList();
        List<Map<String, String>> result = new ArrayList<>();
        for (Long id : published.getDimensionPropertyIds()) {
            if (id != null) result.add(property(published, id));
        }
        return result;
    }

    private String text(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
