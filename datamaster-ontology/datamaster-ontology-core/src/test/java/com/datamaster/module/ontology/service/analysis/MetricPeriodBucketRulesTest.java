package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricPeriodBucketRulesTest {
    @Test public void alignsEqualCompleteDaysByOrdinalAndPreservesActualBaselineLabels() {
        var current = query("2026-03-01", "2026-03-03", "DAY"); var baseline = query("2026-02-01", "2026-02-03", "DAY");
        MetricComparisonRespVO result = new MetricComparisonRespVO(); result.setCurrent(response("2026-03-01", "10")); result.setBaseline(response("2026-02-01", "5"));
        MetricQueryRespVO original = result.getBaseline();
        MetricPeriodBucketRules.calculate(result, current, baseline);
        assertSame(original, result.getBaseline()); assertEquals("2026-02-01", original.getRows().get(0).get("time_bucket"));
        assertEquals("2026-03-01", result.getRows().get(0).get("time_bucket")); assertEquals("2026-02-01", result.getRows().get(0).get("baseline_time_bucket"));
        assertEquals("100", result.getRows().get(0).get("change_percent"));
    }
    @Test public void acceptsCompleteMonthsButRejectsUnequalDaysAndPartialBoundaries() {
        MetricPeriodBucketRules.validate(query("2026-03-01", "2026-04-01", "MONTH"), query("2026-02-01", "2026-03-01", "MONTH"));
        rejected(() -> MetricPeriodBucketRules.validate(query("2026-03-01", "2026-04-01", "DAY"), query("2026-02-01", "2026-03-01", "DAY")));
        rejected(() -> MetricPeriodBucketRules.validate(query("2026-03-01T01:00:00", "2026-03-03T01:00:00", "DAY"), query("2026-02-01T00:00:00", "2026-02-03T00:00:00", "DAY")));
        rejected(() -> MetricPeriodBucketRules.validate(query("2026-03-02", "2026-04-02", "MONTH"), query("2026-02-02", "2026-03-02", "MONTH")));
        rejected(() -> MetricPeriodBucketRules.validate(query("2026-02-01", "2027-02-01", "YEAR"), query("2025-02-01", "2026-02-01", "YEAR")));
    }
    @Test public void rejectsSourceBucketsOutsideDeclaredRangeAndNeverUsesTruncatedResults() {
        var current = query("2026-03-01", "2026-03-03", "DAY"); var baseline = query("2026-02-01", "2026-02-03", "DAY");
        MetricComparisonRespVO result = new MetricComparisonRespVO(); result.setCurrent(response("2026-03-01", "10")); result.setBaseline(response("2026-02-05", "5"));
        rejected(() -> MetricPeriodBucketRules.calculate(result, current, baseline));
        result.setBaseline(response("2026-02-01", "5")); result.getCurrent().setTruncated(true);
        rejected(() -> MetricPeriodBucketRules.calculate(result, current, baseline));
    }
    @Test public void nullMeasuresAndNullDimensionKeysSurviveBaselineAlignment() {
        var current = query("2026-03-01", "2026-03-03", "DAY"); var baseline = query("2026-02-01", "2026-02-03", "DAY");
        MetricQueryRespVO now = response("2026-03-01", "10"), before = response("2026-02-01", "5");
        MetricQueryColumnVO dimension = new MetricQueryColumnVO(); dimension.setKey("d_2"); dimension.setDataType("string");
        now.setColumns(List.of(now.getColumns().get(0), dimension, now.getColumns().get(1)));
        before.setColumns(List.of(before.getColumns().get(0), dimension, before.getColumns().get(1)));
        now.getRows().get(0).put("d_2", null); before.getRows().get(0).put("d_2", null); before.getRows().get(0).put("metric_value", null);
        MetricComparisonRespVO result = new MetricComparisonRespVO(); result.setCurrent(now); result.setBaseline(before);
        MetricPeriodBucketRules.calculate(result, current, baseline);
        assertEquals(1, result.getRows().size()); assertTrue(result.getRows().get(0).containsKey("d_2"));
        assertNull(result.getRows().get(0).get("d_2")); assertNull(result.getRows().get(0).get("baseline_value"));
        assertNull(result.getRows().get(0).get("difference"));
    }
    @Test public void annualAlignmentKeepsZeroPaddedLabelsAcrossCenturyBoundary() {
        var current = query("0100-01-01", "0102-01-01", "YEAR"); var baseline = query("0098-01-01", "0100-01-01", "YEAR");
        MetricComparisonRespVO result = new MetricComparisonRespVO(); result.setCurrent(response("0100", "10")); result.setBaseline(response("0098", "5"));
        MetricPeriodBucketRules.calculate(result, current, baseline);
        assertEquals("0100", result.getRows().get(0).get("time_bucket")); assertEquals("0098", result.getRows().get(0).get("baseline_time_bucket"));
        assertEquals("100", result.getRows().get(0).get("change_percent"));
    }
    private static MetricQueryReqVO query(String start, String end, String bucket) { var query = new MetricQueryReqVO(); query.setMetricId(10L); query.setVersion(1); query.setStartTime(start); query.setEndTime(end); query.setTimeBucket(bucket); return query; }
    private static MetricQueryRespVO response(String bucket, String amount) {
        MetricQueryRespVO response = new MetricQueryRespVO(); response.setTruncated(false); response.setRows(List.of(new LinkedHashMap<>(Map.of("time_bucket", bucket, "metric_value", amount))));
        MetricQueryColumnVO time = new MetricQueryColumnVO(); time.setKey("time_bucket"); time.setDataType("string");
        MetricQueryColumnVO value = new MetricQueryColumnVO(); value.setKey("metric_value"); value.setDataType("decimal"); response.setColumns(List.of(time, value)); return response;
    }
}
