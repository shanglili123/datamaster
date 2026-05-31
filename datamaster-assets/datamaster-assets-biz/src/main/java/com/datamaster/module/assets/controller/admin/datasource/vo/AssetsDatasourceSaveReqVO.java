package com.datamaster.module.assets.controller.admin.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;

import javax.validation.constraints.Size;
import java.util.List;

/**
 *  / Request VO DA_DATASOURCE
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = " Response VO")
@Data
public class AssetsDatasourceSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String datasourceName;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String datasourceType;

    @Schema(description = "(json)", example = "")
    @Size(max = 256, message = "(json)256")
    private String datasourceConfig;

    @Schema(description = "", example = "")
    private List<Long> projectListOld;

    @Schema(description = "", example = "")
    private List<AssetsDatasourceProjectRelDO> projectList;

    @Schema(description = "IP", example = "")
    @Size(max = 256, message = "IP256")
    private String ip;

    @Schema(description = "", example = "")
    private Long port;

    @Schema(description = "", example = "")
    private Long listCount;

    @Schema(description = "", example = "")
    private Long syncCount;

    @Schema(description = "", example = "")
    private Long DataSize;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String description;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
