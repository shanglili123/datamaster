package com.datamaster.module.ai.agent.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个可供 Agent 调用的平台工具。
 *
 * 这是平台治理注解：权限、风险、确认和审计由 AgentToolAspect 统一处理，
 * 业务实现放在 Tool 内部调用已有 Service。Tool 名称和描述不在这里重复声明，
 * execute 方法上的 LangChain4j dev.langchain4j.agent.tool.Tool 是唯一来源。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentTool {

    AgentToolRisk risk() default AgentToolRisk.READ_ONLY;

    /** 写操作默认需要确认；查询类工具由 risk=READ_ONLY 自动免确认。 */
    boolean requiresConfirmation() default true;

    /** 需要用户二次确认时，用于校验确认资源是否与调用参数一致。 */
    String confirmationArgument() default "";
}
