package com.datamaster.module.assets.controller.admin.assetchild.geo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * - / Request VO DA_ASSET_GEO
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "- Response VO")
@Data
public class AssetsAssetGeoSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String fileName;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String fileUrl;

    @Schema(description = "", example = "")
    private String fileType;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String elementType;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String coordinateSystem;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
