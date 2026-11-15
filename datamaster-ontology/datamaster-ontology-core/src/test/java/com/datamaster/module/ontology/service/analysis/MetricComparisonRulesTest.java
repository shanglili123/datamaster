package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricComparisonRulesTest {
    @Test public void shiftsEqualDurationAndCalendarPeriodsExplicitly() {
        MetricComparisonReqVO request = request("PREVIOUS_PERIOD", "2026-03-01", "2026-04-01");
        MetricQueryReqVO before = MetricComparisonRules.baseline(request);
        assertEquals("2026-01-29", before.getStartTime()); assertEquals("2026-03-01", before.getEndTime());
        request.setMode("PREVIOUS_MONTH"); before = MetricComparisonRules.baseline(request);
        assertEquals("2026-02-01", before.getStartTime()); assertEquals("2026-03-01", before.getEndTime());
        request = request("PREVIOUS_YEAR", "2024-02-29", "2024-03-02"); before = MetricComparisonRules.baseline(request);
        assertEquals("2023-02-28", before.getStartTime()); assertEquals("2023-03-02", before.getEndTime());
    }
    @Test public void acceptsCompleteGroupsAndBucketsButRejectsIndependentTopNAndPartialBoundaries() {
        MetricComparisonReqVO request = request("PREVIOUS_PERIOD", "2026-01-01", "2026-02-01");
        request.getQuery().setDimensionPropertyIds(List.of(2L));
        assertEquals(List.of(2L), MetricComparisonRules.baseline(request).getDimensionPropertyIds());
        request.getQuery().setSortBy("metric_value"); request.getQuery().setSortDirection("DESC");
        rejected(() -> MetricComparisonRules.baseline(request));
        request.getQuery().setSortBy(null); request.getQuery().setSortDirection(null);
        request.getQuery().setTimeBucket("DAY");
        assertEquals("DAY", MetricComparisonRules.baseline(request).getTimeBucket());
        request.getQuery().setStartTime("2026-01-01T01:00:00"); request.getQuery().setEndTime("2026-02-01T01:00:00");
        rejected(() -> MetricComparisonRules.baseline(request));
        request.getQuery().setTimeBucket("NONE"); request.setMode("SQL"); rejected(() -> MetricComparisonRules.baseline(request));
    }
    @Test public void preservesDimensionFiltersButRejectsTimePropertyFilters() {
        MetricComparisonReqVO request = request("PREVIOUS_PERIOD", "2026-01-01", "2026-02-01");
        request.getQuery().setFilters(List.of(MetricQueryBuilderTest.filter(2L, "EQ", "west")));
        MetricComparisonRules.validateFilters(MetricQueryBuilderTest.snapshot(), request.getQuery());
        assertEquals("west", MetricComparisonRules.baseline(request).getFilters().get(0).getValue());
        request.getQuery().setFilters(List.of(MetricQueryBuilderTest.filter(3L, "GE", "2026-01-01")));
        rejected(() -> MetricComparisonRules.validateFilters(MetricQueryBuilderTest.snapshot(), request.getQuery()));
    }
    @Test public void completeTrendChronologicalSortIsNotMistakenForIndependentTopN() {
        MetricComparisonReqVO request = request("PREVIOUS_MONTH", "2026-03-01", "2026-04-01");
        request.getQuery().setTimeBucket("MONTH"); request.getQuery().setSortBy("time_bucket"); request.getQuery().setSortDirection("ASC"); request.getQuery().setRequireComplete(true);
        assertEquals("2026-02-01", MetricComparisonRules.baseline(request).getStartTime());
        request.getQuery().setRequireComplete(false); request.getQuery().setSortDirection("DESC");
        rejected(() -> MetricComparisonRules.baseline(request));
    }
    @Test public void nullOperatorFiltersKeepNullValueWhenBaselineIsCopied() {
        MetricComparisonReqVO request = request("PREVIOUS_MONTH", "2026-03-01", "2026-04-01");
        request.getQuery().setFilters(List.of(MetricQueryBuilderTest.filter(2L, "IS_NULL", null)));
        MetricQueryReqVO baseline = MetricComparisonRules.baseline(request);
        assertEquals("IS_NULL", baseline.getFilters().get(0).getOperator()); assertNull(baseline.getFilters().get(0).getValue());
        assertNull(request.getQuery().getFilters().get(0).getValue());
    }
    @Test public void customDatesMustBeConsistentAndOrdered() {
        MetricComparisonReqVO request = request("CUSTOM", "2026-01-01", "2026-02-01");
        request.setBaselineStartTime("2025-01-01"); request.setBaselineEndTime("2025-02-01");
        assertEquals("2025-01-01", MetricComparisonRules.baseline(request).getStartTime());
        request.setBaselineEndTime("2025-01-01"); rejected(() -> MetricComparisonRules.baseline(request));
    }
    @Test public void baselineRejectsOutOfRangeYearsAndNonLocalIsoInputsBeforeSourceAuthorization() {
        rejected(() -> MetricComparisonRules.baseline(request("PREVIOUS_YEAR", "0001-01-01", "0001-02-01")));
        rejected(() -> MetricComparisonRules.baseline(request("PREVIOUS_PERIOD", "0001-01-01", "0001-01-03")));
        rejected(() -> MetricComparisonRules.baseline(request("PREVIOUS_MONTH", "0000-01-01", "0000-02-01")));
        MetricComparisonReqVO custom = request("CUSTOM", "2026-01-01T00:00", "2026-02-01T00:00");
        custom.setBaselineStartTime("2025-01-01T00:00:00.123456789"); custom.setBaselineEndTime("2025-02-01T00:00");
        assertEquals("2025-01-01T00:00:00.123456789", MetricComparisonRules.baseline(custom).getStartTime());
        custom.setBaselineStartTime("2025-01-01T00:00Z"); rejected(() -> MetricComparisonRules.baseline(custom));
        custom.setBaselineStartTime("2025-01-01 00:00:00"); rejected(() -> MetricComparisonRules.baseline(custom));
        custom.setBaselineStartTime("2025-01-01"); rejected(() -> MetricComparisonRules.baseline(custom));
    }
    @Test public void computesDecimalDifferenceWithoutFloatingPointAndLeavesZeroRateUnknown() {
        MetricComparisonRespVO result = new MetricComparisonRespVO(); result.setCurrent(value("9007199254740993.10")); result.setBaseline(value("9007199254740993.00"));
        MetricComparisonRules.calculate(result); assertEquals("0.10", result.getDifference());
        result = new MetricComparisonRespVO(); result.setCurrent(value("5")); result.setBaseline(value("0"));
        MetricComparisonRules.calculate(result); assertEquals("5", result.getDifference()); assertNull(result.getChangePercent());
        result = new MetricComparisonRespVO(); result.setCurrent(value("-5")); result.setBaseline(value("-10"));
        MetricComparisonRules.calculate(result); assertEquals("50", result.getChangePercent());
    }
    @Test public void alignsGroupsIncludingNullAndNeverFillsMissingPeriodWithZero() {
        MetricComparisonRespVO result = new MetricComparisonRespVO();
        result.setCurrent(grouped(List.of(group("A", "10"), group(null, "8"))));
        result.setBaseline(grouped(List.of(group("A", "5"), group("B", "7"))));
        MetricComparisonRules.calculate(result);
        assertTrue(result.isGrouped()); assertEquals(3, result.getRows().size());
        assertNull(result.getRows().get(0).get("d_2"));
        assertNull(result.getRows().get(0).get("baseline_value")); assertNull(result.getRows().get(0).get("difference"));
        assertEquals("A", result.getRows().get(1).get("d_2")); assertEquals("100", result.getRows().get(1).get("change_percent"));
        assertEquals("B", result.getRows().get(2).get("d_2"));
        assertNull(result.getRows().get(2).get("current_value")); assertNull(result.getRows().get(2).get("difference"));
        assertNull(result.getDifference());
    }
    @Test public void rejectsTruncatedOrDuplicateGroupsRatherThanCalculatingPartialChange() {
        MetricComparisonRespVO result = new MetricComparisonRespVO();
        result.setCurrent(grouped(List.of(group("A", "10")))); result.setBaseline(grouped(List.of(group("A", "5"))));
        result.getBaseline().setTruncated(true); rejected(() -> MetricComparisonRules.calculate(result));
        result.getBaseline().setTruncated(false); result.getCurrent().setRows(List.of(group("A", "10"), group("A", "11")));
        rejected(() -> MetricComparisonRules.calculate(result));
    }
    private static MetricQueryRespVO grouped(List<Map<String,Object>> rows) {
        MetricQueryRespVO response = new MetricQueryRespVO(); response.setTruncated(false); response.setRows(rows);
        MetricQueryColumnVO dimension = new MetricQueryColumnVO(); dimension.setKey("d_2"); dimension.setLabel("地区"); dimension.setDataType("string");
        MetricQueryColumnVO value = new MetricQueryColumnVO(); value.setKey("metric_value"); value.setDataType("decimal"); response.setColumns(List.of(dimension, value)); return response;
    }
    private static Map<String,Object> group(String key, String value) {
        Map<String,Object> row = new LinkedHashMap<>(); row.put("d_2", key); row.put("metric_value", value); return row;
    }
    private static MetricComparisonReqVO request(String mode, String start, String end) {
        MetricComparisonReqVO request = new MetricComparisonReqVO(); MetricQueryReqVO query = new MetricQueryReqVO();
        query.setMetricId(10L); query.setVersion(1); query.setStartTime(start); query.setEndTime(end); request.setQuery(query); request.setMode(mode); return request;
    }
    private static MetricQueryRespVO value(String value) {
        MetricQueryRespVO response = new MetricQueryRespVO(); response.setTruncated(false); response.setRows(List.of(Map.of("metric_value", value)));
        MetricQueryColumnVO column = new MetricQueryColumnVO(); column.setKey("metric_value"); column.setDataType("decimal"); response.setColumns(List.of(column)); return response;
    }
}
