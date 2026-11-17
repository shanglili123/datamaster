package com.datamaster.module.ai.service.dbgpt;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.DbGptProperties;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO;
import com.datamaster.module.ai.service.profile.AiModelProfileService;
import com.datamaster.module.ai.service.profile.AiModelRoutingService;
import com.datamaster.module.ai.service.profile.DbGptModelSyncService;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Selects a trusted space-owned registration, not a caller-supplied global DB-GPT model name. */
@Service
public class DbGptModelSelectionService {
    private final DbGptProperties properties;
    private final AiModelProfileService profiles;
    private final AiModelRoutingService routing;
    private final DbGptModelSyncService sync;
    private final ISpaceApi spaces;
    public DbGptModelSelectionService(DbGptProperties properties,AiModelProfileService profiles,
            AiModelRoutingService routing,DbGptModelSyncService sync,ISpaceApi spaces) {
        this.properties=properties; this.profiles=profiles; this.routing=routing; this.sync=sync; this.spaces=spaces;
    }
    public String resolve(Long userId,Long spaceId,String spaceCode,Long profileId,String legacyModel) {
        if(!sync.enabled()) return legacyModel!=null && !legacyModel.isBlank()?legacyModel:properties.getModel();
        canonical(userId,spaceId,spaceCode);
        if(legacyModel!=null && !legacyModel.isBlank()) throw new ServiceException("接口同步模式不接受裸模型名称，请选择当前空间模型配置或使用默认路由");
        Long selected=profileId;
        if(selected==null) {
            AiModelRoutingRespVO routes=routing.effective(spaceId);
            // Remote react-agent performs tool/data work: never downgrade it to an economy classifier.
            if(routes!=null && Boolean.TRUE.equals(routes.getEnabled())) selected=routes.getAdvancedProfileId();
        }
        AiModelProfileService.Resolved profile=profiles.resolve(spaceId,selected);
        if(profile==null) throw new ServiceException("当前空间尚未配置默认模型");
        return sync.requireReady(profile.getId(),spaceId);
    }
    public String conversation(Long userId,Long spaceId,String spaceCode,String conversationId,String legacyPrefix) {
        if(!sync.enabled()) return legacyPrefix+(conversationId!=null && !conversationId.isBlank()?conversationId:UUID.randomUUID());
        String canonical=canonical(userId,spaceId,spaceCode);
        String local=conversationId==null || conversationId.isBlank()?UUID.randomUUID().toString():conversationId;
        // Stable per-user/per-space namespace; user input cannot collide with another space's conversation.
        return "dm-"+UUID.nameUUIDFromBytes((userId+"|"+spaceId+"|"+canonical+"|"+local).getBytes(StandardCharsets.UTF_8));
    }
    private String canonical(Long userId,Long spaceId,String code) {
        if(userId==null || spaceId==null || spaceId<=0) throw new ServiceException("模型选择缺少可信用户或空间");
        String canonical=spaces.getSpaceCodeBySpaceId(spaceId);
        if(canonical==null || code!=null && !code.isBlank() && !canonical.equals(code)
                || !spaces.hasUserSpace(userId,spaceId,canonical)) throw new ServiceException("当前用户无权使用该空间模型");
        return canonical;
    }
}
