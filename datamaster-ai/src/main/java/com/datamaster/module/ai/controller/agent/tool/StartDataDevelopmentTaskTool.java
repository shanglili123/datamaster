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
import java.util.Map;

/** 启动已确认的数据开发任务。任务发布、调度和 DolphinScheduler 执行由平台负责。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class StartDataDevelopmentTaskTool {

    @Resource
    private CollectorEtlTaskService taskService;

    @Tool(name = "start_data_development_task", value = "启动当前空间的数据开发任务；会触发任务发布/调度，必须在用户明确确认后调用")
    public AgentToolResult execute(AgentToolRequest.StartDataDevelopmentTask input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        try {
            Long id = AgentToolIds.longValue(input == null ? null : input.getTaskId());
            if (id == null) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "启动数据开发任务需要真实 taskId", operationId(context));
            }
            Map<String, Object> result = taskService.startVisibleTask(
                    id, context.getSpaceId(), context.getSpaceCode());
            boolean success = result != null && Boolean.TRUE.equals(result.get("success"));
            return AgentToolResult.of(success ? AgentToolResult.SUCCEEDED : AgentToolResult.FAILED,
                    result, success ? "数据开发任务已启动" : String.valueOf(result == null ? "启动失败" : result.get("message")),
                    operationId(context));
        } catch (ServiceException ex) {
            return AgentToolResult.failure(ex.getMessage());
        }
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
