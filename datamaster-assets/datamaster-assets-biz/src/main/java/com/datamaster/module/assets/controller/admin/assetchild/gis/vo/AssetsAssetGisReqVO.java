package com.datamaster.module.assets.controller.admin.assetchild.gis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * - Request VO  DA_ASSET_GIS
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "- Request VO")
@Data
public class AssetsAssetGisReqVO {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "", example = "")
    private String url;

    @Schema(description = "", example = "")
    private String type;

    @Schema(description = "", example = "")
    private String httpMethod;

    @Schema(description = "", example = "")
    private String coordinateSystem;

    Map<String, Object> queryParams;

}
