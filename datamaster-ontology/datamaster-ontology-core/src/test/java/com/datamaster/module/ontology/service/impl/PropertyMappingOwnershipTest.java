package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PropertyMappingOwnershipTest {
    @Test public void invalidReplacementCannotDeleteExistingMappings() throws Exception {
        PropertyColumnServiceImpl service = new PropertyColumnServiceImpl();
        PropertyColumnMapper mappings = mock(PropertyColumnMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        PropertyMapper properties = mock(PropertyMapper.class); MetricMetadataLockMapper locks = mock(MetricMetadataLockMapper.class);
        ConceptTableConsumptionGuard access = mock(ConceptTableConsumptionGuard.class);
        inject(service, "propertyColumnMapper", mappings); inject(service, "tableMapper", tables); inject(service, "propertyMapper", properties); inject(service, "metadataLocks", locks); inject(service, "consumption", access);
        ConceptTableDO table = new ConceptTableDO(); table.setId(40L); table.setConceptId(30L); when(tables.selectById(40L)).thenReturn(table);
        OntologyDO ontology = new OntologyDO(); ontology.setId(20L); when(access.ownership(table)).thenReturn(ontology); when(locks.lockOntology(20L)).thenReturn(20L);
        PropertyDO property = new PropertyDO(); property.setId(1L); property.setConceptId(99L); when(properties.selectById(1L)).thenReturn(property);
        rejected(() -> service.batchSavePropertyColumns(40L, null)); verifyNoInteractions(mappings);
        PropertyColumnSaveReqVO mapping = new PropertyColumnSaveReqVO(); mapping.setPropertyId(1L); mapping.setColumnName("id");
        rejected(() -> service.batchSavePropertyColumns(40L, List.of(mapping))); verifyNoInteractions(mappings);
        property.setConceptId(30L); mapping.setColumnName("id;DROP TABLE other");
        rejected(() -> service.batchSavePropertyColumns(40L, List.of(mapping))); verifyNoInteractions(mappings);
        mapping.setColumnName("id"); rejected(() -> service.batchSavePropertyColumns(40L, List.of(mapping, mapping))); verifyNoInteractions(mappings);
    }
    private static void rejected(Runnable call) { try { call.run(); fail("Must reject invalid mapping before delete"); } catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); } }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
