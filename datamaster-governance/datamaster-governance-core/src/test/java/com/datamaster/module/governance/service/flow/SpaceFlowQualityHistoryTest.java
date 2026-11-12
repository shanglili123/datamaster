package com.datamaster.module.governance.service.flow;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi.Policy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SpaceFlowQualityHistoryTest {
    private SpaceFlowService service;
    private JdbcTemplate jdbc;
    private ISpaceApi spaces;

    @Before public void setUp() throws Exception {
        LoginUser user = new LoginUser(7L, 1L, new SysUser(), Set.of("tax:space:query"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        jdbc = mock(JdbcTemplate.class); spaces = mock(ISpaceApi.class);
        service = new SpaceFlowService(); inject(service, "jdbc", jdbc); inject(service, "spaces", spaces);
        when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space01");
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(true);
        when(jdbc.queryForObject(contains("manager_id"), eq(Long.class), eq(1L))).thenReturn(7L);
    }
    @After public void tearDown() { SecurityContextHolder.clearContext(); }

    @Test public void distinguishesCurrentRevokedAndExpiredEvidence() {
        Policy policy = new Policy(); policy.setQualityRequired(true); policy.setVersion(2);
        assertEquals("PENDING", SpaceFlowService.qualityStatus(policy, null));
        assertEquals("AVAILABLE", SpaceFlowService.qualityStatus(policy, Map.of("available", true, "flow_version", 2)));
        assertEquals("VERSION_EXPIRED", SpaceFlowService.qualityStatus(policy, Map.of("available", true, "flow_version", 1)));
        assertEquals("REVOKED", SpaceFlowService.qualityStatus(policy, Map.of("available", false, "flow_version", 2)));
        policy.setQualityRequired(false);
        assertEquals("NOT_REQUIRED", SpaceFlowService.qualityStatus(policy, null));
    }

    @Test public void scopesHistoryAndUsesStableServerPagination() {
        String definition = "{\"nodes\":[\"METADATA\",\"QUALITY\",\"API\",\"ANALYSIS\"],\"edges\":[{\"from\":\"METADATA\",\"to\":\"QUALITY\"},{\"from\":\"QUALITY\",\"to\":\"API\"},{\"from\":\"QUALITY\",\"to\":\"ANALYSIS\"}]}";
        when(jdbc.queryForList(contains("FROM gov_space_flow WHERE"), eq(1L)))
                .thenReturn(List.of(Map.of("published_version", 2, "published_json", definition)));
        when(jdbc.queryForList(contains("FROM gov_space_quality_admission WHERE"), eq(1L), eq(9L), eq("orders")))
                .thenReturn(List.of(Map.of("available", true, "flow_version", 1)));
        when(jdbc.queryForObject(contains("FROM gov_space_quality_admission_event"), eq(Long.class), eq(1L), eq(9L), eq("orders"))).thenReturn(12L);
        List<Map<String,Object>> rows = List.of(Map.of("id", 3L, "basis", "MANUAL_AVAILABLE"));
        when(jdbc.queryForList(contains("ORDER BY confirmed_at DESC,id DESC LIMIT ? OFFSET ?"), eq(1L), eq(9L), eq("orders"), eq(10), eq(10))).thenReturn(rows);
        Map<String,Object> result = service.qualityHistory(1L, "space01", 9L, "orders", 2, 10);
        assertEquals("VERSION_EXPIRED", result.get("status")); assertEquals(12L, result.get("total"));
        assertSame(rows, result.get("rows"));
        verify(jdbc).queryForList(contains("ORDER BY confirmed_at DESC,id DESC LIMIT ? OFFSET ?"), eq(1L), eq(9L), eq("orders"), eq(10), eq(10));
    }

    @Test public void rejectsOtherSpaceBeforeReadingHistory() {
        when(spaces.hasUserSpace(7L, 1L, "space01")).thenReturn(false);
        reject(() -> service.qualityHistory(1L, "space01", 9L, "orders", 1, 10));
        verifyNoInteractions(jdbc);
    }
    @Test public void rejectsUnboundedOrInvalidPagination() {
        reject(() -> service.qualityHistory(1L, "space01", 9L, "orders", 0, 10));
        reject(() -> service.qualityHistory(1L, "space01", 9L, "orders", 1, 101));
        reject(() -> service.qualityHistory(1L, "space01", -1L, "orders", 1, 10));
        verify(jdbc, times(3)).queryForObject(contains("manager_id"), eq(Long.class), eq(1L));
        verifyNoMoreInteractions(jdbc);
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Must reject invalid history request"); }
        catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
