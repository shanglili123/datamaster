package com.datamaster.module.assets.controller.admin.assetchild.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * - Request VO  DA_ASSET_VIDEO
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "- Request VO")
@Data
public class AssetsAssetVideoReqVO {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "IP", example = "")
    private String ip;

    @Schema(description = "", example = "")
    private Long port;

    @Schema(description = "", example = "")
    private String protocol;

    @Schema(description = "", example = "")
    private String platform;

    @Schema(description = "JSON", example = "")
    private String config;

}
