package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 对象实例行操作 Response VO
 *
 * <p>preview 返回 executionId + 状态（是否需审批、当前用户能否审批）+ SQL 预览；
 * confirm 返回执行终态与快照。字段与动作执行记录对齐，保证行操作与普通动作记录一致。</p>
 */
@Schema(description = "对象实例行操作 Response VO")
@Data
public class RowOperateRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "执行记录ID（ONT_ACTION_EXECUTION.id）", example = "1")
    private Long executionId;

    @Schema(description = "执行记录状态：PENDING_APPROVAL=待审批 / APPROVED=已通过待执行 / EXECUTED=已执行 / REJECTED=已拒绝 / FAILED=失败")
    private String status;

    @Schema(description = "当前登录用户是否可审批（仅 PENDING_APPROVAL 时有意义）", example = "true")
    private Boolean canApprove;

    @Schema(description = "操作结果提示语")
    private String message;

    @Schema(description = "生成的SQL（提交 dry-run 产物）")
    private String generatedSql;

    @Schema(description = "dry-run 预览结果 / 执行结果（affectedRows 或 rows）")
    private String previewResult;

    @Schema(description = "执行前数据快照（仅 confirm 后存在）")
    private String beforeData;

    @Schema(description = "执行后数据快照（仅 confirm 后存在）")
    private String afterData;

    @Schema(description = "执行失败原因（status=FAILED 时）")
    private String errorMessage;

    @Schema(description = "本次是否触发 Webhook 回调")
    private Boolean webhookTriggered;
}