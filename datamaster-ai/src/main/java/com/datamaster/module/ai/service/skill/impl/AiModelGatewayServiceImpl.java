package com.datamaster.module.ai.service.skill.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.config.AiSkillModelProperties;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionRequest;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionResponse;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatMessage;
import com.datamaster.module.ai.service.skill.IAiModelGatewayService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class AiModelGatewayServiceImpl implements IAiModelGatewayService {

    @Resource
    private AiSkillModelProperties properties;

    @Override
    public boolean available() {
        return Boolean.TRUE.equals(properties.getEnabled()) && StringUtils.isNotBlank(properties.getApiUrl());
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ModelConfig modelConfig = resolveModelConfig();
        if (modelConfig == null) {
            throw new ServiceException("AI问数模型未启用或未配置API地址");
        }
        DbGptChatCompletionRequest request = new DbGptChatCompletionRequest();
        request.setModel(modelConfig.getModel());
        request.setTemperature(modelConfig.getTemperature());
        request.setMaxTokens(modelConfig.getMaxTokens());
        request.setStream(false);
        request.setMessages(Arrays.asList(
                new DbGptChatMessage("system", systemPrompt),
                new DbGptChatMessage("user", userPrompt)
        ));

        try (HttpResponse response = HttpUtil.createRequest(Method.POST, normalizeUrl(modelConfig.getApiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", StringUtils.isBlank(modelConfig.getApiKey()) ? "" : "Bearer " + modelConfig.getApiKey())
                .body(JSON.toJSONString(request))
                .timeout(modelConfig.getTimeout())
                .execute()) {
            String body = response.body();
            if (response.getStatus() != 200) {
                throw new ServiceException("AI模型调用失败，状态码: " + response.getStatus() + "，响应: " + body);
            }
            DbGptChatCompletionResponse result = JSON.parseObject(body, DbGptChatCompletionResponse.class);
            if (result != null && result.getChoices() != null && !result.getChoices().isEmpty()
                    && result.getChoices().get(0).getMessage() != null) {
                return result.getChoices().get(0).getMessage().getContent();
            }
            JSONObject raw = JSON.parseObject(body);
            String text = raw.getString("content");
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
            throw new ServiceException("AI模型响应为空");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("AI模型调用异常: " + e.getMessage());
        }
    }

    private String normalizeUrl(String apiUrl) {
        String url = apiUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/chat/completions")) {
            return url;
        }
        return url + "/chat/completions";
    }

    private ModelConfig resolveModelConfig() {
        if (Boolean.TRUE.equals(properties.getEnabled()) && StringUtils.isNotBlank(properties.getApiUrl())) {
            ModelConfig modelConfig = new ModelConfig();
            modelConfig.setModel(defaultText(properties.getModel(), "qwen-plus"));
            modelConfig.setApiUrl(properties.getApiUrl());
            modelConfig.setApiKey(resolveApiKey(properties.getApiKey()));
            modelConfig.setTimeout(defaultInt(properties.getTimeout(), 60000));
            modelConfig.setTemperature(defaultDouble(properties.getTemperature(), 0.2D));
            modelConfig.setMaxTokens(defaultInt(properties.getMaxTokens(), 4096));
            return modelConfig;
        }
        return null;
    }

    private String resolveApiKey(String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            return "";
        }
        if (!looksLikeEncryptedText(apiKey)) {
            return apiKey;
        }
        try {
            String decrypted = AesEncryptUtil.desEncrypt(apiKey);
            return StringUtils.isBlank(decrypted) ? apiKey : decrypted.trim();
        } catch (Exception e) {
            return apiKey;
        }
    }

    private boolean looksLikeEncryptedText(String value) {
        String text = value.trim();
        return text.length() % 4 == 0 && text.matches("^[A-Za-z0-9+/=]+$");
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Double defaultDouble(Double value, Double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static class ModelConfig {
        private String model;
        private String apiUrl;
        private String apiKey;
        private Integer timeout;
        private Double temperature;
        private Integer maxTokens;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
}

