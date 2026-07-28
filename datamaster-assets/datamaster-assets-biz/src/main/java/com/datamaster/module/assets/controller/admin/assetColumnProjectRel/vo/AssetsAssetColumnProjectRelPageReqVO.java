package com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据资产字段与空间关联关系分页 Request VO
 *
 * @author DATAMASTER
 */
@Schema(description = "数据资产字段与空间关联关系分页 Request VO")
@Data
public class AssetsAssetColumnProjectRelPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID", example = "")
    private Long id;

    @Schema(description = "资产id", example = "")
    private Long assetId;

    @Schema(description = "字段id", example = "")
    private Long columnId;

    @Schema(description = "空间id", example = "")
    private Long projectId;

    @Schema(description = "空间编码", example = "")
    private String projectCode;
}
