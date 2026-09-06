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
 * 审批关卡任务 DO
 *
 * <p>对应表 ONT_ACTION_TASK。当前每个请求只创建一个人工确认任务；
 * 每个关卡需 REQUIRED_APPROVALS 位审阅人同意后 APPROVED 并推进下一关。</p>
 */
@Data
@TableName("ONT_ACTION_TASK")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionApprovalTaskDO extends BaseEntity {

    /** 审批请求ID */
    private Long requestId;

    /** 关卡序号（1..totalStages） */
    private Integer stageNo;

    /** 状态：PENDING/APPROVED/REJECTED */
    private String status;

    /** 本关卡需同意人次数（默认1） */
    private Integer requiredApprovals;

    /** 本关卡已同意人次数 */
    private Integer approvedCount;

    /** 本关卡审批人用户ID（配置了审批人时非空；为空=任意登录用户可审批本关，兼容旧数据） */
    private Long approverId;

    /** 本关卡审批人姓名 */
    private String approverName;

    @TableLogic
    private Integer delFlag;
}
