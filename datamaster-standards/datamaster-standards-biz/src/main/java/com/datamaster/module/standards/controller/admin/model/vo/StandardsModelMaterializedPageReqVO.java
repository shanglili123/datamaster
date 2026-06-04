package com.datamaster.module.standards.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 物化模型记录 Request VO 对象 STD_MODEL_MATERIALIZED
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Schema(description = "物化模型记录 Request VO")
@Data
public class StandardsModelMaterializedPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "模型编码", example = "")
    private String modelName;

    @Schema(description = "模型名称", example = "")
    private String modelAlias;

    @Schema(description = "模型表id", example = "")
    private Long modelId;

    @Schema(description = "状态", example = "")
    private String status;

    @Schema(description = "执行日志信息", example = "")
    private String message;

    @Schema(description = "执行sql备份", example = "")
    private String sqlCommand;

    @Schema(description = "数据源id", example = "")
    private String datasourceId;

    @Schema(description = "数据源类型", example = "")
    private String datasourceType;

    @Schema(description = "数据源名称", example = "")
    private String datasourceName;

    @Schema(description = "资产表id", example = "")
    private String assetId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
