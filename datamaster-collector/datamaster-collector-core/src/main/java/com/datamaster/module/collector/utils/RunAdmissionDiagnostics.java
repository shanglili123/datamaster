package com.datamaster.module.collector.utils;

import com.datamaster.common.exception.ServiceException;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Per-call admission diagnostics. Never retains credentials, SQL, job JSON or raw exception messages. */
public final class RunAdmissionDiagnostics {
    private final String diagnosticId = UUID.randomUUID().toString();
    private final Long taskId;
    private Long schedulerInstanceId;
    private Long executionId;
    private String step = "REQUEST";
    private String observation = "";
    private boolean guardVisibilityPending;

    public RunAdmissionDiagnostics(Long taskId) { this.taskId = taskId; }

    public void workflow(Long expectedCode, Integer expectedVersion, Long actualCode, Integer actualVersion, String state) {
        observation = "expectedWorkflow=" + expectedCode + ":" + expectedVersion
                + ",actualWorkflow=" + actualCode + ":" + actualVersion + ",state=" + safeState(state);
    }

    public void guard(Long code, int nodeCount, String state) {
        observation += ",guardCode=" + code + ",nodeCount=" + nodeCount + ",guardState=" + safeState(state);
        guardVisibilityPending = "DISPATCH".equals(state) || "SUBMITTED_SUCCESS".equals(state) || "NOT_VISIBLE".equals(state);
    }

    private static String safeState(String value) {
        return value != null && value.matches("[A-Z_]{1,40}") ? value : "UNKNOWN";
    }

    public void step(String value) { step = value; }
    public void schedulerInstance(Long value) { schedulerInstanceId = value; }
    public void execution(Long value) { executionId = value; }

    public <T> T execute(Supplier<T> action) {
        try { return action.get(); }
        catch (Failure failure) { throw failure; }
        catch (RuntimeException exception) { throw new Failure(this, exception); }
    }

    public static Failure failure(Exception exception) {
        return exception instanceof Failure ? (Failure) exception
                : new Failure(new RunAdmissionDiagnostics(null), exception);
    }

    public static final class Failure extends RuntimeException {
        private final String diagnosticId;
        private final Long taskId;
        private final Long schedulerInstanceId;
        private final Long executionId;
        private final String step;
        private final boolean businessRejection;
        private final String reason;
        private final String observation;
        private final boolean retryable;

        private Failure(RunAdmissionDiagnostics context, Exception exception) {
            super("Admission failure; diagnosticId=" + context.diagnosticId);
            diagnosticId = context.diagnosticId;
            taskId = context.taskId;
            schedulerInstanceId = context.schedulerInstanceId;
            executionId = context.executionId;
            step = context.step;
            reason = exception instanceof ServiceException ? safeReason(exception.getMessage()) : "INTERNAL_ERROR";
            businessRejection = exception instanceof ServiceException && !isInfrastructureFailure(reason);
            observation = context.observation;
            retryable = businessRejection && "GUARD_NODE_RUNNING_CHECK".equals(step)
                    && "GUARD_NODE_NOT_RUNNING".equals(reason) && context.guardVisibilityPending;
            // Copy stack frames and cause types, not messages: SQL/HTTP/driver messages can contain secrets.
            initCause(sanitized(exception, Collections.newSetFromMap(new IdentityHashMap<>()), 0));
        }

        public String getDiagnosticId() { return diagnosticId; }
        public Long getTaskId() { return taskId; }
        public Long getSchedulerInstanceId() { return schedulerInstanceId; }
        public Long getExecutionId() { return executionId; }
        public String getStep() { return step; }
        public boolean isBusinessRejection() { return businessRejection; }
        public String getReason() { return reason; }
        public String getObservation() { return observation; }
        public boolean isRetryable() { return retryable; }
    }

    private static boolean isInfrastructureFailure(String reason) {
        return reason.equals("DS_LATEST_QUERY_FAILED") || reason.equals("DS_TASK_QUERY_FAILED")
                || reason.equals("DS_TASK_RESPONSE_INCOMPLETE") || reason.equals("DS_TASK_TOTAL_CHANGED")
                || reason.equals("DS_TASK_IDENTITY_OR_STATE_INVALID") || reason.equals("DS_TASK_PAGE_INCOMPLETE")
                || reason.equals("DS_TASK_QUERY_LIMIT") || reason.equals("DS_WORKFLOW_QUERY_FAILED")
                || reason.equals("DS_WORKFLOW_RESPONSE_INCOMPLETE") || reason.equals("DS_HTTP_STATUS_ERROR")
                || reason.equals("DS_NON_JSON_RESPONSE");
    }

    private static String safeReason(String message) {
        if (message == null) return "BUSINESS_REJECTED";
        if (message.startsWith("DolphinScheduler接口请求失败，状态码：")) return "DS_HTTP_STATUS_ERROR";
        if (message.startsWith("DolphinScheduler接口返回非JSON响应，地址：")) return "DS_NON_JSON_RESPONSE";
        // Exact-match only. Never log arbitrary ServiceException text from upstream APIs.
        switch (message) {
            case "运行保护未启用": return "GUARD_DISABLED";
            case "任务不存在": case "运行所属任务不存在": return "TASK_NOT_FOUND";
            case "任务发布配置不存在": return "PUBLICATION_NOT_FOUND";
            case "任务回调凭据无效": return "CALLBACK_CREDENTIAL_REJECTED";
            case "组件任务未发布或已下线，禁止准入/恢复": return "TASK_NOT_PUBLISHED";
            case "准入请求与已发布组件快照不一致，请重新发布任务": return "SNAPSHOT_MISMATCH";
            case "运行实例不属于已发布组件版本或已结束": return "WORKFLOW_VERSION_OR_STATE_MISMATCH";
            case "工作流运行准入节点未在执行": return "GUARD_NODE_NOT_RUNNING";
            case "准入过程中发布版本已变化，拒绝登记旧快照": return "PUBLICATION_CHANGED";
            case "流程实例运行owner初始化失败": return "OWNER_INITIALIZATION_FAILED";
            case "运行已暂停、待核对或结束，不能重新准入": return "OWNER_NOT_ACTIVE";
            case "运行写目标或工作流已变化，禁止重试覆盖原执行上下文": return "OWNER_WORKFLOW_MISMATCH";
            case "同一任务已有运行资格，等待原实例确认结束": return "TASK_ALREADY_OWNED";
            case "运行租约冲突，禁止启动写入": return "LEASE_CONFLICT";
            case "无法按code查询DS最新执行": return "DS_LATEST_QUERY_FAILED";
            case "DS尚无对应工作流执行": return "DS_INSTANCE_NOT_VISIBLE";
            case "DS最新执行code不一致": return "DS_LATEST_IDENTITY_MISMATCH";
            case "无法完整确认 DS 子任务状态": return "DS_TASK_QUERY_FAILED";
            case "DS 子任务状态响应不完整": return "DS_TASK_RESPONSE_INCOMPLETE";
            case "DS 子任务分页总数缺失或查询期间变化": return "DS_TASK_TOTAL_CHANGED";
            case "DS 子任务分页重复、身份或状态无效": return "DS_TASK_IDENTITY_OR_STATE_INVALID";
            case "DS 子任务分页数量不完整": return "DS_TASK_PAGE_INCOMPLETE";
            case "DS 子任务数量超过安全查询上限": return "DS_TASK_QUERY_LIMIT";
            case "无法确认 DolphinScheduler 流程实例状态": return "DS_WORKFLOW_QUERY_FAILED";
            case "DS流程响应身份或状态不完整": return "DS_WORKFLOW_RESPONSE_INCOMPLETE";
            default: return "BUSINESS_REJECTED";
        }
    }

    private static Throwable sanitized(Throwable original, Set<Throwable> visited, int depth) {
        Throwable safe = new Throwable(original.getClass().getName());
        safe.setStackTrace(original.getStackTrace());
        if (depth < 12 && visited.add(original) && original.getCause() != null
                && !visited.contains(original.getCause())) {
            safe.initCause(sanitized(original.getCause(), visited, depth + 1));
        }
        return safe;
    }
}
