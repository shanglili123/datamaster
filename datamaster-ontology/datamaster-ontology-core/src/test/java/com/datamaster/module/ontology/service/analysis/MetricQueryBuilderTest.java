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
