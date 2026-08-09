package com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据资产与空间关联关系分页 Request VO
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Schema(description = "数据资产与空间关联关系分页 Request VO")
@Data
public class AssetsAssetSpaceRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;

    @Schema(description = "资产 ID", example = "1")
    private Long assetId;

    @Schema(description = "空间 ID", example = "1")
    private Long spaceId;

    @Schema(description = "空间编码", example = "bank_risk")
    private String spaceCode;

    @Schema(description = "描述", example = "")
    private String description;

}
