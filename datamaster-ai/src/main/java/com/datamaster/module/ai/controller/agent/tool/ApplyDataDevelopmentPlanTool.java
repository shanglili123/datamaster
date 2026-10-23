package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** 将已预览通过的计划保存为草稿；必须确认，不发布、不启动。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true, confirmationArgument = "taskId")
public class ApplyDataDevelopmentPlanTool {

    @Resource
    private CollectorEtlTaskService taskService;

    @Resource
    private PreviewDataDevelopmentPlanTool previewTool;

    @Tool(name = "apply_data_development_plan", value = "应用最近一次预览通过的数据开发计划到当前任务草稿；必须用户确认，只保存草稿，不发布、不启动")
    public AgentToolResult execute(AgentToolRequest.ApplyDataDevelopmentPlan input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        if (input == null || input.getTaskId() == null || input.getPlanJson() == null
                || input.getPreviewHash() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "应用计划需要 taskId、planJson 和 previewHash", operationId(context));
        }
        if (!sha256(input.getPlanJson()).equalsIgnoreCase(input.getPreviewHash().trim())) {
            return AgentToolResult.failure("planJson 与最近一次预览的 previewHash 不一致，请重新预览");
        }
        AgentToolRequest.PreviewDataDevelopmentPlan previewRequest = new AgentToolRequest.PreviewDataDevelopmentPlan();
        previewRequest.setTaskId(input.getTaskId());
        previewRequest.setPlanJson(input.getPlanJson());
        AgentToolResult preview = previewTool.execute(previewRequest);
        if (preview == null || !AgentToolResult.SUCCEEDED.equals(preview.getStatus())) {
            return preview == null ? AgentToolResult.failure("应用前计划校验失败") : preview;
        }
        try {
            Long taskId = AgentToolIds.longValue(input.getTaskId());
            Map<String, Object> result = taskService.applyVisibleTaskPlan(
                    taskId, context.getSpaceId(), context.getSpaceCode(), input.getPlanJson());
            return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                    "数据开发计划已保存为草稿，尚未发布或启动", operationId(context));
        } catch (ServiceException ex) {
            return AgentToolResult.failure(ex.getMessage());
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
