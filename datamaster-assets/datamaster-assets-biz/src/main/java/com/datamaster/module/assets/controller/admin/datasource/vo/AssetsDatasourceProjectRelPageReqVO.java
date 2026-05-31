package com.datamaster.module.assets.controller.admin.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 *  Request VO  DA_DATASOURCE_PROJECT_REL
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Schema(description = " Request VO")
@Data
public class AssetsDatasourceProjectRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "id", example = "")
    private Long projectId;

    @Schema(description = "", example = "")
    private String projectCode;

    @Schema(description = "id", example = "")
    private Long datasourceId;

    @Schema(description = "", example = "")
    private String description;

    @Schema(description = "", example = "")
    private Boolean dppAssigned;

}
