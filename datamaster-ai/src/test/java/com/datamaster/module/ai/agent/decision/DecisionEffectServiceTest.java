package com.datamaster.module.ai.agent.decision;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.*;
import org.junit.Test;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class DecisionEffectServiceTest {
    @Test public void comparesFrozenQueryAgainstPostExecutionWindowAndMarksFreshnessUnknown() throws Exception {
        Fixture f = new Fixture(); Map<String, Object> result = f.service.review(f.proposal);
        assertEquals("IMPROVED", result.get("status")); assertEquals("20", result.get("difference"));
        assertEquals(false, result.get("dataFreshnessVerified")); assertEquals("100", result.get("baseline"));
        assertEquals(f.plan.getObservationStart(), f.lastQuery.getStartTime()); assertEquals(Long.valueOf(10), f.lastQuery.getMetricId());
    }
    @Test public void doesNotQueryBeforeReadyOrWhenExecutionOverlapsObservation() throws Exception {
        Fixture f = new Fixture(); f.plan.setReadyAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).toString());
        assertEquals("WAITING_OBSERVATION", f.service.review(f.proposal).get("status")); assertEquals(0, f.calls);
        f.plan.setReadyAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toString());
        f.proposal.getExecutionDetails().put("executedAt", new Date());
        assertEquals("INCONCLUSIVE", f.service.review(f.proposal).get("status")); assertEquals(0, f.calls);
    }
    @Test public void nullOrTruncatedObservationDoesNotBecomeZeroOrImprovement() throws Exception {
        Fixture f = new Fixture(); f.response.setTruncated(true);
        assertEquals("INCONCLUSIVE", f.service.review(f.proposal).get("status"));
        f.response.setTruncated(false); f.response.setRows(Collections.singletonList(Collections.singletonMap("metric_value", null)));
        assertEquals("INCONCLUSIVE", f.service.review(f.proposal).get("status"));
    }
    @Test public void missingPlanStaysUnknownAndMissingBaselineRejects() throws Exception {
        Fixture f = new Fixture(); f.proposal.setEffectPlan(null);
        assertEquals("UNKNOWN", f.service.review(f.proposal).get("status"));
        f.proposal.setEffectPlan(f.plan); f.proposal.getEvidence().clear();
        try { f.service.review(f.proposal); fail(); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    @Test public void zeroThresholdDoesNotLabelUnchangedValueImproved() throws Exception {
        Fixture f = new Fixture(); f.plan.setMinimumChange("0");
        f.response.setRows(Collections.singletonList(Collections.singletonMap("metric_value", "100")));
        assertEquals("NOT_IMPROVED", f.service.review(f.proposal).get("status"));
        f.plan.setDirection("DECREASE"); f.response.setRows(Collections.singletonList(Collections.singletonMap("metric_value", "80")));
        assertEquals("IMPROVED", f.service.review(f.proposal).get("status"));
    }

    private static class Fixture {
        DecisionEffectService service = new DecisionEffectService(); Proposal proposal = new Proposal(); EffectPlan plan = new EffectPlan();
        MetricQueryRespVO response = new MetricQueryRespVO(); MetricQueryReqVO lastQuery; int calls;
        Fixture() throws Exception {
            proposal.setOntologyId("1"); proposal.setStatus("SUCCEEDED");
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            String beforeStart = today.minusDays(4).toString(), beforeEnd = today.minusDays(3).toString();
            plan.setObservationStart(today.minusDays(2).toString()); plan.setObservationEnd(today.minusDays(1).toString());
            plan.setReadyAt(today.minusDays(1).atTime(8, 0).atOffset(ZoneOffset.UTC).toString()); plan.setDirection("INCREASE"); plan.setMinimumChange("10");
            MeasureRef ref = new MeasureRef(); ref.setSource("METRIC"); ref.setEvidenceId("E1"); ref.setToolIndex(0); ref.setRowIndex(0); ref.setColumnKey("metric_value"); plan.setBaseline(ref); proposal.setEffectPlan(plan);
            Map<String, Object> details = new LinkedHashMap<>(); details.put("kind", "ACTION"); details.put("executedAt", Date.from(today.minusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant())); proposal.setExecutionDetails(details);
            MetricQueryReqVO q = new MetricQueryReqVO(); q.setMetricId(10L); q.setVersion(1); q.setStartTime(beforeStart); q.setEndTime(beforeEnd);
            Map<String, Object> data = new LinkedHashMap<>(); data.put("queryDefinition", q); data.put("metricId", "10"); data.put("version", "1"); data.put("truncated", false); data.put("rows", Collections.singletonList(Collections.singletonMap("metric_value", "100")));
            Map<String, Object> column = new LinkedHashMap<>(); column.put("key", "metric_value"); column.put("dataType", "decimal"); data.put("columns", Collections.singletonList(column));
            Map<String, Object> tool = new LinkedHashMap<>(); tool.put("toolName", "execute_metric_analysis"); tool.put("status", "SUCCEEDED"); tool.put("operationId", "op"); tool.put("data", data);
            Evidence e = new Evidence(); e.setId("E1"); e.setVerification("VERIFIED_METRIC"); e.setOperationId("op"); e.getDetails().put("queryEvidence", Collections.singletonList(tool)); proposal.getEvidence().add(e);
            response.setMetricId(10L); response.setVersion(1); response.setTruncated(false); response.setRows(Collections.singletonList(Collections.singletonMap("metric_value", "120"))); response.setUnit("件");
            inject(service, "analysis", Proxy.newProxyInstance(IMetricAnalysisService.class.getClassLoader(), new Class<?>[]{IMetricAnalysisService.class}, (p,m,a) -> { calls++; lastQuery = (MetricQueryReqVO) a[0]; return response; }));
            inject(service, "metrics", Proxy.newProxyInstance(IMetricService.class.getClassLoader(), new Class<?>[]{IMetricService.class}, (p,m,a) -> { MetricRespVO metric = new MetricRespVO(); metric.setOntologyId(1L); return metric; }));
        }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field f = bean.getClass().getDeclaredField(name); f.setAccessible(true); f.set(bean, value); }
}
