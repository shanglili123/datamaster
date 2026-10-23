package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskNodeRelRespDTO;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;

/** 查询当前空间单个数据开发任务。 */
@Component
@AgentTool
public class GetDataDevelopmentTaskTool {

    @Resource
    private CollectorEtlTaskService taskService;

    @Tool(name = "get_data_development_task", value = "按真实任务 ID 或名称查询当前空间的数据开发任务详情；不会返回连接密码或节点脚本")
    public AgentToolResult execute(AgentToolRequest.GetDataDevelopmentTask input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        AgentToolRequest.GetDataDevelopmentTask request = input == null
                ? new AgentToolRequest.GetDataDevelopmentTask() : input;
        try {
            Long id = AgentToolIds.longValue(request.getTaskId());
            if (id != null) {
                CollectorEtlTaskRespDTO task = taskService.getVisibleTask(
                        id, context.getSpaceId(), context.getSpaceCode());
                if (task == null) return AgentToolResult.failure("数据开发任务不存在或不属于当前空间");
                return AgentToolResult.of(AgentToolResult.SUCCEEDED,
                        detail(task, context), "已找到数据开发任务", operationId(context));
            }
            String name = request.getName() == null ? null : request.getName().trim();
            if (name == null || name.isEmpty()) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "请提供真实 taskId 或数据开发任务名称", operationId(context));
            }
            List<CollectorEtlTaskRespDTO> candidates = taskService.listVisibleTasks(
                    context.getSpaceId(), context.getSpaceCode(), name, null, 20);
            if (candidates == null || candidates.isEmpty()) {
                return AgentToolResult.failure("当前空间未找到数据开发任务: " + name);
            }
            if (candidates.size() > 1) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (CollectorEtlTaskRespDTO candidate : candidates) {
                    rows.add(ListDataDevelopmentTasksTool.summary(candidate));
                }
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, rows,
                        "数据开发任务名称匹配多条，请选择真实 taskId", operationId(context));
            }
            return AgentToolResult.of(AgentToolResult.SUCCEEDED,
                    detail(candidates.get(0), context), "已找到数据开发任务", operationId(context));
        } catch (ServiceException ex) {
            return AgentToolResult.failure(ex.getMessage());
        }
    }

    private Map<String, Object> detail(CollectorEtlTaskRespDTO task, AgentToolContext context) {
        Map<String, Object> result = ListDataDevelopmentTasksTool.summary(task);
        Long taskId = task.getId();
        List<CollectorEtlNodeRespDTO> nodes = taskService.listVisibleTaskNodes(
                taskId, context.getSpaceId(), context.getSpaceCode());
        List<CollectorEtlTaskNodeRelRespDTO> relations = taskService.listVisibleTaskRelations(
                taskId, context.getSpaceId(), context.getSpaceCode());
        List<Map<String, Object>> nodeSummaries = new ArrayList<>();
        if (nodes != null) for (CollectorEtlNodeRespDTO node : nodes) {
            if (node == null) continue;
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", id(node.getId()));
            item.put("code", node.getCode());
            item.put("name", node.getName());
            item.put("type", node.getType());
            item.put("componentType", node.getComponentType());
            item.put("version", node.getVersion());
            item.put("validFlag", node.getValidFlag());
            item.put("parameterSummary", parameterSummary(node.getParameters()));
            nodeSummaries.add(item);
        }
        List<Map<String, Object>> relationSummaries = new ArrayList<>();
        if (relations != null) for (CollectorEtlTaskNodeRelRespDTO relation : relations) {
            if (relation == null) continue;
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("preNodeId", id(relation.getPreNodeId()));
            item.put("preNodeCode", relation.getPreNodeCode());
            item.put("postNodeId", id(relation.getPostNodeId()));
            item.put("postNodeCode", relation.getPostNodeCode());
            item.put("taskVersion", relation.getTaskVersion());
            relationSummaries.add(item);
        }
        result.put("nodes", nodeSummaries);
        result.put("relations", relationSummaries);
        result.put("nodeCount", nodeSummaries.size());
        result.put("relationCount", relationSummaries.size());
        return result;
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> parameterSummary(String raw) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            result.put("present", false);
            result.put("keys", new ArrayList<>());
            return result;
        }
        result.put("present", true);
        result.put("length", raw.length());
        Set<String> keys = new LinkedHashSet<>();
        Set<String> sensitive = new LinkedHashSet<>();
        try {
            Object value = JSON.parse(raw);
            collectKeys(value, keys, sensitive);
        } catch (Exception ignored) {
            result.put("format", "opaque");
        }
        result.put("keys", new ArrayList<>(keys));
        result.put("sensitiveFields", new ArrayList<>(sensitive));
        result.put("contentOmitted", true);
        return result;
    }

    private void collectKeys(Object value, Set<String> keys, Set<String> sensitive) {
        if (value instanceof JSONObject) {
            for (Map.Entry<String, Object> entry : ((JSONObject) value).entrySet()) {
                String key = entry.getKey();
                if (key != null) {
                    keys.add(key);
                    String normalized = key.replace("_", "").toLowerCase(Locale.ROOT);
                    if (normalized.contains("password") || normalized.contains("secret")
                            || normalized.contains("token") || normalized.contains("authorization")
                            || normalized.equals("sql") || normalized.contains("script")
                            || normalized.contains("header")) sensitive.add(key);
                }
                collectKeys(entry.getValue(), keys, sensitive);
            }
        } else if (value instanceof JSONArray) {
            for (Object item : (JSONArray) value) collectKeys(item, keys, sensitive);
        }
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
