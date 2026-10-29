package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricFilterVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.IMetricService;
import java.util.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** Fail-closed boundary shared by the two read-only metric tools; not a model tool. */
@Component
public class MetricAgentToolSupport {
    @Resource private AgentToolResourceGuard resourceGuard;
    @Resource private IMetricService metricService;

    public AgentToolResult guard(AgentToolContext context, String... permissions) {
        try {
            if (context == null || context.getUserId() == null) {
                return result(AgentToolResult.NEED_INPUT, null, "请先登录并确定当前空间", context);
            }
            LoginUser login = SecurityUtils.getLoginUser();
            if (login == null || !context.getUserId().equals(login.getUserId())) {
                return result(AgentToolResult.FAILED, null, "登录用户与当前 Agent 上下文不一致", context);
            }
            for (String permission : permissions) {
                if (login.getPermissions() == null || !SecurityUtils.hasPermi(permission)) {
                    return result(AgentToolResult.FAILED, null, "当前用户没有所需指标权限", context);
                }
            }
            if (!resourceGuard.hasSpace(context)) {
                return result(AgentToolResult.NEED_INPUT, null, "请先确定当前用户可访问的空间", context);
            }
            return null;
        } catch (Exception ignored) {
            return result(AgentToolResult.FAILED, null, "无法验证当前登录用户、空间或指标权限", context);
        }
    }

    public Long currentOntology(AgentToolContext context) {
        Object pageId = context.getAttributes().get("ontologyId");
        if (pageId != null && !String.valueOf(pageId).isEmpty()) return positiveId(String.valueOf(pageId));
        AiAgentOperationDO operation = resourceGuard.currentOperation(context);
        if (operation == null) return null;
        boolean sameSpace = context.getSpaceId() != null && operation.getSpaceId() != null
                ? context.getSpaceId().equals(operation.getSpaceId())
                : context.getSpaceCode() != null && operation.getSpaceCode() != null
                    && context.getSpaceCode().equalsIgnoreCase(operation.getSpaceCode());
        if (!context.getUserId().equals(operation.getCreatorId()) || !sameSpace
                || !java.util.Objects.equals(context.getOperationId(), operation.getId())) {
            throw new IllegalArgumentException("流程上下文不匹配");
        }
        return operation.getOntologyId();
    }

    public void assertOntology(Long ontologyId, AgentToolContext context) {
        if (ontologyId == null || !resourceGuard.ontologyVisible(ontologyId, context)) {
            throw new IllegalArgumentException("本体不属于当前空间");
        }
    }

    public Long positiveId(String value) {
        // Do not trim, accept signs, scientific notation, or a floating-point ID.
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("ID 格式无效");
        return Long.valueOf(value);
    }

    public int limit(Integer value, int defaultValue, int maximum) {
        int limit = value == null ? defaultValue : value;
        if (limit < 1 || limit > maximum) throw new IllegalArgumentException("limit 超出范围");
        return limit;
    }

    public Integer positiveVersion(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,8}")) throw new IllegalArgumentException("版本必须为正整数");
        return Integer.valueOf(value);
    }

    public boolean wasOffered(Long metricId, Integer version, AgentToolContext context) {
        for (AgentToolResult result : AgentToolContextHolder.results()) {
            if (!"search_metrics".equals(result.getToolName()) || !AgentToolResult.SUCCEEDED.equals(result.getStatus())
                    || !Objects.equals(context.getOperationId(), result.getOperationId()) || !(result.getData() instanceof Map)) continue;
            Object rows = ((Map<?, ?>) result.getData()).get("rows");
            if (!(rows instanceof List)) continue;
            for (Object item : (List<?>) rows) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> row = (Map<?, ?>) item;
                if (String.valueOf(metricId).equals(row.get("id")) && String.valueOf(version).equals(row.get("publishedVersion"))
                        && "PUBLISHED".equals(row.get("status")) && !row.containsKey("metadataNotice")) return true;
            }
        }
        return false;
    }

    public MetricRespVO published(Long id, Integer version, AgentToolContext context) {
        MetricRespVO current = metricService.get(id);
        if (current == null || !"PUBLISHED".equals(current.getStatus())) throw new IllegalArgumentException("指标不可用");
        assertOntology(current.getOntologyId(), context);
        Long active = currentOntology(context);
        if (active != null && !active.equals(current.getOntologyId())) throw new IllegalArgumentException("指标不属于当前本体");
        MetricRespVO snapshot = metricService.version(id, version);
        if (snapshot == null || !id.equals(snapshot.getId()) || !version.equals(snapshot.getPublishedVersion())
                || !"PUBLISHED".equals(snapshot.getStatus()) || !Objects.equals(current.getOntologyId(), snapshot.getOntologyId())
                || !Objects.equals(current.getConceptId(), snapshot.getConceptId()) || !Objects.equals(current.getCode(), snapshot.getCode())) {
            throw new IllegalArgumentException("发布快照身份不一致");
        }
        return snapshot;
    }

    public List<MetricFilterVO> filters(List<AgentToolRequest.AnalysisFilter> filters) {
        if (filters != null && filters.size() > 20) throw new IllegalArgumentException("过滤条件最多20个");
        List<MetricFilterVO> result = new ArrayList<>();
        if (filters == null) return result;
        for (AgentToolRequest.AnalysisFilter filter : filters) {
            if (filter == null) throw new IllegalArgumentException("过滤条件无效");
            MetricFilterVO mapped = new MetricFilterVO();
            mapped.setPropertyId(positiveId(filter.getPropertyId()));
            String operator = filter.getOperator() == null ? "" : filter.getOperator().trim().toUpperCase(Locale.ROOT);
            mapped.setOperator(operator);
            if ("IN".equals(operator)) {
                List<String> values = filter.getValueList();
                if (values == null || values.isEmpty() || values.size() > 100 || filter.getValue() != null) throw new IllegalArgumentException("IN必须使用valueList");
                mapped.setValue(new ArrayList<>(values));
            } else if ("IS_NULL".equals(operator) || "IS_NOT_NULL".equals(operator)) {
                if (filter.getValue() != null || (filter.getValueList() != null && !filter.getValueList().isEmpty())) throw new IllegalArgumentException("NULL条件不接受值");
                mapped.setValue(null);
            } else {
                if (filter.getValue() == null || (filter.getValueList() != null && !filter.getValueList().isEmpty())) throw new IllegalArgumentException("需要标量过滤值");
                mapped.setValue(filter.getValue());
            }
            result.add(mapped);
        }
        return result;
    }

    public Map<String, Object> semanticProperty(MetricRespVO snapshot, Long id) {
        if (id == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(id));
        Object mappings = snapshot.getSourceSnapshot() == null ? null : snapshot.getSourceSnapshot().get("propertyColumns");
        Object raw = mappings instanceof Map ? ((Map<?, ?>) mappings).get(String.valueOf(id)) : null;
        if (!(raw instanceof Map)) throw new IllegalArgumentException("缺少发布属性定义");
        Map<?, ?> property = (Map<?, ?>) raw;
        result.put("name", property.get("propertyName"));
        result.put("code", property.get("propertyCode"));
        result.put("dataType", property.get("dataType"));
        return result;
    }

    public AgentToolResult result(String status, Object data, String message, AgentToolContext context) {
        return AgentToolResult.of(status, data, message, context == null ? null : context.getOperationId());
    }
}
