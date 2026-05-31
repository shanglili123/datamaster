package com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 *  Request VO  DA_ASSET_PROJECT_REL
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Schema(description = " Request VO")
@Data
public class AssetsAssetProjectRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "id", example = "")
    private Long projectId;

    @Schema(description = "", example = "")
    private String projectCode;

}
