package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.service.IActionService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 删除动作定义。 */
@Component
@AgentTool(risk = AgentToolRisk.DESTRUCTIVE, requiresConfirmation = true, confirmationArgument = "actionId")
public class DeleteActionTool {
    @Resource private IActionService actionService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "delete_action", value = "删除当前空间的动作定义；不可逆，必须确认同一个 actionId")
    public AgentToolResult execute(AgentToolRequest.DeleteAction input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = input == null ? null : AgentToolIds.longValue(input.getActionId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "删除动作需要 actionId", operationId(context));
        if (!resourceGuard.actionVisible(id, context)) return AgentToolResult.failure("动作不属于当前空间，拒绝删除");
        actionService.deleteAction(id);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, java.util.Collections.singletonMap("actionId", id), "动作定义已删除", operationId(context));
    }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
