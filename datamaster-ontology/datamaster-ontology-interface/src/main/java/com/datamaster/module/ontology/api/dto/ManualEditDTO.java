package com.datamaster.module.ontology.api.dto;

import java.util.Date;

/**
 * 人为编辑登记条目（供数据接收工程冲突仲裁使用）
 *
 * <p>一条对应一次发起并落库的本体数据写动作（EXECUTED）或回退动作（ROLLED_BACK），
 * 数据接收工程据此判断某条目标表记录是否被人为覆盖，从而做字段级仲裁与第三值告警。</p>
 */
public class ManualEditDTO {

    /** 执行记录ID */
    private Long executionId;

    /** 动作ID */
    private Long actionId;

    /** 动作类型：UPDATE / CREATE / DELETE / FUNCTION */
    private String actionType;

    /** 状态：EXECUTED（人为编辑生效）/ ROLLED_BACK（人为回退） */
    private String status;

    /** 编辑目标物理表 */
    private String targetTable;

    /** 编辑前数据快照 (JSON 数组)：人为编辑前的整行数据 */
    private String beforeData;

    /** 编辑后数据快照 (JSON 数组)：人为编辑后的整行数据（EXECUTED 时的人为决策值） */
    private String afterData;

    /** 回退前数据快照 (JSON 数组) */
    private String rollbackBeforeData;

    /** 回退后数据快照 (JSON 数组)：ROLLED_BACK 时的人为决策值 */
    private String rollbackAfterData;

    /** 执行时间 */
    private Date executeTime;

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }
    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String beforeData) { this.beforeData = beforeData; }
    public String getAfterData() { return afterData; }
    public void setAfterData(String afterData) { this.afterData = afterData; }
    public String getRollbackBeforeData() { return rollbackBeforeData; }
    public void setRollbackBeforeData(String rollbackBeforeData) { this.rollbackBeforeData = rollbackBeforeData; }
    public String getRollbackAfterData() { return rollbackAfterData; }
    public void setRollbackAfterData(String rollbackAfterData) { this.rollbackAfterData = rollbackAfterData; }
    public Date getExecuteTime() { return executeTime; }
    public void setExecuteTime(Date executeTime) { this.executeTime = executeTime; }
}