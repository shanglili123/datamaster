package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.service.IActionExecutionService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 查询动作执行记录状态。 */
@Component
@AgentTool
public class GetActionExecutionTool {

    @Resource
    private IActionExecutionService actionExecutionService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "get_action_execution", value = "查询动作执行记录的审批、预览、执行状态和错误信息")
    public AgentToolResult execute(AgentToolRequest.GetActionExecution input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long executionId = AgentToolIds.longValue(input.getExecutionId());
        if (executionId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 executionId", context.getOperationId());
        ExecutionRespVO response = actionExecutionService.getExecutionById(executionId);
        if (response == null) return AgentToolResult.failure("动作执行记录不存在: " + executionId);
        if (!resourceGuard.executionVisible(executionId, context)) {
            return AgentToolResult.failure("执行记录不属于当前空间，拒绝查询");
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response, "已查询动作执行状态", context.getOperationId());
    }

}
