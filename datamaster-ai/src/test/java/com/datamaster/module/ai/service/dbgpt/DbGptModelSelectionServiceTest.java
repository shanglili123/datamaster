package com.datamaster.module.ai.service.dbgpt;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.DbGptProperties;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO;
import com.datamaster.module.ai.service.profile.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Pure mocks; no provider or DB-GPT requests. */
public class DbGptModelSelectionServiceTest {
    @Test public void routedRemoteToolWorkSelectsAdvancedAndNeverAcceptsBareNames() {
        Fixture f=new Fixture(); AiModelRoutingRespVO routes=new AiModelRoutingRespVO(); routes.setEnabled(true); routes.setAdvancedProfileId(8L);
        when(f.routing.effective(1L)).thenReturn(routes);
        // The resolver is deliberately rejected after route selection: no global fallback is permitted.
        when(f.profiles.resolve(1L,8L)).thenThrow(new ServiceException("当前模型尚未就绪"));
        rejected(() -> f.service.resolve(7L,1L,"space1",null,null)); verify(f.profiles).resolve(1L,8L);
        rejected(() -> f.service.resolve(7L,1L,"space1",null,"qwen-plus"));
        verifyNoInteractions(f.syncClient);
    }
    @Test public void explicitProfileIsResolvedOnlyWithinAuthorizedSpace() {
        Fixture f=new Fixture();
        when(f.profiles.resolve(1L,88L)).thenThrow(new ServiceException("该空间没有该配置"));
        rejected(() -> f.service.resolve(7L,1L,"space1",88L,null));
        verify(f.profiles).resolve(1L,88L); verifyNoInteractions(f.routing);
        rejected(() -> f.service.resolve(7L,2L,"space1",88L,null));
        verify(f.profiles,never()).resolve(2L,88L);
    }
    @Test public void conversationsAreStableButIsolatedByUserAndSpace() {
        Fixture f=new Fixture(); when(f.spaces.getSpaceCodeBySpaceId(2L)).thenReturn("space2");
        when(f.spaces.hasUserSpace(7L,2L,"space2")).thenReturn(true);
        when(f.spaces.hasUserSpace(8L,1L,"space1")).thenReturn(true);
        String a=f.service.conversation(7L,1L,"space1","session","old-");
        assertEquals(a,f.service.conversation(7L,1L,"space1","session","old-"));
        assertNotEquals(a,f.service.conversation(7L,2L,"space2","session","old-"));
        assertNotEquals(a,f.service.conversation(8L,1L,"space1","session","old-"));
        assertNotEquals(f.service.conversation(7L,1L,"space1",null,"old-"),f.service.conversation(7L,1L,"space1",null,"old-"));
    }
    @Test public void disabledSyncKeepsExplicitlySeparateLegacyMode() {
        Fixture f=new Fixture(); when(f.sync.enabled()).thenReturn(false);
        assertEquals("legacy-model",f.service.resolve(null,null,null,null,"legacy-model"));
        verifyNoInteractions(f.profiles,f.routing,f.spaces);
    }
    private static void rejected(Runnable action) { try { action.run(); fail("Expected rejection"); } catch(ServiceException expected) { assertNotNull(expected.getMessage()); } }
    private static class Fixture {
        final DbGptProperties config=new DbGptProperties();
        final AiModelProfileService profiles=mock(AiModelProfileService.class);
        final AiModelRoutingService routing=mock(AiModelRoutingService.class);
        final DbGptModelSyncService sync=mock(DbGptModelSyncService.class);
        final DbGptModelManagementClient syncClient=mock(DbGptModelManagementClient.class);
        final ISpaceApi spaces=mock(ISpaceApi.class);
        final DbGptModelSelectionService service=new DbGptModelSelectionService(config,profiles,routing,sync,spaces);
        Fixture() {
            when(sync.enabled()).thenReturn(true);
            when(spaces.getSpaceCodeBySpaceId(1L)).thenReturn("space1"); when(spaces.hasUserSpace(7L,1L,"space1")).thenReturn(true);
        }
    }
}
