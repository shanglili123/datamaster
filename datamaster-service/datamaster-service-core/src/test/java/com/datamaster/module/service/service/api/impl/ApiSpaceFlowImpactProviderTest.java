package com.datamaster.module.service.service.api.impl;

import com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ApiSpaceFlowImpactProviderTest {
    @Test public void fileTypeOverridesAnApparentlyValidSingleTableConfig() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ApiSpaceFlowImpactProvider provider = new ApiSpaceFlowImpactProvider();
        Field field = provider.getClass().getDeclaredField("jdbc"); field.setAccessible(true); field.set(provider, jdbc);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L), eq("space01"))).thenReturn(1L);
        when(jdbc.queryForList(anyString(), eq(1L), eq("space01"), eq(10), eq(0))).thenReturn(List.of(Map.of("id", 10L, "name", "文件服务", "status", "1", "api_service_type", "4", "config_json", "{\"apiServiceType\":\"1\",\"sourceId\":\"9\",\"tableName\":\"orders\"}")));
        var result = provider.inventory(1L, "space01", 1, 10).getRows().get(0);
        assertEquals("NON_QUERY_UNRESOLVED", result.getSourceCoverage()); assertNull(result.getDatasourceId()); assertNull(result.getTableName());
    }
    @Test public void returnsRealScopedInventoryWithoutSqlAndKeepsMalformedSourcesUnknown() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ApiSpaceFlowImpactProvider provider = new ApiSpaceFlowImpactProvider();
        Field field = provider.getClass().getDeclaredField("jdbc"); field.setAccessible(true); field.set(provider, jdbc);
        when(jdbc.queryForObject(contains("space_id=? OR (space_id IS NULL AND space_code=?)"), eq(Long.class), eq(1L), eq("space01"))).thenReturn(2L);
        when(jdbc.queryForList(contains("ORDER BY id DESC LIMIT ? OFFSET ?"), eq(1L), eq("space01"), eq(10), eq(0)))
                .thenReturn(List.of(Map.of("id", 10L, "name", "订单服务", "status", "1", "config_json", "{\"apiServiceType\":\"2\",\"sqlText\":\"SECRET_SQL\"}"),
                        Map.of("id", 9L, "name", "旧服务", "status", "0", "config_json", "broken")));
        SpaceFlowImpactProvider.Page page = provider.inventory(1L, "space01", 1, 10);
        assertEquals(2L, page.getTotal()); assertEquals("CUSTOM_SQL_UNRESOLVED", page.getRows().get(0).getSourceCoverage());
        assertEquals("UNKNOWN", page.getRows().get(1).getSourceCoverage());
        assertFalse(com.alibaba.fastjson2.JSON.toJSONString(page).contains("SECRET_SQL"));
        verify(jdbc).queryForObject(contains("del_flag='0'"), eq(Long.class), eq(1L), eq("space01"));
        verify(jdbc).queryForList(contains("del_flag='0'"), eq(1L), eq("space01"), eq(10), eq(0));
    }
}
