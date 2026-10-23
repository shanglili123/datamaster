package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionPageReqVO;
import com.datamaster.module.ontology.service.IActionService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/** 查询本体动作定义，不执行动作。 */
@Component
@AgentTool
public class ListActionsTool {

    @Resource
    private IActionService actionService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "list_actions", value = "查询当前或指定本体下已有动作定义；支持 actionId、name、actionType 条件，供 Agent 选择后续预览或提交；ontologyId 可从页面/流程上下文复用")
    public AgentToolResult execute(AgentToolRequest.ListActions input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long ontologyId = input == null ? null : AgentToolIds.longValue(input.getOntologyId());
        Object pageOntologyId = context == null || context.getAttributes() == null
                ? null : context.getAttributes().get("ontologyId");
        if (pageOntologyId != null) ontologyId = AgentToolIds.longValue(String.valueOf(pageOntologyId));
        if (ontologyId == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            ontologyId = operation == null ? null : operation.getOntologyId();
        }
        if (ontologyId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "缺少 ontologyId，且当前页面/流程没有绑定本体", context == null ? null : context.getOperationId());
        if (!resourceGuard.ontologyVisible(ontologyId, context)) {
            return AgentToolResult.failure("本体不属于当前空间，拒绝查询动作");
        }
        String actionId = input == null ? null : input.getActionId();
        String name = input == null ? null : input.getName();
        String actionType = input == null ? null : input.getActionType();
        Object result;
        if ((actionId != null && !actionId.trim().isEmpty()) || (name != null && !name.trim().isEmpty())
                || (actionType != null && !actionType.trim().isEmpty()) || (input != null && input.getLimit() != null)) {
            ActionPageReqVO req = new ActionPageReqVO(); req.setOntologyId(ontologyId);
            req.setName(name); req.setActionType(actionType); req.setPageNum(1);
            req.setPageSize(input == null || input.getLimit() == null ? 100 : Math.max(1, Math.min(input.getLimit(), 100)));
            if (actionId != null && !actionId.trim().isEmpty()) req.setId(AgentToolIds.longValue(actionId));
            result = actionService.getActionPage(req);
        } else {
            List<ActionRespVO> actions = actionService.getActionsByOntologyId(ontologyId);
            result = actions;
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "已查询本体动作",
                context == null ? null : context.getOperationId());
    }

}
