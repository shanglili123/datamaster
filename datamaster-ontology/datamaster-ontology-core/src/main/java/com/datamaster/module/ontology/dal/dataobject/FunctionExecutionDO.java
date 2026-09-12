package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * 函数执行记录 DO — 对应表 ONT_FUNCTION_EXECUTION
 */
@Data
@TableName("ONT_FUNCTION_EXECUTION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FunctionExecutionDO extends BaseEntity {

    /** 函数ID */
    private Long functionId;

    /** 本体ID */
    @TableField(exist = false)
    private Long ontologyId;

    /** 执行者ID */
    @TableField(exist = false)
    private Long executorId;

    /** 输入参数 (JSON) */
    @TableField("input_data")
    private String inputParams;

    /** 输出结果 (JSON) */
    @TableField("output_data")
    private String outputResult;

    /** 状态：DRAFT / PENDING_APPROVAL / APPROVED / REJECTED / EXECUTED / FAILED */
    private String status;

    /** 审批意见 */
    private String approvalReason;

    /** 审批时间 */
    @TableField(exist = false)
    private Date approveTime;

    /** 执行时间 */
    @TableField(exist = false)
    private Date executeTime;

    /** 错误信息 */
    private String errorMessage;

    /** 执行耗时 (ms) */
    private Long durationMs;

    @TableLogic
    private Integer delFlag;
}
