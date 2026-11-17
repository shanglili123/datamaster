package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.impl.MetricAnalysisServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

/** Saved conditions never imply source authorization or source execution. Mock-only cases. */
public class MetricAnalysisPersistenceTest {
    private MetricAnalysisServiceImpl service;
    private MetricAnalysisMapper analyses;
    private MetricMapper metrics;
    private MetricVersionMapper versions;
    private MetricAnalysisAccessGuard access;
    private MetricQuerySourceGuard sources;
    private MetricQueryExecutor executor;
    private MetricDO metric;

    @Before public void setUp() throws Exception {
        LoginUser user = new LoginUser(7L, 1L, new SysUser(), Set.of("ont:analysis:add", "ont:analysis:edit"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        service = new MetricAnalysisServiceImpl(); analyses = mock(MetricAnalysisMapper.class);
        metrics = mock(MetricMapper.class); versions = mock(MetricVersionMapper.class);
        access = mock(MetricAnalysisAccessGuard.class); sources = mock(MetricQuerySourceGuard.class); executor = mock(MetricQueryExecutor.class);
        inject("analysisMapper", analyses); inject("metricMapper", metrics); inject("versionMapper", versions);
        inject("access", access); inject("sourceGuard", sources); inject("executor", executor);
        inject("policy", new MetricQueryPolicy(true, 15, 1));
        metric = new MetricDO(); metric.setId(10L); metric.setOntologyId(20L); metric.setConceptId(30L);
        metric.setCode("sales"); metric.setStatus("PUBLISHED"); metric.setPublishedVersion(1);
        when(metrics.selectById(10L)).thenReturn(metric);
        MetricVersionDO version = new MetricVersionDO(); version.setSnapshotJson(JSON.toJSONString(MetricQueryBuilderTest.snapshot()));
        when(versions.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(version);
        OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
        when(access.ontology(20L)).thenReturn(ontology);
    }
    @After public void tearDown() { SecurityContextHolder.clearContext(); }

    @Test public void createPinsOwnerAndExactVersionAndPersistsConditionsWithoutExecuting() {
        MetricAnalysisSaveReqVO request = request();
        request.getQuery().setTimeBucket("MONTH"); request.getQuery().setSortBy("time_bucket"); request.getQuery().setSortDirection("ASC");
        request.getQuery().setRequireComplete(true); request.getQuery().setFilters(List.of(MetricQueryBuilderTest.filter(2L, "EQ", "")));
        when(analyses.insert(any(MetricAnalysisDO.class))).thenAnswer(invocation -> {
            invocation.<MetricAnalysisDO>getArgument(0).setId(70L); return 1;
        });
        assertEquals(Long.valueOf(70L), service.create(request));
        ArgumentCaptor<MetricAnalysisDO> saved = ArgumentCaptor.forClass(MetricAnalysisDO.class); verify(analyses).insert(saved.capture());
        assertEquals(Long.valueOf(7L), saved.getValue().getOwnerId()); assertEquals(Integer.valueOf(0), saved.getValue().getRevision());
        assertEquals(Long.valueOf(10L), saved.getValue().getMetricId()); assertEquals(Integer.valueOf(1), saved.getValue().getVersion());
        MetricQueryReqVO query = JSON.parseObject(saved.getValue().getQueryJson(), MetricQueryReqVO.class);
        assertEquals("MONTH", query.getTimeBucket()); assertEquals("time_bucket", query.getSortBy()); assertTrue(query.getRequireComplete());
        assertEquals("", query.getFilters().get(0).getValue());
        verifyNoInteractions(sources, executor);
    }

    @Test public void staleRevisionCannotOverwriteSavedConditions() {
        MetricAnalysisDO saved = saved(); when(analyses.selectOwnedForUpdate(70L, 7L)).thenReturn(saved);
        MetricAnalysisSaveReqVO request = request(); request.setId(70L); request.setRevision(2);
        rejected(() -> service.update(request)); verify(analyses, never()).updateById(any(MetricAnalysisDO.class));
        assertEquals(Integer.valueOf(3), saved.getRevision()); verifyNoInteractions(sources, executor);
    }

    @Test public void updateUsesOwnedRowLockAndAdvancesRevisionWithoutExecuting() {
        when(analyses.selectOwnedForUpdate(70L, 7L)).thenReturn(saved()); when(analyses.updateById(any(MetricAnalysisDO.class))).thenReturn(1);
        MetricAnalysisSaveReqVO request = request(); request.setId(70L); request.setRevision(3); request.setName("新分析名称");
        assertEquals(Integer.valueOf(1), service.update(request));
        org.mockito.InOrder order = inOrder(analyses);
        order.verify(analyses).selectOwnedForUpdate(70L, 7L);
        order.verify(analyses).updateById(argThat((MetricAnalysisDO row) -> row.getRevision() == 4 && "新分析名称".equals(row.getName()) && Long.valueOf(7L).equals(row.getOwnerId())));
        verifyNoInteractions(sources, executor);
    }

    @Test public void foreignOwnerIsDeniedBeforeMetricLookupOrMutation() {
        MetricAnalysisDO foreign = saved(); foreign.setOwnerId(8L);
        when(analyses.selectOwnedForUpdate(70L, 7L)).thenReturn(foreign);
        when(analyses.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(foreign);
        rejected(() -> service.get(70L)); rejected(() -> service.delete(70L));
        verifyNoInteractions(metrics, versions, access, sources, executor); verify(analyses, never()).deleteById(anyLong());
    }

    @Test public void disabledMetricConditionsRemainReadableButCannotExecute() {
        metric.setStatus("DISABLED"); MetricAnalysisDO saved = saved();
        when(analyses.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(saved);
        assertEquals(Integer.valueOf(1), service.get(70L).getQuery().getVersion());
        rejected(() -> service.query(MetricQueryBuilderTest.request())); verifyNoInteractions(sources, executor);
    }

    @Test public void corruptSavedVersionIdentityCannotBeReadAsAnotherQuery() {
        MetricAnalysisDO saved = saved(); saved.setVersion(2);
        when(analyses.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(saved);
        rejected(() -> service.get(70L)); verifyNoInteractions(metrics, versions, sources, executor);
    }

    @Test public void zeroRowWritesNeverReportSuccessfulSaveOrDelete() {
        MetricAnalysisSaveReqVO create = request(); rejected(() -> service.create(create));
        when(analyses.selectOwnedForUpdate(70L, 7L)).thenAnswer(ignored -> saved());
        MetricAnalysisSaveReqVO update = request(); update.setId(70L); update.setRevision(3);
        rejected(() -> service.update(update)); rejected(() -> service.delete(70L));
        verifyNoInteractions(sources, executor);
    }

    @Test public void reportedInsertWithoutGeneratedIdentityIsRejected() {
        when(analyses.insert(any(MetricAnalysisDO.class))).thenReturn(1);
        rejected(() -> service.create(request())); verifyNoInteractions(sources, executor);
    }

    @Test public void exhaustedAnalysisRevisionCannotWrapOrWrite() {
        MetricAnalysisDO row = saved(); row.setRevision(Integer.MAX_VALUE);
        when(analyses.selectOwnedForUpdate(70L, 7L)).thenReturn(row);
        MetricAnalysisSaveReqVO update = request(); update.setId(70L); update.setRevision(Integer.MAX_VALUE);
        rejected(() -> service.update(update)); verify(analyses, never()).updateById(any(MetricAnalysisDO.class));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), row.getRevision()); verifyNoInteractions(sources, executor);
    }

    private static MetricAnalysisSaveReqVO request() {
        MetricAnalysisSaveReqVO request = new MetricAnalysisSaveReqVO(); request.setOntologyId(20L); request.setName("我的销售分析");
        request.setQuery(MetricQueryBuilderTest.request()); return request;
    }
    private static MetricAnalysisDO saved() {
        MetricAnalysisDO row = new MetricAnalysisDO(); row.setId(70L); row.setOwnerId(7L); row.setOntologyId(20L);
        row.setMetricId(10L); row.setVersion(1); row.setRevision(3); row.setName("我的销售分析");
        row.setQueryJson(JSON.toJSONString(MetricQueryBuilderTest.request())); return row;
    }
    private void inject(String name, Object value) throws Exception {
        Field field = service.getClass().getDeclaredField(name); field.setAccessible(true); field.set(service, value);
    }
}
