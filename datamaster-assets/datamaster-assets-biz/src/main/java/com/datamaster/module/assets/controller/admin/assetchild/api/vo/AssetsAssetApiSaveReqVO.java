package com.datamaster.module.assets.controller.admin.assetchild.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * -API / Request VO DA_ASSET_API
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "-API Response VO")
@Data
public class AssetsAssetApiSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "API", example = "")
    @Size(max = 256, message = "API256")
    private String url;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String httpMethod;

    @Schema(description = "", example = "")
    private String developerName;

    @Schema(description = "", example = "")
    private String appName;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
