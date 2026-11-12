package com.datamaster.module.service.service.api.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.service.dal.dataobject.api.ExecuteConfig;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.dto.ReqParam;
import com.datamaster.module.service.dal.dataobject.dto.ResParam;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ApiSourceGuardTest {
    private ApiSourceGuard guard;
    private ISpaceApi spaces;
    private ISpaceFlowApi flows;
    private IDatasourceApiService datasources;
    private IAssetsTableGovernanceApiService governance;
    private DatasourceRespDTO source;
    private ISpaceFlowApi.Policy policy;

    @Before public void setUp() throws Exception {
        LoginUser user = new LoginUser(7L, 1L, new SysUser(), Set.of("ast:asset:edit"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        guard = new ApiSourceGuard(); spaces = mock(ISpaceApi.class); flows = mock(ISpaceFlowApi.class);
        datasources = mock(IDatasourceApiService.class); governance = mock(IAssetsTableGovernanceApiService.class);
        inject(guard, "spaces", spaces); inject(guard, "flows", flows); inject(guard, "datasources", datasources); inject(guard, "governance", governance);
        when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space01"); when(spaces.getSpaceIdBySpaceCode("space01")).thenReturn(1L);
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(true);
        policy = new ISpaceFlowApi.Policy(); policy.setConfigured(true); policy.setQualityRequired(true); policy.setVersion(2);
        when(flows.policy(1L, "space01")).thenReturn(policy);
        source = new DatasourceRespDTO(); source.setId(9L); source.setDatasourceType("mysql"); source.setDatasourceConfig("{\"dbname\":\"business\"}");
        when(datasources.getDatasourceById(9L)).thenReturn(source);
        when(datasources.hasDatasourceSpace(9L, 1L, "space01")).thenReturn(true);
        AssetsTableGovernanceRespDTO allowed = new AssetsTableGovernanceRespDTO(); allowed.setAccessAllowed(true);
        when(governance.resolveTable(any(AssetsTableGovernanceReqDTO.class))).thenReturn(allowed);
    }
    @After public void tearDown() { SecurityContextHolder.clearContext(); }

    @Test public void datasourceSpaceRevocationRejectsBeforeGovernance() {
        when(datasources.hasDatasourceSpace(9L, 1L, "space01")).thenReturn(false);
        ServiceApiDO request = api(); reject(() -> guard.check(request, request.getExecuteConfig(), "DATA_SERVICE"));
        verifyNoInteractions(governance);
    }
    @Test public void strictFormIgnoresSqlAndClientDatabaseWhileCheckingAllColumns() {
        ServiceApiDO api = api(); api.getExecuteConfig().setSqlText("SELECT * FROM forbidden");
        api.getExecuteConfig().setDbName("forbidden"); api.getExecuteConfig().setSid("other"); api.getExecuteConfig().setDbType("postgresql");
        ReqParam filter = new ReqParam(); filter.setParamName("amount"); filter.setWhereType("6"); api.getReqParamsList().add(filter);
        guard.check(api, api.getExecuteConfig(), "DATA_SERVICE_TEST");
        assertNull(api.getExecuteConfig().getSqlText()); assertEquals("business", api.getExecuteConfig().getDbName());
        assertNull(api.getExecuteConfig().getSid()); assertEquals("mysql", api.getExecuteConfig().getDbType());
        verify(governance).resolveTable(argThat(request -> request.getSpaceId().equals(1L) && request.getColumnNames().containsAll(List.of("id", "amount"))));
    }
    @Test public void rejectsStrictScriptsAndUnknownTypesBeforeGovernance() {
        ServiceApiDO api = api(); api.getExecuteConfig().setApiServiceType("2");
        reject(() -> guard.check(api, api.getExecuteConfig(), "DATA_SERVICE"));
        api.getExecuteConfig().setApiServiceType("unknown");
        reject(() -> guard.check(api, api.getExecuteConfig(), "DATA_SERVICE"));
        verifyNoInteractions(governance);
    }
    @Test public void rejectsExpressionsAndQualifiedNamesInFormSourcesAndColumns() {
        ServiceApiDO api = api(); api.getExecuteConfig().setTableName("orders JOIN secret ON 1=1");
        reject(() -> guard.check(api, api.getExecuteConfig(), "DATA_SERVICE"));
        api.getExecuteConfig().setTableName("other.orders");
        reject(() -> guard.check(api, api.getExecuteConfig(), "DATA_SERVICE"));
        api.getExecuteConfig().setTableName("orders"); api.getResParamsList().get(0).setFieldName("(SELECT token FROM secret)");
        reject(() -> guard.check(api, api.getExecuteConfig(), "DATA_SERVICE"));
        verifyNoInteractions(governance);
    }
    @Test public void rejectsUnownedSpaceAndLocksSavedOwnershipEvenIfRequestOmitsIt() {
        ServiceApiDO stored = api(), request = api(); request.setSpaceId(null); request.setSpaceCode(null);
        guard.bindStoredSpace(request, stored);
        assertEquals(Long.valueOf(1), request.getSpaceId()); assertEquals("space01", request.getSpaceCode());
        request.setSpaceId(2L); reject(() -> guard.bindStoredSpace(request, stored));
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(false);
        reject(() -> guard.normalizeSpace(stored, true));
        // Actual API consumers retain existing API authentication rather than management membership.
        guard.normalizeSpace(stored, false);
        request.setSpaceId(null); request.setSpaceCode(null); reject(() -> guard.normalizeSpace(request, false));
    }
    @Test public void rejectsUnauthorizedFilterRatherThanSilentlyRemovingIt() {
        ServiceApiDO api = api(); ReqParam filter = new ReqParam(); filter.setParamName("secret"); filter.setWhereType("1"); api.getReqParamsList().add(filter);
        AssetsTableGovernanceRespDTO limited = new AssetsTableGovernanceRespDTO(); limited.setAccessAllowed(true); limited.setAllowedColumns(List.of("id"));
        when(governance.resolveTable(any(AssetsTableGovernanceReqDTO.class))).thenReturn(limited);
        reject(() -> guard.check(api, api.getExecuteConfig(), "DATA_SERVICE"));
    }
    @Test public void actualEngineRebuildsPersistedFormSqlAfterAdmission() throws Exception {
        ServiceApiDO api = api(); api.setResDataType("2"); api.getExecuteConfig().setSqlText("SELECT token FROM forbidden");
        api.setConfigJson(JSON.toJSONString(api.getExecuteConfig())); api.setReqParams("[]"); api.setResParams(JSON.toJSONString(api.getResParamsList()));
        ApiMappingEngine engine = new ApiMappingEngine(); DbQuery query = mock(DbQuery.class); DataSourceFactory factory = mock(DataSourceFactory.class);
        inject(engine, "apiSourceGuard", guard); inject(engine, "iAssetsDatasourceApiService", datasources); inject(engine, "dataSourceFactory", factory);
        inject(engine, "assetsTableGovernanceApiService", governance);
        when(factory.createDbQuery(any())).thenReturn(query); when(query.queryList(anyString(), anyMap(), anyInt())).thenReturn(new ArrayList<>());
        engine.execute(api, new HashMap<>());
        verify(query).queryList(argThat(sql -> sql.contains("business.orders") && sql.contains("id") && !sql.contains("forbidden")), anyMap(), anyInt());
        verify(query).close();
    }
    private static ServiceApiDO api() {
        ServiceApiDO api = new ServiceApiDO(); api.setSpaceId(1L); api.setSpaceCode("space01");
        ExecuteConfig config = new ExecuteConfig(); config.setSourceId("9"); config.setTableName("orders"); config.setApiServiceType("1"); api.setExecuteConfig(config);
        ResParam output = new ResParam(); output.setFieldName("id"); api.setResParamsList(new ArrayList<>(List.of(output))); api.setReqParamsList(new ArrayList<>()); return api;
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Must reject invalid API source"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
