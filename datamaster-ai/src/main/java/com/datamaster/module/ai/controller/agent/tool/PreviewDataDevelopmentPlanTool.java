package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeRespDTO;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

/** 只读预览数据开发计划；只解析和校验，不写库、不发布、不执行。 */
@Component
@AgentTool
public class PreviewDataDevelopmentPlanTool {

    private static final Pattern NODE_CODE = Pattern.compile("[A-Za-z0-9_.-]{1,128}");
    private static final Set<String> ALLOWED_TYPES = allowedTypes();
    private static final Set<String> FORBIDDEN_KEYS = new HashSet<>(Arrays.asList(
            "password", "passwd", "secret", "token", "authorization", "accesskey",
            "secretkey", "jdbcurl", "connectionstring", "privatekey"));

    @Resource
    private CollectorEtlTaskService taskService;

    @Tool(name = "preview_data_development_plan", value = "预览并校验数据开发任务计划；只检查节点、依赖、类型和敏感参数，不会保存、发布或执行任务")
    public AgentToolResult execute(AgentToolRequest.PreviewDataDevelopmentPlan input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        if (input == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "请提供 taskId 和 planJson", operationId(context));
        }
        try {
            Long taskId = AgentToolIds.longValue(input.getTaskId());
            if (taskId == null) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "预览数据开发计划需要真实 taskId", operationId(context));
            }
            CollectorEtlTaskRespDTO task = taskService.getVisibleTask(
                    taskId, context.getSpaceId(), context.getSpaceCode());
            if (task == null) return AgentToolResult.failure("数据开发任务不存在或不属于当前空间");
            JSONObject plan = JSON.parseObject(input.getPlanJson());
            if (plan == null) return AgentToolResult.failure("planJson 必须是 JSON 对象");
            return validate(task, taskId, plan, input.getPlanJson(), context);
        } catch (ServiceException ex) {
            return AgentToolResult.failure(ex.getMessage());
        } catch (Exception ex) {
            return AgentToolResult.failure("planJson 解析失败：" + safeMessage(ex));
        }
    }

    private AgentToolResult validate(CollectorEtlTaskRespDTO task, Long taskId,
                                     JSONObject plan, String rawPlan, AgentToolContext context) {
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, JSONObject> byCode = new LinkedHashMap<>();
        JSONArray nodeArray = plan.getJSONArray("nodes");
        if (nodeArray == null) nodeArray = plan.getJSONArray("taskDefinitionList");
        if (nodeArray == null || nodeArray.isEmpty()) {
            errors.add("nodes 必须是非空数组");
        } else {
            for (int i = 0; i < nodeArray.size(); i++) {
                Object raw = nodeArray.get(i);
                if (!(raw instanceof JSONObject)) {
                    errors.add("nodes[" + i + "] 必须是对象");
                    continue;
                }
                JSONObject node = (JSONObject) raw;
                String code = text(node, "code", "nodeCode", "id");
                String name = text(node, "name", "label");
                String type = text(node, "type", "componentType", "taskType");
                if (blank(code)) errors.add("nodes[" + i + "] 缺少 code");
                else if (!NODE_CODE.matcher(code).matches()) errors.add("节点编码非法: " + code);
                else if (byCode.put(code, node) != null) errors.add("节点编码重复: " + code);
                if (blank(name)) errors.add("节点 " + (code == null ? i : code) + " 缺少 name");
                if (blank(type)) errors.add("节点 " + (code == null ? i : code) + " 缺少 type");
                else if (!ALLOWED_TYPES.contains(type.toUpperCase(Locale.ROOT))) {
                    errors.add("节点 " + code + " 类型不受支持: " + type);
                }
                Object parameters = node.get("parameters");
                if (parameters == null) parameters = node.get("params");
                if (containsForbiddenKey(parameters)) {
                    errors.add("节点 " + code + " 的参数包含密码、令牌或连接密钥；请使用平台已配置的数据源引用");
                }
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("code", code);
                summary.put("name", name);
                summary.put("type", type);
                summary.put("parameterPresent", parameters != null);
                summary.put("parameterContentOmitted", parameters != null);
                nodes.add(summary);
            }
        }

        Map<String, Set<String>> edges = new LinkedHashMap<>();
        for (String code : byCode.keySet()) edges.put(code, new LinkedHashSet<>());
        JSONArray relationArray = plan.getJSONArray("relations");
        if (relationArray != null) {
            for (int i = 0; i < relationArray.size(); i++) {
                Object raw = relationArray.get(i);
                if (!(raw instanceof JSONObject)) {
                    errors.add("relations[" + i + "] 必须是对象");
                    continue;
                }
                JSONObject relation = (JSONObject) raw;
                String from = text(relation, "from", "preNodeCode", "pre", "source");
                String to = text(relation, "to", "postNodeCode", "post", "target");
                addEdge(edges, from, to, errors, "relations[" + i + "]");
            }
        }
        for (Map.Entry<String, JSONObject> entry : byCode.entrySet()) {
            Object preTasks = entry.getValue().get("preTasks");
            if (preTasks instanceof JSONArray) {
                for (Object pre : (JSONArray) preTasks) {
                    addEdge(edges, String.valueOf(pre), entry.getKey(), errors,
                            "nodes[" + entry.getKey() + "].preTasks");
                }
            }
        }
        List<String> cycle = cycleNodes(edges);
        if (!cycle.isEmpty()) errors.add("节点依赖存在环: " + String.join(" -> ", cycle));

        List<Map<String, Object>> relationSummary = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
            for (String to : entry.getValue()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("from", entry.getKey());
                item.put("to", to);
                relationSummary.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", String.valueOf(taskId));
        result.put("taskName", task.getName());
        result.put("taskStatus", task.getStatus());
        result.put("readOnly", true);
        result.put("willWrite", false);
        result.put("planHash", sha256(rawPlan));
        result.put("nodes", nodes);
        result.put("relations", relationSummary);
        result.put("nodeCount", nodes.size());
        result.put("relationCount", relationSummary.size());
        result.put("errors", errors);
        addChangeSummary(result, taskId, context, byCode.keySet());
        if ("1".equals(task.getStatus())) result.put("publishedTask", true);
        return AgentToolResult.of(errors.isEmpty() ? AgentToolResult.SUCCEEDED : AgentToolResult.FAILED,
                result, errors.isEmpty() ? "数据开发计划校验通过；尚未写入任务" : "数据开发计划校验未通过",
                operationId(context));
    }

    private void addChangeSummary(Map<String, Object> result, Long taskId,
                                  AgentToolContext context, Set<String> plannedCodes) {
        List<CollectorEtlNodeRespDTO> existing = taskService.listVisibleTaskNodes(
                taskId, context.getSpaceId(), context.getSpaceCode());
        Set<String> existingCodes = new LinkedHashSet<>();
        if (existing != null) for (CollectorEtlNodeRespDTO node : existing) {
            if (node != null && node.getCode() != null) existingCodes.add(node.getCode());
        }
        Set<String> added = new LinkedHashSet<>(plannedCodes);
        added.removeAll(existingCodes);
        Set<String> removed = new LinkedHashSet<>(existingCodes);
        removed.removeAll(plannedCodes);
        result.put("existingNodeCount", existingCodes.size());
        result.put("addedNodeCodes", added);
        result.put("removedNodeCodes", removed);
        result.put("potentiallyChangedNodeCodes", intersection(plannedCodes, existingCodes));
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private void addEdge(Map<String, Set<String>> edges, String from, String to,
                         List<String> errors, String location) {
        if (blank(from) || blank(to)) {
            errors.add(location + " 必须同时提供前置和后置节点");
            return;
        }
        if (!edges.containsKey(from)) errors.add(location + " 引用了不存在的前置节点: " + from);
        if (!edges.containsKey(to)) errors.add(location + " 引用了不存在的后置节点: " + to);
        if (edges.containsKey(from) && edges.containsKey(to)) edges.get(from).add(to);
    }

    private List<String> cycleNodes(Map<String, Set<String>> edges) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        for (String node : edges.keySet()) indegree.put(node, 0);
        for (Set<String> next : edges.values()) for (String target : next) indegree.put(target, indegree.get(target) + 1);
        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) if (entry.getValue() == 0) queue.add(entry.getKey());
        int visited = 0;
        while (!queue.isEmpty()) {
            String node = queue.remove();
            visited++;
            for (String target : edges.get(node)) if (indegree.put(target, indegree.get(target) - 1) == 1) queue.add(target);
        }
        if (visited == edges.size()) return Collections.emptyList();
        List<String> cycle = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) if (entry.getValue() > 0) cycle.add(entry.getKey());
        return cycle;
    }

    private boolean containsForbiddenKey(Object value) {
        if (value instanceof JSONObject) {
            for (Map.Entry<String, Object> entry : ((JSONObject) value).entrySet()) {
                String normalized = entry.getKey() == null ? "" : entry.getKey().replace("_", "").toLowerCase(Locale.ROOT);
                if (FORBIDDEN_KEYS.contains(normalized) || normalized.contains("password")
                        || normalized.contains("secret") || normalized.contains("token")) return true;
                if (containsForbiddenKey(entry.getValue())) return true;
            }
        } else if (value instanceof JSONArray) {
            for (Object item : (JSONArray) value) if (containsForbiddenKey(item)) return true;
        } else if (value instanceof String) {
            String text = ((String) value).toLowerCase(Locale.ROOT);
            return text.contains("password=") || text.contains("jdbc:") && text.contains("@")
                    || text.contains("authorization:");
        }
        return false;
    }

    private String text(JSONObject object, String... keys) {
        for (String key : keys) {
            Object value = object.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) return String.valueOf(value).trim();
        }
        return null;
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    private static Set<String> allowedTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (TaskComponentTypeEnum item : TaskComponentTypeEnum.values()) {
            types.add(item.getCode().toUpperCase(Locale.ROOT));
            types.add(item.name().toUpperCase(Locale.ROOT));
        }
        types.addAll(Arrays.asList("SQL", "PROCEDURE", "SHELL", "PYTHON", "CHUNJUN", "HTTP", "CONDITIONS", "SUB_PROCESS"));
        return types;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }

    private String safeMessage(Exception ex) { return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(); }
}
