package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MetricSpaceFlowImpactProviderTest {
    @Test public void inventoriesImmutablePublicationAndKeepsMissingSnapshotVisible() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); MetricSpaceFlowImpactProvider provider = new MetricSpaceFlowImpactProvider();
        Field field = provider.getClass().getDeclaredField("jdbc"); field.setAccessible(true); field.set(provider, jdbc);
        when(jdbc.queryForObject(contains("m.status='PUBLISHED'"), eq(Long.class), eq(1L), eq("space01"))).thenReturn(2L);
        Map<String,Object> missing = new HashMap<>(); missing.put("id", 9L); missing.put("code", "missing"); missing.put("status", "PUBLISHED"); missing.put("snapshot_json", null);
        when(jdbc.queryForList(contains("LEFT JOIN ont_metric_version"), eq(1L), eq("space01"), eq(10), eq(0)))
                .thenReturn(List.of(Map.of("id", 10L, "code", "sales", "status", "PUBLISHED", "ontology_id", 20L, "concept_id", 30L, "published_version", 1, "snapshot_json", "{\"id\":10,\"ontologyId\":20,\"conceptId\":30,\"publishedVersion\":1,\"code\":\"sales\",\"status\":\"PUBLISHED\",\"name\":\"正式销售额\",\"sourceSnapshot\":{\"datasourceId\":50,\"tableName\":\"orders\",\"validationScope\":\"METADATA_ONLY\"}}"), missing));
        SpaceFlowImpactProvider.Page page = provider.inventory(1L, "space01", 1, 10);
        assertEquals(2L, page.getTotal()); assertEquals("正式销售额", page.getRows().get(0).getName());
        assertEquals("PUBLISHED_SOURCE_SNAPSHOT", page.getRows().get(0).getSourceCoverage());
        assertEquals("UNKNOWN", page.getRows().get(1).getSourceCoverage());
        assertEquals("missing", page.getRows().get(1).getName());
    }
}
