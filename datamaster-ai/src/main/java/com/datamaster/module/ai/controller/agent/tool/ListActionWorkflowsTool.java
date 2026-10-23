package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowPageReqVO;
import com.datamaster.module.ontology.service.IActionWorkflowService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 查询本体动作编排。 */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class ListActionWorkflowsTool {
    @Resource private IActionWorkflowService workflowService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "list_action_workflows", value = "查询当前或指定本体下的动作编排；支持 name、status 条件，ontologyId 可从页面/流程上下文复用")
    public AgentToolResult execute(AgentToolRequest.ListActionWorkflows input) {
        AgentToolContext context = AgentToolContextHolder.get();
        Long ontologyId = input == null ? null : AgentToolIds.longValue(input.getOntologyId());
        Object pageOntologyId = context == null || context.getAttributes() == null ? null : context.getAttributes().get("ontologyId");
        if (pageOntologyId != null) ontologyId = AgentToolIds.longValue(String.valueOf(pageOntologyId));
        if (ontologyId == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            ontologyId = operation == null ? null : operation.getOntologyId();
        }
        if (ontologyId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "缺少 ontologyId，且当前页面/流程没有绑定本体", operationId(context));
        if (!resourceGuard.ontologyVisible(ontologyId, context)) {
            return AgentToolResult.failure("本体不属于当前空间，拒绝查询动作编排");
        }
        ActionWorkflowPageReqVO req = new ActionWorkflowPageReqVO();
        req.setOntologyId(ontologyId); req.setName(input == null ? null : input.getName());
        req.setStatus(input == null ? null : input.getStatus()); req.setPageNum(1); req.setPageSize(100);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, workflowService.getWorkflowPage(req), "动作编排查询完成", operationId(context));
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
