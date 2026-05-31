package com.datamaster.module.catalog.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 数据集成调度信息 DO 对象 Catalog_TASK_SCHEDULER
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Data
@TableName(value = "CAT_TASK_SCHEDULER")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("Catalog_TASK_SCHEDULER_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogTaskSchedulerDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 任务id */
    private Long taskId;

    /** 调度器ID */
    private String jobId;

    /** DolphinScheduler任务编码（用于API调用） */
    private String taskCode;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 时区 */
    private String timezoneId;

    /** cron表达式 */
    private String cronExpression;

    /** 失败策略 */
    private String failureStrategy;

    /** 调度状态 */
    private String status;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
