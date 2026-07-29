package com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo;

import com.datamaster.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 数据资产字段与空间关联关系保存 Request VO
 *
 * @author DATAMASTER
 */
@Schema(description = "数据资产字段与空间关联关系保存 Request VO")
@Data
public class AssetsAssetColumnSpaceRelSaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "资产 ID", example = "")
    private Long assetId;

    @Schema(description = "字段 ID", example = "")
    private Long columnId;

    @Schema(description = "空间 ID", example = "")
    private Long spaceId;

    @Schema(description = "空间编码", example = "")
    @Size(max = 256, message = "空间编码长度不能超过256")
    private String spaceCode;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256")
    private String description;
}
