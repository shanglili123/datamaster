package com.datamaster.module.ai.service.gateway;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.common.utils.StringUtils;
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
    @Resource private com.datamaster.module.ai.service.profile.AiModelRoutingService routingService;

    public boolean available() {
        com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO routes = routes();
        AiSkillModelProperties selected = resolver.resolve(properties, routes == null ? null : routes.getAdvancedProfileId());
        return Boolean.TRUE.equals(selected.getEnabled()) && StringUtils.isNotBlank(selected.getApiUrl());
    }

    public ChatModel create(String scene) {
        com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO routes = routes();
        if (routes == null) return client(resolver.resolve(properties), scene, false);
        ChatModel economy = client(resolver.resolve(properties, routes.getEconomyProfileId()), scene, routes.getStrictJsonSchema());
        ChatModel advanced = client(resolver.resolve(properties, routes.getAdvancedProfileId()), scene, routes.getStrictJsonSchema());
        if (economy == null || advanced == null) throw new ServiceException("模型路由配置不可用");
        dev.langchain4j.model.chat.router.ChatModelRouter router =
                new TaskComplexityModelRouter(scene, routes.getComplexInputCharacters());
        if (routes.getSemanticDecisionEnabled()) {
            Long classifierId = routes.getDecisionProfileId() == null
                    ? routes.getEconomyProfileId() : routes.getDecisionProfileId();
            if (classifierId <= 0) throw new ServiceException("决策模型配置ID无效");
            // Direct client construction is intentional: create() here would recursively invoke routing.
            ChatModel classifier = client(resolver.resolve(properties, classifierId), "MODEL_ROUTE_DECISION", false);
            if (classifier == null) throw new ServiceException("决策分类模型不可用");
            router = new FrameworkDecisionModelRouter(scene, routes.getComplexInputCharacters(), classifier);
        }
        return RoutingChatModel.builder()
                .route("economy", economy).route("advanced", advanced)
                .defaultRoute("advanced")
                .router(router)
                .build();
    }

    private com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO routes() {
        Long spaceId = AiInvocationGateway.snapshotContext().getSpaceId();
        if (spaceId == null) return null;
        com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO routes = routingService.effective(spaceId);
        if (routes == null || !Boolean.TRUE.equals(routes.getEnabled())) return null;
        if (routes.getEconomyProfileId() == null || routes.getEconomyProfileId() <= 0
                || routes.getAdvancedProfileId() == null || routes.getAdvancedProfileId() <= 0
                || routes.getComplexInputCharacters() == null || routes.getComplexInputCharacters() < 1 || routes.getComplexInputCharacters() > 16000) {
            throw new ServiceException("模型路由配置无效，请检查当前空间模型及阈值");
        }
        return routes;
    }

    /** Final-answer streaming uses the configured advanced/default model without another classification round. */
    public String streamAnswer(String scene, dev.langchain4j.model.chat.request.ChatRequest request,
                               java.util.concurrent.atomic.AtomicBoolean cancelled, long remainingMillis,
                               java.util.function.Consumer<String> output) {
        var route = routes();
        AiSkillModelProperties selected = resolver.resolve(properties, route == null ? null : route.getAdvancedProfileId());
        if (!Boolean.TRUE.equals(selected.getEnabled()) || StringUtils.isBlank(selected.getApiUrl()))
            throw new ServiceException("问数模型未配置");
        String key = apiKey(selected);
        String model = modelName(selected);
        int timeout = (int) Math.min(Math.max(1, remainingMillis), Math.max(1000, selected.getTimeout() == null ? 180000 : selected.getTimeout()));
        var stream = dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder().baseUrl(baseUrl(selected))
                .apiKey(StringUtils.isBlank(key) ? "none" : key).modelName(model)
                .maxTokens(selected.getMaxTokens() == null ? 8192 : selected.getMaxTokens())
                .returnThinking(false)
                .timeout(Duration.ofMillis(timeout)).logRequests(false).logResponses(false).build();
        var completion = new java.util.concurrent.CompletableFuture<dev.langchain4j.model.chat.response.ChatResponse>();
        var handle = new java.util.concurrent.atomic.AtomicReference<dev.langchain4j.model.chat.response.StreamingHandle>();
        try (var invocation = gateway.begin("MODEL", scene, selected.getProvider(), model, true, AiInvocationGateway.snapshotContext())) {
            try {
                stream.chat(request, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String text) {
                        if (!cancelled.get() && !completion.isDone()) output.accept(text);
                    }
                    @Override public void onPartialResponse(dev.langchain4j.model.chat.response.PartialResponse part,
                                                            dev.langchain4j.model.chat.response.PartialResponseContext context) {
                        handle.set(context.streamingHandle());
                        if (cancelled.get() || completion.isDone()) context.streamingHandle().cancel();
                        else onPartialResponse(part.text());
                    }
                    @Override public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse result) {
                        completion.complete(result);
                    }
                    @Override public void onError(Throwable error) { completion.completeExceptionally(error); }
                });
                long until = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeout);
                while (!cancelled.get() && System.nanoTime() < until) {
                    try {
                        var result = completion.get(200, java.util.concurrent.TimeUnit.MILLISECONDS);
                        InvocationChatModelListener.markSuccess(invocation, result.tokenUsage());
                        if (result.finishReason() != null && result.finishReason() != dev.langchain4j.model.output.FinishReason.STOP)
                            throw new IncompleteAnswerException();
                        return result.aiMessage().text();
                    } catch (java.util.concurrent.TimeoutException notReady) {
                        // Check cancellation between waits even when the provider is silent.
                    }
                }
                throw new IllegalStateException("最终回答已停止或超时");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                InvocationFailureClassifier.mark(invocation, ex);
                throw new IllegalStateException("最终回答已中断", ex);
            } catch (java.util.concurrent.ExecutionException ex) {
                InvocationFailureClassifier.mark(invocation, ex.getCause());
                throw new IllegalStateException("最终回答生成失败", ex.getCause());
            } catch (IncompleteAnswerException ex) {
                // The provider completed and reported usage, but the application must not treat the body as final.
                throw ex;
            } catch (RuntimeException ex) {
                InvocationFailureClassifier.mark(invocation, ex);
                throw ex;
            } finally {
                if (completion.cancel(false) && handle.get() != null) handle.get().cancel();
            }
        }
    }

    public static final class IncompleteAnswerException extends RuntimeException {
        private IncompleteAnswerException() { super("模型未正常结束正文输出"); }
    }

    private static String apiKey(AiSkillModelProperties selected) {
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
        return key;
    }

    private static String baseUrl(AiSkillModelProperties selected) {
        String url = selected.getApiUrl().trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) url = url.substring(0, url.length() - "/chat/completions".length());
        return url;
    }

    private static String modelName(AiSkillModelProperties selected) {
        return StringUtils.isBlank(selected.getModel()) ? "qwen-plus" : selected.getModel();
    }

    private ChatModel client(AiSkillModelProperties selected, String scene, boolean strictJsonSchema) {
        if (!Boolean.TRUE.equals(selected.getEnabled()) || StringUtils.isBlank(selected.getApiUrl())) return null;
        String key = apiKey(selected);
        String model = modelName(selected);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder().baseUrl(baseUrl(selected))
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
