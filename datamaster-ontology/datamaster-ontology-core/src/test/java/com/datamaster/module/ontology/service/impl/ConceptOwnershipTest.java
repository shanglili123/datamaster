package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.service.analysis.MetricAnalysisAccessGuard;
import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ConceptOwnershipTest {
    @Test public void ordinaryEditCannotMoveExistingBindingsToAnotherOntologyPolicy() throws Exception {
        ConceptServiceImpl service = new ConceptServiceImpl(); ConceptMapper mapper = mock(ConceptMapper.class); MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class);
        inject(service, "conceptMapper", mapper); inject(service, "ownership", access);
        com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper locks = mock(com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper.class);
        inject(service, "metadataLocks", locks); when(locks.lockOntology(20L)).thenReturn(20L);
        ConceptDO existing = new ConceptDO(); existing.setId(30L); existing.setOntologyId(20L); when(mapper.selectById(30L)).thenReturn(existing);
        ConceptSaveReqVO request = new ConceptSaveReqVO(); request.setId(30L); request.setOntologyId(99L); request.setCode("orders"); request.setName("订单");
        try { service.updateConcept(request); fail("Must reject parent reassignment"); } catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
        verify(mapper, never()).updateById(any(ConceptDO.class)); verify(access, times(2)).ontology(20L);
        request.setOntologyId(null); service.updateConcept(request);
        verify(mapper).updateById(argThat((ConceptDO concept) -> Long.valueOf(20L).equals(concept.getOntologyId())));
    }
    @Test public void editRechecksExistenceAfterWaitingForParentLock() throws Exception {
        ConceptServiceImpl service = new ConceptServiceImpl(); ConceptMapper mapper = mock(ConceptMapper.class);
        MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class);
        com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper locks = mock(com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper.class);
        inject(service, "conceptMapper", mapper); inject(service, "ownership", access); inject(service, "metadataLocks", locks);
        ConceptDO existing = new ConceptDO(); existing.setId(30L); existing.setOntologyId(20L);
        when(mapper.selectById(30L)).thenReturn(existing, null); when(locks.lockOntology(20L)).thenReturn(20L);
        ConceptSaveReqVO request = new ConceptSaveReqVO(); request.setId(30L);
        try { service.updateConcept(request); fail("Deleted parent must not be edited"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
        org.mockito.InOrder order = inOrder(mapper, locks);
        order.verify(mapper).selectById(30L); order.verify(locks).lockOntology(20L); order.verify(mapper).selectById(30L);
        verify(mapper, never()).updateById(any(ConceptDO.class));
    }
    @Test public void createRechecksOntologyAfterLockAndDoesNotInsertOnRevokedAccess() throws Exception {
        ConceptServiceImpl service = new ConceptServiceImpl(); ConceptMapper mapper = mock(ConceptMapper.class);
        MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class);
        com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper locks = mock(com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper.class);
        inject(service, "conceptMapper", mapper); inject(service, "ownership", access); inject(service, "metadataLocks", locks);
        when(locks.lockOntology(20L)).thenReturn(20L);
        when(access.ontology(20L)).thenReturn(new com.datamaster.module.ontology.dal.dataobject.OntologyDO())
                .thenThrow(new com.datamaster.common.exception.ServiceException("空间成员权限已失效"));
        ConceptSaveReqVO request = new ConceptSaveReqVO(); request.setOntologyId(20L);
        try { service.createConcept(request); fail("Must check access again after lock"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertEquals("空间成员权限已失效", expected.getMessage()); }
        org.mockito.InOrder order = inOrder(access, locks);
        order.verify(access).ontology(20L); order.verify(locks).lockOntology(20L); order.verify(access).ontology(20L);
        verify(mapper, never()).insert(any(ConceptDO.class));
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
