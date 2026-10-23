package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowRespVO;
import com.datamaster.module.ontology.service.IActionWorkflowService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 删除动作编排。 */
@Component
@AgentTool(risk = AgentToolRisk.DESTRUCTIVE, requiresConfirmation = true, confirmationArgument = "workflowId")
public class DeleteActionWorkflowTool {
    @Resource private IActionWorkflowService workflowService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "delete_action_workflow", value = "删除当前空间的动作编排；不可逆，必须确认同一个 workflowId")
    public AgentToolResult execute(AgentToolRequest.DeleteActionWorkflow input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = input == null ? null : AgentToolIds.longValue(input.getWorkflowId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "删除动作编排需要 workflowId", operationId(context));
        ActionWorkflowRespVO current = workflowService.getWorkflow(id);
        if (current == null || !resourceGuard.ontologyVisible(current.getOntologyId(), context))
            return AgentToolResult.failure("动作编排不存在或不属于当前空间");
        workflowService.deleteWorkflow(id);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, java.util.Collections.singletonMap("workflowId", id), "动作编排已删除", operationId(context));
    }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
