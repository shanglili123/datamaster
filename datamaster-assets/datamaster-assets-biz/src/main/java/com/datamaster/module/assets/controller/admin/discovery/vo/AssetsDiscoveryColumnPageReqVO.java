package com.datamaster.module.assets.controller.admin.discovery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 *  Request VO  DA_DISCOVERY_COLUMN
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Schema(description = " Request VO")
@Data
public class AssetsDiscoveryColumnPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long taskId;

    @Schema(description = "id", example = "")
    private Long tableId;

    @Schema(description = "/", example = "")
    private String columnName;

    @Schema(description = "/", example = "")
    private String columnComment;

    @Schema(description = "", example = "")
    private String columnType;

    @Schema(description = "", example = "")
    private Long columnLength;

    @Schema(description = "", example = "")
    private Long columnScale;

    @Schema(description = "", example = "")
    private String nullableFlag;

    @Schema(description = "", example = "")
    private String pkFlag;

    @Schema(description = "", example = "")
    private String defaultValue;

}
