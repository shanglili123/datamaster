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
import java.util.List;

/** 查询待审批动作；审批本身仍由人工审批接口完成。 */
@Component
@AgentTool
public class ListPendingApprovalsTool {

    @Resource
    private IActionExecutionService actionExecutionService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "list_pending_approvals", value = "查询指定本体下待人工审批的动作执行记录；Agent 不得代替审批人批准")
    public AgentToolResult execute(AgentToolRequest.ListPendingApprovals input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long ontologyId = AgentToolIds.longValue(input.getOntologyId());
        if (ontologyId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 ontologyId", context.getOperationId());
        if (!resourceGuard.ontologyVisible(ontologyId, context)) {
            return AgentToolResult.failure("本体不属于当前空间，拒绝查询待审批动作");
        }
        List<ExecutionRespVO> result = actionExecutionService.getPendingApprovals(ontologyId);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "已查询待审批动作", context.getOperationId());
    }

}
