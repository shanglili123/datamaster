package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

public class DecisionCalculationServiceTest {
    @Test
    public void computesCostsAndRecommendsCheaperCompleteAlternative() throws Exception {
        Proposal proposal = proposal();
        CalculationRequest request = request();
        request.getCandidates().add(spec("C1", "60", "5", "1", "20"));
        request.getCandidates().add(spec("C2", "10", "25", "6", "0"));
        service().calculate(proposal, request, "7");
        assertEquals("320", candidate(proposal, "C1").getCalculation().getTotalCost());
        assertEquals("250", candidate(proposal, "C2").getCalculation().getTotalCost());
        assertEquals("0", candidate(proposal, "C2").getCalculation().getRemainingGap());
        assertEquals("60", candidate(proposal, "BASELINE").getCalculation().getRemainingGap());
        assertEquals(Collections.singletonList("C2"), proposal.getComparison().getRecommendedIds());
        assertEquals(Integer.valueOf(1), candidate(proposal, "C2").getCalculation().getRank());
        assertTrue(candidate(proposal, "C2").getCalculation().isConditional());
    }

    @Test
    public void budgetBoundaryUsesExactDecimalAndTiesArePreserved() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        request.getContext().setBudget(value("0.3", "CNY"));
        request.getContext().setDemand(value("1", "件")); request.getContext().setExistingSupply(value("0", "件"));
        request.getCandidates().add(spec("C1", "1", "0.1", "1", "0.2"));
        request.getCandidates().add(spec("C2", "1", "0.3", "1", "0"));
        service().calculate(proposal, request, "7");
        assertEquals("PASS", candidate(proposal, "C1").getCalculation().getBudgetStatus());
        assertEquals(Integer.valueOf(1), candidate(proposal, "C1").getCalculation().getRank());
        assertEquals(Integer.valueOf(1), candidate(proposal, "C2").getCalculation().getRank());
        assertEquals(2, proposal.getComparison().getRecommendedIds().size());
    }

    @Test
    public void unknownInputsAreNotZeroOrRanked() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        CalculationSpec broken = spec("C1", "60", "5", "1", "0"); broken.getLines().get(0).setUnitPrice(null);
        request.getCandidates().add(broken);
        service().calculate(proposal, request, "7");
        CalculationResult result = candidate(proposal, "C1").getCalculation();
        assertEquals("UNKNOWN", result.getStatus()); assertNull(result.getTotalCost()); assertNull(result.getRank());
        assertTrue(proposal.getComparison().getRecommendedIds().isEmpty());
    }

    @Test
    public void executionQuantityMustMatchCalculatedQuantity() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        request.getCandidates().add(spec("C1", "60", "5", "1", "0"));
        Binding original = new Binding(); original.setType("ACTION"); original.setTargetId("9"); original.setInputParams("{\"quantity\":\"60\"}");
        candidate(proposal, "C1").setSuggestedBinding(original);
        DecisionCalculationService service = service(); service.calculate(proposal, request, "7");
        Binding binding = new Binding(); binding.setType("ACTION"); binding.setTargetId("9"); binding.setInputParams("{\"quantity\":\"60\"}");
        service.assertExecution(candidate(proposal, "C1"), binding);
        binding.setInputParams("{\"quantity\":\"61\"}");
        try { service.assertExecution(candidate(proposal, "C1"), binding); fail("Must reject changed quantity"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertTrue(expected.getMessage().contains("变化") || expected.getMessage().contains("不一致")); }
    }

    @Test
    public void truncatedEvidenceCannotBeUsedForCalculation() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        Evidence evidence = new Evidence(); evidence.setId("E1"); evidence.setVerification("VERIFIED_METRIC");
        Map<String, Object> data = new LinkedHashMap<>(); data.put("truncated", true); data.put("unit", "件");
        data.put("rows", Collections.singletonList(Collections.singletonMap("metric_value", "100")));
        Map<String, Object> tool = new LinkedHashMap<>(); tool.put("toolName", "execute_metric_analysis"); tool.put("status", "SUCCEEDED"); tool.put("data", data);
        evidence.getDetails().put("queryEvidence", Collections.singletonList(tool)); proposal.getEvidence().add(evidence);
        MeasureRef ref = new MeasureRef(); ref.setSource("METRIC"); ref.setEvidenceId("E1"); ref.setToolIndex(0); ref.setRowIndex(0); ref.setColumnKey("metric_value"); ref.setUnit("件");
        request.getContext().setDemand(ref); request.getCandidates().add(spec("C1", "60", "5", "1", "0"));
        service().calculate(proposal, request, "7");
        assertEquals("UNKNOWN", candidate(proposal, "C1").getCalculation().getStatus());
        assertTrue(proposal.getComparison().getRecommendedIds().isEmpty());
    }

    @Test
    public void zeroDemandNeverDividesByZero() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        request.getContext().setDemand(value("0", "件"));
        request.getCandidates().add(spec("C1", "0", "0", "1", "0"));
        service().calculate(proposal, request, "7");
        assertEquals("100", candidate(proposal, "C1").getCalculation().getCoveragePercent());
    }

    @Test
    public void negativeQuantityIsNotComputed() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        request.getCandidates().add(spec("C1", "-1", "5", "1", "0"));
        service().calculate(proposal, request, "7");
        assertEquals("UNKNOWN", candidate(proposal, "C1").getCalculation().getStatus());
    }

    @Test
    public void explicitQuantityAlignmentUpdatesOnlyExistingNumericParameter() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        CalculationSpec spec = spec("C1", "60", "5", "1", "0"); spec.setAlignExecutionQuantities(true); request.getCandidates().add(spec);
        Binding binding = new Binding(); binding.setType("ACTION"); binding.setTargetId("9"); binding.setInputParams("{\"quantity\":\"10\",\"warehouse\":\"A\"}");
        candidate(proposal, "C1").setSuggestedBinding(binding);
        DecisionCalculationService service = service(); service.calculate(proposal, request, "7");
        service.assertExecution(candidate(proposal, "C1"), binding);
        assertEquals("60", com.alibaba.fastjson2.JSON.parseObject(binding.getInputParams()).getString("quantity"));
        assertEquals("A", com.alibaba.fastjson2.JSON.parseObject(binding.getInputParams()).getString("warehouse"));
    }

    @Test
    public void resolvesNumericValueFromServerMetricCellNotClientValue() throws Exception {
        Proposal proposal = proposal(); CalculationRequest request = request();
        Evidence evidence = new Evidence(); evidence.setId("E1"); evidence.setOperationId("metric-operation"); evidence.setVerification("VERIFIED_METRIC");
        Map<String, Object> data = new LinkedHashMap<>(); data.put("truncated", false); data.put("unit", "件");
        data.put("rows", Collections.singletonList(Collections.singletonMap("metric_value", "100")));
        Map<String, Object> column = new LinkedHashMap<>(); column.put("key", "metric_value"); column.put("dataType", "decimal"); data.put("columns", Collections.singletonList(column));
        Map<String, Object> tool = new LinkedHashMap<>(); tool.put("toolName", "execute_metric_analysis"); tool.put("status", "SUCCEEDED"); tool.put("operationId", "metric-operation"); tool.put("data", data);
        evidence.getDetails().put("queryEvidence", Collections.singletonList(tool)); proposal.getEvidence().add(evidence);
        MeasureRef ref = new MeasureRef(); ref.setSource("METRIC"); ref.setEvidenceId("E1"); ref.setToolIndex(0); ref.setRowIndex(0); ref.setColumnKey("metric_value"); ref.setUnit("件"); ref.setValue("999999");
        request.getContext().setDemand(ref); request.getCandidates().add(spec("C1", "60", "5", "1", "0"));
        service().calculate(proposal, request, "7");
        assertEquals("100", candidate(proposal, "C1").getCalculation().getInputs().get("demand"));
        assertEquals("0", candidate(proposal, "C1").getCalculation().getRemainingGap());
    }

    private DecisionCalculationService service() throws Exception {
        DecisionCalculationService service = new DecisionCalculationService();
        inject(service, "evaluator", new StandardCostCapacityEvaluator()); inject(service, "comparison", new DecisionComparisonService()); return service;
    }
    private void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
    private Proposal proposal() {
        Proposal proposal = new Proposal();
        for (String id : Arrays.asList("BASELINE", "C1", "C2")) { Candidate candidate = new Candidate(); candidate.setId(id); candidate.setName(id); candidate.setBaseline("BASELINE".equals(id)); proposal.getCandidates().add(candidate); }
        return proposal;
    }
    private Candidate candidate(Proposal proposal, String id) { return proposal.getCandidates().stream().filter(item -> id.equals(item.getId())).findFirst().get(); }
    private CalculationRequest request() {
        CalculationRequest request = new CalculationRequest(); request.setConfirmedInputs(true);
        CalculationContext context = new CalculationContext(); context.setScope("订单批次X"); context.setPeriod("2026-10-29 Asia/Shanghai");
        context.setCurrency("CNY"); context.setQuantityUnit("件"); context.setStrategy("MEET_DEMAND_MIN_COST"); context.setNoBudgetLimit(false);
        context.setDemand(value("100", "件")); context.setExistingSupply(value("40", "件")); context.setBudget(value("500", "CNY")); request.setContext(context); return request;
    }
    private CalculationSpec spec(String id, String quantity, String price, String output, String fixed) {
        CalculationSpec spec = new CalculationSpec(); spec.setCandidateId(id); spec.setFixedCost(value(fixed, "CNY"));
        ResourceLine line = new ResourceLine(); line.setName("投入"); line.setInputUnit("小时"); line.setFractionalQuantity(true);
        line.setQuantity(value(quantity, "小时")); line.setUnitPrice(value(price, "CNY/小时")); line.setOutputPerUnit(value(output, "件/小时")); line.setQuantityParameter("quantity"); spec.getLines().add(line); return spec;
    }
    private MeasureRef value(String value, String unit) { MeasureRef ref = new MeasureRef(); ref.setSource("USER"); ref.setValue(value); ref.setUnit(unit); return ref; }
}
