package com.datamaster.module.assets.controller.admin.assetchild.theme.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.List;

/**
 * - Request VO  DA_ASSET_THEME_REL
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "- Request VO")
@Data
public class AssetsAssetThemeRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "id", example = "")
    private Long themeId;

    private String themeName;

    private List<String> themeIdList;

}
