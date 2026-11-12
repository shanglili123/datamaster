package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.controller.admin.property.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PropertyOwnershipTest {
    @Test public void propertyCannotBeMovedToAnotherConceptToChangeSourcePolicy() throws Exception {
        PropertyServiceImpl service = new PropertyServiceImpl(); PropertyMapper mapper = mock(PropertyMapper.class); ConceptMapper concepts = mock(ConceptMapper.class);
        MetricMetadataLockMapper locks = mock(MetricMetadataLockMapper.class); OntologyConsumptionContext contexts = mock(OntologyConsumptionContext.class);
        inject(service, "propertyMapper", mapper); inject(service, "concepts", concepts); inject(service, "metadataLocks", locks); inject(service, "contexts", contexts);
        PropertyDO existing = new PropertyDO(); existing.setId(1L); existing.setConceptId(30L); when(mapper.selectById(1L)).thenReturn(existing);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
        OntologyDO owner = new OntologyDO(); owner.setId(20L); when(contexts.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(owner); when(locks.lockOntology(20L)).thenReturn(20L);
        PropertySaveReqVO request = new PropertySaveReqVO(); request.setId(1L); request.setConceptId(99L);
        reject(() -> service.updateProperty(request)); verify(mapper, never()).updateById(any(PropertyDO.class));
        request.setConceptId(null); service.updateProperty(request); verify(mapper).updateById(argThat((PropertyDO row) -> Long.valueOf(30L).equals(row.getConceptId())));
    }
    @Test public void emptyPrimarySelectionCannotClearAllPrimaryFlags() {
        PropertyServiceImpl service = new PropertyServiceImpl(); PropertyPrimaryReqVO request = new PropertyPrimaryReqVO(); request.setConceptId(30L); request.setPropertyIds(List.of());
        reject(() -> service.setPrimaryProperties(request));
    }
    @Test public void createCannotInsertIntoConceptDeletedWhileWaitingForLock() throws Exception {
        PropertyServiceImpl service = new PropertyServiceImpl(); PropertyMapper mapper = mock(PropertyMapper.class);
        ConceptMapper concepts = mock(ConceptMapper.class); MetricMetadataLockMapper locks = mock(MetricMetadataLockMapper.class);
        OntologyConsumptionContext contexts = mock(OntologyConsumptionContext.class);
        inject(service, "propertyMapper", mapper); inject(service, "concepts", concepts); inject(service, "metadataLocks", locks); inject(service, "contexts", contexts);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L);
        when(concepts.selectById(30L)).thenReturn(concept, null);
        OntologyDO owner = new OntologyDO(); owner.setId(20L);
        when(contexts.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(owner); when(locks.lockOntology(20L)).thenReturn(20L);
        PropertySaveReqVO request = new PropertySaveReqVO(); request.setConceptId(30L);
        reject(() -> service.createProperty(request));
        org.mockito.InOrder order = inOrder(concepts, locks);
        order.verify(concepts).selectById(30L); order.verify(locks).lockOntology(20L); order.verify(concepts).selectById(30L);
        verify(mapper, never()).insert(any(PropertyDO.class));
    }
    private static void reject(Runnable call) { try { call.run(); fail("Must reject invalid ownership change"); } catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); } }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
