package com.datamaster.module.ai.agent.decision;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.junit.Test;

import java.util.*;
import static org.junit.Assert.*;

public class DecisionMetricScopeTest {
    @Test public void sameScopeAcrossDifferentMetricIdsIsAccepted() {
        Proposal p = proposal(); CalculationContext context = context();
        assertNotNull(DecisionMetricScope.validate(p, context, Arrays.asList(ref(0, 0), ref(1, 0))));
    }

    @Test public void rejectsDifferentTimeWindowsConceptsGrainsAndFixedFilters() {
        for (String field : Arrays.asList("time", "concept", "grain", "fixed", "filter", "row")) {
            Proposal p = proposal(); Map<String, Object> data = query(p, 1);
            if ("time".equals(field)) object(data.get("queryDefinition")).put("startTime", "2026-02-01");
            if ("concept".equals(field)) object(data.get("scopeContract")).put("conceptId", "99");
            if ("grain".equals(field)) object(data.get("scopeContract")).put("grain", "每月一行");
            if ("fixed".equals(field)) object(data.get("scopeContract")).put("fixedFilters", Collections.singletonList(filter("C")));
            if ("filter".equals(field)) object(data.get("queryDefinition")).put("filters", Collections.singletonList(filter("B")));
            if ("row".equals(field)) object(((List<?>) data.get("rows")).get(0)).put("d_5", "B");
            reject(() -> DecisionMetricScope.validate(p, context(), Arrays.asList(ref(0, 0), ref(1, 0))));
        }
    }

    @Test public void rejectsOtherGroupCellEvenInSameQuery() {
        Proposal p = proposal();
        reject(() -> DecisionMetricScope.validate(p, context(), Collections.singletonList(ref(0, 1))));
    }

    @Test public void rejectsMissingReferenceAndLegacyEvidence() {
        Proposal p = proposal(); CalculationContext c = new CalculationContext();
        reject(() -> DecisionMetricScope.validate(p, c, Collections.singletonList(ref(0, 0))));
        query(p, 0).remove("scopeContract");
        reject(() -> DecisionMetricScope.validate(p, context(), Collections.singletonList(ref(0, 0))));
    }

    @Test public void rejectsTruncationAndForgedOperation() {
        Proposal truncated = proposal(); query(truncated, 1).put("truncated", true);
        reject(() -> DecisionMetricScope.validate(truncated, context(), Collections.singletonList(ref(1, 0))));
        Proposal changed = proposal(); changed.getEvidence().get(0).setOperationId("changed");
        reject(() -> DecisionMetricScope.validate(changed, context(), Collections.singletonList(ref(0, 0))));
    }

    @Test public void manualAssumptionsDoNotClaimMetricScopeVerification() {
        MeasureRef manual = new MeasureRef(); manual.setSource("USER");
        assertNull(DecisionMetricScope.validate(new Proposal(), new CalculationContext(), Collections.singletonList(manual)));
    }

    @Test public void filterOrderDoesNotChangeScope() {
        Proposal p = proposal();
        Map<String, Object> a = filter("A"), b = filter("B"); b.put("propertyId", 7L);
        object(query(p, 0).get("queryDefinition")).put("filters", Arrays.asList(a, b));
        object(query(p, 1).get("queryDefinition")).put("filters", Arrays.asList(b, a));
        assertNotNull(DecisionMetricScope.validate(p, context(), Arrays.asList(ref(0, 0), ref(1, 0))));
    }

    private static Proposal proposal() {
        Proposal p = new Proposal(); p.setOntologyId("1");
        Evidence e = new Evidence(); e.setId("E1"); e.setOperationId("op"); e.setVerification("VERIFIED_METRIC");
        List<Map<String, Object>> tools = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Map<String, Object> data = new LinkedHashMap<>(); data.put("metricId", String.valueOf(10 + i)); data.put("version", "1"); data.put("truncated", false);
            Map<String, Object> query = new LinkedHashMap<>(); query.put("metricId", String.valueOf(10 + i)); query.put("version", "1");
            query.put("startTime", "2026-01-01"); query.put("endTime", "2026-03-01");
            query.put("dimensionPropertyIds", Collections.singletonList(5L)); query.put("filters", Collections.singletonList(filter("A")));
            data.put("queryDefinition", query);
            Map<String, Object> contract = new LinkedHashMap<>(); contract.put("version", "METRIC_SCOPE_V1"); contract.put("ontologyId", "1"); contract.put("conceptId", "2");
            contract.put("timePropertyId", "6"); contract.put("grain", "每对象每日一行"); contract.put("fixedFilters", Collections.emptyList()); data.put("scopeContract", contract);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (String group : Arrays.asList("A", "B")) { Map<String, Object> row = new LinkedHashMap<>(); row.put("d_5", group); row.put("metric_value", "100"); rows.add(row); }
            data.put("rows", rows);
            Map<String, Object> tool = new LinkedHashMap<>(); tool.put("toolName", "execute_metric_analysis"); tool.put("status", "SUCCEEDED"); tool.put("operationId", "op"); tool.put("data", data); tools.add(tool);
        }
        e.getDetails().put("queryEvidence", tools); p.getEvidence().add(e); return p;
    }
    private static Map<String, Object> filter(String value) { Map<String, Object> f = new LinkedHashMap<>(); f.put("propertyId", 5L); f.put("operator", "EQ"); f.put("value", value); return f; }
    private static CalculationContext context() { CalculationContext c = new CalculationContext(); c.setMetricScopeReference(ref(0, 0)); return c; }
    private static MeasureRef ref(int tool, int row) { MeasureRef r = new MeasureRef(); r.setSource("METRIC"); r.setEvidenceId("E1"); r.setToolIndex(tool); r.setRowIndex(row); r.setColumnKey("metric_value"); return r; }
    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) { return (Map<String, Object>) value; }
    private static Map<String, Object> query(Proposal p, int index) { return object(object(((List<?>) p.getEvidence().get(0).getDetails().get("queryEvidence")).get(index)).get("data")); }
    private static void reject(Runnable action) { try { action.run(); fail("Must reject incompatible or unverifiable scope"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); } }
}
