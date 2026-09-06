package com.datamaster.module.ontology.controller.admin.action.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "执行记录 Response VO")
@Data
public class ExecutionRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "动作ID", example = "1")
    @Excel(name = "动作ID")
    private Long actionId;

    @Schema(description = "本体ID", example = "1")
    @Excel(name = "本体ID")
    private Long ontologyId;

    @Schema(description = "输入参数", example = "{\"name\":\"张三\"}")
    @Excel(name = "输入参数")
    private String inputParams;

    @Schema(description = "生成的SQL", example = "INSERT INTO t_customer (name) VALUES ('张三')")
    @Excel(name = "生成SQL")
    private String generatedSql;

    @Schema(description = "预览结果")
    @Excel(name = "预览结果")
    private String previewResult;

    @Schema(description = "执行前数据")
    @Excel(name = "执行前数据")
    private String beforeData;

    @Schema(description = "执行后数据：UPDATE=重查新值 / DELETE=空集 / CREATE=按主键回查新行")
    @Excel(name = "执行后数据")
    private String afterData;

    @Schema(description = "回退前数据：回退动作执行前一刻的数据（= 原执行后数据，即回退时的当前状态）")
    @Excel(name = "回退前数据")
    private String rollbackBeforeData;

    @Schema(description = "回退后数据：回退动作执行后的数据（UPDATE/DELETE 回退=还原的旧值 / CREATE 回退=空集）")
    @Excel(name = "回退后数据")
    private String rollbackAfterData;

    @Schema(description = "状态", example = "DRAFT")
    @Excel(name = "状态", readConverterExp = "DRAFT=草稿,PENDING_APPROVAL=待审批,APPROVED=已批准,RUNNING=执行中,REJECTED=已拒绝,EXECUTED=已执行,FAILED=失败,RECONCILIATION_REQUIRED=需对账")
    private String status;

    @Schema(description = "是否由后台 Worker 自动执行")
    private Boolean autoExecute;

    @Schema(description = "触发类型", example = "DATA_ARRIVAL")
    private String triggerType;

    @Schema(description = "触发器引用", example = "customer_stream")
    private String triggerRef;

    @Schema(description = "来源事件唯一编号", example = "customer_stream:0:1024")
    private String eventId;

    @Schema(description = "最大执行尝试次数", example = "1")
    private Integer maxAttempts;

    @Schema(description = "最早可执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextRunTime;

    @Schema(description = "审批意见", example = "同意")
    @Excel(name = "审批意见")
    private String approvalReason;

    @Schema(description = "审批者", example = "admin")
    @Excel(name = "审批者")
    private String approverId;

    @Schema(description = "审批时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    @Schema(description = "执行时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date executeTime;

    @Schema(description = "回退SQL（ROLLED_BACK 留痕）")
    @Excel(name = "回退SQL", width = 60)
    private String rollbackSql;

    @Schema(description = "回退时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "回退时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date rollbackTime;

    @Schema(description = "错误信息")
    @Excel(name = "错误信息")
    private String errorMessage;

    @Schema(description = "目标对象主键（对象实例级决策载体）", example = "CUST001")
    private String objectKey;

    @Schema(description = "①提交判定结果 JSONB：{\"decision\":\"PASS|REJECT\",\"passed\":boolean,\"detail\":[...]}")
    private String criteriaResult;

    @Schema(description = "人工确认兼容关卡：0=无需/未提交确认，1=唯一人工确认任务", example = "1")
    private Integer currentStage;

    @Schema(description = "当前登录用户是否可审批该执行记录（待审批且未绑定审批人或本人为当前关指定审批人时=true）", example = "true")
    private Boolean canApprove;

    @Schema(description = "提交时冻结的动作定义版本", example = "1")
    private Integer actionVersion;

    @Schema(description = "提交时冻结的函数版本（函数类型动作）", example = "1")
    private Integer functionVersion;

    @Schema(description = "提交时冻结的解析函数体 SHA-256")
    private String functionHash;

    @Schema(description = "幂等键", example = "evt-20260905-0001")
    private String idempotencyKey;

    @Schema(description = "尝试次数", example = "1")
    private Integer attemptNo;

    @Schema(description = "执行中锁标记（非空=正在执行）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockTime;

    @Schema(description = "执行锁持有者")
    private String lockOwner;

    @Schema(description = "统一执行结果上下文 JSONB {actionType,status,objectKey,actionVersion,targetTable,affectedRows,output,...}")
    private String resultContext;

    @Schema(description = "失败分类错误码", example = "EXECUTE_ERROR")
    private String errorCode;

    @Schema(description = "所属工作流运行ID（预留）")
    private Long workflowRunId;

    @Schema(description = "所属工作流步骤ID（预留）")
    private Long workflowStepId;

    @Schema(description = "触发本次执行的上游执行记录ID（预留）")
    private Long parentExecutionId;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
