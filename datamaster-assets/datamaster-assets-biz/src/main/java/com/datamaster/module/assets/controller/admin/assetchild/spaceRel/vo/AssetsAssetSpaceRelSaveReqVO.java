package com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 数据资产与空间关联关系保存 Request VO
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Schema(description = "数据资产与空间关联关系保存 Request VO")
@Data
public class AssetsAssetSpaceRelSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "资产 ID", example = "1")
    private Long assetId;

    @Schema(description = "空间 ID", example = "1")
    private Long spaceId;

    @Schema(description = "空间编码", example = "bank_risk")
    @Size(max = 256, message = "空间编码长度不能超过256个字符")
    private String spaceCode;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

}
