package com.datamaster.module.ontology.service.metric;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.impl.*;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Publication lifecycle only; no source connection, SQL execution, Spring context or migration. */
public class MetricDerivedLifecycleTest {
    @Test public void publishesImmutableFormalDerivationAndDisablesWithoutDeletingVersions() throws Exception {
        MetricServiceImpl service = new MetricServiceImpl();
        MetricMapper metrics = mock(MetricMapper.class); MetricVersionMapper versions = mock(MetricVersionMapper.class);
        ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        PropertyMapper properties = mock(PropertyMapper.class); PropertyColumnMapper columns = mock(PropertyColumnMapper.class);
        IDatasourceApiService datasources = mock(IDatasourceApiService.class); OntologyMapper ontologies = mock(OntologyMapper.class);
        MetricMetadataLockMapper locks = mock(MetricMetadataLockMapper.class); ISpaceFlowApi flows = mock(ISpaceFlowApi.class);
        MetricAccessGuard access = mock(MetricAccessGuard.class);
        inject(service, "metricMapper", metrics); inject(service, "metricVersionMapper", versions); inject(service, "conceptMapper", concepts);
        inject(service, "conceptTableMapper", tables); inject(service, "propertyMapper", properties); inject(service, "propertyColumnMapper", columns);
        inject(service, "datasourceApiService", datasources); inject(service, "ontologyMapper", ontologies); inject(service, "metadataLockMapper", locks);
        inject(service, "spaceFlows", flows); inject(service, "accessGuard", access);
        MetricSaveReqVO definition = new MetricSaveReqVO(); definition.setOntologyId(20L); definition.setConceptId(30L); definition.setCode("average_per_order"); definition.setName("每笔金额"); definition.setDescription("同一来源聚合后计算金额与订单行数比率");
        definition.setOwner("负责人"); definition.setUnit("元/笔"); definition.setGrain("订单"); definition.setConceptTableId(40L);
        definition.setAggregation("SUM"); definition.setValuePropertyId(1L); definition.setDerivedOperation("RATIO"); definition.setSecondaryAggregation("COUNT"); definition.setPrimaryUnit("元"); definition.setSecondaryUnit("笔");
        MetricDO metric = new MetricDO(); metric.setId(10L); metric.setOntologyId(20L); metric.setConceptId(30L); metric.setCode(definition.getCode()); metric.setName(definition.getName()); metric.setConceptTableId(40L); metric.setDefinitionJson(MetricRules.definitionJson(definition)); metric.setStatus("DRAFT"); metric.setPublishedVersion(0); metric.setRevision(0);
        when(metrics.updateById(any(MetricDO.class))).thenReturn(1);
        when(metrics.selectById(10L)).thenReturn(metric); when(metrics.selectForUpdate(10L)).thenReturn(metric); when(locks.lockOntology(20L)).thenReturn(20L);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
        ConceptTableDO binding = new ConceptTableDO(); binding.setId(40L); binding.setConceptId(30L); binding.setDatasourceId(50L); binding.setDatabaseName("business"); binding.setTableName("orders"); when(tables.selectById(40L)).thenReturn(binding);
        PropertyDO property = new PropertyDO(); property.setId(1L); property.setConceptId(30L); property.setCode("amount"); property.setName("金额"); property.setDataType("decimal"); when(properties.selectById(1L)).thenReturn(property);
        PropertyColumnDO mapping = new PropertyColumnDO(); mapping.setPropertyId(1L); mapping.setConceptTableId(40L); mapping.setColumnName("amount"); when(columns.selectByConceptTableId(40L)).thenReturn(List.of(mapping));
        DatasourceRespDTO datasource = new DatasourceRespDTO(); datasource.setId(50L); datasource.setDatasourceType("MySql"); when(datasources.getDatasourceById(50L)).thenReturn(datasource);
        OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(ontologies.selectById(20L)).thenReturn(ontology); when(flows.policy(1L, "space01")).thenReturn(new ISpaceFlowApi.Policy());
        List<MetricVersionDO> saved = new ArrayList<>();
        when(versions.insert(any(MetricVersionDO.class))).thenAnswer(invocation -> { MetricVersionDO version = invocation.getArgument(0); saved.add(version); return 1; });
        service.publish(10L, 0);
        assertEquals("PUBLISHED", metric.getStatus()); assertEquals(Integer.valueOf(1), metric.getPublishedVersion());
        String frozen = saved.get(0).getSnapshotJson(); MetricRespVO publication = JSON.parseObject(frozen, MetricRespVO.class);
        assertEquals("RATIO", publication.getDerivedOperation()); assertEquals("COUNT", publication.getSecondaryAggregation()); assertEquals("SPACE_FLOW", publication.getSourceSnapshot().get("sourceMode"));
        service.disable(10L, 1); assertEquals("DISABLED", metric.getStatus()); assertEquals(1, saved.size());
        definition.setName("每笔新金额"); metric.setName(definition.getName()); metric.setDefinitionJson(MetricRules.definitionJson(definition));
        when(versions.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(saved.get(0));
        service.publish(10L, 2);
        assertEquals(Integer.valueOf(2), metric.getPublishedVersion()); assertEquals(2, saved.size()); assertEquals(frozen, saved.get(0).getSnapshotJson());
        assertEquals("每笔新金额", JSON.parseObject(saved.get(1).getSnapshotJson(), MetricRespVO.class).getName());
        when(versions.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(saved.get(0), saved.get(1));
        Integer revision = metric.getRevision();
        service.restoreDraft(10L, 1, revision);
        assertEquals("每笔金额", metric.getName()); assertEquals(Integer.valueOf(2), metric.getPublishedVersion());
        assertEquals("PUBLISHED", metric.getStatus()); assertTrue(metric.getDraftChanged()); assertEquals(frozen, saved.get(0).getSnapshotJson());
        try { service.restoreDraft(10L, 1, revision); fail("Stale revision must reject recovery"); } catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
        verify(versions, times(2)).insert(any(MetricVersionDO.class));
        verify(versions, times(3)).selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)); verifyNoMoreInteractions(versions);
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
