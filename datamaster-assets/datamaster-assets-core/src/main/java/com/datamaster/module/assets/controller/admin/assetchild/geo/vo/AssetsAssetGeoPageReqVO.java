package com.datamaster.module.assets.controller.admin.assetchild.geo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * - Request VO  DA_ASSET_GEO
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "- Request VO")
@Data
public class AssetsAssetGeoPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "", example = "")
    private String fileName;

    @Schema(description = "", example = "")
    private String fileUrl;

    @Schema(description = "", example = "")
    private String fileType;

    @Schema(description = "", example = "")
    private String elementType;

    @Schema(description = "", example = "")
    private String coordinateSystem;

}
