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
import com.datamaster.module.ontology.api.IOntologyModelGateway;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class AiModelGatewayServiceImpl implements IAiModelGatewayService, IOntologyModelGateway {

    @Resource
    private AiSkillModelProperties properties;
    @Resource
    private com.datamaster.module.ai.service.gateway.ActiveModelResolver activeModelResolver;
    @Resource
    private com.datamaster.module.ai.service.gateway.AiInvocationGateway invocationGateway;
    @Resource
    private com.datamaster.module.ai.service.gateway.StructuredModelService structured;

    @Override
    public com.datamaster.module.ai.service.skill.dto.ReportTemplateOutput generateReportTemplate(String system, String user) {
        return structured.report(system, user);
    }

    @Override
    public com.datamaster.module.ai.service.skill.dto.SkillEnhancementOutput enhanceSkill(String system, String user) {
        return structured.skill(system, user);
    }

    @Override
    public com.datamaster.module.ontology.api.dto.OntologyModelOutput generateOntology(String system, String user) {
        return structured.ontology(system, user);
    }

    @Override
    public com.datamaster.module.ontology.api.dto.ActionGenerationOutput generateActions(String system, String user) {
        return structured.actions(system, user);
    }

    @Override
    public com.datamaster.module.ontology.api.dto.ActionDecisionOutput decideAction(String system, String user) {
        return structured.decision(system, user);
    }

    @Override
    public boolean available() {
        AiSkillModelProperties properties = activeModelResolver.resolve(this.properties);
        return Boolean.TRUE.equals(properties.getEnabled()) && StringUtils.isNotBlank(properties.getApiUrl());
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ModelConfig modelConfig = resolveModelConfig();
        if (modelConfig == null) {
            throw new ServiceException("决策智能体模型未启用或未配置API地址");
        }
        try (com.datamaster.module.ai.service.gateway.AiInvocationGateway.Invocation call =
                     invocationGateway.begin("MODEL", "TEXT_COMPLETION", modelConfig.provider, modelConfig.getModel(), false,
                             com.datamaster.module.ai.service.gateway.AiInvocationGateway.snapshotContext())) {
            int maxAttempts = Math.max(1, modelConfig.getRetryCount() + 1);
            ServiceException lastFailure = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    String text = requestCompletion(systemPrompt, userPrompt, modelConfig, call);
                    return text;
                } catch (ServiceException e) {
                    lastFailure = e;
                    if (!isRetryable(e) || attempt >= maxAttempts) throw e;
                    sleepBeforeRetry(modelConfig.getRetryInterval(), attempt);
                }
            }
            throw lastFailure == null ? new ServiceException("AI模型调用失败") : lastFailure;
        }
    }

    private String requestCompletion(String systemPrompt, String userPrompt, ModelConfig modelConfig,
                                     com.datamaster.module.ai.service.gateway.AiInvocationGateway.Invocation call) {
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
                call.failure("HTTP", response.getStatus());
                throw new ServiceException("AI模型调用失败，状态码: " + response.getStatus());
            }
            DbGptChatCompletionResponse result = JSON.parseObject(body, DbGptChatCompletionResponse.class);
            if (result != null && result.getChoices() != null && !result.getChoices().isEmpty()
                    && result.getChoices().get(0).getMessage() != null) {
                com.datamaster.module.ai.service.gateway.InvocationUsage usage =
                        com.datamaster.module.ai.service.gateway.InvocationUsage.fromJson(body);
                call.success(usage.getInput(), usage.getOutput());
                return result.getChoices().get(0).getMessage().getContent();
            }
            JSONObject raw = JSON.parseObject(body);
            String text = raw.getString("content");
            if (StringUtils.isNotBlank(text)) {
                com.datamaster.module.ai.service.gateway.InvocationUsage usage = com.datamaster.module.ai.service.gateway.InvocationUsage.fromJson(body);
                call.success(usage.getInput(), usage.getOutput());
                return text;
            }
            throw new ServiceException("AI模型响应为空");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            call.failure("NETWORK", null);
            throw new ServiceException("AI模型调用异常，请检查模型连接配置");
        }
    }

    private boolean isRetryable(ServiceException exception) {
        String message = exception.getMessage();
        if (message == null) return false;
        String text = message.toLowerCase();
        return text.contains("timed out") || text.contains("timeout")
                || text.contains("connection reset") || text.contains("connection refused")
                || text.contains("状态码: 429") || text.contains("状态码: 500")
                || text.contains("状态码: 502") || text.contains("状态码: 503")
                || text.contains("状态码: 504");
    }

    private void sleepBeforeRetry(Long retryInterval, int attempt) {
        long interval = retryInterval == null ? 1500L : Math.max(0L, retryInterval);
        try {
            Thread.sleep(interval * Math.max(1, attempt));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ServiceException("AI模型重试被中断");
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
        AiSkillModelProperties properties = activeModelResolver.resolve(this.properties);
        if (Boolean.TRUE.equals(properties.getEnabled()) && StringUtils.isNotBlank(properties.getApiUrl())) {
            ModelConfig modelConfig = new ModelConfig();
            modelConfig.provider = properties.getProvider();
            modelConfig.setModel(defaultText(properties.getModel(), "qwen-plus"));
            modelConfig.setApiUrl(properties.getApiUrl());
            modelConfig.setApiKey(resolveApiKey(properties.getApiKey()));
            modelConfig.setTimeout(defaultInt(properties.getTimeout(), 180000));
            modelConfig.setRetryCount(defaultInt(properties.getRetryCount(), 2));
            modelConfig.setRetryInterval(properties.getRetryInterval() == null ? 1500L : properties.getRetryInterval());
            modelConfig.setTemperature(defaultDouble(properties.getTemperature(), 0.2D));
            modelConfig.setMaxTokens(defaultInt(properties.getMaxTokens(), 8192));
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
        private String provider;
        private String model;
        private String apiUrl;
        private String apiKey;
        private Integer timeout;
        private Integer retryCount;
        private Long retryInterval;
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

        public Integer getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(Integer retryCount) {
            this.retryCount = retryCount;
        }

        public Long getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(Long retryInterval) {
            this.retryInterval = retryInterval;
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

