package com.datamaster.module.assets.controller.admin.assetchild.theme.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * - / Request VO DA_ASSET_THEME_REL
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "- Response VO")
@Data
public class AssetsAssetThemeRelSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "id", example = "")
    private Long themeId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
