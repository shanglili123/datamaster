package com.datamaster.module.ai.agent.analysis;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MetricResultAnalysisTest {
    private Map<String, Object> evidence(String type, String[] categories, String[] values) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        Map<String, Object> dimension = new LinkedHashMap<>();
        dimension.put("key", "d_123"); dimension.put("dataType", type);
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("key", "metric_value"); metric.put("dataType", "decimal");
        evidence.put("columns", Arrays.asList(dimension, metric));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < categories.length; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("d_123", categories[i]); row.put("metric_value", values[i]); rows.add(row);
        }
        evidence.put("rows", rows); evidence.put("truncated", false);
        return evidence;
    }

    @Test
    public void ranksExactValuesWithCompetitionTiesAndNoInventedShares() {
        Map<String, Object> result = MetricResultAnalysis.analyze(evidence("string",
                new String[]{"A", "B", "C", "D"}, new String[]{"9007199254740993", "2", "2", null}), false);
        List<?> ranking = (List<?>) result.get("ranking");
        assertEquals(3, ranking.size());
        assertEquals("9007199254740993", ((Map<?, ?>) ranking.get(0)).get("value"));
        assertEquals(2, ((Map<?, ?>) ranking.get(2)).get("rank"));
        assertEquals(1L, result.get("nullValueCount"));
        assertFalse(result.containsKey("share"));
        assertFalse(result.containsKey("total"));
    }

    @Test
    public void sortsDatesAndDoesNotCompareAcrossMissingDays() {
        Map<String, Object> result = MetricResultAnalysis.analyze(evidence("date",
                new String[]{"2026-10-04", "2026-10-01", "2026-10-02"}, new String[]{"20", "10", "15"}), true);
        List<?> points = (List<?>) result.get("points");
        assertEquals("2026-10-01", ((Map<?, ?>) points.get(0)).get("dimension"));
        assertEquals(1, result.get("missingDateIntervals"));
        assertNull(((Map<?, ?>) points.get(2)).get("comparison"));
        assertEquals("50.000000", ((Map<?, ?>) ((Map<?, ?>) points.get(1)).get("comparison")).get("growthRatePercent"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTruncatedDistribution() {
        Map<String, Object> evidence = evidence("string", new String[]{"A"}, new String[]{"1"});
        evidence.put("truncated", true);
        MetricResultAnalysis.analyze(evidence, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTimestampWithoutExplicitDateDimension() {
        MetricResultAnalysis.analyze(evidence("timestamp", new String[]{"2026-10-01T00:00:00"}, new String[]{"1"}), true);
    }

    @Test
    public void chartSpecUsesOnlyActualDataAndFallsBackToTable() {
        Map<String, Object> evidence = evidence("date", new String[]{"2026-10-01", "2026-10-02"}, new String[]{"1", "2"});
        assertEquals("line", MetricChartSpec.build(evidence, "AUTO").get("chartType"));
        evidence.put("truncated", true);
        Map<String, Object> table = MetricChartSpec.build(evidence, "AUTO");
        assertEquals("table", table.get("chartType"));
        assertEquals("SPEC_ONLY", table.get("rendererStatus"));
        assertFalse(table.containsKey("javascript"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsExplicitLineForMissingDates() {
        MetricChartSpec.build(evidence("date", new String[]{"2026-10-01", "2026-10-03"}, new String[]{"1", "2"}), "LINE");
    }
}
