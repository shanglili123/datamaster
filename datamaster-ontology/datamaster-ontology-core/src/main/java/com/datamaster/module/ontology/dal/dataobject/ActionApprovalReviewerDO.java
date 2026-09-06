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
 * 审阅人决策审计 DO
 *
 * <p>对应表 ONT_ACTION_REVIEWER。只追加（append-only）：每一条为一位审阅人对某个关卡的永久决策
 * （谁 + 何时 + 结论 + 可选意见），确认后不再修改，构成永久审计留痕。</p>
 */
@Data
@TableName("ONT_ACTION_REVIEWER")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionApprovalReviewerDO extends BaseEntity {

    /** 人工确认任务ID */
    private Long taskId;

    /** 审批请求ID */
    private Long requestId;

    /** 审阅人ID */
    private Long reviewerId;

    /** 审阅人姓名（冗余，便于审计展示） */
    private String reviewerName;

    /** 决策：APPROVE/REJECT */
    private String decision;

    /** 审批意见（可选；空字符串表示未填写，确认结论仍永久审计） */
    private String reason;

    /** 决策时间 */
    private java.util.Date decideTime;

    @TableLogic
    private Integer delFlag;
}
