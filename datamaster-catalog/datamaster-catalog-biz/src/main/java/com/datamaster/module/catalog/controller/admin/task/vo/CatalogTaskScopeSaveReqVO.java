package com.datamaster.module.catalog.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 采集范围 创建/修改 Request VO Catalog_TASK_SCOPE
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Schema(description = "采集范围 Response VO")
@Data
public class CatalogTaskScopeSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "任务id", example = "")
    private Long taskId;

    @Schema(description = "数据库名称", example = "")
    @Size(max = 256, message = "数据库名称长度不能超过256个字符")
    private String dbName;

    @Schema(description = "模式名", example = "")
    @Size(max = 256, message = "模式名长度不能超过256个字符")
    private String schemaName;

    @Schema(description = "备注", example = "")
    @Size(max = 3000, message = "备注长度不能超过3000个字符")
    private String remark;

    @Schema(description = "描述", example = "")
    @Size(max = 3000, message = "描述长度不能超过3000个字符")
    private String description;

    /** 项目ID */
    @Schema(description = "项目ID")
    private Long projectId;

    /** 项目编码 */
    @Schema(description = "项目编码")
    private String projectCode;

}
