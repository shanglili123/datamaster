package com.datamaster.metadata.controller.admin.task.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 探查任务-质量探查任务关联 Response VO
 *
 * @author DATAMASTER
 * @date 2026-07-31
 */
@Schema(description = "探查任务-质量探查任务关联 Response VO")
@Data
public class CatalogTaskQualityRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "探查任务ID")
    private Long catTaskId;

    @Schema(description = "质量探查任务ID")
    private Long qualityTaskId;

    @Schema(description = "质量探查任务名称")
    private String qualityTaskName;

    @Schema(description = "质量探查任务状态 0:上线 1:下线")
    private String qualityTaskStatus;

    @Schema(description = "质量探查任务调度周期")
    private String qualityTaskCycle;

    @Schema(description = "质量探查任务稽查对象数")
    private Integer qualityTaskObjNum;

    @Schema(description = "质量探查任务稽查规则数")
    private Integer qualityTaskEvaluateNum;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
