package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.impl.MetricAnalysisServiceImpl;
import org.junit.Test;
import org.mockito.InOrder;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricComparisonServiceTest {
    @Test public void bothPeriodsUseOneExplicitPublicationAndAuthorizeBeforeExecuting() throws Exception {
        Fixture fixture = new Fixture(); MetricComparisonReqVO request = fixture.request();
        when(fixture.executor.execute(any(), any(), any())).thenReturn(value("10"), value("5"));
        MetricComparisonRespVO result = fixture.service.compare(request);
        assertEquals("100", result.getChangePercent()); assertEquals("2025-12-01T00:00", result.getBaselineStartTime());
        InOrder ordered = inOrder(fixture.guard, fixture.executor);
        ordered.verify(fixture.guard).authorize(any(), argThat(plan -> plan.getRequest().getStartTime().equals("2026-01-01T00:00:00")), any());
        ordered.verify(fixture.guard).authorize(any(), argThat(plan -> plan.getRequest().getStartTime().equals("2025-12-01T00:00")), any());
        ordered.verify(fixture.executor).execute(any(), argThat(plan -> plan.getRequest().getStartTime().equals("2026-01-01T00:00:00")), any());
        ordered.verify(fixture.executor).execute(any(), argThat(plan -> plan.getRequest().getStartTime().equals("2025-12-01T00:00")), any());
        verify(fixture.versions, times(1)).selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
    @Test public void baselineAuthorizationFailureExecutesNeitherPeriod() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.guard.authorize(any(), any(), any())).thenReturn(fixture.source).thenThrow(new com.datamaster.common.exception.ServiceException("基期未授权"));
        rejected(() -> fixture.service.compare(fixture.request())); verifyNoInteractions(fixture.executor);
    }
    @Test public void rejectsTimeFilterBeforeAuthorizingAnySource() throws Exception {
        Fixture fixture = new Fixture(); MetricComparisonReqVO request = fixture.request();
        request.getQuery().setFilters(List.of(MetricQueryBuilderTest.filter(3L, "GE", "2026-01-01T00:00:00")));
        rejected(() -> fixture.service.compare(request)); verifyNoInteractions(fixture.guard, fixture.executor);
    }
    private static MetricQueryRespVO value(String amount) {
        MetricQueryRespVO response = new MetricQueryRespVO(); response.setTruncated(false); response.setRows(List.of(Map.of("metric_value", amount)));
        MetricQueryColumnVO column = new MetricQueryColumnVO(); column.setKey("metric_value"); column.setDataType("decimal"); response.setColumns(List.of(column)); return response;
    }
    private static class Fixture {
        final MetricAnalysisServiceImpl service = new MetricAnalysisServiceImpl();
        final MetricMapper metrics = mock(MetricMapper.class); final MetricVersionMapper versions = mock(MetricVersionMapper.class);
        final MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class);
        final MetricQuerySourceGuard guard = mock(MetricQuerySourceGuard.class); final MetricQueryExecutor executor = mock(MetricQueryExecutor.class);
        final DbQueryProperty source = new DbQueryProperty("DORIS", "unused", "reader", null, 1, "ads", null);
        Fixture() throws Exception {
            inject(service, "metricMapper", metrics); inject(service, "versionMapper", versions); inject(service, "access", access);
            inject(service, "sourceGuard", guard); inject(service, "executor", executor); inject(service, "policy", new MetricQueryPolicy(true, "40", "reader", 15, 1));
            MetricRespVO publication = MetricQueryBuilderTest.snapshot();
            MetricDO metric = new MetricDO(); metric.setId(10L); metric.setOntologyId(20L); metric.setConceptId(30L); metric.setCode("sales"); metric.setStatus("PUBLISHED");
            when(metrics.selectById(10L)).thenReturn(metric);
            MetricVersionDO version = new MetricVersionDO(); version.setSnapshotJson(JSON.toJSONString(publication));
            when(versions.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(version);
            OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(access.ontology(20L)).thenReturn(ontology);
            when(guard.authorize(any(), any(), any())).thenReturn(source);
        }
        MetricComparisonReqVO request() { MetricComparisonReqVO request = new MetricComparisonReqVO(); request.setQuery(MetricQueryBuilderTest.request()); request.setMode("PREVIOUS_MONTH"); return request; }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
