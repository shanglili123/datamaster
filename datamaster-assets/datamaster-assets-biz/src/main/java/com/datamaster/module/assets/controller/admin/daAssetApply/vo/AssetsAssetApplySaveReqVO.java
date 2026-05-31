package com.datamaster.module.assets.controller.admin.daAssetApply.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 *  / Request VO DA_ASSET_APPLY
 *
 * @author shu
 * @date 2025-03-19
 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetApplySaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "id", example = "")
    private Long projectId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String projectCode;

    @Schema(description = "01", example = "")
    private String sourceType;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String applyReason;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String approvalReason;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String status;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

    @Schema(description = "", example = "")
    private String assetName;

    @Schema(description = "", example = "")
    private String assetTableName;

    @Schema(description = "", example = "")
    private String catAssetName;

    @Schema(description = "", example = "")
    private String catAssetCode;

    @Schema(description = "", example = "")
    private String projectName;

    @Schema(description = "", example = "")
    private String themeName;

}
