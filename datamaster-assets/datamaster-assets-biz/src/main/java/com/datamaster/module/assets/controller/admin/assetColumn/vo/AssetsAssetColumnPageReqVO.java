package com.datamaster.module.assets.controller.admin.assetColumn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 *  Request VO  DA_ASSET_COLUMN
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = " Request VO")
@Data
public class AssetsAssetColumnPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private String assetId;

    @Schema(description = "/", example = "")
    private String columnName;

    @Schema(description = "/", example = "")
    private String columnComment;

    @Schema(description = "", example = "")
    private String columnType;

    @Schema(description = "", example = "")
    private Long columnLength;

    @Schema(description = "", example = "")
    private Long columnScale;

    @Schema(description = "", example = "")
    private String nullableFlag;

    @Schema(description = "", example = "")
    private String pkFlag;

    @Schema(description = "", example = "")
    private String defaultValue;

    @Schema(description = "", example = "")
        private String dataElemCodeFlag;

    @Schema(description = "id", example = "")
    private String dataElemCodeId;

    @Schema(description = "id", example = "")
    private String sensitiveLevelId;

    @Schema(description = "", example = "")
    private String sensitiveLevelName;

    @Schema(description = "", example = "")
    private String relDataElmeFlag;

    @Schema(description = "", example = "")
    private String relCleanFlag;

    @Schema(description = "", example = "")
    private String relAuditFlag;

    @Schema(description = "", example = "")
    private String description;

}
