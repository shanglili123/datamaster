package com.datamaster.module.collector.dal.dataobject.etl;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 数据集成调度信息 DO 对象 COL_ETL_SCHEDULER
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Data
@TableName(value = "COL_ETL_SCHEDULER")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("COL_ETL_SCHEDULER_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectorEtlSchedulerDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 任务id */
    private Long taskId;

    /** 任务编码 */
    private String taskCode;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 时区 */
    private String timezoneId;

    @Schema(description = "任务状态", example = "")
    private String status;

    /** cron表达式 */
    private String cronExpression;

    /** 失败策略 */
    private String failureStrategy;

    /** DolphinScheduler的id */
    private Long dsId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
