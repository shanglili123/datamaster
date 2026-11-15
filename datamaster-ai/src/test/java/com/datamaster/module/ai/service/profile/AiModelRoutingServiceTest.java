package com.datamaster.module.ai.service.profile;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.AiModelRoutingProperties;
import com.datamaster.module.ai.controller.admin.profile.vo.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class AiModelRoutingServiceTest {
    private JdbcTemplate jdbc;
    private AiModelProfileService profiles;
    private AiModelRoutingProperties defaults;
    private AiModelRoutingService service;
    @Before public void setup() {
        jdbc = mock(JdbcTemplate.class); profiles = mock(AiModelProfileService.class); defaults = new AiModelRoutingProperties();
        service = new AiModelRoutingService(jdbc, profiles, defaults);
        when(jdbc.query(contains("FROM ai_model_routing"), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());
        when(jdbc.query(contains("FOR UPDATE"), any(RowMapper.class), eq(10L))).thenReturn(List.of(10L));
    }
    @Test public void absentOverrideUsesDisabledDefaultWithoutResolvingModels() {
        var value = service.get(10L, "space10"); assertFalse(value.getEnabled()); assertEquals(Integer.valueOf(0), value.getRevision());
        assertEquals("DEPLOYMENT", value.getSource()); verifyNoInteractions(profiles);
    }
    @Test public void existingYamlRemainsTheDefaultUntilPageSaves() {
        defaults.setEnabled(true); var route = new AiModelRoutingProperties.SpaceRoutes();
        route.setEconomyProfileId(1L); route.setAdvancedProfileId(2L); defaults.getSpaces().put(10L, route);
        assertTrue(service.get(10L, "space10").getEnabled()); assertEquals(Long.valueOf(2), service.get(10L, "space10").getAdvancedProfileId());
        assertNull(service.effective(null));
    }
    @Test public void storedDisableOverridesEnabledYaml() {
        defaults.setEnabled(true); defaults.getSpaces().put(10L, new AiModelRoutingProperties.SpaceRoutes());
        var stored = new AiModelRoutingRespVO(); stored.setSpaceCode("space10"); stored.setEnabled(false); stored.setRevision(3);
        when(jdbc.query(contains("FROM ai_model_routing"), any(RowMapper.class), eq(10L))).thenReturn(List.of(stored));
        assertFalse(service.effective(10L).getEnabled()); assertEquals(Integer.valueOf(3), service.get(10L, "space10").getRevision());
        rejected(() -> service.get(10L, "other"));
    }
    @Test public void staleRevisionAndMissingSpaceRejectBeforeResolvingModels() {
        var request = request(); request.setRevision(2);
        rejected(() -> service.save(10L, "space10", 7L, request)); verifyNoInteractions(profiles);
        request.setRevision(0); when(jdbc.query(contains("FOR UPDATE"), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());
        rejected(() -> service.save(10L, "space10", 7L, request)); verifyNoInteractions(profiles);
    }
    @Test public void enablingRequiresBothModelsAndValidThreshold() {
        var request = request(); request.setEconomyProfileId(null);
        rejected(() -> service.save(10L, "space10", 7L, request)); verifyNoInteractions(profiles);
        var low = request(); low.setComplexInputCharacters(0); rejected(() -> AiModelRoutingService.validate(low));
        low.setComplexInputCharacters(16001); rejected(() -> AiModelRoutingService.validate(low));
        low.setComplexInputCharacters(12000); low.setDecisionProfileId(-1L); rejected(() -> AiModelRoutingService.validate(low));
    }
    @Test public void crossSpaceDisabledOrUndecryptableModelNeverWritesRouting() {
        when(profiles.resolve(10L, 2L)).thenThrow(new ServiceException("wrong space or disabled"));
        rejected(() -> service.save(10L, "space10", 7L, request()));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test public void firstSaveAndUpdateReturnSavedRevisionWithoutModelCalls() {
        var stored = new AiModelRoutingRespVO(); stored.setSpaceId(10L); stored.setSpaceCode("space10");
        stored.setEnabled(true); stored.setRevision(1); stored.setSource("DATABASE");
        when(jdbc.query(contains("FROM ai_model_routing"), any(RowMapper.class), eq(10L)))
                .thenReturn(Collections.emptyList(), List.of(stored));
        when(jdbc.update(startsWith("INSERT INTO ai_model_routing"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        assertEquals(Integer.valueOf(1), service.save(10L, "space10", 7L, request()).getRevision());
        verify(profiles).resolve(10L, 1L); verify(profiles).resolve(10L, 2L);
        stored.setRevision(2);
        var previous = new AiModelRoutingRespVO(); previous.setSpaceCode("space10"); previous.setRevision(1);
        when(jdbc.query(contains("FROM ai_model_routing"), any(RowMapper.class), eq(10L)))
                .thenReturn(List.of(previous), List.of(stored));
        when(jdbc.update(startsWith("UPDATE ai_model_routing"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        var update = request(); update.setRevision(1);
        assertEquals(Integer.valueOf(2), service.save(10L, "space10", 7L, update).getRevision());
    }

    @Test public void zeroRowWriteDoesNotReportSuccess() {
        rejected(() -> service.save(10L, "space10", 7L, request()));
        verify(profiles).resolve(10L, 1L); verify(profiles).resolve(10L, 2L);
    }
    @Test public void malformedFlagsAndMissingRevisionAreRejected() {
        var value = request(); value.setEnabled(null); rejected(() -> AiModelRoutingService.validate(value));
        value.setEnabled(false); value.setRevision(null); rejected(() -> AiModelRoutingService.validate(value));
    }
    private AiModelRoutingSaveReqVO request() {
        var request = new AiModelRoutingSaveReqVO(); request.setSpaceId(10L); request.setEnabled(true);
        request.setSemanticDecisionEnabled(false); request.setStrictJsonSchema(false); request.setEconomyProfileId(1L);
        request.setAdvancedProfileId(2L); request.setComplexInputCharacters(12000); request.setRevision(0); return request;
    }
    private static void rejected(Runnable action) { try { action.run(); fail("must reject"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); } }
}
