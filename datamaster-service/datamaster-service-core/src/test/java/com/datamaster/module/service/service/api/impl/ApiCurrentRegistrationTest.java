package com.datamaster.module.service.service.api.impl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.mapper.api.ServiceApiMapper;
import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApiCurrentRegistrationTest {
    @Test public void reloadsCurrentRecordRatherThanExecutingStaleMappedConfiguration() throws Exception {
        var fixture = new Fixture(); ServiceApiDO registered = api(); registered.setConfigJson("stale");
        ServiceApiDO current = api(); current.setConfigJson("current"); when(fixture.mapper.selectById(10L)).thenReturn(current);
        assertSame(current, fixture.engine.currentRegisteredApi(registered));
        verify(fixture.mapper).selectById(10L);
    }
    @Test public void disabledDeletedOrChangedRoutesAreRejectedBeforeInterceptorsAndQuery() throws Exception {
        var fixture = new Fixture(); ServiceApiDO current = api(); when(fixture.mapper.selectById(10L)).thenReturn(current);
        current.setStatus("0"); rejected(() -> fixture.engine.currentRegisteredApi(api()));
        current.setStatus("1"); current.setDelFlag(true); rejected(() -> fixture.engine.currentRegisteredApi(api()));
        current.setDelFlag(false); current.setValidFlag(false); rejected(() -> fixture.engine.currentRegisteredApi(api()));
        current.setValidFlag(true); current.setApiUrl("/new"); rejected(() -> fixture.engine.currentRegisteredApi(api()));
        when(fixture.mapper.selectById(10L)).thenReturn(null); rejected(() -> fixture.engine.currentRegisteredApi(api()));
    }
    @Test public void fileServicesCannotBypassStrictSpaceEvenWithStaleRegistration() throws Exception {
        var fixture = new Fixture(); ServiceApiDO current = api(); current.setApiServiceType("4"); when(fixture.mapper.selectById(10L)).thenReturn(current);
        ISpaceFlowApi.Policy strict = new ISpaceFlowApi.Policy(); strict.setAssetRequired(true); when(fixture.flows.policy(1L, "space01")).thenReturn(strict);
        rejected(() -> fixture.engine.currentRegisteredApi(api()));
        when(fixture.flows.policy(1L, "space01")).thenReturn(new ISpaceFlowApi.Policy());
        assertSame(current, fixture.engine.currentRegisteredApi(api()));
    }
    private static ServiceApiDO api() { ServiceApiDO api = new ServiceApiDO(); api.setId(10L); api.setSpaceId(1L); api.setSpaceCode("space01"); api.setApiUrl("/orders"); api.setApiVersion("v1"); api.setReqMethod("1"); api.setStatus("1"); api.setValidFlag(true); return api; }
    private static void rejected(Runnable action) { try { action.run(); fail("Must reject stale route"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); } }
    private static class Fixture {
        final ApiMappingEngine engine = new ApiMappingEngine(); final ServiceApiMapper mapper = mock(ServiceApiMapper.class); final ISpaceFlowApi flows = mock(ISpaceFlowApi.class);
        Fixture() throws Exception { ApiSourceGuard guard = new ApiSourceGuard(); ISpaceApi spaces = mock(ISpaceApi.class); when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space01"); when(flows.policy(1L, "space01")).thenReturn(new ISpaceFlowApi.Policy()); inject(guard, "spaces", spaces); inject(guard, "flows", flows); inject(engine, "apiSourceGuard", guard); inject(engine, "apiMapper", mapper); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
