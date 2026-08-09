package com.datamaster.module.assets.controller.admin.assetchild.files.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * - Request VO  DA_ASSET_FILES
 *
 * @author DATAMASTER
 * @date 2025-06-26
 */
@Schema(description = "- Request VO")
@Data
public class AssetsAssetFilesPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "", example = "")
    private String name;
    @Schema(description = "", example = "")
    private Integer startColumn;
    @Schema(description = "", example = "")
    private Integer startData;
    @Schema(description = "", example = "")
    private String url;

    @Schema(description = "", example = "")
    private String type;

}
