package com.datamaster.module.ontology.service.metric;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MetricRulesTest {
    private MetricSaveReqVO draft() {
        MetricSaveReqVO vo = new MetricSaveReqVO();
        vo.setOntologyId(1L);
        vo.setConceptId(2L);
        vo.setCode("order_count");
        vo.setName("订单数");
        vo.setDescription("统计符合固定过滤条件的订单行数");
        return vo;
    }
    private MetricFilterVO filter(String operator, Object value) {
        MetricFilterVO filter = new MetricFilterVO();
        filter.setPropertyId(3L);
        filter.setOperator(operator);
        filter.setValue(value);
        return filter;
    }
    private void rejected(Runnable runnable) {
        try {
            runnable.run();
            fail("expected ServiceException");
        } catch (ServiceException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void incompleteDraftAllowedButPublicationRequiresDefinition() {
        MetricSaveReqVO vo = MetricRules.normalize(draft());
        assertTrue(vo.getDimensionPropertyIds().isEmpty());
        assertTrue(vo.getFixedFilters().isEmpty());
        rejected(() -> MetricRules.requirePublishFields(vo));
        vo.setOwner("数据负责人");
        vo.setUnit("笔");
        vo.setGrain("一行一个订单");
        vo.setConceptTableId(4L);
        vo.setAggregation("COUNT");
        MetricRules.requirePublishFields(vo); // COUNT(*) intentionally has no value property.
        vo.setAggregation("COUNT_DISTINCT");
        rejected(() -> MetricRules.requirePublishFields(vo));
    }

    @Test
    public void normalizeDeduplicatesDimensionsAndDoesNotMutateCaller() {
        MetricSaveReqVO vo = draft();
        vo.setDimensionPropertyIds(Arrays.asList(3L, 3L, 5L));
        vo.setFixedFilters(Collections.singletonList(filter("IS_NULL", Collections.singletonMap("bad", true))));
        MetricSaveReqVO normalized = MetricRules.normalize(vo);
        assertEquals(Arrays.asList(3L, 5L), normalized.getDimensionPropertyIds());
        assertNull(normalized.getFixedFilters().get(0).getValue());
        assertNotNull(vo.getFixedFilters().get(0).getValue());
        vo.setDimensionPropertyIds(null);
        vo.setFixedFilters(null);
        assertTrue(MetricRules.normalize(vo).getFixedFilters().isEmpty());
    }

    @Test
    public void filtersRejectObjectsArraysAndEmptyIn() {
        MetricSaveReqVO vo = draft();
        vo.setFixedFilters(Collections.singletonList(filter("EQ", Arrays.asList(1, 2))));
        rejected(() -> MetricRules.normalize(vo));
        vo.setFixedFilters(Collections.singletonList(filter("EQ", Collections.singletonMap("x", 1))));
        rejected(() -> MetricRules.normalize(vo));
        vo.setFixedFilters(Collections.singletonList(filter("IN", Collections.emptyList())));
        rejected(() -> MetricRules.normalize(vo));
        vo.setFixedFilters(Collections.singletonList(filter("IN", Arrays.asList(1, Collections.singletonMap("x", 1)))));
        rejected(() -> MetricRules.normalize(vo));
        vo.setFixedFilters(Collections.singletonList(filter("IN", Arrays.asList("1", "9007199254740993.000000001"))));
        MetricSaveReqVO normalized = MetricRules.normalize(vo);
        MetricRules.validateTypedFilter(normalized.getFixedFilters().get(0), "decimal");
    }

    @Test
    public void numericFiltersPreservePrecisionAndRejectMalformedValues() {
        MetricRules.validateTypedFilter(filter("GE", "9007199254740993.000000001"), "decimal");
        rejected(() -> MetricRules.validateTypedFilter(filter("EQ", "NaN"), "decimal"));
        rejected(() -> MetricRules.validateTypedFilter(filter("EQ", "1 OR 1=1"), "integer"));
        rejected(() -> MetricRules.validateTypedFilter(filter("EQ", true), "integer"));
        MetricRules.validateTypedFilter(filter("EQ", "2026-10-27"), "date");
        rejected(() -> MetricRules.validateTypedFilter(filter("EQ", "2026-02-30"), "date"));
    }

    @Test
    public void revisionAndIdentityRejectStaleOrCrossOntologyUpdates() {
        MetricRules.checkRevision(2, 2);
        rejected(() -> MetricRules.checkRevision(null, 2));
        rejected(() -> MetricRules.checkRevision(1, 2));
        MetricDO current = new MetricDO();
        current.setId(7L);
        current.setOntologyId(1L);
        current.setConceptId(2L);
        current.setCode("order_count");
        MetricSaveReqVO vo = draft();
        vo.setId(7L);
        MetricRules.checkIdentity(current, vo);
        vo.setOntologyId(9L);
        rejected(() -> MetricRules.checkIdentity(current, vo));
        vo.setOntologyId(1L);
        vo.setCode("different_code");
        rejected(() -> MetricRules.checkIdentity(current, vo));
    }

    @Test
    public void snapshotsAreIndependentAndIdempotenceIncludesActualMappings() {
        MetricRespVO working = new MetricRespVO();
        org.springframework.beans.BeanUtils.copyProperties(draft(), working);
        working.setId(7L);
        working.setRevision(0);
        working.setDimensionPropertyIds(new ArrayList<>(Collections.singletonList(3L)));
        working.setFixedFilters(new ArrayList<>(Collections.singletonList(filter("EQ", "ok"))));
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("columnName", "order_id");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("datasourceId", 1L);
        source.put("tableName", "ads_orders");
        source.put("schemaName", null);
        source.put("propertyColumns", column);
        MetricRespVO snapshot = MetricRules.snapshot(working, source, 1, 1);
        String frozen = JSON.toJSONString(snapshot);
        assertTrue(snapshot.getSourceSnapshot().containsKey("schemaName"));
        MetricRespVO stored = JSON.parseObject(frozen, MetricRespVO.class);
        assertTrue(MetricRules.samePublishedDefinition(working, source, stored));
        column.put("columnName", "renamed_column");
        assertFalse(MetricRules.samePublishedDefinition(working, source, stored));
        column.put("columnName", "order_id");
        working.getDimensionPropertyIds().clear();
        working.getFixedFilters().get(0).setValue("changed");
        column.put("columnName", "new_order_id");
        assertEquals(frozen, JSON.toJSONString(snapshot));
        assertFalse(MetricRules.samePublishedDefinition(working, source, stored));
        assertEquals("PUBLISHED", stored.getStatus());
        assertEquals(Integer.valueOf(1), stored.getPublishedVersion());
        assertFalse(stored.getDraftChanged());
        assertFalse(MetricRules.definitionJson(working).contains("sourceSnapshot"));
        assertFalse(MetricRules.definitionJson(working).contains("createBy"));
    }

    @Test
    public void sourceLayerAndAttributeTypesAreExplicit() {
        assertTrue(MetricRules.adsSource("warehouse", "ads_orders"));
        assertTrue(MetricRules.adsSource("ads", "orders"));
        assertFalse(MetricRules.adsSource("warehouse", "dwd_orders"));
        assertFalse(MetricRules.adsSource("ads", null));
        assertTrue(MetricRules.numeric("integer"));
        assertTrue(MetricRules.numeric("decimal"));
        assertFalse(MetricRules.numeric("string"));
        assertTrue(MetricRules.temporal("date"));
        assertFalse(MetricRules.temporal("string"));
    }
}
