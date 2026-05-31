package com.datamaster.module.assets.controller.admin.sensitiveLevel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 *  Request VO  DA_SENSITIVE_LEVEL
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Schema(description = " Request VO")
@Data
public class AssetsSensitiveLevelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "", example = "")
    private String sensitiveLevel;

    @Schema(description = "", example = "")
    private String sensitiveRule;

    @Schema(description = "", example = "")
    private Long startCharLoc;

    @Schema(description = "", example = "")
    private Long endCharLoc;

    @Schema(description = "", example = "")
    private String maskCharacter;

    @Schema(description = "", example = "")
    private String onlineFlag;

    @Schema(description = "", example = "")
    private String description;

}
