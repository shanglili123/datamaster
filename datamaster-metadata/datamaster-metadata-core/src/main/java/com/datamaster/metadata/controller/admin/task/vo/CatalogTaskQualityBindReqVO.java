package com.datamaster.metadata.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 探查任务-质量探查任务绑定 Request VO
 *
 * @author DATAMASTER
 * @date 2026-07-31
 */
@Schema(description = "探查任务-质量探查任务绑定 Request VO")
@Data
public class CatalogTaskQualityBindReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "探查任务ID", example = "1")
    @NotNull(message = "探查任务ID不能为空")
    private Long catTaskId;

    @Schema(description = "质量探查任务ID", example = "1")
    @NotNull(message = "质量探查任务ID不能为空")
    private Long qualityTaskId;

}
