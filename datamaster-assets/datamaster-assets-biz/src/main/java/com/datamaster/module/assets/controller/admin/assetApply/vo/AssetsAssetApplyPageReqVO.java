package com.datamaster.module.assets.controller.admin.assetApply.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.Set;

/**
 * Request VO  DA_ASSET_APPLY
 *
 * @author shu
 * @date 2025-03-19
 */
@Schema(description = " Request VO")
@Data
public class AssetsAssetApplyPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID", example = "")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "", example = "")
    private String assetName;

    @Schema(description = "", example = "")
    private String assetTableName;

    @Schema(description = "", example = "")
    private String catAssetName;

    @Schema(description = "", example = "")
    private String catAssetCode;

    @Schema(description = "id", example = "")
    private Long projectId;

    @Schema(description = "01", example = "")
    private String sourceType;

    @Schema(description = "", example = "")
    private String projectName;

    @Schema(description = "", example = "")
    private String themeName;

    @Schema(description = "", example = "")
    private String projectCode;

    @Schema(description = "", example = "")
    private String applyReason;

    @Schema(description = "", example = "")
    private String approvalReason;

    @Schema(description = "", example = "")
    private String status;

    private Set<Long> assetIds;

}
