package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ai.controller.agent.PlatformAgentOperationReqVO;
import com.datamaster.module.ai.controller.agent.PlatformAgentOperationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把受控数据接入流程包装成 Tool。流程服务会根据当前数据源、目录元数据和
 * 探查实例状态复用已完成步骤，不会因为进入标准接入就机械重复探查。
 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class StandardDataOnboardingTool {

    @Resource
    private PlatformAgentOperationService operationService;
    @Resource
    private AgentToolResourceGuard resourceGuard;

    /** 用户明确提出完整接入目标时，按当前状态自适应推进各步骤。 */
    @Tool(name = "run_standard_data_onboarding", value = "按当前数据源和元数据实际状态自适应完成接入：复用已完成步骤，必要时才探查，并同步资产授权、生成本体、动作草稿和 Skill")
    public AgentToolResult execute(AgentToolRequest.StandardDataOnboarding input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "标准数据接入前请先确定当前空间", context == null ? null : context.getOperationId());
        }
        PlatformAgentOperationReqVO request = new PlatformAgentOperationReqVO();
        request.setGoal(stringValue(input.getGoal()));
        request.setDatasourceId(AgentToolIds.longValue(input.getDatasourceId()));
        request.setDatasourceName(stringValue(input.getDatasourceName()));
        request.setDatabaseName(stringValue(input.getDatabaseName()));
        request.setSchemaName(stringValue(input.getSchemaName()));
        request.setDatasourceType(stringValue(input.getDatasourceType()));
        request.setIp(stringValue(input.getIp()));
        request.setPort(input.getPort());
        // 空间只能来自受信任的调用上下文，不能由模型通过 Tool 参数指定。
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        // 自主 Agent 的一次操作可能多次调用标准接入 Tool；使用可信上下文
        // 隔离不同会话，避免同一数据源上的不同目标互相复用流程记录。
        request.setOperationKey(context.getOperationId());
        if (context.getAttributes() != null && context.getAttributes().get("conversationId") != null) {
            request.setConversationId(String.valueOf(context.getAttributes().get("conversationId")));
        }
        Object datasourceConfig = input.getDatasourceConfig();
        if (datasourceConfig instanceof String) {
            request.setDatasourceConfig((String) datasourceConfig);
        } else if (datasourceConfig != null) {
            request.setDatasourceConfig(JSON.toJSONString(datasourceConfig));
        }

        if (request.getGoal() == null || request.getGoal().trim().isEmpty()) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 goal，请说明要接入和处理的数据目标", context.getOperationId());
        }

        PlatformAgentOperationService.OperationState state = operationService.start(
                request, context.getUserId(), context.getUserName());
        String status = state == null || state.getStatus() == null
                ? AgentToolResult.FAILED : state.getStatus();
        boolean partial = "PARTIAL".equals(status);
        AgentToolResult result = AgentToolResult.of(partial ? AgentToolResult.SUCCEEDED : status, summarize(state),
                state == null ? "标准数据接入流程未返回状态" : state.getMessage(),
                state == null ? null : state.getId());
        if (partial) {
            result.getMetadata().put("partial", Boolean.TRUE);
            result.getMetadata().put("operationStatus", status);
            if (state.getResult() != null && state.getResult().get("warnings") != null) {
                result.getMetadata().put("warnings", state.getResult().get("warnings"));
            }
        }
        if (state != null && state.getConfirmationType() != null) {
            result.getMetadata().put("confirmationType", state.getConfirmationType());
            result.getMetadata().put("confirmationMessage", state.getConfirmationMessage());
        }
        return result;
    }

    private Map<String, Object> summarize(PlatformAgentOperationService.OperationState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (state == null) return data;
        data.put("id", state.getId());
        data.put("status", state.getStatus());
        data.put("progress", state.getProgress());
        data.put("currentStep", state.getCurrentStep());
        data.put("message", state.getMessage());
        data.put("datasourceId", state.getDatasourceId());
        data.put("datasourceName", state.getDatasourceName());
        data.put("databaseName", state.getDatabaseName());
        data.put("operationKey", state.getOperationKey());
        data.put("conversationId", state.getConversationId());
        data.put("flowKey", state.getFlowKey());
        data.put("catalogTaskId", state.getCatalogTaskId());
        data.put("ontologyId", state.getOntologyId());
        data.put("actionCount", state.getActionCount());
        data.put("workflowId", state.getWorkflowId());
        data.put("confirmationType", state.getConfirmationType());
        data.put("confirmationMessage", state.getConfirmationMessage());
        data.put("output", state.getOutput());
        return data;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
