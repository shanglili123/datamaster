package com.datamaster.module.assets.controller.admin.assetchild.files.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * - / Request VO DA_ASSET_FILES
 *
 * @author DATAMASTER
 * @date 2025-06-26
 */
@Schema(description = "- Response VO")
@Data
public class AssetsAssetFilesSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;
    @Schema(description = "", example = "")
    private Integer startColumn;
    @Schema(description = "", example = "")
    private Integer startData;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String name;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String url;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String type;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
