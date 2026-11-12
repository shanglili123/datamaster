package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.assets.api.governance.dto.*;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ObjectConsumptionColumnsTest {
    @Test public void strictQueriesCannotUseUnknownColumnsOrPartiallyAuthorizedProjection() throws Exception {
        ObjectInstanceQueryServiceImpl service = new ObjectInstanceQueryServiceImpl();
        ISpaceFlowApi flows = mock(ISpaceFlowApi.class); IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class);
        inject(service, "spaceFlows", flows); inject(service, "tableGovernanceApiService", governance);
        ISpaceFlowApi.Policy policy = new ISpaceFlowApi.Policy(); policy.setAssetRequired(true); when(flows.policy(1L, "space01")).thenReturn(policy);
        ConceptTableDO table = new ConceptTableDO(); table.setDatasourceId(9L); table.setTableName("orders");
        Method check = service.getClass().getDeclaredMethod("assertQueryColumns", ConceptTableDO.class, Long.class, String.class, List.class); check.setAccessible(true);
        rejected(() -> check.invoke(service, table, 1L, "space01", List.of())); verifyNoInteractions(governance);
        AssetsTableGovernanceRespDTO allowed = new AssetsTableGovernanceRespDTO(); allowed.setAccessAllowed(true); allowed.setAllowedColumns(List.of("id"));
        when(governance.resolveTable(any())).thenReturn(allowed);
        rejected(() -> check.invoke(service, table, 1L, "space01", List.of("id", "secret")));
        check.invoke(service, table, 1L, "space01", List.of("id"));
        verify(governance, times(2)).resolveTable(argThat(request -> request.getSpaceId().equals(1L) && request.getDatasourceId().equals(9L) && request.getColumnNames().contains("id")));
        rejected(() -> check.invoke(service, table, 1L, "space01", List.of("(SELECT secret FROM other)")));
    }
    @Test public void returnTimeFieldRevocationRejectsRawRowsBeforeMasking() throws Exception {
        ObjectInstanceQueryServiceImpl service = new ObjectInstanceQueryServiceImpl();
        IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class); inject(service, "tableGovernanceApiService", governance);
        ConceptTableDO table = new ConceptTableDO(); table.setDatasourceId(9L); table.setTableName("orders");
        AssetsTableGovernanceRespDTO denied = new AssetsTableGovernanceRespDTO(); denied.setAccessAllowed(true); denied.setAllowedColumns(List.of("id"));
        when(governance.resolveTable(any())).thenReturn(denied);
        Method mask = service.getClass().getDeclaredMethod("maskObjectRows", ConceptTableDO.class, Long.class, String.class, List.class); mask.setAccessible(true);
        rejected(() -> mask.invoke(service, table, 1L, "space01", List.of(Map.of("id", "1", "secret", "raw"))));
        verify(governance, never()).desensitizeResultData(any(), anyList(), any(), any(), anyString());
    }
    @Test public void sameNamedRelationTableWithUnknownSchemaIsNotTreatedAsAlreadyCheckedEndpoint() throws Exception {
        ObjectInstanceQueryServiceImpl service = new ObjectInstanceQueryServiceImpl();
        com.datamaster.module.ontology.dal.dataobject.RelationTableDO relation = new com.datamaster.module.ontology.dal.dataobject.RelationTableDO(); relation.setDatasourceId(9L); relation.setTableName("orders"); relation.setDatabaseName("business");
        ConceptTableDO table = new ConceptTableDO(); table.setDatasourceId(9L); table.setTableName("orders"); table.setDatabaseName("business"); table.setSchemaName("reporting");
        Method same = service.getClass().getDeclaredMethod("samePhysicalTable", com.datamaster.module.ontology.dal.dataobject.RelationTableDO.class, ConceptTableDO.class); same.setAccessible(true);
        assertEquals(false, same.invoke(service, relation, table));
        relation.setSchemaName("reporting"); assertEquals(true, same.invoke(service, relation, table));
        relation.setDatabaseName("other"); assertEquals(false, same.invoke(service, relation, table));
    }
    private interface Call { void run() throws Exception; }
    private static void rejected(Call call) throws Exception {
        try { call.run(); fail("Must reject unknown or unauthorized columns"); }
        catch (InvocationTargetException error) { assertTrue(error.getCause() instanceof com.datamaster.common.exception.ServiceException); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
