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

/** Mock-only service wiring cases. Does not access any physical source. */
public class MetricMultiQueryServiceTest {
    @Test public void allFourPublicationsAreAuthorizedBeforeAnyExecution() throws Exception {
        Fixture f = new Fixture();
        MetricMultiQueryReqVO request = f.request(4);
        when(f.executor.execute(any(), any(), any())).thenAnswer(invocation -> value(invocation.<MetricRespVO>getArgument(0).getId()));

        MetricMultiQueryRespVO result = f.service.multiQuery(request);

        assertEquals(4, result.getMetrics().size()); assertEquals(4, result.getColumns().size());
        assertEquals("10", result.getRows().get(0).get("m_0")); assertEquals("13", result.getRows().get(0).get("m_3"));
        InOrder ordered = inOrder(f.guard, f.executor);
        for (long id = 10; id <= 13; id++) {
            final long expected = id;
            ordered.verify(f.guard).authorize(argThat(metric -> Long.valueOf(expected).equals(metric.getId())), any(), any());
        }
        for (long id = 10; id <= 13; id++) {
            final long expected = id;
            ordered.verify(f.executor).execute(argThat(metric -> Long.valueOf(expected).equals(metric.getId())), any(), any());
        }
        verify(f.versions, times(4)).selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test public void lastMetricAuthorizationFailureExecutesNoneOfTheFour() throws Exception {
        Fixture f = new Fixture(); MetricMultiQueryReqVO request = f.request(4);
        when(f.guard.authorize(any(), any(), any())).thenReturn(f.source, f.source, f.source)
                .thenThrow(new com.datamaster.common.exception.ServiceException("第四项未授权"));
        rejected(() -> f.service.multiQuery(request)); verifyNoInteractions(f.executor);
        verify(f.guard, times(4)).authorize(any(), any(), any());
    }

    @Test public void binaryArithmeticRejectsThreeMetricsBeforeSourceAuthorization() throws Exception {
        Fixture f = new Fixture(); MetricMultiQueryReqVO request = f.request(3); request.setDerivedOperation("RATIO");
        rejected(() -> f.service.multiQuery(request)); verifyNoInteractions(f.guard, f.executor);
    }

    @Test public void textAggregateCannotReachSourceExecutionForArithmetic() throws Exception {
        Fixture f = new Fixture(); MetricMultiQueryReqVO request = f.request(2); request.setDerivedOperation("RATIO");
        MetricRespVO text = f.publications.get(11L); text.setAggregation("MIN");
        text.setValuePropertyId(2L);
        f.storeVersion(text);
        // The first plan may be authorized, but execution of every plan waits for all checks.
        rejected(() -> f.service.multiQuery(request)); verifyNoInteractions(f.executor);
    }

    private static MetricQueryRespVO value(Long id) {
        MetricQueryRespVO response = new MetricQueryRespVO(); response.setMetricId(id); response.setVersion(1);
        response.setMetricName("指标" + id); response.setUnit("元"); response.setTruncated(false);
        MetricQueryColumnVO column = new MetricQueryColumnVO(); column.setKey("metric_value"); column.setDataType("decimal");
        response.setColumns(List.of(column)); response.setRows(List.of(Map.of("metric_value", id.toString()))); return response;
    }

    private static class Fixture {
        final MetricAnalysisServiceImpl service = new MetricAnalysisServiceImpl();
        final MetricMapper metrics = mock(MetricMapper.class); final MetricVersionMapper versions = mock(MetricVersionMapper.class);
        final MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class);
        final MetricQuerySourceGuard guard = mock(MetricQuerySourceGuard.class); final MetricQueryExecutor executor = mock(MetricQueryExecutor.class);
        final DbQueryProperty source = new DbQueryProperty("DORIS", "unused", "reader", null, 1, "ads", null);
        final Map<Long,MetricRespVO> publications = new LinkedHashMap<>();
        final List<MetricVersionDO> versionRows = new ArrayList<>();
        Fixture() throws Exception {
            inject(service, "metricMapper", metrics); inject(service, "versionMapper", versions); inject(service, "access", access);
            inject(service, "sourceGuard", guard); inject(service, "executor", executor);
            inject(service, "policy", new MetricQueryPolicy(true, "40", "reader", 15, 1));
            OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
            when(access.ontology(20L)).thenReturn(ontology); when(guard.authorize(any(), any(), any())).thenReturn(source);
        }
        MetricMultiQueryReqVO request(int count) {
            MetricMultiQueryReqVO request = new MetricMultiQueryReqVO(); List<MetricQueryReqVO> queries = new ArrayList<>();
            for (long id = 10; id < 10 + count; id++) {
                MetricRespVO publication = MetricQueryBuilderTest.snapshot(); publication.setId(id); publications.put(id, publication);
                MetricDO metric = new MetricDO(); metric.setId(id); metric.setOntologyId(20L); metric.setConceptId(30L); metric.setCode(publication.getCode()); metric.setStatus("PUBLISHED");
                when(metrics.selectById(id)).thenReturn(metric);
                MetricVersionDO version = new MetricVersionDO(); version.setSnapshotJson(JSON.toJSONString(publication)); versionRows.add(version);
                MetricQueryReqVO query = MetricQueryBuilderTest.request(); query.setMetricId(id); queries.add(query);
            }
            Iterator<MetricVersionDO> sequence = versionRows.iterator();
            when(versions.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(ignored -> sequence.next());
            request.setQueries(queries); return request;
        }
        void storeVersion(MetricRespVO publication) {
            int index = new ArrayList<>(publications.keySet()).indexOf(publication.getId());
            versionRows.get(index).setSnapshotJson(JSON.toJSONString(publication));
        }
    }
    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
