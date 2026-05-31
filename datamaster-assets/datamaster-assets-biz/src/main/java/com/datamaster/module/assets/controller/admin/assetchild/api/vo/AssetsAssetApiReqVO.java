package com.datamaster.module.assets.controller.admin.assetchild.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * -API Request VO  DA_ASSET_API
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "-API Request VO")
@Data
public class AssetsAssetApiReqVO {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "API", example = "")
    private String url;

    @Schema(description = "", example = "")
    private String httpMethod;

    @Schema(description = "", example = "")
    private String developerName;

    @Schema(description = "", example = "")
    private String appName;

    Map<String, Object> queryParams;
}
