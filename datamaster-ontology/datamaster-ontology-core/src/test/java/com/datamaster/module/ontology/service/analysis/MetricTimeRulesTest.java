package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricTimeRulesTest {
    @Test public void convertsBusinessBoundsToSourceAndBucketsInBusinessOffset() {
        MetricRespVO snapshot = MetricQueryBuilderTest.snapshot(); snapshot.setSourceTimeOffset("+00:00"); snapshot.setBusinessTimeOffset("+08:00");
        assertEquals(480, MetricTimeRules.businessShiftMinutes(snapshot));
        assertEquals(LocalDateTime.parse("2025-12-31T16:00:00"), MetricTimeRules.toSource(LocalDateTime.parse("2026-01-01T00:00:00"), snapshot));
        var query = MetricQueryBuilderTest.request(); query.setTimeBucket("DAY");
        var plan = MetricQueryBuilder.compile(snapshot, query);
        assertTrue(plan.getSql().contains("DATE_FORMAT(DATE_ADD(`event_time`, INTERVAL 480 MINUTE), '%Y-%m-%d')"));
        assertEquals(java.sql.Timestamp.valueOf("2025-12-31 16:00:00"), plan.getParameters().get(1));
        assertEquals(java.sql.Timestamp.valueOf("2026-01-31 16:00:00"), plan.getParameters().get(2));
        assertFalse(plan.getSql().contains("+08:00"));
    }
    @Test public void legacyDefaultsRemainLocalAndOffsetsMustBePairedExplicitAndBounded() {
        MetricRespVO metric = MetricQueryBuilderTest.snapshot(); assertEquals(0, MetricTimeRules.businessShiftMinutes(metric));
        metric.setSourceTimeOffset("+00:00"); rejected(() -> MetricTimeRules.validate(metric));
        metric.setBusinessTimeOffset("Asia/Shanghai"); rejected(() -> MetricTimeRules.validate(metric));
        metric.setBusinessTimeOffset("+14:01"); rejected(() -> MetricTimeRules.validate(metric));
        metric.setBusinessTimeOffset("+08:00"); metric.setTimePropertyId(null); rejected(() -> MetricTimeRules.validate(metric));
        assertEquals(-330, MetricTimeRules.offset("-05:30").getTotalSeconds() / 60);
    }
    @Test public void postgresShiftUsesClosedIntegerIntervalExpression() {
        assertEquals("(\"event_time\" + INTERVAL '480 minutes')", MetricSqlDialect.of("PostgreSQL").shiftMinutes("\"event_time\"", 480));
        rejected(() -> MetricSqlDialect.of("DORIS").shiftMinutes("`event_time`", Integer.MIN_VALUE));
    }
    @Test public void timeConversionRejectsJdbcOutOfRangeYearsInsteadOfWrapping() {
        rejected(() -> MetricQueryBuilder.typedValue("0000-01-01", "date"));
        rejected(() -> MetricQueryBuilder.typedValue("+10000-01-01T00:00:00", "datetime"));
        MetricRespVO metric = MetricQueryBuilderTest.snapshot(); metric.setSourceTimeOffset("+00:00"); metric.setBusinessTimeOffset("+08:00");
        rejected(() -> MetricTimeRules.toSource(LocalDateTime.parse("0001-01-01T00:00:00"), metric));
    }
    @Test public void dateColumnsCannotAcquireTimeOfDayByGuessing() {
        MetricRespVO metric = MetricQueryBuilderTest.snapshot(); metric.setSourceTimeOffset("+00:00"); metric.setBusinessTimeOffset("+08:00");
        @SuppressWarnings("unchecked") var mappings = (java.util.Map<String,Object>) metric.getSourceSnapshot().get("propertyColumns");
        @SuppressWarnings("unchecked") var time = (java.util.Map<String,Object>) mappings.get("3"); time.put("dataType", "date");
        var query = MetricQueryBuilderTest.request(); query.setStartTime("2026-01-01"); query.setEndTime("2026-02-01");
        rejected(() -> MetricQueryBuilder.compile(metric, query));
    }
}
