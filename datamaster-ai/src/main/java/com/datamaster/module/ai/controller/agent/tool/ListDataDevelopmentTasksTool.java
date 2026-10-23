package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 查询当前空间的数据开发任务目录。 */
@Component
@AgentTool
public class ListDataDevelopmentTasksTool {

    @Resource
    private CollectorEtlTaskService taskService;

    @Tool(name = "list_data_development_tasks", value = "查询当前空间的数据开发任务，可按名称和状态筛选；只返回任务摘要，不返回连接密码或节点脚本")
    public AgentToolResult execute(AgentToolRequest.ListDataDevelopmentTasks input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        AgentToolRequest.ListDataDevelopmentTasks request = input == null
                ? new AgentToolRequest.ListDataDevelopmentTasks() : input;
        int limit = request.getLimit() == null ? 20 : Math.max(1, Math.min(request.getLimit(), 100));
        List<CollectorEtlTaskRespDTO> tasks = taskService.listVisibleTasks(
                context.getSpaceId(), context.getSpaceCode(), request.getName(), request.getStatus(), limit);
        List<Map<String, Object>> result = new ArrayList<>();
        if (tasks != null) for (CollectorEtlTaskRespDTO task : tasks) {
            if (task != null) result.add(summary(task));
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                "已查询当前空间数据开发任务", operationId(context));
    }

    static Map<String, Object> summary(CollectorEtlTaskRespDTO task) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", task.getId() == null ? null : String.valueOf(task.getId()));
        item.put("name", task.getName());
        item.put("code", task.getCode());
        item.put("type", task.getType());
        item.put("status", task.getStatus());
        item.put("executionType", task.getExecutionType());
        item.put("description", task.getDescription());
        item.put("version", task.getVersion());
        item.put("dsId", task.getDsId() == null ? null : String.valueOf(task.getDsId()));
        item.put("extractionCount", task.getExtractionCount());
        item.put("writeCount", task.getWriteCount());
        return item;
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
