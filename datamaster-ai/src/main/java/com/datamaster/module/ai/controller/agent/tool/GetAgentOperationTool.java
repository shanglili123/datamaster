package com.datamaster.module.ai.controller.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.controller.agent.PlatformAgentOperationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 查询标准流程或自主流程的持久化状态。 */
@Component
@AgentTool
public class GetAgentOperationTool {

    @Resource
    private PlatformAgentOperationService operationService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "get_agent_operation", value = "查询数据智能体流程主记录、当前步骤、进度、确认状态和结果")
    public AgentToolResult execute(AgentToolRequest.GetAgentOperation input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        String id = text(input.getOperationId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 operationId", context.getOperationId());
        if (!resourceGuard.hasSpace(context)
                || !operationService.belongsTo(id, context.getUserId(), context.getSpaceId(), context.getSpaceCode())) {
            return AgentToolResult.failure("流程不属于当前用户或空间，拒绝查询: " + id);
        }
        PlatformAgentOperationService.OperationState state = operationService.get(id);
        if (state == null) return AgentToolResult.failure("流程不存在: " + id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", state.getId());
        data.put("status", state.getStatus());
        data.put("progress", state.getProgress());
        data.put("currentStep", state.getCurrentStep());
        data.put("message", state.getMessage());
        data.put("datasourceId", state.getDatasourceId());
        data.put("datasourceName", state.getDatasourceName());
        data.put("databaseName", state.getDatabaseName());
        data.put("flowKey", state.getFlowKey());
        data.put("catalogTaskId", state.getCatalogTaskId());
        data.put("ontologyId", state.getOntologyId());
        data.put("actionCount", state.getActionCount());
        data.put("confirmationType", state.getConfirmationType());
        data.put("confirmationMessage", state.getConfirmationMessage());
        data.put("output", state.getOutput());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "已查询流程状态", context.getOperationId());
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim();
    }
}
