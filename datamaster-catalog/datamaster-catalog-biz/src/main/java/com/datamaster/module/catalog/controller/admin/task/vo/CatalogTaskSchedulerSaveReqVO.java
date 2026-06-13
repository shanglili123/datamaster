package com.datamaster.module.catalog.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.utils.DateUtils;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;

import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 数据集成调度信息 创建/修改 Request VO Catalog_TASK_SCHEDULER
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Schema(description = "数据集成调度信息 Response VO")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CatalogTaskSchedulerSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "任务id", example = "")
    private Long taskId;

    @Schema(description = "调度器ID", example = "")
    @Size(max = 256, message = "调度器ID长度不能超过256个字符")
    private String jobId;

    @Schema(description = "DolphinScheduler任务编码（用于API调用）", example = "")
    @Size(max = 256, message = "任务编码长度不能超过256个字符")
    private String taskCode;

    @Schema(description = "开始时间", example = "")
    private Date startTime;

    @Schema(description = "结束时间", example = "")
    private Date endTime;

    @Schema(description = "时区", example = "")
    @Size(max = 256, message = "时区长度不能超过256个字符")
    private String timezoneId;

    @Schema(description = "cron表达式", example = "")
    @Size(max = 256, message = "cron表达式长度不能超过256个字符")
    private String cronExpression;

    @Schema(description = "失败策略", example = "")
    @Size(max = 256, message = "失败策略长度不能超过256个字符")
    private String failureStrategy;

    @Schema(description = "调度状态", example = "")
    @Size(max = 256, message = "调度状态长度不能超过256个字符")
    private String status;

    @Schema(description = "备注", example = "")
    @Size(max = 3000, message = "备注长度不能超过3000个字符")
    private String remark;

    /** 项目ID */
    @Schema(description = "项目ID")
    private Long projectId;

    /** 项目编码 */
    @Schema(description = "项目编码")
    private String projectCode;


    public CatalogTaskSchedulerSaveReqVO(CatalogTaskDO dictType) {
        this.taskId = dictType.getId();
        this.jobId = null;

        Date nowDate = DateUtils.getNowDate();
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime + 100L * 365 * 24 * 60 * 60 * 1000);

        this.startTime = nowDate;
        this.endTime = date;

        this.timezoneId = "Asia/Shanghai";
        this.cronExpression = dictType.getCronExpression();

        this.failureStrategy = "0";
        this.status = "0";
        this.projectId = dictType.getProjectId();
        this.projectCode = dictType.getProjectCode();
    }
}
