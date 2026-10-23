package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionSubmitReqVO;
import com.datamaster.module.ontology.service.IActionExecutionService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 提交本体动作执行请求。底层负责生成 SQL、dry-run、条件检查和审批判定；本 Tool 不直接拼接或执行 SQL。
 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class SubmitActionTool {

    @Resource
    private IActionExecutionService actionExecutionService;
    @Resource
    private AgentToolResourceGuard resourceGuard;

    /** 提交本身只生成执行记录和预览，真正执行由动作配置和审批引擎决定。 */
    @Tool(name = "submit_action", value = "提交一个已经存在的动作执行请求并生成执行预览/执行记录；必须提供 actionId；不等于直接绕过审批执行数据库更新；不能用于生成动作定义或动作草稿，生成动作请使用 generate_actions")
    public AgentToolResult execute(AgentToolRequest.SubmitAction input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long actionId = AgentToolIds.longValue(input.getActionId());
        if (actionId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "submit_action 仅用于执行已有动作且必须提供 actionId；如果目标是生成动作，请改用 generate_actions",
                context == null ? null : context.getOperationId());
        if (!resourceGuard.actionVisible(actionId, context)) {
            return AgentToolResult.failure("动作不属于当前空间，拒绝提交");
        }
        ExecutionSubmitReqVO request = new ExecutionSubmitReqVO();
        request.setActionId(actionId);
        request.setInputParams(text(input.getInputParams()));
        request.setObjectKey(text(input.getObjectKey()));
        request.setIdempotencyKey(text(input.getIdempotencyKey()));
        if (request.getIdempotencyKey() == null) request.setIdempotencyKey("agent:" + UUID.randomUUID());
        request.setTriggerType(text(input.getTriggerType()));
        request.setTriggerRef(text(input.getTriggerRef()));
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        ExecutionRespVO response = actionExecutionService.submitExecution(request);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response,
                "动作已提交并完成执行预览；如状态为待审批需先审批", context.getOperationId());
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim();
    }

}
