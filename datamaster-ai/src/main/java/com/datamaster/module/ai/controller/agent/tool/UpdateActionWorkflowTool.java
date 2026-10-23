package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowRespVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowSaveReqVO;
import com.datamaster.module.ontology.service.IActionWorkflowService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 修改动作编排草稿元数据。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class UpdateActionWorkflowTool {
    @Resource private IActionWorkflowService workflowService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "update_action_workflow", value = "修改已有动作编排草稿的名称、编码、触发器或描述；不会自动发布或执行，需要用户确认")
    public AgentToolResult execute(AgentToolRequest.UpdateActionWorkflow input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = input == null ? null : AgentToolIds.longValue(input.getWorkflowId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "修改动作编排需要 workflowId", operationId(context));
        ActionWorkflowRespVO current = workflowService.getWorkflow(id);
        if (current == null || !resourceGuard.ontologyVisible(current.getOntologyId(), context))
            return AgentToolResult.failure("动作编排不存在或不属于当前空间");
        ActionWorkflowSaveReqVO request = new ActionWorkflowSaveReqVO(); request.setId(id);
        request.setOntologyId(current.getOntologyId()); request.setName(blank(input.getName()) ? current.getName() : input.getName().trim());
        request.setCode(input.getCode() == null ? current.getCode() : input.getCode());
        request.setTriggerRef(input.getTriggerRef() == null ? current.getTriggerRef() : input.getTriggerRef());
        request.setDescription(input.getDescription() == null ? current.getDescription() : input.getDescription());
        request.setNodes(current.getNodes()); request.setEdges(current.getEdges());
        workflowService.updateWorkflow(request);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, workflowService.getWorkflow(id), "动作编排更新完成", operationId(context));
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
