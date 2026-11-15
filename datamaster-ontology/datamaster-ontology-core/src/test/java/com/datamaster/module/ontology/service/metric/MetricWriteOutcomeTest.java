package com.datamaster.module.ontology.service.metric;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.impl.MetricAccessGuard;
import com.datamaster.module.ontology.service.impl.MetricServiceImpl;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Mapper outcome regressions; does not prove transaction rollback on a real database. */
public class MetricWriteOutcomeTest {
    @Test public void draftInsertRequiresOneRowAndGeneratedIdentity() throws Exception {
        Fixture f = new Fixture();
        reject(() -> f.service.create(f.definition()));
        when(f.metrics.insert(any(MetricDO.class))).thenReturn(1);
        reject(() -> f.service.create(f.definition()));
        when(f.metrics.insert(any(MetricDO.class))).thenAnswer(invocation -> { invocation.<MetricDO>getArgument(0).setId(10L); return 1; });
        assertEquals(Long.valueOf(10L), f.service.create(f.definition()));
    }

    @Test public void unchangedDraftIsNoOpButChangedDraftNeedsOneUpdatedRow() throws Exception {
        Fixture f = new Fixture(); MetricSaveReqVO request = f.definition(); request.setId(10L); request.setRevision(0);
        assertEquals(Integer.valueOf(0), f.service.update(request)); verify(f.metrics, never()).updateById(any(MetricDO.class));
        request.setName("更新名称"); reject(() -> f.service.update(request));
        verify(f.metrics).updateById(any(MetricDO.class));
    }

    @Test public void draftDeletionCannotReportSuccessWhenNoRowWasDeleted() throws Exception {
        Fixture f = new Fixture(); reject(() -> f.service.delete(10L));
        when(f.metrics.deleteById(10L)).thenReturn(1); assertEquals(Integer.valueOf(1), f.service.delete(10L));
        verifyNoInteractions(f.versions);
    }

    @Test public void failedDisableIsAnErrorAndDoesNotDeleteHistoricalVersions() throws Exception {
        Fixture f = new Fixture(); f.metric.setStatus("PUBLISHED"); f.metric.setPublishedVersion(1);
        reject(() -> f.service.disable(10L, 0)); verifyNoInteractions(f.versions);
    }

    @Test public void failedSnapshotInsertNeverUpdatesPublicationStatus() throws Exception {
        Fixture f = new Fixture();
        reject(() -> f.service.publish(10L, 0));
        verify(f.versions).insert(any(MetricVersionDO.class)); verify(f.metrics, never()).updateById(any(MetricDO.class));
        assertEquals("DRAFT", f.metric.getStatus()); assertEquals(Integer.valueOf(0), f.metric.getPublishedVersion());
    }

    @Test public void failedPublicationStatusWriteIsReportedForTransactionalRollback() throws Exception {
        Fixture f = new Fixture(); when(f.versions.insert(any(MetricVersionDO.class))).thenReturn(1);
        reject(() -> f.service.publish(10L, 0));
        verify(f.versions).insert(any(MetricVersionDO.class)); verify(f.metrics).updateById(any(MetricDO.class));
        // Direct mock calls cannot test rollback; only the thrown failure is asserted here.
    }

    @Test public void exhaustedPublicationRevisionRejectsBeforeWritingSnapshot() throws Exception {
        Fixture f = new Fixture(); f.metric.setRevision(Integer.MAX_VALUE);
        reject(() -> f.service.publish(10L, Integer.MAX_VALUE));
        verify(f.versions, never()).insert(any(MetricVersionDO.class)); verify(f.metrics, never()).updateById(any(MetricDO.class));
    }

    @Test public void foreignPublicationCannotDriveDraftChangedOrRepublishing() throws Exception {
        Fixture f = new Fixture(); f.metric.setPublishedVersion(1); f.metric.setStatus("PUBLISHED");
        com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO publication = new com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO();
        org.springframework.beans.BeanUtils.copyProperties(f.definition(), publication);
        publication.setId(10L); publication.setOntologyId(99L); publication.setPublishedVersion(1); publication.setStatus("PUBLISHED");
        MetricVersionDO version = new MetricVersionDO(); version.setSnapshotJson(com.alibaba.fastjson2.JSON.toJSONString(publication));
        when(f.versions.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(version);
        MetricSaveReqVO request = f.definition(); request.setId(10L); request.setRevision(0); request.setName("新名称");
        reject(() -> f.service.update(request));
        reject(() -> f.service.publish(10L, 0));
        verify(f.metrics, never()).updateById(any(MetricDO.class)); verify(f.versions, never()).insert(any(MetricVersionDO.class));
    }

    private static class Fixture {
        final MetricServiceImpl service = new MetricServiceImpl(); final MetricMapper metrics = mock(MetricMapper.class);
        final MetricVersionMapper versions = mock(MetricVersionMapper.class); final MetricDO metric = new MetricDO();
        Fixture() throws Exception {
            ConceptMapper concepts = mock(ConceptMapper.class); MetricMetadataLockMapper locks = mock(MetricMetadataLockMapper.class);
            ConceptTableMapper tables = mock(ConceptTableMapper.class); PropertyColumnMapper columns = mock(PropertyColumnMapper.class);
            com.datamaster.common.datasource.mgmt.api.IDatasourceApiService sources = mock(com.datamaster.common.datasource.mgmt.api.IDatasourceApiService.class);
            OntologyMapper ontologies = mock(OntologyMapper.class);
            com.datamaster.module.governance.api.flow.ISpaceFlowApi flows = mock(com.datamaster.module.governance.api.flow.ISpaceFlowApi.class);
            inject(service, "conceptTableMapper", tables); inject(service, "propertyColumnMapper", columns);
            inject(service, "datasourceApiService", sources); inject(service, "ontologyMapper", ontologies); inject(service, "spaceFlows", flows);
            ConceptTableDO binding = new ConceptTableDO(); binding.setId(40L); binding.setConceptId(30L); binding.setDatasourceId(50L); binding.setDatabaseName("business"); binding.setTableName("orders");
            when(tables.selectById(40L)).thenReturn(binding); when(columns.selectByConceptTableId(40L)).thenReturn(java.util.List.of());
            com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO source = new com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO(); source.setId(50L); source.setDatasourceType("MySql");
            when(sources.getDatasourceById(50L)).thenReturn(source);
            OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
            when(ontologies.selectById(20L)).thenReturn(ontology); when(flows.policy(1L, "space01")).thenReturn(new com.datamaster.module.governance.api.flow.ISpaceFlowApi.Policy());
            inject(service, "metricMapper", metrics); inject(service, "metricVersionMapper", versions);
            inject(service, "accessGuard", mock(MetricAccessGuard.class)); inject(service, "metadataLockMapper", locks); inject(service, "conceptMapper", concepts);
            ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
            when(locks.lockOntology(20L)).thenReturn(20L);
            metric.setId(10L); metric.setOntologyId(20L); metric.setConceptId(30L); metric.setConceptTableId(40L); metric.setCode("sales"); metric.setName("销售额");
            metric.setRevision(0); metric.setStatus("DRAFT"); metric.setPublishedVersion(0); metric.setDefinitionJson(MetricRules.definitionJson(definition()));
            when(metrics.selectById(10L)).thenReturn(metric); when(metrics.selectForUpdate(10L)).thenReturn(metric);
        }
        MetricSaveReqVO definition() {
            MetricSaveReqVO definition = new MetricSaveReqVO(); definition.setOntologyId(20L); definition.setConceptId(30L);
            definition.setCode("sales"); definition.setName("销售额"); definition.setDescription("尚未发布的口径");
            definition.setOwner("负责人"); definition.setUnit("笔"); definition.setGrain("订单"); definition.setConceptTableId(40L); definition.setAggregation("COUNT"); return definition;
        }
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Zero-row write must not report success"); }
        catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
