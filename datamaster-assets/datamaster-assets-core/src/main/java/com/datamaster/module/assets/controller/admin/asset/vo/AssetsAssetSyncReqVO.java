package com.datamaster.module.assets.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资产元数据同步 Request VO
 *
 * 整库同步传 datasourceId + databaseName + catCode，可选 schemaName；单资产同步传 assetId。
 */
@Schema(description = "资产元数据同步 Request VO")
@Data
public class AssetsAssetSyncReqVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源ID", example = "")
    private Long datasourceId;

    @Schema(description = "数据库名称", example = "datamaster_test")
    private String databaseName;

    @Schema(description = "模式名称", example = "public")
    private String schemaName;

    @Schema(description = "资产目录编码", example = "A03A02")
    private String catCode;

    @Schema(description = "资产ID", example = "")
    private Long assetId;

}
