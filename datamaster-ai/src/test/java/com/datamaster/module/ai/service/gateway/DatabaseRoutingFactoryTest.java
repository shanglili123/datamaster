package com.datamaster.module.ai.service.gateway;

import com.datamaster.module.ai.config.AiSkillModelProperties;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO;
import com.datamaster.module.ai.service.profile.AiModelRoutingService;
import dev.langchain4j.model.chat.router.RoutingChatModel;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Client construction only; never calls an endpoint. */
public class DatabaseRoutingFactoryTest {
    @Test public void readsCurrentOverrideForEachClientAndDoesNotRouteWhenDisabled() throws Exception {
        GovernedChatModelFactory factory = new GovernedChatModelFactory();
        AiModelRoutingService routing = mock(AiModelRoutingService.class); ActiveModelResolver resolver = mock(ActiveModelResolver.class);
        AiSkillModelProperties fallback = new AiSkillModelProperties();
        AiSkillModelProperties selected = new AiSkillModelProperties(); selected.setEnabled(true);
        selected.setApiUrl("http://127.0.0.1:1/v1"); selected.setApiKey("test-only"); selected.setModel("local-model");
        when(resolver.resolve(fallback)).thenReturn(selected); when(resolver.resolve(fallback, 1L)).thenReturn(selected);
        when(resolver.resolve(fallback, 2L)).thenReturn(selected);
        inject(factory, "properties", fallback); inject(factory, "resolver", resolver); inject(factory, "routingService", routing);
        var disabled = new AiModelRoutingRespVO(); disabled.setEnabled(false);
        var enabled = new AiModelRoutingRespVO(); enabled.setEnabled(true); enabled.setEconomyProfileId(1L); enabled.setAdvancedProfileId(2L);
        enabled.setComplexInputCharacters(100); enabled.setStrictJsonSchema(false); enabled.setSemanticDecisionEnabled(false);
        when(routing.effective(10L)).thenReturn(disabled, enabled, disabled);
        try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(new AgentToolContext("test", 7L, "user", 10L, "space10", Collections.emptyMap()))) {
            assertFalse(factory.create("REPORT_TEMPLATE") instanceof RoutingChatModel);
            assertTrue(factory.create("REPORT_TEMPLATE") instanceof RoutingChatModel);
            assertFalse(factory.create("REPORT_TEMPLATE") instanceof RoutingChatModel);
        }
        verify(routing, times(3)).effective(10L);
    }
    @Test public void missingTrustedSpaceNeverLooksUpAnotherSpacesRouting() throws Exception {
        var factory = new GovernedChatModelFactory(); var routing = mock(AiModelRoutingService.class); var resolver = mock(ActiveModelResolver.class);
        var properties = new AiSkillModelProperties(); when(resolver.resolve(properties)).thenReturn(properties);
        inject(factory, "properties", properties); inject(factory, "resolver", resolver); inject(factory, "routingService", routing);
        try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(null)) { assertNull(factory.create("REPORT_TEMPLATE")); }
        verifyNoInteractions(routing);
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
