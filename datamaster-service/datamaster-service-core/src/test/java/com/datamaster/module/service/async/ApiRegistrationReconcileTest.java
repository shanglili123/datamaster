package com.datamaster.module.service.async;

import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.handler.MappingHandlerMapping;
import com.datamaster.module.service.service.api.IServiceApiService;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Map;
import static org.mockito.Mockito.*;

public class ApiRegistrationReconcileTest {
    @Test public void staleReleaseMessageCannotReenableDisabledApi() throws Exception {
        Fixture f = new Fixture(); ServiceApiDO current = api("0", "/orders"); ServiceApiDO original = api("1", "/orders");
        when(f.service.getServiceApiById(10L)).thenReturn(current);
        f.task.releaseOrCancelDataApi(Map.of("id", "10", "type", "1", "api", original));
        verify(f.mapping).unregisterMapping(original); verifyNoMoreInteractions(f.mapping);
    }
    @Test public void staleCancelMessageReconcilesCurrentPublishedState() throws Exception {
        Fixture f = new Fixture(); ServiceApiDO current = api("1", "/orders");
        when(f.service.getServiceApiById(10L)).thenReturn(current);
        f.task.releaseOrCancelDataApi(Map.of("id", "10", "type", "2", "api", api("0", "/orders")));
        verify(f.mapping).registerMapping(current); verifyNoMoreInteractions(f.mapping);
    }
    @Test public void deletesOldRouteWhenCurrentRouteChangedOrRecordDeleted() throws Exception {
        Fixture f = new Fixture(); ServiceApiDO old = api("1", "/old"), current = api("1", "/new");
        when(f.service.getServiceApiById(10L)).thenReturn(current);
        f.task.releaseOrCancelDataApi(Map.of("id", "10", "type", "1", "api", old));
        verify(f.mapping).unregisterMapping(old); verify(f.mapping).registerMapping(current);
        reset(f.mapping); when(f.service.getServiceApiById(10L)).thenReturn(null);
        f.task.releaseOrCancelDataApi(Map.of("id", "10", "type", "2", "api", old));
        verify(f.mapping).unregisterMapping(old); verifyNoMoreInteractions(f.mapping);
    }
    private static ServiceApiDO api(String status, String url) { ServiceApiDO api = new ServiceApiDO(); api.setId(10L); api.setStatus(status); api.setApiUrl(url); api.setApiVersion("v1"); api.setReqMethod("1"); api.setValidFlag(true); return api; }
    private static class Fixture {
        final AsyncTask task = new AsyncTask(); final MappingHandlerMapping mapping = mock(MappingHandlerMapping.class); final IServiceApiService service = mock(IServiceApiService.class);
        Fixture() throws Exception { inject(task, "mappingHandlerMapping", mapping); inject(task, "IServiceApiService", service); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
