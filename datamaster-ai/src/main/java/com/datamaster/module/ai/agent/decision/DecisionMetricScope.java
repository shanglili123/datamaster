package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryReqVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricFilterVO;
import com.datamaster.module.ontology.service.analysis.MetricQueryBuilder;

import java.util.*;

/** Conservative equality of server-recorded semantic scopes, not inference from user labels. */
final class DecisionMetricScope {
    private DecisionMetricScope() { }

    static String validate(Proposal proposal, CalculationContext context, List<MeasureRef> inputs) {
        List<MeasureRef> metrics = new ArrayList<>();
        for (MeasureRef input : inputs) if (input != null && "METRIC".equals(input.getSource())) metrics.add(input);
        if (metrics.isEmpty()) return null;
        MeasureRef reference = context.getMetricScopeReference();
        if (reference == null || !"METRIC".equals(reference.getSource()))
            throw new ServiceException("引用正式指标时，请明确选择共同对象范围与统计窗口的指标基准");
        String expected = signature(proposal, reference);
        for (MeasureRef metric : metrics) {
            if (!expected.equals(signature(proposal, metric)))
                throw new ServiceException("正式指标的概念、粒度、时间范围、固定过滤、分析过滤或分组值与共同口径基准不一致；请重新查询同口径指标，不可混合测算");
        }
        return DecisionBindingResolver.sha256(expected);
    }

    private static String signature(Proposal proposal, MeasureRef ref) {
        Map<?, ?> data = data(proposal, ref);
        Map<?, ?> contract = object(data.get("scopeContract"));
        if (!"METRIC_SCOPE_V1".equals(contract.get("version")))
            throw new ServiceException("历史指标证据缺少结构化口径，请重新进行正式指标分析");
        String ontology = id(contract.get("ontologyId"));
        if (!Objects.equals(ontology, proposal.getOntologyId())) throw new ServiceException("指标口径不属于提案本体");
        String concept = id(contract.get("conceptId"));
        Object grain = contract.get("grain");
        if (!(grain instanceof String) || ((String) grain).trim().isEmpty()) throw new ServiceException("指标证据缺少粒度");
        Object rawQuery = data.get("queryDefinition");
        if (!(rawQuery instanceof Map)) throw new ServiceException("指标证据缺少完整查询条件，请重新查询");
        MetricQueryReqVO query;
        try { query = MetricQueryBuilder.normalize(JSON.parseObject(JSON.toJSONString(rawQuery), MetricQueryReqVO.class)); }
        catch (RuntimeException invalid) { throw new ServiceException("指标证据查询条件不可用，请重新查询"); }
        if (!String.valueOf(query.getMetricId()).equals(String.valueOf(data.get("metricId")))
                || !String.valueOf(query.getVersion()).equals(String.valueOf(data.get("version"))))
            throw new ServiceException("指标证据版本身份不一致");
        Object timeId = contract.get("timePropertyId");
        String time = timeId == null ? null : id(timeId);
        if ((time != null) != (query.getStartTime() != null)) throw new ServiceException("指标证据统计窗口不完整");
        Object rawRows = data.get("rows");
        if (!(rawRows instanceof List) || ref.getRowIndex() == null || ref.getRowIndex() < 0
                || ref.getRowIndex() >= ((List<?>) rawRows).size() || !"metric_value".equals(ref.getColumnKey()))
            throw new ServiceException("共同口径指标单元格引用无效");
        Map<?, ?> row = object(((List<?>) rawRows).get(ref.getRowIndex()));
        Map<String, Object> dimensions = new TreeMap<>();
        for (Long dimension : query.getDimensionPropertyIds()) {
            String key = "d_" + dimension;
            if (!row.containsKey(key)) throw new ServiceException("指标证据缺少分组值");
            Object value = row.get(key);
            if (value != null && !(value instanceof String) && !(value instanceof Boolean))
                throw new ServiceException("指标证据分组值类型无效");
            dimensions.put(key, value);
        }
        Set<String> expectedKeys = new HashSet<>(dimensions.keySet()); expectedKeys.add("metric_value");
        if (!row.keySet().equals(expectedKeys)) throw new ServiceException("指标证据分组与查询定义不一致");
        Object fixed = contract.get("fixedFilters");
        if (!(fixed instanceof List)) throw new ServiceException("指标证据缺少固定口径，请重新查询");
        List<MetricFilterVO> fixedFilters;
        try { fixedFilters = JSON.parseArray(JSON.toJSONString(fixed), MetricFilterVO.class); }
        catch (RuntimeException invalid) { throw new ServiceException("指标固定口径不可用"); }
        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("ontologyId", ontology); signature.put("conceptId", concept);
        signature.put("grain", grain); signature.put("timePropertyId", time);
        signature.put("startTime", query.getStartTime()); signature.put("endTime", query.getEndTime());
        signature.put("fixedFilters", filters(fixedFilters)); signature.put("filters", filters(query.getFilters()));
        signature.put("dimensions", dimensions);
        return DecisionBindingResolver.canonical(signature);
    }

    private static List<String> filters(List<MetricFilterVO> filters) {
        List<String> result = new ArrayList<>();
        if (filters == null || filters.size() > 20) throw new ServiceException("指标过滤口径不可用");
        for (MetricFilterVO filter : filters) {
            if (filter == null || filter.getPropertyId() == null || filter.getOperator() == null)
                throw new ServiceException("指标过滤口径不可用");
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("propertyId", id(filter.getPropertyId())); value.put("operator", filter.getOperator());
            Object raw = filter.getValue();
            if (raw instanceof Collection) {
                List<String> items = new ArrayList<>();
                if (((Collection<?>) raw).size() > 100) throw new ServiceException("指标过滤口径超限");
                for (Object item : (Collection<?>) raw) items.add(DecisionBindingResolver.canonical(item));
                Collections.sort(items); value.put("value", items);
            } else value.put("value", raw);
            result.add(DecisionBindingResolver.canonical(value));
        }
        Collections.sort(result); return result;
    }

    private static Map<?, ?> data(Proposal proposal, MeasureRef ref) {
        for (Evidence evidence : proposal.getEvidence()) {
            if (!Objects.equals(evidence.getId(), ref.getEvidenceId())) continue;
            if (!"VERIFIED_METRIC".equals(evidence.getVerification()) || evidence.getOperationId() == null)
                throw new ServiceException("共同口径必须来自正式指标证据");
            Object raw = evidence.getDetails().get("queryEvidence");
            if (!(raw instanceof List) || ref.getToolIndex() == null || ref.getToolIndex() < 0
                    || ref.getToolIndex() >= ((List<?>) raw).size()) throw new ServiceException("指标口径工具引用无效");
            Map<?, ?> tool = object(((List<?>) raw).get(ref.getToolIndex()));
            if (!"execute_metric_analysis".equals(tool.get("toolName")) || !"SUCCEEDED".equals(tool.get("status"))
                    || !Objects.equals(evidence.getOperationId(), tool.get("operationId")))
                throw new ServiceException("共同口径必须来自成功的正式单指标查询");
            Map<?, ?> data = object(tool.get("data"));
            if (!Boolean.FALSE.equals(data.get("truncated"))) throw new ServiceException("截断指标不可作为共同口径");
            return data;
        }
        throw new ServiceException("找不到共同口径指标证据");
    }

    private static Map<?, ?> object(Object value) {
        Object json = JSON.toJSON(value);
        if (!(json instanceof Map)) throw new ServiceException("指标口径证据不可用，请重新查询");
        return (Map<?, ?>) json;
    }

    private static String id(Object value) {
        String text = String.valueOf(value);
        if (!text.matches("[1-9][0-9]{0,18}")) throw new ServiceException("指标口径身份无效");
        return text;
    }
}
