package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.metric.MetricRules;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricFormalDerivedTest {
    @Test public void ratioCompilesAggregatesBeforeDivisionAndGuardsZeroDenominator() {
        MetricRespVO metric = ratio();
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request());
        assertTrue(plan.getSql().contains("SUM(`amount`)")); assertTrue(plan.getSql().contains("COUNT(*)"));
        assertTrue(plan.getSql().contains("NULLIF((COUNT(*)), 0)")); assertTrue(plan.getSql().contains(" AS `metric_value`"));
        assertEquals("decimal", plan.getColumns().get(0).getDataType());
        assertEquals("RATIO", JSON.parseObject(JSON.toJSONString(metric), MetricRespVO.class).getDerivedOperation());
    }
    @Test public void secondAggregateMappingIsIncludedInSourceAuthorizationAndSnapshot() {
        MetricRespVO metric = ratio(); metric.setSecondaryAggregation("SUM"); metric.setSecondaryValuePropertyId(5L); metric.setSecondaryUnit("CNY"); metric.setUnit("1");
        @SuppressWarnings("unchecked") Map<String,Object> mappings = (Map<String,Object>) metric.getSourceSnapshot().get("propertyColumns");
        mappings.put("5", Map.of("propertyId", 5L, "columnName", "cost", "dataType", "decimal", "propertyName", "成本"));
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request());
        assertTrue(plan.getUsedColumns().containsAll(List.of("amount", "cost"))); assertTrue(MetricRules.usedProperties(metric).contains(5L));
        String definition = MetricRules.definitionJson(metric); metric.setSecondaryValuePropertyId(6L);
        assertNotEquals(definition, MetricRules.definitionJson(metric)); rejected(() -> MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request()));
    }
    @Test public void publicationRejectsUnitMismatchArbitraryFormulaAndAddingAverages() {
        MetricRespVO metric = ratio(); metric.setUnit("%"); rejected(() -> MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request()));
        metric.setUnit("CNY/笔"); metric.setDerivedOperation("x / y; DROP"); rejected(() -> MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request()));
        metric.setDerivedOperation("SUM"); metric.setAggregation("AVG"); metric.setSecondaryAggregation("AVG"); metric.setSecondaryValuePropertyId(1L);
        metric.setPrimaryUnit("CNY"); metric.setSecondaryUnit("CNY"); metric.setUnit("CNY");
        rejected(() -> MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request()));
    }
    @Test public void oldSingleAggregateDefinitionIsUnchangedAndSecondaryFieldsCannotBeHidden() {
        MetricRespVO original = MetricQueryBuilderTest.snapshot(); String sql = MetricQueryBuilder.compile(original, MetricQueryBuilderTest.request()).getSql();
        assertTrue(sql.startsWith("SELECT SUM(`amount`) AS `metric_value`"));
        original.setSecondaryAggregation("COUNT"); rejected(() -> MetricQueryBuilder.compile(original, MetricQueryBuilderTest.request()));
    }
    @Test public void distinctTextIdentifiersProduceNumericAggregateForFormalRatio() {
        MetricRespVO metric = ratio(); metric.setSecondaryAggregation("COUNT_DISTINCT"); metric.setSecondaryValuePropertyId(2L);
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request());
        assertTrue(plan.getSql().contains("NULLIF((COUNT(DISTINCT `region`)), 0)"));
        assertTrue(plan.getUsedColumns().contains("region"));
        metric.setSecondaryAggregation("MIN"); rejected(() -> MetricQueryBuilder.compile(metric, MetricQueryBuilderTest.request()));
    }
    private static MetricRespVO ratio() {
        MetricRespVO metric = MetricQueryBuilderTest.snapshot(); metric.setDerivedOperation("RATIO"); metric.setSecondaryAggregation("COUNT");
        metric.setPrimaryUnit("CNY"); metric.setSecondaryUnit("笔"); metric.setUnit("CNY/笔"); return metric;
    }
}
