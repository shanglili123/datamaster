package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowEdgeVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowNodeVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowSaveReqVO;
import com.datamaster.module.ontology.service.IActionWorkflowService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;

/** 根据动作顺序生成动作编排草稿。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class GenerateActionWorkflowTool {
    @Resource private IActionWorkflowService workflowService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "generate_action_workflow", value = "根据本体动作 ID 顺序生成一个线性动作编排草稿，只保存为草稿，不自动发布或执行")
    public AgentToolResult execute(AgentToolRequest.GenerateActionWorkflow input) {
        AgentToolContext context = AgentToolContextHolder.get();
        if (input == null || input.getOntologyId() == null || input.getName() == null
                || input.getActionIds() == null || input.getActionIds().isEmpty()) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "生成动作编排需要 ontologyId、name 和至少一个 actionId", operationId(context));
        }
        if (!resourceGuard.ontologyVisible(AgentToolIds.longValue(input.getOntologyId()), context)) {
            return AgentToolResult.failure("本体不属于当前空间，拒绝生成动作编排");
        }
        ActionWorkflowSaveReqVO req = new ActionWorkflowSaveReqVO();
        req.setOntologyId(AgentToolIds.longValue(input.getOntologyId()));
        req.setName(input.getName().trim());
        req.setCode(blankToNull(input.getCode()));
        req.setTriggerRef(blankToNull(input.getTriggerRef()));
        req.setDescription(blankToNull(input.getDescription()));
        req.setNodes(new ArrayList<>());
        req.setEdges(new ArrayList<>());
        String previous = null;
        int index = 0;
        for (Long actionId : AgentToolIds.longValues(input.getActionIds())) {
            if (actionId == null) continue;
            String key = "action-" + (++index);
            ActionWorkflowNodeVO node = new ActionWorkflowNodeVO();
            node.setNodeKey(key); node.setName("动作 " + actionId); node.setNodeType("ACTION"); node.setActionId(actionId);
            req.getNodes().add(node);
            if (previous != null) {
                ActionWorkflowEdgeVO edge = new ActionWorkflowEdgeVO();
                edge.setEdgeKey(previous + "-to-" + key); edge.setFromNodeKey(previous); edge.setToNodeKey(key); edge.setPriority(index);
                req.getEdges().add(edge);
            }
            previous = key;
        }
        Long workflowId = workflowService.createWorkflow(req);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, Collections.singletonMap("workflowId", workflowId),
                "动作编排草稿已生成，需校验并发布后才能执行", operationId(context));
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
    private String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
