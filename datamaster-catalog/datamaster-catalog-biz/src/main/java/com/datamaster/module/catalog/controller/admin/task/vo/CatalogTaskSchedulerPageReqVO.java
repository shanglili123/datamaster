package com.datamaster.module.catalog.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.Date;

/**
 * 数据集成调度信息 Request VO 对象 Catalog_TASK_SCHEDULER
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Schema(description = "数据集成调度信息 Request VO")
@Data
public class CatalogTaskSchedulerPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "任务id", example = "")
    private Long taskId;

    @Schema(description = "调度器id", example = "")
    private String jobId;

    @Schema(description = "开始时间", example = "")
    private Date startTime;

    @Schema(description = "结束时间", example = "")
    private Date endTime;

    @Schema(description = "时区", example = "")
    private String timezoneId;

    @Schema(description = "cron表达式", example = "")
    private String cronExpression;

    @Schema(description = "失败策略", example = "")
    private String failureStrategy;

    @Schema(description = "调度状态", example = "")
    private String status;




}
