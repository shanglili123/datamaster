package com.datamaster.module.ai.agent.tool;

import com.alibaba.fastjson2.JSON;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台 Tool 目录，仅用于管理端目录查询和人工调用入口。
 *
 * Agent 的 Tool Calling 和调度交给 LangChain4j AiServices；本类不是 Agent
 * 注册器，也不负责调度循环。它只为管理端目录/人工调用提供查询能力，并提供
 * 一个把 Spring AOP 代理接入 LangChain4j ToolExecutor 的兼容适配器。
 */
@Component
public class AgentToolCatalog {

    @Resource
    private ApplicationContext applicationContext;

    private final Map<String, Descriptor> descriptors = new LinkedHashMap<>();

    @PostConstruct
    public void initialize() {
        descriptors.clear();
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(AgentTool.class);
        for (Object bean : beans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Method execute = findExecuteMethod(targetClass);
            if (execute == null) {
                throw new IllegalStateException("Agent Tool 缺少带 @Tool 的 execute 方法: " + targetClass.getName());
            }
            Tool annotation = execute.getAnnotation(Tool.class);
            if (annotation == null) {
                throw new IllegalStateException("Agent Tool 缺少 LangChain4j @Tool 注解: " + targetClass.getName());
            }
            ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(execute);
            String name = specification.name() == null ? "" : specification.name().trim();
            if (name.isEmpty() || specification.description() == null || specification.description().trim().isEmpty()) {
                throw new IllegalStateException("LangChain4j @Tool 缺少名称或描述: " + targetClass.getName());
            }
            if (descriptors.put(name, new Descriptor(bean, execute, specification)) != null) {
                throw new IllegalStateException("重复注册 Agent Tool: " + name);
            }
        }
    }

    /**
     * 以 LangChain4j 官方 ToolSpecification/ToolExecutor 形式提供工具。
     * AiServices 仍负责 Tool Calling、参数校验和调度；这里仅解决 Spring AOP
     * 代理无法直接暴露目标类 @Tool 注解的问题，不维护第二套工具协议。
     */
    public Map<ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> frameworkTools() {
        return frameworkTools(null);
    }

    /** 管理流程不暴露正式指标和 DB-GPT 查询工具，避免跨路径降级。 */
    public java.util.Set<String> managementToolNames() {
        java.util.Set<String> names = new java.util.HashSet<>(descriptors.keySet());
        names.removeAll(java.util.Arrays.asList("search_metrics", "get_metric_definition", "execute_metric_analysis", "compare_periods", "analyze_distribution", "analyze_trend", "build_chart_spec", "save_analysis", "dbgpt_query", "dbgpt_report", "generate_report_template"));
        return names;
    }

    /** A null allowlist keeps the full catalogue for existing callers; empty means no tools. */
    public Map<ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> frameworkTools(java.util.Set<String> allowedNames) {
        Map<ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> result = new LinkedHashMap<>();
        for (Descriptor descriptor : descriptors.values()) {
            if (allowedNames != null && !allowedNames.contains(descriptor.specification.name())) continue;
            final dev.langchain4j.service.tool.ToolExecutor delegate =
                    new DefaultToolExecutor(descriptor.bean, descriptor.execute);
            result.put(descriptor.specification, (request, memoryId) -> {
                String value;
                try {
                    value = delegate.execute(request, memoryId);
                } catch (com.datamaster.module.ai.agent.runtime.AgentToolFlowHaltException ex) {
                    throw ex;
                } catch (Exception ex) {
                    Throwable root = ex;
                    while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                    String detail = root.getClass().getSimpleName()
                            + (root.getMessage() == null ? "" : ": " + root.getMessage());
                    throw new IllegalStateException("Tool " + descriptor.specification.name() + " 执行失败: " + detail, ex);
                }
                AgentToolResult latest = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.lastResult();
                if (latest != null && (AgentToolResult.NEED_INPUT.equals(latest.getStatus())
                        || AgentToolResult.NEED_CONFIRMATION.equals(latest.getStatus())
                        || AgentToolResult.FAILED.equals(latest.getStatus()))) {
                    throw new com.datamaster.module.ai.agent.runtime.AgentToolFlowHaltException(latest);
                }
                return value;
            });
        }
        return result;
    }

    public List<Map<String, Object>> describe() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Descriptor descriptor : descriptors.values()) {
            AgentTool definition = descriptor.bean.getClass().getAnnotation(AgentTool.class);
            if (definition == null) definition = AopUtils.getTargetClass(descriptor.bean).getAnnotation(AgentTool.class);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", descriptor.specification.name());
            item.put("description", descriptor.specification.description());
            item.put("risk", definition == null ? AgentToolRisk.READ_ONLY.name() : definition.risk().name());
            item.put("requiresConfirmation", definition != null
                    && definition.risk() != AgentToolRisk.READ_ONLY && definition.requiresConfirmation());
            item.put("inputSchema", parameters(descriptor.specification.parameters()));
            result.add(item);
        }
        return result;
    }

    /** 管理端受控调用入口使用框架默认执行器，不再由平台反射适配参数。 */
    public AgentToolResult invoke(String name, AgentToolContext context, Map<String, Object> arguments) {
        Descriptor descriptor = descriptors.get(name);
        if (descriptor == null) {
            return AgentToolResult.of(AgentToolResult.FAILED, null, "未找到 Agent Tool: " + name,
                    context == null ? null : context.getOperationId());
        }
        Map<String, Object> invocationArguments = new LinkedHashMap<>();
        if (descriptor.execute.getParameterCount() == 1) {
            invocationArguments.put(descriptor.execute.getParameters()[0].getName(),
                    arguments == null ? Collections.emptyMap() : arguments);
        }
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id(name)
                .name(name)
                .arguments(JSON.toJSONString(invocationArguments))
                .build();
        com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.set(context);
        try {
            String value = new DefaultToolExecutor(descriptor.bean, descriptor.execute).execute(request, null);
            AgentToolResult recorded = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.lastResult();
            if (recorded != null && recorded.getStatus() != null
                    && !AgentToolResult.SUCCEEDED.equals(recorded.getStatus())
                    && !AgentToolResult.RUNNING.equals(recorded.getStatus())) return recorded;
            AgentToolResult result = JSON.parseObject(value, AgentToolResult.class);
            if (result == null) return AgentToolResult.failure("Tool 没有返回结果");
            result.setToolName(name);
            if (result.getOperationId() == null && context != null) result.setOperationId(context.getOperationId());
            return result;
        } catch (Exception ex) {
            return AgentToolResult.of(AgentToolResult.FAILED, null,
                    ex.getMessage() == null ? "Tool 参数或执行失败" : ex.getMessage(),
                    context == null ? null : context.getOperationId());
        } finally {
            com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.clear();
        }
    }

    private Map<String, Object> parameters(ToolParameters parameters) {
        if (parameters == null) return Collections.singletonMap("type", "object");
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", parameters.type());
        schema.put("properties", parameters.properties());
        if (parameters.required() != null && !parameters.required().isEmpty()) schema.put("required", parameters.required());
        return schema;
    }

    private Method findExecuteMethod(Class<?> type) {
        Method found = null;
        for (Method method : type.getMethods()) {
            if (!"execute".equals(method.getName()) || !java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
            if (method.getAnnotation(Tool.class) == null) continue;
            if (found != null) throw new IllegalStateException("Agent Tool 不允许定义多个 @Tool execute 重载: " + type.getName());
            found = method;
        }
        return found;
    }

    private static final class Descriptor {
        private final Object bean;
        private final Method execute;
        private final ToolSpecification specification;

        private Descriptor(Object bean, Method execute, ToolSpecification specification) {
            this.bean = bean;
            this.execute = execute;
            this.specification = specification;
        }
    }
}
