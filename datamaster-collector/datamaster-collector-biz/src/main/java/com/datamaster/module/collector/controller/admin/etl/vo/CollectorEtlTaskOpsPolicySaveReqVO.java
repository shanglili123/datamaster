package com.datamaster.module.collector.controller.admin.etl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Task ops policy save request.
 */
@Data
public class CollectorEtlTaskOpsPolicySaveReqVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "失败即停")
    private Boolean failStopEnabled;

    @Schema(description = "AI托管")
    private Boolean aiManaged;

    @Schema(description = "自动恢复")
    private Boolean autoRecoverEnabled;

    @Schema(description = "最大自动恢复次数")
    private Integer maxRecoverTimes;

    @Schema(description = "恢复策略")
    private String recoverStrategy;

    @Schema(description = "通知用户")
    private String notifyUsers;
}
