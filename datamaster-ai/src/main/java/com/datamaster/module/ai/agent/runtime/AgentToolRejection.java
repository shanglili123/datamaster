package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;

/** Only predefined business reasons may cross the tool error boundary. */
public final class AgentToolRejection extends RuntimeException {
    public enum Reason {
        PERMISSION_DENIED("当前账号没有调用此能力的权限"),
        RESOURCE_DENIED("当前账号无权访问所请求的资源"),
        QUERY_TIMEOUT("查询超时，请缩小查询范围后重试"),
        GOVERNANCE_DENIED("当前空间要求受控查询，不能使用未核验的探索问答"),
        SOURCE_UNAVAILABLE("当前数据源不可用，请检查数据源配置"),
        NO_REMOTE_RESULT("普通问答没有返回可用结果");

        private final String message;
        Reason(String message) { this.message = message; }
        public AgentToolResult result(String operationId) {
            AgentToolResult result = AgentToolResult.of(AgentToolResult.FAILED, null, message, operationId);
            result.getMetadata().put("publicReason", name());
            return result;
        }
    }

    private final Reason reason;

    public AgentToolRejection(Reason reason) {
        super(reason.message);
        this.reason = reason;
    }

    AgentToolResult result(String operationId) { return reason.result(operationId); }

    public static AgentToolResult classifiedFailure(Throwable failure, String operationId) {
        var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof AgentToolRejection rejected) return rejected.result(operationId);
            if (cause instanceof org.springframework.security.access.AccessDeniedException) return Reason.RESOURCE_DENIED.result(operationId);
            if (cause instanceof java.sql.SQLTimeoutException || cause instanceof java.net.SocketTimeoutException
                    || cause instanceof java.util.concurrent.TimeoutException) return Reason.QUERY_TIMEOUT.result(operationId);
            if (cause instanceof com.datamaster.common.exception.ServiceException service
                    && java.util.Objects.equals(service.getCode(), 403)) return Reason.RESOURCE_DENIED.result(operationId);
        }
        return null;
    }

    static String publicFailure(AgentToolResult result, String fallback) {
        if (result == null) return fallback;
        Object reason = result.getMetadata().get("publicReason");
        for (Reason value : Reason.values()) if (value.name().equals(reason)) return value.message;
        Object id = result.getMetadata().get("diagnosticId");
        if (id instanceof String diagnostic && diagnostic.matches("[0-9a-f-]{36}")) return fallback + "，诊断编号=" + diagnostic;
        return fallback;
    }
}
