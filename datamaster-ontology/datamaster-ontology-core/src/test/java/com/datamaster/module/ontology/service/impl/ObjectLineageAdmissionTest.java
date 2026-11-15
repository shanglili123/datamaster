package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ObjectLineageAdmissionTest {
    @Test public void failedAdmissionDoesNotReturnDecisionSnapshotsOrReadExternalLineage() throws Exception {
        ObjectLineageServiceImpl service = new ObjectLineageServiceImpl(); ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        ActionMapper actions = mock(ActionMapper.class); ActionExecutionMapper executions = mock(ActionExecutionMapper.class);
        IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class); OntologyConsumptionContext context = mock(OntologyConsumptionContext.class);
        inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables); inject(service, "actionMapper", actions); inject(service, "actionExecutionMapper", executions); inject(service, "tableGovernanceApiService", governance); inject(service, "consumptionContext", context);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
        OntologyDO ontology = new OntologyDO(); ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(context.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(ontology);
        ConceptTableDO table = new ConceptTableDO(); table.setId(40L); table.setConceptId(30L); table.setDatasourceId(50L); table.setTableName("orders"); when(tables.selectByConceptId(30L)).thenReturn(List.of(table));
        inject(service, "datasources", permittedDatasources());
        when(governance.resolveTable(any())).thenThrow(new com.datamaster.common.exception.ServiceException("新流程未满足"));
        var response = service.objectLineage(30L, null, null);
        assertEquals(Boolean.FALSE, response.getPermission().getAccessible());
        verifyNoInteractions(actions, executions);
        com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO partial = new com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO(); partial.setAccessAllowed(true); partial.setAllowedColumns(List.of("id"));
        doReturn(partial).when(governance).resolveTable(any());
        assertEquals(Boolean.FALSE, service.objectLineage(30L, null, null).getPermission().getAccessible());
        verifyNoInteractions(actions, executions);
        partial.setAllowedColumns(List.of());
        assertEquals(Boolean.FALSE, service.objectLineage(30L, null, null).getPermission().getAccessible());
        doReturn(null).when(governance).resolveTable(any());
        assertEquals(Boolean.FALSE, service.objectLineage(30L, null, null).getPermission().getAccessible());
        verifyNoInteractions(actions, executions);
    }
    @Test public void multipleSourcesCannotBeAuthorizedByOnlyTheFirstBinding() throws Exception {
        ObjectLineageServiceImpl service = new ObjectLineageServiceImpl(); ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        ActionMapper actions = mock(ActionMapper.class); ActionExecutionMapper executions = mock(ActionExecutionMapper.class);
        IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class); OntologyConsumptionContext context = mock(OntologyConsumptionContext.class);
        com.datamaster.neo4j.service.LineageDataService lineage = mock(com.datamaster.neo4j.service.LineageDataService.class);
        inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables); inject(service, "actionMapper", actions); inject(service, "actionExecutionMapper", executions);
        inject(service, "tableGovernanceApiService", governance); inject(service, "consumptionContext", context); inject(service, "lineageDataService", lineage);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
        OntologyDO ontology = new OntologyDO(); ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(context.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(ontology);
        ConceptTableDO first = new ConceptTableDO(); first.setId(40L); first.setConceptId(30L); first.setDatasourceId(50L); first.setTableName("orders");
        ConceptTableDO second = new ConceptTableDO(); second.setId(41L); second.setConceptId(30L); second.setDatasourceId(51L); second.setTableName("private_orders");
        when(tables.selectByConceptId(30L)).thenReturn(List.of(first, second));
        var response = service.objectLineage(30L, null, null);
        assertEquals(Boolean.FALSE, response.getPermission().getAccessible()); assertTrue(response.getPermission().getMessage().contains("唯一来源"));
        verifyNoInteractions(actions, executions, governance, lineage);
    }
    @Test public void revokedDatasourceSpaceLinkBlocksSnapshotsBeforeGovernanceOrExternalReads() throws Exception {
        ObjectLineageServiceImpl service = new ObjectLineageServiceImpl();
        ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        ActionMapper actions = mock(ActionMapper.class); ActionExecutionMapper executions = mock(ActionExecutionMapper.class);
        IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class);
        OntologyConsumptionContext context = mock(OntologyConsumptionContext.class);
        com.datamaster.common.datasource.mgmt.api.IDatasourceApiService sources = mock(com.datamaster.common.datasource.mgmt.api.IDatasourceApiService.class);
        com.datamaster.neo4j.service.LineageDataService lineage = mock(com.datamaster.neo4j.service.LineageDataService.class);
        inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables); inject(service, "actionMapper", actions);
        inject(service, "actionExecutionMapper", executions); inject(service, "tableGovernanceApiService", governance);
        inject(service, "consumptionContext", context); inject(service, "datasources", sources); inject(service, "lineageDataService", lineage);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
        OntologyDO ontology = new OntologyDO(); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
        when(context.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(ontology);
        ConceptTableDO table = new ConceptTableDO(); table.setId(40L); table.setConceptId(30L); table.setDatasourceId(50L); table.setTableName("orders");
        when(tables.selectByConceptId(30L)).thenReturn(List.of(table));
        var response = service.objectLineage(30L, null, null);
        assertEquals(Boolean.FALSE, response.getPermission().getAccessible()); assertTrue(response.getPermission().getMessage().contains("未有效关联"));
        verify(sources).hasDatasourceSpace(50L, 1L, "space01"); verifyNoInteractions(governance, actions, executions, lineage);
        when(sources.hasDatasourceSpace(50L, 1L, "space01")).thenThrow(new IllegalStateException("lookup unavailable"));
        var unavailable = service.objectLineage(30L, null, null);
        assertEquals(Boolean.FALSE, unavailable.getPermission().getAccessible());
        assertTrue(unavailable.getPermission().getMessage().contains("核验不可用"));
        verifyNoInteractions(governance, actions, executions, lineage);
    }
    @Test public void admittedSoleSourceStillReturnsAnEmptyHistoryWithoutExecutingDataQueries() throws Exception {
        ObjectLineageServiceImpl service = new ObjectLineageServiceImpl();
        ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        ActionMapper actions = mock(ActionMapper.class); ActionExecutionMapper executions = mock(ActionExecutionMapper.class);
        IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class);
        OntologyConsumptionContext context = mock(OntologyConsumptionContext.class);
        var sources = permittedDatasources();
        inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables); inject(service, "actionMapper", actions);
        inject(service, "actionExecutionMapper", executions); inject(service, "tableGovernanceApiService", governance);
        inject(service, "consumptionContext", context); inject(service, "datasources", sources);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
        OntologyDO ontology = new OntologyDO(); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
        when(context.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(ontology);
        ConceptTableDO table = new ConceptTableDO(); table.setId(40L); table.setConceptId(30L); table.setDatasourceId(50L); table.setTableName("orders");
        when(tables.selectByConceptId(30L)).thenReturn(List.of(table));
        var allowed = new com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO(); allowed.setAccessAllowed(true);
        when(governance.resolveTable(any())).thenReturn(allowed);
        when(actions.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        var response = service.objectLineage(30L, null, null);
        assertEquals(Boolean.TRUE, response.getPermission().getAccessible()); assertTrue(response.getVersions().isEmpty());
        org.mockito.InOrder order = inOrder(sources, governance, actions);
        order.verify(sources).hasDatasourceSpace(50L, 1L, "space01");
        order.verify(governance).resolveTable(argThat(request -> Long.valueOf(50L).equals(request.getDatasourceId())
                && Long.valueOf(1L).equals(request.getSpaceId()) && "space01".equals(request.getSpaceCode()) && "orders".equals(request.getTableName())));
        order.verify(actions).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        allowed.setAccessAllowed(false); allowed.setMessage("该表质量准入已撤销");
        clearInvocations(actions);
        var denied = service.objectLineage(30L, null, null);
        assertEquals(Boolean.FALSE, denied.getPermission().getAccessible()); assertEquals("该表质量准入已撤销", denied.getPermission().getMessage());
        verifyNoInteractions(actions, executions);
        verify(sources, times(2)).hasDatasourceSpace(50L, 1L, "space01");
    }
    private static com.datamaster.common.datasource.mgmt.api.IDatasourceApiService permittedDatasources() {
        var sources = mock(com.datamaster.common.datasource.mgmt.api.IDatasourceApiService.class);
        when(sources.hasDatasourceSpace(50L, 1L, "space01")).thenReturn(true); return sources;
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
