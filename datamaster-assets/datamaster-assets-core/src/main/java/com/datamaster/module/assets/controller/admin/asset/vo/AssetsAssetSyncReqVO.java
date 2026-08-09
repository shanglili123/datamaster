package com.datamaster.module.assets.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资产元数据同步 Request VO
 *
 * 三个筛选条件均可选；全部为空时执行全量同步。
 */
@Schema(description = "资产元数据同步 Request VO")
@Data
public class AssetsAssetSyncReqVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源ID", example = "")
    private Long datasourceId;

    @Schema(description = "探查任务ID", example = "")
    private Long taskId;

    @Schema(description = "资产ID", example = "")
    private Long assetId;

}
