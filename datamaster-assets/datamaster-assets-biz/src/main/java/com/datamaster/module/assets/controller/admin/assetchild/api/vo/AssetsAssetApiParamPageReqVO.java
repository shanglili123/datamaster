package com.datamaster.module.assets.controller.admin.assetchild.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * -API- Request VO  DA_ASSET_API_PARAM
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "-API- Request VO")
@Data
public class AssetsAssetApiParamPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "API id", example = "")
    private Long apiId;

    @Schema(description = "id", example = "")
    private Long parentId;

    @Schema(description = "", example = "")
    private String name;

    @Schema(description = "", example = "")
    private String type;

    @Schema(description = "", example = "")
    private String requestFlag;

    @Schema(description = "", example = "")
    private String columnType;

    @Schema(description = "", example = "")
    private String defaultValue;
    @Schema(description = "", example = "")
    private String exampleValue;
    @Schema(description = "", example = "")
    private String description;

}
