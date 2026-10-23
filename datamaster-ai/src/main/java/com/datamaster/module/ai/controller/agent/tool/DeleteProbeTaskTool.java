package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;

/** 删除当前空间的探查任务。 */
@Component
@AgentTool(risk = AgentToolRisk.DESTRUCTIVE, requiresConfirmation = true, confirmationArgument = "taskId")
public class DeleteProbeTaskTool {
    @Resource private ICatalogTaskService catalogTaskService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "delete_probe_task", value = "删除当前空间的元数据探查任务；不可逆，必须确认同一个 taskId；生成动作时不要调用")
    public AgentToolResult execute(AgentToolRequest.DeleteProbeTask input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = input == null ? null : AgentToolIds.longValue(input.getTaskId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "删除探查任务需要 taskId", operationId(context));
        CatalogTaskDO task = catalogTaskService.getCatalogTaskById(id);
        if (task == null) return AgentToolResult.failure("探查任务不存在: " + id);
        if (!visible(task, context)) return AgentToolResult.failure("探查任务不属于当前空间，拒绝删除");
        catalogTaskService.removeCatalogTask(Collections.singletonList(id));
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, Collections.singletonMap("taskId", id), "探查任务已删除", operationId(context));
    }
    private boolean visible(CatalogTaskDO task, AgentToolContext context) {
        return task != null && context != null && (context.getSpaceId() == null || context.getSpaceId().equals(task.getSpaceId()))
                && (context.getSpaceCode() == null || context.getSpaceCode().trim().isEmpty()
                || context.getSpaceCode().equalsIgnoreCase(task.getSpaceCode()));
    }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
