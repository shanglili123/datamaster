package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.alibaba.fastjson2.JSON;

import java.util.Map;

import java.util.ArrayList;
import java.util.List;

/** 当前 Agent 调用的可信上下文，不进入 LangChain4j Tool 参数 Schema。 */
public final class AgentToolContextHolder {

    private static final ThreadLocal<AgentToolContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<List<AgentToolResult>> RESULTS = new ThreadLocal<>();
    private static final ThreadLocal<List<ToolCall>> CALLS = new ThreadLocal<>();

    private AgentToolContextHolder() {
    }

    public static void set(AgentToolContext context) {
        CONTEXT.set(context);
        RESULTS.set(new ArrayList<AgentToolResult>());
        CALLS.set(new ArrayList<ToolCall>());
    }

    public static AgentToolContext get() {
        return CONTEXT.get();
    }

    public static void record(AgentToolResult result) {
        List<AgentToolResult> results = RESULTS.get();
        if (results != null && result != null) results.add(result);
    }

    public static List<AgentToolResult> results() {
        List<AgentToolResult> results = RESULTS.get();
        return results == null ? new ArrayList<AgentToolResult>() : new ArrayList<>(results);
    }

    public static boolean hasSuccessfulCall(String toolName, Map<String, Object> arguments) {
        List<ToolCall> calls = CALLS.get();
        if (calls == null) return false;
        String current = JSON.toJSONString(arguments == null ? new java.util.LinkedHashMap<String, Object>() : arguments);
        for (ToolCall call : calls) {
            if (call.result != null && AgentToolResult.SUCCEEDED.equals(call.result.getStatus())
                    && toolName.equals(call.toolName) && current.equals(call.argumentsJson)) return true;
        }
        return false;
    }

    public static void recordCall(String toolName, Map<String, Object> arguments, AgentToolResult result) {
        List<ToolCall> calls = CALLS.get();
        if (calls != null) calls.add(new ToolCall(toolName,
                JSON.toJSONString(arguments == null ? new java.util.LinkedHashMap<String, Object>() : arguments), result));
    }

    public static AgentToolResult lastResult() {
        List<AgentToolResult> results = RESULTS.get();
        return results == null || results.isEmpty() ? null : results.get(results.size() - 1);
    }

    public static void clear() {
        CONTEXT.remove();
        RESULTS.remove();
        CALLS.remove();
    }

    private static final class ToolCall {
        private final String toolName;
        private final String argumentsJson;
        private final AgentToolResult result;
        private ToolCall(String toolName, String argumentsJson, AgentToolResult result) {
            this.toolName = toolName;
            this.argumentsJson = argumentsJson;
            this.result = result;
        }
    }
}
