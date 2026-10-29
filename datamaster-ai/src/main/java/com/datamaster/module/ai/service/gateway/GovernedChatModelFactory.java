package com.datamaster.module.ai.service.gateway;

import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.config.AiSkillModelProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Base64;

/** Shared provider-compatible client construction; all invocations retain admission and usage governance. */
@Component
public class GovernedChatModelFactory {
    @Resource private AiSkillModelProperties properties;
    @Resource private ActiveModelResolver resolver;
    @Resource private AiInvocationGateway gateway;

    public boolean available() {
        AiSkillModelProperties selected = resolver.resolve(properties);
        return Boolean.TRUE.equals(selected.getEnabled()) && StringUtils.isNotBlank(selected.getApiUrl());
    }

    public ChatLanguageModel create(String scene) {
        AiSkillModelProperties selected = resolver.resolve(properties);
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
        return new GovernedChatLanguageModel(OpenAiChatModel.builder().baseUrl(url)
                .apiKey(StringUtils.isBlank(key) ? "none" : key).modelName(model)
                .temperature(selected.getTemperature() == null ? 0.2D : selected.getTemperature())
                .maxTokens(selected.getMaxTokens() == null ? 8192 : selected.getMaxTokens())
                .timeout(Duration.ofMillis(Math.max(1000, selected.getTimeout() == null ? 180000 : selected.getTimeout())))
                .maxRetries(Math.max(0, selected.getRetryCount() == null ? 2 : selected.getRetryCount()))
                .parallelToolCalls(false).strictTools(false).build(), gateway, scene, selected.getProvider(), model);
    }
}
