package com.datamaster.module.assets.controller.admin.discovery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 *  / Request VO DA_DISCOVERY_COLUMN
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryColumnSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long taskId;

    @Schema(description = "id", example = "")
    private Long tableId;

    @Schema(description = "/", example = "")
    @Size(max = 256, message = "/256")
    private String columnName;

    @Schema(description = "/", example = "")
    @Size(max = 256, message = "/256")
    private String columnComment;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String columnType;

    @Schema(description = "", example = "")
    private Long columnLength;

    @Schema(description = "", example = "")
    private Long columnScale;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String nullableFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String pkFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String defaultValue;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
