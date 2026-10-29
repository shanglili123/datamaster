package com.datamaster.module.ai.service.gateway;

import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionRequest;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/** 仅统计平台到DB-GPT的一次业务请求，不冒充DB-GPT内部模型调用数。 */
@Aspect
@Component
public class DbGptInvocationAspect {
    @Resource private AiInvocationGateway gateway;
    @Resource private com.datamaster.module.ai.config.DbGptProperties properties;

    @Around("execution(* com.datamaster.module.ai.service.dbgpt.impl.DbGptClientServiceImpl.chatCompletion*(..))")
    public Object invoke(ProceedingJoinPoint point) throws Throwable {
        DbGptChatCompletionRequest request = null;
        for (Object argument : point.getArgs()) if (argument instanceof DbGptChatCompletionRequest) request = (DbGptChatCompletionRequest) argument;
        String method = point.getSignature().getName();
        boolean streaming = method.contains("Stream");
        try (AiInvocationGateway.Invocation call = gateway.begin("DBGPT", method, properties.getProvider(),
                     request == null || request.getModel() == null ? properties.getModel() : request.getModel(), streaming, AiInvocationGateway.snapshotContext());
             DbGptInvocationUsage.Scope usageScope = DbGptInvocationUsage.scope()) {
            try {
                Object result = point.proceed();
                InvocationUsage measured = usageScope.usage();
                Long input = measured.getInput(), output = measured.getOutput();
                if (result instanceof DbGptChatCompletionResponse) {
                    DbGptChatCompletionResponse.Usage usage = ((DbGptChatCompletionResponse) result).getUsage();
                    if (usage != null) {
                        if (usage.getPromptTokens() != null) input = usage.getPromptTokens().longValue();
                        if (usage.getCompletionTokens() != null) output = usage.getCompletionTokens().longValue();
                    }
                }
                if (result instanceof String) {
                    for (String line : ((String) result).split("\\r?\\n")) measured.acceptLine(line);
                    input = measured.getInput(); output = measured.getOutput();
                }
                call.success(input, output);
                return result;
            } catch (Throwable ex) {
                call.usage(usageScope.usage().getInput(), usageScope.usage().getOutput());
                call.failure("PROVIDER", null);
                throw ex;
            }
        }
    }
}
