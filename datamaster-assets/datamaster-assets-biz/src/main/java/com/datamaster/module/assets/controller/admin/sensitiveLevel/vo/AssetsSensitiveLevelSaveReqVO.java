package com.datamaster.module.assets.controller.admin.sensitiveLevel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 *  / Request VO DA_SENSITIVE_LEVEL
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Schema(description = " Response VO")
@Data
public class AssetsSensitiveLevelSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String sensitiveLevel;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String sensitiveRule;

    @Schema(description = "", example = "")
    private Long startCharLoc;

    @Schema(description = "", example = "")
    private Long endCharLoc;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String maskCharacter;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String onlineFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String description;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
