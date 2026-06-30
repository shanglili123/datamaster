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
 * Task ops policy for data integration and data development.
 */
@Data
@TableName(value = "COL_ETL_TASK_OPS_POLICY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectorEtlTaskOpsPolicyDO extends BaseEntity {

    private Long taskId;

    private String taskType;

    private Boolean failStopEnabled;

    private Boolean aiManaged;

    private Boolean autoRecoverEnabled;

    private Integer maxRecoverTimes;

    private String recoverStrategy;

    private String notifyUsers;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;
}
