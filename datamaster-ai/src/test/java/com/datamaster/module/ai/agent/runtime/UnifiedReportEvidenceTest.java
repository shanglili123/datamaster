package com.datamaster.module.ai.agent.runtime;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UnifiedReportEvidenceTest {
    private static final String TEMPLATE = """
            {"dataSchema":{"required":["title","metrics.total"],"fields":[
              {"key":"metrics.total","type":"number","evidencePath":"queries.0.data.rows.0.metric_value"}
            ]}}
            """;

    @Test public void bindsTheExactValueFromTheCurrentResult() {
        String exact = "9007199254741593.61";
        var row = Map.of("metric_value", exact);
        var query = Map.of("data", Map.of("rows", List.of(row)));
        var report = UnifiedReportEvidence.bind(TEMPLATE, "销售统计", List.of(query));
        assertNotNull(report);
        assertEquals("销售统计", report.getString("title"));
        assertEquals(exact, report.getJSONObject("metrics").get("total"));
    }

    @Test public void remoteReportMustContainRequiredFieldsOfTheRightType() {
        var data = com.alibaba.fastjson2.JSON.parseObject("{\"title\":\"销售统计\",\"metrics\":{\"total\":\"未查询\"}}");
        assertFalse(UnifiedReportEvidence.satisfiesTemplate(TEMPLATE, data));
        data.getJSONObject("metrics").put("total", "9007199254741593.61");
        assertTrue(UnifiedReportEvidence.satisfiesTemplate(TEMPLATE, data));
        data.remove("title");
        assertFalse(UnifiedReportEvidence.satisfiesTemplate(TEMPLATE, data));
    }

    @Test public void missingRequiredValueDoesNotProduceAnAppliedTemplate() {
        var query = Map.of("data", Map.of("rows", List.of()));
        assertNull(UnifiedReportEvidence.bind(TEMPLATE, "销售统计", List.of(query)));
    }
}
