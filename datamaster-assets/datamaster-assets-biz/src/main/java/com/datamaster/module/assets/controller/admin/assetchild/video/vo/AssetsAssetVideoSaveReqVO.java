package com.datamaster.module.assets.controller.admin.assetchild.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * - / Request VO DA_ASSET_VIDEO
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "- Response VO")
@Data
public class AssetsAssetVideoSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "IP", example = "")
    @Size(max = 256, message = "IP256")
    private String ip;

    @Schema(description = "", example = "")
    private Long port;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String protocol;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String platform;

    @Schema(description = "JSON", example = "")
    private String config;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
