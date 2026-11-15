package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryReqVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard;
import com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext;
import com.datamaster.neo4j.service.LineageDataService;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Mock-only object consumption wiring; does not start a source or Neo4j. */
public class ObjectQueryAdmissionTest {
    @Test public void partialGrantRejectsBeforeReadingRowsAndDoesNotRecordSuccessfulLineage() throws Exception {
        Fixture f = new Fixture(); f.allowed.setAllowedColumns(List.of("other"));
        reject(() -> f.service.queryObjects(f.request()));
        verify(f.query, never()).queryByPage(anyString(), anyMap(), anyLong(), anyLong(), anyInt());
        verify(f.query).close(); verifyNoInteractions(f.lineage);
    }

    @Test public void absentResultDataFailsAndClosesSourceWithoutLineageWrite() throws Exception {
        Fixture f = new Fixture();
        when(f.query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0)))
                .thenReturn(new com.datamaster.common.database.core.PageResult<>(0, null));
        reject(() -> f.service.queryObjects(f.request()));
        verify(f.query).close(); verifyNoInteractions(f.lineage);
    }

    @Test public void validEmptyPageIsReturnedWithExplicitProjectionHeaders() throws Exception {
        Fixture f = new Fixture(); // No lineage service: this case never submits an asynchronous graph task.
        inject(f.service, "lineageDataService", null);
        when(f.query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0)))
                .thenReturn(new com.datamaster.common.database.core.PageResult<>(0, List.of()));
        ObjectInstanceQueryReqVO request = f.request(); request.setFilters("{\"columns\":[\"id\"]}");
        var response = f.service.queryObjects(request);
        assertEquals(Long.valueOf(0L), response.getTotal()); assertTrue(response.getRows().isEmpty());
        assertEquals(List.of("id"), response.getColumns()); assertEquals(Long.valueOf(40L), response.getTableBindingId());
        verify(f.query).queryByPage(eq("SELECT id FROM orders"), anyMap(), eq(0L), eq(20L), eq(0)); verify(f.query).close();
    }

    @Test public void absentPageOrMissingTotalNeverLooksLikeZeroObjects() throws Exception {
        Fixture missingPage = new Fixture(); reject(() -> missingPage.service.queryObjects(missingPage.request()));
        verify(missingPage.query).close(); verifyNoInteractions(missingPage.lineage);
        Fixture missingTotal = new Fixture();
        when(missingTotal.query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0)))
                .thenReturn(new com.datamaster.common.database.core.PageResult<>(null, List.of()));
        reject(() -> missingTotal.service.queryObjects(missingTotal.request()));
        verify(missingTotal.query).close(); verifyNoInteractions(missingTotal.lineage);
    }

    @Test public void revokedReturnPermissionClosesSourceWithoutReturningRowsOrRecordingLineage() throws Exception {
        Fixture f = new Fixture();
        AssetsTableGovernanceRespDTO revoked = new AssetsTableGovernanceRespDTO(); revoked.setAccessAllowed(false);
        when(f.governance.resolveTable(any())).thenReturn(f.allowed, revoked);
        when(f.query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0)))
                .thenReturn(new com.datamaster.common.database.core.PageResult<>(1, List.of(Map.of("id", "1", "name", "private"))));
        reject(() -> f.service.queryObjects(f.request())); verify(f.query).close(); verifyNoInteractions(f.lineage);
    }

    @Test public void nonemptyProjectionPreservesRowsAndRechecksPermissionBeforeReturning() throws Exception {
        Fixture f = new Fixture(); inject(f.service, "lineageDataService", null);
        when(f.query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0)))
                .thenReturn(new com.datamaster.common.database.core.PageResult<>(1, List.of(Map.of("id", "9007199254740993"))));
        ObjectInstanceQueryReqVO request = f.request(); request.setFilters("{\"columns\":[\"id\"]}");
        var response = f.service.queryObjects(request);
        assertEquals(List.of("id"), response.getColumns()); assertEquals(Long.valueOf(1L), response.getTotal());
        assertEquals("9007199254740993", response.getRows().get(0).get("id"));
        org.mockito.InOrder order = inOrder(f.governance, f.query);
        order.verify(f.governance).checkTableAccess(any());
        order.verify(f.governance).resolveTable(argThat(value -> value.getColumnNames().containsAll(List.of("id", "name"))));
        order.verify(f.query).queryByPage(eq("SELECT id FROM orders"), anyMap(), eq(0L), eq(20L), eq(0));
        order.verify(f.governance).resolveTable(argThat(value -> value.getColumnNames().equals(List.of("id"))));
        order.verify(f.query).close();
    }

    private static class Fixture {
        final ObjectInstanceQueryServiceImpl service = new ObjectInstanceQueryServiceImpl();
        final DbQuery query = mock(DbQuery.class); final LineageDataService lineage = mock(LineageDataService.class);
        final AssetsTableGovernanceRespDTO allowed = new AssetsTableGovernanceRespDTO();
        final IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class);
        Fixture() throws Exception {
            ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
            PropertyColumnMapper columns = mock(PropertyColumnMapper.class); PropertyMapper properties = mock(PropertyMapper.class);
            OntologyConsumptionContext context = mock(OntologyConsumptionContext.class); ConceptTableConsumptionGuard identity = mock(ConceptTableConsumptionGuard.class);
            IDatasourceApiService sources = mock(IDatasourceApiService.class); DataSourceFactory factory = mock(DataSourceFactory.class);
            ISpaceFlowApi flows = mock(ISpaceFlowApi.class);
            inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables); inject(service, "propertyColumnMapper", columns); inject(service, "propertyMapper", properties);
            inject(service, "consumptionContext", context); inject(service, "sourceIdentity", identity); inject(service, "datasourceApiService", sources);
            inject(service, "dataSourceFactory", factory); inject(service, "tableGovernanceApiService", governance); inject(service, "spaceFlows", flows); inject(service, "lineageDataService", lineage);
            ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
            OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
            when(context.resolve(eq(concept), any(), any(), any())).thenReturn(ontology);
            ConceptTableDO table = new ConceptTableDO(); table.setId(40L); table.setConceptId(30L); table.setDatasourceId(9L); table.setDatabaseName("business"); table.setTableName("orders");
            when(tables.selectByConceptId(30L)).thenReturn(List.of(table));
            List<PropertyColumnDO> mappings = new ArrayList<>();
            for (long id = 1; id <= 2; id++) {
                String name = id == 1 ? "id" : "name";
                PropertyColumnDO mapping = new PropertyColumnDO(); mapping.setPropertyId(id); mapping.setColumnName(name); mappings.add(mapping);
                PropertyDO property = new PropertyDO(); property.setId(id); property.setConceptId(30L); property.setCode(name); property.setDataType(id == 1 ? "integer" : "string");
                when(properties.selectById(id)).thenReturn(property);
            }
            when(columns.selectByConceptTableId(40L)).thenReturn(mappings);
            DatasourceRespDTO source = new DatasourceRespDTO(); source.setId(9L); source.setDatasourceType("MySql"); source.setIp("unused"); source.setPort(3306L);
            source.setDatasourceConfig("{\"dbname\":\"business\",\"username\":\"unit_reader\",\"password\":\"unit-test-only\"}");
            when(sources.getDatasourceById(9L)).thenReturn(source); when(sources.hasDatasourceSpace(9L, 1L, "space01")).thenReturn(true);
            when(factory.createDbQuery(any())).thenReturn(query); when(identity.physicalTable(eq(table), any())).thenReturn("orders");
            when(flows.policy(1L, "space01")).thenReturn(new ISpaceFlowApi.Policy());
            allowed.setAccessAllowed(true); when(governance.resolveTable(any())).thenReturn(allowed);
        }
        ObjectInstanceQueryReqVO request() {
            ObjectInstanceQueryReqVO request = new ObjectInstanceQueryReqVO(); request.setConceptId(30L); request.setTableBindingId(40L); request.setPageNum(1); request.setPageSize(20); return request;
        }
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Invalid admission/result must not be successful consumption"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
