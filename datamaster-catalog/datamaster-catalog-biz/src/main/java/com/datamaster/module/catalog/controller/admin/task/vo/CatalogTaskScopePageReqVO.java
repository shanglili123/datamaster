package com.datamaster.module.catalog.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 采集范围 Request VO 对象 Catalog_TASK_SCOPE
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Schema(description = "采集范围 Request VO")
@Data
public class CatalogTaskScopePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "任务id", example = "")
    private Long taskId;

    @Schema(description = "数据库名称", example = "")
    private String dbName;

    @Schema(description = "模式名", example = "")
    private String schemaName;



    @Schema(description = "描述", example = "")
    private String description;


    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
