package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskInstanceDO;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.metadata.service.task.ICatalogTaskInstanceService;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 查询元数据探查任务及其范围配置摘要。 */
@Component
@AgentTool
public class GetProbeTaskTool {

    @Resource
    private ICatalogTaskService catalogTaskService;
    @Resource
    private ICatalogTaskInstanceService catalogTaskInstanceService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "get_probe_task", value = "查询元数据探查任务配置及最近一次实际执行结果；taskId 可省略并从当前流程复用；用于判断元数据是否可用，不会发布或执行任务")
    public AgentToolResult execute(AgentToolRequest.GetProbeTask input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "请先确定当前空间", context == null ? null : context.getOperationId());
        Long taskId = input == null ? null : AgentToolIds.longValue(input.getTaskId());
        if (taskId == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            taskId = operation == null ? null : operation.getCatalogTaskId();
        }
        if (taskId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 taskId，且当前流程没有绑定探查任务",
                context == null ? null : context.getOperationId());
        CatalogTaskDO task = catalogTaskService.getCatalogTaskById(taskId);
        if (task == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            if (operation != null && operation.getOntologyId() != null) {
                Map<String, Object> skipped = new LinkedHashMap<>();
                skipped.put("skipped", true);
                skipped.put("ontologyId", operation.getOntologyId());
                skipped.put("staleTaskId", taskId);
                return AgentToolResult.of(AgentToolResult.SUCCEEDED, skipped,
                        "当前流程已有 ontologyId=" + operation.getOntologyId()
                                + "，本轮是基于本体生成动作，不需要查询历史探查任务；请调用 generate_actions",
                        context.getOperationId());
            }
            return AgentToolResult.failure("探查任务不存在: " + taskId);
        }
        if (!visibleInSpace(task, context)) return AgentToolResult.failure("探查任务不属于当前空间，拒绝查询");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", task.getId());
        data.put("name", task.getName());
        data.put("datasourceId", task.getDatasourceId());
        data.put("dbType", task.getDbType());
        data.put("collectionMode", task.getCollectionMode());
        data.put("collectionScope", task.getCollectionScope());
        data.put("collectType", task.getCollectType());
        data.put("status", task.getStatus());
        data.put("spaceId", task.getSpaceId());
        data.put("spaceCode", task.getSpaceCode());
        // 任务配置状态不能代表探查是否实际成功。把最近一次执行结果一并
        // 暴露给 Agent，让它基于事实判断是否需要再次探查，而不是看到任务
        // 存在就机械地发布/运行。
        CatalogTaskInstanceDO latest = catalogTaskInstanceService.getCatalogTaskInstanceByTaskId(taskId);
        if (latest == null) {
            data.put("hasExecution", false);
            data.put("probeReadiness", "NO_EXECUTION");
        } else {
            data.put("hasExecution", true);
            data.put("lastExecutionId", latest.getId());
            data.put("lastExecutionStatusCode", latest.getStatus());
            data.put("lastExecutionStatus", executionStatus(latest.getStatus()));
            data.put("lastExecutionSucceeded", "9".equals(latest.getStatus()));
            data.put("lastExecutionStartTime", latest.getStartTime());
            data.put("lastExecutionEndTime", latest.getEndTime());
            data.put("lastExecutionTotalTables", latest.getTotalCount());
            data.put("lastExecutionSuccessTables", latest.getSuccessCount());
            data.put("lastExecutionFailedTables", latest.getFailCount());
            data.put("lastExecutionFailCause", latest.getFailCause());
            data.put("probeReadiness", probeReadiness(latest.getStatus()));
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "已找到探查任务", context.getOperationId());
    }

    private String executionStatus(String status) {
        if ("9".equals(status)) return "SUCCEEDED";
        if ("2".equals(status)) return "FAILED";
        if ("1".equals(status)) return "RUNNING";
        return status == null ? "UNKNOWN" : "OTHER";
    }

    private String probeReadiness(String status) {
        if ("9".equals(status)) return "METADATA_AVAILABLE";
        if ("1".equals(status)) return "PROBE_IN_PROGRESS";
        if ("2".equals(status)) return "REQUIRES_RETRY";
        return "UNKNOWN";
    }


    private boolean visibleInSpace(CatalogTaskDO task, AgentToolContext context) {
        if (task == null || context == null
                || (context.getSpaceId() == null && blank(context.getSpaceCode()))) return false;
        return (context.getSpaceId() == null || context.getSpaceId().equals(task.getSpaceId()))
                && (blank(context.getSpaceCode()) || context.getSpaceCode().equalsIgnoreCase(task.getSpaceCode()));
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
