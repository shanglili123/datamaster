package com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo;

import com.datamaster.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 数据资产字段与项目关联关系保存 Request VO
 *
 * @author DATAMASTER
 */
@Schema(description = "数据资产字段与项目关联关系保存 Request VO")
@Data
public class AssetsAssetColumnProjectRelSaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "资产id", example = "")
    private Long assetId;

    @Schema(description = "字段id", example = "")
    private Long columnId;

    @Schema(description = "项目id", example = "")
    private Long projectId;

    @Schema(description = "项目编码", example = "")
    @Size(max = 256, message = "项目编码长度不能超过256")
    private String projectCode;
}
