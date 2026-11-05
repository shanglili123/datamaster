package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.runtime.AgentPayloadSanitizer;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.*;

import java.util.*;

/** Limited owner-visible execution projection. Never returns SQL, raw inputs or full data snapshots. */
final class DecisionExecutionDetails {
    private DecisionExecutionDetails() { }

    static Map<String, Object> action(ExecutionRespVO execution) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", "ACTION"); result.put("executionId", id(execution.getId()));
        result.put("status", execution.getStatus()); result.put("executedAt", execution.getExecuteTime());
        result.put("attemptNo", execution.getAttemptNo()); result.put("result", output(execution.getResultContext()));
        return result;
    }

    static Map<String, Object> workflow(ActionWorkflowRunRespVO run) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", "WORKFLOW"); result.put("runId", id(run.getId())); result.put("status", run.getStatus());
        result.put("currentNodeKey", run.getCurrentNodeKey()); result.put("finishedAt", run.getFinishedTime());
        List<Map<String, Object>> steps = new ArrayList<>();
        if (run.getSteps() != null) for (ActionWorkflowStepRespVO step : run.getSteps()) {
            if (steps.size() >= 20) break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stepId", id(step.getId())); item.put("nodeKey", step.getNodeKey());
            item.put("status", step.getStatus()); item.put("executionId", id(step.getExecutionId()));
            item.put("attemptNo", step.getAttemptNo()); item.put("finishedAt", step.getFinishedTime());
            item.put("output", output(step.getOutputJson())); steps.add(item);
        }
        result.put("steps", steps); result.put("stepsTruncated", run.getSteps() != null && run.getSteps().size() > 20);
        return result;
    }

    static Object output(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        if (json.length() > 12000) return "结果超过展示上限，请在原执行记录中查看";
        try { return bounded(JSON.parse(json), 0); }
        catch (RuntimeException invalid) { return "非结构化结果请在原执行记录中查看"; }
    }

    private static Object bounded(Object value, int depth) {
        if (depth > 6) return "[深度已限制]";
        if (value instanceof Map<?, ?>) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = String.valueOf(entry.getKey());
                String normalized = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
                if (normalized.contains("sql") || normalized.contains("script") || normalized.contains("functionbody")
                        || normalized.contains("sourcesnapshot") || normalized.contains("targettable")
                        || normalized.contains("datasource") || normalized.contains("inputparam")
                        || normalized.contains("beforedata") || normalized.contains("afterdata")) continue;
                if (result.size() >= 20) { result.put("_truncated", true); break; }
                Object safe = AgentPayloadSanitizer.sanitizeValue(key, entry.getValue());
                result.put(key, bounded(safe, depth + 1));
            }
            return result;
        }
        if (value instanceof Collection<?>) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                if (result.size() >= 10) { result.add("[其余结果已省略]"); break; }
                result.add(bounded(item, depth + 1));
            }
            return result;
        }
        if (value instanceof String) {
            String text = (String) value;
            // Embedded JSON strings receive the same filtering as structured output.
            if (text.trim().startsWith("{") || text.trim().startsWith("[")) {
                try { return bounded(JSON.parse(text), depth + 1); }
                catch (RuntimeException ignored) { return "[非结构化内容已省略]"; }
            }
            String safe = String.valueOf(AgentPayloadSanitizer.sanitizeValue(null, text));
            return safe.length() <= 500 ? safe : safe.substring(0, 500) + "[已截断]";
        }
        return value;
    }
    private static String id(Long value) { return value == null ? null : String.valueOf(value); }
}
