package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import com.datamaster.module.ontology.service.IMetricService;
import com.datamaster.module.ontology.service.analysis.MetricQueryBuilder;
import com.datamaster.module.ontology.service.metric.MetricRules;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Explicit observation windows and deterministic target checks, never causal attribution. */
@Service
public class DecisionEffectService {
    @Resource private IMetricAnalysisService analysis;
    @Resource private IMetricService metrics;

    public EffectPlan validatePlan(Proposal proposal, EffectPlan input) {
        if (input == null) return null; // Explicit clearing before dispatch is allowed.
        EffectPlan plan = JSON.parseObject(JSON.toJSONString(input), EffectPlan.class);
        Map<?, ?> data = baseline(proposal, plan.getBaseline());
        MetricQueryReqVO query = definition(data);
        MetricRespVO published = metrics.version(query.getMetricId(), query.getVersion());
        if (!proposal.getOntologyId().equals(String.valueOf(published.getOntologyId())) || published.getTimePropertyId() == null)
            throw new ServiceException("效果目标必须是当前本体含时间属性的正式指标");
        if (!Arrays.asList("INCREASE", "DECREASE").contains(plan.getDirection())) throw new ServiceException("请选择提升或降低方向");
        decimal(plan.getMinimumChange());
        MetricQueryBuilder.compile(published, query);
        if (query.getStartTime() == null || query.getEndTime() == null) throw new ServiceException("效果基线缺少明确日期范围");
        if (query.getFilters().stream().anyMatch(f -> Objects.equals(f.getPropertyId(), published.getTimePropertyId()))
                || published.getFixedFilters().stream().anyMatch(f -> Objects.equals(f.getPropertyId(), published.getTimePropertyId())))
            throw new ServiceException("效果基线不能额外过滤时间属性，以免观察窗口被旧条件截断");
        MetricQueryReqVO after = JSON.parseObject(JSON.toJSONString(query), MetricQueryReqVO.class);
        after.setStartTime(plan.getObservationStart()); after.setEndTime(plan.getObservationEnd());
        MetricQueryBuilder.compile(published, after);
        LocalDateTime baselineEnd = local(query.getEndTime());
        LocalDateTime start = local(plan.getObservationStart()), end = local(plan.getObservationEnd());
        if (start.isBefore(baselineEnd)) throw new ServiceException("观察窗口必须在基线窗口之后且不重叠");
        if (!Duration.between(local(query.getStartTime()), baselineEnd).equals(Duration.between(start, end)))
            throw new ServiceException("首版效果复查要求基线和观察窗口等长，不自动归一化");
        OffsetDateTime ready;
        try { ready = OffsetDateTime.parse(plan.getReadyAt()); }
        catch (RuntimeException invalid) { throw new ServiceException("预计数据就绪时间须为带时区偏移的ISO时间，例如2026-11-12T08:00:00+08:00"); }
        if (ready.toLocalDateTime().isBefore(end)) throw new ServiceException("预计数据就绪时间不能早于观察窗口结束");
        if (start.isBefore(LocalDateTime.now(ready.getOffset()))) throw new ServiceException("提交前配置的观察窗口必须从未来开始");
        return plan;
    }

    public Map<String, Object> review(Proposal proposal) {
        EffectPlan plan = proposal.getEffectPlan();
        if (plan == null) return status("UNKNOWN", "未配置执行前冻结的效果观察计划，不能推断实际改善");
        Instant ready = OffsetDateTime.parse(plan.getReadyAt()).toInstant();
        if (Instant.now().isBefore(ready)) return status("WAITING_OBSERVATION", "尚未到达约定的数据就绪时间，请在观察窗口结束及预期刷新后复查");
        Date finished = completedAt(proposal.getExecutionDetails());
        if (finished == null || local(plan.getObservationStart()).isBefore(LocalDateTime.ofInstant(finished.toInstant(), OffsetDateTime.parse(plan.getReadyAt()).getOffset())))
            return status("INCONCLUSIVE", "观察窗口开始早于实际执行完成，不能把执行前数据当作执行后效果；请重新制定任务");
        Map<?, ?> data = baseline(proposal, plan.getBaseline());
        MetricQueryReqVO baseQuery = definition(data);
        if (!String.valueOf(baseQuery.getMetricId()).equals(String.valueOf(data.get("metricId")))
                || !String.valueOf(baseQuery.getVersion()).equals(String.valueOf(data.get("version"))))
            throw new ServiceException("效果基线版本身份不一致");
        MetricQueryReqVO query = JSON.parseObject(JSON.toJSONString(baseQuery), MetricQueryReqVO.class);
        query.setStartTime(plan.getObservationStart()); query.setEndTime(plan.getObservationEnd());
        MetricRespVO published = metrics.version(query.getMetricId(), query.getVersion());
        if (!proposal.getOntologyId().equals(String.valueOf(published.getOntologyId()))) throw new ServiceException("效果指标不属于提案本体");
        MetricQueryRespVO result = analysis.query(query); // Re-authorizes current source/space/columns.
        if (result == null || !Boolean.FALSE.equals(result.getTruncated()) || result.getRows() == null || result.getRows().size() != 1
                || !Objects.equals(query.getMetricId(), result.getMetricId()) || !Objects.equals(query.getVersion(), result.getVersion()))
            return status("INCONCLUSIVE", "未获得完整同版本单值结果，无法判定目标");
        Object raw = result.getRows().get(0).get("metric_value");
        if (!(raw instanceof String)) return status("INCONCLUSIVE", "观察值为空或不是精确数值，不能当作零");
        BigDecimal before = signed(value(data)), after = signed((String) raw), difference = after.subtract(before);
        BigDecimal change = "DECREASE".equals(plan.getDirection()) ? difference.negate() : difference;
        boolean improved = change.signum() > 0 && change.compareTo(decimal(plan.getMinimumChange())) >= 0;
        Map<String, Object> response = status(improved ? "IMPROVED" : "NOT_IMPROVED",
                "仅表示按约定同口径观测变化达到/未达到阈值，不证明干预因果收益；预计就绪时间是人工约定，不是源数据刷新证明");
        response.put("metricId", String.valueOf(query.getMetricId())); response.put("version", query.getVersion());
        response.put("unit", result.getUnit()); response.put("baseline", before.toPlainString()); response.put("observed", after.toPlainString());
        response.put("difference", difference.toPlainString()); response.put("minimumChange", plan.getMinimumChange()); response.put("direction", plan.getDirection());
        response.put("observationStart", plan.getObservationStart()); response.put("observationEnd", plan.getObservationEnd());
        response.put("executedAt", result.getExecutedAt()); response.put("baselineExecutedAt", data.get("executedAt"));
        response.put("equalDuration", Duration.between(local(definition(data).getStartTime()), local(definition(data).getEndTime()))
                .equals(Duration.between(local(plan.getObservationStart()), local(plan.getObservationEnd()))));
        response.put("dataFreshnessVerified", false); return response;
    }

    private Map<?, ?> baseline(Proposal proposal, MeasureRef ref) {
        if (ref == null || !"METRIC".equals(ref.getSource()) || ref.getRowIndex() == null || ref.getRowIndex() != 0 || !"metric_value".equals(ref.getColumnKey()))
            throw new ServiceException("请选择正式指标单值总量作为效果基线，不支持分组或模型数字");
        for (Evidence e : proposal.getEvidence()) if (Objects.equals(e.getId(), ref.getEvidenceId()) && "VERIFIED_METRIC".equals(e.getVerification())) {
            Object raw = e.getDetails().get("queryEvidence");
            if (!(raw instanceof List) || ref.getToolIndex() == null || ref.getToolIndex() < 0 || ref.getToolIndex() >= ((List<?>) raw).size()) break;
            Map<?, ?> tool = JSON.parseObject(JSON.toJSONString(((List<?>) raw).get(ref.getToolIndex())), Map.class);
            if (!"execute_metric_analysis".equals(tool.get("toolName")) || !"SUCCEEDED".equals(tool.get("status")) || e.getOperationId() == null || !e.getOperationId().equals(tool.get("operationId"))) break;
            Map<?, ?> data = JSON.parseObject(JSON.toJSONString(tool.get("data")), Map.class);
            if (data == null || !Boolean.FALSE.equals(data.get("truncated")) || !definition(data).getDimensionPropertyIds().isEmpty()) break;
            MetricQueryReqVO identity = definition(data);
            if (!String.valueOf(identity.getMetricId()).equals(String.valueOf(data.get("metricId"))) || !String.valueOf(identity.getVersion()).equals(String.valueOf(data.get("version")))) break;
            Object columns = data.get("columns");
            if (!(columns instanceof List) || ((List<?>) columns).size() != 1) break;
            Map<?, ?> column = (Map<?, ?>) ((List<?>) columns).get(0);
            if (!"metric_value".equals(column.get("key")) || !MetricRules.numeric(String.valueOf(column.get("dataType")))) break;
            signed(value(data)); return data;
        }
        throw new ServiceException("效果基线缺少可核验的完整正式单指标总量证据");
    }
    private MetricQueryReqVO definition(Map<?, ?> data) {
        try { return MetricQueryBuilder.normalize(JSON.parseObject(JSON.toJSONString(data.get("queryDefinition")), MetricQueryReqVO.class)); }
        catch (RuntimeException invalid) { throw new ServiceException("效果证据查询定义不可用"); }
    }
    private String value(Map<?, ?> data) {
        Object rows = data.get("rows");
        if (!(rows instanceof List) || ((List<?>) rows).size() != 1 || !(((List<?>) rows).get(0) instanceof Map)) throw new ServiceException("效果基线必须是单值");
        Object raw = ((Map<?, ?>) ((List<?>) rows).get(0)).get("metric_value");
        if (!(raw instanceof String)) throw new ServiceException("效果基线为空或非数值"); return (String) raw;
    }
    private static BigDecimal signed(String value) {
        try { BigDecimal n = new BigDecimal(value); if (n.precision() <= 1000 && Math.abs((long) n.scale()) <= 1000) return n; }
        catch (RuntimeException ignored) { } throw new ServiceException("效果数值超限或不可用");
    }
    private static BigDecimal decimal(String value) { BigDecimal n = signed(value); if (n.signum() < 0) throw new ServiceException("变化阈值不能为负数"); return n; }
    private static LocalDateTime local(String value) {
        try { return value.length() == 10 ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value); }
        catch (RuntimeException invalid) { throw new ServiceException("效果观察日期不可用"); }
    }
    private static Date completedAt(Map<String, Object> details) {
        if (details == null) return null;
        Object raw = details.get("ACTION".equals(details.get("kind")) ? "executedAt" : "finishedAt");
        if (raw instanceof Date) return (Date) raw;
        if (raw instanceof Number) return new Date(((Number) raw).longValue());
        return null;
    }
    private static Map<String, Object> status(String status, String notice) { Map<String, Object> map = new LinkedHashMap<>(); map.put("status", status); map.put("notice", notice); return map; }
}
