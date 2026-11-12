package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricTrendRulesTest {
    @Test public void absentCalendarBucketsAreNullAndLineGapsNotInventedZeros() {
        MetricQueryReqVO request = request(); MetricQueryRespVO result = new MetricQueryRespVO(); result.setTruncated(false);
        result.setRows(List.of(Map.of("time_bucket", "2026-01-01", "metric_value", "12.50"), Map.of("time_bucket", "2026-01-03", "metric_value", "8")));
        MetricTrendRules.completeCalendar(request, result);
        assertEquals(3, result.getRows().size()); assertEquals("2026-01-02", result.getRows().get(1).get("time_bucket"));
        assertNull(result.getRows().get(1).get("metric_value")); assertEquals("12.50", result.getRows().get(0).get("metric_value"));
    }
    @Test public void calendarLimitCheckedBeforeQueryEvenIfManyBucketsWouldBeAbsent() {
        MetricQueryReqVO request = request(); request.setLimit(2);
        rejected(() -> MetricTrendRules.validateCalendar(request));
        request.setLimit(500); request.setEndTime("2030-01-01T00:00:00"); rejected(() -> MetricTrendRules.validateCalendar(request));
    }
    @Test public void truncatedOrUnexpectedBucketsNeverBecomeCompleteTrend() {
        MetricQueryReqVO request = request(); MetricQueryRespVO result = new MetricQueryRespVO(); result.setTruncated(true); result.setRows(List.of());
        rejected(() -> MetricTrendRules.completeCalendar(request, result));
        result.setTruncated(false); result.setRows(List.of(Map.of("time_bucket", "2026-02-01", "metric_value", "3")));
        rejected(() -> MetricTrendRules.completeCalendar(request, result));
    }
    private static MetricQueryReqVO request() { MetricQueryReqVO request = new MetricQueryReqVO(); request.setMetricId(10L); request.setVersion(1); request.setTimeBucket("DAY"); request.setRequireComplete(true); request.setStartTime("2026-01-01T12:00:00"); request.setEndTime("2026-01-04T00:00:00"); request.setLimit(500); return request; }
}
