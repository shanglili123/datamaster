package com.datamaster.module.governance.service.flow;

import com.datamaster.module.governance.api.flow.ISpaceFlowApi.*;
import com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Consumer;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SpaceFlowReadinessServiceTest {
    @Test public void identifiesMissingAssetAndNextVersionEvidenceWithoutWritingOrGranting() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); SpaceFlowReadinessService service = service(jdbc);
        Consumer consumer = source();
        when(jdbc.queryForList(contains("FROM ast_asset"), eq(9L), eq("orders"))).thenReturn(List.of());
        when(jdbc.queryForObject(contains("FROM gov_space_quality_admission"), eq(Long.class), eq(1L), eq(9L), eq("orders"), eq(3))).thenReturn(0L);
        service.assess(consumer, definition("ASSET", "QUALITY"), 1L, "space01", 3);
        assertEquals("MISSING_REQUIREMENTS", consumer.getReadiness());
        assertEquals(List.of("ASSET_NOT_REGISTERED", "QUALITY_EVIDENCE_FOR_NEW_VERSION"), consumer.getMissingRequirements());
        assertEquals(2, consumer.getNextSteps().size()); assertFalse(consumer.getPendingChecks().isEmpty());
        verify(jdbc).queryForList(contains("FROM ast_asset"), eq(9L), eq("orders"));
        verify(jdbc).queryForObject(contains("FROM gov_space_quality_admission"), eq(Long.class), eq(1L), eq(9L), eq("orders"), eq(3));
        verifyNoMoreInteractions(jdbc);
    }
    @Test public void availableRegistrationRemainsPendingRuntimeUserAndColumnChecks() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); SpaceFlowReadinessService service = service(jdbc); Consumer consumer = source();
        when(jdbc.queryForList(contains("FROM ast_asset"), eq(9L), eq("orders"))).thenReturn(List.of(Map.of("id", 5L, "status", "2", "valid_flag", true)));
        when(jdbc.queryForObject(contains("FROM ast_asset_space_rel"), eq(Long.class), eq(5L), eq(1L), eq("space01"), eq(5L), eq(1L), eq("space01"))).thenReturn(1L);
        service.assess(consumer, definition("ASSET"), 1L, "space01", 3);
        assertEquals("PENDING_RUNTIME_CHECKS", consumer.getReadiness()); assertTrue(consumer.getMissingRequirements().isEmpty());
        assertFalse(consumer.getPendingChecks().isEmpty());
    }
    @Test public void strictUnresolvedScriptsAndSpaceMismatchNeverAppearReady() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); SpaceFlowReadinessService service = service(jdbc);
        Consumer script = source(); script.setSourceCoverage("CUSTOM_SQL_UNRESOLVED");
        service.assess(script, definition("ASSET"), 1L, "space01", 3);
        assertEquals(List.of("UNVERIFIED_SQL_SOURCES"), script.getMissingRequirements());
        Consumer file = source(); file.setSourceCoverage("NON_QUERY_UNRESOLVED");
        service.assess(file, definition("QUALITY"), 1L, "space01", 3);
        assertEquals(List.of("UNVERIFIED_NON_QUERY_SOURCES"), file.getMissingRequirements());
        Consumer inconsistent = source(); inconsistent.setSourceCoverage("SPACE_INCONSISTENT");
        service.assess(inconsistent, definition(), 1L, "space01", 3);
        assertEquals(List.of("SPACE_INCONSISTENT"), inconsistent.getMissingRequirements()); verifyNoInteractions(jdbc);
    }
    @Test public void unknownAndLightSourcesAreNotPresentedAsAuthorized() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); SpaceFlowReadinessService service = service(jdbc);
        Consumer unknown = new Consumer(); service.assess(unknown, definition(), 1L, "space01", 3);
        assertEquals("PENDING_RUNTIME_CHECKS", unknown.getReadiness()); assertFalse(unknown.getNextSteps().isEmpty());
        Consumer light = source(); service.assess(light, definition(), 1L, "space01", 3);
        assertEquals("PENDING_RUNTIME_CHECKS", light.getReadiness()); assertTrue(light.getMissingRequirements().isEmpty()); verifyNoInteractions(jdbc);
    }
    private static SpaceFlowReadinessService service(JdbcTemplate jdbc) throws Exception {
        SpaceFlowReadinessService service = new SpaceFlowReadinessService(); Field field = service.getClass().getDeclaredField("jdbc"); field.setAccessible(true); field.set(service, jdbc); return service;
    }
    private static Consumer source() { Consumer source = new Consumer(); source.setDatasourceId(9L); source.setTableName("orders"); source.setSourceCoverage("DECLARED_SINGLE_TABLE"); return source; }
    private static Definition definition(String... gates) { Definition definition = new Definition(); definition.getNodes().add("METADATA"); definition.getNodes().addAll(Arrays.asList(gates)); definition.getNodes().addAll(List.of("API", "ANALYSIS")); return definition; }
}
