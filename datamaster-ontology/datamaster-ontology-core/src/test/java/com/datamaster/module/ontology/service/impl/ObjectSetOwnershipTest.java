package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.ObjectInstanceServiceImpl;
import com.datamaster.module.ontology.service.analysis.MetricAnalysisAccessGuard;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Metadata discovery is member-scoped, not source execution authorization. */
public class ObjectSetOwnershipTest {
    @Test public void deniedMembershipStopsBeforeEnumeratingConceptsOrBindings() throws Exception {
        ObjectInstanceServiceImpl service = new ObjectInstanceServiceImpl();
        MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class); ConceptMapper concepts = mock(ConceptMapper.class);
        inject(service, "access", access); inject(service, "conceptMapper", concepts);
        when(access.ontology(20L)).thenThrow(new ServiceException("无权访问所属空间"));
        reject(() -> service.listObjectSets(20L)); verifyNoInteractions(concepts);
    }
    @Test public void authorizedDiscoveryReturnsOnlyMatchingConceptAndPropertyMetadata() throws Exception {
        Fixture f = new Fixture();
        var result = f.service.listObjectSets(20L);
        assertEquals(1, result.size()); assertEquals(Long.valueOf(40L), result.get(0).getTableBindingId());
        assertEquals("id", result.get(0).getProperties().get(0).getPhysicalColumnName());
        verify(f.access).ontology(20L);
        f.property.setConceptId(99L); reject(() -> f.service.listObjectSets(20L));
    }
    @Test public void wrongBindingParentCannotBeListed() throws Exception {
        Fixture f = new Fixture(); f.table.setConceptId(99L);
        reject(() -> f.service.listObjectSets(20L)); verifyNoInteractions(f.columns, f.properties);
    }
    @Test public void rowQueryRejectsCrossConceptMappingRatherThanFallingBackToAllColumns() throws Exception {
        ObjectInstanceQueryServiceImpl service = new ObjectInstanceQueryServiceImpl(); PropertyColumnMapper columns = mock(PropertyColumnMapper.class); PropertyMapper properties = mock(PropertyMapper.class);
        inject(service, "propertyColumnMapper", columns); inject(service, "propertyMapper", properties);
        PropertyColumnDO mapping = new PropertyColumnDO(); mapping.setPropertyId(1L); mapping.setColumnName("secret");
        when(columns.selectByConceptTableId(40L)).thenReturn(List.of(mapping));
        PropertyDO foreign = new PropertyDO(); foreign.setId(1L); foreign.setConceptId(99L); when(properties.selectById(1L)).thenReturn(foreign);
        Method method = service.getClass().getDeclaredMethod("buildProperties", Long.class, Long.class); method.setAccessible(true);
        try { method.invoke(service, 40L, 30L); fail("Cross-concept property must fail"); }
        catch (InvocationTargetException failure) { assertTrue(failure.getCause() instanceof ServiceException); }
    }
    @Test public void incompleteStoredMappingIsRejectedInsteadOfSilentlyDisappearing() throws Exception {
        Fixture f = new Fixture(); PropertyColumnDO invalid = new PropertyColumnDO(); invalid.setPropertyId(1L); invalid.setColumnName("");
        when(f.columns.selectByConceptTableId(40L)).thenReturn(List.of(invalid));
        reject(() -> f.service.listObjectSets(20L)); verifyNoInteractions(f.properties);
    }
    @Test public void authorizedOntologyWithoutConceptsIsStillALegitimateEmptyList() throws Exception {
        Fixture f = new Fixture(); when(f.concepts.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        assertTrue(f.service.listObjectSets(20L).isEmpty()); verify(f.access).ontology(20L);
        verifyNoInteractions(f.tables, f.columns, f.properties);
    }
    @Test public void unavailableMappingsAreNotTreatedAsAnUnmappedObjectSet() throws Exception {
        Fixture f = new Fixture(); when(f.columns.selectByConceptTableId(40L)).thenReturn(null);
        reject(() -> f.service.listObjectSets(20L)); verifyNoInteractions(f.properties);
        ObjectInstanceQueryServiceImpl query = new ObjectInstanceQueryServiceImpl(); inject(query, "propertyColumnMapper", f.columns);
        Method method = query.getClass().getDeclaredMethod("buildProperties", Long.class, Long.class); method.setAccessible(true);
        try { method.invoke(query, 40L, 30L); fail("Unavailable mappings must not activate physical metadata fallback"); }
        catch (InvocationTargetException failure) { assertTrue(failure.getCause() instanceof ServiceException); }
    }
    private static class Fixture {
        final ObjectInstanceServiceImpl service = new ObjectInstanceServiceImpl();
        final MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class);
        final ConceptMapper concepts = mock(ConceptMapper.class); final ConceptTableMapper tables = mock(ConceptTableMapper.class);
        final PropertyColumnMapper columns = mock(PropertyColumnMapper.class); final PropertyMapper properties = mock(PropertyMapper.class);
        final ConceptTableDO table = new ConceptTableDO(); final PropertyDO property = new PropertyDO();
        Fixture() throws Exception {
            inject(service, "access", access); inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables);
            inject(service, "propertyColumnMapper", columns); inject(service, "propertyMapper", properties);
            ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L);
            when(concepts.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(concept));
            table.setId(40L); table.setConceptId(30L); table.setDatasourceId(9L); table.setTableName("orders"); when(tables.selectByConceptId(30L)).thenReturn(List.of(table));
            PropertyColumnDO mapping = new PropertyColumnDO(); mapping.setPropertyId(1L); mapping.setColumnName("id"); when(columns.selectByConceptTableId(40L)).thenReturn(List.of(mapping));
            property.setId(1L); property.setConceptId(30L); property.setCode("id"); property.setDataType("integer"); when(properties.selectById(1L)).thenReturn(property);
        }
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Invalid metadata ownership must be rejected"); }
        catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
