package com.datamaster.ingestion.model;

/**
 * 数据接收处理结果
 */
public class IngestionResult {

    public enum Action {
        INSERT,
        UPSERT,
        SKIP,
        ERROR,
        /** 命中人为编辑且字段级仲裁生效（编辑值优先回写） */
        MANUAL_EDIT_APPLIED,
        /** 命中人为编辑但库中值已是编辑后值，无需再写 */
        MANUAL_EDIT_SKIP,
        /** 存在人为编辑但库中当前值与编辑前/后均不符（第三值），告警不覆盖 */
        THIRD_VALUE_ALERT
    }

    private Action action;
    private boolean success;
    private String message;

    public IngestionResult(Action action, boolean success, String message) {
        this.action = action;
        this.success = success;
        this.message = message;
    }

    public static IngestionResult ok(Action action, String message) {
        return new IngestionResult(action, true, message);
    }

    public static IngestionResult fail(Action action, String message) {
        return new IngestionResult(action, false, message);
    }

    public Action getAction() { return action; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "IngestionResult{action=" + action + ", success=" + success + ", message=" + message + '}';
    }
}