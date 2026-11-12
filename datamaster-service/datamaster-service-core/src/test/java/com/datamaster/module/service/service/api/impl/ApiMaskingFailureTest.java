package com.datamaster.module.service.service.api.impl;

import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ApiMaskingFailureTest {
    @Before public void login() { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginUser(7L, 1L, new SysUser(), Set.of()), null)); }
    @After public void logout() { SecurityContextHolder.clearContext(); }
    @Test public void realConsumptionNeverReturnsOriginalRowsWhenMaskingFails() throws Exception {
        ApiMappingEngine engine = new ApiMappingEngine(); IAssetsTableGovernanceApiService masking = mock(IAssetsTableGovernanceApiService.class); inject(engine, "assetsTableGovernanceApiService", masking);
        when(masking.desensitizeResultData(eq(5L), anyList(), eq(7L), isNull(), eq("3"))).thenThrow(new IllegalStateException("driver-secret"));
        AssetsTableGovernanceRespDTO source = new AssetsTableGovernanceRespDTO(); source.setAssetId(5L);
        rejectPrivate(engine, "desensitizeData", new Class<?>[]{List.class, AssetsTableGovernanceRespDTO.class}, List.of(Map.of("secret", "raw")), source);
        rejectPrivate(engine, "desensitizeSingleData", new Class<?>[]{Map.class, AssetsTableGovernanceRespDTO.class}, Map.of("secret", "raw"), source);
    }
    @Test public void debugNeverReturnsOriginalRowsForNullMaskingResult() throws Exception {
        ServiceApiServiceImpl service = new ServiceApiServiceImpl(); IAssetsTableGovernanceApiService masking = mock(IAssetsTableGovernanceApiService.class); inject(service, "assetsTableGovernanceApiService", masking);
        when(masking.desensitizeResultData(eq(5L), anyList(), eq(7L), isNull(), eq("3"))).thenReturn(null);
        AssetsTableGovernanceRespDTO source = new AssetsTableGovernanceRespDTO(); source.setAssetId(5L);
        rejectPrivate(service, "desensitizeServiceResultMap", new Class<?>[]{Map.class, AssetsTableGovernanceRespDTO.class}, Map.of("secret", "raw"), source);
    }
    private static void rejectPrivate(Object bean, String method, Class<?>[] types, Object... args) throws Exception {
        Method target = bean.getClass().getDeclaredMethod(method, types); target.setAccessible(true);
        try { target.invoke(bean, args); fail("Must reject masking failure instead of returning raw rows"); }
        catch (InvocationTargetException error) { assertTrue(error.getCause() instanceof ServiceException); assertFalse(error.getCause().getMessage().contains("driver-secret")); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
