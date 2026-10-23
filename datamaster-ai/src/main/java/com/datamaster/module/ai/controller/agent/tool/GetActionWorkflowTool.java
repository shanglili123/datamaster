package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.service.IActionWorkflowService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 查询动作编排详情。 */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class GetActionWorkflowTool {
    @Resource private IActionWorkflowService workflowService;

    @Tool(name = "get_action_workflow", value = "获取动作编排的节点、边、条件和失败策略")
    public AgentToolResult execute(AgentToolRequest.GetActionWorkflow input) {
        AgentToolContext context = AgentToolContextHolder.get();
        if (input == null || input.getWorkflowId() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 workflowId", operationId(context));
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, workflowService.getWorkflow(AgentToolIds.longValue(input.getWorkflowId())), "动作编排详情查询完成", operationId(context));
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
