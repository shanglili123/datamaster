package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MetricGroupIdentityTest {
    @Test public void numericScaleDoesNotSplitOneDimensionAcrossPeriods() {
        MetricQueryColumnVO dimension = dimension("decimal");
        assertEquals(MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "1.0")), MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "1.00")));
        assertEquals(MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "-0.00")), MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "0")));
        assertEquals(MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "9007199254740993.10")), MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "9007199254740993.100")));
    }
    @Test public void textFormattingAndNullRemainDistinct() {
        MetricQueryColumnVO dimension = dimension("string"); Map<String,Object> absent = new HashMap<>(); absent.put("d_2", null);
        assertNotEquals(MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "1.0")), MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "1.00")));
        assertNotEquals(MetricGroupIdentity.of(List.of(dimension), absent), MetricGroupIdentity.of(List.of(dimension), Map.of("d_2", "null")));
    }
    @Test public void comparisonUsesTypedIdentityInsteadOfDecimalDisplayScale() {
        MetricComparisonRespVO comparison = new MetricComparisonRespVO(); comparison.setCurrent(response("1.0", "10")); comparison.setBaseline(response("1.00", "5"));
        MetricComparisonRules.calculate(comparison); assertEquals(1, comparison.getRows().size()); assertEquals("100", comparison.getRows().get(0).get("change_percent"));
        MetricMultiQueryRespVO multi = MetricMultiQueryRules.align(List.of(response("1.0", "10"), response("1.00", "5")), "RATIO");
        assertEquals(1, multi.getRows().size()); assertEquals("2", multi.getRows().get(0).get("derived_value"));
    }
    @Test public void completeUnionsSortNumericDimensionsNumericallyIncludingBaselineOnlyGroups() {
        MetricQueryRespVO first = response("10", "10"), second = response("2", "5");
        MetricMultiQueryRespVO multi = MetricMultiQueryRules.align(List.of(first, second), "NONE");
        assertEquals("2", multi.getRows().get(0).get("d_2")); assertEquals("10", multi.getRows().get(1).get("d_2"));
        MetricComparisonRespVO comparison = new MetricComparisonRespVO(); comparison.setCurrent(first); comparison.setBaseline(second);
        MetricComparisonRules.calculate(comparison);
        assertEquals("2", comparison.getRows().get(0).get("d_2")); assertNull(comparison.getRows().get(0).get("current_value"));
        assertEquals("10", comparison.getRows().get(1).get("d_2"));
    }
    @Test public void textOrderIsLexicalWhileTemporalOrderComparesActualLocalTimes() {
        List<Map<String,Object>> text = new ArrayList<>(List.of(Map.of("d_2", "2"), Map.of("d_2", "10")));
        text.sort(MetricGroupIdentity.ascending(List.of(dimension("string")))); assertEquals("10", text.get(0).get("d_2"));
        List<Map<String,Object>> times = new ArrayList<>(List.of(Map.of("d_2", "2026-11-12T10:00:00.000000001"), Map.of("d_2", "2026-11-12T10:00:00")));
        times.sort(MetricGroupIdentity.ascending(List.of(dimension("datetime"))));
        assertEquals("2026-11-12T10:00:00", times.get(0).get("d_2"));
    }
    @Test public void nullSortsSeparatelyFromZeroAndLargeNumbersStayExact() {
        Map<String,Object> absent = new HashMap<>(); absent.put("d_2", null);
        List<Map<String,Object>> values = new ArrayList<>(List.of(Map.of("d_2", "9007199254740993"), Map.of("d_2", "9007199254740992"), Map.of("d_2", "0"), absent));
        values.sort(MetricGroupIdentity.ascending(List.of(dimension("decimal"))));
        assertNull(values.get(0).get("d_2")); assertEquals("0", values.get(1).get("d_2"));
        assertEquals("9007199254740992", values.get(2).get("d_2")); assertEquals("9007199254740993", values.get(3).get("d_2"));
    }
    private static MetricQueryColumnVO dimension(String type) { MetricQueryColumnVO column = new MetricQueryColumnVO(); column.setKey("d_2"); column.setDataType(type); return column; }
    private static MetricQueryRespVO response(String key, String value) {
        MetricQueryRespVO result = new MetricQueryRespVO(); result.setTruncated(false); result.setUnit("元"); result.setMetricName("金额");
        MetricQueryColumnVO measure = new MetricQueryColumnVO(); measure.setKey("metric_value"); measure.setDataType("decimal");
        result.setColumns(List.of(dimension("decimal"), measure)); result.setRows(List.of(Map.of("d_2", key, "metric_value", value))); return result;
    }
}
