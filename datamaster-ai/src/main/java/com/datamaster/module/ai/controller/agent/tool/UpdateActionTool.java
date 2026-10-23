package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionSaveReqVO;
import com.datamaster.module.ontology.service.IActionService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 修改动作定义元数据。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class UpdateActionTool {
    @Resource private IActionService actionService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "update_action", value = "修改已有动作定义；必须提供真实 actionId，不会把生成动作和执行动作混为一谈，需要用户确认")
    public AgentToolResult execute(AgentToolRequest.UpdateAction input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = input == null ? null : AgentToolIds.longValue(input.getActionId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "修改动作需要真实 actionId", operationId(context));
        if (!resourceGuard.actionVisible(id, context)) return AgentToolResult.failure("动作不属于当前空间，拒绝修改");
        ActionRespVO current = actionService.getActionById(id); if (current == null) return AgentToolResult.failure("动作不存在: " + id);
        ActionSaveReqVO request = new ActionSaveReqVO(); request.setId(id); request.setOntologyId(current.getOntologyId());
        request.setName(blank(input.getName()) ? current.getName() : input.getName().trim());
        request.setActionType(blank(input.getActionType()) ? current.getActionType() : input.getActionType().trim());
        request.setConceptId(input.getConceptId() == null ? current.getConceptId() : AgentToolIds.longValue(input.getConceptId()));
        request.setDescription(input.getDescription() == null ? current.getDescription() : input.getDescription());
        request.setExecutionSteps(input.getExecutionSteps() == null ? current.getExecutionSteps() : input.getExecutionSteps());
        request.setNeedsApproval(input.getNeedsApproval() == null ? current.getNeedsApproval() : input.getNeedsApproval());
        request.setParamConfig(input.getParamConfig() == null ? current.getParamConfig() : input.getParamConfig());
        actionService.updateAction(request);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, actionService.getActionById(id), "动作定义更新完成", operationId(context));
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
