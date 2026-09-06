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
 * 审批请求 DO
 *
 * <p>对应表 ONT_ACTION_REQUEST。一个对象执行记录对应一个审批流转（③ Approvals+Checkpoint）：
 * 动作配置为人工确认模式时创建；唯一确认人同意后置 APPROVED，决定不执行则置 REJECTED。</p>
 */
@Data
@TableName("ONT_ACTION_REQUEST")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionApprovalRequestDO extends BaseEntity {

    /** 动作执行记录ID（对象实例级决策载体，一个执行记录一个审批流） */
    private Long actionExecutionId;

    /** 本体ID */
    private Long ontologyId;

    /** 动作ID */
    private Long actionId;

    /** 目标对象主键 */
    private String objectKey;

    /** 总关卡数 */
    private Integer totalStages;

    /** 已推进到的关卡（1..totalStages） */
    private Integer currentStage;

    /** 状态：PENDING/APPROVED/REJECTED/CANCELLED */
    private String status;

    /** 提交人ID */
    private Long requestBy;

    /** 提交时间 */
    private java.util.Date requestTime;

    /** 备注 */
    private String remark;

    @TableLogic
    private Integer delFlag;
}
