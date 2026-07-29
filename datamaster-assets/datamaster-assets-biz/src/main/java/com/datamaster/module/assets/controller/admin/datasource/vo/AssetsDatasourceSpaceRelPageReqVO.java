package com.datamaster.module.assets.controller.admin.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据源与空间关系分页查询 Request VO
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Schema(description = "数据源与空间关系分页查询 Request VO")
@Data
public class AssetsDatasourceSpaceRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "空间ID", example = "1")
    private Long spaceId;

    @Schema(description = "空间编码", example = "bank_risk")
    private String spaceCode;

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "描述", example = "空间可使用该数据源")
    private String description;

    @Schema(description = "是否已分配到数据加工", example = "false")
    private Boolean dppAssigned;

}
