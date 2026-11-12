package com.datamaster.module.service.handler;

import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MappingOwnershipTest {
    @Test public void staleUnregisterCannotRemoveAnotherApiAndDefaultVersionIsNotMutated() {
        MappingHandlerMapping mapping = new MappingHandlerMapping(); RequestMappingHandlerMapping spring = mock(RequestMappingHandlerMapping.class);
        when(spring.getBuilderConfiguration()).thenReturn(new org.springframework.web.servlet.mvc.method.RequestMappingInfo.BuilderConfiguration());
        mapping.setRequestMappingHandlerMapping(spring); mapping.setHandler(new RequestHandler());
        ServiceApiDO first = api(910001L, "v_explicit"); ServiceApiDO other = api(910002L, "v_explicit");
        try {
            mapping.registerMapping(first);
            try { mapping.registerMapping(other); fail("Route ownership must not be overwritten"); } catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
            mapping.unregisterMapping(other);
            assertSame(first, MappingHandlerMapping.getMappingApiInfo(MappingHandlerMapping.buildMappingKey("1", "/services/v_explicit/unit-test-route")));
            ServiceApiDO defaultVersion = api(910003L, null); mapping.registerMapping(defaultVersion);
            assertSame(defaultVersion, MappingHandlerMapping.getMappingApiInfo(MappingHandlerMapping.buildMappingKey("1", "/services/v1.0.0/unit-test-route")));
            mapping.unregisterMapping(defaultVersion);
        } finally { mapping.unregisterMapping(first); }
    }
    private static ServiceApiDO api(Long id, String version) { ServiceApiDO api = new ServiceApiDO(); api.setId(id); api.setApiUrl("/unit-test-route"); api.setReqMethod("1"); api.setApiVersion(version); return api; }
}
