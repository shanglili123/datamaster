package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricMultiQueryRulesTest {
    @Test public void comparesSameSourceAndRejectsIndependentTopNAndMixedSources() {
        MetricRespVO a = MetricQueryBuilderTest.snapshot(), b = MetricQueryBuilderTest.snapshot(); b.setId(11L);
        MetricQueryReqVO first = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()), second = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()); second.setMetricId(11L);
        MetricMultiQueryRules.comparable(List.of(a, b), List.of(first, second), "RATIO");
        second.setSortBy("metric_value"); second.setSortDirection("DESC");
        rejected(() -> MetricMultiQueryRules.comparable(List.of(a, b), List.of(first, second), "RATIO"));
        second.setSortBy(null); second.setSortDirection(null); b.getSourceSnapshot().put("tableName", "other");
        rejected(() -> MetricMultiQueryRules.comparable(List.of(a, b), List.of(first, second), "RATIO"));
    }
    @Test public void alignsMissingGroupsWithoutZeroAndUsesDecimalDerivation() {
        MetricQueryRespVO left = response("10000000000000000.10", "A"), right = response("10000000000000000.00", "A");
        MetricMultiQueryRespVO difference = MetricMultiQueryRules.align(List.of(left, right), "DIFFERENCE");
        assertEquals("0.10", difference.getRows().get(0).get("derived_value"));
        right = response("0", "A"); MetricMultiQueryRespVO ratio = MetricMultiQueryRules.align(List.of(left, right), "RATIO");
        assertNull(ratio.getRows().get(0).get("derived_value")); assertEquals("1", ratio.getDerivedUnit());
        MetricMultiQueryRespVO missing = MetricMultiQueryRules.align(List.of(left, response("2", "B")), "RATIO");
        assertEquals(2, missing.getRows().size()); for (Map<String,Object> row : missing.getRows()) assertNull(row.get("derived_value"));
    }
    @Test public void rejectsTruncatedGroupsAndUnequalAdditionUnits() {
        MetricQueryRespVO left = response("1", "A"), right = response("2", "A"); right.setTruncated(true);
        rejected(() -> MetricMultiQueryRules.align(List.of(left, right), "NONE"));
        MetricRespVO a = MetricQueryBuilderTest.snapshot(), b = MetricQueryBuilderTest.snapshot(); b.setId(11L); b.setUnit("人");
        MetricQueryReqVO first = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()), second = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()); second.setMetricId(11L);
        rejected(() -> MetricMultiQueryRules.comparable(List.of(a, b), List.of(first, second), "SUM"));
    }
    @Test public void rejectsAddingAveragesOrDistinctCountsEvenWithSameUnit() {
        MetricRespVO a = MetricQueryBuilderTest.snapshot(), b = MetricQueryBuilderTest.snapshot(); b.setId(11L);
        a.setAggregation("AVG"); b.setAggregation("AVG");
        MetricQueryReqVO first = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()), second = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()); second.setMetricId(11L);
        rejected(() -> MetricMultiQueryRules.comparable(List.of(a, b), List.of(first, second), "SUM"));
        a.setAggregation("COUNT_DISTINCT"); b.setAggregation("COUNT_DISTINCT");
        rejected(() -> MetricMultiQueryRules.comparable(List.of(a, b), List.of(first, second), "SUM"));
    }
    @Test public void duplicateOrMissingGroupKeysAreNotSilentlyOverwritten() {
        MetricQueryRespVO first = response("1", "A"), second = response("2", "A");
        first.setRows(List.of(Map.of("d_2", "A", "metric_value", "1"), Map.of("d_2", "A", "metric_value", "2")));
        rejected(() -> MetricMultiQueryRules.align(List.of(first, second), "NONE"));
        first.setRows(List.of(Map.of("metric_value", "1")));
        rejected(() -> MetricMultiQueryRules.align(List.of(first, second), "NONE"));
    }
    @Test public void parallelComparisonPreservesNonNumericMeasureType() {
        MetricQueryRespVO first = response("2026-01-01", "A"), second = response("2026-02-01", "A");
        first.getColumns().get(1).setDataType("date"); second.getColumns().get(1).setDataType("date");
        MetricMultiQueryRespVO result = MetricMultiQueryRules.align(List.of(first, second), "NONE");
        assertEquals("date", result.getColumns().get(1).getDataType()); assertEquals("date", result.getColumns().get(2).getDataType());
    }
    @Test public void laterMetricMappingsAreCheckedPairwiseNotOnlyAgainstFirstPublication() {
        MetricRespVO first = MetricQueryBuilderTest.snapshot(), second = MetricQueryBuilderTest.snapshot(), third = MetricQueryBuilderTest.snapshot();
        second.setId(11L); third.setId(12L);
        @SuppressWarnings("unchecked") Map<String,Object> secondMappings = (Map<String,Object>) second.getSourceSnapshot().get("propertyColumns");
        @SuppressWarnings("unchecked") Map<String,Object> thirdMappings = (Map<String,Object>) third.getSourceSnapshot().get("propertyColumns");
        secondMappings.put("5", Map.of("propertyId", 5L, "columnName", "cost"));
        thirdMappings.put("5", Map.of("propertyId", 5L, "columnName", "other_cost"));
        MetricQueryReqVO a = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()), b = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request()), c = MetricQueryBuilder.normalize(MetricQueryBuilderTest.request());
        b.setMetricId(11L); c.setMetricId(12L);
        rejected(() -> MetricMultiQueryRules.comparable(List.of(first, second, third), List.of(a, b, c), "NONE"));
    }
    @Test public void completeTrendChronologyIsAllowedButDescendingRankIsNot() {
        MetricRespVO a = MetricQueryBuilderTest.snapshot(), b = MetricQueryBuilderTest.snapshot(); b.setId(11L);
        MetricQueryReqVO first = MetricQueryBuilderTest.request(), second = MetricQueryBuilderTest.request(); second.setMetricId(11L);
        for (MetricQueryReqVO request : List.of(first, second)) { request.setTimeBucket("MONTH"); request.setSortBy("time_bucket"); request.setSortDirection("ASC"); request.setRequireComplete(true); }
        MetricMultiQueryRules.comparable(List.of(a, b), List.of(MetricQueryBuilder.normalize(first), MetricQueryBuilder.normalize(second)), "NONE");
        first.setRequireComplete(false); first.setSortDirection("DESC");
        rejected(() -> MetricMultiQueryRules.comparable(List.of(a, b), List.of(MetricQueryBuilder.normalize(first), MetricQueryBuilder.normalize(second)), "NONE"));
    }
    private static MetricQueryRespVO response(String value, String group) {
        MetricQueryRespVO response = new MetricQueryRespVO(); response.setMetricName("销售额"); response.setUnit("元"); response.setTruncated(false);
        MetricQueryColumnVO dimension = new MetricQueryColumnVO(); dimension.setKey("d_2"); dimension.setLabel("地区"); dimension.setDataType("string");
        MetricQueryColumnVO measure = new MetricQueryColumnVO(); measure.setKey("metric_value"); measure.setLabel("销售额"); measure.setDataType("decimal");
        response.setColumns(List.of(dimension, measure)); response.setRows(List.of(Map.of("d_2", group, "metric_value", value))); return response;
    }
}
