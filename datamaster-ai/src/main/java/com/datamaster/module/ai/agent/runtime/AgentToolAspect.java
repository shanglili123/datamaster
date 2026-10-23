package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool 的统一治理切面。Tool 类只负责参数适配和调用 Service，
 * 确认、脱敏审计、结果归一化和异常边界在这里集中处理；入口权限和资源归属
 * 仍由 Controller/Service 负责，避免异步线程丢失 SecurityContext。
 */
@Aspect
@Component
@Slf4j
public class AgentToolAspect {

    @Resource
    private AgentToolAuditService auditService;

    @Around("execution(public * execute(..)) && @within(com.datamaster.module.ai.agent.tool.AgentTool)")
    public Object invoke(ProceedingJoinPoint joinPoint) {
        long started = System.currentTimeMillis();
        AgentTool tool = AopUtils.getTargetClass(joinPoint.getTarget()).getAnnotation(AgentTool.class);
        String toolName = frameworkToolName(joinPoint);
        AgentToolContext context = context(joinPoint.getArgs());
        Map<String, Object> arguments = arguments(joinPoint.getArgs());
        AgentToolResult result;
        try {
            if (tool.risk() != AgentToolRisk.READ_ONLY
                    && AgentToolContextHolder.hasSuccessfulCall(toolName, arguments)) {
                // LangChain4j 可能在同一轮继续请求同一个写 Tool；成功后禁止重复写入，
                // 否则模型会在“已成功”与“继续修改”之间循环。
                result = AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "该写操作已使用相同参数成功执行，本轮不再重复调用，请直接向用户返回已完成结果",
                        context == null ? null : context.getOperationId());
            } else if (needsConfirmation(tool) && !confirmed(tool, context, arguments)) {
                result = AgentToolResult.of(AgentToolResult.NEED_CONFIRMATION, null,
                        confirmationLabel(toolName) + "属于写操作，需要用户确认后执行",
                        context == null ? null : context.getOperationId());
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("toolName", toolName);
                metadata.put("risk", tool.risk().name());
                metadata.put("confirmationArgument", tool.confirmationArgument());
                metadata.put("arguments", AgentPayloadSanitizer.sanitizeValue(null, arguments));
                if (tool.confirmationArgument() != null && !tool.confirmationArgument().trim().isEmpty()) {
                    metadata.put(tool.confirmationArgument(), AgentPayloadSanitizer.sanitizeValue(
                            tool.confirmationArgument(), arguments.get(tool.confirmationArgument())));
                }
                result.setMetadata(metadata);
            } else {
                Object value = joinPoint.proceed();
                result = value instanceof AgentToolResult
                        ? (AgentToolResult) value
                        : AgentToolResult.of(AgentToolResult.SUCCEEDED, value, "Tool 执行完成",
                        context == null ? null : context.getOperationId());
            }
        } catch (Throwable ex) {
            log.error("Agent Tool 执行异常，toolName={}，arguments={}", toolName,
                    AgentPayloadSanitizer.sanitizeValue(null, arguments), ex);
            result = AgentToolResult.of(AgentToolResult.FAILED, null,
                    describeException(ex),
                    context == null ? null : context.getOperationId());
        }
        result.setToolName(toolName);
        if (result.getOperationId() == null && context != null) result.setOperationId(context.getOperationId());
        AgentToolContextHolder.record(result);
        AgentToolContextHolder.recordCall(toolName, arguments, result);
        if (auditService != null) auditService.record(toolName, context, arguments, result,
                System.currentTimeMillis() - started);
        return result;
    }

    private String describeException(Throwable error) {
        Throwable root = error;
        while (root != null && root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root == null) return "Tool 执行失败";
        String message = root.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Tool 执行失败: " + root.getClass().getSimpleName()
                : "Tool 执行失败: " + root.getClass().getSimpleName() + ": " + message.trim();
    }

    private String frameworkToolName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Tool definition = signature.getMethod().getAnnotation(Tool.class);
        if (definition == null) {
            try {
                definition = AopUtils.getTargetClass(joinPoint.getTarget())
                        .getMethod(signature.getName(), signature.getParameterTypes())
                        .getAnnotation(Tool.class);
            } catch (NoSuchMethodException ignored) {
                // 目录组件会在启动时拦截缺少框架注解的 Tool；这里仅为代理调用提供兜底。
            }
        }
        if (definition == null || definition.name() == null || definition.name().trim().isEmpty()) {
            throw new IllegalStateException("Tool 缺少 LangChain4j @Tool name");
        }
        return definition.name().trim();
    }

    private boolean needsConfirmation(AgentTool tool) {
        return tool.risk() != AgentToolRisk.READ_ONLY && tool.requiresConfirmation();
    }

    private String confirmationLabel(String toolName) {
        if ("generate_ontology".equals(toolName)) return "生成本体模型（generate_ontology）";
        if ("generate_actions".equals(toolName)) return "生成动作预览（generate_actions）";
        if ("generate_action_workflow".equals(toolName)) return "生成动作编排草稿（generate_action_workflow）";
        return "Tool " + toolName;
    }

    private boolean confirmed(AgentTool tool, AgentToolContext context, Map<String, Object> arguments) {
        if (context == null || context.getAttributes() == null) return false;
        Object flag = context.getAttributes().get("confirmed");
        if (!(Boolean.TRUE.equals(flag) || "true".equalsIgnoreCase(String.valueOf(flag)))) return false;
        String key = tool.confirmationArgument();
        if (key == null || key.trim().isEmpty()) return true;
        Object actual = arguments.get(key);
        Object expected = context.getAttributes().get("confirmed" + Character.toUpperCase(key.charAt(0)) + key.substring(1));
        return actual != null && expected != null && String.valueOf(actual).equals(String.valueOf(expected));
    }

    private AgentToolContext context(Object[] args) {
        for (Object arg : args) if (arg instanceof AgentToolContext) return (AgentToolContext) arg;
        return AgentToolContextHolder.get();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> arguments(Object[] args) {
        for (Object arg : args) {
            if (arg == null || arg instanceof AgentToolContext) continue;
            if (arg instanceof Map) return new LinkedHashMap<>((Map<String, Object>) arg);
            // Tool 现在接收类型化请求 DTO；将其展开为 Map，保证确认校验和审计
            // 仍然能够看到 executionId、datasourceId 等业务参数，而不把治理逻辑
            // 重新耦合到每个 DTO 类型。
            try {
                Map<String, Object> value = JSON.parseObject(JSON.toJSONString(arg), Map.class);
                if (value != null) return new LinkedHashMap<>(value);
            } catch (Exception ignored) {
                // 无法展开的参数不影响 Tool 本身执行，审计保留空参数摘要。
            }
        }
        return new LinkedHashMap<>();
    }
}
