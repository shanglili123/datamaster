package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
/** 执行一次元数据探查任务，底层复用 Redis 并发锁和任务实例日志。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class RunProbeTaskTool {

    @Resource
    private ICatalogTaskService catalogTaskService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "run_probe_task", value = "执行当前流程绑定或指定的已发布元数据探查任务；仅用于生成/刷新本体，不用于生成动作")
    public AgentToolResult execute(AgentToolRequest.RunProbeTask input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "请先确定当前空间", context == null ? null : context.getOperationId());
        Long taskId = input == null ? null : AgentToolIds.longValue(input.getTaskId());
        if (taskId == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            taskId = operation == null ? null : operation.getCatalogTaskId();
        }
        if (taskId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 taskId", context.getOperationId());
        com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO task = catalogTaskService.getCatalogTaskById(taskId);
        if (task == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            if (operation != null && operation.getOntologyId() != null) {
                java.util.Map<String, Object> skipped = new java.util.LinkedHashMap<>();
                skipped.put("skipped", true);
                skipped.put("ontologyId", operation.getOntologyId());
                skipped.put("staleTaskId", taskId);
                return AgentToolResult.of(AgentToolResult.SUCCEEDED, skipped,
                        "当前流程已有 ontologyId=" + operation.getOntologyId()
                                + "，本轮是基于本体生成动作，已跳过不存在的历史探查任务；请调用 generate_actions",
                        context.getOperationId());
            }
            return AgentToolResult.failure("探查任务不存在: " + taskId);
        }
        if (!visibleInSpace(task, context)) return AgentToolResult.failure("探查任务不属于当前空间，拒绝执行");
        if (!resourceGuard.matchesCurrentDatasource(task.getDatasourceId(), context)) {
            return AgentToolResult.failure("探查任务所属数据源与当前会话目标不一致；当前目标为 "
                    + resourceGuard.currentDatasourceDescription(context) + "，已拒绝执行错误任务");
        }
        boolean success = catalogTaskService.runDaDiscoveryTask(taskId);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("taskId", taskId); data.put("catalogTaskId", taskId);
        data.put("datasourceId", task.getDatasourceId());
        resourceGuard.bindCatalogTask(context, taskId);
        return AgentToolResult.of(success ? AgentToolResult.SUCCEEDED : AgentToolResult.FAILED,
                data, success ? "探查任务执行完成" : "探查任务执行失败",
                context.getOperationId());
    }


    private boolean visibleInSpace(com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO task,
                                    AgentToolContext context) {
        if (task == null || context == null
                || (context.getSpaceId() == null && blank(context.getSpaceCode()))) return false;
        return (context.getSpaceId() == null || context.getSpaceId().equals(task.getSpaceId()))
                && (blank(context.getSpaceCode()) || context.getSpaceCode().equalsIgnoreCase(task.getSpaceCode()));
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
