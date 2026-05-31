package com.datamaster.module.assets.controller.admin.discovery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 *  Request VO  DA_DISCOVERY_TABLE
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Schema(description = " Request VO")
@Data
public class AssetsDiscoveryTablePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long taskId;

    @Schema(description = "", example = "")
    private String tableName;

    @Schema(description = "", example = "")
    private String tableComment;

    @Schema(description = "", example = "")
    private Long dataCount;

    @Schema(description = "", example = "")
    private Long fieldCount;

    @Schema(description = "", example = "")
    private String changeFlag;

    @Schema(description = "", example = "")
    private String status;

    @Schema(description = "", example = "")
    private String ignoreFlag;

    private String keyword;

}
