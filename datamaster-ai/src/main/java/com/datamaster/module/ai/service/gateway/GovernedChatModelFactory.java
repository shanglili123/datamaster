package com.datamaster.module.ai.service.gateway;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.config.AiModelRoutingProperties;
import com.datamaster.module.ai.config.AiSkillModelProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.router.RoutingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Base64;

/** Shared provider-compatible clients; route selection never bypasses per-model admission/monitoring. */
@Component
public class GovernedChatModelFactory {
    @Resource private AiSkillModelProperties properties;
    @Resource private ActiveModelResolver resolver;
    @Resource private AiInvocationGateway gateway;
    @Resource private AiModelRoutingProperties routing;

    public boolean available() {
        AiModelRoutingProperties.SpaceRoutes routes = routes();
        AiSkillModelProperties selected = resolver.resolve(properties, routes == null ? null : routes.getAdvancedProfileId());
        return Boolean.TRUE.equals(selected.getEnabled()) && StringUtils.isNotBlank(selected.getApiUrl());
    }

    public ChatModel create(String scene) {
        AiModelRoutingProperties.SpaceRoutes routes = routes();
        if (routes == null) return client(resolver.resolve(properties), scene, false);
        ChatModel economy = client(resolver.resolve(properties, routes.getEconomyProfileId()), scene, routes.isStrictJsonSchema());
        ChatModel advanced = client(resolver.resolve(properties, routes.getAdvancedProfileId()), scene, routes.isStrictJsonSchema());
        if (economy == null || advanced == null) throw new ServiceException("模型路由配置不可用");
        dev.langchain4j.model.chat.router.ChatModelRouter router =
                new TaskComplexityModelRouter(scene, routing.getComplexInputCharacters());
        if (routes.isSemanticDecisionEnabled()) {
            Long classifierId = routes.getDecisionProfileId() == null
                    ? routes.getEconomyProfileId() : routes.getDecisionProfileId();
            if (classifierId <= 0) throw new ServiceException("决策模型配置ID无效");
            // Direct client construction is intentional: create() here would recursively invoke routing.
            ChatModel classifier = client(resolver.resolve(properties, classifierId), "MODEL_ROUTE_DECISION", false);
            if (classifier == null) throw new ServiceException("决策分类模型不可用");
            router = new FrameworkDecisionModelRouter(scene, routing.getComplexInputCharacters(), classifier);
        }
        return RoutingChatModel.builder()
                .route("economy", economy).route("advanced", advanced)
                .defaultRoute("advanced")
                .router(router)
                .build();
    }

    private AiModelRoutingProperties.SpaceRoutes routes() {
        if (!routing.isEnabled()) return null;
        Long spaceId = AiInvocationGateway.snapshotContext().getSpaceId();
        if (spaceId == null) return null;
        AiModelRoutingProperties.SpaceRoutes routes = routing.getSpaces().get(spaceId);
        if (routes == null) return null;
        if (routes.getEconomyProfileId() == null || routes.getEconomyProfileId() <= 0
                || routes.getAdvancedProfileId() == null || routes.getAdvancedProfileId() <= 0) {
            throw new ServiceException("模型路由必须配置同一空间的经济与高级模型ID");
        }
        return routes;
    }

    private ChatModel client(AiSkillModelProperties selected, String scene, boolean strictJsonSchema) {
        if (!Boolean.TRUE.equals(selected.getEnabled()) || StringUtils.isBlank(selected.getApiUrl())) return null;
        String key = selected.getApiKey();
        if (StringUtils.isNotBlank(key) && key.length() % 4 == 0 && key.matches("^[A-Za-z0-9+/=]+$")) {
            try {
                byte[] decoded = Base64.getDecoder().decode(key);
                if (decoded.length > 0 && decoded.length % 16 == 0) {
                    String decrypted = AesEncryptUtil.desEncrypt(key);
                    if (StringUtils.isNotBlank(decrypted)) key = decrypted.trim();
                }
            } catch (Exception ignored) { /* Preserve existing plaintext-key compatibility. */ }
        }
        String url = selected.getApiUrl().trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) url = url.substring(0, url.length() - "/chat/completions".length());
        String model = StringUtils.isBlank(selected.getModel()) ? "qwen-plus" : selected.getModel();
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder().baseUrl(url)
                .apiKey(StringUtils.isBlank(key) ? "none" : key).modelName(model)
                .temperature(selected.getTemperature() == null ? 0.2D : selected.getTemperature())
                .maxTokens(selected.getMaxTokens() == null ? 8192 : selected.getMaxTokens())
                .timeout(Duration.ofMillis(Math.max(1000, selected.getTimeout() == null ? 180000 : selected.getTimeout())))
                .maxRetries(Math.max(0, selected.getRetryCount() == null ? 2 : selected.getRetryCount()))
                .parallelToolCalls(false).strictTools(false)
                .logRequests(false).logResponses(false)
                .listeners(new InvocationChatModelListener());
        // Opt in only after BOTH configured routes have verified provider JSON-schema support.
        if (strictJsonSchema) builder.supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA).strictJsonSchema(true);
        return new GovernedChatLanguageModel(builder.build(), gateway, scene, selected.getProvider(), model);
    }
}
