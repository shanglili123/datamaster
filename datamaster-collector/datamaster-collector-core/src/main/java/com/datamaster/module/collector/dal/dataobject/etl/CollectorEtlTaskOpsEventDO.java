package com.datamaster.module.collector.dal.dataobject.etl;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Task ops event.
 */
@Data
@TableName(value = "COL_ETL_TASK_OPS_EVENT")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectorEtlTaskOpsEventDO extends BaseEntity {

    private Long taskId;

    private String taskCode;

    private String taskType;

    private Long taskInstanceId;

    private Long dsProcessInstanceId;

    private String instanceStatus;

    private String eventType;

    private String failureType;

    private String riskLevel;

    private Boolean recoverable;

    private String action;

    private String actionStatus;

    private String reason;

    private String suggestion;

    private String logExcerpt;

    private String aiRawResult;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;
}
