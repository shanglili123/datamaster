package com.datamaster.module.assets.controller.admin.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 *  / Request VO DA_DATASOURCE_PROJECT_REL
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Schema(description = " Response VO")
@Data
public class AssetsDatasourceProjectRelSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long projectId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String projectCode;

    @Schema(description = "id", example = "")
    private Long datasourceId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String description;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

    @Schema(description = "", example = "")
    private Boolean dppAssigned;

}
