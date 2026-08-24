package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 动作执行记录 DO
 *
 * 对应表 ONT_ACTION_EXECUTION
 */
@Data
@TableName("ONT_ACTION_EXECUTION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionExecutionDO extends BaseEntity {

    /** 动作ID */
    private Long actionId;

    /** 本体ID */
    private Long ontologyId;

    /** 执行者ID */
    private Long executorId;

    /** 输入参数 (JSONB) */
    private String inputParams;

    /** 生成的SQL */
    private String generatedSql;

    /** 预览结果 (JSONB) */
    private String previewResult;

    /** 执行前数据快照 (JSONB) */
    private String beforeData;

    /** 状态：DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/EXECUTED/FAILED */
    private String status;

    /** 审批意见 */
    private String approvalReason;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private java.util.Date approveTime;

    /** 执行时间 */
    private java.util.Date executeTime;

    /** 错误信息 */
    private String errorMessage;

    @TableLogic
    private Integer delFlag;
}
