package com.datamaster.module.ai.service.gateway;

import com.datamaster.module.ai.config.AiSkillModelProperties;
import com.datamaster.module.ai.service.profile.AiModelProfileService;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/** 每次创建客户端时读取当前空间默认配置；没有动态默认才使用原部署配置。 */
@Component
public class ActiveModelResolver {
    @Resource private AiModelProfileService profiles;
    public AiSkillModelProperties resolve(AiSkillModelProperties fallback) {
        return resolve(fallback, null);
    }

    public AiSkillModelProperties resolve(AiSkillModelProperties fallback, Long profileId) {
        Long spaceId = AiInvocationGateway.snapshotContext().getSpaceId();
        if (profileId != null && spaceId == null) {
            throw new com.datamaster.common.exception.ServiceException("模型路由需要可信空间上下文");
        }
        AiModelProfileService.Resolved selected = profiles.resolve(spaceId, profileId);
        if (selected == null) return fallback;
        AiSkillModelProperties result = new AiSkillModelProperties();
        result.setEnabled(selected.getEnabled()); result.setApiUrl(selected.getApiUrl());
        result.setApiKey(selected.getApiKey()); result.setModel(selected.getModel()); result.setProvider(selected.getProvider());
        result.setTimeout(fallback.getTimeout()); result.setRetryCount(fallback.getRetryCount());
        result.setRetryInterval(fallback.getRetryInterval()); result.setTemperature(fallback.getTemperature()); result.setMaxTokens(fallback.getMaxTokens());
        return result;
    }
}
