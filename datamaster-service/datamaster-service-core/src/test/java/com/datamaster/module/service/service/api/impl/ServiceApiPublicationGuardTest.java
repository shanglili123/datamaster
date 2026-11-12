package com.datamaster.module.service.service.api.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.service.dal.dataobject.api.ExecuteConfig;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.mapper.api.ServiceApiMapper;
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

public class ServiceApiPublicationGuardTest {
    private ServiceApiServiceImpl service;
    private ServiceApiMapper mapper;
    private DataSourceFactory factory;

    @Before public void setUp() throws Exception {
        LoginUser user = new LoginUser(7L, 1L, new SysUser(), Set.of("ast:asset:edit"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        ISpaceApi spaces = mock(ISpaceApi.class); ISpaceFlowApi flows = mock(ISpaceFlowApi.class);
        when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space01"); when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(true);
        ISpaceFlowApi.Policy policy = new ISpaceFlowApi.Policy(); policy.setConfigured(true); policy.setQualityRequired(true);
        when(flows.policy(1L, "space01")).thenReturn(policy);
        ApiSourceGuard guard = new ApiSourceGuard(); inject(guard, "spaces", spaces); inject(guard, "flows", flows);
        mapper = mock(ServiceApiMapper.class); factory = mock(DataSourceFactory.class);
        service = new ServiceApiServiceImpl(); inject(service, "ServiceApiMapper", mapper); inject(service, "baseMapper", mapper);
        inject(service, "apiSourceGuard", guard); inject(service, "dataSourceFactory", factory);
    }
    @After public void tearDown() { SecurityContextHolder.clearContext(); }

    @Test public void saveWithEnabledStatusChecksBeforeAnyInsertOrRegistration() {
        reject(() -> service.saveDataApi(script()));
        verifyNoInteractions(mapper, factory);
    }
    @Test public void updateInheritsEnabledStatusAndChecksBeforeWriting() {
        ServiceApiDO stored = script(); stored.setId(10L); when(mapper.selectById(10L)).thenReturn(stored);
        ServiceApiDO request = script(); request.setId(10L); request.setStatus(null); request.setSpaceId(null); request.setSpaceCode(null);
        reject(() -> service.updateDataApi(request));
        assertEquals("1", request.getStatus()); assertEquals(Long.valueOf(1L), request.getSpaceId());
        verify(mapper).selectById(10L); verifyNoMoreInteractions(mapper); verifyNoInteractions(factory);
    }
    @Test public void explicitPublishRejectsStrictScriptBeforeCompatibilityCatch() {
        ServiceApiDO stored = script(); stored.setId(10L); when(mapper.selectById("10")).thenReturn(stored);
        reject(() -> service.releaseDataApi("10", 7L, "tester"));
        verify(mapper).selectById("10"); verifyNoMoreInteractions(mapper); verifyNoInteractions(factory);
    }
    @Test public void savedDebugCannotDiscardOrChangeStoredSpace() {
        ServiceApiDO stored = script(); stored.setId(10L); when(mapper.selectById(10L)).thenReturn(stored);
        ServiceApiDO request = script(); request.setId(10L); request.setSpaceId(null); request.setSpaceCode(null);
        reject(() -> service.serviceTesting(request));
        assertEquals(Long.valueOf(1L), request.getSpaceId()); assertEquals("space01", request.getSpaceCode());
        request.setSpaceId(2L); reject(() -> service.serviceTesting(request));
        verifyNoInteractions(factory);
    }
    @Test public void unsavedDebugWithoutSpaceFailsBeforeConnecting() {
        ServiceApiDO request = script(); request.setSpaceId(null); request.setSpaceCode(null);
        reject(() -> service.serviceTesting(request)); verifyNoInteractions(mapper, factory);
    }
    @Test public void strictSqlInspectionRejectsBeforeCreatingSourceConnection() {
        com.datamaster.module.service.dal.dataobject.api.SqlParseDto request = new com.datamaster.module.service.dal.dataobject.api.SqlParseDto();
        request.setSpaceId(1L); request.setSpaceCode("space01"); request.setSourceId("9"); request.setSqlText("SELECT id FROM orders");
        reject(() -> service.sqlParse(request)); verifyNoInteractions(mapper, factory);
    }
    @Test public void strictFilePublicationRejectsBeforeWriteOrRegistration() {
        ServiceApiDO request = script(); request.setApiServiceType("4"); request.setExecuteConfig(null); request.setConfigJson(null);
        reject(() -> service.saveDataApi(request)); verifyNoInteractions(mapper, factory);
    }
    @Test public void deletionChecksEveryStoredOwnershipBeforeRemovingAnyRecord() {
        ServiceApiDO stored = script(); stored.setId(10L); when(mapper.selectById(10L)).thenReturn(stored);
        reject(() -> service.removeServiceApi(java.util.List.of(10L, 11L)));
        verify(mapper).selectById(10L); verify(mapper).selectById(11L); verifyNoMoreInteractions(mapper);
    }
    @Test public void spreadsheetImportCannotBypassSpaceOwnershipOrDirectEnableChecks() {
        ServiceApiDO existing = script(); existing.setId(10L); when(mapper.selectById(10L)).thenReturn(existing);
        com.datamaster.module.service.controller.admin.api.vo.ServiceApiRespVO imported = new com.datamaster.module.service.controller.admin.api.vo.ServiceApiRespVO();
        imported.setId(10L); imported.setSpaceId(2L); imported.setSpaceCode("space02"); imported.setStatus("1"); imported.setConfigJson(existing.getConfigJson());
        reject(() -> service.importServiceApi(java.util.List.of(imported), true, "tester"));
        verify(mapper).selectById(10L); verifyNoMoreInteractions(mapper); verifyNoInteractions(factory);
    }
    @Test public void visibleApiCallsRequireExplicitMemberSpaceBeforeReadingRecord() {
        reject(() -> service.getVisibleServiceApi(10L, null, null));
        reject(() -> service.listVisibleServiceApis(null, null, null, null, 20));
        verifyNoInteractions(mapper, factory);
    }
    private static ServiceApiDO script() {
        ServiceApiDO api = new ServiceApiDO(); api.setSpaceId(1L); api.setSpaceCode("space01"); api.setStatus("1");
        ExecuteConfig config = new ExecuteConfig(); config.setApiServiceType("2"); config.setSourceId("9"); config.setSqlText("SELECT id FROM orders");
        api.setExecuteConfig(config); api.setConfigJson(JSON.toJSONString(config)); return api;
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Must reject before publishing or querying"); }
        catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception {
        Class<?> type = bean.getClass();
        while (type != null) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(bean, value); return; }
            catch (NoSuchFieldException missing) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
}
