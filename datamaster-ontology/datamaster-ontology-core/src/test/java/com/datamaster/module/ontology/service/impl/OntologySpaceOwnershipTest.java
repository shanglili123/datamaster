package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OntologySpaceOwnershipTest {
    private OntologyServiceImpl service;
    private OntologyMapper mapper;
    private ISpaceApi spaces;

    @Before public void setUp() throws Exception {
        LoginUser user = new LoginUser(7L, 1L, new SysUser(), Set.of("ont:ontology:edit"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        mapper = mock(OntologyMapper.class); spaces = mock(ISpaceApi.class);
        service = new OntologyServiceImpl(); inject(service, "ontologyMapper", mapper); inject(service, "spaceApi", spaces);
        when(spaces.getSpaceIdBySpaceCode("space01")).thenReturn(1L);
        when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space01");
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(true);
        OntologyDO existing = new OntologyDO(); existing.setId(10L); existing.setSpaceId(1L); existing.setSpaceCode("space01");
        when(mapper.selectById(10L)).thenReturn(existing);
    }
    @After public void tearDown() { SecurityContextHolder.clearContext(); }

    @Test public void ordinaryEditPreservesStoredOwnershipWhenOmitted() {
        OntologySaveReqVO request = request();
        service.updateOntology(request);
        verify(mapper).updateById(argThat((OntologyDO ontology) -> Long.valueOf(1L).equals(ontology.getSpaceId()) && "space01".equals(ontology.getSpaceCode())));
    }
    @Test public void rejectsChangingEitherSpaceIdentifier() {
        OntologySaveReqVO request = request(); request.setSpaceId(2L);
        reject(() -> service.updateOntology(request));
        request.setSpaceId(null); request.setSpaceCode("other");
        reject(() -> service.updateOntology(request));
        verify(mapper, never()).updateById(any(OntologyDO.class));
    }
    @Test public void rejectsEditWithoutAccessToOriginalSpace() {
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(false);
        reject(() -> service.updateOntology(request()));
        verify(mapper, never()).updateById(any(OntologyDO.class));
    }
    @Test public void normalizesCodeOnlyCreationAndRequiresRealMembership() {
        OntologySaveReqVO request = request(); request.setId(null); request.setSpaceCode("space01");
        service.createOntology(request);
        verify(mapper).insert(argThat((OntologyDO ontology) -> Long.valueOf(1L).equals(ontology.getSpaceId()) && "space01".equals(ontology.getSpaceCode())));
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(false);
        reject(() -> service.createOntology(request));
        verify(mapper, times(1)).insert(any(OntologyDO.class));
    }
    @Test public void rejectsMissingOrInconsistentCreationSpace() {
        OntologySaveReqVO request = request(); request.setId(null);
        reject(() -> service.createOntology(request));
        request.setSpaceId(1L); request.setSpaceCode("other");
        reject(() -> service.createOntology(request));
        verify(mapper, never()).insert(any(OntologyDO.class));
    }
    @Test public void rejectsUnknownOntologyBeforeUpdating() {
        OntologySaveReqVO request = request(); request.setId(99L);
        reject(() -> service.updateOntology(request));
        verify(mapper, never()).updateById(any(OntologyDO.class));
    }
    @Test public void emptyOntologyCannotBypassMembershipOnDeletion() throws Exception {
        MetricReferenceGuard references = mock(MetricReferenceGuard.class); inject(service, "metricReferenceGuard", references);
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(false);
        reject(() -> service.deleteOntology(10L));
        verifyNoInteractions(references); verify(mapper, never()).deleteById(anyLong());
    }
    @Test public void deletionRechecksParentAfterReferenceGuardAcquiresLock() throws Exception {
        MetricReferenceGuard references = mock(MetricReferenceGuard.class); inject(service, "metricReferenceGuard", references);
        OntologyDO existing = new OntologyDO(); existing.setId(10L); existing.setSpaceId(1L); existing.setSpaceCode("space01");
        when(mapper.selectById(10L)).thenReturn(existing, null);
        reject(() -> service.deleteOntology(10L));
        org.mockito.InOrder order = inOrder(mapper, references);
        order.verify(mapper).selectById(10L); order.verify(references).assertOntologyDeletable(10L); order.verify(mapper).selectById(10L);
        verify(mapper, never()).deleteById(anyLong());
    }
    @Test public void nullEditIsReportedAsBusinessError() {
        reject(() -> service.updateOntology(null)); verifyNoInteractions(mapper);
    }
    private static OntologySaveReqVO request() {
        OntologySaveReqVO request = new OntologySaveReqVO(); request.setId(10L); request.setName("订单本体"); request.setCode("orders"); return request;
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Must reject invalid ownership change"); }
        catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
