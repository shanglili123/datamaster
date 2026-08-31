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

    /** 提交时的空间ID（执行阶段二次权限校验复用） */
    private Long spaceId;

    /** 提交时的空间编码（执行阶段二次权限校验复用） */
    private String spaceCode;

    /** 输入参数 (JSONB) */
    private String inputParams;

    /** 生成的SQL */
    private String generatedSql;

    /** 预览结果 (JSONB) */
    private String previewResult;

    /** 执行前数据快照 (JSONB) */
    private String beforeData;

    /** 执行后数据快照 (JSONB)：UPDATE=重查新值 / DELETE=空集 / CREATE=按主键回查新行 */
    private String afterData;

    /** 回退前数据快照 (JSONB)：回退动作执行前一刻的数据（= 原执行 afterData，即回退时的当前状态） */
    private String rollbackBeforeData;

    /** 回退后数据快照 (JSONB)：回退动作执行后的数据（UPDATE/DELETE 回退=还原的 beforeData / CREATE 回退=空集） */
    private String rollbackAfterData;

    /** 状态：DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/EXECUTED/FAILED/ROLLED_BACK */
    private String status;

    /** 审批意见 */
    private String approvalReason;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private java.util.Date approveTime;

    /** 执行时间 */
    private java.util.Date executeTime;

    /** 回退SQL（ROLLED_BACK 留痕：回退动作实际执行的还原语句） */
    private String rollbackSql;

    /** 回退时间 */
    private java.util.Date rollbackTime;

    /** 错误信息 */
    private String errorMessage;

    @TableLogic
    private Integer delFlag;
}
