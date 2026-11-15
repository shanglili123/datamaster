package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryReqVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.Assert.*;

public class MetricQueryBuilderTest {
    @Test public void immutableDimensionsAreAcceptedWhileActualNullElementsAreRejected() {
        MetricRespVO metric = snapshot(); metric.setDimensionPropertyIds(List.of(2L));
        MetricQueryReqVO request = request(); request.setDimensionPropertyIds(List.of(2L));
        assertEquals(List.of(2L), MetricQueryBuilder.normalize(request).getDimensionPropertyIds());
        assertTrue(MetricQueryBuilder.compile(metric, request).getSql().contains("GROUP BY"));
        request.setDimensionPropertyIds(Arrays.asList(2L, null)); rejected(() -> MetricQueryBuilder.normalize(request));
        request.setDimensionPropertyIds(List.of(2L)); metric.setDimensionPropertyIds(Arrays.asList(2L, null));
        rejected(() -> MetricQueryBuilder.compile(metric, request));
    }
    @Test public void parameterizesInjectionAndUsesExplicitDimensionOrder() {
        MetricRespVO metric = snapshot(); MetricQueryReqVO query = request();
        query.setDimensionPropertyIds(Collections.singletonList(2L));
        String attack = "x' OR 1=1 --";
        query.setFilters(Collections.singletonList(filter(2L, "EQ", attack)));
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, query);
        assertFalse(plan.getSql().contains(attack)); assertTrue(plan.getParameters().contains(attack));
        assertTrue(plan.getSql().contains("GROUP BY `region` ORDER BY `region`"));
        assertEquals("d_2", plan.getColumns().get(0).getKey());
        assertEquals("metric_value", plan.getColumns().get(1).getKey());
        assertTrue(plan.getUsedColumns().containsAll(Arrays.asList("amount", "region", "event_time", "active")));
    }
    @Test public void totalDoesNotGroupByPublishedDimensionsAndLimitPlusOne() {
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(snapshot(), request());
        assertFalse(plan.getSql().contains("GROUP BY")); assertFalse(plan.getSql().contains("ORDER BY"));
        assertFalse(plan.getUsedColumns().contains("region"));
        assertEquals(101, plan.getParameters().get(plan.getParameters().size() - 1));
        assertTrue(plan.getSql().contains("`active` = ? AND `event_time` >= ? AND `event_time` < ?"));
        assertEquals(Boolean.TRUE, plan.getParameters().get(0));
    }
    @Test public void dateRangeIsHalfOpenAndStrictlyTyped() {
        MetricRespVO metric = snapshot();
        mapping(metric, 3L).put("dataType", "date");
        MetricQueryReqVO req = request(); req.setStartTime("2026-01-01"); req.setEndTime("2026-02-01");
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, req);
        assertTrue(plan.getParameters().get(1) instanceof java.sql.Date);
        req.setEndTime("2026-01-01"); rejected(() -> MetricQueryBuilder.compile(metric, req));
        req.setEndTime("2026-02-30"); rejected(() -> MetricQueryBuilder.compile(metric, req));
    }
    @Test public void datetimeRejectsSpacesOffsetAndInvalidCalendarDate() {
        rejected(() -> MetricQueryBuilder.typedValue("2026-01-01 00:00:00", "datetime"));
        rejected(() -> MetricQueryBuilder.typedValue("2026-01-01T00:00:00Z", "datetime"));
        rejected(() -> MetricQueryBuilder.typedValue("2026-02-30", "date"));
    }
    @Test public void timeRequiredAndMustBePairedIncreasing() {
        MetricQueryReqVO req = request(); req.setStartTime(null);
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        req.setEndTime(null); rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        req.setStartTime("2026-02-01T00:00:00"); req.setEndTime("2026-01-01T00:00:00");
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
    }
    @Test public void rejectsUnpublishedDimensionValueFilterAndSqlIdentifiers() {
        MetricQueryReqVO req = request(); req.setDimensionPropertyIds(Collections.singletonList(4L));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        req.setDimensionPropertyIds(Collections.emptyList()); req.setFilters(Collections.singletonList(filter(1L, "EQ", 2)));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        for (String id : Arrays.asList("a.b", "a b", "a`", "1col", "x;drop", "x--")) rejected(() -> MetricQueryBuilder.identifier(id));
        MetricRespVO bad = snapshot(); bad.getSourceSnapshot().put("schemaName", "other");
        rejected(() -> MetricQueryBuilder.compile(bad, request()));
    }
    @Test public void rejectsVersionIdentityAndDoesNotNeedLatestDraftDefinition() {
        MetricRespVO metric = snapshot(); MetricQueryReqVO req = request(); req.setVersion(2);
        rejected(() -> MetricQueryBuilder.compile(metric, req));
        MetricDO current = new MetricDO(); current.setId(10L); current.setOntologyId(20L); current.setConceptId(30L);
        current.setCode("sales"); current.setPublishedVersion(8); current.setDefinitionJson("changed draft");
        MetricQueryBuilder.checkIdentity(current, metric, 10L, 1);
        metric.setOntologyId(21L); rejected(() -> MetricQueryBuilder.checkIdentity(current, metric, 10L, 1));
    }
    @Test public void checksTypeScalarAndOperatorBoundaries() {
        assertEquals(new BigDecimal("12"), MetricQueryBuilder.typedValue("12", "integer"));
        assertEquals(Boolean.FALSE, MetricQueryBuilder.typedValue("false", "boolean"));
        rejected(() -> MetricQueryBuilder.typedValue("1.2", "integer"));
        rejected(() -> MetricQueryBuilder.typedValue("NaN", "decimal"));
        rejected(() -> MetricQueryBuilder.typedValue("1e1000000000", "decimal"));
        rejected(() -> MetricQueryBuilder.typedValue("TRUE", "boolean"));
        rejected(() -> MetricQueryBuilder.typedValue(null, "text"));
        rejected(() -> MetricQueryBuilder.typedValue(Collections.singletonMap("sql", "x"), "text"));
        rejected(() -> MetricQueryBuilder.typedValue(String.join("", Collections.nCopies(2001, "x")), "text"));
        MetricQueryReqVO req = request(); req.setFilters(Collections.singletonList(filter(2L, "LIKE", "x")));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
    }
    @Test public void decimalStringsBeyondJavascriptRangeRemainExactAndBounded() {
        for (String value : List.of("9007199254740993", "1e400", "1e-400", ".50", "+1.", "1e1000", "1e-1000")) {
            assertEquals(new BigDecimal(value), MetricQueryBuilder.typedValue(value, "decimal"));
        }
        for (String value : List.of("0x10", "0b10", "Infinity", "1_000", "1e1001", "1e-1001", "1".repeat(1001))) {
            rejected(() -> MetricQueryBuilder.typedValue(value, "decimal"));
        }
        for (String value : List.of("1.00", "100e-2", "1.20e1", "0.000")) assertEquals(new BigDecimal(value), MetricQueryBuilder.typedValue(value, "integer"));
        for (String value : List.of("1.20", "1e-1", "100e-3")) rejected(() -> MetricQueryBuilder.typedValue(value, "integer"));
    }
    @Test public void inAndNullFiltersAreBoundAndValidated() {
        MetricQueryReqVO req = request(); req.setFilters(Collections.singletonList(filter(2L, "IN", Arrays.asList("a", "b"))));
        assertTrue(MetricQueryBuilder.compile(snapshot(), req).getSql().contains("`region` IN (?, ?)"));
        req.setFilters(Collections.singletonList(filter(2L, "IN", Collections.emptyList())));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        req.setFilters(Collections.singletonList(filter(2L, "IN", Collections.nCopies(101, "a"))));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        req.setFilters(Collections.singletonList(filter(2L, "IN", Arrays.asList("a", null))));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        req.setFilters(Collections.singletonList(filter(2L, "IS_NULL", "x")));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        req.setFilters(Collections.singletonList(filter(2L, "IS_NULL", null)));
        assertTrue(MetricQueryBuilder.compile(snapshot(), req).getSql().contains("`region` IS NULL"));
    }
    @Test public void emptyTextFilterRemainsBoundAndIsDistinctFromSqlNull() {
        MetricQueryReqVO request = request(); request.setFilters(List.of(filter(2L, "EQ", "")));
        MetricQueryBuilder.Plan empty = MetricQueryBuilder.compile(snapshot(), request);
        assertTrue(empty.getSql().contains("`region` = ?")); assertTrue(empty.getParameters().contains(""));
        request.setFilters(List.of(filter(2L, "IS_NULL", null)));
        MetricQueryBuilder.Plan sqlNull = MetricQueryBuilder.compile(snapshot(), request);
        assertTrue(sqlNull.getSql().contains("`region` IS NULL")); assertFalse(sqlNull.getParameters().contains(""));
    }
    @Test public void capsFixedPlusUserFiltersAndDimensionsAndLimits() {
        MetricRespVO metric = snapshot(); metric.setFixedFilters(Collections.nCopies(21, filter(4L, "EQ", true)));
        rejected(() -> MetricQueryBuilder.compile(metric, request()));
        MetricQueryReqVO req = request(); req.setLimit(501); rejected(() -> MetricQueryBuilder.normalize(req));
        req.setLimit(0); rejected(() -> MetricQueryBuilder.normalize(req));
        req.setLimit(null); assertEquals(Integer.valueOf(100), MetricQueryBuilder.normalize(req).getLimit());
        req.setDimensionPropertyIds(Arrays.asList(2L, 2L)); rejected(() -> MetricQueryBuilder.normalize(req));
        req.setDimensionPropertyIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L)); rejected(() -> MetricQueryBuilder.normalize(req));
    }
    @Test public void countStarAndNumericAggregationWhitelist() {
        MetricRespVO metric = snapshot(); metric.setAggregation("COUNT"); metric.setValuePropertyId(null);
        assertTrue(MetricQueryBuilder.compile(metric, request()).getSql().contains("COUNT(*)"));
        metric.setAggregation("COUNT_DISTINCT"); rejected(() -> MetricQueryBuilder.compile(metric, request()));
        metric.setAggregation("SUM"); metric.setValuePropertyId(2L); rejected(() -> MetricQueryBuilder.compile(metric, request()));
        metric.setAggregation("SUM(amount) --"); rejected(() -> MetricQueryBuilder.compile(metric, request()));
    }
    @Test public void rejectsMissingMappingAndPropertyIdentity() {
        MetricRespVO metric = snapshot(); mapping(metric, 1L).put("propertyId", 900L);
        rejected(() -> MetricQueryBuilder.compile(metric, request()));
        MetricRespVO invalid = snapshot(); invalid.setUnit(" ");
        rejected(() -> MetricQueryBuilder.compile(invalid, request()));
    }

    @Test public void legacyRequestsDefaultToNoBucketAndRetainOriginalOrdering() {
        MetricQueryReqVO req = request(); req.setTimeBucket(null);
        req.setDimensionPropertyIds(Arrays.asList(2L, 4L));
        MetricRespVO metric = snapshot(); metric.setDimensionPropertyIds(Arrays.asList(2L, 4L));
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, req);
        assertEquals("NONE", plan.getRequest().getTimeBucket());
        assertNull(plan.getRequest().getSortBy()); assertNull(plan.getRequest().getSortDirection());
        assertTrue(plan.getSql().endsWith("GROUP BY `region`, `active` ORDER BY `region`, `active` LIMIT ?"));
        assertFalse(plan.getSql().contains("time_bucket"));
    }
    @Test public void metricTopNIsOrderedInSqlBeforeLimitWithAllGroupTieBreaks() {
        MetricRespVO metric = snapshot(); metric.setDimensionPropertyIds(Arrays.asList(2L, 4L));
        for (String direction : Arrays.asList("ASC", "DESC")) {
            MetricQueryReqVO req = request(); req.setDimensionPropertyIds(Arrays.asList(2L, 4L));
            req.setSortBy("metric_value"); req.setSortDirection(direction); req.setLimit(7);
            MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, req);
            assertTrue(plan.getSql().endsWith("ORDER BY `metric_value` " + direction + ", `region` ASC, `active` ASC LIMIT ?"));
            assertEquals(8, plan.getParameters().get(plan.getParameters().size() - 1));
            assertFalse(plan.getParameters().contains(direction));
        }
    }
    @Test public void dimensionSortUsesOnlySelectedGroupsAndStableRemainingDimensions() {
        MetricRespVO metric = snapshot(); metric.setDimensionPropertyIds(Arrays.asList(2L, 4L));
        for (String direction : Arrays.asList("ASC", "DESC")) {
            MetricQueryReqVO req = request(); req.setDimensionPropertyIds(Arrays.asList(2L, 4L));
            req.setSortBy("d_4"); req.setSortDirection(direction);
            assertTrue(MetricQueryBuilder.compile(metric, req).getSql()
                    .endsWith("ORDER BY `active` " + direction + ", `region` ASC LIMIT ?"));
        }
        MetricQueryReqVO total = request(); total.setSortBy("metric_value"); total.setSortDirection("DESC");
        String sql = MetricQueryBuilder.compile(snapshot(), total).getSql();
        assertFalse(sql.contains("GROUP BY")); assertTrue(sql.endsWith("ORDER BY `metric_value` DESC LIMIT ?"));
    }
    @Test public void rejectsSortInjectionUnselectedDimensionsAndUnpairedSortFields() {
        for (String key : Arrays.asList("region", "d_2", "metric_value DESC", "metric_value;DROP", "time_bucket", "d_999")) {
            MetricQueryReqVO req = request(); req.setSortBy(key); req.setSortDirection("DESC");
            rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        }
        for (String direction : Arrays.asList("desc", "DESC NULLS LAST", "DESC;DROP", "")) {
            MetricQueryReqVO req = request(); req.setSortBy("metric_value"); req.setSortDirection(direction);
            rejected(() -> MetricQueryBuilder.normalize(req));
        }
        MetricQueryReqVO req = request(); req.setSortBy("metric_value");
        rejected(() -> MetricQueryBuilder.normalize(req));
        req.setSortBy(null); req.setSortDirection("ASC"); rejected(() -> MetricQueryBuilder.normalize(req));
    }
    @Test public void calendarBucketsAreSourceAggregationsWithBoundHalfOpenRange() {
        for (String grain : Arrays.asList("DAY", "MONTH", "YEAR")) {
            MetricQueryReqVO req = request(); req.setTimeBucket(grain);
            req.setStartTime("2026-01-15T10:30:00"); req.setEndTime("2026-02-03T11:00:00");
            MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(snapshot(), req);
            String bucket = MetricSqlDialect.of("DORIS").bucket("`event_time`", grain);
            assertTrue(plan.getSql().startsWith("SELECT " + bucket + " AS `time_bucket`, SUM(`amount`)"));
            assertTrue(plan.getSql().endsWith("GROUP BY " + bucket + " ORDER BY " + bucket + " LIMIT ?"));
            assertTrue(plan.getSql().contains("`event_time` >= ? AND `event_time` < ?"));
            assertEquals(java.sql.Timestamp.valueOf("2026-01-15 10:30:00"), plan.getParameters().get(1));
            assertEquals(java.sql.Timestamp.valueOf("2026-02-03 11:00:00"), plan.getParameters().get(2));
            assertEquals("time_bucket", plan.getColumns().get(0).getKey());
            assertEquals("string", plan.getColumns().get(0).getDataType());
            assertEquals("metric_value", plan.getColumns().get(1).getKey());
            assertTrue(plan.getUsedColumns().contains("event_time"));
            assertFalse(plan.getUsedColumns().contains("region"));
        }
    }
    @Test public void bucketAndDimensionAreGroupedTogetherBeforeTopN() {
        MetricQueryReqVO req = request(); req.setTimeBucket("MONTH"); req.setDimensionPropertyIds(Collections.singletonList(2L));
        req.setSortBy("metric_value"); req.setSortDirection("DESC");
        String bucket = MetricSqlDialect.of("DORIS").bucket("`event_time`", "MONTH");
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(snapshot(), req);
        assertTrue(plan.getSql().contains("GROUP BY " + bucket + ", `region` ORDER BY `metric_value` DESC, " + bucket + " ASC, `region` ASC LIMIT ?"));
        req.setSortBy("time_bucket"); req.setSortDirection("DESC");
        assertTrue(MetricQueryBuilder.compile(snapshot(), req).getSql().endsWith("ORDER BY " + bucket + " DESC, `region` ASC LIMIT ?"));
        req.setTimeBucket("NONE"); rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
    }
    @Test public void rejectsBucketsWithoutPublishedTimeOrWithInvalidGrainAndType() {
        MetricQueryReqVO req = request(); req.setTimeBucket("DAY"); req.setStartTime(null); req.setEndTime(null);
        MetricRespVO noTime = snapshot(); noTime.setTimePropertyId(null);
        rejected(() -> MetricQueryBuilder.compile(noTime, req));
        MetricRespVO invalidTime = snapshot(); mapping(invalidTime, 3L).put("dataType", "string");
        rejected(() -> MetricQueryBuilder.compile(invalidTime, req));
        rejected(() -> MetricQueryBuilder.compile(snapshot(), req));
        for (String grain : Arrays.asList("day", "WEEK", "DAY);DROP", "")) {
            req.setTimeBucket(grain); rejected(() -> MetricQueryBuilder.normalize(req));
        }
        req.setTimeBucket("NONE"); MetricQueryBuilder.compile(noTime, req);
    }
    @Test public void dateBucketsPreservePartialDayRangeAndNormalizationCopiesControls() {
        MetricRespVO metric = snapshot(); mapping(metric, 3L).put("dataType", "date");
        MetricQueryReqVO req = request(); req.setTimeBucket("YEAR"); req.setSortBy("time_bucket"); req.setSortDirection("ASC");
        req.setStartTime("2026-01-15"); req.setEndTime("2026-02-03");
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, req);
        assertEquals(java.sql.Date.valueOf("2026-01-15"), plan.getParameters().get(1));
        assertEquals(java.sql.Date.valueOf("2026-02-03"), plan.getParameters().get(2));
        MetricQueryReqVO normalized = MetricQueryBuilder.normalize(req);
        req.setTimeBucket("NONE"); req.setSortBy(null);
        assertEquals("YEAR", normalized.getTimeBucket()); assertEquals("time_bucket", normalized.getSortBy());
        assertEquals("ASC", normalized.getSortDirection());
    }
    @Test public void flowSourcesUseDialectForEveryIdentifierAndBucketButLegacyStaysRestricted() {
        for (String type : Arrays.asList("MYSQL", "POSTGRESQL")) {
            MetricRespVO metric = snapshot(); Map<String, Object> source = metric.getSourceSnapshot();
            source.put("datasourceType", type); source.put("sourceMode", "SPACE_FLOW");
            source.put("databaseName", "business"); source.put("schemaName", "POSTGRESQL".equals(type) ? "reporting" : null);
            source.put("tableName", "orders");
            MetricQueryReqVO req = request(); req.setTimeBucket("MONTH"); req.setDimensionPropertyIds(Collections.singletonList(2L));
            req.setSortBy("metric_value"); req.setSortDirection("DESC"); req.setFilters(Collections.singletonList(filter(2L, "EQ", "west")));
            MetricSqlDialect dialect = MetricSqlDialect.of(type);
            MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, req);
            String bucket = dialect.bucket(dialect.quote("event_time"), "MONTH");
            assertTrue(plan.getSql().contains("FROM " + dialect.table("business", (String) source.get("schemaName"), "orders")));
            assertTrue(plan.getSql().contains(dialect.quote("region") + " = ?"));
            assertTrue(plan.getSql().contains("GROUP BY " + bucket + ", " + dialect.quote("region")));
            assertTrue(plan.getSql().contains("ORDER BY " + dialect.quote("metric_value") + " DESC"));
            if ("POSTGRESQL".equals(type)) { assertFalse(plan.getSql().contains("`")); assertTrue(plan.getSql().contains("TO_CHAR(")); }
            else assertTrue(plan.getSql().contains("DATE_FORMAT("));
            source.remove("sourceMode"); rejected(() -> MetricQueryBuilder.compile(metric, req));
        }
    }

    @Test public void serverPaginationUsesBoundOffsetAndRetainsStableOrdering() {
        MetricQueryReqVO query = request(); query.setDimensionPropertyIds(List.of(2L)); query.setPageNum(3); query.setLimit(20);
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(snapshot(), query);
        assertTrue(plan.getSql().endsWith("ORDER BY `region` LIMIT ? OFFSET ?"));
        assertEquals(21, plan.getParameters().get(plan.getParameters().size() - 2));
        assertEquals(40, plan.getParameters().get(plan.getParameters().size() - 1));
        query.setPageNum(3); query.setRequireComplete(true); rejected(() -> MetricQueryBuilder.normalize(query));
        query.setRequireComplete(false);
        query.setPageNum(1001); rejected(() -> MetricQueryBuilder.normalize(query));
        query.setPageNum(2); query.setDimensionPropertyIds(List.of()); rejected(() -> MetricQueryBuilder.normalize(query));
    }
    @Test public void completeTrendRejectsMetricRankingAndKeepsCanonicalTimeOrder() {
        MetricQueryReqVO query = request(); query.setTimeBucket("DAY"); query.setRequireComplete(true);
        query.setSortBy("metric_value"); query.setSortDirection("DESC"); rejected(() -> MetricQueryBuilder.normalize(query));
        query.setSortBy("time_bucket"); query.setSortDirection("DESC"); rejected(() -> MetricQueryBuilder.normalize(query));
        query.setSortDirection("ASC"); assertEquals("time_bucket", MetricQueryBuilder.normalize(query).getSortBy());
    }
    static MetricQueryReqVO request() {
        MetricQueryReqVO req = new MetricQueryReqVO(); req.setMetricId(10L); req.setVersion(1);
        req.setStartTime("2026-01-01T00:00:00"); req.setEndTime("2026-02-01T00:00:00"); return req;
    }
    static MetricRespVO snapshot() {
        MetricRespVO vo = new MetricRespVO(); vo.setId(10L); vo.setOntologyId(20L); vo.setConceptId(30L);
        vo.setConceptTableId(40L); vo.setCode("sales"); vo.setName("Sales"); vo.setUnit("CNY"); vo.setGrain("order");
        vo.setStatus("PUBLISHED"); vo.setPublishedVersion(1); vo.setAggregation("SUM"); vo.setValuePropertyId(1L);
        vo.setDimensionPropertyIds(Collections.singletonList(2L)); vo.setTimePropertyId(3L);
        vo.setFixedFilters(Collections.singletonList(filter(4L, "EQ", "true")));
        Map<String, Object> source = new LinkedHashMap<>(); source.put("datasourceId", 50L); source.put("datasourceType", "DORIS");
        source.put("databaseName", "ads"); source.put("tableName", "ads_orders"); source.put("schemaName", null);
        source.put("validationScope", "METADATA_ONLY");
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("1", field(1L, "amount", "decimal")); columns.put("2", field(2L, "region", "string"));
        columns.put("3", field(3L, "event_time", "datetime")); columns.put("4", field(4L, "active", "boolean"));
        source.put("propertyColumns", columns); vo.setSourceSnapshot(source); return vo;
    }
    static MetricFilterVO filter(Long id, String op, Object value) {
        MetricFilterVO f = new MetricFilterVO(); f.setPropertyId(id); f.setOperator(op); f.setValue(value); return f;
    }
    private static Map<String, Object> field(Long id, String column, String type) {
        Map<String, Object> f = new LinkedHashMap<>(); f.put("propertyId", id); f.put("columnName", column);
        f.put("propertyName", column); f.put("dataType", type); return f;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapping(MetricRespVO vo, Long id) {
        return (Map<String, Object>) ((Map<?, ?>) vo.getSourceSnapshot().get("propertyColumns")).get(String.valueOf(id));
    }
    static void rejected(Runnable action) {
        try { action.run(); fail("Expected a closed rejection"); } catch (ServiceException expected) { /* expected */ }
    }
}
