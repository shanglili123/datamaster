package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Local mock-only scenarios, never started automatically. */
public class ConceptTablePreviewAdmissionTest {
    @Test public void deniedAdmissionNeverCreatesSourceQuery() throws Exception {
        Fixture f = new Fixture(); doThrow(new com.datamaster.common.exception.ServiceException("准入缺项")).when(f.guard).admit(eq(f.table), eq(f.ontology), isNull());
        try { f.service.previewData(40L, 20, null); fail("Must deny before connecting"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertEquals("准入缺项", expected.getMessage()); }
        verifyNoInteractions(f.factory);
    }
    @Test public void queryAndMaskingUseExplicitColumnsAndCloseOnMaskingFailure() throws Exception {
        Fixture f = new Fixture(); DbQuery query = mock(DbQuery.class); when(f.factory.createDbQuery(any())).thenReturn(query);
        DbColumn column = mock(DbColumn.class); when(column.getColName()).thenReturn("id"); when(column.getDataType()).thenReturn("integer");
        when(query.getTableColumns(any(com.datamaster.common.database.constants.DbQueryProperty.class), eq("orders"))).thenReturn(List.of(column));
        when(query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0))).thenReturn(new com.datamaster.common.database.core.PageResult<>(1, List.of(Map.of("id", "1"))));
        doThrow(new com.datamaster.common.exception.ServiceException("脱敏失败")).when(f.guard).mask(eq(f.table), eq(f.ontology), anyList());
        try { f.service.previewData(40L, 20, null); fail("Must not return raw rows"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertEquals("脱敏失败", expected.getMessage()); }
        verify(query).queryByPage(argThat(sql -> sql.startsWith("SELECT id FROM orders") && !sql.contains("*")), anyMap(), eq(0L), eq(20L), eq(0));
        verify(f.guard).admit(f.table, f.ontology, List.of("id")); verify(query).close();
    }
    @Test public void createRechecksStoredParentAfterLockBeforeInsertingBinding() throws Exception {
        Fixture f = new Fixture(); f.ontology.setId(20L);
        com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper locks = mock(com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper.class);
        inject(f.service, "metadataLocks", locks); when(locks.lockOntology(20L)).thenReturn(20L);
        when(f.guard.source(any(ConceptTableDO.class))).thenReturn(f.ontology)
                .thenThrow(new com.datamaster.common.exception.ServiceException("概念已删除"));
        com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO request = new com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO();
        request.setConceptId(30L); request.setDatasourceId(9L); request.setTableName("orders");
        try { f.service.createConceptTable(request); fail("Must recheck parent after locking"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertEquals("概念已删除", expected.getMessage()); }
        org.mockito.InOrder order = inOrder(f.guard, locks);
        order.verify(f.guard).source(any(ConceptTableDO.class)); order.verify(locks).lockOntology(20L); order.verify(f.guard).source(any(ConceptTableDO.class));
        verify(f.mapper, never()).insert(any(ConceptTableDO.class)); verifyNoInteractions(f.factory);
    }
    @Test public void explicitProjectionAlsoDefinesResponseHeaders() throws Exception {
        Fixture f = new Fixture(); DbQuery query = mock(DbQuery.class); when(f.factory.createDbQuery(any())).thenReturn(query);
        DbColumn id = new DbColumn(); id.setColName("id"); id.setDataType("integer");
        DbColumn name = new DbColumn(); name.setColName("name"); name.setDataType("varchar");
        when(query.getTableColumns(any(com.datamaster.common.database.constants.DbQueryProperty.class), eq("orders"))).thenReturn(List.of(id, name));
        when(query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0))).thenReturn(new com.datamaster.common.database.core.PageResult<>(1, List.of(Map.of("id", "1"))));
        when(f.guard.mask(eq(f.table), eq(f.ontology), anyList())).thenAnswer(invocation -> invocation.getArgument(2));
        var result = f.service.previewData(40L, 20, "{\"columns\":[\"id\"]}");
        assertEquals(List.of("id"), result.getColumns()); assertEquals(1, result.getRows().size()); verify(query).close();
    }
    @Test public void missingResultDataIsNotPresentedAsAnEmptyPreview() throws Exception {
        Fixture f = new Fixture(); DbQuery query = mock(DbQuery.class); when(f.factory.createDbQuery(any())).thenReturn(query);
        DbColumn id = new DbColumn(); id.setColName("id"); id.setDataType("integer");
        when(query.getTableColumns(any(com.datamaster.common.database.constants.DbQueryProperty.class), eq("orders"))).thenReturn(List.of(id));
        when(query.queryByPage(anyString(), anyMap(), eq(0L), eq(20L), eq(0))).thenReturn(new com.datamaster.common.database.core.PageResult<>(0, null));
        try { f.service.previewData(40L, 20, null); fail("Missing result must not look like zero rows"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
        verify(f.guard, never()).mask(any(), any(), anyList()); verify(query).close();
    }
    private static class Fixture {
        final ConceptTableServiceImpl service = new ConceptTableServiceImpl(); final ConceptTableMapper mapper = mock(ConceptTableMapper.class);
        final ConceptTableConsumptionGuard guard = mock(ConceptTableConsumptionGuard.class); final DataSourceFactory factory = mock(DataSourceFactory.class);
        final IDatasourceApiService sources = mock(IDatasourceApiService.class); final ConceptTableDO table = new ConceptTableDO(); final OntologyDO ontology = new OntologyDO();
        Fixture() throws Exception {
            table.setId(40L); table.setConceptId(30L); table.setDatasourceId(9L); table.setDatabaseName("business"); table.setTableName("orders");
            ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(mapper.selectById(40L)).thenReturn(table); when(guard.source(table)).thenReturn(ontology);
            when(guard.physicalTable(eq(table), any())).thenReturn("orders");
            DatasourceRespDTO source = new DatasourceRespDTO(); source.setId(9L); source.setDatasourceType("MySql"); source.setIp("unused"); source.setPort(3306L); source.setDatasourceConfig("{\"dbname\":\"business\",\"username\":\"unit_reader\",\"password\":\"unit-test-only\"}"); when(sources.getDatasourceById(9L)).thenReturn(source);
            inject(service, "conceptTableMapper", mapper); inject(service, "consumption", guard); inject(service, "dataSourceFactory", factory); inject(service, "datasourceApiService", sources);
        }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
