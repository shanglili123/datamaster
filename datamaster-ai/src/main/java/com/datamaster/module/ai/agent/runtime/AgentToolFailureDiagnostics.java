package com.datamaster.module.ai.agent.runtime;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

/** Diagnostic metadata for a failed tool; never retains raw arguments, model text or exception messages. */
public final class AgentToolFailureDiagnostics {
    private AgentToolFailureDiagnostics() { }

    public static final class Diagnostic {
        private final String id = UUID.randomUUID().toString();
        private final String category;
        private final String exceptionType;
        private final String reason;
        private final Throwable stack;

        private Diagnostic(Throwable failure) {
            Throwable cause = failure;
            Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            while (cause.getCause() != null && seen.add(cause) && !seen.contains(cause.getCause())) cause = cause.getCause();
            exceptionType = cause.getClass().getName();
            category = category(cause);
            reason = safeReason(cause);
            stack = sanitized(failure, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        }

        public String getId() { return id; }
        public String getCategory() { return category; }
        public String getExceptionType() { return exceptionType; }
        public String getReason() { return reason; }
        public Throwable getStack() { return stack; }
    }

    public static Diagnostic capture(Throwable failure) { return new Diagnostic(failure); }

    private static String safeReason(Throwable cause) {
        if (!(cause instanceof com.datamaster.common.exception.ServiceException)) return "UNCLASSIFIED";
        String message = cause.getMessage();
        if (message == null) return "BUSINESS_REJECTED";
        switch (message) {
            case "请缩小分析范围": return "ANALYSIS_INPUT_OR_LIMIT";
            case "正式指标请求必须使用指标分析能力": return "METRIC_SOURCE_REQUIRED";
            case "当前用户或空间无权分析": return "IDENTITY_OR_SPACE_REJECTED";
            case "正式指标任务不能回退探索查询": return "EXPLORATORY_FALLBACK_REJECTED";
            case "分析来源必须为METRIC或EXPLORATORY": return "SOURCE_INVALID";
            case "请先取得分析证据": return "EVIDENCE_REQUIRED";
            case "没有成功查询证据": return "QUERY_EVIDENCE_MISSING";
            default: return "BUSINESS_REJECTED";
        }
    }

    private static String category(Throwable cause) {
        if (cause instanceof IllegalAccessException) return "TOOL_REFLECTION_ACCESS";
        if (cause instanceof IllegalArgumentException) return "TOOL_ARGUMENT_INVALID";
        if (cause instanceof com.datamaster.common.exception.ServiceException) return "TOOL_BUSINESS_REJECTED";
        if (cause instanceof NullPointerException) return "TOOL_NULL_STATE";
        return "TOOL_INTERNAL_ERROR";
    }

    private static Throwable sanitized(Throwable original, Set<Throwable> visited, int depth) {
        Throwable copy = new Throwable(original.getClass().getName());
        copy.setStackTrace(original.getStackTrace());
        if (depth < 12 && visited.add(original) && original.getCause() != null && !visited.contains(original.getCause())) {
            copy.initCause(sanitized(original.getCause(), visited, depth + 1));
        }
        return copy;
    }
}
