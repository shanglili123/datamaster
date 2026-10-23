package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskSaveReqVO;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 发布元数据探查任务；真正执行仍由单独的 run_probe_task Tool 完成。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class PublishProbeTaskTool {

    @Resource
    private ICatalogTaskService catalogTaskService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "publish_probe_task", value = "发布当前流程绑定或指定的元数据探查任务，使其可以执行；生成动作时禁止调用")
    public AgentToolResult execute(AgentToolRequest.PublishProbeTask input) {
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
                Map<String, Object> skipped = new LinkedHashMap<>();
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
        if (!visibleInSpace(task, context)) return AgentToolResult.failure("探查任务不属于当前空间，拒绝发布");
        if (!resourceGuard.matchesCurrentDatasource(task.getDatasourceId(), context)) {
            return AgentToolResult.failure("探查任务所属数据源与当前会话目标不一致；当前目标为 "
                    + resourceGuard.currentDatasourceDescription(context) + "，已拒绝发布错误任务");
        }
        // 发布必须是幂等操作。历史任务已经是发布状态时，不能再次拿旧 taskCode
        // 强制更新 DolphinScheduler 定义；远端定义已变化或已清理时会导致本可直接
        // 执行的任务被误报为“更新任务定义失败”。一次性探查走 runDaDiscoveryTask，
        // 不依赖这里重复更新远端定义。
        if ("1".equals(task.getStatus())) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("taskId", taskId);
            data.put("catalogTaskId", taskId);
            data.put("datasourceId", task.getDatasourceId());
            data.put("status", task.getStatus());
            data.put("alreadyPublished", true);
            resourceGuard.bindCatalogTask(context, taskId);
            return AgentToolResult.of(AgentToolResult.SUCCEEDED, data,
                    "探查任务已处于发布状态", context.getOperationId());
        }
        CatalogTaskSaveReqVO request = new CatalogTaskSaveReqVO();
        request.setId(taskId);
        request.setStatus("1");
        Map<String, Object> data = catalogTaskService.updateReleaseJobTask(request);
        if (data == null) data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("catalogTaskId", taskId);
        data.put("datasourceId", task.getDatasourceId());
        data.put("status", "1");
        data.put("alreadyPublished", false);
        resourceGuard.bindCatalogTask(context, taskId);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "探查任务已发布", context.getOperationId());
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
