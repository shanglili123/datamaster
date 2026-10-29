package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/** Resolves values from server evidence or explicitly confirmed assumptions, never LLM summaries. */
@Service
public class DecisionCalculationService {
    @Resource private StandardCostCapacityEvaluator evaluator;
    @Resource private DecisionComparisonService comparison;

    public boolean usesMetric(CalculationRequest request) {
        List<MeasureRef> values = new ArrayList<>();
        if (request.getContext() != null) {
            values.add(request.getContext().getDemand()); values.add(request.getContext().getExistingSupply()); values.add(request.getContext().getBudget());
        }
        if (request.getCandidates() != null) for (CalculationSpec spec : request.getCandidates()) {
            if (spec == null) continue;
            values.add(spec.getFixedCost());
            if (spec.getLines() != null) for (ResourceLine line : spec.getLines()) if (line != null) {
                values.add(line.getQuantity()); values.add(line.getUnitPrice()); values.add(line.getOutputPerUnit());
            }
        }
        return values.stream().anyMatch(ref -> ref != null && "METRIC".equals(ref.getSource()));
    }

    public void calculate(Proposal proposal, CalculationRequest request, String userId) {
        if (!Boolean.TRUE.equals(request.getConfirmedInputs())) throw new ServiceException("请明确确认测算值来源、单位映射、对象及统计窗口");
        CalculationContext context = request.getContext();
        if (context == null) throw new ServiceException("缺少共同测算条件");
        text(context.getScope(), "对象范围"); text(context.getPeriod(), "统计窗口");
        text(context.getCurrency(), "币种"); text(context.getQuantityUnit(), "产出数量单位");
        if (!Arrays.asList("MEET_DEMAND_MIN_COST", "MIN_GAP_THEN_COST").contains(context.getStrategy()))
            throw new ServiceException("请选择明确的比较策略");
        if (context.getNoBudgetLimit() == null) throw new ServiceException("请明确预算限制，不能将未填预算视为无限预算");
        if (Boolean.TRUE.equals(context.getNoBudgetLimit()) && context.getBudget() != null)
            throw new ServiceException("无预算限制与预算值不能同时提交");
        if (request.getCandidates() == null || request.getCandidates().isEmpty() || request.getCandidates().size() > 3)
            throw new ServiceException("请选择1至3个处置候选进行测算");
        Map<String, CalculationSpec> specs = new LinkedHashMap<>();
        Set<String> candidateIds = new HashSet<>();
        for (Candidate candidate : proposal.getCandidates()) if (!candidate.isBaseline()) candidateIds.add(candidate.getId());
        for (CalculationSpec spec : request.getCandidates()) {
            if (spec == null || !candidateIds.contains(spec.getCandidateId()) || specs.put(spec.getCandidateId(), spec) != null)
                throw new ServiceException("测算候选不存在、重复或为不干预基线");
        }
        for (Candidate candidate : proposal.getCandidates()) {
            candidate.setCalculation(null); candidate.setCalculationSpec(null);
            CalculationSpec spec = specs.get(candidate.getId());
            if (!candidate.isBaseline() && spec == null) continue;
            CalculationResult result = new CalculationResult();
            result.setConfirmedBy(userId); result.setConfirmedAt(new Date());
            try {
                BigDecimal demand = resolve(proposal, context.getDemand(), context.getQuantityUnit(), "demand", result);
                BigDecimal existing = resolve(proposal, context.getExistingSupply(), context.getQuantityUnit(), "existingSupply", result);
                BigDecimal budget = Boolean.TRUE.equals(context.getNoBudgetLimit()) ? null
                        : resolve(proposal, context.getBudget(), context.getCurrency(), "budget", result);
                BigDecimal cost = BigDecimal.ZERO, additional = BigDecimal.ZERO;
                if (!candidate.isBaseline()) {
                    if (spec.getLines() == null || spec.getLines().isEmpty() || spec.getLines().size() > 10)
                        throw new ServiceException("每个方案需要1至10条投入");
                    cost = resolve(proposal, spec.getFixedCost(), context.getCurrency(), "fixedCost", result);
                    Set<String> mapped = new HashSet<>();
                    for (int i = 0; i < spec.getLines().size(); i++) {
                        ResourceLine line = spec.getLines().get(i);
                        if (line == null) throw new ServiceException("投入行为空");
                        text(line.getName(), "投入名称"); text(line.getInputUnit(), "投入单位");
                        String prefix = "lines." + i;
                        BigDecimal quantity = resolve(proposal, line.getQuantity(), line.getInputUnit(), prefix + ".quantity", result);
                        boolean discrete = Arrays.asList("件", "台", "个", "人", "辆").contains(line.getInputUnit());
                        if ((discrete || !Boolean.TRUE.equals(line.getFractionalQuantity())) && quantity.stripTrailingZeros().scale() > 0)
                            throw new ServiceException("该投入数量必须为整数");
                        BigDecimal price = resolve(proposal, line.getUnitPrice(), context.getCurrency() + "/" + line.getInputUnit(), prefix + ".unitPrice", result);
                        BigDecimal output = resolve(proposal, line.getOutputPerUnit(), context.getQuantityUnit() + "/" + line.getInputUnit(), prefix + ".outputPerUnit", result);
                        cost = cost.add(quantity.multiply(price)); additional = additional.add(quantity.multiply(output));
                        result.getInputs().put(prefix + ".lineCost", quantity.multiply(price).toPlainString());
                        result.getInputs().put(prefix + ".lineOutput", quantity.multiply(output).toPlainString());
                        if (line.getQuantityParameter() != null && !line.getQuantityParameter().trim().isEmpty()) {
                            String key = line.getQuantityParameter();
                            if (!key.matches("[A-Za-z_][A-Za-z0-9_]{0,63}") || !mapped.add(key))
                                throw new ServiceException("执行映射只支持唯一的顶层参数字段");
                            result.getExecutionQuantities().put(key, quantity.toPlainString());
                        }
                    }
                }
                evaluator.compute(result, demand, existing, cost, additional, budget, Boolean.TRUE.equals(context.getNoBudgetLimit()));
                result.getLimitations().add("共同范围：" + context.getScope() + "；统计窗口：" + context.getPeriod());
                if (!candidate.isBaseline() && result.getExecutionQuantities().size() != spec.getLines().size())
                    result.getLimitations().add("投入数量未完整映射到执行参数，仅可比较，不可按已测算方案提交");
            } catch (ServiceException invalid) {
                result.setStatus("UNKNOWN"); result.setBudgetStatus("UNKNOWN");
                result.getExecutionQuantities().clear(); result.getLimitations().add(invalid.getMessage());
            }
            if (Boolean.TRUE.equals(spec == null ? null : spec.getAlignExecutionQuantities()) && "COMPUTED".equals(result.getStatus())) {
                Binding binding = candidate.getSuggestedBinding();
                if (binding == null || result.getExecutionQuantities().size() != spec.getLines().size())
                    throw new ServiceException("同步执行数量需要已有动作绑定且每条投入完整映射");
                Map<String, Object> parameters = DecisionBindingResolver.parseInput(binding.getInputParams());
                for (Map.Entry<String, String> entry : result.getExecutionQuantities().entrySet()) {
                    if (!parameters.containsKey(entry.getKey())) throw new ServiceException("不能同步不存在的执行参数：" + entry.getKey());
                    StandardCostCapacityEvaluator.decimal(String.valueOf(parameters.get(entry.getKey())));
                    parameters.put(entry.getKey(), entry.getValue());
                }
                binding.setInputParams(DecisionBindingResolver.canonical(parameters));
                result.getLimitations().add("用户明确确认同步映射的已有数值参数；未提交业务执行，仍需重新预览确认");
            }
            Map<String, Object> digest = new LinkedHashMap<>();
            digest.put("context", context); digest.put("spec", spec); digest.put("inputs", result.getInputs());
            digest.put("sources", result.getSources()); digest.put("quantities", result.getExecutionQuantities());
            result.setInputDigest(DecisionBindingResolver.sha256(DecisionBindingResolver.canonical(digest)));
            if (candidate.getSuggestedBinding() != null) result.setBindingInputDigest(bindingDigest(candidate.getSuggestedBinding()));
            candidate.setCalculation(result); candidate.setCalculationSpec(spec);
        }
        proposal.setCalculationContext(context); proposal.setComparison(comparison.compare(proposal, context));
    }

    @SuppressWarnings("unchecked")
    private BigDecimal resolve(Proposal proposal, MeasureRef ref, String expectedUnit, String name, CalculationResult result) {
        if (ref == null) throw new ServiceException("缺少测算值：" + name);
        if (!expectedUnit.equals(ref.getUnit())) throw new ServiceException("单位不匹配：" + name + "，要求 " + expectedUnit);
        String raw;
        String source;
        if ("USER".equals(ref.getSource())) {
            raw = ref.getValue(); source = "USER_CONFIRMED_ASSUMPTION;unit=" + expectedUnit;
        } else if ("METRIC".equals(ref.getSource())) {
            Evidence evidence = null;
            for (Evidence item : proposal.getEvidence()) if (ref.getEvidenceId() != null && ref.getEvidenceId().equals(item.getId())) evidence = item;
            if (evidence == null || !"VERIFIED_METRIC".equals(evidence.getVerification()) || evidence.getDetails() == null)
                throw new ServiceException("测算值不是已验证的正式指标证据：" + name);
            Object queries = evidence.getDetails().get("queryEvidence");
            if (!(queries instanceof List) || ref.getToolIndex() == null || ref.getToolIndex() < 0 || ref.getToolIndex() >= ((List<?>) queries).size())
                throw new ServiceException("指标工具引用越界：" + name);
            Map<String, Object> tool = JSON.parseObject(JSON.toJSONString(((List<?>) queries).get(ref.getToolIndex())), Map.class);
            if (!"execute_metric_analysis".equals(tool.get("toolName")) || !"SUCCEEDED".equals(tool.get("status")) || !(tool.get("data") instanceof Map))
                throw new ServiceException("只能引用成功的正式单指标查询单元格：" + name);
            if (!Objects.equals(tool.get("operationId"), evidence.getOperationId()))
                throw new ServiceException("指标证据操作标识不一致：" + name);
            Map<String, Object> data = (Map<String, Object>) tool.get("data");
            if (!Boolean.FALSE.equals(data.get("truncated"))) throw new ServiceException("截断或未知完整性的指标不能测算：" + name);
            if (data.get("unit") == null || !expectedUnit.equals(String.valueOf(data.get("unit"))))
                throw new ServiceException("指标发布单位与测算字段不一致：" + name);
            Object rows = data.get("rows");
            if (!(rows instanceof List) || ref.getRowIndex() == null || ref.getRowIndex() < 0 || ref.getRowIndex() >= ((List<?>) rows).size())
                throw new ServiceException("指标行引用越界：" + name);
            if (!"metric_value".equals(ref.getColumnKey())) throw new ServiceException("仅支持数值度量列metric_value，不允许把维度当数量");
            Object columns = data.get("columns");
            boolean numeric = false;
            if (columns instanceof List) for (Object column : (List<?>) columns) {
                if (!(column instanceof Map)) continue;
                Map<?, ?> definition = (Map<?, ?>) column;
                if ("metric_value".equals(definition.get("key")) && definition.get("dataType") != null
                        && com.datamaster.module.ontology.service.metric.MetricRules.numeric(String.valueOf(definition.get("dataType")))) numeric = true;
            }
            if (!numeric) throw new ServiceException("指标度量列不是已声明的数值类型：" + name);
            Map<?, ?> row = ((List<?>) rows).get(ref.getRowIndex()) instanceof Map ? (Map<?, ?>) ((List<?>) rows).get(ref.getRowIndex()) : null;
            if (row == null) throw new ServiceException("指标行不可用：" + name);
            Object value = row.get(ref.getColumnKey());
            if (!(value instanceof String)) throw new ServiceException("指标值缺失或不是精确数值字符串：" + name);
            raw = (String) value;
            source = "VERIFIED_METRIC;" + evidence.getId() + ";tool=" + ref.getToolIndex() + ";row=" + ref.getRowIndex()
                    + ";metric=" + data.get("metricId") + ";version=" + data.get("version") + ";executedAt=" + data.get("executedAt") + ";unit=" + expectedUnit;
        } else throw new ServiceException("测算值来源必须为正式指标或用户确认：" + name);
        BigDecimal value = StandardCostCapacityEvaluator.decimal(raw);
        result.getInputs().put(name, value.toPlainString()); result.getSources().put(name, source);
        return value;
    }

    /** Called at preview, confirmation and immediately before dispatch. */
    public void assertExecution(Candidate candidate, Binding binding) {
        if (candidate.getCalculation() == null) return;
        CalculationResult result = candidate.getCalculation();
        if (!"COMPUTED".equals(result.getStatus()) || "FAIL".equals(result.getBudgetStatus()))
            throw new ServiceException("测算未完成或超过预算，不可提交");
        CalculationSpec spec = candidate.getCalculationSpec();
        if (result.getBindingInputDigest() == null || !result.getBindingInputDigest().equals(bindingDigest(binding)))
            throw new ServiceException("测算时没有明确执行绑定或目标/参数发生变化，请先绑定已有动作并重新测算");
        if (spec == null || spec.getLines() == null || spec.getLines().isEmpty()
                || result.getExecutionQuantities().size() != spec.getLines().size())
            throw new ServiceException("请为每条测算投入配置唯一执行数量参数映射后重新测算");
        Map<String, Object> input = DecisionBindingResolver.parseInput(binding.getInputParams());
        for (Map.Entry<String, String> entry : result.getExecutionQuantities().entrySet()) {
            Object actual = input.get(entry.getKey());
            if (actual == null || StandardCostCapacityEvaluator.decimal(String.valueOf(actual)).compareTo(new BigDecimal(entry.getValue())) != 0)
                throw new ServiceException("执行数量与已测算输入不一致：" + entry.getKey() + "；请调整方案后重新测算/预览");
        }
    }

    private String bindingDigest(Binding binding) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("type", "COMPOSITE".equals(binding.getType()) ? "ACTION" : binding.getType());
        identity.put("targetId", binding.getTargetId()); identity.put("objectKey", binding.getObjectKey());
        identity.put("input", DecisionBindingResolver.parseInput(binding.getInputParams()));
        return DecisionBindingResolver.sha256(DecisionBindingResolver.canonical(identity));
    }

    private void text(String value, String label) {
        if (value == null || value.trim().isEmpty() || value.length() > 200) throw new ServiceException("请填写" + label + "（最多200字符）");
    }
}
