package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.service.IActionExecutionService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 执行已通过动作审批的执行记录，默认强制用户确认。 */
@Component
@AgentTool(risk = AgentToolRisk.DESTRUCTIVE, requiresConfirmation = true, confirmationArgument = "executionId")
public class ExecuteActionTool {

    @Resource
    private IActionExecutionService actionExecutionService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "execute_action", value = "执行已经通过动作审批的执行记录；未经用户确认不会执行数据库更新")
    public AgentToolResult execute(AgentToolRequest.ExecuteAction input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long executionId = AgentToolIds.longValue(input.getExecutionId());
        if (executionId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 executionId", context.getOperationId());
        if (!resourceGuard.executionVisible(executionId, context)) {
            return AgentToolResult.failure("执行记录不属于当前空间，拒绝执行");
        }
        ExecutionRespVO response = actionExecutionService.executeExecution(executionId, text(input.getInputParams()), true);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response, "动作执行完成", context.getOperationId());
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim();
    }

}
